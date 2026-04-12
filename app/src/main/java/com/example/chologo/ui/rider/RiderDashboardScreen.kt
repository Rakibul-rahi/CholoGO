@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.rider

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideRequest
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.example.chologo.ui.components.LocalAdCarouselBanner
import com.example.chologo.utils.LevelSystem
import com.example.chologo.viewmodel.AuthViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

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
fun RiderDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Requests", "Tomorrow")

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val authState by authViewModel.uiState.collectAsState()

    val userRepository = remember { UserRepository() }

    var riderXp by remember { mutableStateOf(0L) }
    var isLevelLoading by remember { mutableStateOf(true) }

    val savedRideList = remember { mutableStateListOf<Ride>() }
    val matchedRequestList = remember { mutableStateListOf<MatchedRequestCardUi>() }
    val processingRequestMap = remember { mutableStateMapOf<String, Boolean>() }

    val tomorrowDate = remember { getTomorrowDateKey() }

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

    val levelInfo = remember(riderXp) {
        LevelSystem.getLevelInfo(riderXp)
    }

    fun refreshRiderXp() {
        userRepository.getCurrentUserData { result ->
            result.onSuccess { user ->
                riderXp = user.xp
            }
        }
    }

    fun reloadMatchedRequests(rides: List<Ride>) {
        val riderUid = auth.currentUser?.uid ?: run {
            matchedRequestList.clear()
            return
        }

        if (rides.isEmpty()) {
            matchedRequestList.clear()
            return
        }

        db.collection("ride_requests")
            .whereEqualTo("rideDate", tomorrowDate)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val requests = result.documents.mapNotNull {
                    it.toObject(RideRequest::class.java)?.copy(requestId = it.id)
                }

                val matched = mutableListOf<MatchedRequestCardUi>()

                rides.forEach { ride ->
                    requests.filter { req ->
                        ride.status == "active" &&
                                ride.availableSeats > 0 &&
                                ride.routeKey == req.routeKey &&
                                abs(ride.timeMinutes - req.timeMinutes) <= 30 &&
                                !req.rejectedByRiderIds.contains(riderUid)
                    }.forEach { req ->
                        matched.add(
                            MatchedRequestCardUi(
                                rideId = ride.rideId,
                                requestId = req.requestId,
                                passengerName = req.passengerName.ifBlank { "Passenger" },
                                tripDirection = req.tripDirection,
                                pickup = req.pickup,
                                destination = req.destination,
                                tripTime = req.tripTime,
                                timeMinutes = req.timeMinutes
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
                val rides = result.documents.mapNotNull {
                    it.toObject(Ride::class.java)?.copy(rideId = it.id)
                }.sortedBy {
                    when (it.tripDirection.lowercase()) {
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

    fun rejectMatchedRequest(
        card: MatchedRequestCardUi,
        onDone: (Boolean, String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onDone(false, "Not logged in")
            return
        }

        if (card.requestId.isBlank()) {
            onDone(false, "Invalid request")
            return
        }

        db.collection("ride_requests").document(card.requestId)
            .update("rejectedByRiderIds", FieldValue.arrayUnion(uid))
            .addOnSuccessListener { onDone(true, "Request declined") }
            .addOnFailureListener { onDone(false, it.message ?: "Failed") }
    }

    fun acceptMatchedRequest(
        card: MatchedRequestCardUi,
        onDone: (Boolean, String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onDone(false, "Not logged in")
            return
        }

        if (card.rideId.isBlank() || card.requestId.isBlank()) {
            onDone(false, "Invalid data")
            return
        }

        val riderName = authState.userName.ifBlank { "Rider" }
        val riderPhone = authState.userPhone.ifBlank { "N/A" }

        val rideRef = db.collection("rides").document(card.rideId)
        val requestRef = db.collection("ride_requests").document(card.requestId)

        db.runTransaction { tx ->
            val rideSnap = tx.get(rideRef)
            val requestSnap = tx.get(requestRef)

            if (!rideSnap.exists()) throw Exception("Ride not found")
            if (!requestSnap.exists()) throw Exception("Request not found")

            val ride = rideSnap.toObject(Ride::class.java) ?: throw Exception("Invalid ride")
            val req = requestSnap.toObject(RideRequest::class.java) ?: throw Exception("Invalid request")

            if (ride.riderId != uid) throw Exception("Not your ride")
            if (ride.status != "active") throw Exception("Ride no longer active")
            if (ride.availableSeats <= 0) throw Exception("No seats available")
            if (req.status != "pending") throw Exception("Request already handled")

            val newSeats = ride.availableSeats - 1

            tx.update(
                requestRef,
                mapOf(
                    "status" to "accepted",
                    "matchedRideId" to ride.rideId,
                    "matchedRiderId" to uid,
                    "matchedRiderName" to riderName,
                    "matchedRiderPhone" to riderPhone,
                    "matchedRideTime" to ride.tripTime,
                    "acceptedAt" to Timestamp.now()
                )
            )

            tx.update(
                rideRef,
                mapOf(
                    "availableSeats" to newSeats,
                    "status" to if (newSeats <= 0) "full" else "active"
                )
            )
        }
            .addOnSuccessListener {
                userRepository.addXpToCurrentUser(10L) { xpResult ->
                    xpResult.onSuccess { refreshRiderXp() }
                }
                onDone(true, "Passenger accepted! (+10 XP)")
            }
            .addOnFailureListener {
                onDone(false, it.message ?: "Failed")
            }
    }

    LaunchedEffect(Unit) {
        reloadSavedRides()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDeep
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item { RiderTopBar(navController) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    RiderHeroCard(
                        riderName = authState.userName,
                        levelInfo = levelInfo,
                        isLevelLoading = isLevelLoading
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    LocalAdCarouselBanner()
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    RideNowEntryCard(
                        onOpenRideNow = {
                            navController.navigate(Screen.RiderRideNow.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                PremiumTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 16 } togetherWith
                                fadeOut(tween(160))
                    },
                    label = "rider_tab_content"
                ) { tab ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        when (tab) {
                            0 -> RiderRequestsTab(
                                savedRideList = savedRideList,
                                matchedRequestList = matchedRequestList,
                                processingRequestMap = processingRequestMap,
                                onAccept = { card ->
                                    processingRequestMap[card.requestId] = true
                                    acceptMatchedRequest(card) { ok, msg ->
                                        processingRequestMap[card.requestId] = false
                                        if (ok) reloadSavedRides()
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDecline = { card ->
                                    processingRequestMap[card.requestId] = true
                                    rejectMatchedRequest(card) { ok, msg ->
                                        processingRequestMap[card.requestId] = false
                                        if (ok) reloadSavedRides()
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRideRemoved = { reloadSavedRides() }
                            )

                            1 -> RiderTomorrowSetupTab(
                                rideDate = tomorrowDate,
                                riderName = authState.userName,
                                userRepository = userRepository,
                                onSaveSuccess = {
                                    refreshRiderXp()
                                    reloadSavedRides()
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
fun RideNowEntryCard(
    onOpenRideNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BorderFocus)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1A2233), Color(0xFF111A24))
                    )
                )
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(88.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        GradientLime,
                        RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Ride Now",
                        color = TextHigh,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Go live and manage instant ride requests from a separate page.",
                        color = TextMed
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MiniBadge(
                        text = "Instant matching",
                        accent = AccentAmber
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(GradientLime)
                        .clickable { onOpenRideNow() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = BgDeep,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Open",
                            color = BgDeep,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RiderRequestsTab(
    savedRideList: SnapshotStateList<Ride>,
    matchedRequestList: SnapshotStateList<MatchedRequestCardUi>,
    processingRequestMap: Map<String, Boolean>,
    onAccept: (MatchedRequestCardUi) -> Unit,
    onDecline: (MatchedRequestCardUi) -> Unit,
    onRideRemoved: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        SectionLabel(text = "Passenger Matches")

        if (matchedRequestList.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.DirectionsCar,
                message = "No matched requests right now.\nCheck back after setting up your tomorrow ride."
            )
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

        Spacer(modifier = Modifier.height(8.dp))
        SectionLabel(text = "Saved Tomorrow Rides")

        if (savedRideList.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.DirectionsCar,
                message = "No rides saved yet.\nHead to the Tomorrow tab to set up your schedule."
            )
        } else {
            savedRideList.forEach { ride ->
                SavedRideCard(
                    ride = ride,
                    onRemove = {
                        if (ride.rideId.isBlank()) return@SavedRideCard

                        db.collection("rides").document(ride.rideId).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Ride removed", Toast.LENGTH_SHORT).show()
                                onRideRemoved()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                )
            }
        }

        InfoBannerCard("Ride Now has been moved to its own page. Tomorrow rides stay here.")
    }
}

@Composable
fun MatchedRequestCard(
    request: MatchedRequestCardUi,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BorderFocus)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientCard)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(100.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        GradientLime,
                        RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LimeDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Lime,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = request.passengerName,
                                color = TextHigh,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = request.tripTime,
                                color = TextMed
                            )
                        }
                    }

                    DirectionBadge(request.tripDirection)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${request.pickup} → ${request.destination}",
                    color = TextMed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            .clickable(enabled = !isProcessing) { onAccept() },
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
                                    Icons.Default.Check,
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
                            .clickable(enabled = !isProcessing) { onDecline() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
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
    }
}

@Composable
fun SavedRideCard(
    ride: Ride,
    onRemove: () -> Unit
) {
    val isCampus = ride.tripDirection.equals("to_campus", ignoreCase = true)
    val accentColor = if (isCampus) AccentBlue else AccentEmerald

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (isCampus) Color(0xFF101A2E) else Color(0xFF0C2018),
                            CardBase
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(80.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        accentColor,
                        RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
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
                            color = TextHigh,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${ride.pickup} → ${ride.destination}",
                            color = TextMed
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = ride.tripTime,
                            color = TextMed
                        )
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

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .clickable { onRemove() }
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
}