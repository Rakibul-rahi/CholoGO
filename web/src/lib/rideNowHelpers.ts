export const availableLocations = [
  "Lalbag",
  "Mirpur 12",
  "Mirpur 11",
  "Mirpur 10",
  "Kazipara",
  "Taltola",
  "Agargoan",
  "Mohammadpur",
  "Khilgaon",
  "Dhanmondi",
  "Jigatola",
  "Azimpur",
  "Gulshan Link Road",
  "Kakrail Mor",
  "AUST Gate",
];

export function buildRouteKey(tripDirection: string, pickup: string, destination: string): string {
  return `${tripDirection.trim().toLowerCase()}|${pickup.trim().toLowerCase()}|${destination.trim().toLowerCase()}`;
}

export function tripDirectionFor(destination: string): "to_campus" | "to_home" {
  return destination.toLowerCase() === "aust gate" ? "to_campus" : "to_home";
}

export function formatTo12Hour(hour: number, minute: number): string {
  const period = hour >= 12 ? "PM" : "AM";
  const h12 = hour % 12 === 0 ? 12 : hour % 12;
  return `${h12}:${String(minute).padStart(2, "0")} ${period}`;
}

export function statusLabel(status: string): string {
  switch (status) {
    case "searching":
      return "Searching";
    case "notified":
      return "Notified";
    case "accepted":
      return "Accepted";
    case "start_pending_confirmation":
      return "Start Pending";
    case "ongoing":
      return "Ongoing";
    case "end_pending_confirmation":
      return "End Pending";
    case "completed":
      return "Completed";
    case "cancelled":
      return "Cancelled";
    case "expired":
      return "Expired";
    case "issue_reported":
      return "Issue Reported";
    case "unverified":
      return "Unverified";
    default:
      return status.charAt(0).toUpperCase() + status.slice(1);
  }
}

export function errorMessage(err: unknown, fallback: string): string {
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}
