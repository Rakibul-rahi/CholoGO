"use client";

import { useEffect, useState } from "react";
import {
  RideNowStatus,
  cancelAcceptedRideNowTrip,
  cancelRideNowRequest,
  closeAbandonedPassengerRequests,
  createRideNowRequest,
  listenPassengerActiveRide,
  passengerConfirmRideNowCompleted,
  passengerConfirmRideNowStarted,
  passengerRejectRideNowStarted,
  submitRideRating,
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
import RatingModal from "./RatingModal";

export default function PassengerRideNow({
  passengerId,
  passengerName,
  passengerPhone,
}: {
  passengerId: string;
  passengerName: string;
  passengerPhone: string;
}) {
  const [activeRequest, setActiveRequest] = useState<RideNowRequest | null>(null);
  const [completedRequest, setCompletedRequest] = useState<RideNowRequest | null>(null);

  const [pickup, setPickup] = useState("Mirpur 12");
  const [destination, setDestination] = useState("AUST Gate");

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showRating, setShowRating] = useState(false);

  useEffect(() => {
    let unsub: (() => void) | undefined;
    closeAbandonedPassengerRequests(passengerId).finally(() => {
      unsub = listenPassengerActiveRide(passengerId, (request) => {
        setActiveRequest(request);
        if (request) setCompletedRequest(null);
      });
    });
    claimRideNowTripXp(passengerId, false);
    return () => unsub?.();
  }, [passengerId]);

  const request = completedRequest ?? activeRequest;

  async function submitRequest() {
    if (pickup === destination) {
      setError("Pickup and destination cannot be the same.");
      return;
    }
    setError(null);
    setIsLoading(true);
    try {
      await createRideNowRequest({
        passengerId,
        passengerName: passengerName || "Passenger",
        passengerPhone,
        pickup,
        destination,
        tripTime: "Now",
        timeMinutes: 0,
        routeKey: buildRouteKey(tripDirectionFor(destination), pickup, destination),
      });
    } catch (err) {
      setError(errorMessage(err, "Failed to create request."));
    } finally {
      setIsLoading(false);
    }
  }

  async function cancel() {
    if (!request) return;
    setIsLoading(true);
    setError(null);
    try {
      if (request.status !== RideNowStatus.SEARCHING && request.matchedRideId) {
        await cancelAcceptedRideNowTrip(request.requestId, request.matchedRideId);
      } else {
        await cancelRideNowRequest(request.requestId);
      }
    } catch (err) {
      setError(errorMessage(err, "Failed to cancel request."));
    } finally {
      setIsLoading(false);
    }
  }

  async function confirmStarted() {
    if (!request) return;
    setIsLoading(true);
    try {
      await passengerConfirmRideNowStarted(request.requestId);
    } catch (err) {
      setError(errorMessage(err, "Failed to confirm start."));
    } finally {
      setIsLoading(false);
    }
  }

  async function rejectStarted() {
    if (!request) return;
    setIsLoading(true);
    try {
      await passengerRejectRideNowStarted(request.requestId);
    } catch (err) {
      setError(errorMessage(err, "Failed to reject start."));
    } finally {
      setIsLoading(false);
    }
  }

  async function confirmCompleted() {
    if (!request) return;
    setIsLoading(true);
    try {
      await passengerConfirmRideNowCompleted(request.requestId, request.matchedRideId);
      setCompletedRequest({ ...request, status: RideNowStatus.COMPLETED });
    } catch (err) {
      setError(errorMessage(err, "Failed to confirm completion."));
    } finally {
      setIsLoading(false);
    }
  }

  async function rate(stars: number, comment: string) {
    if (!request) return;
    try {
      await submitRideRating({
        requestId: request.requestId,
        ratedBy: passengerId,
        ratedTo: request.matchedRiderId,
        stars,
        comment,
      });
      setShowRating(false);
      claimRideNowTripXp(passengerId, false);
    } catch (err) {
      setError(errorMessage(err, "Failed to submit rating."));
    }
  }

  if (request && request.status !== RideNowStatus.CANCELLED && request.status !== RideNowStatus.EXPIRED) {
    return (
      <div className="flex flex-col gap-4">
        <StatusCard request={request} isPassenger />

        {error && <p className="text-sm text-accent-red">{error}</p>}

        {(request.status === RideNowStatus.SEARCHING ||
          request.status === RideNowStatus.NOTIFIED) && (
          <ActionButton loading={isLoading} onClick={cancel} variant="danger">
            Cancel request
          </ActionButton>
        )}

        {request.status === RideNowStatus.ACCEPTED && (
          <div className="flex gap-3">
            <a
              href={`tel:${request.matchedRiderPhone}`}
              className="flex-1 rounded-full border border-line-strong px-4 py-2 text-center text-sm font-medium"
            >
              Call rider
            </a>
            <ActionButton loading={isLoading} onClick={cancel} variant="danger">
              Cancel ride
            </ActionButton>
          </div>
        )}

        {request.status === RideNowStatus.START_PENDING_CONFIRMATION && (
          <div className="flex gap-3">
            <ActionButton loading={isLoading} onClick={confirmStarted}>
              Confirm started
            </ActionButton>
            <ActionButton loading={isLoading} onClick={rejectStarted} variant="secondary">
              Not yet
            </ActionButton>
          </div>
        )}

        {request.status === RideNowStatus.ONGOING && (
          <a
            href={`tel:${request.matchedRiderPhone}`}
            className="rounded-full border border-line-strong px-4 py-2 text-center text-sm font-medium"
          >
            Call rider
          </a>
        )}

        {request.status === RideNowStatus.END_PENDING_CONFIRMATION && (
          <ActionButton loading={isLoading} onClick={confirmCompleted}>
            Confirm safe arrival
          </ActionButton>
        )}

        {request.status === RideNowStatus.COMPLETED && (
          <div className="flex gap-3">
            {!request.riderRated && (
              <ActionButton loading={false} onClick={() => setShowRating(true)}>
                Rate rider
              </ActionButton>
            )}
            <ActionButton
              loading={false}
              variant="secondary"
              onClick={() => setCompletedRequest(null)}
            >
              Find another ride
            </ActionButton>
          </div>
        )}

        {showRating && request.matchedRiderId && (
          <RatingModal subject="rider" onDismiss={() => setShowRating(false)} onSubmit={rate} />
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-line p-5">
      <h2 className="text-base font-semibold">Find an instant ride</h2>

      <LocationField label="Pickup" value={pickup} onChange={setPickup} exclude={destination} />
      <LocationField
        label="Destination"
        value={destination}
        onChange={setDestination}
        exclude={pickup}
      />

      {error && <p className="text-sm text-accent-red">{error}</p>}

      <ActionButton loading={isLoading} onClick={submitRequest}>
        Find Ride Now
      </ActionButton>
    </div>
  );
}

function LocationField({
  label,
  value,
  onChange,
  exclude,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  exclude: string;
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

export function ActionButton({
  children,
  onClick,
  loading,
  variant = "primary",
}: {
  children: React.ReactNode;
  onClick: () => void;
  loading: boolean;
  variant?: "primary" | "secondary" | "danger";
}) {
  const styles = {
    primary: "bg-accent text-accent-ink hover:opacity-90",
    secondary: "border border-line-strong",
    danger: "border border-accent-red/30 text-accent-red hover:bg-accent-red/10",
  }[variant];

  return (
    <button
      onClick={onClick}
      disabled={loading}
      className={`flex-1 rounded-full px-4 py-2 text-sm font-semibold disabled:opacity-60 ${styles}`}
    >
      {loading ? "Working..." : children}
    </button>
  );
}

export function StatusCard({
  request,
  isPassenger,
}: {
  request: RideNowRequest;
  isPassenger: boolean;
}) {
  return (
    <div className="rounded-2xl border border-line p-5">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold">
          {request.pickup} → {request.destination}
        </span>
        <span className="rounded-full bg-surface px-3 py-1 text-xs font-medium">
          {statusLabel(request.status)}
        </span>
      </div>

      {isPassenger && request.matchedRiderName && (
        <div className="mt-3 flex flex-col gap-1 text-sm text-ink-soft">
          <p>
            Rider: {request.matchedRiderName}
            {request.matchedVehicleModel ? ` · ${request.matchedVehicleModel}` : ""}
            {request.matchedVehicleNumber ? ` · ${request.matchedVehicleNumber}` : ""}
          </p>
          {request.matchedRiderPhone && (
            <p>
              Phone:{" "}
              <a href={`tel:${request.matchedRiderPhone}`} className="font-medium text-accent">
                {request.matchedRiderPhone}
              </a>
            </p>
          )}
        </div>
      )}

      {!isPassenger && request.passengerName && (
        <div className="mt-3 flex flex-col gap-1 text-sm text-ink-soft">
          <p>Passenger: {request.passengerName}</p>
          {request.passengerPhone && (
            <p>
              Phone:{" "}
              <a href={`tel:${request.passengerPhone}`} className="font-medium text-accent">
                {request.passengerPhone}
              </a>
            </p>
          )}
        </div>
      )}
    </div>
  );
}
