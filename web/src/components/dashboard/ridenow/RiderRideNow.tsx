"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, limit, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import {
  RideNowStatus,
  acceptRideNowRequest,
  goLiveAsRider,
  listenForMatchingRequests,
  listenToLiveRide,
  listenToPassengerRequest,
  riderCancelUnstartedTrip,
  riderCloseUnconfirmedTrip,
  riderRequestRideNowCompletion,
  riderStartRideNowTrip,
  riderMayForceClose,
  stopLiveRide,
  submitPassengerRating,
  type LiveRide,
  type RideNowRequest,
} from "@/lib/rideNow";
import { claimRideNowTripXp } from "@/lib/xpClaim";
import {
  availableLocations,
  buildRouteKey,
  errorMessage,
  statusLabel,
  tripDirectionFor,
} from "@/lib/rideNowHelpers";
import { ActionButton, StatusCard } from "./PassengerRideNow";
import RatingModal from "./RatingModal";

export default function RiderRideNow({
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
  const [pickup, setPickup] = useState("AUST Gate");
  const [destination, setDestination] = useState("Dhanmondi");

  const [liveRideId, setLiveRideId] = useState<string | null>(null);
  const [liveRide, setLiveRide] = useState<LiveRide | null>(null);
  const [activeRequest, setActiveRequest] = useState<RideNowRequest | null>(null);
  const [completedRequest, setCompletedRequest] = useState<RideNowRequest | null>(null);
  const [availableRequests, setAvailableRequests] = useState<RideNowRequest[]>([]);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showRating, setShowRating] = useState(false);

  const request = completedRequest ?? activeRequest;
  const isLive = Boolean(liveRide?.liveNow && liveRide.status === "active");
  const hasLockedTrip = Boolean(
    request &&
      [
        RideNowStatus.ACCEPTED,
        RideNowStatus.START_PENDING_CONFIRMATION,
        RideNowStatus.ONGOING,
        RideNowStatus.END_PENDING_CONFIRMATION,
      ].includes(request.status as never)
  );

  // Restore an in-progress live session across a page reload.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const snapshot = await getDocs(
        query(
          collection(db, "live_rides"),
          where("riderId", "==", riderId),
          where("status", "==", "active"),
          limit(1)
        )
      );
      const first = snapshot.docs[0];
      if (first && !cancelled) setLiveRideId(first.id);
    })();
    claimRideNowTripXp(riderId, true);
    return () => {
      cancelled = true;
    };
  }, [riderId]);

  useEffect(() => {
    if (!liveRideId) {
      setLiveRide(null);
      return;
    }
    return listenToLiveRide(liveRideId, setLiveRide);
  }, [liveRideId]);

  useEffect(() => {
    const requestId = liveRide?.currentRequestId;
    if (!requestId) {
      setActiveRequest(null);
      return;
    }
    return listenToPassengerRequest(requestId, (req) => {
      setActiveRequest(req);
      if (req) setCompletedRequest(null);
    });
  }, [liveRide?.currentRequestId]);

  useEffect(() => {
    if (!isLive || hasLockedTrip || !liveRide) {
      setAvailableRequests([]);
      return;
    }
    return listenForMatchingRequests(liveRide.routeKey, setAvailableRequests);
  }, [isLive, hasLockedTrip, liveRide]);

  async function goLive() {
    if (pickup === destination) {
      setError("Pickup and destination cannot be the same.");
      return;
    }
    setError(null);
    setIsLoading(true);
    try {
      const tripDirection = tripDirectionFor(destination);
      const rideId = await goLiveAsRider({
        riderId,
        riderName: riderName || "Rider",
        pickup,
        destination,
        tripDirection,
        tripTime: "Now",
        timeMinutes: 0,
        routeKey: buildRouteKey(tripDirection, pickup, destination),
        availableSeats: 1,
        vehicleType,
        vehicleModel,
        vehicleNumber,
        vehicleColor,
      });
      setLiveRideId(rideId);
      setCompletedRequest(null);
    } catch (err) {
      setError(errorMessage(err, "Failed to go live."));
    } finally {
      setIsLoading(false);
    }
  }

  async function stopLive() {
    if (!liveRideId || hasLockedTrip) return;
    setIsLoading(true);
    setError(null);
    try {
      await stopLiveRide(liveRideId);
      setLiveRideId(null);
      setLiveRide(null);
      setAvailableRequests([]);
    } catch (err) {
      setError(errorMessage(err, "Failed to stop live ride."));
    } finally {
      setIsLoading(false);
    }
  }

  async function accept(req: RideNowRequest) {
    if (!liveRideId) return;
    setIsLoading(true);
    setError(null);
    try {
      await acceptRideNowRequest({
        requestId: req.requestId,
        liveRideId,
        riderId,
        riderName: riderName || "Rider",
        riderPhone: riderPhone || "N/A",
      });
    } catch (err) {
      setError(errorMessage(err, "Failed to accept request."));
    } finally {
      setIsLoading(false);
    }
  }

  async function startTrip() {
    if (!request) return;
    setIsLoading(true);
    try {
      await riderStartRideNowTrip(request.requestId);
    } catch (err) {
      setError(errorMessage(err, "Failed to start trip."));
    } finally {
      setIsLoading(false);
    }
  }

  async function completeTrip() {
    if (!request) return;
    setIsLoading(true);
    try {
      await riderRequestRideNowCompletion(request.requestId);
    } catch (err) {
      setError(errorMessage(err, "Failed to complete trip."));
    } finally {
      setIsLoading(false);
    }
  }

  async function cancelUnstarted() {
    if (!request) return;
    setIsLoading(true);
    try {
      await riderCancelUnstartedTrip(request.requestId, riderId);
      setActiveRequest(null);
    } catch (err) {
      setError(errorMessage(err, "Failed to cancel trip."));
    } finally {
      setIsLoading(false);
    }
  }

  async function closeUnconfirmed() {
    if (!request) return;
    setIsLoading(true);
    try {
      await riderCloseUnconfirmedTrip(request.requestId, riderId);
      setActiveRequest(null);
    } catch (err) {
      setError(errorMessage(err, "Failed to close trip."));
    } finally {
      setIsLoading(false);
    }
  }

  async function rate(stars: number, comment: string) {
    if (!request) return;
    try {
      await submitPassengerRating({
        requestId: request.requestId,
        ratedBy: riderId,
        ratedTo: request.passengerId,
        stars,
        comment,
      });
      setShowRating(false);
      claimRideNowTripXp(riderId, true);
    } catch (err) {
      setError(errorMessage(err, "Failed to submit rating."));
    }
  }

  const canForceClose = request ? riderMayForceClose(request, Math.floor(Date.now() / 1000)) : false;
  const hasStarted =
    request?.status === RideNowStatus.ONGOING ||
    request?.status === RideNowStatus.END_PENDING_CONFIRMATION;

  return (
    <div className="flex flex-col gap-5">
      <div className="rounded-2xl border border-line p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold">Go live</h2>
          <span
            className={`rounded-full px-3 py-1 text-xs font-medium ${
              isLive
                ? "bg-accent-emerald/10 text-accent-emerald"
                : "bg-surface"
            }`}
          >
            {isLive ? "Live" : "Offline"}
          </span>
        </div>

        <div className="mt-4 flex flex-col gap-3">
          <LocationField label="Pickup" value={pickup} onChange={setPickup} exclude={destination} disabled={isLive} />
          <LocationField
            label="Destination"
            value={destination}
            onChange={setDestination}
            exclude={pickup}
            disabled={isLive}
          />
        </div>

        {error && <p className="mt-3 text-sm text-accent-red">{error}</p>}

        <div className="mt-4 flex gap-3">
          <ActionButton loading={isLoading} onClick={goLive}>
            {isLive ? "You are live" : "Go Live"}
          </ActionButton>
          <ActionButton
            loading={isLoading}
            variant="secondary"
            onClick={stopLive}
          >
            {hasLockedTrip ? "Trip active" : "Stop Live"}
          </ActionButton>
        </div>
      </div>

      {request &&
        request.status !== RideNowStatus.CANCELLED &&
        request.status !== RideNowStatus.EXPIRED && (
          <div className="flex flex-col gap-4">
            <StatusCard request={request} isPassenger={false} />

            {request.status === RideNowStatus.ACCEPTED && (
              <div className="flex gap-3">
                <a
                  href={`tel:${request.passengerPhone}`}
                  className="flex-1 rounded-full border border-line-strong px-4 py-2 text-center text-sm font-medium"
                >
                  Call passenger
                </a>
                <ActionButton loading={isLoading} onClick={startTrip}>
                  Start trip
                </ActionButton>
              </div>
            )}

            {request.status === RideNowStatus.START_PENDING_CONFIRMATION && (
              <p className="text-sm text-ink-soft">
                Waiting for the passenger to confirm the trip started.
              </p>
            )}

            {request.status === RideNowStatus.ONGOING && (
              <div className="flex gap-3">
                <a
                  href={`tel:${request.passengerPhone}`}
                  className="flex-1 rounded-full border border-line-strong px-4 py-2 text-center text-sm font-medium"
                >
                  Call passenger
                </a>
                <ActionButton loading={isLoading} onClick={completeTrip}>
                  Trip completed
                </ActionButton>
              </div>
            )}

            {request.status === RideNowStatus.END_PENDING_CONFIRMATION && (
              <p className="text-sm text-ink-soft">
                Waiting for the passenger to confirm safe arrival.
              </p>
            )}

            {request.status === RideNowStatus.COMPLETED && !request.passengerRated && (
              <ActionButton loading={false} onClick={() => setShowRating(true)}>
                Rate passenger
              </ActionButton>
            )}

            {canForceClose && (
              <div className="rounded-xl border border-dashed border-line-strong p-4">
                <p className="text-sm text-ink-soft">
                  {hasStarted
                    ? "The passenger hasn't confirmed this trip. Closing it records it as unverified."
                    : "The passenger hasn't responded. You can cancel and go live again."}
                </p>
                <div className="mt-3">
                  <ActionButton
                    loading={isLoading}
                    variant="danger"
                    onClick={hasStarted ? closeUnconfirmed : cancelUnstarted}
                  >
                    {hasStarted ? "Close as unverified" : "Passenger didn't show"}
                  </ActionButton>
                </div>
              </div>
            )}

            {showRating && request.passengerId && (
              <RatingModal
                subject="passenger"
                onDismiss={() => setShowRating(false)}
                onSubmit={rate}
              />
            )}
          </div>
        )}

      {isLive && !hasLockedTrip && (
        <div className="rounded-2xl border border-line p-5">
          <h2 className="text-base font-semibold">Live passenger requests</h2>
          {availableRequests.length === 0 ? (
            <p className="mt-3 text-sm text-ink-soft">
              No live passenger requests for this route yet.
            </p>
          ) : (
            <div className="mt-4 flex flex-col gap-3">
              {availableRequests.map((req) => (
                <div
                  key={req.requestId}
                  className="rounded-xl border border-line p-4"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold">{req.passengerName}</span>
                    <span className="text-xs text-ink-faint">{statusLabel(req.status)}</span>
                  </div>
                  <p className="mt-1 text-sm text-ink-soft">
                    {req.pickup} → {req.destination}
                  </p>
                  <button
                    onClick={() => accept(req)}
                    disabled={isLoading}
                    className="mt-3 w-full rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-ink hover:opacity-90 disabled:opacity-60"
                  >
                    Accept request
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function LocationField({
  label,
  value,
  onChange,
  exclude,
  disabled,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  exclude: string;
  disabled: boolean;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium">{label}</label>
      <select
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent disabled:opacity-60"
      >
        {availableLocations
          .filter((loc) => loc !== exclude)
          .map((loc) => (
            <option key={loc} value={loc}>
              {loc}
            </option>
          ))}
      </select>
    </div>
  );
}
