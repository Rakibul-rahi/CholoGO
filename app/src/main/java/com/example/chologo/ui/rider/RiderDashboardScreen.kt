@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.rider

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.RideRequestStatus
import com.example.chologo.data.model.VehicleType
import com.example.chologo.data.model.seatCapacity
import com.example.chologo.data.model.seatSummary
import com.example.chologo.navigation.Screen
import com.example.chologo.notifications.TomorrowRideReminderScheduler
import com.example.chologo.data.repository.XpRepository
import com.example.chologo.ui.common.CancelRideDialog
import com.example.chologo.ui.common.CholoGoTabRow
import com.example.chologo.ui.common.CholoGoTopBar
import com.example.chologo.ui.common.RatingDialog
import com.example.chologo.ui.common.rememberNotificationPermissionRequester
import com.example.chologo.ui.components.LevelCard
import com.example.chologo.ui.components.LocalAdCarouselBanner
import com.example.chologo.ui.theme.LocalIsDarkTheme
import com.example.chologo.utils.LevelSystem
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.viewmodel.TomorrowMatchedRequest
import com.example.chologo.viewmodel.TomorrowRideUiState
import com.example.chologo.viewmodel.TomorrowRideViewModel

private val DashboardBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0A0D0F) else Color(0xFFF7F9FA)

/** Short badge text for where one passenger's trip currently sits. */
private fun String.passengerStatusLabel(): String {
    return when (this) {
        RideRequestStatus.START_PENDING_CONFIRMATION -> "Starting"
        RideRequestStatus.ONGOING -> "On trip"
        RideRequestStatus.END_PENDING_CONFIRMATION -> "Ending"
        RideRequestStatus.COMPLETED -> "Done"
        else -> "Accepted"
    }
}

@Composable
private fun String.passengerStatusAccent(): Color {
    return when (this) {
        RideRequestStatus.ONGOING -> AccentBlue
        RideRequestStatus.COMPLETED -> AccentEmerald
        RideRequestStatus.START_PENDING_CONFIRMATION,
        RideRequestStatus.END_PENDING_CONFIRMATION -> AccentAmber
        else -> AccentEmerald
    }
}

private fun openDialer(context: Context, phoneNumber: String) {
    if (phoneNumber.isBlank() || phoneNumber == "N/A") {
        Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
    }

    context.startActivity(intent)
}

@Composable
fun RiderDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    tomorrowRideViewModel: TomorrowRideViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    val authState by authViewModel.uiState.collectAsState()
    val tomorrowUiState by tomorrowRideViewModel.uiState.collectAsState()

    val xpRepository = remember { XpRepository() }

    var riderXp by remember { mutableStateOf(0L) }
    var isLevelLoading by remember { mutableStateOf(true) }

    val tomorrowDate = remember { getTomorrowDateKey() }

    var tomorrowRatingTarget by remember { mutableStateOf<RideRequest?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser()
    }

    // XP is derived from the ledger, not read off the user document -
    // users/{uid}.xp is no longer written by anything, and where it
    // survives on older accounts it holds whatever the previous system
    // let people put there. A listener means the card moves the moment an
    // award lands, so nothing has to hand a refreshed number back up the
    // tree any more.
    DisposableEffect(authState.userId) {
        if (authState.userId.isBlank()) {
            isLevelLoading = false
            return@DisposableEffect onDispose { }
        }

        val registration = xpRepository.listenTotalXp(
            userId = authState.userId,
            onData = { total ->
                riderXp = total
                isLevelLoading = false
            },
            onError = {
                isLevelLoading = false
            }
        )

        onDispose { registration.remove() }
    }

    // Pays out anything owed for trips that finished while this rider
    // wasn't looking - the passenger is the one who confirms a completion,
    // so the rider's half is always claimed after the fact.
    LaunchedEffect(authState.userId) {
        if (authState.userId.isNotBlank()) {
            xpRepository.claimTripXpFor(authState.userId, isRider = true)
        }
    }

    // Starts the live listeners for saved rides + pending passenger
    // requests as soon as the rider's uid is available. RiderTomorrowSetupTab
    // also calls this on its own with the same riderId/date - both calls
    // share this one TomorrowRideViewModel instance, so it's harmless
    // (start() always tears down its previous listener before starting a
    // new one, so this just re-attaches the same live data).
    LaunchedEffect(authState.userId) {
        if (authState.userId.isNotBlank()) {
            tomorrowRideViewModel.startRiderSession(authState.userId, tomorrowDate)
        }
    }

    LaunchedEffect(tomorrowUiState.errorMessage) {
        tomorrowUiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            tomorrowRideViewModel.clearMessage()
        }
    }

    LaunchedEffect(tomorrowUiState.successMessage) {
        tomorrowUiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            tomorrowRideViewModel.clearMessage()
        }
    }

    val levelInfo = remember(riderXp) {
        LevelSystem.getLevelInfo(riderXp)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBg),
        color = DashboardBg
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                CholoGoTopBar(
                    onLogoClick = {
                        navController.navigate(Screen.RiderHome.route) {
                            popUpTo(Screen.RiderHome.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onRideHistoryClick = {
                        navController.navigate(Screen.RideHistory.createRoute("rider"))
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.createRoute("rider")) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                LevelCard(
                    level = if (isLevelLoading) 1 else levelInfo.level,
                    levelTitle = if (isLevelLoading) "New Rider" else levelInfo.levelTitle,
                    currentXp = if (isLevelLoading) 0L else riderXp,
                    xpNeededForNextLevel = if (isLevelLoading) 150L else levelInfo.xpNeededForNextLevel,
                    progress = if (isLevelLoading) 0f else levelInfo.progressFraction,
                    userName = authState.userName.ifBlank { "Rider" }
                )
            }

            item {
                LocalAdCarouselBanner()
            }

            item {
                CholoGoTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (
                                fadeIn(animationSpec = tween(260)) +
                                        slideInVertically(animationSpec = tween(260)) { it / 10 }
                                ) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "rider_tab_content"
                ) { tab ->
                    Box {
                        when (tab) {
                            0 -> RiderRideNowScreen(
                                navController = navController
                            )

                            1 -> RiderTomorrowDashboardContent(
                                uiState = tomorrowUiState,
                                tomorrowDate = tomorrowDate,
                                riderId = authState.userId,
                                riderName = authState.userName,
                                vehicleType = authState.userVehicleType,
                                vehicleModel = authState.userVehicleModel,
                                vehicleNumber = authState.userVehicleNumber,
                                vehicleColor = authState.userVehicleColor,
                                tomorrowRideViewModel = tomorrowRideViewModel,
                                onAccept = { match ->
                                    tomorrowRideViewModel.acceptRequest(
                                        match = match,
                                        riderId = authState.userId,
                                        riderName = authState.userName.ifBlank { "Rider" },
                                        riderPhone = authState.userPhone.ifBlank { "N/A" }
                                    )
                                },
                                onDecline = { match ->
                                    tomorrowRideViewModel.declineRequest(
                                        match = match,
                                        riderId = authState.userId
                                    )
                                },
                                onRemoveRide = { ride ->
                                    tomorrowRideViewModel.removeRiderRide(ride.rideId)
                                },
                                onCancelRide = { request, reason ->
                                    tomorrowRideViewModel.cancelAcceptedRideAsRider(
                                        request = request,
                                        riderId = authState.userId,
                                        reason = reason
                                    )
                                },
                                onStartTrip = { request ->
                                    tomorrowRideViewModel.startTripAsRider(
                                        requestId = request.requestId,
                                        riderId = authState.userId
                                    )
                                },
                                onCompleteTrip = { request ->
                                    tomorrowRideViewModel.requestTripCompletionAsRider(
                                        requestId = request.requestId,
                                        riderId = authState.userId
                                    )
                                },
                                onRatePassenger = { request ->
                                    tomorrowRatingTarget = request
                                },
                                onSaveSuccess = {
                                    // Nothing to do: the ride list and the
                                    // XP total are both live listeners.
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val ratingTarget = tomorrowRatingTarget
    if (ratingTarget != null) {
        RatingDialog(
            subject = "Passenger",
            onDismiss = {
                tomorrowRatingTarget = null
            },
            onSubmit = { stars, comment ->
                tomorrowRideViewModel.submitTomorrowPassengerRating(
                    request = ratingTarget,
                    ratedBy = authState.userId,
                    ratedTo = ratingTarget.userId,
                    stars = stars,
                    comment = comment
                )

                tomorrowRatingTarget = null
            }
        )
    }
}

@Composable
private fun RiderTomorrowDashboardContent(
    uiState: TomorrowRideUiState,
    tomorrowDate: String,
    riderId: String,
    riderName: String,
    vehicleType: String,
    vehicleModel: String,
    vehicleNumber: String,
    vehicleColor: String,
    tomorrowRideViewModel: TomorrowRideViewModel,
    onAccept: (TomorrowMatchedRequest) -> Unit,
    onDecline: (TomorrowMatchedRequest) -> Unit,
    onRemoveRide: (Ride) -> Unit,
    onCancelRide: (RideRequest, String) -> Unit,
    onStartTrip: (RideRequest) -> Unit,
    onCompleteTrip: (RideRequest) -> Unit,
    onRatePassenger: (RideRequest) -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    var scheduledReminderKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Same "1 hour before, is this still happening?" reminder as the
    // passenger side, for any ride that actually has someone riding along.
    //
    // This keys off accepted requests rather than status != "active",
    // because a car only reaches "full" once its LAST seat goes - a 4-seat
    // car with two passengers booked is still "active", and the rider very
    // much needs reminding about it.
    LaunchedEffect(uiState.savedRides, uiState.acceptedRequestsForRider) {
        val passengersByRide = uiState.acceptedRequestsForRider.groupBy { it.matchedRideId }
        val matchedRides = uiState.savedRides.filter { !passengersByRide[it.rideId].isNullOrEmpty() }
        val currentKeys = matchedRides.map { "rider_${it.rideId}" }.toSet()

        (scheduledReminderKeys - currentKeys).forEach { staleKey ->
            TomorrowRideReminderScheduler.cancelReminder(context, staleKey)
        }

        if (matchedRides.isNotEmpty()) {
            requestNotificationPermission()
        }

        matchedRides.forEach { ride ->
            val passengers = passengersByRide[ride.rideId].orEmpty()
            val directionLabel = if (ride.tripDirection == "to_campus") "to campus" else "back home"

            val passengerLabel = when (passengers.size) {
                0 -> "your passenger"
                1 -> passengers.first().passengerName.ifBlank { "your passenger" }
                else -> "${passengers.size} passengers"
            }

            TomorrowRideReminderScheduler.scheduleReminder(
                context = context,
                uniqueKey = "rider_${ride.rideId}",
                rideDate = ride.rideDate,
                timeMinutes = ride.timeMinutes,
                title = "Tomorrow Ride reminder",
                message = "Your ride $directionLabel with $passengerLabel at ${ride.tripTime} is in 1 hour. Still happening?"
            )
        }

        scheduledReminderKeys = currentKeys
    }

    // The "passenger available for your route" push is now sent
    // server-side (see TomorrowRideViewModel.savePassengerPlan ->
    // notifyMatchingRiders / server/src/index.ts's notify-match endpoint)
    // right when the passenger submits a matching request - it reaches a
    // backgrounded or killed app too, and the server dedups per
    // (ride, request) pair itself. A local-only effect here would
    // double-notify a rider whose app happens to be open.

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RiderTomorrowIntroCard(vehicleType = vehicleType)

        // The "did this ride happen?" review for a leg that was never
        // marked finished lives in Ride History now, not here - see
        // TomorrowRideViewModel.startMissedRideReview.
        RiderRequestsTab(
            uiState = uiState,
            vehicleType = vehicleType,
            onAccept = onAccept,
            onDecline = onDecline,
            onRemoveRide = onRemoveRide,
            onCancelRide = onCancelRide,
            onStartTrip = onStartTrip,
            onCompleteTrip = onCompleteTrip,
            onRatePassenger = onRatePassenger
        )

        RiderTomorrowSetupTab(
            riderId = riderId,
            rideDate = tomorrowDate,
            riderName = riderName,
            vehicleType = vehicleType,
            vehicleModel = vehicleModel,
            vehicleNumber = vehicleNumber,
            vehicleColor = vehicleColor,
            tomorrowRideViewModel = tomorrowRideViewModel,
            onSaveSuccess = onSaveSuccess
        )
    }
}

@Composable
private fun RiderTomorrowIntroCard(vehicleType: String) {
    RiderSectionCard(
        title = "Tomorrow Rides",
        subtitle = "Set your route for tomorrow and accept matched passenger requests.",
        icon = "📅"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Schedule rides", accent = AccentBlue)
            MiniBadge(text = VehicleType.label(vehicleType), accent = Lime)
            MiniBadge(text = "Earn XP", accent = AccentEmerald)
        }
    }
}

@Composable
fun RiderRequestsTab(
    uiState: TomorrowRideUiState,
    vehicleType: String,
    onAccept: (TomorrowMatchedRequest) -> Unit,
    onDecline: (TomorrowMatchedRequest) -> Unit,
    onRemoveRide: (Ride) -> Unit,
    onCancelRide: (RideRequest, String) -> Unit,
    onStartTrip: (RideRequest) -> Unit,
    onCompleteTrip: (RideRequest) -> Unit,
    onRatePassenger: (RideRequest) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RiderSectionCard(
            title = "Passenger Matches",
            subtitle = "Requests that match your saved tomorrow ride route and time.",
            icon = "🙋"
        ) {
            if (uiState.matchedRequestsForRider.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.DirectionsCar,
                    message = "No matched requests right now.\nCheck back after setting up your tomorrow ride."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.matchedRequestsForRider.forEach { request ->
                        MatchedRequestCard(
                            request = request,
                            isProcessing = uiState.processingRequestIds.contains(request.requestId),
                            onAccept = {
                                onAccept(request)
                            },
                            onDecline = {
                                onDecline(request)
                            }
                        )
                    }
                }
            }
        }

        RiderSectionCard(
            title = "Saved Tomorrow Rides",
            subtitle = "Your active rides for tomorrow, and who's riding along.",
            icon = VehicleType.emoji(vehicleType)
        ) {
            if (uiState.savedRides.isEmpty()) {
                EmptyStateCard(
                    icon = if (VehicleType.isCar(vehicleType)) {
                        Icons.Default.DirectionsCar
                    } else {
                        Icons.Default.TwoWheeler
                    },
                    message = "No rides saved yet.\nUse the setup form below to create your tomorrow ride."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.savedRides.forEach { ride ->
                        // A car can carry several accepted passengers on the
                        // same leg, so this is a list rather than the single
                        // match a bike could ever have.
                        val matchedRequests = uiState.acceptedRequestsForRider
                            .filter { it.matchedRideId == ride.rideId }
                            .sortedBy { it.passengerName.lowercase() }

                        SavedRideCard(
                            ride = ride,
                            matchedRequests = matchedRequests,
                            processingRequestIds = uiState.processingRequestIds,
                            onRemove = {
                                if (ride.rideId.isNotBlank()) {
                                    onRemoveRide(ride)
                                }
                            },
                            onCancel = { request, reason -> onCancelRide(request, reason) },
                            onStartTrip = { request -> onStartTrip(request) },
                            onCompleteTrip = { request -> onCompleteTrip(request) },
                            onRatePassenger = { request -> onRatePassenger(request) }
                        )
                    }
                }
            }
        }

        InfoBannerCard(
            message = "Ride Now is available in the first tab. Tomorrow rides are managed here."
        )
    }
}

@Composable
fun MatchedRequestCard(
    request: TomorrowMatchedRequest,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBase)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    LimeGlow,
                                    AccentEmerald.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Lime.copy(alpha = 0.2f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.passengerName.take(1).uppercase(),
                        color = Lime,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = request.passengerName,
                        color = TextHigh,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = request.tripTime,
                        color = TextMed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            DirectionBadge(request.tripDirection)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${request.pickup} → ${request.destination}",
            color = TextMed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = if (isProcessing) {
                            Brush.linearGradient(listOf(LimeDim, LimeDim))
                        } else {
                            GradientLime
                        }
                    )
                    .clickable(enabled = !isProcessing) {
                        onAccept()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = BgDeep,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BgDeep,
                            modifier = Modifier.size(16.dp)
                        )

                        Text(
                            text = "Accept",
                            color = BgDeep,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated)
                    .clickable(enabled = !isProcessing) {
                        onDecline()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = TextMed,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = "Decline",
                        color = TextMed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * One saved leg, plus every passenger already riding along on it.
 *
 * A bike fills on its first accept, so this used to only ever have a single
 * matched request. A car can carry several, each running its own
 * start/complete/cancel lifecycle independently, so passengers render as a
 * list of self-contained rows rather than one inline block.
 */
@Composable
fun SavedRideCard(
    ride: Ride,
    matchedRequests: List<RideRequest>,
    processingRequestIds: Set<String>,
    onRemove: () -> Unit,
    onCancel: (RideRequest, String) -> Unit,
    onStartTrip: (RideRequest) -> Unit,
    onCompleteTrip: (RideRequest) -> Unit,
    onRatePassenger: (RideRequest) -> Unit
) {
    val isCampus = ride.tripDirection.equals("to_campus", ignoreCase = true)
    val accentColor = if (isCampus) AccentBlue else AccentEmerald

    // Drives both the layout and the "can this still be removed" check.
    // Deliberately keyed off real passengers rather than ride.status: a
    // part-full car is still "active" but is absolutely not removable.
    val hasPassengers = matchedRequests.isNotEmpty()

    val vehicleDetails = VehicleType.detailsSummary(
        ride.vehicleType,
        ride.vehicleModel,
        ride.vehicleNumber,
        ride.vehicleColor
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBase)
            .border(
                1.dp,
                accentColor.copy(alpha = 0.25f),
                RoundedCornerShape(18.dp)
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (ride.tripDirection) {
                        "to_campus" -> "To Campus"
                        "to_home" -> "To Home"
                        else -> "Saved Ride"
                    },
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${ride.pickup} → ${ride.destination}",
                    color = TextMed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ride.tripTime,
                    color = TextMed
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = buildString {
                        append(VehicleType.emoji(ride.vehicleType))
                        append("  ")
                        append(VehicleType.label(ride.vehicleType))
                        if (vehicleDetails != null) {
                            append(" · ")
                            append(vehicleDetails)
                        }
                    },
                    color = TextMed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MiniBadge(
                    text = ride.status.replaceFirstChar { it.uppercase() },
                    accent = when (ride.status.lowercase()) {
                        "active" -> AccentEmerald
                        "full" -> AccentAmber
                        else -> AccentRed
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                MiniBadge(text = ride.seatSummary(), accent = Lime)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (hasPassengers) {
            Text(
                text = "Passengers (${matchedRequests.size} of ${ride.seatCapacity()})",
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                matchedRequests.forEach { request ->
                    MatchedPassengerRow(
                        request = request,
                        isProcessing = processingRequestIds.contains(request.requestId),
                        onCancel = { reason -> onCancel(request, reason) },
                        onStartTrip = { onStartTrip(request) },
                        onCompleteTrip = { onCompleteTrip(request) },
                        onRatePassenger = { onRatePassenger(request) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentRed.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        AccentRed.copy(alpha = 0.18f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onRemove()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Remove",
                    color = AccentRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * A single accepted passenger on a saved ride: who they are, how to reach
 * them, and whichever lifecycle action their trip is currently waiting on.
 */
@Composable
private fun MatchedPassengerRow(
    request: RideRequest,
    isProcessing: Boolean,
    onCancel: (String) -> Unit,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit,
    onRatePassenger: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardElevated)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(LimeGlow)
                    .border(1.dp, Lime.copy(alpha = 0.2f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = request.passengerName.take(1).uppercase().ifBlank { "P" },
                    color = Lime,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.passengerName.ifBlank { "Passenger" },
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = request.passengerPhone.ifBlank { "N/A" },
                    color = TextMed,
                    fontSize = 12.sp
                )

                rememberPassengerStats(request.userId)?.let { stats ->
                    Text(
                        text = passengerStatsLabel(stats),
                        color = TextMed,
                        fontSize = 12.sp
                    )
                }
            }

            MiniBadge(
                text = request.status.passengerStatusLabel(),
                accent = request.status.passengerStatusAccent()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (request.status) {
            RideRequestStatus.START_PENDING_CONFIRMATION -> {
                RiderTripWaitingNotice(
                    message = "Waiting for the passenger to confirm the trip started."
                )
            }

            RideRequestStatus.ONGOING -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RiderFullWidthActionButton(
                        text = "Call Passenger",
                        icon = Icons.Default.Call,
                        accent = AccentEmerald,
                        enabled = true,
                        onClick = {
                            openDialer(context, request.passengerPhone)
                        }
                    )

                    RiderFullWidthActionButton(
                        text = if (isProcessing) "Completing..." else "Trip Completed",
                        icon = Icons.Default.CheckCircle,
                        accent = AccentBlue,
                        enabled = !isProcessing,
                        onClick = onCompleteTrip
                    )
                }
            }

            RideRequestStatus.END_PENDING_CONFIRMATION -> {
                RiderTripWaitingNotice(
                    message = "Waiting for the passenger to confirm the trip is complete."
                )
            }

            RideRequestStatus.COMPLETED -> {
                if (request.passengerRated) {
                    RiderTripWaitingNotice(
                        message = "Trip completed. You rated this passenger ${request.passengerRating}/5.",
                        accent = AccentEmerald
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderTripWaitingNotice(
                            message = "Trip completed.",
                            accent = AccentEmerald
                        )

                        RiderFullWidthActionButton(
                            text = "Rate Passenger",
                            icon = Icons.Default.Star,
                            accent = AccentAmber,
                            enabled = true,
                            onClick = onRatePassenger
                        )
                    }
                }
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(IntrinsicSize.Max)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentEmerald.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    AccentEmerald.copy(alpha = 0.18f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    openDialer(context, request.passengerPhone)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(16.dp)
                                )

                                Text(
                                    text = "Call Passenger",
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentAmber.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    AccentAmber.copy(alpha = 0.18f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isProcessing) {
                                    showCancelDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isProcessing) "Cancelling..." else "Cancel Ride",
                                color = AccentAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    RiderFullWidthActionButton(
                        text = if (isProcessing) "Starting..." else "Start Trip",
                        icon = Icons.Default.PlayArrow,
                        accent = AccentBlue,
                        enabled = !isProcessing,
                        onClick = onStartTrip
                    )
                }
            }
        }
    }

    if (showCancelDialog) {
        CancelRideDialog(
            title = "Cancel this passenger's ride?",
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason ->
                showCancelDialog = false
                onCancel(reason)
            }
        )
    }
}

@Composable
private fun RiderFullWidthActionButton(
    text: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = if (enabled) 0.08f else 0.04f))
            .border(
                1.dp,
                accent.copy(alpha = if (enabled) 0.18f else 0.08f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accent else accent.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = text,
                color = if (enabled) accent else accent.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RiderTripWaitingNotice(
    message: String,
    accent: Color = AccentAmber
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = message,
            color = accent,
            fontWeight = FontWeight.Medium
        )
    }
}
