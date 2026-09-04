@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.rider

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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.example.chologo.ui.components.LevelCard
import com.example.chologo.ui.theme.LocalIsDarkTheme
import com.example.chologo.utils.Greeting
import com.example.chologo.utils.LevelInfo
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Shared design tokens - matched with PassengerSharedUI
// ─────────────────────────────────────────────────────────────────────────────

val BgDeep: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0A0D0F) else Color(0xFFF7F9FA)
val BgSurface: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF111418) else Color(0xFFEFF2F5)

val CardBase: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF161B20) else Color(0xFFFFFFFF)
val CardElevated: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1C2228) else Color(0xFFF1F4F7)
val CardHighlight: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF202832) else Color(0xFFE8EEF3)

val Lime: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFC6F135) else Color(0xFF5E7A17)
val LimeDim: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9DC429) else Color(0xFF4C6412)
val LimeDeep: Color @Composable get() = Color(0xFF6F8F1A)
val LimeGlow: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0x1FC6F135) else Color(0x1F5E7A17)
val LimeGlowMd: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0x38C6F135) else Color(0x385E7A17)

val AccentBlue: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF60A5FA) else Color(0xFF1D6FE0)
val AccentAmber: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFBBF24) else Color(0xFFA6720A)
val AccentEmerald: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF34D399) else Color(0xFF0E9A6B)
val AccentRed: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF87171) else Color(0xFFDC2626)

val TextHigh: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF1F5F9) else Color(0xFF10151B)
val TextMed: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8B96A5) else Color(0xFF4B5563)
val TextLow: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF4E5A66) else Color(0xFF98A2AE)

val BorderSubtle: Color @Composable get() = if (LocalIsDarkTheme.current) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.08f)
val BorderFocus: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0x59C6F135) else Color(0x595E7A17)

val GradientLime: Brush @Composable get() = Brush.linearGradient(
    listOf(Lime, LimeDim)
)

val GradientHero: Brush
    @Composable get() = if (LocalIsDarkTheme.current) {
        Brush.linearGradient(
            listOf(
                Color(0xFF1A2410),
                Color(0xFF0D1A0A),
                BgSurface
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFEDF6DC),
                Color(0xFFF6FAF0),
                BgSurface
            )
        )
    }

val GradientCard: Brush @Composable get() = Brush.linearGradient(listOf(CardBase, BgSurface))

val GradientActive: Brush @Composable get() = Brush.linearGradient(
    listOf(
        Lime.copy(alpha = 0.06f),
        CardBase
    )
)

val GradientSuccess: Brush @Composable get() = Brush.linearGradient(
    listOf(
        AccentEmerald.copy(alpha = 0.10f),
        CardBase
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

// ─────────────────────────────────────────────────────────────────────────────
// Top bar / hero
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RiderTopBar(navController: NavController) {
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
            modifier = Modifier.clickable {
                navController.navigate(Screen.RiderHome.route) {
                    launchSingleTop = true
                    popUpTo(Screen.RiderHome.route) {
                        inclusive = false
                    }
                }
            },
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
                modifier = Modifier.size(19.dp)
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
fun RiderHeroCard(
    riderName: String,
    levelInfo: LevelInfo,
    isLevelLoading: Boolean
) {
    val greeting = Greeting.forHour()
    // drawBehind runs at draw time, outside composition, so the themed
    // colors it reads must be plain values captured here rather than
    // @Composable property reads inside the lambda.
    val lime = Lime
    val accentEmerald = AccentEmerald

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, lime.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientHero)
                .drawBehind {
                    drawCircle(
                        color = lime.copy(alpha = 0.15f),
                        radius = size.width * 0.42f,
                        center = Offset(size.width * 0.94f, -20f)
                    )
                    drawCircle(
                        color = accentEmerald.copy(alpha = 0.10f),
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
                    text = if (riderName.isBlank()) {
                        "Ready to ride? 🏍️"
                    } else {
                        "$riderName 🏍️"
                    },
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Accept passengers, manage Ride Now, and earn XP from completed rides.",
                    color = TextMed,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBadge(text = "Rider mode", accent = AccentBlue)
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
// Shared tab row - same visual style as passenger
// ─────────────────────────────────────────────────────────────────────────────

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
                        text = when (index) {
                            0 -> "⚡"
                            1 -> "📅"
                            else -> "•"
                        },
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = title,
                        color = if (selected) Lime else TextLow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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

/**
 * Keep this only if any old rider screen still uses Material TabRow behavior.
 * You can ignore/delete this later if unused.
 */
@Composable
fun LegacyRiderTabRow(
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
                        shape = RoundedCornerShape(
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
// Shared card containers
// ─────────────────────────────────────────────────────────────────────────────

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
fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    RiderSectionCard(
        title = title,
        subtitle = subtitle,
        icon = "🏍️",
        content = content
    )
}

@Composable
fun RiderSectionCard(
    title: String,
    subtitle: String,
    icon: String = "🏍️",
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

// ─────────────────────────────────────────────────────────────────────────────
// Small UI blocks
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
fun InfoBannerCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentBlue.copy(alpha = 0.06f))
            .border(1.dp, AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = AccentBlue,
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
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
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
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Location picker - same style as passenger, old rider signature kept
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LocationSelectionCard(
    label: String,
    selectedLocation: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    locations: List<String>,
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
                    imageVector = Icons.Default.Place,
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
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextLow,
                modifier = Modifier.size(20.dp)
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

/**
 * A passenger's track record as seen by a rider: their star rating.
 * Mirrors PassengerSharedUI's RiderStats/rememberRiderStats/riderStatsLabel
 * on the other side of the match - same shape, looked up by whichever
 * user id is the one on the other end of the trip.
 */
data class PassengerStats(
    val ratingAverage: Double,
    val ratingCount: Int
) {
    // An unrated passenger isn't a bad passenger - showing 0★ would read
    // as a warning nobody earned. Five stars until evidence says otherwise.
    val displayRating: Double get() = if (ratingCount <= 0) 5.0 else ratingAverage
}

/**
 * Fetches [passengerId]'s rating stats once per passenger, for display on
 * the matched-passenger cards. Returns null while loading or when there's
 * no passenger to look up yet.
 */
@Composable
fun rememberPassengerStats(passengerId: String): PassengerStats? {
    var stats by remember(passengerId) { mutableStateOf<PassengerStats?>(null) }

    LaunchedEffect(passengerId) {
        if (passengerId.isBlank()) {
            stats = null
            return@LaunchedEffect
        }

        UserRepository().getUserByUid(passengerId) { result ->
            stats = result.getOrNull()?.let {
                PassengerStats(
                    ratingAverage = it.ratingAverage,
                    ratingCount = it.ratingCount
                )
            }
        }
    }

    return stats
}

/** "★ 4.8", ready to drop into any Text/info row. */
fun passengerStatsLabel(stats: PassengerStats): String {
    return "★ ${"%.1f".format(stats.displayRating)}"
}