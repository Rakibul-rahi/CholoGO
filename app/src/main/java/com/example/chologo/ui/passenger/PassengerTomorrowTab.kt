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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideRequest
import com.example.chologo.repository.UserRepository
import com.example.chologo.viewmodel.AuthViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

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

@Composable
fun PassengerTomorrowTab(
    authViewModel: AuthViewModel,
    userRepository: UserRepository,
    onXpUpdated: (Long) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val tomorrowDate = remember { getTomorrowDateKey() }
    val authState by authViewModel.uiState.collectAsState()

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

    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingRequests by remember { mutableStateOf(true) }
    var isLoadingMatches by remember { mutableStateOf(false) }
    var requestSubmitted by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var submittedDateText by remember { mutableStateOf("") }
    var hasAcceptedRequest by remember { mutableStateOf(false) }

    val savedRequestList = remember { mutableStateListOf<RideRequest>() }
    val matchedRideCards = remember { mutableStateListOf<RideCardUi>() }

    val classStartText = formatTo12Hour(classStartHour, classStartMinute)
    val classEndText = formatTo12Hour(classEndHour, classEndMinute)

    fun applyRequestsToUi(requests: List<RideRequest>) {
        var foundCampus = false
        var foundHome = false
        var foundDate: String? = null

        hasAcceptedRequest = requests.any { it.status.equals("accepted", true) }

        requests.forEach { request ->
            if (request.createdAt != null && foundDate == null) {
                foundDate = formatTimestampToDate(request.createdAt)
            }

            when (request.tripDirection.lowercase()) {
                "to_campus" -> {
                    campusPickupLocation = request.pickup.ifBlank { "Mirpur 12" }
                    if (request.timeMinutes > 0) {
                        classStartHour = request.timeMinutes / 60
                        classStartMinute = request.timeMinutes % 60
                    } else {
                        classStartHour = request.hour
                        classStartMinute = request.minute
                    }
                    foundCampus = true
                    wantToCampus = true
                }

                "to_home" -> {
                    homeReturnLocation = request.destination.ifBlank { "Mirpur 12" }
                    if (request.timeMinutes > 0) {
                        classEndHour = request.timeMinutes / 60
                        classEndMinute = request.timeMinutes % 60
                    } else {
                        classEndHour = request.hour
                        classEndMinute = request.minute
                    }
                    foundHome = true
                    wantToHome = true
                }
            }
        }

        if (foundCampus || foundHome) {
            wantToCampus = foundCampus
            wantToHome = foundHome
        }

        submittedDateText = foundDate ?: tomorrowDate
        requestSubmitted = foundCampus || foundHome
        hasClassesTomorrow = if (requestSubmitted) true else null
        isEditing = !requestSubmitted
        isLoadingRequests = false
    }

    fun loadMatchedRides(requests: List<RideRequest>) {
        val pendingRequests = requests.filter { it.status.equals("pending", true) }

        if (pendingRequests.isEmpty()) {
            matchedRideCards.clear()
            isLoadingMatches = false
            return
        }

        isLoadingMatches = true
        matchedRideCards.clear()

        db.collection("rides")
            .whereEqualTo("rideDate", tomorrowDate)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { result ->
                val rides = result.documents.mapNotNull { doc ->
                    doc.toObject(Ride::class.java)?.copy(rideId = doc.id)
                }

                val matched = mutableListOf<RideCardUi>()

                pendingRequests.forEach { request ->
                    rides.filter { ride ->
                        ride.availableSeats > 0 &&
                                ride.routeKey == request.routeKey &&
                                abs(ride.timeMinutes - request.timeMinutes) <= 30
                    }.forEach { ride ->
                        matched.add(
                            RideCardUi(
                                rideId = ride.rideId,
                                driverName = ride.riderName.ifBlank { "Rider" },
                                routeLabel = when (ride.tripDirection) {
                                    "to_campus" -> "To Campus"
                                    "to_home" -> "To Home"
                                    else -> "Ride"
                                },
                                origin = ride.pickup,
                                destination = ride.destination,
                                departureTime = ride.tripTime,
                                seatsLeft = ride.availableSeats,
                                phone =  ""
                            )
                        )
                    }
                }

                matchedRideCards.clear()
                matchedRideCards.addAll(matched.distinctBy { it.rideId })
                isLoadingMatches = false
            }
            .addOnFailureListener { e ->
                isLoadingMatches = false
                Toast.makeText(
                    context,
                    "Failed to load matched rides: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun loadTomorrowRequests() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            isLoadingRequests = false
            isEditing = true
            savedRequestList.clear()
            return
        }

        db.collection("ride_requests")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("rideDate", tomorrowDate)
            .get()
            .addOnSuccessListener { result ->
                val requests = result.documents.mapNotNull { doc ->
                    doc.toObject(RideRequest::class.java)?.copy(requestId = doc.id)
                }.sortedBy {
                    when (it.tripDirection) {
                        "to_campus" -> 0
                        "to_home" -> 1
                        else -> 2
                    }
                }

                savedRequestList.clear()
                savedRequestList.addAll(requests)
                applyRequestsToUi(requests)
                loadMatchedRides(requests)
            }
            .addOnFailureListener { e ->
                isLoadingRequests = false
                isEditing = true
                savedRequestList.clear()
                Toast.makeText(
                    context,
                    "Could not load previous requests: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun refreshPassengerXp() {
        userRepository.getCurrentUserData { result ->
            result.onSuccess { user ->
                onXpUpdated(user.xp)
            }
        }
    }

    fun saveTomorrowRequests() {
        if (!wantToCampus && !wantToHome) {
            Toast.makeText(
                context,
                "Please select at least one trip direction.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true

        val userId = currentUser.uid
        val passengerName = authState.userName.ifBlank { "Passenger" }

        db.collection("ride_requests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("rideDate", tomorrowDate)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()

                result.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val now = Timestamp.now()
                val savedRequests = mutableListOf<RideRequest>()

                if (wantToCampus) {
                    val toCampusRef = db.collection("ride_requests").document()
                    val toCampusRequest = RideRequest(
                        requestId = toCampusRef.id,
                        userId = userId,
                        passengerName = passengerName,
                        pickup = campusPickupLocation,
                        destination = "AUST Gate",
                        tripDirection = "to_campus",
                        tripTime = classStartText,
                        hour = classStartHour,
                        minute = classStartMinute,
                        timeMinutes = toMinutes(classStartHour, classStartMinute),
                        routeKey = buildRouteKey("to_campus", campusPickupLocation, "AUST Gate"),
                        rideDate = tomorrowDate,
                        status = "pending",
                        createdAt = now
                    )
                    batch.set(toCampusRef, toCampusRequest)
                    savedRequests.add(toCampusRequest)
                }

                if (wantToHome) {
                    val toHomeRef = db.collection("ride_requests").document()
                    val toHomeRequest = RideRequest(
                        requestId = toHomeRef.id,
                        userId = userId,
                        passengerName = passengerName,
                        pickup = "AUST Gate",
                        destination = homeReturnLocation,
                        tripDirection = "to_home",
                        tripTime = classEndText,
                        hour = classEndHour,
                        minute = classEndMinute,
                        timeMinutes = toMinutes(classEndHour, classEndMinute),
                        routeKey = buildRouteKey("to_home", "AUST Gate", homeReturnLocation),
                        rideDate = tomorrowDate,
                        status = "pending",
                        createdAt = now
                    )
                    batch.set(toHomeRef, toHomeRequest)
                    savedRequests.add(toHomeRequest)
                }

                batch.commit()
                    .addOnSuccessListener {
                        userRepository.addXpToCurrentUser(5L) { xpResult ->
                            xpResult.onSuccess { refreshPassengerXp() }
                        }

                        isSubmitting = false
                        requestSubmitted = true
                        isEditing = false
                        hasClassesTomorrow = true
                        hasAcceptedRequest = false
                        submittedDateText = tomorrowDate

                        savedRequestList.clear()
                        savedRequestList.addAll(savedRequests)
                        loadMatchedRides(savedRequests)

                        val dirLabel = when {
                            wantToCampus && wantToHome -> "both trips"
                            wantToCampus -> "campus trip"
                            else -> "return trip"
                        }

                        Toast.makeText(
                            context,
                            "Tomorrow $dirLabel saved (+5 XP)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        isSubmitting = false
                        Toast.makeText(
                            context,
                            "Submission failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                isSubmitting = false
                Toast.makeText(
                    context,
                    "Failed to prepare submission: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    LaunchedEffect(Unit) {
        loadTomorrowRequests()
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
            isLoadingRequests -> {
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
                            savedRequests = savedRequestList,
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
                            AcceptedRequestsSection(savedRequestList = savedRequestList)
                        } else {
                            when {
                                isLoadingMatches -> {
                                    PremiumLoadingCard("Loading matched rides...")
                                }

                                matchedRideCards.isEmpty() -> {
                                    EmptyStateCard(
                                        icon = Icons.Default.Info,
                                        message = "No rider matched your saved request yet."
                                    )
                                }

                                else -> {
                                    matchedRideCards.forEach { ride ->
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
                        requestSubmitted = false
                        isEditing = false
                        matchedRideCards.clear()
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

                            Spacer(modifier = Modifier.height(16.dp))

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

                            AnimatedVisibility(visible = !wantToCampus && !wantToHome) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    NeitherDirectionWarning()
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            LimeActionButton(
                                text = if (isSubmitting) "Saving..." else "Save Tomorrow Request",
                                icon = Icons.Default.CheckCircle,
                                isLoading = isSubmitting,
                                onClick = { saveTomorrowRequests() }
                            )
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
fun PassengerTomorrowSubmittedCard(
    savedRequests: SnapshotStateList<RideRequest>,
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

@Composable
fun AcceptedRequestsSection(
    savedRequestList: SnapshotStateList<RideRequest>
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
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
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
            .border(
                1.dp,
                AccentAmber.copy(alpha = 0.22f),
                RoundedCornerShape(12.dp)
            )
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