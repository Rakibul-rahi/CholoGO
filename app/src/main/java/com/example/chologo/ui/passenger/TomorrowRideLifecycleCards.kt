package com.example.chologo.ui.passenger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.RideRequest

/**
 * Five states of a matched Tomorrow leg, mirroring the Ride Now lifecycle
 * cards (RideAcceptedCard/RideOngoingCard/PassengerRideStartConfirmationCard/
 * etc.) but built in the Tomorrow tab's own visual style
 * (PassengerSectionCard/MiniBadge/RideMetaRow/Lime-accent theme) instead of
 * copying Ride Now's plainer default-Material3 cards.
 */

@Composable
fun TomorrowRideAcceptedCard(
    request: RideRequest,
    onCallRider: () -> Unit
) {
    PassengerSectionCard(
        title = if (request.tripDirection == "to_campus") {
            "Accepted Campus Ride"
        } else {
            "Accepted Return Ride"
        },
        subtitle = "Your rider will start the trip when ready.",
        icon = "✅"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Accepted", accent = AccentEmerald)
        }

        Spacer(modifier = Modifier.height(14.dp))

        RideMetaRow(Icons.Default.LocationOn, "${request.pickup} → ${request.destination}")
        Spacer(modifier = Modifier.height(8.dp))
        RideMetaRow(Icons.Default.Schedule, request.tripTime)
        Spacer(modifier = Modifier.height(8.dp))
        RideMetaRow(Icons.Default.Person, request.matchedRiderName.ifBlank { "Accepted rider" })

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onCallRider,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Lime),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Call Rider")
        }
    }
}

@Composable
fun TomorrowRideStartConfirmationCard(
    request: RideRequest,
    onConfirmStarted: () -> Unit,
    onRejectStarted: () -> Unit
) {
    PassengerSectionCard(
        title = "Has the Ride Started?",
        subtitle = "Your rider marked this trip as started. Please confirm.",
        icon = "🚦"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Awaiting confirmation", accent = AccentAmber)
        }

        Spacer(modifier = Modifier.height(14.dp))

        RideMetaRow(Icons.Default.LocationOn, "${request.pickup} → ${request.destination}")
        Spacer(modifier = Modifier.height(8.dp))
        RideMetaRow(Icons.Default.Person, request.matchedRiderName.ifBlank { "Your rider" })

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onConfirmStarted,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Yes, Started", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onRejectStarted,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHigh)
            ) {
                Text("Not Yet")
            }
        }
    }
}

@Composable
fun TomorrowRideOngoingCard(
    request: RideRequest,
    onCallRider: () -> Unit
) {
    PassengerSectionCard(
        title = "Trip in Progress",
        subtitle = "You are currently on the ride.",
        icon = "🏍️"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Ongoing", accent = AccentBlue)
        }

        Spacer(modifier = Modifier.height(14.dp))

        RideMetaRow(Icons.Default.LocationOn, "${request.pickup} → ${request.destination}")
        Spacer(modifier = Modifier.height(8.dp))
        RideMetaRow(Icons.Default.Person, request.matchedRiderName.ifBlank { "Your rider" })

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onCallRider,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Lime),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Call Rider")
        }
    }
}

@Composable
fun TomorrowRideCompletionConfirmationCard(
    request: RideRequest,
    onConfirmCompleted: () -> Unit,
    onReportIssue: () -> Unit
) {
    PassengerSectionCard(
        title = "Did You Safely Arrive?",
        subtitle = "Your rider marked this trip as completed. Please confirm.",
        icon = "🏁"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Awaiting confirmation", accent = AccentAmber)
        }

        Spacer(modifier = Modifier.height(14.dp))

        RideMetaRow(Icons.Default.LocationOn, "${request.pickup} → ${request.destination}")
        Spacer(modifier = Modifier.height(8.dp))
        RideMetaRow(Icons.Default.Person, request.matchedRiderName.ifBlank { "Your rider" })

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onConfirmCompleted,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Yes, Completed", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onReportIssue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
        ) {
            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Report Issue")
        }
    }
}

@Composable
fun TomorrowRideCompletedCard(
    request: RideRequest,
    onRateRide: () -> Unit,
    onReportRide: () -> Unit
) {
    val ratingDisabled = request.riderRated || request.issueReported
    val reportDisabled = request.issueReported || request.riderRated

    PassengerSectionCard(
        title = "Trip Completed",
        subtitle = "You safely reached your destination.",
        icon = "🎉"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(text = "Completed", accent = AccentEmerald)
        }

        Spacer(modifier = Modifier.height(14.dp))

        RideMetaRow(Icons.Default.LocationOn, "${request.pickup} → ${request.destination}")
        Spacer(modifier = Modifier.height(8.dp))
        RideMetaRow(Icons.Default.Person, request.matchedRiderName.ifBlank { "Your rider" })

        if (request.riderRated) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You rated this rider ${request.rating}/5",
                color = Lime,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        if (request.issueReported) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Issue reported. Our team will review it.",
                color = AccentRed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onRateRide,
            enabled = !ratingDisabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Color.Black,
                disabledContainerColor = LimeDeep,
                disabledContentColor = Color.Black
            )
        ) {
            Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (request.riderRated) "Rating Submitted" else "Rate Rider",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onReportRide,
            enabled = !reportDisabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
        ) {
            Icon(imageVector = Icons.Default.Report, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (request.issueReported) "Issue Reported" else "Report Rider")
        }
    }
}
