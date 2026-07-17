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

@Composable
fun PassengerRideAcceptedCard(
    request: RideNowRequest,
    onCallRider: () -> Unit = {},
    onCancelRide: (() -> Unit)? = null
) {

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
                    text = "Ride Accepted",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Rider info
            Text(
                text = "Rider: ${request.matchedRiderName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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

            // Status message
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "Your rider accepted the trip. Waiting for ride start.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Call Rider
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

            // Cancel Ride
            if (onCancelRide != null) {

                OutlinedButton(
                    onClick = onCancelRide,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Ride")
                }
            }
        }
    }
}