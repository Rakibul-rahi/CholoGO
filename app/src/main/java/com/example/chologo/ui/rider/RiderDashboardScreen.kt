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
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.RideRequestStatus
import com.example.chologo.navigation.Screen
import com.example.chologo.notifications.TomorrowRideReminderScheduler
import com.example.chologo.repository.UserRepository
import com.example.chologo.ui.common.CancelRideDialog
import com.example.chologo.ui.common.CholoGoTabRow
import com.example.chologo.ui.common.CholoGoTopBar
import com.example.chologo.ui.common.rememberNotificationPermissionRequester
import com.example.chologo.ui.components.LevelCard
import com.example.chologo.ui.components.LocalAdCarouselBanner
import com.example.chologo.utils.LevelSystem
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.viewmodel.TomorrowMatchedRequest
import com.example.chologo.viewmodel.TomorrowRideUiState
import com.example.chologo.viewmodel.TomorrowRideViewModel

private val DashboardBg = Color(0xFF0A0D0F)

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

    val userRepository = remember { UserRepository() }

    var riderXp by remember { mutableStateOf(0L) }
    var isLevelLoading by remember { mutableStateOf(true) }

    val tomorrowDate = remember { getTomorrowDateKey() }

    fun refreshRiderXp() {
        userRepository.getCurrentUserData { result ->
            result.onSuccess { user ->
                riderXp = user.xp
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser()

        userRepository.getCurrentUserData { result ->
            result.onSuccess { user ->
                riderXp = user.xp
                isLevelLoading = false
            }.onFailure {
                riderXp = 0L
                isLevelLoading = false
            }
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
                                tomorrowRideViewModel = tomorrowRideViewModel,
                                onAccept = { match ->
                                    tomorrowRideViewModel.acceptRequest(
                                        match = match,
                                        riderId = authState.userId,
                                        riderName = authState.userName.ifBlank { "Rider" },
                                        riderPhone = authState.userPhone.ifBlank { "N/A" },
                                        onXpAwarded = { refreshRiderXp() }
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
                                onSaveSuccess = {
                                    refreshRiderXp()
                                    // No manual reload needed - the live
                                    // listener already reflects the change.
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderTomorrowDashboardContent(
    uiState: TomorrowRideUiState,
    tomorrowDate: String,
    riderId: String,
    riderName: String,
    tomorrowRideViewModel: TomorrowRideViewModel,
    onAccept: (TomorrowMatchedRequest) -> Unit,
    onDecline: (TomorrowMatchedRequest) -> Unit,
    onRemoveRide: (Ride) -> Unit,
    onCancelRide: (RideRequest, String) -> Unit,
    onStartTrip: (RideRequest) -> Unit,
    onCompleteTrip: (RideRequest) -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    var scheduledReminderKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Same "1 hour before, is this still happening?" reminder as the
    // passenger side, but keyed off the rider's own saved ride once a
    // passenger has actually matched it (status != "active" means matched/
    // full - see SavedRideCard's isMatched check below).
    LaunchedEffect(uiState.savedRides, uiState.acceptedRequestsForRider) {
        val matchedRides = uiState.savedRides.filter { it.status != "active" }
        val currentKeys = matchedRides.map { "rider_${it.rideId}" }.toSet()

        (scheduledReminderKeys - currentKeys).forEach { staleKey ->
            TomorrowRideReminderScheduler.cancelReminder(context, staleKey)
        }

        if (matchedRides.isNotEmpty()) {
            requestNotificationPermission()
        }

        matchedRides.forEach { ride ->
            val matchedRequest = uiState.acceptedRequestsForRider
                .firstOrNull { it.matchedRideId == ride.rideId }
            val directionLabel = if (ride.tripDirection == "to_campus") "to campus" else "back home"
            val passengerLabel = matchedRequest?.passengerName?.ifBlank { "your passenger" }
                ?: "your passenger"

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
        RiderTomorrowIntroCard()

        RiderRequestsTab(
            uiState = uiState,
            onAccept = onAccept,
            onDecline = onDecline,
            onRemoveRide = onRemoveRide,
            onCancelRide = onCancelRide,
            onStartTrip = onStartTrip,
            onCompleteTrip = onCompleteTrip
        )

        RiderTomorrowSetupTab(
            riderId = riderId,
            rideDate = tomorrowDate,
            riderName = riderName,
            tomorrowRideViewModel = tomorrowRideViewModel,
            onSaveSuccess = onSaveSuccess
        )
    }
}

@Composable
private fun RiderTomorrowIntroCard() {
    RiderSectionCard(
        title = "Tomorrow Rides",
        subtitle = "Set your route for tomorrow and accept matched passenger requests.",
        icon = "📅"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Schedule rides", accent = AccentBlue)
            MiniBadge(text = "Earn XP", accent = AccentEmerald)
        }
    }
}

@Composable
fun RiderRequestsTab(
    uiState: TomorrowRideUiState,
    onAccept: (TomorrowMatchedRequest) -> Unit,
    onDecline: (TomorrowMatchedRequest) -> Unit,
    onRemoveRide: (Ride) -> Unit,
    onCancelRide: (RideRequest, String) -> Unit,
    onStartTrip: (RideRequest) -> Unit,
    onCompleteTrip: (RideRequest) -> Unit
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
            subtitle = "Your active rides for tomorrow are listed here.",
            icon = "🏍️"
        ) {
            if (uiState.savedRides.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.TwoWheeler,
                    message = "No rides saved yet.\nUse the setup form below to create your tomorrow ride."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.savedRides.forEach { ride ->
                        val matchedRequest = uiState.acceptedRequestsForRider
                            .firstOrNull { it.matchedRideId == ride.rideId }

                        SavedRideCard(
                            ride = ride,
                            matchedRequest = matchedRequest,
                            isProcessing = matchedRequest != null &&
                                    uiState.processingRequestIds.contains(matchedRequest.requestId),
                            onRemove = {
                                if (ride.rideId.isNotBlank()) {
                                    onRemoveRide(ride)
                                }
                            },
                            onCancel = { reason ->
                                matchedRequest?.let { onCancelRide(it, reason) }
                            },
                            onStartTrip = {
                                matchedRequest?.let { onStartTrip(it) }
                            },
                            onCompleteTrip = {
                                matchedRequest?.let { onCompleteTrip(it) }
                            }
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

@Composable
fun SavedRideCard(
    ride: Ride,
    matchedRequest: RideRequest?,
    isProcessing: Boolean,
    onRemove: () -> Unit,
    onCancel: (String) -> Unit,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isCampus = ride.tripDirection.equals("to_campus", ignoreCase = true)
    val accentColor = if (isCampus) AccentBlue else AccentEmerald
    val isMatched = ride.status != "active"

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

                if (isMatched && matchedRequest != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Passenger: ${matchedRequest.passengerName.ifBlank { "Passenger" }}",
                        color = TextMed
                    )
                    Text(
                        text = "Phone: ${matchedRequest.passengerPhone.ifBlank { "N/A" }}",
                        color = TextMed
                    )
                }
            }

            MiniBadge(
                text = ride.status.replaceFirstChar { it.uppercase() },
                accent = when (ride.status.lowercase()) {
                    "active" -> AccentEmerald
                    "full" -> AccentAmber
                    else -> AccentRed
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isMatched) {
            when (matchedRequest?.status) {
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
                                openDialer(context, matchedRequest?.passengerPhone.orEmpty())
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
                    RiderTripWaitingNotice(
                        message = "Trip completed.",
                        accent = AccentEmerald
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentEmerald.copy(alpha = 0.08f))
                                    .border(
                                        1.dp,
                                        AccentEmerald.copy(alpha = 0.18f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = matchedRequest != null) {
                                        openDialer(context, matchedRequest?.passengerPhone.orEmpty())
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentAmber.copy(alpha = 0.08f))
                                    .border(
                                        1.dp,
                                        AccentAmber.copy(alpha = 0.18f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = matchedRequest != null && !isProcessing) {
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
                            enabled = matchedRequest != null && !isProcessing,
                            onClick = onStartTrip
                        )
                    }
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

    if (showCancelDialog) {
        CancelRideDialog(
            title = "Cancel this ride?",
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