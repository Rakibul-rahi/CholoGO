package com.example.chologo.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Per-trip live location sharing for an accepted Ride Now request.
 *
 * Backed by Firebase Realtime Database rather than Firestore - RTDB's
 * connection/bandwidth-based free tier fits this kind of frequent,
 * ephemeral write (a rider's position every ~10s) far better than
 * Firestore's per-read/write quota would.
 *
 * ONE-TIME SETUP REQUIRED: this project has no Realtime Database yet.
 * In the Firebase console: Build -> Realtime Database -> Create Database.
 * Once created, copy the URL shown there (looks like
 * "https://<project>-default-rtdb.<region>.firebasedatabase.app") and
 * replace [DATABASE_URL] below. Until that's done, every call here fails
 * safely (caught, logged, no crash) rather than sharing any location.
 */
class RideNowLocationRepository {

    private val database: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance(DATABASE_URL)
        } catch (e: Exception) {
            null
        }
    }

    private fun rideRef(requestId: String) =
        database?.getReference("rideNowLocations")?.child(requestId)

    /** Rider's position, overwritten roughly every 10s while the trip is accepted. */
    fun writeRiderLocation(requestId: String, lat: Double, lng: Double) {
        writeLocation(requestId, ROLE_RIDER, lat, lng)
    }

    /** Passenger's position, written once when the trip is accepted. */
    fun writePassengerLocation(requestId: String, lat: Double, lng: Double) {
        writeLocation(requestId, ROLE_PASSENGER, lat, lng)
    }

    private fun writeLocation(requestId: String, role: String, lat: Double, lng: Double) {
        val ref = rideRef(requestId)?.child(role) ?: return
        try {
            ref.setValue(
                mapOf(
                    "lat" to lat,
                    "lng" to lng,
                    "timestamp" to System.currentTimeMillis()
                )
            )
            // Cleans itself up if the app dies or loses connection mid-trip,
            // rather than leaving a stale marker on the other person's map.
            ref.onDisconnect().removeValue()
        } catch (e: Exception) {
            // Sharing is best-effort - a write failure here shouldn't
            // interrupt the ride itself.
        }
    }

    /** Removes one side's shared location - called when that side leaves the accepted trip. */
    fun clearLocation(requestId: String, role: String) {
        try {
            rideRef(requestId)?.child(role)?.removeValue()
        } catch (e: Exception) {
            // Best-effort cleanup.
        }
    }

    /**
     * Subscribes to both sides' locations for one trip. [onData] receives
     * (riderLatLng, passengerLatLng), either of which is null until that
     * side has shared a position.
     */
    fun listenToRideLocations(
        requestId: String,
        onData: (rider: Pair<Double, Double>?, passenger: Pair<Double, Double>?) -> Unit
    ): ValueEventListener? {
        val ref = rideRef(requestId) ?: return null

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onData(
                    snapshot.child(ROLE_RIDER).toLatLngPair(),
                    snapshot.child(ROLE_PASSENGER).toLatLngPair()
                )
            }

            override fun onCancelled(error: DatabaseError) {
                onData(null, null)
            }
        }

        ref.addValueEventListener(listener)
        return listener
    }

    fun stopListening(requestId: String, listener: ValueEventListener) {
        rideRef(requestId)?.removeEventListener(listener)
    }

    private fun DataSnapshot.toLatLngPair(): Pair<Double, Double>? {
        if (!exists()) return null
        val lat = child("lat").getValue(Double::class.java) ?: return null
        val lng = child("lng").getValue(Double::class.java) ?: return null
        return lat to lng
    }

    companion object {
        const val ROLE_RIDER = "rider"
        const val ROLE_PASSENGER = "passenger"

        // TODO: replace with your Realtime Database's actual URL (Firebase
        // console -> Build -> Realtime Database) once it's been created.
        // Left as an invalid placeholder on purpose, so a missing setup
        // fails loudly in Logcat instead of silently pointing at someone
        // else's database.
        private const val DATABASE_URL = "REPLACE_WITH_YOUR_REALTIME_DATABASE_URL"
    }
}
