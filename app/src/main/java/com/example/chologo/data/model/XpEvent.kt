package com.example.chologo.data.model

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.TimeZone

/**
 * One immutable, evidence-backed row in a user's XP ledger.
 *
 * XP is a ledger rather than a counter on the user document, and that is
 * the whole security model. Firestore rules cannot bind a counter
 * increment to the thing that justified it: a rule permissive enough to
 * let a client add 10 to its own xp is permissive enough to let it add 10
 * a thousand times, and there is no way to express "only if a matching
 * event was written in the same breath". The old system did exactly this
 * - "xp" sat in the owner-writable key list on users/{uid} with no value
 * constraint at all, so any signed-in user could set their own XP to any
 * number straight from a Firestore client, and every one-shot guard in
 * the app was decoration on top of an open door.
 *
 * So no counter. A user's XP is the sum of their rows, and a row can only
 * exist if firestore.rules agreed to create it. Three things make one
 * unforgeable, all enforced server-side:
 *
 *  1. The document ID is *derived* ([XpRules.eventId]), so one
 *     achievement can only ever produce one row. A replayed claim is a
 *     create against an ID that already exists, which Firestore refuses -
 *     no app-side bookkeeping required, and it survives reinstalls and
 *     second devices.
 *  2. The [amount] is not the client's to pick. It must equal the
 *     canonical value for that reason and role.
 *  3. [sourceId] must point at a real document that actually proves the
 *     claim, in a state the claimer could not have reached on their own.
 *
 * Rows are create-only. Nobody updates or deletes them, ever.
 */
data class XpEvent(
    val eventId: String = "",

    val userId: String = "",

    /** One of [XpRules]' reason constants. */
    val reason: String = "",

    /** "rider" or "passenger" - which side of the trip earned this. */
    val role: String = "",

    /**
     * The document that proves this claim: a ride_requests id, a rides
     * id, or a ride_now_requests id depending on [reason]. Rules read it
     * back and re-check the evidence.
     */
    val sourceId: String = "",

    val amount: Long = 0L,

    val createdAt: Timestamp? = null
)

/**
 * The XP economy, in one place.
 *
 * IMPORTANT: every rule and amount here is mirrored in firestore.rules
 * under `match /xp_events/{eventId}`. The rules are the authority - this
 * object only exists so the app builds the same IDs and amounts the rules
 * will accept. Change one, change the other, or awards start failing.
 */
object XpRules {

    // ---- Reasons ----

    /**
     * Planning a day's travel. Small, and worth exactly once per travel
     * date: the day is part of the event ID, so editing, deleting and
     * re-saving a plan can never earn it twice - which the old
     * "+5 XP whenever a leg is newly created" award absolutely could,
     * since a still-pending plan is deletable.
     */
    const val REASON_PLAN_SAVED = "plan_saved"

    /**
     * A Tomorrow leg that reached "completed" - either through the normal
     * Start/Complete lifecycle, or reconstructed afterwards with both
     * sides agreeing it happened. Deliberately the same reason for both:
     * a trip is a trip, and sharing one ID space means the two paths can
     * never both pay out for the same leg.
     */
    const val REASON_TOMORROW_TRIP = "tomorrow_trip_completed"

    /** A Ride Now trip the passenger confirmed the completion of. */
    const val REASON_RIDE_NOW_TRIP = "ride_now_trip_completed"

    // ---- Roles ----

    const val ROLE_RIDER = "rider"
    const val ROLE_PASSENGER = "passenger"

    // ---- Amounts ----

    /**
     * Completing a trip is the only thing worth real XP, and the rider
     * earns more because they did the driving.
     *
     * Note what is NOT on this list: accepting a passenger. That used to
     * be worth 10 XP to the rider at the moment of acceptance, which two
     * accounts could farm indefinitely - accept, cancel, resubmit, accept
     * - without a wheel ever turning. Nothing pays out before a trip is
     * finished any more.
     */
    const val TRIP_XP_RIDER = 12L
    const val TRIP_XP_PASSENGER = 8L
    const val PLAN_SAVED_XP = 2L

    /** The canonical amount for a claim, or 0 if the pair is unknown. */
    fun amountFor(reason: String, role: String): Long {
        val isRider = role == ROLE_RIDER

        return when (reason) {
            REASON_PLAN_SAVED -> PLAN_SAVED_XP
            REASON_TOMORROW_TRIP, REASON_RIDE_NOW_TRIP ->
                if (isRider) TRIP_XP_RIDER else TRIP_XP_PASSENGER
            else -> 0L
        }
    }

    /**
     * The document ID for a claim. Everything that should collapse into a
     * single award goes in here, and nothing else does.
     *
     * [dedupeKey] is what actually caps the economy, and every reason uses
     * a key that contains a day, on purpose. Two accounts working together
     * can always walk a trip lifecycle end to end - no rule can tell a
     * staged commute from a real one - so the goal is not to make that
     * impossible but to make it pointless: a day of cheating is worth
     * exactly what a day of commuting is worth, and no more.
     *  - plan_saved uses the travel date, so planning a day is worth its
     *    2 XP once however many times the plan is rewritten
     *  - a Tomorrow trip uses date + direction, so no volume of fabricated
     *    requests beats the two legs a real commuter makes
     *  - a Ride Now trip uses its completion date + route, so repeating
     *    the same hop pays once a day
     */
    fun eventId(userId: String, reason: String, dedupeKey: String): String {
        return "${userId}__${reason}__$dedupeKey"
    }

    /** Dedupe key for [REASON_PLAN_SAVED]: one award per travel date. */
    fun planDedupeKey(rideDate: String): String = rideDate

    /**
     * Dedupe key for [REASON_TOMORROW_TRIP]: one award per travel date
     * per direction, so a user can earn at most a campus leg and a return
     * leg on any given day.
     */
    fun tomorrowTripDedupeKey(rideDate: String, tripDirection: String): String {
        return "${rideDate}__$tripDirection"
    }

    /**
     * Dedupe key for [REASON_RIDE_NOW_TRIP]: one award per completion day
     * per route.
     *
     * Ride Now has no travel date of its own to key on the way a Tomorrow
     * leg does, and keying on the request id alone left it as the one
     * uncapped hole in the economy - a pair of accounts could cycle
     * go-live, request, accept, start, confirm, complete, confirm in about
     * a minute and repeat it all evening. Bucketing by the day the trip
     * actually finished closes that: a genuine round trip is two different
     * routes and still earns twice, while the same hop over and over earns
     * once.
     *
     * Returns null when the trip has no completion time, which is not a
     * claimable state anyway.
     */
    fun rideNowTripDedupeKey(completedAt: Timestamp?, routeKey: String): String? {
        val dayKey = utcDayKey(completedAt) ?: return null
        return "${dayKey}__$routeKey"
    }

    /**
     * "yyyy-M-d" for a Firestore timestamp, in UTC and deliberately not
     * zero-padded.
     *
     * UTC because firestore.rules rebuilds this exact string from the
     * trip's own completedAt to check the ID, and rules timestamps are
     * always UTC - a local-time key here would disagree with them for
     * anyone in a non-zero offset (Dhaka is +6, so every trip finished
     * before 6 AM would land on the wrong day). Unpadded because rules'
     * string(int) doesn't pad either. Nobody ever sees this string; it
     * only has to be derived identically on both sides.
     */
    fun utcDayKey(timestamp: Timestamp?): String? {
        val date = timestamp?.toDate() ?: return null

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = date
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return "$year-$month-$day"
    }
}
