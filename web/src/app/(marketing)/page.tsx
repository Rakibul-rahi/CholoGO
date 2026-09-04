import Image from "next/image";
import Link from "next/link";

const stats = [
  { label: "Status", value: "Live" },
  { label: "Campus", value: "AUST, Dhaka" },
  { label: "Fares", value: "Below ride-hailing" },
];

const problems = [
  {
    icon: "💸",
    n: "01",
    title: "High fare costs",
    body: "Ride-hailing apps stack surge pricing, commissions, and service fees onto every trip — expensive for students on a tight budget.",
  },
  {
    icon: "🏍️",
    n: "02",
    title: "Wasted seats & fuel",
    body: "Hundreds of students with bikes and cars make the same campus trip alone every day, paying for fuel on a ride they were already taking.",
  },
  {
    icon: "🚦",
    n: "03",
    title: "Traffic & congestion",
    body: "More lone vehicles on the road means more congestion around campus gates — shared rides cut that directly.",
  },
  {
    icon: "🤝",
    n: "04",
    title: "No trusted platform",
    body: "Students already share rides informally, but there's no structured, verified way to find and confirm one.",
  },
];

const steps = [
  {
    n: "1",
    title: "Pick Ride Now or Tomorrow",
    body: "Riders go live instantly for an on-demand pickup, or set tomorrow's route and time the night before.",
  },
  {
    n: "2",
    title: "Matched by route & time",
    body: "CholoGO matches passengers to nearby live riders, or to tomorrow's saved routes — same direction, close departure time.",
  },
  {
    n: "3",
    title: "Confirm and go",
    body: "Both sides see pickup point, time, and a verified profile before the trip starts — no back-and-forth.",
  },
  {
    n: "4",
    title: "Rate, earn XP, repeat",
    body: "Every completed trip is confirmed by both riders and passengers, then logged to an evidence-backed XP ledger and ride history.",
  },
];

const forRiders = [
  "Earn back fuel costs on a trip you're already making",
  "Small extra income without becoming a full-time driver",
  "Build a rating and XP history on campus",
  "Go live only when you want to — no pressure",
];

const forPassengers = [
  "Pay less than typical ride-hailing apps",
  "Ride with a verified fellow student, not a stranger",
  "Plan tomorrow's commute the night before",
  "Simple accept/confirm flow, no surge pricing",
];

export default function Home() {
  return (
    <div className="flex flex-1 flex-col">
      {/* Hero */}
      <section className="mx-auto flex w-full max-w-5xl flex-col items-center gap-8 px-6 py-24 text-center">
        <span className="rounded-full border border-line-strong px-4 py-1.5 text-xs font-semibold tracking-widest text-accent uppercase">
          Live at AUST · 2026
        </span>

        <div className="relative flex flex-col items-center gap-3">
          <div
            aria-hidden
            className="absolute inset-0 -z-10 scale-150 rounded-full bg-accent opacity-20 blur-3xl"
          />
          <div className="flex items-center gap-3">
            <Image
              src="/logo.png"
              alt=""
              width={64}
              height={64}
              priority
              className="rounded-2xl shadow-2xl"
            />
            <h1 className="text-5xl font-extrabold tracking-tight sm:text-6xl">
              Cholo<span className="text-accent">GO</span>
            </h1>
          </div>
          <div className="h-1 w-16 rounded-full bg-accent" />
        </div>

        <p className="max-w-xl text-lg text-ink-soft">
          A student-to-student ride-sharing platform for AUST commuters —
          request a ride right now, or plan tomorrow&apos;s trip the night
          before, with a rider you can actually trust.
        </p>

        <div className="flex flex-col gap-4 sm:flex-row">
          <Link
            href="/download"
            className="rounded-full bg-accent px-6 py-3 text-base font-medium text-accent-ink transition-colors hover:opacity-90"
          >
            Download the app
          </Link>
          <Link
            href="/features"
            className="rounded-full border border-line px-6 py-3 text-base font-medium transition-colors hover:bg-card-elevated"
          >
            See how it works
          </Link>
        </div>

        <div className="mt-4 flex flex-wrap items-center justify-center gap-x-10 gap-y-4">
          {stats.map((s) => (
            <div key={s.label} className="text-left">
              <p className="text-xs font-semibold tracking-widest text-ink-faint uppercase">
                {s.label}
              </p>
              <p className="text-sm font-semibold">{s.value}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Problem */}
      <section className="border-t border-line px-6 py-20">
        <div className="mx-auto max-w-5xl">
          <p className="text-xs font-semibold tracking-widest text-accent uppercase">
            The problem
          </p>
          <h2 className="mt-2 max-w-xl text-3xl font-semibold tracking-tight sm:text-4xl">
            Getting to campus is expensive & inefficient
          </h2>
          <p className="mt-3 max-w-xl text-ink-soft">
            Students face high ride costs from commercial apps, while fellow
            students with bikes and cars make the same trip alone every day.
          </p>

          <div className="mt-10 grid gap-5 sm:grid-cols-2">
            {problems.map((p) => (
              <div
                key={p.n}
                className="rounded-2xl border border-line bg-card p-6"
              >
                <div className="flex items-center justify-between">
                  <span className="text-2xl">{p.icon}</span>
                  <span className="text-xs font-semibold text-ink-faint">
                    {p.n}
                  </span>
                </div>
                <h3 className="mt-4 text-base font-semibold">{p.title}</h3>
                <p className="mt-2 text-sm text-ink-soft">{p.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Solution */}
      <section className="border-t border-line px-6 py-20">
        <div className="mx-auto max-w-5xl">
          <p className="text-xs font-semibold tracking-widest text-accent uppercase">
            The solution
          </p>
          <h2 className="mt-2 max-w-xl text-3xl font-semibold tracking-tight sm:text-4xl">
            Ride together, pay less, earn more
          </h2>
          <p className="mt-3 max-w-xl text-ink-soft">
            CholoGO connects student riders with fellow students heading the
            same way — no commission taken from either side.
          </p>

          <div className="mt-10 grid gap-8 sm:grid-cols-2">
            {steps.map((s) => (
              <div key={s.n} className="flex gap-4">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-accent-soft text-sm font-bold text-accent">
                  {s.n}
                </span>
                <div>
                  <h3 className="text-base font-semibold">{s.title}</h3>
                  <p className="mt-1 text-sm text-ink-soft">{s.body}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Why it works */}
      <section className="border-t border-line px-6 py-20">
        <div className="mx-auto max-w-5xl">
          <p className="text-xs font-semibold tracking-widest text-accent uppercase">
            Why it works
          </p>
          <h2 className="mt-2 max-w-xl text-3xl font-semibold tracking-tight sm:text-4xl">
            Built for both sides of the ride
          </h2>

          <div className="mt-10 grid gap-6 sm:grid-cols-2">
            <div className="rounded-2xl border border-line bg-card p-6">
              <h3 className="text-base font-semibold">
                🏍️ For riders
              </h3>
              <ul className="mt-4 flex flex-col gap-3">
                {forRiders.map((item) => (
                  <li key={item} className="flex gap-2.5 text-sm text-ink-soft">
                    <span className="text-accent">•</span>
                    {item}
                  </li>
                ))}
              </ul>
            </div>

            <div className="rounded-2xl border border-line bg-card p-6">
              <h3 className="text-base font-semibold">
                🎒 For passengers
              </h3>
              <ul className="mt-4 flex flex-col gap-3">
                {forPassengers.map((item) => (
                  <li key={item} className="flex gap-2.5 text-sm text-ink-soft">
                    <span className="text-accent">•</span>
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="border-t border-line px-6 py-20">
        <div className="mx-auto flex max-w-5xl flex-col items-center gap-6 rounded-3xl border border-line bg-card px-6 py-14 text-center">
          <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">
            Ready to ride?
          </h2>
          <p className="max-w-md text-ink-soft">
            Sign up as a passenger or a rider and get matched with your
            campus community.
          </p>
          <Link
            href="/download"
            className="rounded-full bg-accent px-6 py-3 text-base font-medium text-accent-ink transition-colors hover:opacity-90"
          >
            Download the app
          </Link>
        </div>
      </section>
    </div>
  );
}
