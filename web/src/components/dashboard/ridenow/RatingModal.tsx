"use client";

import { useState } from "react";

export default function RatingModal({
  subject,
  onDismiss,
  onSubmit,
}: {
  subject: string;
  onDismiss: () => void;
  onSubmit: (stars: number, comment: string) => void;
}) {
  const [stars, setStars] = useState(5);
  const [comment, setComment] = useState("");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-6">
      <div className="w-full max-w-sm rounded-2xl border border-line bg-background p-6">
        <h2 className="text-lg font-semibold">Rate {subject}</h2>

        <div className="mt-4 flex gap-2">
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              onClick={() => setStars(n)}
              className={`text-2xl ${n <= stars ? "text-accent-amber" : "text-ink-faint"}`}
              aria-label={`${n} star${n > 1 ? "s" : ""}`}
            >
              ★
            </button>
          ))}
        </div>

        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Optional comment"
          rows={3}
          className="mt-4 w-full rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
        />

        <div className="mt-5 flex gap-3">
          <button
            onClick={() => onSubmit(stars, comment)}
            className="flex-1 rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-ink hover:opacity-90"
          >
            Submit
          </button>
          <button
            onClick={onDismiss}
            className="rounded-full border border-line-strong px-4 py-2 text-sm font-medium"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
