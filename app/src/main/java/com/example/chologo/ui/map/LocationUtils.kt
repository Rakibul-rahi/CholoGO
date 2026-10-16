package com.example.chologo.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineResult

internal fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

/** A single fetch of the device's most recent known location - not a subscription. */
internal fun fetchOneShotLocation(
    context: Context,
    onFound: (LatLng) -> Unit,
    onUnavailable: () -> Unit
) {
    val engine = LocationEngineDefault.getDefaultLocationEngine(context)
    try {
        engine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                val location = result.lastLocation
                if (location != null) {
                    onFound(LatLng(location.latitude, location.longitude))
                } else {
                    onUnavailable()
                }
            }

            override fun onFailure(exception: Exception) {
                onUnavailable()
            }
        })
    } catch (e: SecurityException) {
        onUnavailable()
    }
}
