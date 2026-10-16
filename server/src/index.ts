import express, {NextFunction, Request, Response} from "express";
import {cert, initializeApp, ServiceAccount} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {DocumentReference, FieldValue, getFirestore} from "firebase-admin/firestore";
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

              // Never hand back more seats than the rider opened up.
              // totalSeats is absent on rides created before cars existed,
              // which were all single-seat bikes - hence the fallback of 1.
              const total = rideData.totalSeats;
              const capacity = typeof total === "number" ?
                Math.max(total, currentSeats, 1) :
                Math.max(currentSeats, 1);

              tx.update(rideRef, {
                availableSeats: Math.min(currentSeats + 1, capacity),
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

// ---------- In-ride chat ----------
//
// Messages live at {ride_now_requests|ride_requests}/{requestId}/messages
// and are written by the clients directly (firestore.rules gates who and
// when). This server only does the two privileged parts: pushing the
// other side, and deleting each thread 24 hours after its trip ends.
// ride_chats/{flow}_{requestId} is a server-only index of threads that
// exist, so the sweep never has to scan every ride.

const CHAT_FLOWS: Record<string, {collection: string; passengerField: string}> = {
  now: {collection: "ride_now_requests", passengerField: "passengerId"},
  tomorrow: {collection: "ride_requests", passengerField: "userId"},
};

const CHAT_OPEN_STATUSES = [
  "accepted",
  "start_pending_confirmation",
  "ongoing",
  "end_pending_confirmation",
];

const CHAT_RETENTION_MS = 24 * 60 * 60 * 1000;
const CHAT_PUSH_MIN_GAP_MS = 5 * 1000;
const CHAT_SWEEP_INTERVAL_MS = 30 * 60 * 1000;

app.post(
    "/api/chat/notify",
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const uid = await requireUid(req);
        const flowName = req.body?.flow as string | undefined;
        const requestId = req.body?.requestId as string | undefined;
        const flow = flowName ? CHAT_FLOWS[flowName] : undefined;

        if (!flow || !requestId) {
          throw new ApiError(400, "flow and requestId are required.");
        }

        const requestRef = db.collection(flow.collection).doc(requestId);
        const requestSnap = await requestRef.get();

        if (!requestSnap.exists) {
          throw new ApiError(404, "Ride not found.");
        }

        const data = requestSnap.data()!;
        const passengerId = data[flow.passengerField] as string;
        const riderId = data.matchedRiderId as string;

        if (!riderId || (uid !== passengerId && uid !== riderId)) {
          throw new ApiError(403, "You aren't part of this ride.");
        }

        const recipientId = uid === passengerId ? riderId : passengerId;
        const senderName = uid === passengerId ?
          (data.passengerName as string) || "Your passenger" :
          (data.matchedRiderName as string) || "Your rider";

        // Register the thread for the cleanup sweep, and rate-limit pushes
        // per recipient so a burst of quick replies is one notification.
        const chatRef = db.collection("ride_chats").doc(`${flowName}_${requestId}`);
        const pushField = `lastPushAt_${recipientId}`;
        const chatSnap = await chatRef.get();
        const lastPushAt = chatSnap.data()?.[pushField]?.toMillis?.() ?? 0;

        await chatRef.set({
          flow: flowName,
          requestId,
          [pushField]: FieldValue.serverTimestamp(),
        }, {merge: true});

        if (Date.now() - lastPushAt < CHAT_PUSH_MIN_GAP_MS) {
          res.status(200).json({success: true, pushed: false});
          return;
        }

        // Latest message from this sender, for the notification preview.
        // (Filtered in code so this needs no composite index.)
        const recent = await requestRef.collection("messages")
            .orderBy("createdAt", "desc")
            .limit(10)
            .get();
        const latest = recent.docs.find((d) => d.get("senderId") === uid);
        const preview = ((latest?.get("text") as string) || "Sent you a message").slice(0, 120);

        const recipientSnap = await db.collection("users").doc(recipientId).get();
        const tokens = (recipientSnap.data()?.fcmTokens as string[] | undefined) ?? [];

        if (tokens.length > 0) {
          const response = await getMessaging().sendEachForMulticast({
            tokens,
            notification: {
              title: `New message from ${senderName}`,
              body: preview,
            },
            data: {type: "chat", flow: flowName!, requestId},
          });

          await pruneInvalidTokens(recipientId, tokens, response);
        }

        res.status(200).json({success: true, pushed: tokens.length > 0});
      } catch (err) {
        next(err);
      }
    }
);

async function deleteChatMessages(requestRef: DocumentReference): Promise<void> {
  const messages = requestRef.collection("messages");

  // Batches cap at 500 writes; loop until the thread is empty.
  for (;;) {
    const page = await messages.limit(400).get();
    if (page.empty) return;

    const batch = db.batch();
    page.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
}

/**
 * Deletes every chat thread whose trip ended more than 24 hours ago. The
 * first sweep that sees a trip finished stamps closedAt; a later sweep
 * deletes once closedAt is old enough. Runs on startup and every 30
 * minutes - if the host sleeps, the next wake-up catches up.
 */
async function sweepExpiredChats(): Promise<void> {
  const chats = await db.collection("ride_chats").get();
  const now = Date.now();

  for (const chatDoc of chats.docs) {
    try {
      const chat = chatDoc.data();
      const flow = CHAT_FLOWS[chat.flow as string];

      if (!flow) {
        await chatDoc.ref.delete();
        continue;
      }

      const requestRef = db.collection(flow.collection).doc(chat.requestId as string);
      const requestSnap = await requestRef.get();
      const status = requestSnap.get("status") as string | undefined;

      if (requestSnap.exists && status && CHAT_OPEN_STATUSES.includes(status)) {
        continue;
      }

      const closedAt = chat.closedAt?.toMillis?.() as number | undefined;

      if (closedAt === undefined) {
        await chatDoc.ref.update({closedAt: FieldValue.serverTimestamp()});
        continue;
      }

      if (now - closedAt >= CHAT_RETENTION_MS) {
        await deleteChatMessages(requestRef);
        await chatDoc.ref.delete();
        console.log(`Deleted expired chat ${chatDoc.id}`);
      }
    } catch (err) {
      console.error(`Chat sweep failed for ${chatDoc.id}`, err);
    }
  }
}

function runChatSweep(): void {
  sweepExpiredChats().catch((err) => console.error("Chat sweep failed", err));
}

runChatSweep();
setInterval(runChatSweep, CHAT_SWEEP_INTERVAL_MS);

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