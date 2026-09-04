import {
  Timestamp,
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  query,
  runTransaction,
  setDoc,
  updateDoc,
  where,
  arrayUnion,
  type Unsubscribe,
} from "firebase/firestore";
import { db, auth } from "@/lib/firebase";

export const RideRequestStatus = {
  PENDING: "pending",
  ACCEPTED: "accepted",
  CANCELLED: "cancelled",
  START_PENDING_CONFIRMATION: "start_pending_confirmation",
  ONGOING: "ongoing",
  END_PENDING_CONFIRMATION: "end_pending_confirmation",
  COMPLETED: "completed",
  NOT_COMPLETED: "not_completed",
  UNVERIFIED: "unverified",
} as const;

export const ACTIVE_LIFECYCLE_STATUSES = [
  RideRequestStatus.ACCEPTED,
  RideRequestStatus.START_PENDING_CONFIRMATION,
  RideRequestStatus.ONGOING,
  RideRequestStatus.END_PENDING_CONFIRMATION,
  RideRequestStatus.COMPLETED,
];

export const UNFINISHED_LIFECYCLE_STATUSES = [
  RideRequestStatus.ACCEPTED,
  RideRequestStatus.START_PENDING_CONFIRMATION,
  RideRequestStatus.ONGOING,
  RideRequestStatus.END_PENDING_CONFIRMATION,
];

const MISSED_RIDE_GRACE_MINUTES = 180;

export interface Ride {
  rideId: string;
  riderId: string;
  riderName: string;
  tripDirection: string;
  pickup: string;
  destination: string;
  tripTime: string;
  timeMinutes: number;
  routeKey: string;
  rideDate: string;
  vehicleType: string;
  vehicleModel: string;
  vehicleNumber: string;
  vehicleColor: string;
  totalSeats: number;
  availableSeats: number;
  status: string;
  isTomorrowSetup: boolean;
  createdAt: Timestamp | null;
}

export interface RideRequest {
  requestId: string;
  userId: string;
  passengerName: string;
  passengerPhone: string;
  pickup: string;
  destination: string;
  tripDirection: string;
  tripTime: string;
  hour: number;
  minute: number;
  timeMinutes: number;
  routeKey: string;
  rideDate: string;
  status: string;
  createdAt: Timestamp | null;
  matchedRideId: string;
  matchedRiderId: string;
  matchedRiderName: string;
  matchedRiderPhone: string;
  matchedRideTime: string;
  matchedVehicleType: string;
  matchedVehicleModel: string;
  matchedVehicleNumber: string;
  matchedVehicleColor: string;
  acceptedAt: Timestamp | null;
  rideStartedByRider: boolean;
  rideConfirmedByPassenger: boolean;
  rideEndedByRider: boolean;
  rideCompletedByPassenger: boolean;
  startedAt: Timestamp | null;
  completedAt: Timestamp | null;
  riderHappenedAnswer: string;
  passengerHappenedAnswer: string;
  riderAnsweredAt: Timestamp | null;
  passengerAnsweredAt: Timestamp | null;
  riderRated: boolean;
  rating: number;
  ratedAt: Timestamp | null;
  passengerRated: boolean;
  passengerRating: number;
  passengerRatedAt: Timestamp | null;
  issueReported: boolean;
  reportReason: string;
  reportDetails: string;
  reportedAt: Timestamp | null;
  rejectedByRiderIds: string[];
  cancelledBy: string;
  cancelledByRole: string;
  cancellationReason: string;
  cancelledAt: Timestamp | null;
}

const ridesCol = collection(db, "rides");
const rideRequestsCol = collection(db, "ride_requests");
const usersCol = collection(db, "users");
const ratingsCol = collection(db, "ride_ratings");
const reportsCol = collection(db, "ride_reports");

function toRide(id: string, data: Record<string, unknown>): Ride {
  return { ...(data as object), rideId: id } as Ride;
}

function toRequest(id: string, data: Record<string, unknown>): RideRequest {
  return { ...(data as object), requestId: id } as RideRequest;
}

export function seatsTaken(ride: Ride): number {
  return Math.max(ride.totalSeats - ride.availableSeats, 0);
}

export function seatCapacity(ride: Ride): number {
  return Math.max(ride.totalSeats, ride.availableSeats, 1);
}

export function isTimeClose(a: number, b: number, gapMinutes = 30): boolean {
  return Math.abs(a - b) <= gapMinutes;
}

export function needsMissedRideReview(
  request: RideRequest,
  nowMillis: number = Date.now()
): boolean {
  if (!UNFINISHED_LIFECYCLE_STATUSES.includes(request.status as never)) return false;

  const parts = request.rideDate.split("-");
  if (parts.length !== 3) return false;
  const [year, month, day] = parts.map((p) => parseInt(p, 10));
  if ([year, month, day].some((n) => Number.isNaN(n))) return false;

  const departure = new Date(
    year,
    month - 1,
    day,
    Math.floor(request.timeMinutes / 60),
    request.timeMinutes % 60,
    0,
    0
  ).getTime();

  return nowMillis - departure > MISSED_RIDE_GRACE_MINUTES * 60_000;
}

// ---------- Rider: saved rides ----------

export function listenRiderRides(
  riderId: string,
  rideDate: string,
  onData: (rides: Ride[]) => void
): Unsubscribe {
  const q = query(
    ridesCol,
    where("riderId", "==", riderId),
    where("rideDate", "==", rideDate)
  );
  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRide(d.id, d.data())));
  });
}

export type TomorrowLegResult =
  | { kind: "saved"; docId: string; isNew: boolean }
  | { kind: "blocked"; reason: string };

function directionLabel(direction: string): string {
  return direction === "to_campus" ? "campus" : "return";
}

export async function upsertRiderRide(params: {
  riderId: string;
  riderName: string;
  rideDate: string;
  tripDirection: string;
  pickup: string;
  destination: string;
  tripTime: string;
  timeMinutes: number;
  vehicleType: string;
  vehicleModel: string;
  vehicleNumber: string;
  vehicleColor: string;
  requestedSeats: number;
}): Promise<TomorrowLegResult> {
  const isCar = params.vehicleType.trim().toLowerCase() === "car";
  const normalizedVehicleType = isCar ? "car" : "bike";
  const seats = isCar ? Math.min(Math.max(params.requestedSeats, 1), 4) : 1;

  const existingSnap = await getDocs(
    query(
      ridesCol,
      where("riderId", "==", params.riderId),
      where("rideDate", "==", params.rideDate),
      where("tripDirection", "==", params.tripDirection),
      limit(1)
    )
  );

  const existingDoc = existingSnap.docs[0];
  const existing = existingDoc ? toRide(existingDoc.id, existingDoc.data()) : null;
  const routeKey = buildTomorrowRouteKey(params.tripDirection, params.pickup, params.destination);

  if (existing) {
    if (existing.status !== "active" || seatsTaken(existing) > 0) {
      return {
        kind: "blocked",
        reason: `Your ${directionLabel(params.tripDirection)} ride is already matched with a passenger and can't be edited here.`,
      };
    }

    await updateDoc(doc(ridesCol, existing.rideId), {
      riderName: params.riderName,
      pickup: params.pickup,
      destination: params.destination,
      tripTime: params.tripTime,
      timeMinutes: params.timeMinutes,
      routeKey,
      vehicleType: normalizedVehicleType,
      vehicleModel: params.vehicleModel,
      vehicleNumber: params.vehicleNumber,
      vehicleColor: params.vehicleColor,
      totalSeats: seats,
      availableSeats: seats,
      status: "active",
    });

    return { kind: "saved", docId: existing.rideId, isNew: false };
  }

  const docRef = doc(ridesCol);
  const ride: Omit<Ride, "rideId"> = {
    riderId: params.riderId,
    riderName: params.riderName,
    tripDirection: params.tripDirection,
    pickup: params.pickup,
    destination: params.destination,
    tripTime: params.tripTime,
    timeMinutes: params.timeMinutes,
    routeKey,
    rideDate: params.rideDate,
    vehicleType: normalizedVehicleType,
    vehicleModel: params.vehicleModel,
    vehicleNumber: params.vehicleNumber,
    vehicleColor: params.vehicleColor,
    totalSeats: seats,
    availableSeats: seats,
    status: "active",
    isTomorrowSetup: true,
    createdAt: Timestamp.now(),
  };

  await setDoc(docRef, ride);
  return { kind: "saved", docId: docRef.id, isNew: true };
}

export function listenAcceptedRequestsForRider(
  riderId: string,
  rideDate: string,
  onData: (requests: RideRequest[]) => void
): Unsubscribe {
  const q = query(
    rideRequestsCol,
    where("matchedRiderId", "==", riderId),
    where("rideDate", "==", rideDate),
    where("status", "in", ACTIVE_LIFECYCLE_STATUSES)
  );
  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRequest(d.id, d.data())));
  });
}

export async function riderCancelAcceptedRide(params: {
  rideId: string;
  requestId: string;
  riderId: string;
  reason: string;
}): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const rideRef = doc(ridesCol, params.rideId);
    const requestRef = doc(rideRequestsCol, params.requestId);

    const rideSnap = await transaction.get(rideRef);
    const requestSnap = await transaction.get(requestRef);

    if (!rideSnap.exists()) throw new Error("Ride not found.");
    if (!requestSnap.exists()) throw new Error("Request not found.");

    const ride = rideSnap.data() as Ride;
    const request = requestSnap.data() as RideRequest;

    if (ride.riderId !== params.riderId) throw new Error("Not your ride.");
    if (request.matchedRiderId !== params.riderId) {
      throw new Error("This request isn't matched to you.");
    }
    if (request.status !== "accepted") {
      throw new Error("This request is no longer accepted.");
    }

    const now = Timestamp.now();

    transaction.update(requestRef, {
      status: "cancelled",
      cancelledBy: params.riderId,
      cancelledByRole: "rider",
      cancellationReason: params.reason,
      cancelledAt: now,
    });

    transaction.update(rideRef, {
      availableSeats: Math.min(ride.availableSeats + 1, seatCapacity(ride)),
      status: "active",
    });
  });
}

export async function deleteRide(rideId: string, riderId: string): Promise<void> {
  const snap = await getDoc(doc(ridesCol, rideId));
  if (!snap.exists()) throw new Error("Ride not found.");
  const ride = toRide(snap.id, snap.data());

  if (ride.riderId !== riderId) throw new Error("You can only remove your own rides.");
  if (ride.status !== "active" || seatsTaken(ride) > 0) {
    throw new Error("This ride is already matched with a passenger and can't be removed here.");
  }

  const { deleteDoc } = await import("firebase/firestore");
  await deleteDoc(doc(ridesCol, rideId));
}

// ---------- Missed-trip reconciliation ----------

export function listenPassengerUnfinishedLegs(
  userId: string,
  onData: (legs: RideRequest[]) => void
): Unsubscribe {
  const q = query(
    rideRequestsCol,
    where("userId", "==", userId),
    where("status", "in", UNFINISHED_LIFECYCLE_STATUSES)
  );
  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRequest(d.id, d.data())));
  });
}

export function listenRiderUnfinishedLegs(
  riderId: string,
  onData: (legs: RideRequest[]) => void
): Unsubscribe {
  const q = query(
    rideRequestsCol,
    where("matchedRiderId", "==", riderId),
    where("status", "in", UNFINISHED_LIFECYCLE_STATUSES)
  );
  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRequest(d.id, d.data())));
  });
}

export async function submitMissedRideAnswer(params: {
  requestId: string;
  userId: string;
  isRider: boolean;
  answer: "yes" | "no";
}): Promise<string> {
  return runTransaction(db, async (transaction) => {
    const requestRef = doc(rideRequestsCol, params.requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideRequest | undefined;
    if (!request) throw new Error("Invalid ride request data.");

    const ownerId = params.isRider ? request.matchedRiderId : request.userId;
    if (ownerId !== params.userId) throw new Error("This trip isn't yours to confirm.");

    if (!UNFINISHED_LIFECYCLE_STATUSES.includes(request.status as never)) {
      throw new Error("This trip has already been closed.");
    }

    const ownAnswer = params.isRider ? request.riderHappenedAnswer : request.passengerHappenedAnswer;
    if (ownAnswer) throw new Error("You already answered for this trip.");

    const otherAnswer = params.isRider ? request.passengerHappenedAnswer : request.riderHappenedAnswer;

    let newStatus: string;
    if (!otherAnswer) {
      newStatus = request.status;
    } else if (otherAnswer !== params.answer) {
      newStatus = RideRequestStatus.UNVERIFIED;
    } else if (params.answer === "yes") {
      newStatus = RideRequestStatus.COMPLETED;
    } else {
      newStatus = RideRequestStatus.NOT_COMPLETED;
    }

    const now = Timestamp.now();
    const updates: Record<string, unknown> = {
      status: newStatus,
      ...(params.isRider
        ? { riderHappenedAnswer: params.answer, riderAnsweredAt: now }
        : { passengerHappenedAnswer: params.answer, passengerAnsweredAt: now }),
    };

    if (newStatus === RideRequestStatus.COMPLETED) {
      updates.completedAt = now;
    }

    transaction.update(requestRef, updates);
    return newStatus;
  });
}

// ---------- Trip lifecycle ----------

export async function riderStartTrip(requestId: string, riderId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideRequest | undefined;

    if (!request || request.matchedRiderId !== riderId) {
      throw new Error("This request isn't matched to you.");
    }
    if (request.status !== RideRequestStatus.ACCEPTED) {
      throw new Error("Trip can only be started after it is accepted.");
    }

    transaction.update(requestRef, {
      status: RideRequestStatus.START_PENDING_CONFIRMATION,
      rideStartedByRider: true,
      rideConfirmedByPassenger: false,
      startedAt: null,
    });
  });
}

export async function passengerConfirmTripStarted(requestId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideRequest | undefined;

    if (!request || request.status !== RideRequestStatus.START_PENDING_CONFIRMATION) {
      throw new Error("Ride is not waiting for start confirmation.");
    }

    transaction.update(requestRef, {
      status: RideRequestStatus.ONGOING,
      rideConfirmedByPassenger: true,
      startedAt: Timestamp.now(),
    });
  });
}

export async function passengerRejectTripStarted(requestId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideRequest | undefined;

    if (!request || request.status !== RideRequestStatus.START_PENDING_CONFIRMATION) {
      throw new Error("Ride is not waiting for start confirmation.");
    }

    transaction.update(requestRef, {
      status: RideRequestStatus.ACCEPTED,
      rideStartedByRider: false,
      rideConfirmedByPassenger: false,
      startedAt: null,
    });
  });
}

export async function riderRequestTripCompletion(requestId: string, riderId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideRequest | undefined;

    if (!request || request.matchedRiderId !== riderId) {
      throw new Error("This request isn't matched to you.");
    }
    if (request.status !== RideRequestStatus.ONGOING) {
      throw new Error("Only an ongoing trip can be marked for completion.");
    }

    transaction.update(requestRef, {
      status: RideRequestStatus.END_PENDING_CONFIRMATION,
      rideEndedByRider: true,
      rideCompletedByPassenger: false,
      completedAt: null,
    });
  });
}

export async function passengerConfirmTripCompleted(requestId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideRequest | undefined;

    if (!request || request.status !== RideRequestStatus.END_PENDING_CONFIRMATION) {
      throw new Error("Ride is not waiting for completion confirmation.");
    }

    transaction.update(requestRef, {
      status: RideRequestStatus.COMPLETED,
      rideCompletedByPassenger: true,
      completedAt: Timestamp.now(),
    });

    if (request.matchedRiderId) {
      const { increment } = await import("firebase/firestore");
      transaction.update(doc(usersCol, request.matchedRiderId), {
        completedRideCount: increment(1),
      });
    }
  });
}

// ---------- Passenger: saved requests ----------

export function listenPassengerRequests(
  userId: string,
  rideDate: string,
  onData: (requests: RideRequest[]) => void
): Unsubscribe {
  const q = query(
    rideRequestsCol,
    where("userId", "==", userId),
    where("rideDate", "==", rideDate)
  );
  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRequest(d.id, d.data())));
  });
}

export async function upsertPassengerRequest(params: {
  userId: string;
  passengerName: string;
  passengerPhone: string;
  rideDate: string;
  tripDirection: string;
  pickup: string;
  destination: string;
  tripTime: string;
  hour: number;
  minute: number;
  timeMinutes: number;
}): Promise<TomorrowLegResult> {
  const existingSnap = await getDocs(
    query(
      rideRequestsCol,
      where("userId", "==", params.userId),
      where("rideDate", "==", params.rideDate),
      where("tripDirection", "==", params.tripDirection),
      limit(1)
    )
  );

  const existingDoc = existingSnap.docs[0];
  const existing = existingDoc ? toRequest(existingDoc.id, existingDoc.data()) : null;
  const routeKey = buildTomorrowRouteKey(params.tripDirection, params.pickup, params.destination);

  if (existing) {
    if (existing.status !== "pending" && existing.status !== "cancelled") {
      return {
        kind: "blocked",
        reason: `Your ${directionLabel(params.tripDirection)} request is already accepted and can't be edited here.`,
      };
    }

    await updateDoc(doc(rideRequestsCol, existing.requestId), {
      passengerName: params.passengerName,
      passengerPhone: params.passengerPhone,
      pickup: params.pickup,
      destination: params.destination,
      tripTime: params.tripTime,
      hour: params.hour,
      minute: params.minute,
      timeMinutes: params.timeMinutes,
      routeKey,
      status: "pending",
      matchedRideId: "",
      matchedRiderId: "",
      matchedRiderName: "",
      matchedRiderPhone: "",
      matchedRideTime: "",
      matchedVehicleType: "",
      matchedVehicleModel: "",
      matchedVehicleNumber: "",
      matchedVehicleColor: "",
      acceptedAt: null,
      cancelledBy: "",
      cancelledByRole: "",
      cancellationReason: "",
      cancelledAt: null,
    });

    return { kind: "saved", docId: existing.requestId, isNew: false };
  }

  const docRef = doc(rideRequestsCol);
  const request: Omit<RideRequest, "requestId"> = {
    userId: params.userId,
    passengerName: params.passengerName,
    passengerPhone: params.passengerPhone,
    pickup: params.pickup,
    destination: params.destination,
    tripDirection: params.tripDirection,
    tripTime: params.tripTime,
    hour: params.hour,
    minute: params.minute,
    timeMinutes: params.timeMinutes,
    routeKey,
    rideDate: params.rideDate,
    status: "pending",
    createdAt: Timestamp.now(),
    matchedRideId: "",
    matchedRiderId: "",
    matchedRiderName: "",
    matchedRiderPhone: "",
    matchedRideTime: "",
    matchedVehicleType: "",
    matchedVehicleModel: "",
    matchedVehicleNumber: "",
    matchedVehicleColor: "",
    acceptedAt: null,
    rideStartedByRider: false,
    rideConfirmedByPassenger: false,
    rideEndedByRider: false,
    rideCompletedByPassenger: false,
    startedAt: null,
    completedAt: null,
    riderHappenedAnswer: "",
    passengerHappenedAnswer: "",
    riderAnsweredAt: null,
    passengerAnsweredAt: null,
    riderRated: false,
    rating: 0,
    ratedAt: null,
    passengerRated: false,
    passengerRating: 0,
    passengerRatedAt: null,
    issueReported: false,
    reportReason: "",
    reportDetails: "",
    reportedAt: null,
    rejectedByRiderIds: [],
    cancelledBy: "",
    cancelledByRole: "",
    cancellationReason: "",
    cancelledAt: null,
  };

  await setDoc(docRef, request);
  return { kind: "saved", docId: docRef.id, isNew: true };
}

async function callTomorrowProxy(action: string, body: Record<string, unknown>): Promise<void> {
  const idToken = await auth.currentUser?.getIdToken();
  if (!idToken) throw new Error("Not authenticated.");

  const res = await fetch(`/api/tomorrow/${action}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const errorBody = await res.json().catch(() => null);
    throw new Error(errorBody?.error ?? `Request failed (${res.status}).`);
  }
}

export async function requestPassengerCancellation(requestId: string, reason: string): Promise<void> {
  await callTomorrowProxy("cancel-request", { requestId, reason });
}

export async function notifyPassengerAccepted(requestId: string): Promise<void> {
  try {
    await callTomorrowProxy("notify-accepted", { requestId });
  } catch {
    // best-effort push, matches Android's fire-and-forget behavior
  }
}

export async function notifyMatchingRiders(requestId: string): Promise<void> {
  try {
    await callTomorrowProxy("notify-match", { requestId });
  } catch {
    // best-effort push
  }
}

export async function deleteRequest(requestId: string, userId: string): Promise<void> {
  const snap = await getDoc(doc(rideRequestsCol, requestId));
  if (!snap.exists()) throw new Error("Request not found.");
  const request = toRequest(snap.id, snap.data());

  if (request.userId !== userId) throw new Error("You can only remove your own requests.");
  if (request.status !== "pending") {
    throw new Error("This request is already accepted and can't be removed here.");
  }

  const { deleteDoc } = await import("firebase/firestore");
  await deleteDoc(doc(rideRequestsCol, requestId));
}

// ---------- Matching ----------

export function listenActiveRidesForDate(
  rideDate: string,
  onData: (rides: Ride[]) => void
): Unsubscribe {
  const q = query(ridesCol, where("rideDate", "==", rideDate), where("status", "==", "active"));
  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRide(d.id, d.data())));
  });
}

export function listenPendingRequests(
  rideDate: string,
  onData: (requests: RideRequest[]) => void
): Unsubscribe {
  const q = query(rideRequestsCol, where("rideDate", "==", rideDate), where("status", "==", "pending"));
  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRequest(d.id, d.data())));
  });
}

export async function declineRequest(requestId: string, riderId: string): Promise<void> {
  await updateDoc(doc(rideRequestsCol, requestId), {
    rejectedByRiderIds: arrayUnion(riderId),
  });
}

export async function acceptRequest(params: {
  rideId: string;
  requestId: string;
  riderId: string;
  riderName: string;
  riderPhone: string;
}): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const rideRef = doc(ridesCol, params.rideId);
    const requestRef = doc(rideRequestsCol, params.requestId);

    const rideSnap = await transaction.get(rideRef);
    const requestSnap = await transaction.get(requestRef);

    if (!rideSnap.exists()) throw new Error("Ride not found.");
    if (!requestSnap.exists()) throw new Error("Request not found.");

    const ride = rideSnap.data() as Ride;
    const request = requestSnap.data() as RideRequest;

    if (ride.riderId !== params.riderId) throw new Error("Not your ride.");
    if (ride.status !== "active") throw new Error("Ride no longer active.");
    if (ride.availableSeats <= 0) throw new Error("No seats available.");
    if (request.status !== "pending") throw new Error("Request already handled.");

    const newSeats = ride.availableSeats - 1;
    const now = Timestamp.now();

    transaction.update(requestRef, {
      status: "accepted",
      matchedRideId: params.rideId,
      matchedRiderId: params.riderId,
      matchedRiderName: params.riderName,
      matchedRiderPhone: params.riderPhone,
      matchedRideTime: ride.tripTime,
      matchedVehicleType: ride.vehicleType,
      matchedVehicleModel: ride.vehicleModel,
      matchedVehicleNumber: ride.vehicleNumber,
      matchedVehicleColor: ride.vehicleColor,
      acceptedAt: now,
    });

    transaction.update(rideRef, {
      availableSeats: newSeats,
      status: newSeats <= 0 ? "full" : "active",
    });
  });
}

// ---------- Ratings / reports (mirrors ridenow.ts, targeting ride_requests) ----------

export async function submitTomorrowRideRating(params: {
  requestId: string;
  ratedBy: string;
  ratedTo: string;
  stars: number;
  comment: string;
}): Promise<void> {
  const existing = await getDocs(
    query(
      ratingsCol,
      where("requestId", "==", params.requestId),
      where("ratedBy", "==", params.ratedBy),
      limit(1)
    )
  );
  if (!existing.empty) throw new Error("You already rated this ride.");

  const ratingRef = doc(ratingsCol);
  const userRef = doc(usersCol, params.ratedTo);
  const requestRef = doc(rideRequestsCol, params.requestId);

  await runTransaction(db, async (transaction) => {
    const userSnap = await transaction.get(userRef);
    const requestSnap = await transaction.get(requestRef);

    if (requestSnap.data()?.riderRated) throw new Error("You already rated this ride.");
    if (requestSnap.data()?.issueReported) {
      throw new Error("You cannot rate after reporting an issue.");
    }

    const oldAverage = (userSnap.data()?.ratingAverage as number) ?? 0;
    const oldCount = (userSnap.data()?.ratingCount as number) ?? 0;
    const newCount = oldCount + 1;
    const newAverage = (oldAverage * oldCount + params.stars) / newCount;
    const request = requestSnap.data() as RideRequest;

    transaction.set(ratingRef, {
      ratingId: ratingRef.id,
      requestId: params.requestId,
      rideId: request.matchedRideId,
      passengerId: request.userId,
      riderId: request.matchedRiderId,
      ratedBy: params.ratedBy,
      ratedTo: params.ratedTo,
      stars: params.stars,
      comment: params.comment,
      createdAt: Timestamp.now(),
    });

    transaction.update(userRef, { ratingAverage: newAverage, ratingCount: newCount });
    transaction.update(requestRef, {
      riderRated: true,
      rating: params.stars,
      ratedAt: Timestamp.now(),
    });
  });
}

export async function submitTomorrowPassengerRating(params: {
  requestId: string;
  ratedBy: string;
  ratedTo: string;
  stars: number;
  comment: string;
}): Promise<void> {
  const existing = await getDocs(
    query(
      ratingsCol,
      where("requestId", "==", params.requestId),
      where("ratedBy", "==", params.ratedBy),
      limit(1)
    )
  );
  if (!existing.empty) throw new Error("You already rated this passenger.");

  const ratingRef = doc(ratingsCol);
  const userRef = doc(usersCol, params.ratedTo);
  const requestRef = doc(rideRequestsCol, params.requestId);

  await runTransaction(db, async (transaction) => {
    const userSnap = await transaction.get(userRef);
    const requestSnap = await transaction.get(requestRef);

    if (requestSnap.data()?.passengerRated) throw new Error("You already rated this passenger.");
    if (requestSnap.data()?.issueReported) {
      throw new Error("You cannot rate after an issue was reported.");
    }

    const oldAverage = (userSnap.data()?.ratingAverage as number) ?? 0;
    const oldCount = (userSnap.data()?.ratingCount as number) ?? 0;
    const newCount = oldCount + 1;
    const newAverage = (oldAverage * oldCount + params.stars) / newCount;
    const request = requestSnap.data() as RideRequest;

    transaction.set(ratingRef, {
      ratingId: ratingRef.id,
      requestId: params.requestId,
      rideId: request.matchedRideId,
      passengerId: request.userId,
      riderId: request.matchedRiderId,
      ratedBy: params.ratedBy,
      ratedTo: params.ratedTo,
      stars: params.stars,
      comment: params.comment,
      createdAt: Timestamp.now(),
    });

    transaction.update(userRef, { ratingAverage: newAverage, ratingCount: newCount });
    transaction.update(requestRef, {
      passengerRated: true,
      passengerRating: params.stars,
      passengerRatedAt: Timestamp.now(),
    });
  });
}

export async function submitTomorrowRideReport(params: {
  requestId: string;
  reportedBy: string;
  reportedUserId: string;
  reason: string;
  details: string;
}): Promise<void> {
  const existing = await getDocs(
    query(
      reportsCol,
      where("requestId", "==", params.requestId),
      where("reportedBy", "==", params.reportedBy),
      limit(1)
    )
  );
  if (!existing.empty) throw new Error("You already reported this ride.");

  const reportRef = doc(reportsCol);
  const requestRef = doc(rideRequestsCol, params.requestId);

  await runTransaction(db, async (transaction) => {
    const requestSnap = await transaction.get(requestRef);
    if (requestSnap.data()?.issueReported) throw new Error("You already reported this ride.");
    if (requestSnap.data()?.riderRated) {
      throw new Error("You cannot report after submitting a rating.");
    }

    const { increment } = await import("firebase/firestore");

    transaction.set(reportRef, {
      reportId: reportRef.id,
      requestId: params.requestId,
      rideId: "",
      passengerId: params.reportedBy,
      riderId: params.reportedUserId,
      reportedBy: params.reportedBy,
      reportedUserId: params.reportedUserId,
      reason: params.reason,
      details: params.details,
      status: "pending",
      createdAt: Timestamp.now(),
    });

    transaction.update(doc(usersCol, params.reportedUserId), {
      reportCount: increment(1),
    });

    transaction.update(requestRef, {
      issueReported: true,
      reportReason: params.reason,
      reportDetails: params.details,
      reportedAt: Timestamp.now(),
    });
  });
}

// ---------- Shared helpers ----------

export function buildTomorrowRouteKey(tripDirection: string, pickup: string, destination: string): string {
  return `${tripDirection.trim().toLowerCase()}|${pickup.trim().toLowerCase()}|${destination.trim().toLowerCase()}`;
}

export function getTomorrowDateKey(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
