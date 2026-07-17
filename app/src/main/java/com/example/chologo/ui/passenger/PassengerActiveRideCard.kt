package com.example.chologo.ui.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus

@Composable
fun PassengerActiveRideCard(
    request: RideNowRequest,
    onCallRider: () -> Unit = {},
    onConfirmStarted: (() -> Unit)? = null,
    onRejectStarted: (() -> Unit)? = null,
    onConfirmCompleted: (() -> Unit)? = null,
    onReportIssue: (() -> Unit)? = null
) {

    val statusText = when (request.status) {

        RideNowStatus.ACCEPTED ->
            "Rider accepted your request"

        RideNowStatus.START_PENDING_CONFIRMATION ->
            "Please confirm if the ride has started"

        RideNowStatus.ONGOING ->
            "Ride is ongoing"

        RideNowStatus.END_PENDING_CONFIRMATION ->
            "Please confirm ride completion"

        RideNowStatus.COMPLETED ->
            "Ride completed successfully"

        RideNowStatus.CANCELLED ->
            "Ride cancelled"

        RideNowStatus.EXPIRED ->
            "Ride expired"

        RideNowStatus.ISSUE_REPORTED ->
            "Issue reported"

        else -> request.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Current Ride",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Rider info
            Text(
                text = "Rider: ${request.matchedRiderName}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Phone: ${request.matchedRiderPhone}",
                style = MaterialTheme.typography.bodyLarge
            )

            // Trip info
            Text(
                text = "Pickup: ${request.pickup}"
            )

            Text(
                text = "Destination: ${request.destination}"
            )

            Text(
                text = "Time: ${request.tripTime}"
            )

            // Status
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Call button
            OutlinedButton(
                onClick = onCallRider,
                modifier = Modifier.fillMaxWidth()
            ) {

                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Call Rider")
            }

            // START CONFIRMATION
            if (request.status == RideNowStatus.START_PENDING_CONFIRMATION) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Button(
                        onClick = {
                            onConfirmStarted?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("YES")
                    }

                    OutlinedButton(
                        onClick = {
                            onRejectStarted?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("NOT YET")
                    }
                }
            }

            // COMPLETION CONFIRMATION
            if (request.status == RideNowStatus.END_PENDING_CONFIRMATION) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Button(
                        onClick = {
                            onConfirmCompleted?.invoke()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("YES, COMPLETED")
                    }

                    OutlinedButton(
                        onClick = {
                            onReportIssue?.invoke()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("REPORT ISSUE")
                    }
                }
            }
        }
    }
}