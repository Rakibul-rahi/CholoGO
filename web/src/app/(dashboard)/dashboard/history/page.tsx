"use client";

import { useEffect, useState } from "react";
import { collection, onSnapshot, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";

interface HistoryEntry {
  historyId: string;
  rideType: string;
  passengerName: string;
  riderName: string;
  pickup: string;
  destination: string;
  tripTime: string;
  completedAt?: { seconds: number } | null;
}

export default function RideHistoryPage() {
  const { profile } = useAuth();
  const [entries, setEntries] = useState<HistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!profile) return;

    const field = profile.role === "rider" ? "riderId" : "passengerId";
    const historyQuery = query(
      collection(db, "ride_history"),
      where(field, "==", profile.uid)
    );

    setLoading(true);
    return onSnapshot(historyQuery, (snapshot) => {
      const rows = snapshot.docs.map((docSnap) => ({
        historyId: docSnap.id,
        ...(docSnap.data() as Omit<HistoryEntry, "historyId">),
      }));
      rows.sort(
        (a, b) => (b.completedAt?.seconds ?? 0) - (a.completedAt?.seconds ?? 0)
      );
      setEntries(rows);
      setLoading(false);
    });
  }, [profile]);

  if (!profile) return null;

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Ride history</h1>

      {loading ? (
        <p className="mt-6 text-sm text-ink-soft">
          Loading...
        </p>
      ) : entries.length === 0 ? (
        <p className="mt-6 text-sm text-ink-soft">
          No completed rides yet.
        </p>
      ) : (
        <div className="mt-6 flex flex-col gap-3">
          {entries.map((entry) => (
            <div
              key={entry.historyId}
              className="rounded-xl border border-line p-4"
            >
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold">
                  {entry.pickup} → {entry.destination}
                </span>
                <span className="text-xs uppercase text-ink-faint">
                  {entry.rideType === "ride_now" ? "Ride Now" : "Tomorrow"}
                </span>
              </div>
              <p className="mt-1 text-sm text-ink-soft">
                {profile.role === "rider"
                  ? `Passenger: ${entry.passengerName}`
                  : `Rider: ${entry.riderName}`}{" "}
                · {entry.tripTime}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
