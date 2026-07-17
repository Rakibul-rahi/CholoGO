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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.RideNowRequest

@Composable
fun RideOngoingCard(
    request: RideNowRequest,
    isRider: Boolean,
    isProcessing: Boolean,
    onCompleteTrip: () -> Unit,
    onCall: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.dp,
            AccentBlue.copy(alpha = 0.35f)
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
                    .padding(16.dp)
            ) {

                //-----------------------------------
                // HEADER
                //-----------------------------------

                RideOngoingHeader(
                    request = request,
                    isRider = isRider
                )

                Spacer(modifier = Modifier.height(14.dp))

                //-----------------------------------
                // ROUTE INFO
                //-----------------------------------

                RideOngoingInfoSection(
                    request = request,
                    isRider = isRider
                )

                Spacer(modifier = Modifier.height(14.dp))

                //-----------------------------------
                // LIVE STATUS
                //-----------------------------------

                RideOngoingStatusSection()

                Spacer(modifier = Modifier.height(14.dp))

                //-----------------------------------
                // SAFETY MESSAGE
                //-----------------------------------

                RideSafetyBanner()

                Spacer(modifier = Modifier.height(14.dp))

                //-----------------------------------
                // ACTIONS
                //-----------------------------------

                if (isRider) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        RideOngoingActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Call",
                            icon = Icons.Default.Call,
                            enabled = !isProcessing,
                            primary = false,
                            onClick = onCall
                        )

                        RideOngoingActionButton(
                            modifier = Modifier.weight(1f),
                            text = "Ride Completed",
                            icon = Icons.Default.Flag,
                            enabled = !isProcessing,
                            primary = true,
                            onClick = onCompleteTrip
                        )
                    }

                } else {

                    RideOngoingActionButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Call Rider",
                        icon = Icons.Default.Call,
                        enabled = !isProcessing,
                        primary = true,
                        onClick = onCall
                    )
                }
            }
        }
    }
}

@Composable
private fun RideOngoingHeader(
    request: RideNowRequest,
    isRider: Boolean
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Column {

                Text(
                    text = if (isRider) {
                        "Trip Ongoing"
                    } else {
                        "Ride in Progress"
                    },
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = if (isRider) {
                        request.passengerName.ifBlank { "Passenger" }
                    } else {
                        request.matchedRiderName.ifBlank { "Rider" }
                    },
                    color = TextMed,
                    fontSize = 13.sp
                )
            }
        }

        MiniBadge(
            text = "ONGOING",
            accent = AccentBlue
        )
    }
}

@Composable
private fun RideOngoingInfoSection(
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

        RideOngoingInfoRow(
            icon = Icons.Default.LocationOn,
            label = "Pickup",
            value = request.pickup
        )

        Spacer(modifier = Modifier.height(10.dp))

        RideOngoingInfoRow(
            icon = Icons.Default.Navigation,
            label = "Destination",
            value = request.destination
        )

        Spacer(modifier = Modifier.height(10.dp))

        RideOngoingInfoRow(
            icon = Icons.Default.Timer,
            label = if (isRider) {
                "Estimated Fare"
            } else {
                "Current Fare"
            },
            value = "${getOngoingRideFare(request)} Tk"
        )
    }
}

@Composable
private fun RideOngoingInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.size(10.dp))

        Column {

            Text(
                text = label,
                color = TextMed,
                fontSize = 11.sp
            )

            Text(
                text = value,
                color = TextHigh,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun RideOngoingStatusSection() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardElevated.copy(alpha = 0.55f))
            .padding(14.dp)
    ) {

        Text(
            text = "Live Trip Status",
            color = TextHigh,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            RideStatusStep(
                title = "Accepted",
                completed = true
            )

            RideStatusStep(
                title = "Started",
                completed = true
            )

            RideStatusStep(
                title = "Ongoing",
                completed = true
            )

            RideStatusStep(
                title = "Completed",
                completed = false
            )
        }
    }
}

@Composable
private fun RideStatusStep(
    title: String,
    completed: Boolean
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (completed) {
                        AccentBlue
                    } else {
                        BorderSubtle
                    }
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = if (completed) "✓" else "",
                color = if (completed) BgDeep else TextLow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            color = if (completed) TextHigh else TextLow,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun RideSafetyBanner() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AccentAmber.copy(alpha = 0.12f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.size(10.dp))

        Text(
            text = "Safety monitoring active. If trip becomes inactive for too long, CholoGO will ask both users for confirmation.",
            color = TextMed,
            fontSize = 12.sp,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun RideOngoingActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit
) {

    val backgroundBrush = when {

        primary && enabled -> {
            GradientLime
        }

        else -> {
            Brush.linearGradient(
                listOf(
                    CardElevated,
                    CardElevated
                )
            )
        }
    }

    val contentColor = when {

        primary && enabled -> {
            BgDeep
        }

        enabled -> {
            TextHigh
        }

        else -> {
            TextLow
        }
    }

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush)
            .clickable(enabled = enabled) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        if (!enabled && primary) {

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

private fun getOngoingRideFare(
    request: RideNowRequest
): Int {

    return when {

        request.pickup.equals("AUST Gate", ignoreCase = true) ||
                request.destination.equals("AUST Gate", ignoreCase = true) -> {
            40
        }

        else -> {
            50
        }
    }
}