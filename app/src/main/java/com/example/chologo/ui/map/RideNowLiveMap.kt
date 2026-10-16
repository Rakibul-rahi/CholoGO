package com.example.chologo.ui.map

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.chologo.data.repository.RideNowLocationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.maplibre.android.geometry.LatLng

private const val RIDER_UPDATE_INTERVAL_MS = 10_000L

/**
 * Live location sharing + map for one accepted Ride Now trip.
 *
 * Rider: shares a fresh position roughly every 10s for as long as this
 * stays on screen (mounted only while the trip is ACCEPTED - see
 * RiderRideNowScreen/PassengerRideNowScreen). Passenger: shares their
 * position once, the moment the trip is accepted. Either side's shared
 * position is cleared the instant this leaves composition, so nothing
 * lingers once the trip moves past accepted.
 */
@Composable
fun RideNowLiveMap(
    requestId: String,
    isRider: Boolean,
    modifier: Modifier = Modifier,
    repository: RideNowLocationRepository = remember { RideNowLocationRepository() }
) {
    val context = LocalContext.current

    var hasLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var riderLocation by remember { mutableStateOf<LatLng?>(null) }
    var passengerLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Subscribes to both sides' shared positions for this specific trip.
    DisposableEffect(requestId) {
        val listener = repository.listenToRideLocations(requestId) { rider, passenger ->
            riderLocation = rider?.let { (lat, lng) -> LatLng(lat, lng) }
            passengerLocation = passenger?.let { (lat, lng) -> LatLng(lat, lng) }
        }

        onDispose {
            if (listener != null) {
                repository.stopListening(requestId, listener)
            }
        }
    }

    // Pushes this side's own position: on a loop for the rider, once for
    // the passenger.
    LaunchedEffect(requestId, hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect

        if (isRider) {
            while (isActive) {
                fetchOneShotLocation(
                    context = context,
                    onFound = { latLng ->
                        repository.writeRiderLocation(requestId, latLng.latitude, latLng.longitude)
                    },
                    onUnavailable = { }
                )
                delay(RIDER_UPDATE_INTERVAL_MS)
            }
        } else {
            fetchOneShotLocation(
                context = context,
                onFound = { latLng ->
                    repository.writePassengerLocation(requestId, latLng.latitude, latLng.longitude)
                },
                onUnavailable = {
                    Toast.makeText(
                        context,
                        "Couldn't share your location yet - try again in a moment",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    // Stops sharing this side's position the moment this trip stops being
    // shown (the request left ACCEPTED, or the screen was left).
    DisposableEffect(requestId) {
        onDispose {
            val role = if (isRider) {
                RideNowLocationRepository.ROLE_RIDER
            } else {
                RideNowLocationRepository.ROLE_PASSENGER
            }
            repository.clearLocation(requestId, role)
        }
    }

    val extraMarkers = buildMap {
        riderLocation?.let { put("Rider", it) }
        passengerLocation?.let { put("Passenger", it) }
    }

    CampusMapScreen(
        modifier = modifier,
        extraMarkers = extraMarkers,
        focusOn = riderLocation ?: passengerLocation
    )
}
