import Image from "next/image";
import Link from "next/link";

const navLinks = [
  { href: "/features", label: "Features" },
  { href: "/download", label: "Download" },
  { href: "/contact", label: "Contact" },
];

export default function Header() {
  return (
    <header className="border-b border-line">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <Link href="/" className="flex items-center gap-2.5">
          <Image src="/logo.png" alt="" width={36} height={36} className="rounded-lg" />
          <span className="text-lg font-semibold tracking-tight">CholoGO</span>
        </Link>
        <nav className="flex items-center gap-6 text-sm font-medium">
          {navLinks.map((link) => (
            <Link key={link.href} href={link.href} className="hover:opacity-70">
              {link.label}
            </Link>
          ))}
          <Link
            href="/login"
            className="rounded-full bg-accent px-4 py-2 text-accent-ink transition-colors hover:opacity-90"
          >
            Sign in
          </Link>
        </nav>
      </div>
    </header>
  );
}
