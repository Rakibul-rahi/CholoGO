package com.example.chologo.ui.rider

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.ui.rider.AccentEmerald
import com.example.chologo.ui.rider.AccentRed
import com.example.chologo.ui.rider.BgDeep
import com.example.chologo.ui.rider.BorderFocus
import com.example.chologo.ui.rider.CardElevated
import com.example.chologo.ui.rider.GradientLime
import com.example.chologo.ui.rider.GradientSuccess
import com.example.chologo.ui.rider.Lime
import com.example.chologo.ui.rider.LimeDim
import com.example.chologo.ui.rider.MiniBadge
import com.example.chologo.ui.rider.TextHigh
import com.example.chologo.ui.rider.TextLow
import com.example.chologo.ui.rider.TextMed

@Composable
fun RideAcceptedCard(
    request: RideNowRequest,
    isRider: Boolean,
    isProcessing: Boolean,
    onStartTrip: () -> Unit,
    onCancelRide: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientSuccess)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                RideAcceptedHeader(
                    name = if (isRider) {
                        request.passengerName.ifBlank { "Passenger" }
                    } else {
                        request.matchedRiderName.ifBlank { "Rider" }
                    },
                    title = if (isRider) {
                        "Passenger matched successfully"
                    } else {
                        "Rider accepted your request"
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                RideAcceptedInfoSection(
                    request = request,
                    isRider = isRider
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isRider) {
                        "Start the trip only after the passenger gets on the bike."
                    } else {
                        "Your rider is ready. Call if needed and wait near the pickup point."
                    },
                    color = TextMed,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RideAcceptedActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Call",
                        icon = Icons.Default.Call,
                        enabled = !isProcessing,
                        primary = false,
                        onClick = onCall
                    )

                    RideAcceptedActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Cancel",
                        icon = Icons.Default.Cancel,
                        enabled = !isProcessing,
                        primary = false,
                        danger = true,
                        onClick = onCancelRide
                    )
                }

                if (isRider) {
                    Spacer(modifier = Modifier.height(10.dp))

                    RideAcceptedActionButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Started",
                        icon = Icons.Default.PlayArrow,
                        enabled = !isProcessing,
                        primary = true,
                        onClick = onStartTrip
                    )
                }
            }
        }
    }
}

@Composable
private fun RideAcceptedHeader(
    name: String,
    title: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(LimeDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Lime,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Column {
                Text(
                    text = title,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Text(
                    text = name,
                    color = TextMed,
                    fontSize = 13.sp
                )
            }
        }

        MiniBadge(
            text = "Accepted",
            accent = AccentEmerald
        )
    }
}

@Composable
private fun RideAcceptedInfoSection(
    request: RideNowRequest,
    isRider: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardElevated.copy(alpha = 0.70f))
            .padding(14.dp)
    ) {
        RideAcceptedInfoRow(
            label = "Pickup",
            value = request.pickup.ifBlank { "Not selected" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RideAcceptedInfoRow(
            label = "Destination",
            value = request.destination.ifBlank { "Not selected" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RideAcceptedInfoRow(
            label = "Trip time",
            value = request.tripTime.ifBlank { "Now" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RideAcceptedInfoRow(
            label = "Fare",
            value = "${getEstimatedRideNowFare(request)} Tk"
        )

        Spacer(modifier = Modifier.height(8.dp))

        RideAcceptedInfoRow(
            label = if (isRider) "Passenger contact" else "Rider contact",
            value = if (isRider) {
                "Available after passengerPhone field"
            } else {
                request.matchedRiderPhone.ifBlank { "N/A" }
            }
        )
    }
}

@Composable
private fun RideAcceptedInfoRow(
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

@Composable
private fun RideAcceptedActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    primary: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundBrush = when {
        primary && enabled -> GradientLime
        else -> Brush.linearGradient(listOf(CardElevated, CardElevated))
    }

    val contentColor = when {
        primary && enabled -> BgDeep
        danger && enabled -> AccentRed
        enabled -> TextHigh
        else -> TextLow
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush)
            .clickable(enabled = enabled) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isProcessingButton(text, enabled)) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = BgDeep,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = text,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun isProcessingButton(
    text: String,
    enabled: Boolean
): Boolean {
    return !enabled && text == "Started"
}

private fun getEstimatedRideNowFare(request: RideNowRequest): Int {
    return when {
        request.pickup.equals("AUST Gate", ignoreCase = true) ||
                request.destination.equals("AUST Gate", ignoreCase = true) -> 40

        else -> 50
    }
}