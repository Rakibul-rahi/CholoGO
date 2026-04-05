package com.example.chologo.ui.passenger

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
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
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val AmberWarn = Color(0xFFFFA41B)
private val SuccessCard = Color(0xFF102A1A)
private val WarningCard = Color(0xFF1E2200)

private val availableLocations = listOf(
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
    "Lalbag",
    "Azimpur",
    "Gulshan Link Road",
    "Kakrail Mor",
    "AUST Gate"
)

data class RideCardUi(
    val rideId: String,
    val driverName: String,
    val rating: Float,
    val routeLabel: String,
    val pricePerSeat: Int,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val seatsLeft: Int
)

@Composable
fun PassengerDashboardScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ride Now", "Tomorrow")

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
            item { PassengerTopBar(navController) }
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
                    0 -> RideNowTab()
                    1 -> TomorrowTab()
                }
            }
        }
    }
}

@Composable
fun PassengerTopBar(navController: NavController) {
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
                    navController.navigate(Screen.Profile.createRoute("passenger")) {
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
fun RideNowTab() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val todayDate = remember { getTodayDateKey() }

    var selectedLocation by remember { mutableStateOf("Mirpur 12") }
    var selectedDestination by remember { mutableStateOf("AUST Gate") }

    var showLocationMenu by remember { mutableStateOf(false) }
    var showDestinationMenu by remember { mutableStateOf(false) }

    var departureHour by remember { mutableIntStateOf(8) }
    var departureMinute by remember { mutableIntStateOf(30) }
    var showTimePicker by remember { mutableStateOf(false) }

    var hasSearched by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val matchedRideCards = remember { mutableStateListOf<RideCardUi>() }

    val departureTimeText = formatTo12Hour(departureHour, departureMinute)
    val selectedTimeMinutes = remember(departureHour, departureMinute) {
        toMinutes(departureHour, departureMinute)
    }

    fun searchRides() {
        if (selectedLocation == selectedDestination) {
            Toast.makeText(
                context,
                "Location and destination cannot be the same",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isLoading = true
        hasSearched = true
        matchedRideCards.clear()

        db.collection("rides")
            .whereEqualTo("rideDate", todayDate)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { result ->
                val rides = result.documents.mapNotNull { doc ->
                    doc.toObject(Ride::class.java)?.copy(rideId = doc.id)
                }

                val filtered = rides.filter { ride ->
                    ride.availableSeats > 0 &&
                            ride.pickup.equals(selectedLocation, ignoreCase = true) &&
                            ride.destination.equals(selectedDestination, ignoreCase = true) &&
                            abs(ride.timeMinutes - selectedTimeMinutes) <= 30
                }

                matchedRideCards.addAll(
                    filtered.map { ride ->
                        RideCardUi(
                            rideId = ride.rideId,
                            driverName = if (ride.riderName.isBlank()) "Rider" else ride.riderName,
                            rating = 5.0f,
                            routeLabel = when (ride.tripDirection) {
                                "to_campus" -> "To Campus"
                                "to_home" -> "To Home"
                                else -> "Ride"
                            },
                            pricePerSeat = 0,
                            origin = ride.pickup,
                            destination = ride.destination,
                            departureTime = ride.tripTime,
                            seatsLeft = ride.availableSeats
                        )
                    }
                )

                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Failed to load rides: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Column {
        LocationSelectionCard(
            title = "Choose your location",
            selectedLocation = selectedLocation,
            expanded = showLocationMenu,
            onExpandChange = { showLocationMenu = it },
            locations = availableLocations,
            onLocationSelected = {
                selectedLocation = it
                if (selectedDestination == it) {
                    selectedDestination = availableLocations.firstOrNull { location -> location != it } ?: it
                }
                showLocationMenu = false
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LocationSelectionCard(
            title = "Choose your destination",
            selectedLocation = selectedDestination,
            expanded = showDestinationMenu,
            onExpandChange = { showDestinationMenu = it },
            locations = availableLocations.filter { it != selectedLocation },
            onLocationSelected = {
                selectedDestination = it
                showDestinationMenu = false
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        RideNowTimeCard(
            selectedTime = departureTimeText,
            onPickTimeClick = { showTimePicker = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { searchRides() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text(
                text = if (isLoading) "Searching..." else "Find Riders",
                color = BgDark,
                fontWeight = FontWeight.Bold
            )
        }

        if (hasSearched) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Available Riders",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$selectedLocation → $selectedDestination at $departureTimeText",
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    LoadingCard("Searching matching rides...")
                }

                matchedRideCards.isEmpty() -> {
                    InfoMessageCard(
                        title = "No Riders Found",
                        message = "No rider is available for this route and time right now."
                    )
                }

                else -> {
                    matchedRideCards.forEach { ride ->
                        RideListCard(ride = ride, onClick = {})
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        CampusTimePickerDialog(
            initialHour = departureHour,
            initialMinute = departureMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                departureHour = hour
                departureMinute = minute
                showTimePicker = false
            }
        )
    }
}

@Composable
fun TomorrowTab() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val tomorrowDate = remember { getTomorrowDateKey() }

    var hasClassesTomorrow by remember { mutableStateOf<Boolean?>(null) }

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
    var submitMessage by remember { mutableStateOf("") }
    var hasAcceptedRequest by remember { mutableStateOf(false) }

    val savedRequestList = remember { mutableStateListOf<RideRequest>() }
    val matchedRideCards = remember { mutableStateListOf<RideCardUi>() }

    val classStartText = formatTo12Hour(classStartHour, classStartMinute)
    val classEndText = formatTo12Hour(classEndHour, classEndMinute)

    fun applyRequestsToUi(requests: List<RideRequest>) {
        var foundCampus = false
        var foundHome = false
        var foundDate: String? = null
        hasAcceptedRequest = requests.any { it.status == "accepted" }

        requests.forEach { request ->
            if (request.createdAt != null && foundDate == null) {
                foundDate = formatTimestampToDate(request.createdAt)
            }

            when (request.tripDirection) {
                "to_campus" -> {
                    campusPickupLocation = if (request.pickup.isNotBlank()) request.pickup else "Mirpur 12"
                    if (request.timeMinutes > 0) {
                        classStartHour = request.timeMinutes / 60
                        classStartMinute = request.timeMinutes % 60
                    } else {
                        classStartHour = request.hour
                        classStartMinute = request.minute
                    }
                    foundCampus = true
                }

                "to_home" -> {
                    homeReturnLocation = if (request.destination.isNotBlank()) request.destination else "Mirpur 12"
                    if (request.timeMinutes > 0) {
                        classEndHour = request.timeMinutes / 60
                        classEndMinute = request.timeMinutes % 60
                    } else {
                        classEndHour = request.hour
                        classEndMinute = request.minute
                    }
                    foundHome = true
                }
            }
        }

        submittedDateText = foundDate ?: tomorrowDate
        requestSubmitted = foundCampus || foundHome
        hasClassesTomorrow = if (foundCampus || foundHome) true else null
        isEditing = !(foundCampus || foundHome)
        isLoadingRequests = false
    }

    fun loadMatchedRides(requests: List<RideRequest>) {
        val pendingRequests = requests.filter { it.status == "pending" }

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
                                driverName = if (ride.riderName.isBlank()) "Rider" else ride.riderName,
                                rating = 5.0f,
                                routeLabel = when (ride.tripDirection) {
                                    "to_campus" -> "To Campus"
                                    "to_home" -> "To Home"
                                    else -> "Ride"
                                },
                                pricePerSeat = 0,
                                origin = ride.pickup,
                                destination = ride.destination,
                                departureTime = ride.tripTime,
                                seatsLeft = ride.availableSeats
                            )
                        )
                    }
                }

                val unique = matched.distinctBy { it.rideId }
                matchedRideCards.addAll(unique)
                isLoadingMatches = false
            }
            .addOnFailureListener { e ->
                isLoadingMatches = false
                Toast.makeText(context, "Failed to load matched rides: ${e.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(context, "Could not load previous requests: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    LaunchedEffect(Unit) {
        loadTomorrowRequests()
    }

    fun saveTomorrowRequests() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        submitMessage = ""

        val userId = currentUser.uid

        db.collection("ride_requests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("rideDate", tomorrowDate)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()

                result.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val toCampusRef = db.collection("ride_requests").document()
                val toHomeRef = db.collection("ride_requests").document()

                val now = Timestamp.now()

                val toCampusRequest = RideRequest(
                    requestId = toCampusRef.id,
                    userId = userId,
                    passengerName = "",
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

                val toHomeRequest = RideRequest(
                    requestId = toHomeRef.id,
                    userId = userId,
                    passengerName = "",
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

                batch.set(toCampusRef, toCampusRequest)
                batch.set(toHomeRef, toHomeRequest)

                batch.commit()
                    .addOnSuccessListener {
                        isSubmitting = false
                        requestSubmitted = true
                        isEditing = false
                        hasClassesTomorrow = true
                        hasAcceptedRequest = false
                        submittedDateText = tomorrowDate
                        submitMessage = "Both tomorrow ride requests submitted successfully."

                        val saved = listOf(toCampusRequest, toHomeRequest)
                        savedRequestList.clear()
                        savedRequestList.addAll(saved)
                        loadMatchedRides(saved)

                        Toast.makeText(
                            context,
                            "Tomorrow ride requests submitted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        isSubmitting = false
                        requestSubmitted = false
                        submitMessage = "Submission failed: ${e.message}"
                        Toast.makeText(context, submitMessage, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                isSubmitting = false
                requestSubmitted = false
                submitMessage = "Could not load previous requests: ${e.message}"
                Toast.makeText(context, submitMessage, Toast.LENGTH_LONG).show()
            }
    }

    Column {
        when {
            isLoadingRequests -> {
                LoadingCard("Loading tomorrow requests...")
            }

            requestSubmitted && !isEditing -> {
                PassengerTomorrowSubmittedCard(
                    campusPickup = campusPickupLocation,
                    classStartText = classStartText,
                    homeReturnLocation = homeReturnLocation,
                    classEndText = classEndText,
                    submittedDate = submittedDateText,
                    isAccepted = hasAcceptedRequest,
                    onEditClick = {
                        isEditing = true
                        hasClassesTomorrow = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (hasAcceptedRequest) {
                    AcceptedRequestsSection(savedRequestList = savedRequestList)
                } else {
                    Text(
                        text = "Matched Rides",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        isLoadingMatches -> {
                            LoadingCard("Loading matched rides...")
                        }

                        matchedRideCards.isEmpty() -> {
                            InfoMessageCard(
                                title = "No Match Yet",
                                message = "No rider matched your saved requests yet."
                            )
                        }

                        else -> {
                            matchedRideCards.forEach { ride ->
                                RideListCard(ride = ride, onClick = {})
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            else -> {
                TomorrowClassPromptCard(
                    hasClassesTomorrow = hasClassesTomorrow,
                    onYesClick = {
                        hasClassesTomorrow = true
                        requestSubmitted = false
                        submitMessage = ""
                    },
                    onNoClick = {
                        hasClassesTomorrow = false
                        requestSubmitted = false
                        isEditing = false
                        submitMessage = ""
                        matchedRideCards.clear()
                    }
                )

                if (hasClassesTomorrow == true) {
                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

                    LocationSelectionCard(
                        title = "Drop location when returning home",
                        selectedLocation = homeReturnLocation,
                        expanded = showHomeReturnMenu,
                        onExpandChange = { showHomeReturnMenu = it },
                        locations = availableLocations,
                        onLocationSelected = {
                            homeReturnLocation = it
                            showHomeReturnMenu = false
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DualTimeSelectionCard(
                        classStartText = classStartText,
                        classEndText = classEndText,
                        isSubmitting = isSubmitting,
                        onPickStartTimeClick = { showStartTimePicker = true },
                        onPickEndTimeClick = { showEndTimePicker = true },
                        onSubmitClick = { saveTomorrowRequests() }
                    )
                }

                if (submitMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoMessageCard(
                        title = if (requestSubmitted) "Request Status" else "Submission Error",
                        message = submitMessage
                    )
                }
            }
        }
    }

    if (showStartTimePicker) {
        CampusTimePickerDialog(
            initialHour = classStartHour,
            initialMinute = classStartMinute,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                classStartHour = hour
                classStartMinute = minute
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        CampusTimePickerDialog(
            initialHour = classEndHour,
            initialMinute = classEndMinute,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                classEndHour = hour
                classEndMinute = minute
                showEndTimePicker = false
            }
        )
    }
}

@Composable
fun AcceptedRequestsSection(savedRequestList: SnapshotStateList<RideRequest>) {
    val acceptedRequests = savedRequestList.filter { it.status == "accepted" }

    Text(
        text = "Accepted Requests",
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (acceptedRequests.isEmpty()) {
        InfoMessageCard(
            title = "Accepted",
            message = "Your ride request was accepted."
        )
    } else {
        acceptedRequests.forEach { request ->
            AcceptedRequestCard(request = request)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AcceptedRequestCard(request: RideRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SuccessCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Accepted",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accepted",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${request.pickup} → ${request.destination}",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Time: ${request.tripTime}",
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Date: ${request.rideDate}",
                color = TextSecondary
            )

            if (request.matchedRideId.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Matched Ride ID: ${request.matchedRideId}",
                    color = TextSecondary
                )
            }

            if (request.matchedRiderId.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Matched Rider ID: ${request.matchedRiderId}",
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun RideNowTimeCard(
    selectedTime: String,
    onPickTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Choose departure time",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    Text("Departure Time: $selectedTime")
                }
            }
        }
    }
}

@Composable
fun PassengerTomorrowSubmittedCard(
    campusPickup: String,
    classStartText: String,
    homeReturnLocation: String,
    classEndText: String,
    submittedDate: String,
    isAccepted: Boolean,
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
                    text = if (isAccepted) "Tomorrow ride request accepted" else "Tomorrow ride requests submitted",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (submittedDate.isNotBlank()) {
                Text(
                    text = "Date: $submittedDate",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "To Campus: $campusPickup → AUST Gate at $classStartText",
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "To Home: AUST Gate → $homeReturnLocation at $classEndText",
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isAccepted) {
                Text(
                    text = "Status: Accepted",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, TextSecondary),
                shape = RoundedCornerShape(10.dp)
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
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun TomorrowClassPromptCard(
    hasClassesTomorrow: Boolean?,
    onYesClick: () -> Unit,
    onNoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WarningCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎓 Tomorrow's Campus Plan",
                color = AmberWarn,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Do you have classes tomorrow?",
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Button(
                    onClick = onYesClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasClassesTomorrow == true) GreenPrimary else CardDark
                    )
                ) {
                    Text(
                        text = "Yes",
                        color = if (hasClassesTomorrow == true) BgDark else TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onNoClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("No")
                }
            }
        }
    }
}

@Composable
fun LocationSelectionCard(
    title: String = "Choose your location",
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
fun DualTimeSelectionCard(
    classStartText: String,
    classEndText: String,
    isSubmitting: Boolean,
    onPickStartTimeClick: () -> Unit,
    onPickEndTimeClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Set tomorrow's class times",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Start time is used for going to campus. End time is used for returning home.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onPickStartTimeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Class Starts: $classStartText")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onPickEndTimeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Class Ends: $classEndText")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSubmitClick,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(
                    text = if (isSubmitting) "Submitting..." else "Submit Both Ride Requests",
                    color = BgDark
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusTimePickerDialog(
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

@Composable
fun InfoMessageCard(
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
fun RideListCard(ride: RideCardUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = ride.driverName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (ride.rating > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberWarn,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${ride.rating}", color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = "${ride.origin} → ${ride.destination}",
                color = TextSecondary
            )

            Text(
                text = "${ride.departureTime} · ${ride.seatsLeft} seat left",
                color = TextSecondary
            )

            if (ride.pricePerSeat > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "৳${ride.pricePerSeat}/seat",
                    color = GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun LoadingCard(text: String) {
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

private fun formatTo12Hour(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
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

private fun getTodayDateKey(): String {
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(calendar.time)
}