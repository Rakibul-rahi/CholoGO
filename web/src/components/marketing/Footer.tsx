import Link from "next/link";

export default function Footer() {
  return (
    <footer className="border-t border-line">
      <div className="mx-auto flex max-w-5xl flex-col items-center justify-between gap-4 px-6 py-8 text-sm text-ink-soft sm:flex-row">
        <p>&copy; {new Date().getFullYear()} CholoGO. All rights reserved.</p>
        <div className="flex gap-6">
          <Link href="/privacy" className="hover:opacity-70">
            Privacy
          </Link>
          <Link href="/terms" className="hover:opacity-70">
            Terms
          </Link>
          <Link href="/contact" className="hover:opacity-70">
            Contact
          </Link>
        </div>
      </div>
    </footer>
  );
}
