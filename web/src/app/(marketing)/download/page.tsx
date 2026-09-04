export default function DownloadPage() {
  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col items-center justify-center gap-6 px-6 py-24 text-center">
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
        Get the CholoGO app
      </h1>
      <p className="max-w-xl text-ink-soft">
        CholoGO is available for Android. A Play Store listing is coming
        soon — for now, download the APK directly.
      </p>
      <a
        href="https://drive.google.com/file/d/12pc1t325Io2liu_F3snUniLo0fumnY3-/view?usp=sharing"
        target="_blank"
        rel="noopener noreferrer"
        className="rounded-full bg-accent px-6 py-3 text-base font-medium text-accent-ink transition-colors hover:opacity-90"
      >
        Download for Android
      </a>
    </div>
  );
}
