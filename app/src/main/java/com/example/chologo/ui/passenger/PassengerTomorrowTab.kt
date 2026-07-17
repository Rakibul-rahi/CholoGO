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
import com.example.chologo.repository.UserRepository
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.viewmodel.TomorrowMatchedRide
import com.example.chologo.viewmodel.TomorrowRideViewModel

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
        phone = ""
    )
}

@Composable
fun PassengerTomorrowTab(
    authViewModel: AuthViewModel,
    userRepository: UserRepository,
    onXpUpdated: (Long) -> Unit,
    tomorrowRideViewModel: TomorrowRideViewModel = viewModel()
) {
    val context = LocalContext.current
    val tomorrowDate = remember { getTomorrowDateKey() }
    val authState by authViewModel.uiState.collectAsState()
    val uiState by tomorrowRideViewModel.uiState.collectAsState()

    var hasClassesTomorrow by remember { mutableStateOf<Boolean?>(null) }

    var wantToCampus by remember { mutableStateOf(true) }
    var wantToHome by remember { mutableStateOf(true) }

    var campusPickupLocation by remember { mutableStateOf("Mirpur 12") }
    var homeReturnLocation by remember { mutableStateOf("Mirpur 12") }

    var showCampusPickupMenu by remember { mutableStateOf(false) }
    var showHomeReturnMenu by remember { mutableStateOf(false) }

    var classStartHour by remember { mutableIntStateOf(8) }
    var classStartMinute by remember { mutableIntStateOf(30) }
    var classEndHour by remember { mutableIntStateOf(15) }
    var classEndMinute by remember { mutableIntStateOf(30) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var isEditing by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }

    val classStartText = formatTo12Hour(classStartHour, classStartMinute)
    val classEndText = formatTo12Hour(classEndHour, classEndMinute)

    val campusRequest = uiState.savedRequests.firstOrNull { it.tripDirection == "to_campus" }
    val homeRequest = uiState.savedRequests.firstOrNull { it.tripDirection == "to_home" }

    val isCampusLocked = campusRequest != null && campusRequest.status != "pending"
    val isHomeLocked = homeRequest != null && homeRequest.status != "pending"

    val requestSubmitted = uiState.savedRequests.isNotEmpty()
    val hasAcceptedRequest = uiState.savedRequests.any { it.status.equals("accepted", true) }
    val submittedDateText = uiState.savedRequests.firstOrNull()?.rideDate ?: tomorrowDate

    fun refreshPassengerXp() {
        userRepository.getCurrentUserData { result ->
            result.onSuccess { user -> onXpUpdated(user.xp) }
        }
    }

    LaunchedEffect(authState.userId) {
        if (authState.userId.isNotBlank()) {
            tomorrowRideViewModel.startPassengerSession(authState.userId, tomorrowDate)
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
            refreshPassengerXp()
            isEditing = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        PassengerSectionCard(
            title = "Tomorrow Ride",
            subtitle = "Plan your next campus trip in advance and get matched automatically."
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBadge(text = "Scheduled", accent = AccentBlue)
                MiniBadge(text = "Auto match", accent = AccentEmerald)
            }
        }

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

                        PassengerSectionCard(
                            title = if (hasAcceptedRequest) "Accepted Ride" else "Matched Riders",
                            subtitle = if (hasAcceptedRequest) {
                                "Your saved request has already been accepted."
                            } else {
                                "These riders match your saved tomorrow request."
                            }
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniBadge(
                                    text = if (hasAcceptedRequest) "Accepted" else "Pending Match",
                                    accent = if (hasAcceptedRequest) AccentEmerald else AccentAmber
                                )
                            }
                        }

                        if (hasAcceptedRequest) {
                            AcceptedRequestsSection(savedRequestList = uiState.savedRequests)
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
                            subtitle = "Choose one or both trips and save them for automatic matching."
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
                                        if (authState.userId.isBlank()) {
                                            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                                            return@LimeActionButton
                                        }

                                        tomorrowRideViewModel.savePassengerPlan(
                                            userId = authState.userId,
                                            passengerName = authState.userName.ifBlank { "Passenger" },
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
fun AcceptedRequestsSection(
    savedRequestList: List<RideRequest>
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        savedRequestList
            .filter { it.status.equals("accepted", true) }
            .forEach { request ->
                PassengerSectionCard(
                    title = if (request.tripDirection == "to_campus") {
                        "Accepted Campus Ride"
                    } else {
                        "Accepted Return Ride"
                    },
                    subtitle = "Your ride request has been accepted."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniBadge(text = "Accepted", accent = AccentEmerald)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    RideMetaRow(
                        Icons.Default.LocationOn,
                        "${request.pickup} → ${request.destination}"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RideMetaRow(Icons.Default.Schedule, request.tripTime)
                    Spacer(modifier = Modifier.height(8.dp))
                    RideMetaRow(
                        Icons.Default.Person,
                        request.matchedRiderName.ifBlank { "Accepted rider" }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { openDialer(context, request.matchedRiderPhone) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Lime),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Rider")
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
    onNoClick: () -> Unit
) {
    PassengerSectionCard(
        title = title,
        subtitle = subtitle
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

    PassengerSectionCard(
        title = "Saved Tomorrow Plan",
        subtitle = "Your request is already saved for $submittedDate."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(
                text = if (isAccepted) "Accepted" else "Pending",
                accent = if (isAccepted) AccentEmerald else AccentAmber
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