import express, {NextFunction, Request, Response} from "express";
import {cert, initializeApp, ServiceAccount} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, getFirestore} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";

const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT;

if (!serviceAccountJson) {
  throw new Error(
      "FIREBASE_SERVICE_ACCOUNT environment variable is not set."
  );
}

const serviceAccount = JSON.parse(serviceAccountJson) as ServiceAccount;

initializeApp({credential: cert(serviceAccount)});

const auth = getAuth();
const db = getFirestore();

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 3000;

class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function requireUid(req: Request): Promise<string> {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";

  if (!token) {
    throw new ApiError(401, "Missing Authorization header.");
  }

  try {
    const decoded = await auth.verifyIdToken(token);
    return decoded.uid;
  } catch {
    throw new ApiError(401, "Invalid or expired token.");
  }
}

/**
 * Removes any device tokens that FCM reported as dead (uninstalled app,
 * rotated token) from a user's fcmTokens array, so it doesn't grow
 * unboundedly with tokens that will only ever fail again.
 */
async function pruneInvalidTokens(
    userId: string,
    tokens: string[],
    response: {responses: {success: boolean; error?: {code: string}}[]}
): Promise<void> {
  const invalid: string[] = [];

  response.responses.forEach((r, i) => {
    const code = r.error?.code;
    if (
      !r.success &&
      (code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token")
    ) {
      invalid.push(tokens[i]);
    }
  });

  if (invalid.length > 0) {
    await db.collection("users").doc(userId).update({
      fcmTokens: FieldValue.arrayRemove(...invalid),
    });
  }
}

/**
 * Cancels a passenger's own accepted Tomorrow-ride request and restores
 * the matched rider's seat, in one synchronous call. Replaces the
 * Firestore-triggered Cloud Function version - functionally identical
 * logic, just invoked directly over HTTP instead of via a trigger, which
 * also means no more "wait a moment for it to finalize" delay.
 */
app.post(
    "/api/tomorrow/cancel-request",
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const uid = await requireUid(req);
        const requestId = req.body?.requestId as string | undefined;
        const reason = (req.body?.reason as string | undefined) || "";

        if (!requestId) {
          throw new ApiError(400, "requestId is required.");
        }

        const requestRef = db.collection("ride_requests").doc(requestId);

        await db.runTransaction(async (tx) => {
          const requestSnap = await tx.get(requestRef);

          if (!requestSnap.exists) {
            throw new ApiError(404, "Request not found.");
          }

          const data = requestSnap.data();

          if (!data || data.userId !== uid) {
            throw new ApiError(403, "You can only cancel your own request.");
          }

          if (data.status !== "accepted") {
            throw new ApiError(
                409,
                "This request isn't in an accepted state."
            );
          }

          const now = FieldValue.serverTimestamp();

          tx.update(requestRef, {
            status: "cancelled",
            cancelledBy: uid,
            cancelledByRole: "passenger",
            cancellationReason: reason,
            cancelledAt: now,
          });

          const rideId = data.matchedRideId as string | undefined;

          if (rideId) {
            const rideRef = db.collection("rides").doc(rideId);
            const rideSnap = await tx.get(rideRef);
            const rideData = rideSnap.data();

            if (rideSnap.exists && rideData) {
              const seats = rideData.availableSeats;
              const currentSeats = typeof seats === "number" ? seats : 0;

              tx.update(rideRef, {
                availableSeats: currentSeats + 1,
                status: "active",
                lastUpdatedAt: now,
              });
            }
          }
        });

        res.status(200).json({success: true});
      } catch (err) {
        next(err);
      }
    }
);

/**
 * Pushes the passenger a notification that their request was accepted.
 * Called by the accepting rider's own client right after their accept
 * transaction succeeds. Only that rider can trigger it (matchedRiderId
 * must equal the caller) - a request can only ever transition to
 * "accepted" once, so this never needs its own dedup.
 */
app.post(
    "/api/tomorrow/notify-accepted",
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const uid = await requireUid(req);
        const requestId = req.body?.requestId as string | undefined;

        if (!requestId) {
          throw new ApiError(400, "requestId is required.");
        }

        const requestSnap = await db.collection("ride_requests").doc(requestId).get();

        if (!requestSnap.exists) {
          throw new ApiError(404, "Request not found.");
        }

        const data = requestSnap.data()!;

        if (data.status !== "accepted") {
          throw new ApiError(409, "This request isn't in an accepted state.");
        }

        if (data.matchedRiderId !== uid) {
          throw new ApiError(403, "You didn't accept this request.");
        }

        const passengerId = data.userId as string;
        const passengerSnap = await db.collection("users").doc(passengerId).get();
        const tokens = (passengerSnap.data()?.fcmTokens as string[] | undefined) ?? [];

        if (tokens.length > 0) {
          const directionLabel = data.tripDirection === "to_campus" ? "to campus" : "back home";
          const riderName = (data.matchedRiderName as string) || "A rider";

          const response = await getMessaging().sendEachForMulticast({
            tokens,
            notification: {
              title: "Tomorrow Ride accepted!",
              body: `${riderName} accepted your ${directionLabel} request for ${data.tripTime}.`,
            },
          });

          await pruneInvalidTokens(passengerId, tokens, response);
        }

        res.status(200).json({success: true});
      } catch (err) {
        next(err);
      }
    }
);

/**
 * Pushes any rider whose saved ride matches this still-pending request.
 * Called by the request's own owner (the passenger) right after
 * creating/resubmitting it. Deduped per (rideId, requestId) pair via a
 * marker doc, since resubmitting an unchanged pending leg is a realistic
 * repeat trigger and would otherwise re-notify the same rider every time.
 */
app.post(
    "/api/tomorrow/notify-match",
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const uid = await requireUid(req);
        const requestId = req.body?.requestId as string | undefined;

        if (!requestId) {
          throw new ApiError(400, "requestId is required.");
        }

        const requestSnap = await db.collection("ride_requests").doc(requestId).get();

        if (!requestSnap.exists) {
          throw new ApiError(404, "Request not found.");
        }

        const data = requestSnap.data()!;

        if (data.userId !== uid) {
          throw new ApiError(403, "This isn't your request.");
        }

        if (data.status !== "pending") {
          res.status(200).json({success: true, matched: 0});
          return;
        }

        const ridesSnap = await db.collection("rides")
            .where("rideDate", "==", data.rideDate)
            .where("status", "==", "active")
            .where("routeKey", "==", data.routeKey)
            .get();

        const requestTimeMinutes = data.timeMinutes as number;
        const directionLabel = data.tripDirection === "to_campus" ? "to campus" : "back home";
        const passengerName = (data.passengerName as string) || "A passenger";

        let notifiedCount = 0;

        for (const rideDoc of ridesSnap.docs) {
          const rideData = rideDoc.data();
          const seats = rideData.availableSeats as number | undefined;
          const rideTimeMinutes = rideData.timeMinutes as number | undefined;

          if (
            (seats ?? 0) <= 0 ||
            rideTimeMinutes === undefined ||
            Math.abs(rideTimeMinutes - requestTimeMinutes) > 30
          ) {
            continue;
          }

          const riderId = rideData.riderId as string | undefined;
          if (!riderId) continue;

          const markerRef = db
              .collection("tomorrow_match_notifications")
              .doc(`${rideDoc.id}_${requestId}`);

          if ((await markerRef.get()).exists) {
            continue;
          }

          const riderSnap = await db.collection("users").doc(riderId).get();
          const tokens = (riderSnap.data()?.fcmTokens as string[] | undefined) ?? [];

          if (tokens.length === 0) {
            continue;
          }

          const response = await getMessaging().sendEachForMulticast({
            tokens,
            notification: {
              title: "Passenger available for your route!",
              body: `${passengerName} wants a ride ${directionLabel} at ${data.tripTime}. Tap to accept.`,
            },
          });

          await pruneInvalidTokens(riderId, tokens, response);

          await markerRef.set({
            rideId: rideDoc.id,
            requestId,
            riderId,
            notifiedAt: FieldValue.serverTimestamp(),
          });

          notifiedCount++;
        }

        res.status(200).json({success: true, matched: notifiedCount});
      } catch (err) {
        next(err);
      }
    }
);

app.get("/health", (_req: Request, res: Response) => {
  res.status(200).send("ok");
});

app.use(
    (err: unknown, _req: Request, res: Response, _next: NextFunction) => {
      if (err instanceof ApiError) {
        res.status(err.status).json({error: err.message});
        return;
      }

      console.error(err);
      res.status(500).json({error: "Internal server error."});
    }
);

app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});