@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.rider

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.chologo.viewmodel.TomorrowRideViewModel

@Composable
fun RiderTomorrowSetupTab(
    riderId: String,
    rideDate: String,
    riderName: String,
    tomorrowRideViewModel: TomorrowRideViewModel = viewModel(),
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val uiState by tomorrowRideViewModel.uiState.collectAsState()

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

    var isEditing by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }

    val campusTimeText = formatTo12Hour(campusHour, campusMinute)
    val homeTimeText = formatTo12Hour(homeHour, homeMinute)

    val campusRide = uiState.savedRides.firstOrNull { it.tripDirection == "to_campus" }
    val homeRide = uiState.savedRides.firstOrNull { it.tripDirection == "to_home" }

    val isCampusLocked = campusRide != null && campusRide.status != "active"
    val isHomeLocked = homeRide != null && homeRide.status != "active"

    val isPlanSubmitted = uiState.savedRides.isNotEmpty()
    val submittedDateText = uiState.savedRides.firstOrNull()?.rideDate ?: rideDate

    LaunchedEffect(riderId) {
        if (riderId.isNotBlank()) {
            tomorrowRideViewModel.startRiderSession(riderId, rideDate)
        }
    }

    // Populate editable fields from the live-listener data, but only when
    // the user isn't actively mid-edit, so an incoming update (e.g. this
    // rider's own write echoing back) doesn't clobber unsaved changes.
    LaunchedEffect(uiState.savedRides, isEditing) {
        if (isEditing) return@LaunchedEffect

        campusRide?.let { ride ->
            campusPickupLocation = ride.pickup.ifBlank { "Mirpur 12" }
            if (ride.timeMinutes > 0) {
                campusHour = ride.timeMinutes / 60
                campusMinute = ride.timeMinutes % 60
            }
        }

        homeRide?.let { ride ->
            homeReturnLocation = ride.destination.ifBlank { "Mirpur 12" }
            if (ride.timeMinutes > 0) {
                homeHour = ride.timeMinutes / 60
                homeMinute = ride.timeMinutes % 60
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        when {
            !hasLoadedOnce -> PremiumLoadingCard("Loading tomorrow plan...")

            isPlanSubmitted && !isEditing -> {
                TomorrowPlanSubmittedCard(
                    campusPickup = campusPickupLocation,
                    campusTime = campusTimeText,
                    campusLocked = isCampusLocked,
                    returnLocation = homeReturnLocation,
                    returnTime = homeTimeText,
                    returnLocked = isHomeLocked,
                    submittedDate = submittedDateText,
                    onEditClick = { isEditing = true }
                )
            }

            else -> {
                SetupHeaderCard()

                SetupSectionHeader(
                    icon = Icons.Default.School,
                    tint = AccentBlue,
                    label = "Going to Campus"
                )

                if (isCampusLocked) {
                    LockedLegNotice(
                        message = "This leg is already matched with a passenger and can't be edited here."
                    )
                } else {
                    LocationSelectionCard(
                        label = "Pickup from",
                        selectedLocation = campusPickupLocation,
                        expanded = showCampusPickupMenu,
                        onExpandChange = { showCampusPickupMenu = it },
                        locations = availableLocations.filter { it != "AUST Gate" },
                        onLocationSelected = {
                            campusPickupLocation = it
                            showCampusPickupMenu = false
                        }
                    )

                    TimeSelectionCard(
                        label = "Departure time",
                        selectedTimeText = campusTimeText,
                        onPickTimeClick = { showCampusTimePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                SetupSectionHeader(
                    icon = Icons.Default.Home,
                    tint = AccentEmerald,
                    label = "Returning Home"
                )

                if (isHomeLocked) {
                    LockedLegNotice(
                        message = "This leg is already matched with a passenger and can't be edited here."
                    )
                } else {
                    LocationSelectionCard(
                        label = "Drop at",
                        selectedLocation = homeReturnLocation,
                        expanded = showHomeReturnMenu,
                        onExpandChange = { showHomeReturnMenu = it },
                        locations = availableLocations.filter { it != "AUST Gate" },
                        onLocationSelected = {
                            homeReturnLocation = it
                            showHomeReturnMenu = false
                        }
                    )

                    TimeSelectionCard(
                        label = "Departure time",
                        selectedTimeText = homeTimeText,
                        onPickTimeClick = { showHomeTimePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isCampusLocked && isHomeLocked) {
                    RideNowInfoBanner(
                        message = "Both legs are already matched with passengers. There's nothing left to edit for tomorrow."
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = if (uiState.isLoading) {
                                    Brush.linearGradient(listOf(LimeDim, LimeDim))
                                } else {
                                    Brush.linearGradient(listOf(Lime, LimeDeep))
                                }
                            )
                            .clickable(enabled = !uiState.isLoading) {
                                if (riderId.isBlank()) {
                                    Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT)
                                        .show()
                                    return@clickable
                                }

                                tomorrowRideViewModel.saveRiderPlan(
                                    riderId = riderId,
                                    riderName = riderName,
                                    rideDate = rideDate,
                                    campusPickup = campusPickupLocation,
                                    campusTripTime = campusTimeText,
                                    campusTimeMinutes = toMinutes(campusHour, campusMinute),
                                    homeDestination = homeReturnLocation,
                                    homeTripTime = homeTimeText,
                                    homeTimeMinutes = toMinutes(homeHour, homeMinute),
                                    onXpAwarded = onSaveSuccess
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = BgDeep,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Saving...",
                                    color = BgDeep,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        } else {
                            Text(
                                text = if (isPlanSubmitted) "Update Tomorrow Setup" else "Save Tomorrow Setup",
                                color = BgDeep,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCampusTimePicker) {
        RiderTimePickerDialog(
            initialHour = campusHour,
            initialMinute = campusMinute,
            onDismiss = { showCampusTimePicker = false },
            onConfirm = { h, m ->
                campusHour = h
                campusMinute = m
                showCampusTimePicker = false
            }
        )
    }

    if (showHomeTimePicker) {
        RiderTimePickerDialog(
            initialHour = homeHour,
            initialMinute = homeMinute,
            onDismiss = { showHomeTimePicker = false },
            onConfirm = { h, m ->
                homeHour = h
                homeMinute = m
                showHomeTimePicker = false
            }
        )
    }
}

@Composable
private fun TomorrowPlanSubmittedCard(
    campusPickup: String,
    campusTime: String,
    campusLocked: Boolean,
    returnLocation: String,
    returnTime: String,
    returnLocked: Boolean,
    submittedDate: String,
    onEditClick: () -> Unit
) {
    SectionCard(
        title = "Tomorrow Plan Saved",
        subtitle = "Your ride plan for $submittedDate is already stored."
    ) {
        MetaRow(Icons.Default.School, "To campus: $campusPickup → AUST Gate")
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(
                text = if (campusLocked) "Matched" else "Active",
                accent = if (campusLocked) AccentEmerald else AccentBlue
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        MetaRow(Icons.Default.Schedule, campusTime)
        Spacer(modifier = Modifier.height(10.dp))
        MetaRow(Icons.Default.Home, "Return: AUST Gate → $returnLocation")
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(
                text = if (returnLocked) "Matched" else "Active",
                accent = if (returnLocked) AccentEmerald else AccentBlue
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        MetaRow(Icons.Default.Schedule, returnTime)
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onEditClick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Lime),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime)
        ) {
            Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Edit Tomorrow Setup")
        }
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
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = message,
            color = TextMed,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SetupHeaderCard() {
    SectionCard(
        title = "Set Up Tomorrow",
        subtitle = "Save your campus and return trip once. Matching happens automatically."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "2 trips", accent = AccentBlue)
            MiniBadge(text = "Auto match", accent = AccentEmerald)
        }
    }
}

@Composable
private fun SetupSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            color = TextHigh,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TimeSelectionCard(
    label: String,
    selectedTimeText: String,
    onPickTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardElevated)
            .clickable { onPickTimeClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextMed,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = selectedTimeText,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

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