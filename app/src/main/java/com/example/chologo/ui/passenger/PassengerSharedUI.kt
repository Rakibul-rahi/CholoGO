@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.passenger
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.ui.components.LevelCard
import com.example.chologo.utils.LevelInfo
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val BgDeep = Color(0xFF070B10)
val BgSurface = Color(0xFF0C1117)

val CardBase = Color(0xFF121821)
val CardElevated = Color(0xFF18202A)
val CardHighlight = Color(0xFF1E2835)

val Lime = Color(0xFFA7E04B)
val LimeDeep = Color(0xFF84C62A)
val LimeDim = Color(0xFF2D4218)

val AccentBlue = Color(0xFF5AA8FF)
val AccentAmber = Color(0xFFFFC857)
val AccentEmerald = Color(0xFF36E08F)
val AccentRed = Color(0xFFFF6673)

val TextHigh = Color(0xFFF5F7FA)
val TextMed = Color(0xFFA6B0BF)
val TextLow = Color(0xFF6A7482)

val BorderSubtle = Color(0xFF1A2430)
val BorderFocus = Color(0xFF30445C)

val GradientLime = Brush.horizontalGradient(
    listOf(Color(0xFFA7E04B), Color(0xFF7FC82A))
)

val GradientCard = Brush.linearGradient(
    listOf(Color(0xFF1A2230), Color(0xFF10161F))
)

val GradientSuccess = Brush.linearGradient(
    listOf(Color(0xFF11261B), Color(0xFF0A1711))
)

val availableLocations = listOf(
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

data class RideCardUi(
    val rideId: String,
    val driverName: String,
    val rating: Float = 5.0f,
    val routeLabel: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val seatsLeft: Int,
    val phone: String = ""
)

@Composable
fun PremiumCardContainer(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) CardHighlight else CardBase
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(
            1.dp,
            if (highlight) BorderFocus else BorderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun PremiumTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = BgDeep,
        contentColor = Lime,
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTab])
                    .padding(horizontal = 28.dp)
                    .height(3.dp)
                    .background(
                        brush = GradientLime,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
            )
        },
        divider = {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index
            Tab(
                selected = selected,
                onClick = { onTabSelected(index) },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .alpha(if (selected) 1f else 0.5f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (selected) Lime else TextMed
                )
            }
        }
    }
}

@Composable
fun PassengerHeroCard(
    passengerName: String,
    levelInfo: LevelInfo,
    isLevelLoading: Boolean
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = BorderStroke(1.dp, BorderFocus)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientCard)
                .drawBehind {
                    drawCircle(
                        color = Lime.copy(alpha = 0.08f),
                        radius = size.width * 0.42f,
                        center = Offset(size.width * 0.9f, size.height * 0.15f)
                    )
                }
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = greeting,
                    color = TextMed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (passengerName.isBlank()) "Ready to ride?" else "Hi, $passengerName",
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Search active campus rides now or save tomorrow's ride plan.",
                    color = TextMed,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBadge(text = "Fast match", accent = Lime)
                    MiniBadge(text = "Campus safe", accent = AccentBlue)
                    MiniBadge(text = "Live status", accent = AccentEmerald)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLevelLoading) {
                    PremiumLoadingCard("Loading your level...")
                } else {
                    LevelCard(
                        level = levelInfo.level,
                        levelTitle = levelInfo.levelTitle,
                        currentXp = levelInfo.currentXp,
                        xpNeededForNextLevel = levelInfo.xpNeededForNextLevel,
                        progress = levelInfo.progressFraction
                    )
                }
            }
        }
    }
}

@Composable
fun CompactGreetingCard(passengerName: String) {
    PassengerSectionCard(
        title = if (passengerName.isBlank()) "Welcome back" else "Welcome, $passengerName",
        subtitle = "Find a ride for today based on route and time."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Live rides", accent = AccentEmerald)
            MiniBadge(text = "Quick search", accent = AccentBlue)
        }
    }
}

@Composable
fun PassengerSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    PremiumCardContainer {
        Text(
            text = title,
            color = TextHigh,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = TextMed,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextHigh,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
}

@Composable
fun MiniBadge(
    text: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        accent.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                1.dp,
                accent.copy(alpha = 0.22f),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun LocationSelectionCard(
    label: String,
    selectedLocation: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    locations: List<String>,
    leadingIcon: ImageVector,
    onLocationSelected: (String) -> Unit
) {
    Box {
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
                .clickable { onExpandChange(true) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Lime,
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
                    text = selectedLocation,
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextMed
            )
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

@Composable
fun PassengerTimeSelectionCard(
    label: String,
    selectedTimeText: String,
    helper: String,
    helperColor: Color = TextLow,
    onPickTimeClick: () -> Unit
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
            .clickable { onPickTimeClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = helper,
                color = helperColor,
                fontSize = 11.sp
            )
        }

        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = AccentAmber
        )
    }
}

@Composable
fun LimeActionButton(
    text: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.98f else 1f,
        label = "lime_button_scale"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LimeDeep,
            contentColor = Color.Black,
            disabledContainerColor = LimeDim,
            disabledContentColor = Color.Black
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.Black
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PremiumLoadingCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Lime
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = message,
                color = TextMed,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    message: String
) {
    PremiumCardContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = TextMed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PassengerRideCard(
    ride: RideCardUi,
    highlight: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, if (highlight) BorderFocus else BorderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (highlight) GradientSuccess else GradientCard)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ride.driverName,
                            color = TextHigh,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniBadge(text = ride.routeLabel, accent = AccentBlue)
                            MiniBadge(text = "${ride.seatsLeft} seats", accent = AccentEmerald)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", ride.rating),
                            color = TextHigh,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                RideMetaRow(Icons.Default.LocationOn, "${ride.origin} → ${ride.destination}")
                RideMetaRow(Icons.Default.Schedule, ride.departureTime)

                if (ride.phone.isNotBlank()) {
                    RideMetaRow(Icons.Default.Phone, ride.phone)
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Lime.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "View Match",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun RideMetaRow(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            color = TextMed,
            fontSize = 13.sp
        )
    }
}

@Composable
fun PastTimeWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentRed.copy(alpha = 0.12f))
            .border(
                1.dp,
                AccentRed.copy(alpha = 0.22f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = AccentRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Selected time is in the past. Please pick a later departure time.",
            color = AccentRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PassengerTimePickerDialogWithMinTime(
    initialHour: Int,
    initialMinute: Int,
    minHour: Int,
    minMinute: Int,
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
                text = "Select departure time",
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Only current or future times are allowed.",
                    color = TextMed,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                TimePicker(state = timePickerState)
            }
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
        }
    )
}

@Composable
fun PassengerRideNowStatusCard(
    passengerRequest: RideNowRequest,
    onCancel: () -> Unit,
    onCallRider: () -> Unit,          // NEW
    showCallButton: Boolean           // NEW
) {
    val statusAccent = when (passengerRequest.status) {
        RideNowStatus.SEARCHING -> AccentAmber
        RideNowStatus.ACCEPTED -> AccentEmerald
        RideNowStatus.ONGOING -> AccentBlue
        RideNowStatus.COMPLETED -> AccentEmerald
        RideNowStatus.CANCELLED -> AccentRed
        RideNowStatus.EXPIRED -> AccentRed
        else -> TextMed
    }

    PassengerSectionCard(
        title = "Ride Now Status",
        subtitle = "Your live request is being tracked in real time."
    ) {
        MiniBadge(
            text = passengerRequest.status.replaceFirstChar { it.uppercase() },
            accent = statusAccent
        )

        Spacer(modifier = Modifier.height(14.dp))

        RideMetaRow(
            icon = Icons.Default.LocationOn,
            text = "${passengerRequest.pickup} → ${passengerRequest.destination}"
        )

        Spacer(modifier = Modifier.height(8.dp))

        RideMetaRow(
            icon = Icons.Default.Schedule,
            text = passengerRequest.tripTime
        )

        when (passengerRequest.status) {

            // 🔍 SEARCHING
            RideNowStatus.SEARCHING -> {
                Spacer(modifier = Modifier.height(14.dp))
                PremiumLoadingCard("Searching for a live rider...")
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AccentRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                ) {
                    Text("Cancel Request")
                }
            }

            // ✅ ACCEPTED
            RideNowStatus.ACCEPTED -> {
                Spacer(modifier = Modifier.height(14.dp))

                PassengerSectionCard(
                    title = "Rider Found",
                    subtitle = "A rider has accepted your request."
                ) {
                    MiniBadge(text = "Matched", accent = AccentEmerald)

                    Spacer(modifier = Modifier.height(12.dp))

                    val riderName = passengerRequest.matchedRiderName.ifBlank { "Rider" }
                    val riderPhone = passengerRequest.matchedRiderPhone

                    RideMetaRow(Icons.Default.Star, riderName)

                    if (riderPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        RideMetaRow(Icons.Default.Phone, riderPhone)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🔥 CALL BUTTON
                    if (showCallButton) {
                        OutlinedButton(
                            onClick = onCallRider,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Lime),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call Rider")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    RideNowInfoBanner(
                        message = "Contact your rider and be ready for pickup."
                    )
                }
            }

            // 🚗 ONGOING
            RideNowStatus.ONGOING -> {
                Spacer(modifier = Modifier.height(14.dp))

                if (showCallButton) {
                    OutlinedButton(
                        onClick = onCallRider,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Lime),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Rider")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                RideNowInfoBanner(
                    message = "Your Ride Now trip is currently ongoing."
                )
            }

            RideNowStatus.COMPLETED -> {
                Spacer(modifier = Modifier.height(14.dp))
                RideNowInfoBanner("Your Ride Now trip has been completed.")
            }

            RideNowStatus.CANCELLED -> {
                Spacer(modifier = Modifier.height(14.dp))
                RideNowInfoBanner("Your request has been cancelled.")
            }

            RideNowStatus.EXPIRED -> {
                Spacer(modifier = Modifier.height(14.dp))
                RideNowInfoBanner("No rider accepted in time. Please try again.")
            }

            else -> Unit
        }
    }
}

@Composable
fun RideNowInfoBanner(
    message: String
) {
    PassengerSectionCard(
        title = "Info",
        subtitle = message
    ) {
        MiniBadge(text = "Ride Now", accent = AccentBlue)
    }
}

fun toMinutes(hour: Int, minute: Int): Int = hour * 60 + minute

fun formatTo12Hour(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}

fun getTodayDateKey(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(Calendar.getInstance().time)
}

fun getTomorrowDateKey(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}

fun formatTimestampToDate(timestamp: Timestamp): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timestamp.toDate())
}

fun buildRouteKey(
    tripDirection: String,
    pickup: String,
    destination: String
): String {
    return "${tripDirection.trim().lowercase()}|${pickup.trim().lowercase()}|${destination.trim().lowercase()}"
}