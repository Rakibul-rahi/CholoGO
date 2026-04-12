@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chologo.R
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.viewmodel.RideNowUiState
import com.example.chologo.viewmodel.RideNowViewModel

@Composable
fun RiderRideNowScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    rideNowViewModel: RideNowViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsState()
    val uiState by rideNowViewModel.uiState.collectAsState()

    val passengerRequest: RideNowRequest? = uiState.passengerRequest
    val availableRequests: List<RideNowRequest> = uiState.availableRequests

    var selectedPickup by remember { mutableStateOf("AUST Gate") }
    var selectedDestination by remember { mutableStateOf("Dhanmondi") }

    var showPickupMenu by remember { mutableStateOf(false) }
    var showDestinationMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            rideNowViewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            rideNowViewModel.clearMessage()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDeep
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                RiderRideNowTopBar(navController = navController)
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    RiderRideNowHeroCard(
                        riderName = authState.userName,
                        isLive = uiState.isRiderLive,
                        isBusy = !uiState.riderLiveRide?.currentRequestId.isNullOrBlank()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionCard(
                        title = "Route Selection",
                        subtitle = "Choose where you are starting from and where you are going before going live."
                    ) {
                        LocationSelectionCard(
                            label = "Pickup location",
                            selectedLocation = selectedPickup,
                            expanded = showPickupMenu,
                            onExpandChange = { expandedValue ->
                                showPickupMenu = expandedValue
                            },
                            locations = availableLocations,
                            onLocationSelected = { selectedValue ->
                                selectedPickup = selectedValue

                                if (selectedDestination == selectedValue) {
                                    selectedDestination =
                                        availableLocations.firstOrNull { location ->
                                            location != selectedValue
                                        } ?: "Dhanmondi"
                                }

                                showPickupMenu = false
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LocationSelectionCard(
                            label = "Destination",
                            selectedLocation = selectedDestination,
                            expanded = showDestinationMenu,
                            onExpandChange = { expandedValue ->
                                showDestinationMenu = expandedValue
                            },
                            locations = availableLocations.filter { location ->
                                location != selectedPickup
                            },
                            onLocationSelected = { selectedValue ->
                                selectedDestination = selectedValue
                                showDestinationMenu = false
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        RideNowInfoBanner(
                            message = "Only passengers requesting this same route will appear in your live request list."
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    RiderLiveControlCard(
                        uiState = uiState,
                        riderId = authState.userId,
                        riderName = authState.userName,
                        selectedPickup = selectedPickup,
                        selectedDestination = selectedDestination,
                        onGoLive = {
                            if (selectedPickup == selectedDestination) {
                                Toast.makeText(
                                    context,
                                    "Pickup and destination cannot be the same",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@RiderLiveControlCard
                            }

                            val tripDirection =
                                if (selectedDestination.equals("AUST Gate", ignoreCase = true)) {
                                    "to_campus"
                                } else {
                                    "to_home"
                                }

                            rideNowViewModel.goLiveAsRider(
                                riderId = authState.userId,
                                riderName = authState.userName.ifBlank { "Rider" },
                                pickup = selectedPickup,
                                destination = selectedDestination,
                                tripDirection = tripDirection,
                                tripTime = "Now",
                                timeMinutes = 0,
                                routeKey = buildRouteKey(
                                    tripDirection = tripDirection,
                                    pickup = selectedPickup,
                                    destination = selectedDestination
                                ),
                                availableSeats = 1
                            )
                        },
                        onStopLive = {
                            rideNowViewModel.stopLiveRide()
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (
                        uiState.riderLiveRide?.currentRequestId?.isNotBlank() == true &&
                        passengerRequest != null
                    ) {
                        ActiveRideNowTripCard(
                            request = passengerRequest,
                            onStartTrip = { rideNowViewModel.startRideNowTrip() },
                            onCompleteTrip = { rideNowViewModel.completeRideNowTrip() }
                        )
                    } else {
                        RideNowInfoBanner(
                            message = "Go live on your selected route to start receiving instant passenger requests."
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionLabel(text = "Live Passenger Requests")
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    when {
                        uiState.isLoading && availableRequests.isEmpty() -> {
                            PremiumLoadingCard("Loading Ride Now data...")
                        }

                        !uiState.isRiderLive -> {
                            EmptyStateCard(
                                icon = Icons.Default.DirectionsCar,
                                message = "You are offline.\nSelect a route and tap Go Live to receive matching passenger requests."
                            )
                        }

                        availableRequests.isEmpty() -> {
                            EmptyStateCard(
                                icon = Icons.Default.Person,
                                message = "No live passenger requests for this route right now.\nStay online and new requests will appear here."
                            )
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                availableRequests.forEach { request ->
                                    RideNowPassengerRequestCard(
                                        request = request,
                                        isProcessing = uiState.isLoading,
                                        onAccept = {
                                            rideNowViewModel.acceptRideNowRequest(
                                                requestId = request.requestId,
                                                riderId = authState.userId,
                                                riderName = authState.userName.ifBlank { "Rider" },
                                                riderPhone = authState.userPhone.ifBlank { "N/A" }
                                            )
                                            rideNowViewModel.listenToPassengerRequest(request.requestId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    RideNowInfoBanner(
                        message = "Ride Now is for instant requests. Tomorrow rides are still managed from the main rider dashboard."
                    )
                }
            }
        }
    }
}

@Composable
fun RiderRideNowTopBar(
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BgSurface, BgDeep)))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextHigh,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Ride Now",
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Instant rider mode",
                    color = TextMed,
                    fontSize = 13.sp
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.chologologo),
            contentDescription = "CholoGO",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(48.dp)
                .wrapContentWidth()
        )
    }
}

@Composable
fun RiderRideNowHeroCard(
    riderName: String,
    isLive: Boolean,
    isBusy: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        border = BorderStroke(1.dp, BorderFocus)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientCard)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (riderName.isBlank()) "Ride Now mode" else "Hey, $riderName",
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Accept instant passenger requests when you are available right now.",
                    color = TextMed,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBadge(
                        text = if (isLive) "Live" else "Offline",
                        accent = if (isLive) AccentEmerald else AccentRed
                    )
                    MiniBadge(
                        text = if (isBusy) "Busy" else "Available",
                        accent = if (isBusy) AccentAmber else AccentBlue
                    )
                }
            }
        }
    }
}

@Composable
fun RiderLiveControlCard(
    uiState: RideNowUiState,
    riderId: String,
    riderName: String,
    selectedPickup: String,
    selectedDestination: String,
    onGoLive: () -> Unit,
    onStopLive: () -> Unit
) {
    val hasActiveRequest = !uiState.riderLiveRide?.currentRequestId.isNullOrBlank()
    val requestStatus = uiState.passengerRequest?.status
    val hasLockedTrip =
        hasActiveRequest ||
                requestStatus == RideNowStatus.ACCEPTED ||
                requestStatus == RideNowStatus.ONGOING

    SectionCard(
        title = "Live Control",
        subtitle = "Turn Ride Now mode on when you are ready to receive instant passengers."
    ) {
        if (riderId.isBlank() || riderName.isBlank()) {
            PremiumLoadingCard("Loading rider profile...")
            return@SectionCard
        }

        Text(
            text = "Selected route: $selectedPickup → $selectedDestination",
            color = TextMed,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (hasLockedTrip) {
            RideNowInfoBanner(
                message = "You already accepted a passenger request. Complete the trip before going offline."
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = if (uiState.isRiderLive) {
                            Brush.linearGradient(listOf(LimeDim, LimeDim))
                        } else {
                            GradientLime
                        }
                    )
                    .clickable(enabled = !uiState.isLoading && !uiState.isRiderLive) {
                        onGoLive()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading && !uiState.isRiderLive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = BgDeep
                    )
                } else {
                    Text(
                        text = if (uiState.isRiderLive) "You are Live" else "Go Live",
                        color = if (uiState.isRiderLive) Lime else BgDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardElevated)
                    .clickable(
                        enabled = !uiState.isLoading && uiState.isRiderLive && !hasLockedTrip
                    ) {
                        onStopLive()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasLockedTrip) "Trip Active" else "Stop Live",
                    color = if (uiState.isRiderLive && !hasLockedTrip) AccentRed else TextLow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun RideNowPassengerRequestCard(
    request: RideNowRequest,
    isProcessing: Boolean,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
                    .height(108.dp)
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
                                text = request.passengerName.ifBlank { "Passenger" },
                                color = TextHigh,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = request.tripTime.ifBlank { "Now" },
                                color = TextMed,
                                fontSize = 12.sp
                            )
                        }
                    }

                    MiniBadge(
                        text = request.status.replaceFirstChar { it.uppercase() },
                        accent = when (request.status) {
                            RideNowStatus.SEARCHING -> AccentAmber
                            RideNowStatus.ACCEPTED -> AccentEmerald
                            RideNowStatus.ONGOING -> AccentBlue
                            else -> Lime
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${request.pickup} → ${request.destination}",
                    color = TextMed,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                text = "Accept Request",
                                color = BgDeep,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveRideNowTripCard(
    request: RideNowRequest?,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit
) {
    if (request == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientSuccess)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Active Ride",
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = request.passengerName.ifBlank { "Passenger" },
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${request.pickup} → ${request.destination}",
                    color = TextMed,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                MiniBadge(
                    text = request.status.replaceFirstChar { it.uppercase() },
                    accent = when (request.status) {
                        RideNowStatus.ACCEPTED -> AccentEmerald
                        RideNowStatus.ONGOING -> AccentBlue
                        else -> Lime
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = if (request.status == RideNowStatus.ONGOING) {
                                    Brush.linearGradient(listOf(CardElevated, CardElevated))
                                } else {
                                    GradientLime
                                }
                            )
                            .clickable(enabled = request.status == RideNowStatus.ACCEPTED) {
                                onStartTrip()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start Trip",
                            color = if (request.status == RideNowStatus.ACCEPTED) BgDeep else TextLow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardElevated)
                            .clickable(enabled = request.status == RideNowStatus.ONGOING) {
                                onCompleteTrip()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Complete",
                            color = if (request.status == RideNowStatus.ONGOING) AccentEmerald else TextLow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RideNowInfoBanner(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AccentAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = TextMed,
                fontSize = 13.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}