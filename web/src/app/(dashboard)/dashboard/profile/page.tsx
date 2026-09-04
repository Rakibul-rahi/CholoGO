"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { doc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";
import { signOutUser } from "@/lib/auth";

export default function DashboardProfilePage() {
  const { profile } = useAuth();
  const router = useRouter();

  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState(profile?.name ?? "");
  const [phone, setPhone] = useState(profile?.phone ?? "");
  const [homeLocation, setHomeLocation] = useState(profile?.homeLocation ?? "");
  const [vehicleModel, setVehicleModel] = useState(profile?.vehicleModel ?? "");
  const [vehicleNumber, setVehicleNumber] = useState(profile?.vehicleNumber ?? "");
  const [vehicleColor, setVehicleColor] = useState(profile?.vehicleColor ?? "");

  if (!profile) return null;

  const isRider = profile.role === "rider";

  const startEditing = () => {
    setName(profile.name);
    setPhone(profile.phone);
    setHomeLocation(profile.homeLocation);
    setVehicleModel(profile.vehicleModel);
    setVehicleNumber(profile.vehicleNumber);
    setVehicleColor(profile.vehicleColor);
    setIsEditing(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSaving(true);
    try {
      await updateDoc(doc(db, "users", profile.uid), {
        name,
        phone,
        homeLocation,
        ...(isRider ? { vehicleModel, vehicleNumber, vehicleColor } : {}),
      });
      setIsEditing(false);
    } catch {
      setError("Couldn't save your changes. Please try again.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleSignOut = async () => {
    await signOutUser();
    router.push("/");
  };

  return (
    <div className="max-w-lg">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Profile</h1>
        {!isEditing && (
          <button
            onClick={startEditing}
            className="rounded-full border border-line-strong px-4 py-1.5 text-sm font-medium transition-colors hover:bg-card-elevated "
          >
            Edit
          </button>
        )}
      </div>

      {!isEditing ? (
        <div className="mt-6 flex flex-col gap-4">
          <InfoRow label="Name" value={profile.name} />
          <InfoRow label="Email" value={profile.email} />
          <InfoRow label="Phone" value={profile.phone || "Not set"} />
          <InfoRow
            label="Role"
            value={isRider ? "Rider" : "Passenger"}
          />
          <InfoRow label="University" value={profile.university} />
          <InfoRow label="Student ID" value={profile.studentId || "Not set"} />
          <InfoRow label="Home location" value={profile.homeLocation || "Not set"} />

          {isRider && (
            <>
              <InfoRow
                label="Vehicle"
                value={
                  [profile.vehicleType, profile.vehicleModel]
                    .filter(Boolean)
                    .join(" · ") || "Not set"
                }
              />
              <InfoRow label="Plate" value={profile.vehicleNumber || "Not set"} />
              <InfoRow label="Color" value={profile.vehicleColor || "Not set"} />
            </>
          )}

          <InfoRow
            label="Rating"
            value={
              profile.ratingCount > 0
                ? `${profile.ratingAverage.toFixed(1)} / 5 (${profile.ratingCount})`
                : "No ratings yet"
            }
          />

          {isRider && (
            <InfoRow
              label="Completed rides"
              value={String(profile.completedRideCount)}
            />
          )}

          <button
            onClick={handleSignOut}
            className="mt-4 self-start rounded-full border border-accent-red/30 px-4 py-2 text-sm font-medium text-accent-red transition-colors hover:bg-accent-red/10"
          >
            Sign out
          </button>
        </div>
      ) : (
        <form onSubmit={handleSave} className="mt-6 flex flex-col gap-4">
          <Field label="Name" id="name" value={name} onChange={setName} />
          <Field label="Phone" id="phone" value={phone} onChange={setPhone} />
          <Field
            label="Home location"
            id="homeLocation"
            value={homeLocation}
            onChange={setHomeLocation}
          />

          {isRider && (
            <>
              <Field
                label="Vehicle model"
                id="vehicleModel"
                value={vehicleModel}
                onChange={setVehicleModel}
              />
              <Field
                label="Plate number"
                id="vehicleNumber"
                value={vehicleNumber}
                onChange={setVehicleNumber}
              />
              <Field
                label="Vehicle color"
                id="vehicleColor"
                value={vehicleColor}
                onChange={setVehicleColor}
              />
            </>
          )}

          {error && <p className="text-sm text-accent-red">{error}</p>}

          <div className="mt-2 flex gap-3">
            <button
              type="submit"
              disabled={isSaving}
              className="rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-ink transition-colors hover:opacity-90 disabled:opacity-60"
            >
              {isSaving ? "Saving..." : "Save"}
            </button>
            <button
              type="button"
              onClick={() => setIsEditing(false)}
              className="rounded-full border border-line-strong px-4 py-2 text-sm font-medium"
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between border-b border-line pb-3">
      <span className="text-sm text-ink-soft">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
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
