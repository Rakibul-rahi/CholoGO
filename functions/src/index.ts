import {onDocumentUpdated} from "firebase-functions/v2/firestore";
import {initializeApp} from "firebase-admin/app";
import {getFirestore, FieldValue} from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";

initializeApp();
const db = getFirestore();

const STATUS_CANCEL_REQUESTED = "cancel_requested_by_passenger";
const STATUS_CANCELLED = "cancelled";

/**
 * A passenger can write "cancel_requested_by_passenger" on their own
 * accepted request (allowed by firestore.rules), but restoring the seat
 * on the matched ride requires writing to a document they don't own.
 * This function does that privileged part.
 *
 * Triggered on every update to ride_requests/{requestId}; only acts on
 * the specific transition into cancel_requested_by_passenger, and
 * re-checks status inside the transaction so it's safe even if this
 * trigger somehow fires more than once for the same write (Cloud
 * Functions guarantees at-least-once delivery, not exactly-once).
 */
export const finalizePassengerCancellation = onDocumentUpdated(
  "ride_requests/{requestId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) {
      return;
    }

    const statusUnchanged = before.status === after.status;
    const isCancelRequest = after.status === STATUS_CANCEL_REQUESTED;

    if (statusUnchanged || !isCancelRequest) {
      return;
    }

    const requestId = event.params.requestId;
    const rideId: string | undefined = after.matchedRideId;

    const requestRef = db.collection("ride_requests").doc(requestId);

    if (!rideId) {
      logger.warn(
          `Request ${requestId} has no matchedRideId; ` +
          "finalizing without seat restore."
      );
      await requestRef.update({
        status: STATUS_CANCELLED,
        cancelledAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    const rideRef = db.collection("rides").doc(rideId);

    await db.runTransaction(async (tx) => {
      const requestSnap = await tx.get(requestRef);

      if (!requestSnap.exists) {
        logger.warn(`Request ${requestId} no longer exists, skipping.`);
        return;
      }

      const requestData = requestSnap.data();

      if (!requestData || requestData.status !== STATUS_CANCEL_REQUESTED) {
        // Already processed, or state moved on - do nothing further.
        return;
      }

      tx.update(requestRef, {
        status: STATUS_CANCELLED,
        cancelledAt: FieldValue.serverTimestamp(),
      });

      const rideSnap = await tx.get(rideRef);
      const rideData = rideSnap.data();

      if (rideSnap.exists && rideData) {
        const seats = rideData.availableSeats;
        const currentSeats = typeof seats === "number" ? seats : 0;

        tx.update(rideRef, {
          availableSeats: currentSeats + 1,
          status: "active",
          lastUpdatedAt: FieldValue.serverTimestamp(),
        });
      } else {
        logger.warn(
            `Ride ${rideId} for request ${requestId} no longer exists.`
        );
      }
    });

    logger.info(
        `Finalized passenger cancellation for request ${requestId}, ` +
        `ride ${rideId}.`
    );
  }
);