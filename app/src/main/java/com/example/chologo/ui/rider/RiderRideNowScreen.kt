@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.rider

import android.content.Context
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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.viewmodel.RideNowUiState
import com.example.chologo.viewmodel.RideNowViewModel

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

    fun goLive() {
        if (selectedPickup == selectedDestination) {
            Toast.makeText(
                context,
                "Pickup and destination cannot be the same",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (authState.userId.isBlank()) {
            Toast.makeText(
                context,
                "Rider profile not loaded yet. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            return
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
    }

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RiderRideNowIntroCard(
            riderName = authState.userName,
            isLive = uiState.isRiderLive,
            isBusy = uiState.riderLiveRide?.currentRequestId?.isNotBlank() == true
        )

        RiderSectionCard(
            title = "Ride Now",
            subtitle = "Go live, receive instant passenger requests, and manage your active trip.",
            icon = "⚡"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBadge(
                    text = if (uiState.isRiderLive) "Live now" else "Offline",
                    accent = if (uiState.isRiderLive) AccentEmerald else AccentRed
                )
                MiniBadge(
                    text = "Instant requests",
                    accent = AccentBlue
                )
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut()
        ) {
            RiderRideNowMainContent(
                uiState = uiState,
                passengerRequest = passengerRequest,
                availableRequests = availableRequests,
                riderId = authState.userId,
                riderName = authState.userName,
                riderPhone = authState.userPhone,
                selectedPickup = selectedPickup,
                selectedDestination = selectedDestination,
                showPickupMenu = showPickupMenu,
                showDestinationMenu = showDestinationMenu,
                onPickupExpandChange = {
                    showPickupMenu = it
                },
                onDestinationExpandChange = {
                    showDestinationMenu = it
                },
                onPickupSelected = { selectedValue ->
                    selectedPickup = selectedValue

                    if (selectedDestination == selectedValue) {
                        selectedDestination =
                            availableLocations.firstOrNull { location ->
                                location != selectedValue
                            } ?: "Dhanmondi"
                    }

                    showPickupMenu = false
                },
                onDestinationSelected = { selectedValue ->
                    selectedDestination = selectedValue
                    showDestinationMenu = false
                },
                onGoLive = {
                    goLive()
                },
                onStopLive = {
                    rideNowViewModel.stopLiveRide()
                },
                onAcceptRequest = { request ->
                    rideNowViewModel.acceptRideNowRequest(
                        requestId = request.requestId,
                        riderId = authState.userId,
                        riderName = authState.userName.ifBlank { "Rider" },
                        riderPhone = authState.userPhone.ifBlank { "N/A" }
                    )

                    rideNowViewModel.listenToPassengerRequest(request.requestId)
                },
                onStartTrip = {
                    rideNowViewModel.startRideNowTrip()
                },
                onCompleteTrip = {
                    rideNowViewModel.completeRideNowTrip()
                },
                onCancelRide = {
                    Toast.makeText(
                        context,
                        "Accepted Ride Now trip cannot be cancelled from here yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onCallPassenger = {
                    Toast.makeText(
                        context,
                        "Passenger phone is not available in RideNowRequest yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        RideNowInfoBanner(
            message = "Ride Now is for instant requests. Tomorrow rides are still managed from the main rider dashboard."
        )
    }
}

@Composable
private fun RiderRideNowMainContent(
    uiState: RideNowUiState,
    passengerRequest: RideNowRequest?,
    availableRequests: List<RideNowRequest>,
    riderId: String,
    riderName: String,
    riderPhone: String,
    selectedPickup: String,
    selectedDestination: String,
    showPickupMenu: Boolean,
    showDestinationMenu: Boolean,
    onPickupExpandChange: (Boolean) -> Unit,
    onDestinationExpandChange: (Boolean) -> Unit,
    onPickupSelected: (String) -> Unit,
    onDestinationSelected: (String) -> Unit,
    onGoLive: () -> Unit,
    onStopLive: () -> Unit,
    onAcceptRequest: (RideNowRequest) -> Unit,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit,
    onCancelRide: () -> Unit,
    onCallPassenger: () -> Unit
) {
    val hasActiveRequest =
        uiState.riderLiveRide?.currentRequestId?.isNotBlank() == true

    val hasLockedTrip =
        hasActiveRequest || passengerRequest?.status.isActiveRideNowStatus()

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RiderRouteAndLiveControlContent(
            uiState = uiState,
            riderId = riderId,
            riderName = riderName,
            selectedPickup = selectedPickup,
            selectedDestination = selectedDestination,
            showPickupMenu = showPickupMenu,
            showDestinationMenu = showDestinationMenu,
            hasLockedTrip = hasLockedTrip,
            onPickupExpandChange = onPickupExpandChange,
            onDestinationExpandChange = onDestinationExpandChange,
            onPickupSelected = onPickupSelected,
            onDestinationSelected = onDestinationSelected,
            onGoLive = onGoLive,
            onStopLive = onStopLive
        )

        RiderActiveTripSection(
            request = passengerRequest,
            hasActiveRequest = hasActiveRequest,
            isProcessing = uiState.isLoading,
            onStartTrip = onStartTrip,
            onCompleteTrip = onCompleteTrip,
            onCancelRide = onCancelRide,
            onCallPassenger = onCallPassenger
        )

        RiderPassengerRequestList(
            uiState = uiState,
            availableRequests = availableRequests,
            riderId = riderId,
            riderName = riderName,
            riderPhone = riderPhone,
            onAcceptRequest = onAcceptRequest
        )
    }
}

@Composable
private fun RiderRouteAndLiveControlContent(
    uiState: RideNowUiState,
    riderId: String,
    riderName: String,
    selectedPickup: String,
    selectedDestination: String,
    showPickupMenu: Boolean,
    showDestinationMenu: Boolean,
    hasLockedTrip: Boolean,
    onPickupExpandChange: (Boolean) -> Unit,
    onDestinationExpandChange: (Boolean) -> Unit,
    onPickupSelected: (String) -> Unit,
    onDestinationSelected: (String) -> Unit,
    onGoLive: () -> Unit,
    onStopLive: () -> Unit
) {
    RiderSectionCard(
        title = "Go Live",
        subtitle = "Choose your current route. Only matching passenger requests will appear.",
        icon = "🏍️"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Route match", accent = AccentEmerald)
            MiniBadge(text = "1 seat", accent = AccentBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel(text = "Trip details")
        Spacer(modifier = Modifier.height(10.dp))

        LocationSelectionCard(
            label = "Pickup location",
            selectedLocation = selectedPickup,
            expanded = showPickupMenu,
            onExpandChange = onPickupExpandChange,
            locations = availableLocations,
            onLocationSelected = onPickupSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        RouteConnectorLabel(label = "same route passengers only")

        Spacer(modifier = Modifier.height(8.dp))

        LocationSelectionCard(
            label = "Destination",
            selectedLocation = selectedDestination,
            expanded = showDestinationMenu,
            onExpandChange = onDestinationExpandChange,
            locations = availableLocations.filter { it != selectedPickup },
            onLocationSelected = onDestinationSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        RiderLiveStatusBanner(
            isLive = uiState.isRiderLive,
            hasLockedTrip = hasLockedTrip,
            selectedPickup = selectedPickup,
            selectedDestination = selectedDestination
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (riderId.isBlank() || riderName.isBlank()) {
            PremiumLoadingCard("Loading rider profile...")
            return@RiderSectionCard
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RiderLimeActionButton(
                text = when {
                    uiState.isLoading && !uiState.isRiderLive -> "Going Live..."
                    uiState.isRiderLive -> "You are Live"
                    else -> "Go Live"
                },
                isLoading = uiState.isLoading && !uiState.isRiderLive,
                enabled = !uiState.isLoading && !uiState.isRiderLive,
                modifier = Modifier.weight(1f),
                onClick = onGoLive
            )

            RiderStopLiveButton(
                text = if (hasLockedTrip) "Trip Active" else "Stop Live",
                enabled = !uiState.isLoading && uiState.isRiderLive && !hasLockedTrip,
                modifier = Modifier.weight(1f),
                onClick = onStopLive
            )
        }
    }
}

@Composable
private fun RiderActiveTripSection(
    request: RideNowRequest?,
    hasActiveRequest: Boolean,
    isProcessing: Boolean,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit,
    onCancelRide: () -> Unit,
    onCallPassenger: () -> Unit
) {
    if (!hasActiveRequest || request == null) {
        RideNowInfoBanner(
            message = "Go live on your selected route to start receiving instant passenger requests."
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel(text = "Active trip")

        when (request.status) {
            RideNowStatus.ACCEPTED -> {
                RideAcceptedCard(
                    request = request,
                    isRider = true,
                    isProcessing = isProcessing,
                    onStartTrip = onStartTrip,
                    onCancelRide = onCancelRide,
                    onCall = onCallPassenger
                )
            }

            RideNowStatus.START_PENDING_CONFIRMATION -> {
                RideConfirmationCard(
                    title = "Waiting for passenger confirmation",
                    message = "You pressed Started. The passenger must confirm that the ride has actually started.",
                    status = request.status,
                    isRider = true
                )
            }

            RideNowStatus.ONGOING -> {
                RideOngoingCard(
                    request = request,
                    isRider = true,
                    isProcessing = isProcessing,
                    onCompleteTrip = onCompleteTrip,
                    onCall = onCallPassenger
                )
            }

            RideNowStatus.END_PENDING_CONFIRMATION -> {
                RideConfirmationCard(
                    title = "Waiting for passenger completion",
                    message = "You pressed Ride Completed. The passenger must confirm safe arrival before the trip becomes completed.",
                    status = request.status,
                    isRider = true
                )
            }

            RideNowStatus.COMPLETED -> {
                RideCompletedCard(
                    request = request,
                    isRider = true
                )
            }

            RideNowStatus.CANCELLED -> {
                RideConfirmationCard(
                    title = "Ride Cancelled",
                    message = "This Ride Now trip has been cancelled.",
                    status = request.status,
                    isRider = true
                )
            }

            RideNowStatus.EXPIRED -> {
                RideConfirmationCard(
                    title = "Request Expired",
                    message = "This passenger request expired before the ride could start.",
                    status = request.status,
                    isRider = true
                )
            }

            RideNowStatus.ISSUE_REPORTED -> {
                RideConfirmationCard(
                    title = "Issue Reported",
                    message = "An issue was reported for this ride. This trip is locked for review.",
                    status = request.status,
                    isRider = true
                )
            }

            else -> {
                RideConfirmationCard(
                    title = "Ride Status",
                    message = "Current status: ${request.status}",
                    status = request.status,
                    isRider = true
                )
            }
        }
    }
}

@Composable
private fun RiderPassengerRequestList(
    uiState: RideNowUiState,
    availableRequests: List<RideNowRequest>,
    riderId: String,
    riderName: String,
    riderPhone: String,
    onAcceptRequest: (RideNowRequest) -> Unit
) {
    RiderSectionCard(
        title = "Live Passenger Requests",
        subtitle = "Passengers on your selected route will appear here in real time.",
        icon = "🙋"
    ) {
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

            uiState.riderLiveRide?.currentRequestId?.isNotBlank() == true -> {
                EmptyStateCard(
                    icon = Icons.Default.Timer,
                    message = "You already have an active passenger.\nComplete the current trip before accepting another request."
                )
            }

            availableRequests.isEmpty() -> {
                EmptyStateCard(
                    icon = Icons.Default.Person,
                    message = "No live passenger requests for this route right now.\nStay online and new requests will appear here."
                )
            }

            riderId.isBlank() || riderName.isBlank() -> {
                PremiumLoadingCard("Loading rider profile...")
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableRequests.forEach { request ->
                        RideNowPassengerRequestCard(
                            request = request,
                            isProcessing = uiState.isLoading,
                            onAccept = {
                                onAcceptRequest(request)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderRideNowIntroCard(
    riderName: String,
    isLive: Boolean,
    isBusy: Boolean
) {
    RiderSectionCard(
        title = if (riderName.isBlank()) {
            "Welcome to Ride Now"
        } else {
            "Welcome, $riderName"
        },
        subtitle = "Turn on live rider mode when you are ready to accept instant passengers.",
        icon = "👋"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(
                text = if (isLive) "Live" else "Offline",
                accent = if (isLive) AccentEmerald else AccentRed
            )

            MiniBadge(
                text = if (isBusy) "Trip active" else "Available",
                accent = if (isBusy) AccentAmber else AccentBlue
            )
        }
    }
}

@Composable
private fun RiderLiveStatusBanner(
    isLive: Boolean,
    hasLockedTrip: Boolean,
    selectedPickup: String,
    selectedDestination: String
) {
    val message = when {
        hasLockedTrip ->
            "You already accepted a passenger. Complete the current trip before going offline."

        isLive ->
            "You are live on $selectedPickup → $selectedDestination. Matching passengers can now request you."

        else ->
            "You are offline. Go live when you are ready to receive instant passenger requests."
    }

    val accent = when {
        hasLockedTrip -> AccentAmber
        isLive -> AccentEmerald
        else -> AccentBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(
                1.dp,
                accent.copy(alpha = 0.18f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = message,
            color = TextMed,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun RiderLimeActionButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Lime,
            contentColor = BgDeep,
            disabledContainerColor = LimeDeep,
            disabledContentColor = BgDeep
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = BgDeep
            )
        } else {
            Icon(
                imageVector = Icons.Default.TwoWheeler,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RiderStopLiveButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (enabled) AccentRed.copy(alpha = 0.28f) else BorderSubtle
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (enabled) AccentRed else TextLow,
            disabledContentColor = TextLow,
            containerColor = if (enabled) {
                AccentRed.copy(alpha = 0.08f)
            } else {
                CardElevated
            }
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RideNowPassengerRequestCard(
    request: RideNowRequest,
    isProcessing: Boolean,
    onAccept: () -> Unit
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
                        text = request.passengerName.ifBlank { "P" }.take(1).uppercase(),
                        color = Lime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = request.passengerName.ifBlank { "Passenger" },
                        color = TextHigh,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = request.tripTime.ifBlank { "Now" },
                        color = TextMed,
                        fontSize = 11.sp
                    )
                }
            }

            MiniBadge(
                text = request.status.toRideNowLabel(),
                accent = request.status.toRideNowAccent()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        RiderRequestRouteLine(
            pickup = request.pickup,
            destination = request.destination
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (!isProcessing) {
                    onAccept()
                }
            },
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = BgDeep,
                disabledContainerColor = LimeDeep,
                disabledContentColor = BgDeep
            )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = BgDeep,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = "Accept Request",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun RiderRequestRouteLine(
    pickup: String,
    destination: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Lime)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = pickup,
                color = TextHigh,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 3.dp, top = 4.dp, bottom = 4.dp)
                .width(1.5.dp)
                .height(18.dp)
                .background(BorderSubtle)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(AccentEmerald)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = destination,
                color = TextHigh,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RideNowInfoBanner(
    message: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentBlue.copy(alpha = 0.06f))
            .border(1.dp, AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "ℹ",
            color = AccentBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = message,
            color = TextMed,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Start
        )
    }
}

private fun String?.isActiveRideNowStatus(): Boolean {
    return this == RideNowStatus.ACCEPTED ||
            this == RideNowStatus.START_PENDING_CONFIRMATION ||
            this == RideNowStatus.ONGOING ||
            this == RideNowStatus.END_PENDING_CONFIRMATION
}

private fun String.toRideNowLabel(): String {
    return when (this) {
        RideNowStatus.SEARCHING -> "Searching"
        RideNowStatus.NOTIFIED -> "Notified"
        RideNowStatus.ACCEPTED -> "Accepted"
        RideNowStatus.START_PENDING_CONFIRMATION -> "Start Pending"
        RideNowStatus.ONGOING -> "Ongoing"
        RideNowStatus.END_PENDING_CONFIRMATION -> "End Pending"
        RideNowStatus.COMPLETED -> "Completed"
        RideNowStatus.CANCELLED -> "Cancelled"
        RideNowStatus.EXPIRED -> "Expired"
        RideNowStatus.ISSUE_REPORTED -> "Issue Reported"
        else -> replaceFirstChar { it.uppercase() }
    }
}

private fun String.toRideNowAccent(): Color {
    return when (this) {
        RideNowStatus.SEARCHING -> AccentAmber
        RideNowStatus.NOTIFIED -> AccentAmber
        RideNowStatus.ACCEPTED -> AccentEmerald
        RideNowStatus.START_PENDING_CONFIRMATION -> AccentAmber
        RideNowStatus.ONGOING -> AccentBlue
        RideNowStatus.END_PENDING_CONFIRMATION -> AccentAmber
        RideNowStatus.COMPLETED -> AccentEmerald
        RideNowStatus.CANCELLED -> AccentRed
        RideNowStatus.EXPIRED -> AccentRed
        RideNowStatus.ISSUE_REPORTED -> AccentRed
        else -> Lime
    }
}