package com.example.chologo.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chologo.data.model.RideHistory
import com.example.chologo.viewmodel.RideNowViewModel

@Composable
fun RideHistoryScreen(
    userId: String,
    source: String,
    viewModel: RideNowViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId, source) {
        if (source == "passenger") {
            viewModel.listenPassengerRideHistory(userId)
        } else {
            viewModel.listenRiderRideHistory(userId)
        }
    }

    val bgDark = Color(0xFF0B0F14)
    val cardDark = Color(0xFF161B20)
    val green = Color(0xFF00C853)
    val softText = Color(0xFFB0BEC5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            RideHistoryTopBar(
                onBackClick = onBackClick,
                green = green
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    RideHistoryHeroCard(
                        totalRides = uiState.rideHistory.size,
                        source = source,
                        cardDark = cardDark,
                        green = green,
                        softText = softText
                    )
                }

                if (uiState.rideHistory.isEmpty()) {
                    item {
                        EmptyRideHistoryCard(
                            cardDark = cardDark,
                            green = green,
                            softText = softText
                        )
                    }
                } else {
                    items(uiState.rideHistory) { history ->
                        RideHistoryCard(
                            history = history,
                            source = source,
                            cardDark = cardDark,
                            green = green,
                            softText = softText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RideHistoryTopBar(
    onBackClick: () -> Unit,
    green: Color
) {
    val topBarBg = Color(0xFF0B0F14)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarBg)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column {
            Text(
                text = "Ride History",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Your completed CholoGO trips",
                color = Color(0xFF90A4AE),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(green.copy(alpha = 0.15f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = green
            )
        }
    }
}

@Composable
private fun RideHistoryHeroCard(
    totalRides: Int,
    source: String,
    cardDark: Color,
    green: Color,
    softText: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF16251E),
                            Color(0xFF161B20)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(green.copy(alpha = 0.16f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = green
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (source == "passenger") {
                            "Passenger History"
                        } else {
                            "Rider History"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$totalRides completed ride${if (totalRides == 1) "" else "s"}",
                        color = softText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRideHistoryCard(
    cardDark: Color,
    green: Color,
    softText: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(green.copy(alpha = 0.15f))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = green
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No ride history yet",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Completed rides will appear here after a passenger confirms ride completion.",
                color = softText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RideHistoryCard(
    history: RideHistory,
    source: String,
    cardDark: Color,
    green: Color,
    softText: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(green.copy(alpha = 0.14f))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = green
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (history.rideType == "ride_now") {
                            "Ride Now"
                        } else {
                            "Tomorrow Ride"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Completed",
                        color = green,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            RideHistoryInfoRow(
                icon = Icons.Default.LocationOn,
                title = "Pickup",
                value = history.pickup.ifBlank { "Unknown" },
                softText = softText
            )

            Spacer(modifier = Modifier.height(8.dp))

            RideHistoryInfoRow(
                icon = Icons.Default.LocationOn,
                title = "Destination",
                value = history.destination.ifBlank { "Unknown" },
                softText = softText
            )

            if (history.tripTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                RideHistoryInfoRow(
                    icon = Icons.Default.Schedule,
                    title = "Time",
                    value = history.tripTime,
                    softText = softText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (source == "passenger") {
                RideHistoryInfoRow(
                    icon = Icons.Default.Person,
                    title = "Rider",
                    value = history.riderName.ifBlank { "Unknown" },
                    softText = softText
                )

                if (history.riderPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    RideHistoryInfoRow(
                        icon = Icons.Default.Phone,
                        title = "Phone",
                        value = history.riderPhone,
                        softText = softText
                    )
                }
            } else {
                RideHistoryInfoRow(
                    icon = Icons.Default.Person,
                    title = "Passenger",
                    value = history.passengerName.ifBlank { "Unknown" },
                    softText = softText
                )
            }
        }
    }
}

@Composable
private fun RideHistoryInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    softText: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF90A4AE)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = title,
                color = softText,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}