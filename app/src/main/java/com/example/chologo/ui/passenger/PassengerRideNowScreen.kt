@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.passenger

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.viewmodel.AuthViewModel
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

    var departureHour by remember { mutableIntStateOf(nowCalendar.get(Calendar.HOUR_OF_DAY)) }
    var departureMinute by remember { mutableIntStateOf(nowCalendar.get(Calendar.MINUTE)) }
    var showTimePicker by remember { mutableStateOf(false) }

    val departureTimeText = formatTo12Hour(departureHour, departureMinute)

    val selectedTimeMinutes = remember(departureHour, departureMinute) {
        toMinutes(departureHour, departureMinute)
    }

    val isPastTime = remember(departureHour, departureMinute) {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val selectedMinutes = departureHour * 60 + departureMinute
        selectedMinutes < currentMinutes
    }

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

    fun submitRideNowRequest() {
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

        val tripDirection = if (selectedDestination.equals("AUST Gate", ignoreCase = true)) {
            "to_campus"
        } else {
            "to_home"
        }

        rideNowViewModel.createRideNowRequest(
            passengerId = authState.userId,
            passengerName = authState.userName.ifBlank { passengerName.ifBlank { "Passenger" } },
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

    val canCallRider = passengerRequest != null &&
            passengerRequest.matchedRiderPhone.isNotBlank() &&
            (passengerRequest.status == RideNowStatus.ACCEPTED ||
                    passengerRequest.status == RideNowStatus.ONGOING)

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CompactGreetingCard(passengerName = passengerName)

        PassengerSectionCard(
            title = "Ride Now",
            subtitle = "Request an instant campus ride and get matched with a live rider."
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBadge(text = "Instant match", accent = AccentEmerald)
                MiniBadge(text = "Live riders", accent = AccentBlue)
            }
        }

        when {
            uiState.isRequestActive && passengerRequest != null -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut()
                ) {
                    PassengerRideNowStatusCard(
                        passengerRequest = passengerRequest,
                        onCancel = { rideNowViewModel.cancelRideNowRequest() },
                        onCallRider = {
                            openDialer(context, passengerRequest.matchedRiderPhone)
                        },
                        showCallButton =
                            passengerRequest.matchedRiderPhone.isNotBlank() &&
                                    (passengerRequest.status == RideNowStatus.ACCEPTED ||
                                            passengerRequest.status == RideNowStatus.ONGOING)
                    )
                }
            }

            else -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PassengerSectionCard(
                            title = "Find an Instant Ride",
                            subtitle = "Create a live request and nearby riders can accept it in real time."
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
                                onExpandChange = { showLocationMenu = it },
                                locations = availableLocations,
                                leadingIcon = Icons.Default.LocationOn,
                                onLocationSelected = {
                                    selectedLocation = it
                                    if (selectedDestination == it) {
                                        selectedDestination =
                                            availableLocations.firstOrNull { loc -> loc != it }
                                                ?: "AUST Gate"
                                    }
                                    showLocationMenu = false
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LocationSelectionCard(
                                label = "Destination",
                                selectedLocation = selectedDestination,
                                expanded = showDestinationMenu,
                                onExpandChange = { showDestinationMenu = it },
                                locations = availableLocations.filter { it != selectedLocation },
                                leadingIcon = Icons.Default.School,
                                onLocationSelected = {
                                    selectedDestination = it
                                    showDestinationMenu = false
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            PassengerTimeSelectionCard(
                                label = "Departure time",
                                selectedTimeText = departureTimeText,
                                helper = if (isPastTime) {
                                    "This time is in the past. Please choose a later time."
                                } else {
                                    "Your request becomes visible to currently live riders."
                                },
                                helperColor = if (isPastTime) AccentRed else TextLow,
                                onPickTimeClick = { showTimePicker = true }
                            )

                            AnimatedVisibility(
                                visible = isPastTime,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    PastTimeWarningBanner()
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            LimeActionButton(
                                text = if (uiState.isLoading) {
                                    "Sending Request..."
                                } else {
                                    "Find Ride Now"
                                },
                                icon = Icons.Default.Search,
                                isLoading = uiState.isLoading,
                                onClick = { submitRideNowRequest() }
                            )
                        }

                        RideNowInfoBanner(
                            message = "After you send a request, live riders on a matching route can accept it instantly."
                        )
                    }
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
            onDismiss = { showTimePicker = false },
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
}