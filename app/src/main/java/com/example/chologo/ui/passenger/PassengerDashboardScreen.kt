package com.example.chologo.ui.passenger

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chologo.R
import com.example.chologo.navigation.Screen
import java.util.Locale

private val BgDark = Color(0xFF0D1117)
private val CardDark = Color(0xFF161B22)
private val GreenPrimary = Color(0xFF8DC63F)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val AmberWarn = Color(0xFFFFA41B)
private val HeaderButtonBg = Color(0xFF111827)

enum class HomeState {
    AT_HOME,
    AT_AUST,
    ROUTE_SELECTED
}

data class Rider(
    val name: String,
    val rating: Float,
    val eta: String
)

data class RideCard(
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
    var selectedTab by remember { mutableStateOf(0) }
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
            item {
                PassengerTopBar(navController = navController)
            }

            item {
                AdBannerBlock()
            }

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

            when (selectedTab) {
                0 -> {
                    item {
                        RideNowTab()
                    }
                }
                1 -> {
                    item {
                        TomorrowTab()
                    }
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
    var state by remember { mutableStateOf(HomeState.AT_HOME) }
    var selectedRoute by remember { mutableStateOf("") }

    val routes = listOf("Mirpur", "Uttara", "Dhanmondi", "Farmgate")
    val riders = listOf(
        Rider("Rakib", 4.9f, "2 min"),
        Rider("Tanvir", 4.8f, "5 min"),
        Rider("Saim", 4.7f, "7 min")
    )

    Column {
        BackBar(
            showBack = state != HomeState.AT_HOME,
            onBackClick = {
                when (state) {
                    HomeState.ROUTE_SELECTED -> state = HomeState.AT_AUST
                    HomeState.AT_AUST -> state = HomeState.AT_HOME
                    else -> {}
                }
            }
        )

        if (state != HomeState.AT_HOME) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (state) {
            HomeState.AT_HOME -> {
                Text(
                    text = "📍 Your Location: Mirpur",
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        selectedRoute = "AUST"
                        state = HomeState.AT_AUST
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(
                        text = "Ride to AUST",
                        color = BgDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HomeState.AT_AUST -> {
                Text(
                    text = "📍 AUST Campus",
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                routes.forEach { route ->
                    RouteButton(route) {
                        selectedRoute = route
                        state = HomeState.ROUTE_SELECTED
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            HomeState.ROUTE_SELECTED -> {
                Text(
                    text = "🏍️ Riders → ${if (selectedRoute.isEmpty()) "AUST" else selectedRoute}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                riders.forEach { rider ->
                    RiderCard(rider) {}
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TomorrowTab() {
    var hasClassesTomorrow by remember { mutableStateOf<Boolean?>(null) }
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(30) }
    var showTimePicker by remember { mutableStateOf(false) }
    var searchPressed by remember { mutableStateOf(false) }

    val selectedTimeText = formatTo12Hour(selectedHour, selectedMinute)

    val matchedRides = listOf(
        RideCard(
            driverName = "Rafiq M.",
            rating = 4.9f,
            routeLabel = "AUST Student",
            pricePerSeat = 35,
            origin = "Mirpur 10",
            destination = "AUST Gate",
            departureTime = "8:00 AM",
            seatsLeft = 1
        ),
        RideCard(
            driverName = "Tanvir H.",
            rating = 4.8f,
            routeLabel = "AUST Student",
            pricePerSeat = 40,
            origin = "Mirpur 10",
            destination = "AUST Gate",
            departureTime = "8:15 AM",
            seatsLeft = 2
        )
    )

    Column {
        TomorrowClassPromptCard(
            hasClassesTomorrow = hasClassesTomorrow,
            onYesClick = {
                hasClassesTomorrow = true
                searchPressed = false
            },
            onNoClick = {
                hasClassesTomorrow = false
                searchPressed = false
            }
        )

        if (hasClassesTomorrow == true) {
            Spacer(modifier = Modifier.height(12.dp))
            TimeSelectionCard(
                selectedTimeText = selectedTimeText,
                onPickTimeClick = { showTimePicker = true },
                onSearchClick = { searchPressed = true }
            )
        }

        if (hasClassesTomorrow == false) {
            Spacer(modifier = Modifier.height(12.dp))
            InfoMessageCard(
                title = "No class tomorrow",
                message = "You can still browse tomorrow's rides manually if you need to go somewhere."
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Tomorrow's Rides", color = TextSecondary)

            Spacer(modifier = Modifier.height(12.dp))

            matchedRides.forEach { ride ->
                RideListCard(ride) {}
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (hasClassesTomorrow == true && searchPressed) {
            Spacer(modifier = Modifier.height(12.dp))
            InfoMessageCard(
                title = "Matching riders found",
                message = "Showing riders from your location for tomorrow based on your first class time: $selectedTimeText"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Available Riders for Tomorrow", color = TextSecondary)

            Spacer(modifier = Modifier.height(12.dp))

            matchedRides.forEach { ride ->
                RideListCard(ride) {}
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showTimePicker) {
        CampusTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            }
        )
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2200)),
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
fun TimeSelectionCard(
    selectedTimeText: String,
    onPickTimeClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Enter your first class time",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Use the clock to choose time and AM/PM",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onPickTimeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Selected Time: $selectedTimeText")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Find Riders", color = BgDark)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Select first class time",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
            }
        }
    )
}

fun formatTo12Hour(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
}

@Composable
fun InfoMessageCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF102A1A)),
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
fun BackBar(
    showBack: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    if (showBack) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = HeaderButtonBg
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Back",
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AdBannerBlock() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ad Space",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sponsored content will appear here",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun RouteButton(route: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Text(
            text = route,
            modifier = Modifier.padding(16.dp),
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RiderCard(rider: Rider, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = rider.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberWarn,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${rider.rating}", color = TextSecondary)
                }
            }

            Text(
                text = rider.eta,
                color = GreenPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RideListCard(ride: RideCard, onClick: () -> Unit) {
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
            Text(
                text = "${ride.origin} → ${ride.destination}",
                color = TextSecondary
            )
            Text(
                text = "${ride.departureTime} · ${ride.seatsLeft} seat left",
                color = TextSecondary
            )
        }
    }
}