package com.example.chologo.ui.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chologo.data.model.RideNowRequest

@Composable
fun PassengerRideOngoingCard(
    request: RideNowRequest,
    onCallRider: () -> Unit = {}
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "Ride Ongoing",
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
            Text("Pickup: ${request.pickup}")

            Text("Destination: ${request.destination}")

            Text("Time: ${request.tripTime}")

            // Ongoing status box
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(14.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {

                        Text(
                            text = "Trip in Progress",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "You are currently on the ride.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Call Rider Button
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
        }
    }
}