"use client";

import { useState } from "react";
import { needsMissedRideReview, type RideRequest } from "@/lib/tomorrow";

export default function MissedRideReview({
  legs,
  isRider,
  onAnswer,
}: {
  legs: RideRequest[];
  isRider: boolean;
  onAnswer: (request: RideRequest, didHappen: boolean) => void;
}) {
  const [processingId, setProcessingId] = useState<string | null>(null);
  const overdue = legs.filter((leg) => needsMissedRideReview(leg));

  if (overdue.length === 0) return null;

  return (
    <div className="flex flex-col gap-3">
      {overdue.map((leg) => (
        <div
          key={leg.requestId}
          className="rounded-2xl border border-accent-amber/30 bg-accent-amber/5 p-5"
        >
          <h3 className="text-sm font-semibold">Did this ride happen?</h3>
          <p className="mt-1 text-sm text-ink-soft">
            {leg.pickup} → {leg.destination} on {leg.rideDate} at {leg.tripTime}. Its
            scheduled lifecycle was never finished — let us know if it actually
            happened.
          </p>
          <div className="mt-3 flex gap-3">
            <button
              disabled={processingId === leg.requestId}
              onClick={async () => {
                setProcessingId(leg.requestId);
                await onAnswer(leg, true);
                setProcessingId(null);
              }}
              className="flex-1 rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-ink disabled:opacity-60"
            >
              Yes, it happened
            </button>
            <button
              disabled={processingId === leg.requestId}
              onClick={async () => {
                setProcessingId(leg.requestId);
                await onAnswer(leg, false);
                setProcessingId(null);
              }}
              className="flex-1 rounded-full border border-line-strong px-4 py-2 text-sm font-medium disabled:opacity-60"
            >
              No, it didn&apos;t
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
