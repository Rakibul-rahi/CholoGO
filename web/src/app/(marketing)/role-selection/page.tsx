"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { completeProfile, readPendingSignup } from "@/lib/auth";

type Role = "passenger" | "rider";

const VEHICLE_TYPES = [
  { value: "bike", label: "Bike" },
  { value: "car", label: "Car" },
];

export default function RoleSelectionPage() {
  const router = useRouter();
  const { firebaseUser } = useAuth();
  const [role, setRole] = useState<Role>("passenger");
  const [vehicleType, setVehicleType] = useState("bike");
  const [vehicleModel, setVehicleModel] = useState("");
  const [vehicleNumber, setVehicleNumber] = useState("");
  const [vehicleColor, setVehicleColor] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!firebaseUser) return;

    setError(null);
    setIsSubmitting(true);

    const pending = readPendingSignup();

    try {
      await completeProfile({
        uid: firebaseUser.uid,
        name: pending?.name ?? firebaseUser.displayName ?? "",
        email: pending?.email ?? firebaseUser.email ?? "",
        phone: pending?.phone ?? "",
        studentId: pending?.studentId ?? "",
        university: pending?.university ?? "AUST",
        role,
        vehicleType: role === "rider" ? vehicleType : "",
        vehicleModel: role === "rider" ? vehicleModel : "",
        vehicleNumber: role === "rider" ? vehicleNumber : "",
        vehicleColor: role === "rider" ? vehicleColor : "",
      });
      router.push("/dashboard");
    } catch {
      setError("Couldn't save your profile. Please try again.");
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-6 px-6 py-24">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          How will you ride?
        </h1>
        <p className="mt-1 text-sm text-ink-soft">
          You can switch vehicle details later from your profile.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3">
          <RoleOption
            label="Passenger"
            description="Book a seat"
            active={role === "passenger"}
            onClick={() => setRole("passenger")}
          />
          <RoleOption
            label="Rider"
            description="Offer a ride"
            active={role === "rider"}
            onClick={() => setRole("rider")}
          />
        </div>

        {role === "rider" && (
          <div className="flex flex-col gap-4 rounded-xl border border-line p-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="vehicleType" className="text-sm font-medium">
                Vehicle type
              </label>
              <select
                id="vehicleType"
                value={vehicleType}
                onChange={(e) => setVehicleType(e.target.value)}
                className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
              >
                {VEHICLE_TYPES.map((v) => (
                  <option key={v.value} value={v.value}>
                    {v.label}
                  </option>
                ))}
              </select>
            </div>

            <Field
              label="Model"
              id="vehicleModel"
              value={vehicleModel}
              onChange={setVehicleModel}
            />
            <Field
              label="Number plate"
              id="vehicleNumber"
              value={vehicleNumber}
              onChange={setVehicleNumber}
            />
            <Field
              label="Color"
              id="vehicleColor"
              value={vehicleColor}
              onChange={setVehicleColor}
            />
          </div>
        )}

        {error && <p className="text-sm text-accent-red">{error}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="mt-2 rounded-full bg-accent px-4 py-2.5 text-sm font-semibold text-accent-ink transition-colors hover:opacity-90 disabled:opacity-60"
        >
          {isSubmitting ? "Saving..." : "Continue"}
        </button>
      </form>
    </div>
  );
}

function RoleOption({
  label,
  description,
  active,
  onClick,
}: {
  label: string;
  description: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex flex-col items-center gap-1 rounded-xl border px-4 py-5 text-center transition-colors ${
        active
          ? "border-accent bg-accent-soft"
          : "border-line-strong"
      }`}
    >
      <span className="text-sm font-semibold">{label}</span>
      <span className="text-xs text-ink-soft">
        {description}
      </span>
    </button>
  );
}

function Field({
  label,
  id,
  value,
  onChange,
}: {
  label: string;
  id: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-lg border border-line-strong bg-transparent px-3 py-2 text-sm outline-none focus:border-accent"
      />
    </div>
  );
}
