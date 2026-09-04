"use client";

import { useState } from "react";

const TABS = ["Ride Now", "Tomorrow"] as const;

export default function RideTabs({
  rideNow,
  tomorrow,
}: {
  rideNow: React.ReactNode;
  tomorrow: React.ReactNode;
}) {
  const [active, setActive] = useState<(typeof TABS)[number]>("Ride Now");

  return (
    <div className="mt-6">
      <div className="flex gap-2 border-b border-line">
        {TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActive(tab)}
            className={`px-4 py-2 text-sm font-medium ${
              active === tab
                ? "border-b-2 border-accent text-accent"
                : "text-ink-faint"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className="pt-6">{active === "Ride Now" ? rideNow : tomorrow}</div>
    </div>
  );
}
