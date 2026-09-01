@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.passenger

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.RideRequestStatus
import com.example.chologo.notifications.NotifiedEventsStore
import com.example.chologo.notifications.ReminderNotifications
import com.example.chologo.notifications.TomorrowRideReminderScheduler
import com.example.chologo.ui.common.RatingDialog
import com.example.chologo.ui.common.ReportDialog
import com.example.chologo.ui.common.rememberNotificationPermissionRequester
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.data.model.VehicleType
import com.example.chologo.viewmodel.TomorrowMatchedRide
import com.example.chologo.viewmodel.TomorrowRideViewModel
import com.google.firebase.auth.FirebaseAuth

private fun openDialer(context: android.content.Context, phoneNumber: String) {
    if (phoneNumber.isBlank() || phoneNumber == "N/A") {
        Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
    }
    context.startActivity(intent)
}

private fun TomorrowMatchedRide.toRideCardUi(): RideCardUi {
    return RideCardUi(
        rideId = rideId,
        driverName = riderName.ifBlank { "Rider" },
        routeLabel = when (tripDirection) {
            "to_campus" -> "To Campus"
            "to_home" -> "To Home"
            else -> "Ride"
        },
        origin = pickup,
        destination = destination,
        departureTime = tripTime,
        seatsLeft = availableSeats,
        phone = "",
        // Contact stays hidden until this rider actually accepts, but the
        // vehicle is fair game up front - it's how a passenger decides
        // whether the ride suits them.
        vehicleLabel = VehicleType.detailsSummary(
            vehicleType,
            vehicleModel,
            vehicleNumber,
            vehicleColor
        ) ?: VehicleType.label(vehicleType),
        isCar = VehicleType.isCar(vehicleType)
    )
}

@Composable
fun PassengerTomorrowTab(
    authViewModel: AuthViewModel,
    onRequireLogin: () -> Unit = {},
    tomorrowRideViewModel: TomorrowRideViewModel = viewModel()
) {
    val context = LocalContext.current
    val tomorrowDate = remember { getTomorrowDateKey() }
    val authState by authViewModel.uiState.collectAsState()
    val uiState by tomorrowRideViewModel.uiState.collectAsState()

    var hasClassesTomorrow by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // Saveable, not just remember: a signed-out user can fill this form,
    // get redirected to sign in when they tap Save (see onRequireLogin
    // below), and come back - their picks should still be here.
    var wantToCampus by rememberSaveable { mutableStateOf(true) }
    var wantToHome by rememberSaveable { mutableStateOf(true) }

    var campusPickupLocation by rememberSaveable { mutableStateOf("Mirpur 12") }
    var homeReturnLocation by rememberSaveable { mutableStateOf("Mirpur 12") }

    var showCampusPickupMenu by remember { mutableStateOf(false) }
    var showHomeReturnMenu by remember { mutableStateOf(false) }

    var classStartHour by rememberSaveable { mutableIntStateOf(8) }
    var classStartMinute by rememberSaveable { mutableIntStateOf(30) }
    var classEndHour by rememberSaveable { mutableIntStateOf(15) }
    var classEndMinute by rememberSaveable { mutableIntStateOf(30) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var isEditing by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }

    // A passenger can have up to 2 legs (campus + home) in flight at once,
    // so the rating/report dialogs need to know which specific leg they
    // were opened for - not just a single global "current request".
    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var feedbackTarget by remember { mutableStateOf<RideRequest?>(null) }

    val classStartText = formatTo12Hour(classStartHour, classStartMinute)
    val classEndText = formatTo12Hour(classEndHour, classEndMinute)

    val campusRequest = uiState.savedRequests.firstOrNull { it.tripDirection == "to_campus" }
    val homeRequest = uiState.savedRequests.firstOrNull { it.tripDirection == "to_home" }

    // Only an actively matched leg (accepted, or anywhere further along the
    // trip lifecycle) is locked. A "cancelled" leg must stay editable so the
    // passenger can resubmit it - otherwise, once a rider cancels an
    // accepted trip, that leg is stuck forever: locked here, and invisible
    // to other riders since it never goes back to "pending".
    val isCampusLocked = campusRequest != null &&
            campusRequest.status !in listOf("pending", "cancelled")
    val isHomeLocked = homeRequest != null &&
            homeRequest.status !in listOf("pending", "cancelled")

    val requestSubmitted = uiState.savedRequests.isNotEmpty()
    val hasAcceptedRequest = uiState.savedRequests.any {
        it.status in RideRequestStatus.ACTIVE_LIFECYCLE_STATUSES
    }
    val submittedDateText = uiState.savedRequests.firstOrNull()?.rideDate ?: tomorrowDate

    // Legs the rider backed out of after accepting - these need to be
    // surfaced clearly, since they're otherwise indistinguishable from a
    // still-open "pending" request once resubmitted.
    val riderCancelledLegs = uiState.savedRequests.filter {
        it.status == "cancelled" && it.cancelledByRole == "rider"
    }

    LaunchedEffect(authState.userId) {
        if (authState.userId.isNotBlank()) {
            tomorrowRideViewModel.startPassengerSession(authState.userId, tomorrowDate)
        }
    }

    val requestNotificationPermission = rememberNotificationPermissionRequester()
    var scheduledReminderKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Once a leg is actually accepted by a rider, schedule an on-device
    // reminder for 1 hour before it - "is this ride still happening?".
    // Re-runs on every savedRequests update, which is harmless: rescheduling
    // the same key just replaces the pending work (see
    // TomorrowRideReminderScheduler). Anything that WAS accepted and no
    // longer is (cancelled, removed) gets its reminder cancelled instead.
    LaunchedEffect(uiState.savedRequests) {
        val acceptedRequests = uiState.savedRequests.filter { it.status == "accepted" }
        val currentKeys = acceptedRequests.map { "passenger_${it.requestId}" }.toSet()

        (scheduledReminderKeys - currentKeys).forEach { staleKey ->
            TomorrowRideReminderScheduler.cancelReminder(context, staleKey)
        }

        if (acceptedRequests.isNotEmpty()) {
            requestNotificationPermission()
        }

        acceptedRequests.forEach { request ->
            val directionLabel = if (request.tripDirection == "to_campus") "to campus" else "back home"
            val riderLabel = request.matchedRiderName.ifBlank { "your rider" }

            TomorrowRideReminderScheduler.scheduleReminder(
                context = context,
                uniqueKey = "passenger_${request.requestId}",
                rideDate = request.rideDate,
                timeMinutes = request.timeMinutes,
                title = "Tomorrow Ride reminder",
                message = "Your ride $directionLabel with $riderLabel at ${request.tripTime} is in 1 hour. Still happening?"
            )
        }

        scheduledReminderKeys = currentKeys
    }

    // Alert the passenger the moment a rider backs out of an already
    // accepted leg - this fires even if the app is backgrounded, unlike the
    // in-tab banner. NotifiedEventsStore is backed by SharedPreferences
    // (not just remember state), so re-opening the app later doesn't
    // re-fire a notification for a state that's still true.
    //
    // The "rider accepted" notification is now sent server-side (see
    // TomorrowRideViewModel.acceptRequest -> notifyPassengerAccepted /
    // server/src/index.ts's notify-accepted endpoint) so it also reaches a
    // backgrounded or killed app - a local-only effect here would double-
    // notify a passenger whose app happens to be open.
    LaunchedEffect(uiState.savedRequests) {
        val newlyCancelled = riderCancelledLegs.filter {
            !NotifiedEventsStore.hasNotified(context, "cancelled_${it.requestId}")
        }

        if (newlyCancelled.isNotEmpty()) {
            requestNotificationPermission()
        }

        newlyCancelled.forEach { request ->
            val directionLabel = if (request.tripDirection == "to_campus") "to campus" else "back home"

            ReminderNotifications.showNow(
                context = context,
                uniqueKey = "cancelled_${request.requestId}",
                title = "Tomorrow Ride cancelled",
                message = "Your rider cancelled your $directionLabel trip. Tap to resubmit your request."
            )

            NotifiedEventsStore.markNotified(context, "cancelled_${request.requestId}")
        }
    }

    // Populate editable fields from live data, but only when not actively
    // editing, so an incoming snapshot doesn't clobber unsaved changes.
    LaunchedEffect(uiState.savedRequests, isEditing) {
        if (!isEditing) {
            campusRequest?.let { request ->
                campusPickupLocation = request.pickup.ifBlank { "Mirpur 12" }
                if (request.timeMinutes > 0) {
                    classStartHour = request.timeMinutes / 60
                    classStartMinute = request.timeMinutes % 60
                }
                wantToCampus = true
            }

            homeRequest?.let { request ->
                homeReturnLocation = request.destination.ifBlank { "Mirpur 12" }
                if (request.timeMinutes > 0) {
                    classEndHour = request.timeMinutes / 60
                    classEndMinute = request.timeMinutes % 60
                }
                wantToHome = true
            }

            if (requestSubmitted) {
                hasClassesTomorrow = true
            }
        }

        hasLoadedOnce = true
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            tomorrowRideViewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            tomorrowRideViewModel.clearMessage()
            isEditing = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        PassengerSectionCard(
            title = "Tomorrow Ride",
            subtitle = "Plan your next campus trip in advance and get matched automatically.",
            icon = "📅"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBadge(text = "Scheduled", accent = AccentBlue)
                MiniBadge(text = "Auto match", accent = AccentEmerald)
            }
        }

        // This tab is for planning the NEXT trip and nothing else. The
        // "did this ride happen?" review for a leg that was never marked
        // finished lives in Ride History now - a trip that already
        // happened is history, and prompting about it here buried the
        // planning form under a question about something the user
        // considered done days ago.
        when {
            !hasLoadedOnce -> {
                PremiumLoadingCard("Loading tomorrow requests...")
            }

            requestSubmitted && !isEditing -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PassengerTomorrowSubmittedCard(
                            savedRequests = uiState.savedRequests,
                            submittedDate = submittedDateText,
                            isAccepted = hasAcceptedRequest,
                            onEditClick = { isEditing = true }
                        )

                        if (riderCancelledLegs.isNotEmpty()) {
                            RiderCancelledNotice(
                                cancelledLegs = riderCancelledLegs,
                                onResubmitClick = { isEditing = true }
                            )
                        }

                        PassengerSectionCard(
                            title = if (hasAcceptedRequest) "Accepted Ride" else "Matched Riders",
                            subtitle = if (hasAcceptedRequest) {
                                "Your saved request has already been accepted."
                            } else {
                                "These riders match your saved tomorrow request."
                            },
                            icon = if (hasAcceptedRequest) "✅" else "🙋"
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniBadge(
                                    text = if (hasAcceptedRequest) "Accepted" else "Pending Match",
                                    accent = if (hasAcceptedRequest) AccentEmerald else AccentAmber
                                )
                            }
                        }

                        if (hasAcceptedRequest) {
                            AcceptedRequestsSection(
                                savedRequestList = uiState.savedRequests,
                                onConfirmStarted = { request ->
                                    tomorrowRideViewModel.confirmTripStarted(request.requestId)
                                },
                                onRejectStarted = { request ->
                                    tomorrowRideViewModel.rejectTripStarted(request.requestId)
                                },
                                onConfirmCompleted = { request ->
                                    tomorrowRideViewModel.confirmTripCompleted(request.requestId)
                                },
                                onRateRide = { request ->
                                    feedbackTarget = request
                                    showRatingDialog = true
                                },
                                onReportRide = { request ->
                                    feedbackTarget = request
                                    showReportDialog = true
                                }
                            )
                        } else {
                            val matchedCards = uiState.matchedRidesForPassenger.map { it.toRideCardUi() }

                            when {
                                matchedCards.isEmpty() -> {
                                    EmptyStateCard(
                                        icon = Icons.Default.Info,
                                        message = "No rider matched your saved request yet."
                                    )
                                }

                                else -> {
                                    matchedCards.forEach { ride ->
                                        PassengerRideCard(
                                            ride = ride,
                                            highlight = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                YesNoPromptCard(
                    title = "Do you have classes tomorrow?",
                    subtitle = "Save your campus and return trip in advance for automatic matching.",
                    icon = "🎓",
                    onYesClick = { hasClassesTomorrow = true },
                    onNoClick = {
                        hasClassesTomorrow = false
                    }
                )

                AnimatedVisibility(
                    visible = hasClassesTomorrow == true,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PassengerSectionCard(
                            title = "Tomorrow's Ride Setup",
                            subtitle = "Choose one or both trips and save them for automatic matching.",
                            icon = "🚗"
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniBadge(text = "To campus", accent = AccentBlue)
                                MiniBadge(text = "To home", accent = AccentEmerald)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            SectionLabel(text = "Trip directions")
                            Spacer(modifier = Modifier.height(10.dp))

                            if (isCampusLocked) {
                                LockedLegNotice(
                                    message = "Your campus trip is already accepted and can't be edited here."
                                )
                            } else {
                                TripDirectionToggle(
                                    label = "Going to Campus",
                                    checked = wantToCampus,
                                    onCheckedChange = { wantToCampus = it }
                                )

                                AnimatedVisibility(visible = wantToCampus) {
                                    Column {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        SectionLabel(text = "Campus trip details")
                                        Spacer(modifier = Modifier.height(10.dp))

                                        LocationSelectionCard(
                                            label = "Pickup for campus trip",
                                            selectedLocation = campusPickupLocation,
                                            expanded = showCampusPickupMenu,
                                            onExpandChange = { showCampusPickupMenu = it },
                                            locations = availableLocations.filter { it != "AUST Gate" },
                                            leadingIcon = Icons.Default.LocationOn,
                                            onLocationSelected = {
                                                campusPickupLocation = it
                                                showCampusPickupMenu = false
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        PassengerTimeSelectionCard(
                                            label = "Class start / campus ride time",
                                            selectedTimeText = classStartText,
                                            helper = "Used for your trip to campus.",
                                            onPickTimeClick = { showStartTimePicker = true }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isHomeLocked) {
                                LockedLegNotice(
                                    message = "Your return trip is already accepted and can't be edited here."
                                )
                            } else {
                                TripDirectionToggle(
                                    label = "Coming Back Home",
                                    checked = wantToHome,
                                    onCheckedChange = { wantToHome = it }
                                )

                                AnimatedVisibility(visible = wantToHome) {
                                    Column {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        SectionLabel(text = "Return trip details")
                                        Spacer(modifier = Modifier.height(10.dp))

                                        LocationSelectionCard(
                                            label = "Drop location for return trip",
                                            selectedLocation = homeReturnLocation,
                                            expanded = showHomeReturnMenu,
                                            onExpandChange = { showHomeReturnMenu = it },
                                            locations = availableLocations.filter { it != "AUST Gate" },
                                            leadingIcon = Icons.Default.Home,
                                            onLocationSelected = {
                                                homeReturnLocation = it
                                                showHomeReturnMenu = false
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        PassengerTimeSelectionCard(
                                            label = "Class end / home ride time",
                                            selectedTimeText = classEndText,
                                            helper = "Used for your trip back home.",
                                            onPickTimeClick = { showEndTimePicker = true }
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = !isCampusLocked && !isHomeLocked &&
                                        !wantToCampus && !wantToHome
                            ) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    NeitherDirectionWarning()
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            if (isCampusLocked && isHomeLocked) {
                                RideNowInfoBanner(
                                    message = "Both trips are already accepted. There's nothing left to edit for tomorrow."
                                )
                            } else {
                                LimeActionButton(
                                    text = if (uiState.isLoading) "Saving..." else "Save Tomorrow Request",
                                    icon = Icons.Default.CheckCircle,
                                    isLoading = uiState.isLoading,
                                    onClick = {
                                        if (FirebaseAuth.getInstance().currentUser == null) {
                                            onRequireLogin()
                                            return@LimeActionButton
                                        }

                                        if (authState.userId.isBlank()) {
                                            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                                            return@LimeActionButton
                                        }

                                        tomorrowRideViewModel.savePassengerPlan(
                                            userId = authState.userId,
                                            passengerName = authState.userName.ifBlank { "Passenger" },
                                            passengerPhone = authState.userPhone,
                                            rideDate = tomorrowDate,
                                            wantCampus = wantToCampus && !isCampusLocked,
                                            campusPickup = campusPickupLocation,
                                            campusTripTime = classStartText,
                                            campusHour = classStartHour,
                                            campusMinute = classStartMinute,
                                            campusTimeMinutes = toMinutes(classStartHour, classStartMinute),
                                            wantHome = wantToHome && !isHomeLocked,
                                            homeDestination = homeReturnLocation,
                                            homeTripTime = classEndText,
                                            homeHour = classEndHour,
                                            homeMinute = classEndMinute,
                                            homeTimeMinutes = toMinutes(classEndHour, classEndMinute)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        PassengerTimePickerDialog(
            initialHour = classStartHour,
            initialMinute = classStartMinute,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { h, m ->
                classStartHour = h
                classStartMinute = m
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        PassengerTimePickerDialog(
            initialHour = classEndHour,
            initialMinute = classEndMinute,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { h, m ->
                classEndHour = h
                classEndMinute = m
                showEndTimePicker = false
            }
        )
    }

    if (showRatingDialog && feedbackTarget != null) {
        val target = feedbackTarget!!

        RatingDialog(
            onDismiss = {
                showRatingDialog = false
            },
            onSubmit = { stars, comment ->
                if (authState.userId.isBlank() || target.matchedRiderId.isBlank()) {
                    Toast.makeText(context, "Rider not found.", Toast.LENGTH_SHORT).show()
                    return@RatingDialog
                }

                tomorrowRideViewModel.submitTomorrowRating(
                    request = target,
                    ratedBy = authState.userId,
                    ratedTo = target.matchedRiderId,
                    stars = stars,
                    comment = comment
                )

                showRatingDialog = false
            }
        )
    }

    if (showReportDialog && feedbackTarget != null) {
        val target = feedbackTarget!!

        ReportDialog(
            onDismiss = {
                showReportDialog = false
            },
            onSubmit = { reason, details ->
                if (authState.userId.isBlank() || target.matchedRiderId.isBlank()) {
                    Toast.makeText(context, "Rider not found.", Toast.LENGTH_SHORT).show()
                    return@ReportDialog
                }

                tomorrowRideViewModel.submitTomorrowReport(
                    request = target,
                    reportedBy = authState.userId,
                    reportedUserId = target.matchedRiderId,
                    reason = reason,
                    details = details
                )

                showReportDialog = false
            }
        )
    }
}

@Composable
private fun LockedLegNotice(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AccentEmerald.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = AccentEmerald,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            color = TextMed,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun RiderCancelledNotice(
    cancelledLegs: List<RideRequest>,
    onResubmitClick: () -> Unit
) {
    PassengerSectionCard(
        title = "Trip cancelled by rider",
        subtitle = "Your rider backed out after accepting. Resubmit to get matched again.",
        icon = "⚠️"
    ) {
        Column {
            cancelledLegs.forEach { request ->
                val directionLabel = if (request.tripDirection == "to_campus") {
                    "To campus"
                } else {
                    "Return trip"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentRed.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$directionLabel: ${request.matchedRiderName.ifBlank { "Your rider" }} cancelled this trip.",
                        color = TextMed,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = onResubmitClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                )
            ) {
                Text("Resubmit Request")
            }
        }
    }
}

@Composable
fun AcceptedRequestsSection(
    savedRequestList: List<RideRequest>,
    onConfirmStarted: (RideRequest) -> Unit,
    onRejectStarted: (RideRequest) -> Unit,
    onConfirmCompleted: (RideRequest) -> Unit,
    onRateRide: (RideRequest) -> Unit,
    onReportRide: (RideRequest) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        savedRequestList
            .filter { it.status in RideRequestStatus.ACTIVE_LIFECYCLE_STATUSES }
            .forEach { request ->
                when (request.status) {
                    RideRequestStatus.START_PENDING_CONFIRMATION -> {
                        TomorrowRideStartConfirmationCard(
                            request = request,
                            onConfirmStarted = { onConfirmStarted(request) },
                            onRejectStarted = { onRejectStarted(request) }
                        )
                    }

                    RideRequestStatus.ONGOING -> {
                        TomorrowRideOngoingCard(
                            request = request,
                            onCallRider = { openDialer(context, request.matchedRiderPhone) }
                        )
                    }

                    RideRequestStatus.END_PENDING_CONFIRMATION -> {
                        TomorrowRideCompletionConfirmationCard(
                            request = request,
                            onConfirmCompleted = { onConfirmCompleted(request) },
                            onReportIssue = { onReportRide(request) }
                        )
                    }

                    RideRequestStatus.COMPLETED -> {
                        TomorrowRideCompletedCard(
                            request = request,
                            onRateRide = { onRateRide(request) },
                            onReportRide = { onReportRide(request) }
                        )
                    }

                    else -> {
                        TomorrowRideAcceptedCard(
                            request = request,
                            onCallRider = { openDialer(context, request.matchedRiderPhone) }
                        )
                    }
                }
            }
    }
}

@Composable
fun YesNoPromptCard(
    title: String,
    subtitle: String,
    onYesClick: () -> Unit,
    onNoClick: () -> Unit,
    icon: String = "•"
) {
    PassengerSectionCard(
        title = title,
        subtitle = subtitle,
        icon = icon
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onYesClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeDeep,
                    contentColor = Color.Black
                )
            ) {
                Text("Yes", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNoClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHigh)
            ) {
                Text("No")
            }
        }
    }
}

@Composable
fun TripDirectionToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(CardElevated, CardBase)
                )
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Lime,
                uncheckedColor = TextMed,
                checkmarkColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = label,
            color = if (checked) TextHigh else TextMed,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
fun NeitherDirectionWarning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentAmber.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Select at least one trip direction to save a request.",
            color = AccentAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PassengerTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select time",
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Composable
private fun PassengerTomorrowSubmittedCard(
    savedRequests: List<RideRequest>,
    submittedDate: String,
    isAccepted: Boolean,
    onEditClick: () -> Unit
) {
    val campusReq = savedRequests.firstOrNull { it.tripDirection == "to_campus" }
    val homeReq = savedRequests.firstOrNull { it.tripDirection == "to_home" }
    val hasCancelledByRider = savedRequests.any {
        it.status == "cancelled" && it.cancelledByRole == "rider"
    }

    PassengerSectionCard(
        title = "Saved Tomorrow Plan",
        subtitle = "Your request is already saved for $submittedDate.",
        icon = "📋"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(
                text = when {
                    isAccepted -> "Accepted"
                    hasCancelledByRider -> "Cancelled"
                    else -> "Pending"
                },
                accent = when {
                    isAccepted -> AccentEmerald
                    hasCancelledByRider -> AccentRed
                    else -> AccentAmber
                }
            )
            MiniBadge(text = "Tomorrow", accent = AccentBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        campusReq?.let { req ->
            RideMetaRow(Icons.Default.DirectionsCar, "To campus: ${req.pickup} → AUST Gate")
            Spacer(modifier = Modifier.height(6.dp))
            RideMetaRow(Icons.Default.Schedule, "Campus time: ${req.tripTime}")
            Spacer(modifier = Modifier.height(12.dp))
        }

        homeReq?.let { req ->
            RideMetaRow(Icons.Default.Home, "Return: AUST Gate → ${req.destination}")
            Spacer(modifier = Modifier.height(6.dp))
            RideMetaRow(Icons.Default.Schedule, "Return time: ${req.tripTime}")
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Lime),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime)
        ) {
            Text("Edit Request")
        }
    }
}