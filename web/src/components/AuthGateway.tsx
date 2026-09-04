"use client";

import { useEffect, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

const REDIRECT_WHEN_SIGNED_IN = new Set(["/", "/login", "/signup", "/role-selection"]);

export default function AuthGateway({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (status === "loading") return;

    const onDashboard = pathname.startsWith("/dashboard");

    if (status === "signed-out" && onDashboard) {
      router.replace("/login");
      return;
    }

    if (status === "onboarding" && pathname !== "/role-selection") {
      router.replace("/role-selection");
      return;
    }

    if (status === "ready" && REDIRECT_WHEN_SIGNED_IN.has(pathname)) {
      router.replace("/dashboard");
    }
  }, [status, pathname, router]);

  return <>{children}</>;
}
