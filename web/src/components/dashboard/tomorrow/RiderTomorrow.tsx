"use client";

import { useEffect, useState } from "react";
import {
  RideRequestStatus,
  acceptRequest,
  declineRequest,
  deleteRide,
  getTomorrowDateKey,
  isTimeClose,
  listenAcceptedRequestsForRider,
  listenPendingRequests,
  listenRiderRides,
  listenRiderUnfinishedLegs,
  notifyPassengerAccepted,
  riderCancelAcceptedRide,
  riderRequestTripCompletion,
  riderStartTrip,
  seatCapacity,
  seatsTaken,
  submitMissedRideAnswer,
  submitTomorrowPassengerRating,
  upsertRiderRide,
  type Ride,
  type RideRequest,
} from "@/lib/tomorrow";
import { claimTomorrowTripXp, awardPlanSavedXp } from "@/lib/xpClaim";
import { availableLocations, errorMessage, statusLabel } from "@/lib/rideNowHelpers";
import { ActionButton } from "@/components/dashboard/ridenow/PassengerRideNow";
import RatingModal from "@/components/dashboard/ridenow/RatingModal";
import MissedRideReview from "./MissedRideReview";

export default function RiderTomorrow({
  riderId,
  riderName,
  riderPhone,
  vehicleType,
  vehicleModel,
  vehicleNumber,
  vehicleColor,
}: {
  riderId: string;
  riderName: string;
  riderPhone: string;
  vehicleType: string;
  vehicleModel: string;
  vehicleNumber: string;
  vehicleColor: string;
}) {
  const rideDate = getTomorrowDateKey();
  const isCar = vehicleType.trim().toLowerCase() === "car";

  const [savedRides, setSavedRides] = useState<Ride[]>([]);
  const [pendingRequests, setPendingRequests] = useState<RideRequest[]>([]);
  const [acceptedRequests, setAcceptedRequests] = useState<RideRequest[]>([]);
  const [unfinishedLegs, setUnfinishedLegs] = useState<RideRequest[]>([]);

  const [campusPickup, setCampusPickup] = useState("Mirpur 12");
  const [homeDestination, setHomeDestination] = useState("Mirpur 12");
  const [campusTime, setCampusTime] = useState("08:00");
  const [homeTime, setHomeTime] = useState("15:30");
  const [campusSeats, setCampusSeats] = useState(1);
  const [homeSeats, setHomeSeats] = useState(1);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ratingTarget, setRatingTarget] = useState<RideRequest | null>(null);
  const [processingId, setProcessingId] = useState<string | null>(null);

  useEffect(() => {
    const unsub1 = listenRiderRides(riderId, rideDate, setSavedRides);
    const unsub2 = listenPendingRequests(rideDate, setPendingRequests);
    const unsub3 = listenAcceptedRequestsForRider(riderId, rideDate, setAcceptedRequests);
    const unsub4 = listenRiderUnfinishedLegs(riderId, setUnfinishedLegs);
    claimTomorrowTripXp(riderId, true);
    return () => {
      unsub1();
      unsub2();
      unsub3();
      unsub4();
    };
  }, [riderId, rideDate]);

  const campusRide = savedRides.find((r) => r.tripDirection === "to_campus");
  const homeRide = savedRides.find((r) => r.tripDirection === "to_home");
  const isCampusLocked = Boolean(campusRide && (campusRide.status !== "active" || seatsTaken(campusRide) > 0));
  const isHomeLocked = Boolean(homeRide && (homeRide.status !== "active" || seatsTaken(homeRide) > 0));

  const matchedRequests = savedRides.flatMap((ride) =>
    pendingRequests
      .filter(
        (req) =>
          ride.status === "active" &&
          ride.availableSeats > 0 &&
          ride.routeKey === req.routeKey &&
          isTimeClose(ride.timeMinutes, req.timeMinutes) &&
          !req.rejectedByRiderIds.includes(riderId)
      )
      .map((req) => ({ ride, request: req }))
  );

  function parseTime(value: string): { hour: number; minute: number } {
    const [h, m] = value.split(":").map(Number);
    return { hour: h || 0, minute: m || 0 };
  }

  function formatDisplayTime(hour: number, minute: number): string {
    const period = hour >= 12 ? "PM" : "AM";
    const h12 = hour % 12 === 0 ? 12 : hour % 12;
    return `${h12}:${String(minute).padStart(2, "0")} ${period}`;
  }

  async function saveSetup() {
    setError(null);
    setIsLoading(true);
    try {
      const results: { docId: string }[] = [];

      if (!isCampusLocked) {
        const { hour, minute } = parseTime(campusTime);
        const result = await upsertRiderRide({
          riderId,
          riderName: riderName || "Rider",
          rideDate,
          tripDirection: "to_campus",
          pickup: campusPickup,
          destination: "AUST Gate",
          tripTime: formatDisplayTime(hour, minute),
          timeMinutes: hour * 60 + minute,
          vehicleType,
          vehicleModel,
          vehicleNumber,
          vehicleColor,
          requestedSeats: campusSeats,
        });
        if (result.kind === "blocked") throw new Error(result.reason);
        results.push(result);
      }

      if (!isHomeLocked) {
        const { hour, minute } = parseTime(homeTime);
        const result = await upsertRiderRide({
          riderId,
          riderName: riderName || "Rider",
          rideDate,
          tripDirection: "to_home",
          pickup: "AUST Gate",
          destination: homeDestination,
          tripTime: formatDisplayTime(hour, minute),
          timeMinutes: hour * 60 + minute,
          vehicleType,
          vehicleModel,
          vehicleNumber,
          vehicleColor,
          requestedSeats: homeSeats,
        });
        if (result.kind === "blocked") throw new Error(result.reason);
        results.push(result);
      }

      if (results.length > 0) {
        await awardPlanSavedXp({
          userId: riderId,
          isRider: true,
          sourceId: results[0].docId,
          rideDate,
        });
      }
    } catch (err) {
      setError(errorMessage(err, "Failed to save your rides."));
    } finally {
      setIsLoading(false);
    }
  }

  async function removeRide(rideId: string) {
    setIsLoading(true);
    try {
      await deleteRide(rideId, riderId);
    } catch (err) {
      setError(errorMessage(err, "Failed to remove ride."));
    } finally {
      setIsLoading(false);
    }
  }

  async function accept(match: { ride: Ride; request: RideRequest }) {
    setProcessingId(match.request.requestId);
    setError(null);
    try {
      await acceptRequest({
        rideId: match.ride.rideId,
        requestId: match.request.requestId,
        riderId,
        riderName: riderName || "Rider",
        riderPhone: riderPhone || "N/A",
      });
      notifyPassengerAccepted(match.request.requestId);
    } catch (err) {
      setError(errorMessage(err, "Failed to accept request."));
    } finally {
      setProcessingId(null);
    }
  }

  async function decline(requestId: string) {
    setProcessingId(requestId);
    try {
      await declineRequest(requestId, riderId);
    } catch (err) {
      setError(errorMessage(err, "Failed to decline request."));
    } finally {
      setProcessingId(null);
    }
  }

  async function cancelAccepted(request: RideRequest) {
    setProcessingId(request.requestId);
    try {
      await riderCancelAcceptedRide({
        rideId: request.matchedRideId,
        requestId: request.requestId,
        riderId,
        reason: "Rider cancelled",
      });
    } catch (err) {
      setError(errorMessage(err, "Failed to cancel ride."));
    } finally {
      setProcessingId(null);
    }
  }

  async function startTrip(requestId: string) {
    setProcessingId(requestId);
    try {
      await riderStartTrip(requestId, riderId);
    } catch (err) {
      setError(errorMessage(err, "Failed to start trip."));
    } finally {
      setProcessingId(null);
    }
  }

  async function completeTrip(requestId: string) {
    setProcessingId(requestId);
    try {
      await riderRequestTripCompletion(requestId, riderId);
    } catch (err) {
      setError(errorMessage(err, "Failed to complete trip."));
    } finally {
      setProcessingId(null);
    }
  }

  async function rate(stars: number, comment: string) {
    if (!ratingTarget) return;
    try {
      await submitTomorrowPassengerRating({
        requestId: ratingTarget.requestId,
        ratedBy: riderId,
        ratedTo: ratingTarget.userId,
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
        userId: riderId,
        isRider: true,
        answer: didHappen ? "yes" : "no",
      });
      if (resolved === RideRequestStatus.COMPLETED) {
        claimTomorrowTripXp(riderId, true);
      }
    } catch (err) {
      setError(errorMessage(err, "Failed to save your answer."));
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <MissedRideReview legs={unfinishedLegs} isRider onAnswer={answerMissedRide} />

      <div className="rounded-2xl border border-line p-5">
        <h2 className="text-base font-semibold">Set up tomorrow&apos;s rides</h2>
        <p className="mt-1 text-sm text-ink-soft">{rideDate}</p>

        <div className="mt-4 flex flex-col gap-4">
          <div>
            <p className="text-sm font-medium">
              To campus{isCampusLocked ? " (already matched)" : ""}
            </p>
            {!isCampusLocked && (
              <div className="mt-2 flex flex-col gap-3">
                <LocationField label="Pickup" value={campusPickup} onChange={setCampusPickup} />
                <TimeField label="Departure time" value={campusTime} onChange={setCampusTime} />
                {isCar && (
                  <SeatField label="Seats offered" value={campusSeats} onChange={setCampusSeats} />
                )}
              </div>
            )}
          </div>

          <div>
            <p className="text-sm font-medium">
              Back home{isHomeLocked ? " (already matched)" : ""}
            </p>
            {!isHomeLocked && (
              <div className="mt-2 flex flex-col gap-3">
                <LocationField label="Destination" value={homeDestination} onChange={setHomeDestination} />
                <TimeField label="Departure time" value={homeTime} onChange={setHomeTime} />
                {isCar && <SeatField label="Seats offered" value={homeSeats} onChange={setHomeSeats} />}
              </div>
            )}
          </div>
        </div>

        {error && <p className="mt-3 text-sm text-accent-red">{error}</p>}

        <div className="mt-4">
          <ActionButton loading={isLoading} onClick={saveSetup}>
            Save rides
          </ActionButton>
        </div>
      </div>

      {savedRides.map((ride) => {
        const passengers = acceptedRequests.filter((r) => r.matchedRideId === ride.rideId);
        return (
          <div key={ride.rideId} className="rounded-2xl border border-line p-5">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold">
                {ride.tripDirection === "to_campus" ? "To campus" : "Back home"}: {ride.pickup} →{" "}
                {ride.destination}
              </span>
              <span className="text-xs text-ink-faint">
                {ride.availableSeats}/{seatCapacity(ride)} seats left
              </span>
            </div>
            <p className="mt-1 text-sm text-ink-soft">{ride.tripTime}</p>

            {passengers.length === 0 ? (
              seatsTaken(ride) === 0 && (
                <ActionButton loading={isLoading} variant="danger" onClick={() => removeRide(ride.rideId)}>
                  Remove
                </ActionButton>
              )
            ) : (
              <div className="mt-3 flex flex-col gap-3">
                {passengers.map((request) => (
                  <PassengerLegRow
                    key={request.requestId}
                    request={request}
                    isProcessing={processingId === request.requestId}
                    onCancel={() => cancelAccepted(request)}
                    onStart={() => startTrip(request.requestId)}
                    onComplete={() => completeTrip(request.requestId)}
                    onRate={() => setRatingTarget(request)}
                  />
                ))}
              </div>
            )}
          </div>
        );
      })}

      <div className="rounded-2xl border border-line p-5">
        <h2 className="text-base font-semibold">Passenger matches</h2>
        {matchedRequests.length === 0 ? (
          <p className="mt-3 text-sm text-ink-soft">
            No matched requests right now.
          </p>
        ) : (
          <div className="mt-4 flex flex-col gap-3">
            {matchedRequests.map(({ request, ride }) => (
              <div
                key={request.requestId}
                className="rounded-xl border border-line p-4"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">{request.passengerName}</span>
                  <span className="text-xs text-ink-faint">{request.tripTime}</span>
                </div>
                <p className="mt-1 text-sm text-ink-soft">
                  {request.pickup} → {request.destination}
                </p>
                <div className="mt-3 flex gap-3">
                  <button
                    onClick={() => accept({ ride, request })}
                    disabled={processingId === request.requestId}
                    className="flex-1 rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-ink disabled:opacity-60"
                  >
                    Accept
                  </button>
                  <button
                    onClick={() => decline(request.requestId)}
                    disabled={processingId === request.requestId}
                    className="flex-1 rounded-full border border-line-strong px-4 py-2 text-sm font-medium disabled:opacity-60"
                  >
                    Decline
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {ratingTarget && (
        <RatingModal subject="passenger" onDismiss={() => setRatingTarget(null)} onSubmit={rate} />
      )}
    </div>
  );
}

function PassengerLegRow({
  request,
  isProcessing,
  onCancel,
  onStart,
  onComplete,
  onRate,
}: {
  request: RideRequest;
  isProcessing: boolean;
  onCancel: () => void;
  onStart: () => void;
  onComplete: () => void;
  onRate: () => void;
}) {
  return (
    <div className="rounded-xl bg-card p-3">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium">{request.passengerName}</span>
        <span className="rounded-full bg-surface px-2 py-0.5 text-xs">
          {statusLabel(request.status)}
        </span>
      </div>

      {request.passengerPhone && (
        <p className="mt-1 text-xs text-ink-soft">
          Phone:{" "}
          <a href={`tel:${request.passengerPhone}`} className="font-medium text-accent">
            {request.passengerPhone}
          </a>
        </p>
      )}

      <div className="mt-2 flex gap-2">
        {request.status === RideRequestStatus.ACCEPTED && (
          <>
            <a
              href={`tel:${request.passengerPhone}`}
              className="flex-1 rounded-full border border-line-strong px-3 py-1.5 text-center text-xs font-medium"
            >
              Call
            </a>
            <button
              onClick={onStart}
              disabled={isProcessing}
              className="flex-1 rounded-full bg-accent px-3 py-1.5 text-xs font-semibold text-accent-ink disabled:opacity-60"
            >
              Start trip
            </button>
            <button
              onClick={onCancel}
              disabled={isProcessing}
              className="flex-1 rounded-full border border-accent-red/30 px-3 py-1.5 text-xs font-medium text-accent-red disabled:opacity-60"
            >
              Cancel
            </button>
          </>
        )}

        {request.status === RideRequestStatus.START_PENDING_CONFIRMATION && (
          <p className="text-xs text-ink-faint">Waiting for passenger to confirm start.</p>
        )}

        {request.status === RideRequestStatus.ONGOING && (
          <>
            <a
              href={`tel:${request.passengerPhone}`}
              className="flex-1 rounded-full border border-line-strong px-3 py-1.5 text-center text-xs font-medium"
            >
              Call
            </a>
            <button
              onClick={onComplete}
              disabled={isProcessing}
              className="flex-1 rounded-full bg-accent px-3 py-1.5 text-xs font-semibold text-accent-ink disabled:opacity-60"
            >
              Trip completed
            </button>
          </>
        )}

        {request.status === RideRequestStatus.END_PENDING_CONFIRMATION && (
          <p className="text-xs text-ink-faint">Waiting for passenger to confirm arrival.</p>
        )}

        {request.status === RideRequestStatus.COMPLETED && !request.passengerRated && (
          <button
            onClick={onRate}
            className="flex-1 rounded-full bg-accent px-3 py-1.5 text-xs font-semibold text-accent-ink"
          >
            Rate passenger
          </button>
        )}
      </div>
    </div>
  );
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

function SeatField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number;
  onChange: (v: number) => void;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
      >
        {[1, 2, 3, 4].map((n) => (
          <option key={n} value={n}>
            {n}
          </option>
        ))}
      </select>
    </div>
  );
}
