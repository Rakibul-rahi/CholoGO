package com.example.chologo.ui.map

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.data.repository.RideNowLocationRepository
import com.example.chologo.ui.passenger.AccentBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.maplibre.android.geometry.LatLng

private const val RIDER_UPDATE_INTERVAL_MS = 10_000L

private val RiderPinColor = Color(0xFF1E88E5)
private val PassengerPinColor = Color(0xFF43A047)

// Trips whose map already auto-opened in this app process. Lives outside
// composition so switching tabs (which disposes the Ride Now screen) doesn't
// pop the map up again; a closed map stays closed.
private val autoOpenedTrips = mutableSetOf<String>()

/**
 * Everything map-related for one matched Ride Now trip, for either side:
 *
 *  - shares this side's position for as long as it's composed (see
 *    [RideNowLocationSharing]), whether or not the map itself is open;
 *  - a "View live map" button that opens [RideNowLiveMap] full screen;
 *  - opens that full-screen map by itself, once per trip, the moment the
 *    trip is ACCEPTED - so a fresh match lands both people on the map.
 *
 * Callers mount this only while the trip is in MATCHED_STATUSES, so leaving
 * that range is also what stops and clears the location sharing.
 */
@Composable
fun RideNowMapSection(
    requestId: String,
    isRider: Boolean,
    status: String,
    otherPartyName: String,
    modifier: Modifier = Modifier
) {
    if (requestId.isBlank()) return

    RideNowLocationSharing(requestId = requestId, isRider = isRider)

    var showFullScreen by rememberSaveable(requestId) { mutableStateOf(false) }

    // Only on ACCEPTED, and only once per trip - later stages (started,
    // ongoing...) never pop it up again.
    LaunchedEffect(requestId, status) {
        if (status == RideNowStatus.ACCEPTED && autoOpenedTrips.add(requestId)) {
            showFullScreen = true
        }
    }

    val accent = AccentBlue
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable { showFullScreen = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "View live map",
                color = accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }

    if (showFullScreen) {
        RideNowFullScreenMap(
            requestId = requestId,
            isRider = isRider,
            otherPartyName = otherPartyName,
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
private fun RideNowFullScreenMap(
    requestId: String,
    isRider: Boolean,
    otherPartyName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                RideNowLiveMap(
                    requestId = requestId,
                    isRider = isRider,
                    otherPartyName = otherPartyName,
                    fullScreen = true,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .systemBarsPadding()
                        .padding(12.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Live trip map", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.size(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                LegendDot(RiderPinColor, if (isRider) "You (rider)" else "Rider")
                                LegendDot(PassengerPinColor, if (isRider) "Passenger" else "You (passenger)")
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close map")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Pushes this side's own position for one trip, with no UI of its own.
 *
 * Rider: a fresh position roughly every 10s for as long as this stays
 * composed. Passenger: once, the moment it's composed. Either side's shared
 * position is cleared the instant this leaves composition, so nothing
 * lingers once the trip moves past the matched statuses.
 */
@Composable
fun RideNowLocationSharing(
    requestId: String,
    isRider: Boolean,
    repository: RideNowLocationRepository = remember { RideNowLocationRepository() }
) {
    val context = LocalContext.current

    var hasLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }

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
}

/**
 * The map for one matched Ride Now trip: listens to both sides' shared
 * positions and draws them as colored pins (rider blue, passenger green),
 * framing both once both are known. Read-only - sharing this side's own
 * position is [RideNowLocationSharing]'s job.
 */
@Composable
fun RideNowLiveMap(
    requestId: String,
    isRider: Boolean,
    modifier: Modifier = Modifier,
    otherPartyName: String = "",
    fullScreen: Boolean = false,
    repository: RideNowLocationRepository = remember { RideNowLocationRepository() }
) {
    var riderLocation by remember { mutableStateOf<LatLng?>(null) }
    var passengerLocation by remember { mutableStateOf<LatLng?>(null) }

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

    val other = otherPartyName.ifBlank { null }
    val riderLabel = if (isRider) "You (rider)" else other?.let { "Rider: $it" } ?: "Rider"
    val passengerLabel = if (isRider) other?.let { "Passenger: $it" } ?: "Passenger" else "You (passenger)"

    val extraMarkers = buildList {
        riderLocation?.let { add(MapPin(riderLabel, it, RiderPinColor)) }
        passengerLocation?.let { add(MapPin(passengerLabel, it, PassengerPinColor)) }
    }

    CampusMapScreen(
        modifier = modifier,
        extraMarkers = extraMarkers,
        focusOn = riderLocation ?: passengerLocation,
        fullScreen = fullScreen
    )
}
