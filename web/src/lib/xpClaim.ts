import { Timestamp, collection, doc, getDocs, limit, query, setDoc, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { RideNowStatus, type RideNowRequest } from "@/lib/rideNow";
import { RideRequestStatus, type RideRequest } from "@/lib/tomorrow";

const REASON_RIDE_NOW_TRIP = "ride_now_trip_completed";
const REASON_TOMORROW_TRIP = "tomorrow_trip_completed";
const REASON_PLAN_SAVED = "plan_saved";
const ROLE_RIDER = "rider";
const ROLE_PASSENGER = "passenger";
const TRIP_XP_RIDER = 12;
const TRIP_XP_PASSENGER = 8;
export const PLAN_SAVED_XP = 2;
const CLAIM_SWEEP_LIMIT = 60;

function amountFor(reason: string, role: string): number {
  if (reason === REASON_RIDE_NOW_TRIP || reason === REASON_TOMORROW_TRIP) {
    return role === ROLE_RIDER ? TRIP_XP_RIDER : TRIP_XP_PASSENGER;
  }
  if (reason === REASON_PLAN_SAVED) {
    return PLAN_SAVED_XP;
  }
  return 0;
}

function eventId(userId: string, reason: string, dedupeKey: string): string {
  return `${userId}__${reason}__${dedupeKey}`;
}

/** "yyyy-M-d" in UTC, unpadded — must match firestore.rules' derivation exactly. */
function utcDayKey(timestamp: Timestamp | null | undefined): string | null {
  if (!timestamp) return null;
  const date = timestamp.toDate();
  return `${date.getUTCFullYear()}-${date.getUTCMonth() + 1}-${date.getUTCDate()}`;
}

function rideNowTripDedupeKey(
  completedAt: Timestamp | null | undefined,
  routeKey: string
): string | null {
  const dayKey = utcDayKey(completedAt);
  if (!dayKey) return null;
  return `${dayKey}__${routeKey}`;
}

async function awardXp(params: {
  userId: string;
  reason: string;
  role: string;
  sourceId: string;
  dedupeKey: string;
}): Promise<boolean> {
  if (!params.userId || !params.sourceId || !params.dedupeKey) return false;

  const amount = amountFor(params.reason, params.role);
  if (amount <= 0) return false;

  const id = eventId(params.userId, params.reason, params.dedupeKey);

  try {
    await setDoc(doc(collection(db, "xp_events"), id), {
      eventId: id,
      userId: params.userId,
      reason: params.reason,
      role: params.role,
      sourceId: params.sourceId,
      amount,
      createdAt: Timestamp.now(),
    });
    return true;
  } catch {
    // Already exists (rules forbid updating an xp_events row), or the
    // evidence didn't hold up — both are expected, non-fatal outcomes.
    return false;
  }
}

/**
 * Offers every completed Ride Now trip this user was part of to the XP
 * ledger. Safe to call repeatedly: derived doc IDs make re-claiming a
 * no-op. Mirrors XpRepository.claimTripXpFor (Ride Now half only — Tomorrow
 * rides aren't on the web yet).
 */
export async function claimRideNowTripXp(userId: string, isRider: boolean): Promise<number> {
  if (!userId) return 0;

  const role = isRider ? ROLE_RIDER : ROLE_PASSENGER;
  const field = isRider ? "matchedRiderId" : "passengerId";

  let trips: RideNowRequest[];
  try {
    const snapshot = await getDocs(
      query(
        collection(db, "ride_now_requests"),
        where(field, "==", userId),
        where("status", "==", RideNowStatus.COMPLETED),
        limit(CLAIM_SWEEP_LIMIT)
      )
    );
    trips = snapshot.docs.map((d) => ({ ...(d.data() as object), requestId: d.id }) as RideNowRequest);
  } catch {
    return 0;
  }

  let awarded = 0;
  for (const trip of trips) {
    if (trip.issueReported) continue;

    const dedupeKey = rideNowTripDedupeKey(trip.completedAt, trip.routeKey);
    if (!dedupeKey) continue;

    const didAward = await awardXp({
      userId,
      reason: REASON_RIDE_NOW_TRIP,
      role,
      sourceId: trip.requestId,
      dedupeKey,
    });

    if (didAward) awarded++;
  }

  return awarded;
}

function tomorrowTripDedupeKey(rideDate: string, tripDirection: string): string | null {
  if (!rideDate || !tripDirection) return null;
  return `${rideDate}__${tripDirection}`;
}

/** Offers every completed Tomorrow leg this user was part of to the XP ledger. */
export async function claimTomorrowTripXp(userId: string, isRider: boolean): Promise<number> {
  if (!userId) return 0;

  const role = isRider ? ROLE_RIDER : ROLE_PASSENGER;
  const field = isRider ? "matchedRiderId" : "userId";

  let legs: RideRequest[];
  try {
    const snapshot = await getDocs(
      query(
        collection(db, "ride_requests"),
        where(field, "==", userId),
        where("status", "==", RideRequestStatus.COMPLETED),
        limit(CLAIM_SWEEP_LIMIT)
      )
    );
    legs = snapshot.docs.map((d) => ({ ...(d.data() as object), requestId: d.id }) as RideRequest);
  } catch {
    return 0;
  }

  let awarded = 0;
  for (const leg of legs) {
    if (leg.issueReported) continue;
    if (!leg.rideDate || !leg.tripDirection) continue;

    const dedupeKey = tomorrowTripDedupeKey(leg.rideDate, leg.tripDirection);
    if (!dedupeKey) continue;

    const didAward = await awardXp({
      userId,
      reason: REASON_TOMORROW_TRIP,
      role,
      sourceId: leg.requestId,
      dedupeKey,
    });

    if (didAward) awarded++;
  }

  return awarded;
}

/** Claims every completed trip (Ride Now + Tomorrow) for this user. Safe to call repeatedly. */
export async function claimTripXpFor(userId: string, isRider: boolean): Promise<number> {
  const [tomorrow, rideNow] = await Promise.all([
    claimTomorrowTripXp(userId, isRider),
    claimRideNowTripXp(userId, isRider),
  ]);
  return tomorrow + rideNow;
}

/**
 * Awards the once-per-travel-date "plan saved" XP. sourceId must be a
 * `rides` doc id (rider) or `ride_requests` doc id (passenger) whose
 * rideDate matches rideDate — firestore.rules re-derives and checks this.
 */
export async function awardPlanSavedXp(params: {
  userId: string;
  isRider: boolean;
  sourceId: string;
  rideDate: string;
}): Promise<boolean> {
  return awardXp({
    userId: params.userId,
    reason: REASON_PLAN_SAVED,
    role: params.isRider ? ROLE_RIDER : ROLE_PASSENGER,
    sourceId: params.sourceId,
    dedupeKey: params.rideDate,
  });
}
