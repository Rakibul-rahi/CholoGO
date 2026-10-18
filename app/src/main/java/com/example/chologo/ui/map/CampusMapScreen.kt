package com.example.chologo.ui.map

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.chologo.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File

// Ahsanullah University of Science and Technology, Tejgaon, Dhaka.
private const val AustLat = 23.7508
private const val AustLng = 90.3935

private const val PmtilesAssetName = "dhaka.pmtiles"
private const val PmtilesUrlPlaceholder = "__PMTILES_URL__"

private val CardHeight = 420.dp

/** Padding (px) kept around the pins when the camera frames all of them. */
private const val FitAllPaddingPx = 160

/** A named pin drawn on top of the base map, optionally in its own color. */
data class MapPin(
    val label: String,
    val position: LatLng,
    val color: Color? = null
)

/**
 * Renders a fully offline map of Dhaka - the street/building/water geometry
 * comes from a Dhaka-only [PmtilesAssetName] archive bundled in assets/,
 * copied once to internal storage since PMTiles needs random byte-range
 * file reads that AssetManager can't do. Text labels use a public glyph CDN
 * and so only render when online; the base map itself needs no network.
 *
 * Tapping the map shows the viewer's current position as a marker
 * (requesting location permission the first time, if needed) - it's a
 * single on-demand fix per tap, not continuous background tracking.
 *
 * [extraMarkers] overlays additional named pins (e.g. an accepted Ride Now
 * trip's rider/passenger positions) alongside the AUST marker. The camera
 * centers on [focusOn] once, the first time it becomes available, and
 * frames every extra pin once, the first time two or more are known - then
 * leaves the user free to pan.
 *
 * [fullScreen] drops the fixed card height and rounded border so the map
 * fills whatever space [modifier] gives it.
 */
@Composable
fun CampusMapScreen(
    modifier: Modifier = Modifier,
    extraMarkers: List<MapPin> = emptyList(),
    focusOn: LatLng? = null,
    fullScreen: Boolean = false
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val border = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.08f)

    var offlineMapPath by remember { mutableStateOf<String?>(null) }
    var copyFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        offlineMapPath = try {
            withContext(Dispatchers.IO) { copyPmtilesIfNeeded(context) }
        } catch (e: Exception) {
            copyFailed = true
            null
        }
    }

    val cardModifier = if (fullScreen) {
        modifier
    } else {
        modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(RoundedCornerShape(18.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(18.dp))
    }

    val mapPath = offlineMapPath

    if (mapPath == null) {
        Box(modifier = cardModifier.background(Color(0xFF1C2228)), contentAlignment = Alignment.Center) {
            if (!copyFailed) {
                CircularProgressIndicator()
            }
        }
        return
    }

    var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var hasLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    // Bumped on every tap so each tap re-triggers the fetch below (a plain
    // boolean stays true after the first tap and never fires again) -
    // there's no continuous tracking, just one fix per tap.
    var locationTaps by remember { mutableStateOf(0) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { view ->
            view.getMapAsync { map ->
                maplibreMap = map
                map.addOnMapClickListener {
                    locationTaps++
                    true
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        // The MapView is created well after the host Activity's own
        // onCreate/onStart/onResume already fired, so this observer would
        // never see those - replay them once up front instead of relying
        // on the (already-past) lifecycle events.
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // A tap before permission is granted asks for it.
    LaunchedEffect(locationTaps, hasLocationPermission) {
        if (locationTaps > 0 && !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Once a tap has happened and permission is in hand, fetch a single
    // location fix - not a subscription, just one request per tap.
    LaunchedEffect(locationTaps, hasLocationPermission) {
        if (locationTaps > 0 && hasLocationPermission) {
            fetchOneShotLocation(
                context = context,
                onFound = { latLng -> userLocation = latLng },
                onUnavailable = {
                    Toast.makeText(context, "Couldn't get your location yet - try again in a moment", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Tracks whether the initial auto-focus (onto `focusOn`, once it's
    // available) has already happened, so later updates re-draw markers
    // without yanking the camera away from wherever the user has panned to.
    var hasAutoFocused by remember { mutableStateOf(false) }
    // Same idea for framing every extra pin at once (e.g. rider + passenger).
    var hasFitAllPins by remember { mutableStateOf(false) }

    // The style callback below fires asynchronously, so it reads the latest
    // values through these rather than whatever the effect captured.
    val currentUserLocation by rememberUpdatedState(userLocation)
    val currentExtraMarkers by rememberUpdatedState(extraMarkers)
    val currentFocusOn by rememberUpdatedState(focusOn)

    fun autoFocus(map: MapLibreMap) {
        val pins = currentExtraMarkers
        val target = currentFocusOn
        if (!hasFitAllPins && pins.size >= 2) {
            val bounds = LatLngBounds.Builder().includes(pins.map { it.position }).build()
            val update = if (bounds.latitudeSpan < 0.0005 && bounds.longitudeSpan < 0.0005) {
                // Practically the same spot - bounds would zoom in absurdly far.
                CameraUpdateFactory.newLatLngZoom(bounds.center, 17.0)
            } else {
                CameraUpdateFactory.newLatLngBounds(bounds, FitAllPaddingPx)
            }
            map.animateCamera(update)
            hasFitAllPins = true
            hasAutoFocused = true
        } else if (!hasAutoFocused && target != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16.0))
            hasAutoFocused = true
        }
    }

    // Loads/reloads the style (initial load, and again on a theme flip) and
    // re-adds every marker (AUST, the "you are here" pin, and extraMarkers).
    LaunchedEffect(maplibreMap, isDark, mapPath) {
        val map = maplibreMap ?: return@LaunchedEffect
        val styleJson = loadStyleJson(context, isDark, mapPath)
        if (!hasAutoFocused) {
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(AustLat, AustLng))
                .zoom(15.0)
                .build()
        }
        map.setStyle(Style.Builder().fromJson(styleJson)) {
            refreshMarkers(context, map, currentUserLocation, currentExtraMarkers)
            autoFocus(map)
        }
    }

    // A fix arrived (or changed) while the style was already loaded - place
    // the marker and center on it without reloading the whole style.
    LaunchedEffect(userLocation, maplibreMap) {
        val map = maplibreMap
        val location = userLocation
        if (map != null && map.style != null && location != null) {
            refreshMarkers(context, map, location, extraMarkers)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16.0))
        }
    }

    // extraMarkers changing (e.g. a rider's position ticking every ~10s)
    // just redraws pins - the camera only moves the first time focusOn (or
    // a second pin) shows up, never on every subsequent update.
    LaunchedEffect(extraMarkers, maplibreMap) {
        val map = maplibreMap
        if (map != null && map.style != null) {
            refreshMarkers(context, map, userLocation, extraMarkers)
            autoFocus(map)
        }
    }

    AndroidView(
        modifier = cardModifier,
        factory = { mapView }
    )
}

private fun refreshMarkers(
    context: Context,
    map: MapLibreMap,
    userLocation: LatLng?,
    extraMarkers: List<MapPin>
) {
    map.clear()
    map.addMarker(
        MarkerOptions()
            .position(LatLng(AustLat, AustLng))
            .title("AUST")
    )
    if (userLocation != null) {
        map.addMarker(
            MarkerOptions()
                .position(userLocation)
                .title("You are here")
        )
    }
    extraMarkers.forEach { pin ->
        val options = MarkerOptions()
            .position(pin.position)
            .title(pin.label)
        pin.color?.let { options.icon(IconFactory.getInstance(context).fromBitmap(dotBitmap(context, it))) }
        map.addMarker(options)
    }
}

/** A filled circle with a white ring - a simple, clearly colored map pin. */
private fun dotBitmap(context: Context, color: Color): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(center, center, center, paint)
    paint.color = color.toArgb()
    canvas.drawCircle(center, center, center - 4 * density, paint)
    return bitmap
}

private fun copyPmtilesIfNeeded(context: Context): String {
    val destination = File(context.filesDir, PmtilesAssetName)
    if (!destination.exists() || destination.length() == 0L) {
        context.assets.open(PmtilesAssetName).use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return destination.absolutePath
}

private fun loadStyleJson(context: Context, isDark: Boolean, pmtilesPath: String): String {
    val assetName = if (isDark) "dhaka_style_dark.json" else "dhaka_style_light.json"
    val raw = context.assets.open(assetName).bufferedReader().use { it.readText() }
    return raw.replace(PmtilesUrlPlaceholder, "pmtiles://file://$pmtilesPath")
}
