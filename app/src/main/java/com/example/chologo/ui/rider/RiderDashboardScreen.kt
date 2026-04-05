package com.example.chologo.ui.rider

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chologo.R
import com.example.chologo.model.Ride
import com.example.chologo.model.RideRequest
import com.example.chologo.navigation.Screen
import com.example.chologo.ui.components.LocalAdCarouselBanner
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private val BgDark = Color(0xFF0D1117)
private val CardDark = Color(0xFF161B22)
private val GreenPrimary = Color(0xFF8DC63F)
private val GreenMuted = Color(0xFF2D3B2D)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val TextMuted = Color(0xFF6E7681)
private val DotYellow = Color(0xFFF0C040)
private val DotGreen = Color(0xFF39D353)
private val CampusBlue = Color(0xFF58A6FF)
private val SuccessCard = Color(0xFF102A1A)

private val availableLocations = listOf(
    "Lalbag",
    "Mirpur 12",
    "Mirpur 11",
    "Mirpur 10",
    "Kazipara",
    "Taltola",
    "Agargoan",
    "Mohammadpur",
    "Khilgaon",
    "Dhanmondi",
    "Jigatola",
    "Azimpur",
    "Gulshan Link Road",
    "Kakrail Mor",
    "AUST Gate"
)

data class MatchedRequestCardUi(
    val rideId: String,
    val requestId: String,
    val passengerName: String,
    val tripDirection: String,
    val pickup: String,
    val destination: String,
    val tripTime: String,
    val timeMinutes: Int
)

@Composable
fun RiderDashboardScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Requests", "Tomorrow Setup")

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val savedRideList = remember { mutableStateListOf<Ride>() }
    val matchedRequestList = remember { mutableStateListOf<MatchedRequestCardUi>() }
    val dismissedRequestIds = remember { mutableStateListOf<String>() }
    val processingRequestMap = remember { mutableStateMapOf<String, Boolean>() }
    val tomorrowDate = remember { getTomorrowDateKey() }

    fun reloadMatchedRequests(rides: List<Ride>) {
        if (rides.isEmpty()) {
            matchedRequestList.clear()
            return
        }

        db.collection("ride_requests")
            .whereEqualTo("rideDate", tomorrowDate)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val requests = result.documents.mapNotNull { doc ->
                    doc.toObject(RideRequest::class.java)?.copy(requestId = doc.id)
                }

                val matched = mutableListOf<MatchedRequestCardUi>()

                rides.forEach { ride ->
                    requests.filter { request ->
                        ride.status == "active" &&
                                ride.availableSeats > 0 &&
                                ride.routeKey == request.routeKey &&
                                abs(ride.timeMinutes - request.timeMinutes) <= 30 &&
                                !dismissedRequestIds.contains(request.requestId)
                    }.forEach { request ->
                        matched.add(
                            MatchedRequestCardUi(
                                rideId = ride.rideId,
                                requestId = request.requestId,
                                passengerName = if (request.passengerName.isBlank()) "Passenger" else request.passengerName,
                                tripDirection = request.tripDirection,
                                pickup = request.pickup,
                                destination = request.destination,
                                tripTime = request.tripTime,
                                timeMinutes = request.timeMinutes
                            )
                        )
                    }
                }

                matchedRequestList.clear()
                matchedRequestList.addAll(
                    matched.distinctBy { "${it.rideId}_${it.requestId}" }
                )
            }
            .addOnFailureListener {
                matchedRequestList.clear()
            }
    }

    fun reloadSavedRides() {
        val riderUid = auth.currentUser?.uid ?: run {
            savedRideList.clear()
            matchedRequestList.clear()
            return
        }

        db.collection("rides")
            .whereEqualTo("riderId", riderUid)
            .whereEqualTo("rideDate", tomorrowDate)
            .get()
            .addOnSuccessListener { result ->
                val rides = result.documents.mapNotNull { doc ->
                    doc.toObject(Ride::class.java)?.copy(rideId = doc.id)
                }.sortedBy {
                    when (it.tripDirection.lowercase(Locale.getDefault())) {
                        "to_campus" -> 0
                        "to_home" -> 1
                        else -> 2
                    }
                }

                savedRideList.clear()
                savedRideList.addAll(rides)
                reloadMatchedRequests(rides)
            }
            .addOnFailureListener {
                savedRideList.clear()
                matchedRequestList.clear()
            }
    }

    fun acceptMatchedRequest(card: MatchedRequestCardUi, onDone: () -> Unit) {
        val riderUid = auth.currentUser?.uid
        if (riderUid == null) {
            onDone()
            return
        }

        if (card.rideId.isBlank() || card.requestId.isBlank()) {
            onDone()
            return
        }

        val rideRef = db.collection("rides").document(card.rideId)
        val requestRef = db.collection("ride_requests").document(card.requestId)

        db.runTransaction { transaction ->
            val rideSnap = transaction.get(rideRef)
            val requestSnap = transaction.get(requestRef)

            if (!rideSnap.exists()) {
                throw Exception("Ride not found")
            }

            if (!requestSnap.exists()) {
                throw Exception("Request not found")
            }

            val ride = rideSnap.toObject(Ride::class.java)
                ?: throw Exception("Invalid ride data")

            val request = requestSnap.toObject(RideRequest::class.java)
                ?: throw Exception("Invalid request data")

            val currentSeats = ride.availableSeats
            val rideStatus = ride.status
            val requestStatus = request.status

            if (ride.riderId != riderUid) {
                throw Exception("You can only accept with your own ride")
            }

            if (rideStatus != "active") {
                throw Exception("Ride is no longer active")
            }

            if (requestStatus != "pending") {
                throw Exception("Request is no longer pending")
            }

            if (currentSeats <= 0) {
                throw Exception("No seats left")
            }

            val newSeats = currentSeats - 1
            val newRideStatus = if (newSeats <= 0) "full" else "active"

            transaction.update(
                requestRef,
                mapOf(
                    "status" to "accepted",
                    "matchedRideId" to card.rideId,
                    "matchedRiderId" to riderUid,
                    "acceptedAt" to Timestamp.now()
                )
            )

            transaction.update(
                rideRef,
                mapOf(
                    "availableSeats" to newSeats,
                    "status" to newRideStatus
                )
            )
        }.addOnSuccessListener {
            dismissedRequestIds.remove(card.requestId)
            onDone()
        }.addOnFailureListener {
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        reloadSavedRides()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDark
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp)
        ) {
            item { RiderTopBar(navController) }
            item { LocalAdCarouselBanner() }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BgDark,
                    contentColor = GreenPrimary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            item {
                when (selectedTab) {
                    0 -> RiderRequestsTab(
                        savedRideList = savedRideList,
                        matchedRequestList = matchedRequestList,
                        processingRequestMap = processingRequestMap,
                        onAccept = { card ->
                            val context = navController.context
                            processingRequestMap[card.requestId] = true

                            acceptMatchedRequest(card) {
                                processingRequestMap[card.requestId] = false
                                reloadSavedRides()
                                Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDecline = { card ->
                            dismissedRequestIds.add(card.requestId)
                            matchedRequestList.removeAll { it.requestId == card.requestId }
                            Toast.makeText(navController.context, "Request declined", Toast.LENGTH_SHORT).show()
                        },
                        onRideRemoved = { reloadSavedRides() }
                    )

                    1 -> RiderTomorrowSetupTab(
                        rideDate = tomorrowDate,
                        onSaveSuccess = { reloadSavedRides() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RiderRequestsTab(
    savedRideList: SnapshotStateList<Ride>,
    matchedRequestList: SnapshotStateList<MatchedRequestCardUi>,
    processingRequestMap: Map<String, Boolean>,
    onAccept: (MatchedRequestCardUi) -> Unit,
    onDecline: (MatchedRequestCardUi) -> Unit,
    onRideRemoved: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "MATCHED PASSENGER REQUESTS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        if (matchedRequestList.isEmpty()) {
            EmptyRequestCard("No matched passenger requests right now.")
        } else {
            matchedRequestList.forEach { request ->
                MatchedRequestCard(
                    request = request,
                    isProcessing = processingRequestMap[request.requestId] == true,
                    onAccept = { onAccept(request) },
                    onDecline = { onDecline(request) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "YOUR SAVED TOMORROW RIDES",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        if (savedRideList.isEmpty()) {
            EmptyRequestCard("No saved rider setup right now.")
        } else {
            savedRideList.forEach { ride ->
                SavedRideCard(
                    ride = ride,
                    onRemove = {
                        if (ride.rideId.isBlank()) return@SavedRideCard

                        db.collection("rides")
                            .document(ride.rideId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Saved ride removed", Toast.LENGTH_SHORT).show()
                                onRideRemoved()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to remove: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                )
            }
        }

        RiderBannerCard("Set your tomorrow campus and return schedule to match more passengers")
    }
}

@Composable
private fun RiderTomorrowSetupTab(
    rideDate: String,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var campusPickupLocation by remember { mutableStateOf("Mirpur 12") }
    var homeReturnLocation by remember { mutableStateOf("Mirpur 12") }

    var showCampusPickupMenu by remember { mutableStateOf(false) }
    var showHomeReturnMenu by remember { mutableStateOf(false) }

    var campusHour by remember { mutableIntStateOf(8) }
    var campusMinute by remember { mutableIntStateOf(0) }

    var homeHour by remember { mutableIntStateOf(15) }
    var homeMinute by remember { mutableIntStateOf(30) }

    var showCampusTimePicker by remember { mutableStateOf(false) }
    var showHomeTimePicker by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var isLoadingPlan by remember { mutableStateOf(true) }
    var isPlanSubmitted by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var submittedDateText by remember { mutableStateOf("") }

    val campusTimeText = formatTo12Hour(campusHour, campusMinute)
    val homeTimeText = formatTo12Hour(homeHour, homeMinute)

    LaunchedEffect(Unit) {
        val riderUid = auth.currentUser?.uid
        if (riderUid == null) {
            isLoadingPlan = false
            isEditing = true
            return@LaunchedEffect
        }

        db.collection("rides")
            .whereEqualTo("riderId", riderUid)
            .whereEqualTo("rideDate", rideDate)
            .get()
            .addOnSuccessListener { result ->
                var foundCampus = false
                var foundHome = false
                var foundDate: String? = null

                result.documents.forEach { doc ->
                    val ride = doc.toObject(Ride::class.java)?.copy(rideId = doc.id) ?: return@forEach

                    if (ride.createdAt != null && foundDate == null) {
                        foundDate = formatTimestampToDate(ride.createdAt)
                    }

                    when (ride.tripDirection.lowercase(Locale.getDefault())) {
                        "to_campus" -> {
                            campusPickupLocation = if (ride.pickup.isNotBlank()) ride.pickup else "Mirpur 12"
                            if (ride.timeMinutes > 0) {
                                campusHour = ride.timeMinutes / 60
                                campusMinute = ride.timeMinutes % 60
                            } else {
                                parse12HourTime(ride.tripTime)?.let { (h, m) ->
                                    campusHour = h
                                    campusMinute = m
                                }
                            }
                            foundCampus = true
                        }

                        "to_home" -> {
                            homeReturnLocation = if (ride.destination.isNotBlank()) ride.destination else "Mirpur 12"
                            if (ride.timeMinutes > 0) {
                                homeHour = ride.timeMinutes / 60
                                homeMinute = ride.timeMinutes % 60
                            } else {
                                parse12HourTime(ride.tripTime)?.let { (h, m) ->
                                    homeHour = h
                                    homeMinute = m
                                }
                            }
                            foundHome = true
                        }
                    }
                }

                submittedDateText = foundDate ?: rideDate
                isPlanSubmitted = foundCampus || foundHome
                isEditing = !(foundCampus || foundHome)
                isLoadingPlan = false
            }
            .addOnFailureListener {
                isLoadingPlan = false
                isEditing = true
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            isLoadingPlan -> {
                LoadingCard("Loading tomorrow plan...")
            }

            isPlanSubmitted && !isEditing -> {
                TomorrowPlanSubmittedCard(
                    campusPickup = campusPickupLocation,
                    campusTime = campusTimeText,
                    returnLocation = homeReturnLocation,
                    returnTime = homeTimeText,
                    submittedDate = submittedDateText,
                    onEditClick = { isEditing = true }
                )
            }

            else -> {
                InfoMessageCard(
                    title = "Tomorrow Ride Setup",
                    message = "Set your to-campus and return-home ride. Date, route key, and time minutes will be saved for matching."
                )

                LocationSelectionCard(
                    title = "Pickup location for going to campus",
                    selectedLocation = campusPickupLocation,
                    expanded = showCampusPickupMenu,
                    onExpandChange = { showCampusPickupMenu = it },
                    locations = availableLocations,
                    onLocationSelected = {
                        campusPickupLocation = it
                        showCampusPickupMenu = false
                    }
                )

                TimeOnlyCard(
                    title = "To Campus Schedule",
                    timeLabel = "Departure Time",
                    selectedTimeText = campusTimeText,
                    onPickTimeClick = { showCampusTimePicker = true }
                )

                LocationSelectionCard(
                    title = "Destination location when returning home",
                    selectedLocation = homeReturnLocation,
                    expanded = showHomeReturnMenu,
                    onExpandChange = { showHomeReturnMenu = it },
                    locations = availableLocations,
                    onLocationSelected = {
                        homeReturnLocation = it
                        showHomeReturnMenu = false
                    }
                )

                TimeOnlyCard(
                    title = "Return Home Schedule",
                    timeLabel = "Departure Time",
                    selectedTimeText = homeTimeText,
                    onPickTimeClick = { showHomeTimePicker = true }
                )

                Button(
                    onClick = {
                        val riderUid = auth.currentUser?.uid
                        if (riderUid == null) {
                            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSaving = true
                        val now = Timestamp.now()

                        val campusRideRef = db.collection("rides").document()
                        val homeRideRef = db.collection("rides").document()

                        val campusRide = Ride(
                            rideId = campusRideRef.id,
                            riderId = riderUid,
                            riderName = "",
                            tripDirection = "to_campus",
                            pickup = campusPickupLocation,
                            destination = "AUST Gate",
                            tripTime = campusTimeText,
                            timeMinutes = toMinutes(campusHour, campusMinute),
                            routeKey = buildRouteKey("to_campus", campusPickupLocation, "AUST Gate"),
                            rideDate = rideDate,
                            availableSeats = 1,
                            status = "active",
                            isTomorrowSetup = true,
                            createdAt = now
                        )

                        val returnRide = Ride(
                            rideId = homeRideRef.id,
                            riderId = riderUid,
                            riderName = "",
                            tripDirection = "to_home",
                            pickup = "AUST Gate",
                            destination = homeReturnLocation,
                            tripTime = homeTimeText,
                            timeMinutes = toMinutes(homeHour, homeMinute),
                            routeKey = buildRouteKey("to_home", "AUST Gate", homeReturnLocation),
                            rideDate = rideDate,
                            availableSeats = 1,
                            status = "active",
                            isTomorrowSetup = true,
                            createdAt = now
                        )

                        db.collection("rides")
                            .whereEqualTo("riderId", riderUid)
                            .whereEqualTo("rideDate", rideDate)
                            .get()
                            .addOnSuccessListener { result ->
                                val batch = db.batch()

                                result.documents.forEach { doc ->
                                    batch.delete(doc.reference)
                                }

                                batch.set(campusRideRef, campusRide)
                                batch.set(homeRideRef, returnRide)

                                batch.commit()
                                    .addOnSuccessListener {
                                        isSaving = false
                                        isPlanSubmitted = true
                                        isEditing = false
                                        submittedDateText = rideDate
                                        onSaveSuccess()
                                        Toast.makeText(context, "Tomorrow setup saved", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isSaving = false
                                        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                isSaving = false
                                Toast.makeText(context, "Failed to load previous rides: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isSaving) "Saving..." else if (isPlanSubmitted) "Update Tomorrow Setup" else "Save Tomorrow Setup",
                        color = BgDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCampusTimePicker) {
        RiderTimePickerDialog(
            initialHour = campusHour,
            initialMinute = campusMinute,
            onDismiss = { showCampusTimePicker = false },
            onConfirm = { hour, minute ->
                campusHour = hour
                campusMinute = minute
                showCampusTimePicker = false
            }
        )
    }

    if (showHomeTimePicker) {
        RiderTimePickerDialog(
            initialHour = homeHour,
            initialMinute = homeMinute,
            onDismiss = { showHomeTimePicker = false },
            onConfirm = { hour, minute ->
                homeHour = hour
                homeMinute = minute
                showHomeTimePicker = false
            }
        )
    }
}

@Composable
private fun MatchedRequestCard(
    request: MatchedRequestCardUi,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = request.passengerName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${request.pickup} → ${request.destination}",
                color = TextSecondary
            )

            Text(
                text = request.tripTime,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text(
                        text = if (isProcessing) "Processing..." else "Accept",
                        color = BgDark,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isProcessing,
                    border = BorderStroke(1.dp, TextMuted),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("Decline", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = GreenPrimary,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, color = TextPrimary)
        }
    }
}

@Composable
private fun TomorrowPlanSubmittedCard(
    campusPickup: String,
    campusTime: String,
    returnLocation: String,
    returnTime: String,
    submittedDate: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SuccessCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Submitted",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tomorrow riding plan is submitted",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (submittedDate.isNotBlank()) {
                Text(
                    text = "Date: $submittedDate",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "To Campus: $campusPickup → AUST Gate at $campusTime",
                color = TextPrimary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "To Home: AUST Gate → $returnLocation at $returnTime",
                color = TextPrimary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Available seat: 1",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onEditClick,
                border = BorderStroke(1.dp, TextMuted),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Edit",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RiderTopBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.chologologo),
            contentDescription = "CholoGO Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(120.dp)
                .wrapContentWidth()
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GreenPrimary)
                .clickable {
                    navController.navigate(Screen.Profile.createRoute("rider")) {
                        launchSingleTop = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = BgDark,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SavedRideCard(
    ride: Ride,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = when (ride.tripDirection) {
                            "to_campus" -> "To Campus"
                            "to_home" -> "To Home"
                            else -> "Saved Ride"
                        },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${ride.pickup} → ${ride.destination}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                DirectionChip(ride.tripDirection)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = GreenMuted, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DotYellow)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ride.tripTime.ifBlank { "No time set" },
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DotGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Available seat: ${ride.availableSeats}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Date: ${ride.rideDate}",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Status: ${ride.status}",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onRemove,
                border = BorderStroke(1.dp, TextMuted),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text(
                    text = "Remove Saved Setup",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DirectionChip(direction: String) {
    val chipColor = if (direction.equals("to_campus", ignoreCase = true)) {
        CampusBlue
    } else {
        GreenPrimary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(chipColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = when (direction) {
                "to_campus" -> "To Campus"
                "to_home" -> "To Home"
                else -> "Ride"
            },
            color = BgDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyRequestCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocationSelectionCard(
    title: String,
    selectedLocation: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    locations: List<String>,
    onLocationSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { onExpandChange(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedLocation)
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Location"
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandChange(false) }
                ) {
                    locations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = { onLocationSelected(location) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeOnlyCard(
    title: String,
    timeLabel: String,
    selectedTimeText: String,
    onPickTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onPickTimeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Departure Time"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$timeLabel: $selectedTimeText")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Available seat: 1",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun InfoMessageCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SuccessCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = GreenPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun RiderBannerCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Ride Banner",
                    tint = GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = GreenPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderTimePickerDialog(
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
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Select time",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
            }
        }
    )
}

private fun formatTo12Hour(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
}

private fun parse12HourTime(time: String): Pair<Int, Int>? {
    return try {
        val parts = time.trim().split(" ")
        if (parts.size != 2) return null
        val hm = parts[0].split(":")
        if (hm.size != 2) return null

        val rawHour = hm[0].toInt()
        val minute = hm[1].toInt()
        val amPm = parts[1].uppercase(Locale.getDefault())

        val hour24 = when {
            amPm == "AM" && rawHour == 12 -> 0
            amPm == "AM" -> rawHour
            amPm == "PM" && rawHour == 12 -> 12
            amPm == "PM" -> rawHour + 12
            else -> return null
        }
        Pair(hour24, minute)
    } catch (_: Exception) {
        null
    }
}

private fun formatTimestampToDate(timestamp: Timestamp): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(timestamp.toDate())
}

private fun toMinutes(hour: Int, minute: Int): Int {
    return hour * 60 + minute
}

private fun buildRouteKey(
    tripDirection: String,
    pickup: String,
    destination: String
): String {
    return "${tripDirection.trim().lowercase()}|${pickup.trim().lowercase()}|${destination.trim().lowercase()}"
}

private fun getTomorrowDateKey(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(calendar.time)
}