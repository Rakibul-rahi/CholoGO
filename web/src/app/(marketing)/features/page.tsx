const features = [
  {
    title: "Ride Now",
    description:
      "Request a ride on demand and get matched with a nearby driver in real time.",
  },
  {
    title: "Tomorrow rides",
    description:
      "Schedule a ride ahead of time so a driver is ready to go when you need them.",
  },
  {
    title: "Evidence-backed XP",
    description:
      "Every trip is reconciled against ride records, so driver and passenger XP reflects rides that actually happened.",
  },
  {
    title: "Two-sided ratings",
    description:
      "Passengers and drivers rate each other after every trip, keeping the community accountable.",
  },
];

export default function FeaturesPage() {
  return (
    <div className="mx-auto w-full max-w-5xl flex-1 px-6 py-24">
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
        What CholoGO does
      </h1>
      <div className="mt-12 grid gap-8 sm:grid-cols-2">
        {features.map((feature) => (
          <div
            key={feature.title}
            className="rounded-2xl border border-line p-6"
          >
            <h2 className="text-lg font-semibold">{feature.title}</h2>
            <p className="mt-2 text-ink-soft">
              {feature.description}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
