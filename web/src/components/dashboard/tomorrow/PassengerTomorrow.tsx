"use client";

import { useEffect, useState } from "react";
import {
  RideRequestStatus,
  deleteRequest,
  getTomorrowDateKey,
  isTimeClose,
  listenActiveRidesForDate,
  listenPassengerRequests,
  listenPassengerUnfinishedLegs,
  notifyMatchingRiders,
  passengerConfirmTripCompleted,
  passengerConfirmTripStarted,
  passengerRejectTripStarted,
  requestPassengerCancellation,
  submitMissedRideAnswer,
  submitTomorrowRideRating,
  upsertPassengerRequest,
  type Ride,
  type RideRequest,
} from "@/lib/tomorrow";
import { claimTomorrowTripXp, awardPlanSavedXp } from "@/lib/xpClaim";
import { availableLocations, errorMessage, statusLabel } from "@/lib/rideNowHelpers";
import { ActionButton, StatusCard } from "@/components/dashboard/ridenow/PassengerRideNow";
import RatingModal from "@/components/dashboard/ridenow/RatingModal";
import MissedRideReview from "./MissedRideReview";

export default function PassengerTomorrow({
  passengerId,
  passengerName,
  passengerPhone,
}: {
  passengerId: string;
  passengerName: string;
  passengerPhone: string;
}) {
  const rideDate = getTomorrowDateKey();

  const [savedRequests, setSavedRequests] = useState<RideRequest[]>([]);
  const [activeRides, setActiveRides] = useState<Ride[]>([]);
  const [unfinishedLegs, setUnfinishedLegs] = useState<RideRequest[]>([]);

  const [wantCampus, setWantCampus] = useState(true);
  const [wantHome, setWantHome] = useState(true);
  const [campusPickup, setCampusPickup] = useState("Mirpur 12");
  const [homeDestination, setHomeDestination] = useState("Mirpur 12");
  const [campusTime, setCampusTime] = useState("08:30");
  const [homeTime, setHomeTime] = useState("15:30");

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ratingTarget, setRatingTarget] = useState<RideRequest | null>(null);

  useEffect(() => {
    const unsub1 = listenPassengerRequests(passengerId, rideDate, setSavedRequests);
    const unsub2 = listenActiveRidesForDate(rideDate, setActiveRides);
    const unsub3 = listenPassengerUnfinishedLegs(passengerId, setUnfinishedLegs);
    claimTomorrowTripXp(passengerId, false);
    return () => {
      unsub1();
      unsub2();
      unsub3();
    };
  }, [passengerId, rideDate]);

  const campusRequest = savedRequests.find((r) => r.tripDirection === "to_campus");
  const homeRequest = savedRequests.find((r) => r.tripDirection === "to_home");
  const isCampusLocked = Boolean(
    campusRequest && !["pending", "cancelled"].includes(campusRequest.status)
  );
  const isHomeLocked = Boolean(
    homeRequest && !["pending", "cancelled"].includes(homeRequest.status)
  );

  const matchedRides = savedRequests
    .filter((r) => r.status === "pending")
    .flatMap((request) =>
      activeRides
        .filter(
          (ride) =>
            ride.availableSeats > 0 &&
            ride.routeKey === request.routeKey &&
            isTimeClose(ride.timeMinutes, request.timeMinutes)
        )
        .map((ride) => ({ ride, request }))
    );

  function parseTime(value: string): { hour: number; minute: number } {
    const [h, m] = value.split(":").map(Number);
    return { hour: h || 0, minute: m || 0 };
  }

  async function savePlan() {
    if (!wantCampus && !wantHome) {
      setError("Please select at least one trip direction.");
      return;
    }
    setError(null);
    setIsLoading(true);

    try {
      const results: { docId: string; isNew: boolean }[] = [];

      if (wantCampus && !isCampusLocked) {
        const { hour, minute } = parseTime(campusTime);
        const result = await upsertPassengerRequest({
          userId: passengerId,
          passengerName: passengerName || "Passenger",
          passengerPhone,
          rideDate,
          tripDirection: "to_campus",
          pickup: campusPickup,
          destination: "AUST Gate",
          tripTime: formatDisplayTime(hour, minute),
          hour,
          minute,
          timeMinutes: hour * 60 + minute,
        });
        if (result.kind === "blocked") throw new Error(result.reason);
        results.push(result);
      }

      if (wantHome && !isHomeLocked) {
        const { hour, minute } = parseTime(homeTime);
        const result = await upsertPassengerRequest({
          userId: passengerId,
          passengerName: passengerName || "Passenger",
          passengerPhone,
          rideDate,
          tripDirection: "to_home",
          pickup: "AUST Gate",
          destination: homeDestination,
          tripTime: formatDisplayTime(hour, minute),
          hour,
          minute,
          timeMinutes: hour * 60 + minute,
        });
        if (result.kind === "blocked") throw new Error(result.reason);
        results.push(result);
      }

      if (results.length > 0) {
        await awardPlanSavedXp({
          userId: passengerId,
          isRider: false,
          sourceId: results[0].docId,
          rideDate,
        });
        results.forEach((r) => notifyMatchingRiders(r.docId));
      }
    } catch (err) {
      setError(errorMessage(err, "Failed to save plan."));
    } finally {
      setIsLoading(false);
    }
  }

  async function cancelAccepted(request: RideRequest) {
    setIsLoading(true);
    setError(null);
    try {
      await requestPassengerCancellation(request.requestId, "Passenger cancelled");
    } catch (err) {
      setError(errorMessage(err, "Failed to cancel."));
    } finally {
      setIsLoading(false);
    }
  }

  async function removeRequest(requestId: string) {
    setIsLoading(true);
    try {
      await deleteRequest(requestId, passengerId);
    } catch (err) {
      setError(errorMessage(err, "Failed to remove request."));
    } finally {
      setIsLoading(false);
    }
  }

  async function confirmStarted(requestId: string) {
    try {
      await passengerConfirmTripStarted(requestId);
    } catch (err) {
      setError(errorMessage(err, "Failed to confirm start."));
    }
  }

  async function rejectStarted(requestId: string) {
    try {
      await passengerRejectTripStarted(requestId);
    } catch (err) {
      setError(errorMessage(err, "Failed to reject start."));
    }
  }

  async function confirmCompleted(requestId: string) {
    try {
      await passengerConfirmTripCompleted(requestId);
      claimTomorrowTripXp(passengerId, false);
    } catch (err) {
      setError(errorMessage(err, "Failed to confirm completion."));
    }
  }

  async function rate(stars: number, comment: string) {
    if (!ratingTarget) return;
    try {
      await submitTomorrowRideRating({
        requestId: ratingTarget.requestId,
        ratedBy: passengerId,
        ratedTo: ratingTarget.matchedRiderId,
        stars,
        comment,
      });
      setRatingTarget(null);
    } catch (err) {
      setError(errorMessage(err, "Failed to submit rating."));
    }
  }

  async function answerMissedRide(request: RideRequest, didHappen: boolean) {
    try {
      const resolved = await submitMissedRideAnswer({
        requestId: request.requestId,
        userId: passengerId,
        isRider: false,
        answer: didHappen ? "yes" : "no",
      });
      if (resolved === RideRequestStatus.COMPLETED) {
        claimTomorrowTripXp(passengerId, false);
      }
    } catch (err) {
      setError(errorMessage(err, "Failed to save your answer."));
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <MissedRideReview legs={unfinishedLegs} isRider={false} onAnswer={answerMissedRide} />

      {[campusRequest, homeRequest]
        .filter((r): r is RideRequest => Boolean(r) && r!.status !== RideRequestStatus.CANCELLED)
        .map((request) => (
          <div key={request.requestId} className="flex flex-col gap-3">
            <StatusCard request={mapToRideNowShape(request)} isPassenger />

            {request.status === RideRequestStatus.PENDING && (
              <ActionButton
                loading={isLoading}
                variant="danger"
                onClick={() => removeRequest(request.requestId)}
              >
                Remove request
              </ActionButton>
            )}

            {request.status === RideRequestStatus.ACCEPTED && (
              <div className="flex gap-3">
                <a
                  href={`tel:${request.matchedRiderPhone}`}
                  className="flex-1 rounded-full border border-line-strong px-4 py-2 text-center text-sm font-medium"
                >
                  Call rider
                </a>
                <ActionButton loading={isLoading} variant="danger" onClick={() => cancelAccepted(request)}>
                  Cancel
                </ActionButton>
              </div>
            )}

            {request.status === RideRequestStatus.START_PENDING_CONFIRMATION && (
              <div className="flex gap-3">
                <ActionButton loading={false} onClick={() => confirmStarted(request.requestId)}>
                  Confirm started
                </ActionButton>
                <ActionButton loading={false} variant="secondary" onClick={() => rejectStarted(request.requestId)}>
                  Not yet
                </ActionButton>
              </div>
            )}

            {request.status === RideRequestStatus.ONGOING && (
              <a
                href={`tel:${request.matchedRiderPhone}`}
                className="rounded-full border border-line-strong px-4 py-2 text-center text-sm font-medium"
              >
                Call rider
              </a>
            )}

            {request.status === RideRequestStatus.END_PENDING_CONFIRMATION && (
              <ActionButton loading={false} onClick={() => confirmCompleted(request.requestId)}>
                Confirm safe arrival
              </ActionButton>
            )}

            {request.status === RideRequestStatus.COMPLETED && !request.riderRated && (
              <ActionButton loading={false} onClick={() => setRatingTarget(request)}>
                Rate rider
              </ActionButton>
            )}
          </div>
        ))}

      <div className="rounded-2xl border border-line p-5">
        <h2 className="text-base font-semibold">Plan tomorrow&apos;s rides</h2>
        <p className="mt-1 text-sm text-ink-soft">{rideDate}</p>

        <div className="mt-4 flex flex-col gap-4">
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="wantCampus"
              checked={wantCampus}
              disabled={isCampusLocked}
              onChange={(e) => setWantCampus(e.target.checked)}
            />
            <label htmlFor="wantCampus" className="text-sm font-medium">
              To campus{isCampusLocked ? " (already matched)" : ""}
            </label>
          </div>
          {wantCampus && !isCampusLocked && (
            <div className="ml-6 flex flex-col gap-3">
              <LocationField label="Pickup" value={campusPickup} onChange={setCampusPickup} />
              <TimeField label="Departure time" value={campusTime} onChange={setCampusTime} />
            </div>
          )}

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="wantHome"
              checked={wantHome}
              disabled={isHomeLocked}
              onChange={(e) => setWantHome(e.target.checked)}
            />
            <label htmlFor="wantHome" className="text-sm font-medium">
              Back home{isHomeLocked ? " (already matched)" : ""}
            </label>
          </div>
          {wantHome && !isHomeLocked && (
            <div className="ml-6 flex flex-col gap-3">
              <LocationField label="Destination" value={homeDestination} onChange={setHomeDestination} />
              <TimeField label="Departure time" value={homeTime} onChange={setHomeTime} />
            </div>
          )}
        </div>

        {error && <p className="mt-3 text-sm text-accent-red">{error}</p>}

        <div className="mt-4">
          <ActionButton loading={isLoading} onClick={savePlan}>
            Save plan
          </ActionButton>
        </div>
      </div>

      <div className="rounded-2xl border border-line p-5">
        <h2 className="text-base font-semibold">Matched rides</h2>
        {matchedRides.length === 0 ? (
          <p className="mt-3 text-sm text-ink-soft">
            No rider matched your saved request yet.
          </p>
        ) : (
          <div className="mt-4 flex flex-col gap-3">
            {matchedRides.map(({ ride, request }) => (
              <div
                key={`${ride.rideId}_${request.requestId}`}
                className="rounded-xl border border-line p-4"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">{ride.riderName}</span>
                  <span className="text-xs text-ink-faint">
                    {ride.availableSeats}/{ride.totalSeats} seats
                  </span>
                </div>
                <p className="mt-1 text-sm text-ink-soft">
                  {ride.pickup} → {ride.destination} at {ride.tripTime}
                </p>
              </div>
            ))}
          </div>
        )}
        <p className="mt-3 text-xs text-ink-faint dark:text-ink-faint">
          Riders accept requests from their side — this list is informational.
        </p>
      </div>

      {ratingTarget && (
        <RatingModal subject="rider" onDismiss={() => setRatingTarget(null)} onSubmit={rate} />
      )}
    </div>
  );
}

function formatDisplayTime(hour: number, minute: number): string {
  const period = hour >= 12 ? "PM" : "AM";
  const h12 = hour % 12 === 0 ? 12 : hour % 12;
  return `${h12}:${String(minute).padStart(2, "0")} ${period}`;
}

/** Adapts a Tomorrow RideRequest onto the shape StatusCard (built for Ride Now) expects. */
function mapToRideNowShape(request: RideRequest) {
  return {
    requestId: request.requestId,
    pickup: request.pickup,
    destination: request.destination,
    status: request.status,
    matchedRiderName: request.matchedRiderName,
    matchedRiderPhone: request.matchedRiderPhone,
    matchedVehicleModel: request.matchedVehicleModel,
    matchedVehicleNumber: request.matchedVehicleNumber,
    passengerName: request.passengerName,
    passengerPhone: request.passengerPhone,
  } as Parameters<typeof StatusCard>[0]["request"];
}

function LocationField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
      >
        {availableLocations
          .filter((loc) => loc !== "AUST Gate")
          .map((loc) => (
            <option key={loc} value={loc}>
              {loc}
            </option>
          ))}
      </select>
    </div>
  );
}

function TimeField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium">{label}</label>
      <input
        type="time"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
      />
    </div>
  );
}

export { statusLabel };
