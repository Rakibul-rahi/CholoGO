@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.passenger

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
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

val BgDeep = Color(0xFF0A0D0F)
val BgSurface = Color(0xFF111418)

val CardBase = Color(0xFF161B20)
val CardElevated = Color(0xFF1C2228)
val CardHighlight = Color(0xFF202832)

val Lime = Color(0xFFC6F135)
val LimeDim = Color(0xFF9DC429)
val LimeDeep = Color(0xFF6F8F1A)
val LimeGlow = Color(0x1FC6F135)
val LimeGlowMd = Color(0x38C6F135)

val AccentBlue = Color(0xFF60A5FA)
val AccentAmber = Color(0xFFFBBF24)
val AccentEmerald = Color(0xFF34D399)
val AccentRed = Color(0xFFF87171)

val TextHigh = Color(0xFFF1F5F9)
val TextMed = Color(0xFF8B96A5)
val TextLow = Color(0xFF4E5A66)

val BorderSubtle = Color.White.copy(alpha = 0.07f)
val BorderFocus = Color(0x59C6F135)

val GradientLime = Brush.linearGradient(
    listOf(Lime, LimeDim)
)

val GradientHero = Brush.linearGradient(
    listOf(
        Color(0xFF1A2410),
        Color(0xFF0D1A0A),
        BgSurface
    )
)

val GradientCard = Brush.linearGradient(
    listOf(CardBase, BgSurface)
)

val GradientActive = Brush.linearGradient(
    listOf(
        Lime.copy(alpha = 0.06f),
        CardBase
    )
)

val GradientAd = Brush.linearGradient(
    listOf(
        Color(0xFF1A1030),
        Color(0xFF0D1520)
    )
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
fun PassengerTopBar(
    onLogoClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(BgSurface, BgDeep.copy(alpha = 0.7f))
                )
            )
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable { onLogoClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Lime),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row {
                Text(
                    text = "Cholo",
                    color = TextHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GO",
                    color = Lime,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CardElevated)
                .border(1.5.dp, BorderFocus, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "👤",
                fontSize = 17.sp
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(AccentEmerald)
                    .border(2.dp, BgDeep, CircleShape)
            )
        }
    }
}

@Composable
fun PremiumCardContainer(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) CardHighlight else CardBase
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(
            1.dp,
            if (highlight) BorderFocus else BorderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBase)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            Brush.linearGradient(
                                listOf(
                                    CardElevated,
                                    Lime.copy(alpha = 0.06f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            )
                        }
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) BorderFocus else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (index == 0) "⚡" else "📅",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = title,
                        color = if (selected) Lime else TextLow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (index == 0 && selected) {
                        Spacer(modifier = Modifier.width(7.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                        )
                    }
                }
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
        hour < 12 -> "Good Morning"
        hour < 18 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Lime.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientHero)
                .drawBehind {
                    drawCircle(
                        color = Lime.copy(alpha = 0.15f),
                        radius = size.width * 0.42f,
                        center = Offset(size.width * 0.94f, -20f)
                    )
                    drawCircle(
                        color = AccentEmerald.copy(alpha = 0.10f),
                        radius = size.width * 0.25f,
                        center = Offset(-20f, size.height + 10f)
                    )
                }
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = greeting.uppercase(),
                    color = LimeDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (passengerName.isBlank()) "Ready to ride? 👋" else "$passengerName 👋",
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

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
fun PassengerAdBanner(
    title: String = "AUST Semester Final — Get ready",
    subtitle: String = "Exam schedule now available on student portal"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GradientAd)
            .border(1.dp, AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentBlue.copy(alpha = 0.12f))
                .border(1.dp, AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎓", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextHigh,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = TextMed,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentBlue)
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle)
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle)
            )
        }
    }
}

@Composable
fun CompactGreetingCard(passengerName: String) {
    PassengerSectionCard(
        title = if (passengerName.isBlank()) "Welcome back" else "Welcome, $passengerName",
        subtitle = "Find a ride for today based on route and time.",
        icon = "👋"
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
    icon: String = "•",
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBase),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.dp, Color.Transparent)
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LimeGlow)
                        .border(1.dp, Lime.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextHigh,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = subtitle,
                        color = TextMed,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            Column(modifier = Modifier.padding(18.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextLow,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp
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
            .background(accent.copy(alpha = 0.12f))
            .border(
                1.dp,
                accent.copy(alpha = 0.22f),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
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
                .clip(RoundedCornerShape(12.dp))
                .background(BgSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .clickable { onExpandChange(true) }
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LimeGlow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Lime,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    color = TextLow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = selectedLocation,
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextLow,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier.background(CardElevated)
        ) {
            locations.forEach { location ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = location,
                            color = TextHigh
                        )
                    },
                    onClick = { onLocationSelected(location) }
                )
            }
        }
    }
}

@Composable
fun RouteConnectorLabel(
    label: String = "via road"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
            .height(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(18.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(LimeGlowMd, Color.Transparent)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            color = TextLow,
            fontSize = 10.sp
        )
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
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onPickTimeClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🕐",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextLow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = helper,
                color = helperColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = selectedTimeText,
            color = Lime,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
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
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Lime,
            contentColor = Color.Black,
            disabledContainerColor = LimeDeep,
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

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
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
            modifier = Modifier.padding(16.dp),
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBase),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, if (highlight) BorderFocus else BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(LimeGlow, AccentEmerald.copy(alpha = 0.10f))
                                )
                            )
                            .border(1.dp, Lime.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ride.driverName.take(1).uppercase(),
                            color = Lime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = ride.driverName,
                            color = TextHigh,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(12.dp)
                            )

                            Spacer(modifier = Modifier.width(3.dp))

                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", ride.rating)} · ${ride.routeLabel}",
                                color = TextMed,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪑", fontSize = 12.sp)

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "${ride.seatsLeft} seats",
                        color = AccentEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            RideMetaRow(
                icon = Icons.Default.LocationOn,
                text = "${ride.origin} → ${ride.destination} · ${ride.departureTime}"
            )

            if (ride.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentBlue,
                            containerColor = AccentBlue.copy(alpha = 0.08f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Call Rider",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Lime.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Lime,
                            containerColor = LimeGlow
                        )
                    ) {
                        Text(
                            text = "Auto-match ✓",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            color = TextMed,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PastTimeWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentRed.copy(alpha = 0.10f))
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

        Spacer(modifier = Modifier.width(8.dp))

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
        containerColor = CardBase,
        titleContentColor = TextHigh,
        textContentColor = TextMed,
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
                Text("OK", color = Lime)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMed)
            }
        }
    )
}

@Composable
fun PassengerRideNowStatusCard(
    passengerRequest: RideNowRequest,
    onCancel: () -> Unit,
    onCallRider: () -> Unit,
    showCallButton: Boolean
) {
    val statusAccent = when (passengerRequest.status) {
        RideNowStatus.SEARCHING -> Lime
        RideNowStatus.ACCEPTED -> AccentEmerald
        RideNowStatus.START_PENDING_CONFIRMATION -> AccentAmber
        RideNowStatus.ONGOING -> AccentBlue
        RideNowStatus.END_PENDING_CONFIRMATION -> AccentAmber
        RideNowStatus.COMPLETED -> AccentEmerald
        RideNowStatus.CANCELLED -> AccentRed
        RideNowStatus.EXPIRED -> AccentRed
        RideNowStatus.ISSUE_REPORTED -> AccentRed
        else -> TextMed
    }

    val statusText = when (passengerRequest.status) {
        RideNowStatus.SEARCHING -> "Searching for riders…"
        RideNowStatus.ACCEPTED -> "Rider accepted"
        RideNowStatus.START_PENDING_CONFIRMATION -> "Waiting start confirmation"
        RideNowStatus.ONGOING -> "Ride ongoing"
        RideNowStatus.END_PENDING_CONFIRMATION -> "Waiting completion confirmation"
        RideNowStatus.COMPLETED -> "Ride completed"
        RideNowStatus.CANCELLED -> "Request cancelled"
        RideNowStatus.EXPIRED -> "Request expired"
        RideNowStatus.ISSUE_REPORTED -> "Issue reported"
        else -> passengerRequest.status.replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientActive)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusAccent)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = statusText,
                        color = statusAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = passengerRequest.tripTime,
                    color = TextMed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
            ) {
                ActiveRouteLine(
                    pickup = passengerRequest.pickup,
                    destination = passengerRequest.destination
                )
            }

            when (passengerRequest.status) {
                RideNowStatus.SEARCHING -> {
                    HorizontalDivider(color = BorderSubtle)

                    Column(modifier = Modifier.padding(18.dp)) {
                        PremiumLoadingCard("Nearby riders can accept your request in real time.")

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentRed,
                                containerColor = AccentRed.copy(alpha = 0.10f)
                            )
                        ) {
                            Text(
                                text = "✕ Cancel Request",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                RideNowStatus.ACCEPTED -> {
                    RiderMatchedSection(
                        passengerRequest = passengerRequest,
                        onCallRider = onCallRider,
                        showCallButton = showCallButton,
                        message = "Contact your rider and be ready for pickup."
                    )
                }

                RideNowStatus.START_PENDING_CONFIRMATION -> {
                    RiderMatchedSection(
                        passengerRequest = passengerRequest,
                        onCallRider = onCallRider,
                        showCallButton = showCallButton,
                        message = "Your rider is ready to start. Confirm from the Ride Now screen."
                    )
                }

                RideNowStatus.ONGOING -> {
                    RiderMatchedSection(
                        passengerRequest = passengerRequest,
                        onCallRider = onCallRider,
                        showCallButton = showCallButton,
                        message = "Your Ride Now trip is currently ongoing."
                    )
                }

                RideNowStatus.END_PENDING_CONFIRMATION -> {
                    RiderMatchedSection(
                        passengerRequest = passengerRequest,
                        onCallRider = onCallRider,
                        showCallButton = showCallButton,
                        message = "Your rider marked the ride as finished. Confirm completion from the Ride Now screen."
                    )
                }

                RideNowStatus.COMPLETED -> {
                    RideNowInfoBanner("Your Ride Now trip has been completed.")
                }

                RideNowStatus.CANCELLED -> {
                    RideNowInfoBanner("Your request has been cancelled.")
                }

                RideNowStatus.EXPIRED -> {
                    RideNowInfoBanner("No rider accepted in time. Please try again.")
                }

                RideNowStatus.ISSUE_REPORTED -> {
                    RideNowInfoBanner("An issue has been reported for this ride. The ride is locked for review.")
                }

                else -> Unit
            }
        }
    }
}

@Composable
fun ActiveRouteLine(
    pickup: String,
    destination: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Lime)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = pickup,
                color = TextHigh,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 3.dp, top = 4.dp, bottom = 4.dp)
                .width(1.5.dp)
                .height(20.dp)
                .background(BorderSubtle)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AccentEmerald)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = destination,
                color = TextHigh,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RiderMatchedSection(
    passengerRequest: RideNowRequest,
    onCallRider: () -> Unit,
    showCallButton: Boolean,
    message: String
) {
    HorizontalDivider(color = BorderSubtle)

    Column(modifier = Modifier.padding(18.dp)) {
        val riderName = passengerRequest.matchedRiderName.ifBlank { "Rider" }
        val riderPhone = passengerRequest.matchedRiderPhone

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LimeGlow)
                    .border(1.dp, Lime.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = riderName.take(1).uppercase(),
                    color = Lime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = riderName,
                    color = TextHigh,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (riderPhone.isNotBlank()) {
                    Text(
                        text = riderPhone,
                        color = TextMed,
                        fontSize = 12.sp
                    )
                }
            }

            MiniBadge(text = "Matched", accent = AccentEmerald)
        }

        Spacer(modifier = Modifier.height(14.dp))

        RideNowInfoBanner(message = message)

        if (showCallButton && riderPhone.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCallRider,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentBlue,
                    containerColor = AccentBlue.copy(alpha = 0.08f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Call Rider",
                    fontWeight = FontWeight.SemiBold
                )
            }
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
            lineHeight = 18.sp
        )
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