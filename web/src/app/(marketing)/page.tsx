import Image from "next/image";
import Link from "next/link";

export default function Home() {
  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col items-center justify-center gap-8 px-6 py-24 text-center">
      <div className="relative">
        <div
          aria-hidden
          className="absolute inset-0 -z-10 scale-150 rounded-full bg-accent opacity-20 blur-3xl"
        />
        <Image
          src="/logo.png"
          alt="CholoGO"
          width={128}
          height={128}
          priority
          className="rounded-3xl shadow-2xl"
        />
      </div>

      <h1 className="max-w-2xl text-4xl font-semibold tracking-tight sm:text-5xl">
        Rides on your schedule, drivers you can trust.
      </h1>
      <p className="max-w-xl text-lg text-ink-soft">
        CholoGO connects passengers and drivers for rides right now or booked
        ahead for tomorrow — with transparent pricing and a trip history you
        can rely on.
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
    </div>
  );
}
