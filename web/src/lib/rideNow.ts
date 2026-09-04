import {
  Timestamp,
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
  limit,
  onSnapshot,
  orderBy,
  query,
  runTransaction,
  setDoc,
  updateDoc,
  where,
  type Unsubscribe,
} from "firebase/firestore";
import { db } from "@/lib/firebase";

export const RideNowStatus = {
  SEARCHING: "searching",
  NOTIFIED: "notified",
  ACCEPTED: "accepted",
  START_PENDING_CONFIRMATION: "start_pending_confirmation",
  ONGOING: "ongoing",
  END_PENDING_CONFIRMATION: "end_pending_confirmation",
  COMPLETED: "completed",
  CANCELLED: "cancelled",
  EXPIRED: "expired",
  ISSUE_REPORTED: "issue_reported",
  UNVERIFIED: "unverified",
} as const;

const BLOCKING_STATUSES = [
  RideNowStatus.SEARCHING,
  RideNowStatus.NOTIFIED,
  RideNowStatus.ACCEPTED,
  RideNowStatus.START_PENDING_CONFIRMATION,
  RideNowStatus.ONGOING,
  RideNowStatus.END_PENDING_CONFIRMATION,
];

const MATCHED_STATUSES = [
  RideNowStatus.ACCEPTED,
  RideNowStatus.START_PENDING_CONFIRMATION,
  RideNowStatus.ONGOING,
  RideNowStatus.END_PENDING_CONFIRMATION,
];

const RIDER_ESCAPE_MINUTES = 10;
const STALE_HOURS = 6;

export interface LiveRide {
  rideId: string;
  riderId: string;
  riderName: string;
  pickup: string;
  destination: string;
  tripDirection: string;
  tripTime: string;
  timeMinutes: number;
  routeKey: string;
  vehicleType: string;
  vehicleModel: string;
  vehicleNumber: string;
  vehicleColor: string;
  availableSeats: number;
  status: string;
  liveNow: boolean;
  available: boolean;
  currentRequestId: string;
  createdAt: Timestamp | null;
  lastUpdatedAt: Timestamp | null;
}

export interface RideNowRequest {
  requestId: string;
  passengerId: string;
  passengerName: string;
  passengerPhone: string;
  pickup: string;
  destination: string;
  tripTime: string;
  timeMinutes: number;
  routeKey: string;
  status: string;
  matchedRideId: string;
  matchedRiderId: string;
  matchedRiderName: string;
  matchedRiderPhone: string;
  matchedVehicleType: string;
  matchedVehicleModel: string;
  matchedVehicleNumber: string;
  matchedVehicleColor: string;
  acceptedAt: Timestamp | null;
  startedAt: Timestamp | null;
  completedAt: Timestamp | null;
  cancelledAt: Timestamp | null;
  expiredAt: Timestamp | null;
  closedByRole: string;
  closedAt: Timestamp | null;
  createdAt: Timestamp | null;
  expiresAt: Timestamp | null;
  riderRated: boolean;
  issueReported: boolean;
  rating: number;
  ratedAt: Timestamp | null;
  passengerRated: boolean;
  passengerRating: number;
  passengerRatedAt: Timestamp | null;
  reportReason: string;
  reportDetails: string;
  reportedAt: Timestamp | null;
  rideStartedByRider: boolean;
  rideConfirmedByPassenger: boolean;
  rideEndedByRider: boolean;
  rideCompletedByPassenger: boolean;
}

const liveRidesCol = collection(db, "live_rides");
const rideNowRequestsCol = collection(db, "ride_now_requests");
const rideHistoryCol = collection(db, "ride_history");
const ratingsCol = collection(db, "ride_ratings");
const reportsCol = collection(db, "ride_reports");
const usersCol = collection(db, "users");

function toRequest(id: string, data: Record<string, unknown>): RideNowRequest {
  return { ...(data as object), requestId: id } as RideNowRequest;
}

function toLiveRide(id: string, data: Record<string, unknown>): LiveRide {
  return { ...(data as object), rideId: id } as LiveRide;
}

export function lastProgressSeconds(request: RideNowRequest): number | null {
  const candidates = [
    request.completedAt,
    request.startedAt,
    request.acceptedAt,
    request.createdAt,
  ].filter((t): t is Timestamp => t != null);

  if (candidates.length === 0) return null;
  return Math.max(...candidates.map((t) => t.seconds));
}

export function isAbandoned(request: RideNowRequest, nowSeconds: number): boolean {
  if (!MATCHED_STATUSES.includes(request.status as (typeof MATCHED_STATUSES)[number])) {
    return false;
  }
  const last = lastProgressSeconds(request);
  if (last == null) return false;
  return nowSeconds - last > STALE_HOURS * 60 * 60;
}

export function riderMayForceClose(request: RideNowRequest, nowSeconds: number): boolean {
  if (!MATCHED_STATUSES.includes(request.status as (typeof MATCHED_STATUSES)[number])) {
    return false;
  }
  const last = lastProgressSeconds(request);
  if (last == null) return false;
  return nowSeconds - last > RIDER_ESCAPE_MINUTES * 60;
}

// ---------- Passenger: create / cancel ----------

export async function createRideNowRequest(params: {
  passengerId: string;
  passengerName: string;
  passengerPhone: string;
  pickup: string;
  destination: string;
  tripTime: string;
  timeMinutes: number;
  routeKey: string;
}): Promise<string> {
  const existing = await getDocs(
    query(
      rideNowRequestsCol,
      where("passengerId", "==", params.passengerId),
      where("status", "in", BLOCKING_STATUSES),
      limit(1)
    )
  );

  if (!existing.empty) {
    throw new Error("You already have an active ride request.");
  }

  const docRef = doc(rideNowRequestsCol);
  const now = Timestamp.now();

  const request: Omit<RideNowRequest, "requestId"> = {
    passengerId: params.passengerId,
    passengerName: params.passengerName,
    passengerPhone: params.passengerPhone,
    pickup: params.pickup,
    destination: params.destination,
    tripTime: params.tripTime,
    timeMinutes: params.timeMinutes,
    routeKey: params.routeKey,
    status: RideNowStatus.SEARCHING,
    matchedRideId: "",
    matchedRiderId: "",
    matchedRiderName: "",
    matchedRiderPhone: "",
    matchedVehicleType: "",
    matchedVehicleModel: "",
    matchedVehicleNumber: "",
    matchedVehicleColor: "",
    acceptedAt: null,
    startedAt: null,
    completedAt: null,
    cancelledAt: null,
    expiredAt: null,
    closedByRole: "",
    closedAt: null,
    createdAt: now,
    expiresAt: new Timestamp(now.seconds + 120, 0),
    riderRated: false,
    issueReported: false,
    rating: 0,
    ratedAt: null,
    passengerRated: false,
    passengerRating: 0,
    passengerRatedAt: null,
    reportReason: "",
    reportDetails: "",
    reportedAt: null,
    rideStartedByRider: false,
    rideConfirmedByPassenger: false,
    rideEndedByRider: false,
    rideCompletedByPassenger: false,
  };

  await setDoc(docRef, request);
  return docRef.id;
}

export async function cancelRideNowRequest(requestId: string): Promise<void> {
  await updateDoc(doc(rideNowRequestsCol, requestId), {
    status: RideNowStatus.CANCELLED,
    cancelledAt: Timestamp.now(),
  });
}

export async function cancelAcceptedRideNowTrip(
  requestId: string,
  liveRideId: string
): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const rideRef = doc(liveRidesCol, liveRideId);

    const requestSnap = await transaction.get(requestRef);
    if (!requestSnap.exists()) throw new Error("Ride request not found.");

    const request = requestSnap.data() as RideNowRequest;
    if (request.status === RideNowStatus.COMPLETED) {
      throw new Error("Completed ride cannot be cancelled.");
    }

    const now = Timestamp.now();

    transaction.update(requestRef, {
      status: RideNowStatus.CANCELLED,
      cancelledAt: now,
    });

    transaction.update(rideRef, {
      status: "inactive",
      available: true,
      liveNow: false,
      currentRequestId: "",
      lastUpdatedAt: now,
    });
  });
}

// ---------- Listeners ----------

export function listenForMatchingRequests(
  routeKey: string,
  onData: (requests: RideNowRequest[]) => void
): Unsubscribe {
  const q = query(
    rideNowRequestsCol,
    where("routeKey", "==", routeKey),
    where("status", "==", RideNowStatus.SEARCHING),
    orderBy("createdAt", "asc")
  );

  return onSnapshot(q, (snapshot) => {
    onData(snapshot.docs.map((d) => toRequest(d.id, d.data())));
  });
}

export function listenToPassengerRequest(
  requestId: string,
  onData: (request: RideNowRequest | null) => void
): Unsubscribe {
  return onSnapshot(doc(rideNowRequestsCol, requestId), (snap) => {
    onData(snap.exists() ? toRequest(snap.id, snap.data()) : null);
  });
}

export function listenPassengerActiveRide(
  passengerId: string,
  onData: (request: RideNowRequest | null) => void
): Unsubscribe {
  const q = query(
    rideNowRequestsCol,
    where("passengerId", "==", passengerId),
    where("status", "in", BLOCKING_STATUSES),
    orderBy("createdAt", "desc"),
    limit(1)
  );

  return onSnapshot(q, (snapshot) => {
    const first = snapshot.docs[0];
    onData(first ? toRequest(first.id, first.data()) : null);
  });
}

export function listenToLiveRide(
  rideId: string,
  onData: (ride: LiveRide | null) => void
): Unsubscribe {
  return onSnapshot(doc(liveRidesCol, rideId), (snap) => {
    onData(snap.exists() ? toLiveRide(snap.id, snap.data()) : null);
  });
}

// ---------- Rider: go live / accept / lifecycle ----------

async function forceReleaseStaleLiveRides(riderId: string): Promise<void> {
  try {
    const stale = await getDocs(
      query(
        liveRidesCol,
        where("riderId", "==", riderId),
        where("status", "==", "active")
      )
    );

    if (stale.empty) return;

    const now = Timestamp.now();
    await Promise.all(
      stale.docs.map((d) =>
        updateDoc(d.ref, {
          status: "inactive",
          liveNow: false,
          available: false,
          currentRequestId: "",
          lastUpdatedAt: now,
        })
      )
    );
  } catch {
    // Best-effort, same as the Android repository.
  }
}

export async function goLiveAsRider(params: {
  riderId: string;
  riderName: string;
  pickup: string;
  destination: string;
  tripDirection: string;
  tripTime: string;
  timeMinutes: number;
  routeKey: string;
  availableSeats: number;
  vehicleType: string;
  vehicleModel: string;
  vehicleNumber: string;
  vehicleColor: string;
}): Promise<string> {
  await forceReleaseStaleLiveRides(params.riderId);

  const docRef = doc(liveRidesCol);
  const now = Timestamp.now();

  const liveRide: Omit<LiveRide, "rideId"> = {
    riderId: params.riderId,
    riderName: params.riderName,
    pickup: params.pickup,
    destination: params.destination,
    tripDirection: params.tripDirection,
    tripTime: params.tripTime,
    timeMinutes: params.timeMinutes,
    routeKey: params.routeKey,
    vehicleType: params.vehicleType,
    vehicleModel: params.vehicleModel,
    vehicleNumber: params.vehicleNumber,
    vehicleColor: params.vehicleColor,
    availableSeats: params.availableSeats,
    status: "active",
    liveNow: true,
    available: true,
    currentRequestId: "",
    createdAt: now,
    lastUpdatedAt: now,
  };

  await setDoc(docRef, liveRide);
  return docRef.id;
}

export async function stopLiveRide(rideId: string): Promise<void> {
  await updateDoc(doc(liveRidesCol, rideId), {
    status: "inactive",
    liveNow: false,
    available: false,
    currentRequestId: "",
    lastUpdatedAt: Timestamp.now(),
  });
}

export async function acceptRideNowRequest(params: {
  requestId: string;
  liveRideId: string;
  riderId: string;
  riderName: string;
  riderPhone: string;
}): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, params.requestId);
    const rideRef = doc(liveRidesCol, params.liveRideId);

    const requestSnap = await transaction.get(requestRef);
    const rideSnap = await transaction.get(rideRef);

    if (!requestSnap.exists()) throw new Error("Ride now request not found.");
    if (!rideSnap.exists()) throw new Error("Live ride not found.");

    const request = requestSnap.data() as RideNowRequest;
    const ride = rideSnap.data() as LiveRide;

    const now = Timestamp.now();
    const isExpired =
      request.expiresAt != null && request.expiresAt.seconds <= now.seconds;
    const isRideBusy = Boolean(ride.currentRequestId);

    if (request.status !== RideNowStatus.SEARCHING) {
      throw new Error("This request was already taken or closed.");
    }

    if (isExpired) {
      transaction.update(requestRef, {
        status: RideNowStatus.EXPIRED,
        expiredAt: now,
      });
      throw new Error("This request already expired.");
    }

    if (ride.status !== "active" || !ride.liveNow || !ride.available) {
      throw new Error("This rider is no longer available.");
    }

    if (isRideBusy) {
      throw new Error("This rider already has an active request.");
    }

    transaction.update(requestRef, {
      status: RideNowStatus.ACCEPTED,
      matchedRideId: params.liveRideId,
      matchedRiderId: params.riderId,
      matchedRiderName: params.riderName,
      matchedRiderPhone: params.riderPhone,
      matchedVehicleType: ride.vehicleType,
      matchedVehicleModel: ride.vehicleModel,
      matchedVehicleNumber: ride.vehicleNumber,
      matchedVehicleColor: ride.vehicleColor,
      acceptedAt: now,
      rideStartedByRider: false,
      rideConfirmedByPassenger: false,
      rideEndedByRider: false,
      rideCompletedByPassenger: false,
      startedAt: null,
      completedAt: null,
    });

    transaction.update(rideRef, {
      available: false,
      currentRequestId: params.requestId,
      lastUpdatedAt: now,
    });
  });
}

export async function riderStartRideNowTrip(requestId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideNowRequest | undefined;

    if (!request || request.status !== RideNowStatus.ACCEPTED) {
      throw new Error("Trip can only be started after it is accepted.");
    }

    transaction.update(requestRef, {
      status: RideNowStatus.START_PENDING_CONFIRMATION,
      rideStartedByRider: true,
      rideConfirmedByPassenger: false,
      startedAt: null,
    });
  });
}

export async function passengerConfirmRideNowStarted(requestId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideNowRequest | undefined;

    if (!request || request.status !== RideNowStatus.START_PENDING_CONFIRMATION) {
      throw new Error("Ride is not waiting for start confirmation.");
    }

    transaction.update(requestRef, {
      status: RideNowStatus.ONGOING,
      rideConfirmedByPassenger: true,
      startedAt: Timestamp.now(),
    });
  });
}

export async function passengerRejectRideNowStarted(requestId: string): Promise<void> {
  await updateDoc(doc(rideNowRequestsCol, requestId), {
    status: RideNowStatus.ACCEPTED,
    rideStartedByRider: false,
    rideConfirmedByPassenger: false,
    startedAt: null,
  });
}

export async function riderRequestRideNowCompletion(requestId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideNowRequest | undefined;

    if (!request || request.status !== RideNowStatus.ONGOING) {
      throw new Error("Only an ongoing trip can be marked for completion.");
    }

    transaction.update(requestRef, {
      status: RideNowStatus.END_PENDING_CONFIRMATION,
      rideEndedByRider: true,
      rideCompletedByPassenger: false,
      completedAt: null,
    });
  });
}

export async function passengerConfirmRideNowCompleted(
  requestId: string,
  liveRideId: string
): Promise<void> {
  const completedAt = Timestamp.now();

  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const rideRef = doc(liveRidesCol, liveRideId);

    const requestSnap = await transaction.get(requestRef);
    const request = requestSnap.data() as RideNowRequest | undefined;

    if (!request || request.status !== RideNowStatus.END_PENDING_CONFIRMATION) {
      throw new Error("Ride is not waiting for completion confirmation.");
    }

    transaction.update(requestRef, {
      status: RideNowStatus.COMPLETED,
      rideCompletedByPassenger: true,
      completedAt,
    });

    transaction.update(rideRef, {
      status: "inactive",
      available: true,
      liveNow: false,
      currentRequestId: "",
      lastUpdatedAt: completedAt,
    });

    if (request.matchedRiderId) {
      transaction.update(doc(usersCol, request.matchedRiderId), {
        completedRideCount: increment(1),
      });
    }
  });

  const completedSnap = await getDoc(doc(rideNowRequestsCol, requestId));
  if (completedSnap.exists()) {
    await saveRideNowToHistory(toRequest(completedSnap.id, completedSnap.data()));
  }
}

function releaseLiveRideWrite(now: Timestamp) {
  return {
    status: "inactive",
    available: false,
    liveNow: false,
    currentRequestId: "",
    lastUpdatedAt: now,
  };
}

export async function riderCancelUnstartedTrip(
  requestId: string,
  riderId: string
): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideNowRequest | undefined;

    if (!request || request.matchedRiderId !== riderId) {
      throw new Error("This trip isn't matched to you.");
    }

    if (
      request.status !== RideNowStatus.ACCEPTED &&
      request.status !== RideNowStatus.START_PENDING_CONFIRMATION
    ) {
      throw new Error("Only a trip that never started can be cancelled here.");
    }

    const now = Timestamp.now();

    transaction.update(requestRef, {
      status: RideNowStatus.CANCELLED,
      cancelledAt: now,
      closedByRole: "rider",
      closedAt: now,
    });

    if (request.matchedRideId) {
      transaction.update(doc(liveRidesCol, request.matchedRideId), releaseLiveRideWrite(now));
    }
  });
}

export async function riderCloseUnconfirmedTrip(
  requestId: string,
  riderId: string
): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideNowRequest | undefined;

    if (!request || request.matchedRiderId !== riderId) {
      throw new Error("This trip isn't matched to you.");
    }

    if (
      request.status !== RideNowStatus.ONGOING &&
      request.status !== RideNowStatus.END_PENDING_CONFIRMATION
    ) {
      throw new Error("Only a trip already under way can be closed here.");
    }

    const now = Timestamp.now();

    transaction.update(requestRef, {
      status: RideNowStatus.UNVERIFIED,
      closedByRole: "rider",
      closedAt: now,
    });

    if (request.matchedRideId) {
      transaction.update(doc(liveRidesCol, request.matchedRideId), releaseLiveRideWrite(now));
    }
  });
}

export async function expireRequestIfNeeded(requestId: string): Promise<void> {
  await runTransaction(db, async (transaction) => {
    const requestRef = doc(rideNowRequestsCol, requestId);
    const snap = await transaction.get(requestRef);
    const request = snap.data() as RideNowRequest | undefined;
    if (!request) return;

    const now = Timestamp.now();
    const shouldExpire =
      request.status === RideNowStatus.SEARCHING &&
      request.expiresAt != null &&
      request.expiresAt.seconds <= now.seconds;

    if (shouldExpire) {
      transaction.update(requestRef, {
        status: RideNowStatus.EXPIRED,
        expiredAt: now,
      });
    }
  });
}

/** Best-effort sweep, mirrors XpRequestRepository.closeAbandonedPassengerRequests. */
export async function closeAbandonedPassengerRequests(passengerId: string): Promise<number> {
  try {
    const snapshot = await getDocs(
      query(
        rideNowRequestsCol,
        where("passengerId", "==", passengerId),
        where("status", "in", MATCHED_STATUSES)
      )
    );

    const nowSeconds = Timestamp.now().seconds;
    const abandoned = snapshot.docs
      .map((d) => toRequest(d.id, d.data()))
      .filter((r) => isAbandoned(r, nowSeconds));

    let closed = 0;
    for (const request of abandoned) {
      try {
        if (request.matchedRideId) {
          await cancelAcceptedRideNowTrip(request.requestId, request.matchedRideId);
        } else {
          await cancelRideNowRequest(request.requestId);
        }
        closed++;
      } catch {
        // best-effort
      }
    }
    return closed;
  } catch {
    return 0;
  }
}

// ---------- History ----------

async function saveRideNowToHistory(request: RideNowRequest): Promise<void> {
  const historyRef = doc(rideHistoryCol);
  await setDoc(historyRef, {
    historyId: historyRef.id,
    rideType: "ride_now",
    requestId: request.requestId,
    passengerId: request.passengerId,
    passengerName: request.passengerName,
    riderId: request.matchedRiderId,
    riderName: request.matchedRiderName,
    riderPhone: request.matchedRiderPhone,
    pickup: request.pickup,
    destination: request.destination,
    tripTime: request.tripTime,
    timeMinutes: request.timeMinutes,
    status: "completed",
    createdAt: request.createdAt,
    completedAt: request.completedAt ?? Timestamp.now(),
  });
}

// ---------- Ratings / reports ----------

export async function submitRideRating(params: {
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
  const requestRef = doc(rideNowRequestsCol, params.requestId);

  await runTransaction(db, async (transaction) => {
    const userSnap = await transaction.get(userRef);
    const requestSnap = await transaction.get(requestRef);

    const alreadyRated = requestSnap.data()?.riderRated ?? false;
    const issueReported = requestSnap.data()?.issueReported ?? false;
    if (alreadyRated) throw new Error("You already rated this ride.");
    if (issueReported) throw new Error("You cannot rate after reporting an issue.");

    const oldAverage = (userSnap.data()?.ratingAverage as number) ?? 0;
    const oldCount = (userSnap.data()?.ratingCount as number) ?? 0;
    const newCount = oldCount + 1;
    const newAverage = (oldAverage * oldCount + params.stars) / newCount;

    transaction.set(ratingRef, {
      ratingId: ratingRef.id,
      requestId: params.requestId,
      rideId: "",
      passengerId: params.ratedBy,
      riderId: params.ratedTo,
      ratedBy: params.ratedBy,
      ratedTo: params.ratedTo,
      stars: params.stars,
      comment: params.comment,
      createdAt: Timestamp.now(),
    });

    transaction.update(userRef, {
      ratingAverage: newAverage,
      ratingCount: newCount,
    });

    transaction.update(requestRef, {
      riderRated: true,
      rating: params.stars,
      ratedAt: Timestamp.now(),
    });
  });
}

export async function submitPassengerRating(params: {
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
  const requestRef = doc(rideNowRequestsCol, params.requestId);

  await runTransaction(db, async (transaction) => {
    const userSnap = await transaction.get(userRef);
    const requestSnap = await transaction.get(requestRef);

    const alreadyRated = requestSnap.data()?.passengerRated ?? false;
    const issueReported = requestSnap.data()?.issueReported ?? false;
    if (alreadyRated) throw new Error("You already rated this passenger.");
    if (issueReported) throw new Error("You cannot rate after an issue was reported.");

    const oldAverage = (userSnap.data()?.ratingAverage as number) ?? 0;
    const oldCount = (userSnap.data()?.ratingCount as number) ?? 0;
    const newCount = oldCount + 1;
    const newAverage = (oldAverage * oldCount + params.stars) / newCount;

    transaction.set(ratingRef, {
      ratingId: ratingRef.id,
      requestId: params.requestId,
      rideId: "",
      passengerId: params.ratedTo,
      riderId: params.ratedBy,
      ratedBy: params.ratedBy,
      ratedTo: params.ratedTo,
      stars: params.stars,
      comment: params.comment,
      createdAt: Timestamp.now(),
    });

    transaction.update(userRef, {
      ratingAverage: newAverage,
      ratingCount: newCount,
    });

    transaction.update(requestRef, {
      passengerRated: true,
      passengerRating: params.stars,
      passengerRatedAt: Timestamp.now(),
    });
  });
}

export async function submitRideReport(params: {
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
  const requestRef = doc(rideNowRequestsCol, params.requestId);

  await runTransaction(db, async (transaction) => {
    const requestSnap = await transaction.get(requestRef);
    const alreadyReported = requestSnap.data()?.issueReported ?? false;
    const alreadyRated = requestSnap.data()?.riderRated ?? false;

    if (alreadyReported) throw new Error("You already reported this ride.");
    if (alreadyRated) throw new Error("You cannot report after submitting a rating.");

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
