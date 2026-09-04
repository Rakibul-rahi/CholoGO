"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { signUp } from "@/lib/auth";

function firebaseErrorMessage(err: unknown): string {
  const code = (err as { code?: string })?.code ?? "";
  if (code === "auth/email-already-in-use") {
    return "An account already exists with that email.";
  }
  if (code === "auth/weak-password") {
    return "Password should be at least 6 characters.";
  }
  if (code === "auth/invalid-email") {
    return "That email address looks invalid.";
  }
  return "Something went wrong creating your account. Please try again.";
}

export default function SignupPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [studentId, setStudentId] = useState("");
  const [university, setUniversity] = useState("AUST");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await signUp({ name, email, phone, studentId, university, password });
      router.push("/role-selection");
    } catch (err) {
      setError(firebaseErrorMessage(err));
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-6 px-6 py-24">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          Create account
        </h1>
        <p className="mt-1 text-sm text-ink-soft">
          Next you&apos;ll pick whether you&apos;re riding as a passenger or a
          driver.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Field label="Full name" id="name" value={name} onChange={setName} />
        <Field
          label="Email"
          id="email"
          type="email"
          value={email}
          onChange={setEmail}
        />
        <Field
          label="Phone"
          id="phone"
          type="tel"
          value={phone}
          onChange={setPhone}
        />
        <Field
          label="Student ID"
          id="studentId"
          value={studentId}
          onChange={setStudentId}
          required={false}
        />
        <Field
          label="University"
          id="university"
          value={university}
          onChange={setUniversity}
        />
        <Field
          label="Password"
          id="password"
          type="password"
          value={password}
          onChange={setPassword}
        />

        {error && <p className="text-sm text-accent-red">{error}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="mt-2 rounded-full bg-accent px-4 py-2.5 text-sm font-semibold text-accent-ink transition-colors hover:opacity-90 disabled:opacity-60"
        >
          {isSubmitting ? "Creating account..." : "Create Account"}
        </button>
      </form>

      <p className="text-center text-sm text-ink-soft">
        Already have an account?{" "}
        <Link href="/login" className="font-medium text-accent underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}

function Field({
  label,
  id,
  value,
  onChange,
  type = "text",
  required = true,
}: {
  label: string;
  id: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        type={type}
        required={required}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
      />
    </div>
  );
}
