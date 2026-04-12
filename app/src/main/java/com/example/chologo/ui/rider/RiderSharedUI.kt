@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.rider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chologo.R
import com.example.chologo.navigation.Screen
import com.example.chologo.ui.components.LevelCard
import com.example.chologo.utils.LevelInfo
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Shared design tokens
// ─────────────────────────────────────────────────────────────────────────────

val BgDeep = Color(0xFF080C10)
val BgSurface = Color(0xFF0E1318)
val CardBase = Color(0xFF141A21)
val CardElevated = Color(0xFF1A2130)

val Lime = Color(0xFF9FD63F)
val LimeDeep = Color(0xFF6FAF1A)
val LimeDim = Color(0xFF2A3E18)

val AccentBlue = Color(0xFF4D9FFF)
val AccentAmber = Color(0xFFFFBD40)
val AccentEmerald = Color(0xFF30D878)
val AccentRed = Color(0xFFFF5461)

val TextHigh = Color(0xFFF0F4F8)
val TextMed = Color(0xFF8B9AB0)
val TextLow = Color(0xFF4A5568)

val BorderSubtle = Color(0xFF1E2D3D)
val BorderFocus = Color(0xFF2D4060)

val GradientLime = Brush.linearGradient(listOf(Lime, Color(0xFF6FBA2A)))
val GradientCard = Brush.linearGradient(
    listOf(Color(0xFF1A2233), Color(0xFF101620))
)
val GradientSuccess = Brush.linearGradient(
    listOf(Color(0xFF0E2418), Color(0xFF081810))
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

// ─────────────────────────────────────────────────────────────────────────────
// Shared top bar / hero
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RiderTopBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BgSurface, BgDeep)))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.chologologo),
            contentDescription = "CholoGO",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(100.dp)
                .wrapContentWidth()
        )

        Box(
            modifier = Modifier
                .size(46.dp)
                .drawBehind {
                    drawCircle(
                        brush = GradientLime,
                        radius = size.minDimension / 2f
                    )
                }
                .padding(2.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(CardElevated)
                .clickable {
                    navController.navigate(Screen.Profile.createRoute("rider")) {
                        launchSingleTop = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Lime,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun RiderHeroCard(
    riderName: String,
    levelInfo: LevelInfo,
    isLevelLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BorderFocus)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientCard)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (riderName.isBlank()) "Ready to ride?" else "Hey, $riderName",
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Manage your tomorrow rides and accept passengers to earn XP.",
                    color = TextMed,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBadge(text = "Driver mode", accent = AccentBlue)
                    MiniBadge(text = "Earn XP", accent = AccentEmerald)
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

// ─────────────────────────────────────────────────────────────────────────────
// Shared tabs
// ─────────────────────────────────────────────────────────────────────────────

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
                    .padding(horizontal = 32.dp)
                    .height(2.dp)
                    .background(
                        brush = GradientLime,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 2.dp,
                            topEnd = 2.dp
                        )
                    )
            )
        },
        divider = {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index
            val alpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0.45f,
                label = "tab_alpha"
            )

            Tab(
                selected = selected,
                onClick = { onTabSelected(index) },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .alpha(alpha),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (selected) Lime else TextMed
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared small UI blocks
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DirectionBadge(direction: String) {
    val (label, color) = when (direction.lowercase()) {
        "to_campus" -> "Campus" to AccentBlue
        "to_home" -> "Home" to AccentEmerald
        else -> "Ride" to Lime
    }

    MiniBadge(text = label, accent = color)
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBase)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = TextMed,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextHigh,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
}

@Composable
fun MiniBadge(
    text: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
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
fun InfoBannerCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBase),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
fun PremiumLoadingCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBase),
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
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = TextMed,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun MetaRow(
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = TextMed,
            fontSize = 13.sp
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
    onLocationSelected: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            color = TextHigh,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(true) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardElevated),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Lime,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = selectedLocation,
                            color = TextHigh,
                            fontSize = 14.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = TextMed
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardElevated)
            ) {
                locations.forEach { location ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = location,
                                color = TextHigh,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onLocationSelected(location)
                            onExpandChange(false)
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

fun getTomorrowDateKey(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}

fun formatTimestampToDate(timestamp: Timestamp): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timestamp.toDate())
}

fun formatTo12Hour(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}

fun toMinutes(hour: Int, minute: Int): Int {
    return hour * 60 + minute
}

fun buildRouteKey(
    tripDirection: String,
    pickup: String,
    destination: String
): String {
    return "${tripDirection.trim().lowercase()}|${pickup.trim().lowercase()}|${destination.trim().lowercase()}"
}

fun parse12HourTime(timeText: String): Pair<Int, Int>? {
    return try {
        val inputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = inputFormat.parse(timeText) ?: return null
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    } catch (_: Exception) {
        null
    }
}