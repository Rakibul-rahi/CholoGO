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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.Ride
import com.example.chologo.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RiderTomorrowSetupTab(
    rideDate: String,
    riderName: String,
    userRepository: UserRepository,
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
        val uid = auth.currentUser?.uid ?: run {
            isLoadingPlan = false
            isEditing = true
            return@LaunchedEffect
        }

        db.collection("rides")
            .whereEqualTo("riderId", uid)
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

                    when (ride.tripDirection.lowercase()) {
                        "to_campus" -> {
                            campusPickupLocation = ride.pickup.ifBlank { "Mirpur 12" }
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
                            homeReturnLocation = ride.destination.ifBlank { "Mirpur 12" }
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

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        when {
            isLoadingPlan -> PremiumLoadingCard("Loading tomorrow plan...")

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
                SetupHeaderCard()

                SetupSectionHeader(
                    icon = Icons.Default.School,
                    tint = AccentBlue,
                    label = "Going to Campus"
                )

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

                Spacer(modifier = Modifier.height(4.dp))

                SetupSectionHeader(
                    icon = Icons.Default.Home,
                    tint = AccentEmerald,
                    label = "Returning Home"
                )

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

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = if (isSaving) {
                                androidx.compose.ui.graphics.Brush.linearGradient(listOf(LimeDim, LimeDim))
                            } else {
                                androidx.compose.ui.graphics.Brush.linearGradient(listOf(Lime, LimeDeep))
                            }
                        )
                        .clickable(enabled = !isSaving) {
                            val uid = auth.currentUser?.uid
                            if (uid == null) {
                                Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }

                            isSaving = true
                            val now = Timestamp.now()

                            val campusRef = db.collection("rides").document()
                            val homeRef = db.collection("rides").document()

                            val campusRide = Ride(
                                rideId = campusRef.id,
                                riderId = uid,
                                riderName = riderName,
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
                                rideId = homeRef.id,
                                riderId = uid,
                                riderName = riderName,
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
                                .whereEqualTo("riderId", uid)
                                .whereEqualTo("rideDate", rideDate)
                                .get()
                                .addOnSuccessListener { result ->
                                    val batch = db.batch()

                                    result.documents.forEach { batch.delete(it.reference) }
                                    batch.set(campusRef, campusRide)
                                    batch.set(homeRef, returnRide)

                                    batch.commit()
                                        .addOnSuccessListener {
                                            userRepository.addXpToCurrentUser(5L) { xpResult ->
                                                xpResult.onSuccess {
                                                    onSaveSuccess()
                                                }
                                            }

                                            isSaving = false
                                            isPlanSubmitted = true
                                            isEditing = false
                                            submittedDateText = rideDate

                                            Toast.makeText(
                                                context,
                                                "Tomorrow setup saved (+5 XP)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            isSaving = false
                                            Toast.makeText(
                                                context,
                                                "Error: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isSaving = false
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
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
    returnLocation: String,
    returnTime: String,
    submittedDate: String,
    onEditClick: () -> Unit
) {
    SectionCard(
        title = "Tomorrow Plan Saved",
        subtitle = "Your ride plan for $submittedDate is already stored."
    ) {
        MetaRow(Icons.Default.School, "To campus: $campusPickup → AUST Gate")
        Spacer(modifier = Modifier.height(6.dp))
        MetaRow(Icons.Default.Schedule, campusTime)
        Spacer(modifier = Modifier.height(10.dp))
        MetaRow(Icons.Default.Home, "Return: AUST Gate → $returnLocation")
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
            Spacer(modifier = Modifier.height(0.dp))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
            Text("Edit Tomorrow Setup")
        }
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
    icon: ImageVector,
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
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
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

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))

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