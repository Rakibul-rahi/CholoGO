export default function ContactPage() {
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
    </div>
  );
}
