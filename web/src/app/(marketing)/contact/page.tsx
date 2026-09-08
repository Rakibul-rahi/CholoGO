"use client";

import { useState, type FormEvent } from "react";
import { submitSuggestion } from "@/lib/suggestions";

export default function ContactPage() {
  const [message, setMessage] = useState("");
  const [name, setName] = useState("");
  const [contact, setContact] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await submitSuggestion({ message, name, contact });
      setSubmitted(true);
      setMessage("");
      setName("");
      setContact("");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to send your suggestion."
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-24">
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
        Contact us
      </h1>
      <p className="mt-4 text-ink-soft">
        Questions, feedback, or support requests — reach out and we&apos;ll
        get back to you.
      </p>
      <a
        href="mailto:rakibulislam.rahi.rir@gmail.com"
        className="mt-8 block text-lg font-medium text-accent hover:underline"
      >
        rakibulislam.rahi.rir@gmail.com
      </a>

      <div className="mt-16 border-t border-line pt-10">
        <h2 className="text-xl font-semibold tracking-tight">
          Suggestion box
        </h2>
        <p className="mt-2 text-sm text-ink-soft">
          Have an idea for CholoGO, or something that could work better? Drop
          it here — no account needed.
        </p>

        {submitted ? (
          <p className="mt-6 rounded-lg border border-accent-emerald/30 bg-accent-emerald/10 px-4 py-3 text-sm text-accent-emerald">
            Thanks — your suggestion has been sent.
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="suggestion" className="text-sm font-medium">
                Your suggestion
              </label>
              <textarea
                id="suggestion"
                required
                rows={5}
                maxLength={2000}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="suggesterName" className="text-sm font-medium">
                Name <span className="text-ink-faint">(optional)</span>
              </label>
              <input
                id="suggesterName"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="suggesterContact" className="text-sm font-medium">
                Email or phone{" "}
                <span className="text-ink-faint">
                  (optional, if you want a reply)
                </span>
              </label>
              <input
                id="suggesterContact"
                value={contact}
                onChange={(e) => setContact(e.target.value)}
                className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
              />
            </div>

            {error && <p className="text-sm text-accent-red">{error}</p>}

            <button
              type="submit"
              disabled={isSubmitting}
              className="mt-2 self-start rounded-full bg-accent px-5 py-2.5 text-sm font-semibold text-accent-ink transition-colors hover:opacity-90 disabled:opacity-60"
            >
              {isSubmitting ? "Sending..." : "Send suggestion"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
