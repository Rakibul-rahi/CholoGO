package com.example.chologo.ui.rider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.RideNowStatus

@Composable
fun RideConfirmationCard(
    title: String,
    message: String,
    status: String,
    isRider: Boolean
) {
    val config = getConfirmationConfig(status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.dp,
            config.accent.copy(alpha = 0.35f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(config.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = null,
                        tint = config.accent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = TextMed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                StatusInfoBox(
                    status = status,
                    label = config.label,
                    accent = config.accent,
                    isRider = isRider
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = config.helperText,
                    color = TextMed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatusInfoBox(
    status: String,
    label: String,
    accent: Color,
    isRider: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardElevated.copy(alpha = 0.70f))
            .padding(14.dp)
    ) {
        ConfirmationRow(
            label = "Current Status",
            value = label
        )

        Spacer(modifier = Modifier.height(8.dp))

        ConfirmationRow(
            label = "User Side",
            value = if (isRider) "Rider" else "Passenger"
        )

        Spacer(modifier = Modifier.height(8.dp))

        ConfirmationRow(
            label = "System Action",
            value = getSystemActionText(status)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(accent.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(getProgressValue(status))
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(accent)
            )
        }
    }
}

@Composable
private fun ConfirmationRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMed,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = TextHigh,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private data class ConfirmationConfig(
    val icon: ImageVector,
    val accent: Color,
    val label: String,
    val helperText: String
)

private fun getConfirmationConfig(status: String): ConfirmationConfig {
    return when (status) {
        RideNowStatus.START_PENDING_CONFIRMATION -> {
            ConfirmationConfig(
                icon = Icons.Default.Timer,
                accent = AccentAmber,
                label = "Start Pending",
                helperText = "The ride will become ongoing only after passenger confirmation."
            )
        }

        RideNowStatus.END_PENDING_CONFIRMATION -> {
            ConfirmationConfig(
                icon = Icons.Default.Flag,
                accent = AccentAmber,
                label = "End Pending",
                helperText = "The ride will be completed only after passenger confirms safe arrival."
            )
        }

        RideNowStatus.CANCELLED -> {
            ConfirmationConfig(
                icon = Icons.Default.Cancel,
                accent = AccentRed,
                label = "Cancelled",
                helperText = "This ride is closed and cannot continue."
            )
        }

        RideNowStatus.EXPIRED -> {
            ConfirmationConfig(
                icon = Icons.Default.HourglassTop,
                accent = AccentRed,
                label = "Expired",
                helperText = "This request was not accepted within the allowed time."
            )
        }

        else -> {
            ConfirmationConfig(
                icon = Icons.Default.Info,
                accent = AccentBlue,
                label = status,
                helperText = "Waiting for the next ride status update."
            )
        }
    }
}

private fun getSystemActionText(status: String): String {
    return when (status) {
        RideNowStatus.START_PENDING_CONFIRMATION -> "Passenger must confirm ride start"
        RideNowStatus.END_PENDING_CONFIRMATION -> "Passenger must confirm completion"
        RideNowStatus.CANCELLED -> "Ride cancelled"
        RideNowStatus.EXPIRED -> "Request expired"
        else -> "Waiting"
    }
}

private fun getProgressValue(status: String): Float {
    return when (status) {
        RideNowStatus.START_PENDING_CONFIRMATION -> 0.45f
        RideNowStatus.END_PENDING_CONFIRMATION -> 0.85f
        RideNowStatus.CANCELLED -> 1f
        RideNowStatus.EXPIRED -> 1f
        else -> 0.25f
    }
}