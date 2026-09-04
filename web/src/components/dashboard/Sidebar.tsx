import Image from "next/image";
import Link from "next/link";

const navLinks = [
  { href: "/dashboard", label: "Overview" },
  { href: "/dashboard/history", label: "Ride History" },
  { href: "/dashboard/profile", label: "Profile" },
];

export default function Sidebar() {
  return (
    <aside className="w-56 shrink-0 border-r border-line px-4 py-8">
      <Link href="/" className="flex items-center gap-2 px-2">
        <Image src="/logo.png" alt="" width={30} height={30} className="rounded-lg" />
        <span className="text-lg font-semibold tracking-tight">CholoGO</span>
      </Link>
      <nav className="mt-8 flex flex-col gap-1 text-sm font-medium">
        {navLinks.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className="rounded-lg px-2 py-2 hover:bg-card-elevated"
          >
            {link.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
