@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.passenger

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.ui.common.RatingDialog
import com.example.chologo.ui.common.ReportDialog
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.viewmodel.RideNowUiState
import com.example.chologo.viewmodel.RideNowViewModel
import java.util.Calendar

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
fun PassengerRideNowScreen(
    passengerName: String,
    authViewModel: AuthViewModel = viewModel(),
    rideNowViewModel: RideNowViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsState()
    val uiState by rideNowViewModel.uiState.collectAsState()

    val passengerRequest: RideNowRequest? = uiState.passengerRequest

    val nowCalendar = remember { Calendar.getInstance() }

    var selectedLocation by remember { mutableStateOf("Mirpur 12") }
    var selectedDestination by remember { mutableStateOf("AUST Gate") }

    var showLocationMenu by remember { mutableStateOf(false) }
    var showDestinationMenu by remember { mutableStateOf(false) }

    var departureHour by remember {
        mutableIntStateOf(nowCalendar.get(Calendar.HOUR_OF_DAY))
    }

    var departureMinute by remember {
        mutableIntStateOf(nowCalendar.get(Calendar.MINUTE))
    }

    var showTimePicker by remember { mutableStateOf(false) }

    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val departureTimeText = formatTo12Hour(departureHour, departureMinute)

    val selectedTimeMinutes = remember(departureHour, departureMinute) {
        toMinutes(departureHour, departureMinute)
    }

    val isPastTime = remember(departureHour, departureMinute) {
        val now = Calendar.getInstance()

        val currentMinutes =
            now.get(Calendar.HOUR_OF_DAY) * 60 +
                    now.get(Calendar.MINUTE)

        val selectedMinutes =
            departureHour * 60 + departureMinute

        selectedMinutes < currentMinutes
    }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser()
    }

    LaunchedEffect(authState.userId) {
        if (authState.userId.isNotBlank()) {
            rideNowViewModel.listenPassengerActiveRide(
                passengerId = authState.userId
            )
        }
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

    val blockingStatuses = listOf(
        RideNowStatus.SEARCHING,
        RideNowStatus.NOTIFIED,
        RideNowStatus.ACCEPTED,
        RideNowStatus.START_PENDING_CONFIRMATION,
        RideNowStatus.ONGOING,
        RideNowStatus.END_PENDING_CONFIRMATION
    )

    val isRequestBlocked =
        passengerRequest?.status in blockingStatuses ||
                uiState.isRequestActive

    fun submitRideNowRequest() {
        if (isRequestBlocked) {
            Toast.makeText(
                context,
                "You already have an active Ride Now request.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (selectedLocation == selectedDestination) {
            Toast.makeText(
                context,
                "Pickup and destination cannot be the same",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isPastTime) {
            Toast.makeText(
                context,
                "Please select a departure time that is not in the past.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (authState.userId.isBlank()) {
            Toast.makeText(
                context,
                "User not loaded yet. Please try again.",
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

        rideNowViewModel.createRideNowRequest(
            passengerId = authState.userId,
            passengerName = authState.userName.ifBlank {
                passengerName.ifBlank { "Passenger" }
            },
            pickup = selectedLocation,
            destination = selectedDestination,
            tripTime = departureTimeText,
            timeMinutes = selectedTimeMinutes,
            routeKey = buildRouteKey(
                tripDirection = tripDirection,
                pickup = selectedLocation,
                destination = selectedDestination
            )
        )
    }

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CompactGreetingCard(passengerName = passengerName)

        PassengerSectionCard(
            title = "Ride Now",
            subtitle = "Request an instant campus ride and get matched with a live rider.",
            icon = "⚡"
        )  {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBadge(text = "Instant match", accent = AccentEmerald)
                MiniBadge(text = "Live riders", accent = AccentBlue)
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut()
        ) {
            when (passengerRequest?.status) {
                RideNowStatus.SEARCHING,
                RideNowStatus.NOTIFIED -> {
                    PassengerActiveRideNowRequestCard(
                        request = passengerRequest,
                        isLoading = uiState.isLoading,
                        onCancelRequest = {
                            rideNowViewModel.cancelRideNowRequest()
                        }
                    )
                }

                RideNowStatus.ACCEPTED -> {
                    PassengerRideAcceptedCard(
                        request = passengerRequest,
                        onCallRider = {
                            openDialer(context, passengerRequest.matchedRiderPhone)
                        },
                        onCancelRide = {
                            Toast.makeText(
                                context,
                                "Accepted ride cannot be cancelled from here.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                RideNowStatus.START_PENDING_CONFIRMATION -> {
                    PassengerRideStartConfirmationCard(
                        request = passengerRequest,
                        onConfirmStarted = {
                            rideNowViewModel.confirmRideStarted(passengerRequest.requestId)
                        },
                        onRejectStarted = {
                            rideNowViewModel.rejectRideStarted(passengerRequest.requestId)
                        }
                    )
                }

                RideNowStatus.ONGOING -> {
                    PassengerRideOngoingCard(
                        request = passengerRequest,
                        onCallRider = {
                            openDialer(context, passengerRequest.matchedRiderPhone)
                        }
                    )
                }

                RideNowStatus.END_PENDING_CONFIRMATION -> {
                    PassengerRideCompletionConfirmationCard(
                        request = passengerRequest,
                        onConfirmCompleted = {
                            rideNowViewModel.confirmRideCompleted(
                                requestId = passengerRequest.requestId,
                                liveRideId = passengerRequest.matchedRideId
                            )
                        },
                        onReportIssue = {
                            showReportDialog = true
                        }
                    )
                }

                RideNowStatus.COMPLETED -> {
                    PassengerRideCompletedCard(
                        request = passengerRequest,
                        onRateRide = {
                            showRatingDialog = true
                        },
                        onReportRide = {
                            showReportDialog = true
                        }
                    )
                }

                RideNowStatus.CANCELLED,
                RideNowStatus.EXPIRED,
                RideNowStatus.ISSUE_REPORTED,
                null -> {
                    PassengerSearchRideNowContent(
                        uiState = uiState,
                        selectedLocation = selectedLocation,
                        selectedDestination = selectedDestination,
                        showLocationMenu = showLocationMenu,
                        showDestinationMenu = showDestinationMenu,
                        departureTimeText = departureTimeText,
                        isPastTime = isPastTime,
                        isRequestBlocked = isRequestBlocked,
                        onLocationExpandChange = { showLocationMenu = it },
                        onDestinationExpandChange = { showDestinationMenu = it },
                        onLocationSelected = {
                            selectedLocation = it

                            if (selectedDestination == it) {
                                selectedDestination =
                                    availableLocations.firstOrNull { loc ->
                                        loc != it
                                    } ?: "AUST Gate"
                            }

                            showLocationMenu = false
                        },
                        onDestinationSelected = {
                            selectedDestination = it
                            showDestinationMenu = false
                        },
                        onPickTimeClick = {
                            showTimePicker = true
                        },
                        onSubmit = {
                            submitRideNowRequest()
                        }
                    )
                }

                else -> {
                    PassengerSearchRideNowContent(
                        uiState = uiState,
                        selectedLocation = selectedLocation,
                        selectedDestination = selectedDestination,
                        showLocationMenu = showLocationMenu,
                        showDestinationMenu = showDestinationMenu,
                        departureTimeText = departureTimeText,
                        isPastTime = isPastTime,
                        isRequestBlocked = isRequestBlocked,
                        onLocationExpandChange = { showLocationMenu = it },
                        onDestinationExpandChange = { showDestinationMenu = it },
                        onLocationSelected = {
                            selectedLocation = it
                            showLocationMenu = false
                        },
                        onDestinationSelected = {
                            selectedDestination = it
                            showDestinationMenu = false
                        },
                        onPickTimeClick = {
                            showTimePicker = true
                        },
                        onSubmit = {
                            submitRideNowRequest()
                        }
                    )
                }
            }
        }

        if (
            !uiState.isRequestActive &&
            uiState.currentRequestId != null &&
            passengerRequest != null &&
            passengerRequest.status == RideNowStatus.EXPIRED
        ) {
            EmptyStateCard(
                icon = Icons.Default.Warning,
                message = "Your Ride Now request expired. Please try again with a new request."
            )
        }
    }

    if (showTimePicker) {
        PassengerTimePickerDialogWithMinTime(
            initialHour = departureHour,
            initialMinute = departureMinute,
            minHour = nowCalendar.get(Calendar.HOUR_OF_DAY),
            minMinute = nowCalendar.get(Calendar.MINUTE),
            onDismiss = {
                showTimePicker = false
            },
            onConfirm = { h, m ->
                val currentMinutes =
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
                            Calendar.getInstance().get(Calendar.MINUTE)

                val pickedMinutes = h * 60 + m

                if (pickedMinutes < currentMinutes) {
                    Toast.makeText(
                        context,
                        "Cannot select a time earlier than now.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    departureHour = h
                    departureMinute = m
                    showTimePicker = false
                }
            }
        )
    }

    if (showRatingDialog && passengerRequest != null) {
        RatingDialog(
            onDismiss = {
                showRatingDialog = false
            },
            onSubmit = { stars, comment ->
                if (authState.userId.isBlank()) {
                    Toast.makeText(
                        context,
                        "User not loaded yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@RatingDialog
                }

                if (passengerRequest.matchedRiderId.isBlank()) {
                    Toast.makeText(
                        context,
                        "Rider not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@RatingDialog
                }

                rideNowViewModel.submitRideRating(
                    ratedBy = authState.userId,
                    ratedTo = passengerRequest.matchedRiderId,
                    stars = stars,
                    comment = comment
                )

                showRatingDialog = false
            }
        )
    }

    if (showReportDialog && passengerRequest != null) {
        ReportDialog(
            onDismiss = {
                showReportDialog = false
            },
            onSubmit = { reason, details ->
                if (authState.userId.isBlank()) {
                    Toast.makeText(
                        context,
                        "User not loaded yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@ReportDialog
                }

                if (passengerRequest.matchedRiderId.isBlank()) {
                    Toast.makeText(
                        context,
                        "Rider not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@ReportDialog
                }

                rideNowViewModel.submitRideReport(
                    reportedBy = authState.userId,
                    reportedUserId = passengerRequest.matchedRiderId,
                    reason = reason,
                    details = details
                )

                showReportDialog = false
            }
        )
    }
}

@Composable
private fun PassengerSearchRideNowContent(
    uiState: RideNowUiState,
    selectedLocation: String,
    selectedDestination: String,
    showLocationMenu: Boolean,
    showDestinationMenu: Boolean,
    departureTimeText: String,
    isPastTime: Boolean,
    isRequestBlocked: Boolean,
    onLocationExpandChange: (Boolean) -> Unit,
    onDestinationExpandChange: (Boolean) -> Unit,
    onLocationSelected: (String) -> Unit,
    onDestinationSelected: (String) -> Unit,
    onPickTimeClick: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PassengerSectionCard(
            title = "Find an Instant Ride",
            subtitle = "Create a live request and nearby riders can accept it in real time.",
            icon = "🔍"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBadge(text = "Live request", accent = AccentEmerald)
                MiniBadge(text = "Auto expires", accent = AccentAmber)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(text = "Trip details")
            Spacer(modifier = Modifier.height(10.dp))

            LocationSelectionCard(
                label = "Pickup location",
                selectedLocation = selectedLocation,
                expanded = showLocationMenu,
                onExpandChange = onLocationExpandChange,
                locations = availableLocations,
                leadingIcon = Icons.Default.LocationOn,
                onLocationSelected = onLocationSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            LocationSelectionCard(
                label = "Destination",
                selectedLocation = selectedDestination,
                expanded = showDestinationMenu,
                onExpandChange = onDestinationExpandChange,
                locations = availableLocations.filter { it != selectedLocation },
                leadingIcon = Icons.Default.School,
                onLocationSelected = onDestinationSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            PassengerTimeSelectionCard(
                label = "Departure time",
                selectedTimeText = departureTimeText,
                helper = when {
                    isPastTime ->
                        "This time is in the past. Please choose a later time."

                    isRequestBlocked ->
                        "You already have an active Ride Now request."

                    else ->
                        "Your request becomes visible to currently live riders."
                },
                helperColor = if (isPastTime || isRequestBlocked) {
                    AccentRed
                } else {
                    TextLow
                },
                onPickTimeClick = onPickTimeClick
            )

            AnimatedVisibility(
                visible = isPastTime || isRequestBlocked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    if (isPastTime) {
                        PastTimeWarningBanner()
                    } else {
                        EmptyStateCard(
                            icon = Icons.Default.Warning,
                            message = "You already have an active Ride Now request."
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LimeActionButton(
                text = when {
                    uiState.isLoading -> "Sending Request..."
                    isRequestBlocked -> "Request Active"
                    else -> "Find Ride Now"
                },
                icon = Icons.Default.Search,
                isLoading = uiState.isLoading,
                onClick = {
                    if (!isPastTime && !isRequestBlocked && !uiState.isLoading) {
                        onSubmit()
                    }
                }
            )
        }

        RideNowInfoBanner(
            message = "After you send a request, live riders on a matching route can accept it instantly."
        )
    }
}

@Composable
private fun PassengerActiveRideNowRequestCard(
    request: RideNowRequest,
    isLoading: Boolean,
    onCancelRequest: () -> Unit
) {
    PassengerSectionCard(
        title = "Ride Request Active",
        subtitle = "You have already requested a Ride Now trip. Please wait for a rider or cancel this request."
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniBadge(
                text = if (request.status == RideNowStatus.NOTIFIED) {
                    "Rider Notified"
                } else {
                    "Searching"
                },
                accent = AccentAmber
            )

            SectionLabel(text = "Trip details")

            Text(
                text = "Pickup: ${request.pickup}",
                color = TextHigh
            )

            Text(
                text = "Destination: ${request.destination}",
                color = TextHigh
            )

            Text(
                text = "Time: ${request.tripTime}",
                color = TextLow
            )

            Text(
                text = "Status: ${request.status}",
                color = TextLow
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    if (!isLoading) {
                        onCancelRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLoading) {
                        "Cancelling..."
                    } else {
                        "Cancel Current Request"
                    }
                )
            }
        }
    }
}