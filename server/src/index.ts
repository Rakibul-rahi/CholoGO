import express, {NextFunction, Request, Response} from "express";
import {cert, initializeApp, ServiceAccount} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, getFirestore} from "firebase-admin/firestore";

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