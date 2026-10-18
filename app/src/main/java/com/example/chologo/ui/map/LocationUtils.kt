package com.example.chologo.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.geometry.LatLng

internal fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

/**
 * A single fresh location fix - not a subscription. Asks the fused provider
 * for a current fix (which actually turns on GPS/network if needed) and only
 * falls back to the cached last-known location if that comes back empty.
 */
@SuppressLint("MissingPermission") // callers check hasLocationPermission first; SecurityException is caught below
internal fun fetchOneShotLocation(
    context: Context,
    onFound: (LatLng) -> Unit,
    onUnavailable: () -> Unit
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val request = CurrentLocationRequest.Builder()
        .setPriority(if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY)
        .setDurationMillis(15_000)
        .build()

    fun fallBackToLastLocation() {
        try {
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) onFound(LatLng(location.latitude, location.longitude)) else onUnavailable()
                }
                .addOnFailureListener { onUnavailable() }
        } catch (e: SecurityException) {
            onUnavailable()
        }
    }

    try {
        client.getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location != null) onFound(LatLng(location.latitude, location.longitude)) else fallBackToLastLocation()
            }
            .addOnFailureListener { fallBackToLastLocation() }
    } catch (e: SecurityException) {
        onUnavailable()
    }
}
