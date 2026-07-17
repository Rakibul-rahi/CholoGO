package com.example.chologo.ui.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chologo.data.model.RideNowRequest

@Composable
fun PassengerRideStartConfirmationCard(
    request: RideNowRequest,
    onConfirmStarted: () -> Unit,
    onRejectStarted: () -> Unit
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
            Text(
                text = "Ride Start Confirmation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

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

            // Ride info
            Text("Pickup: ${request.pickup}")

            Text("Destination: ${request.destination}")

            Text("Time: ${request.tripTime}")

            // Confirmation box
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(14.dp)
            ) {

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null
                        )

                        Text(
                            text = "Has the ride started?",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Please confirm whether your rider has started the trip.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // YES button
            Button(
                onClick = onConfirmStarted,
                modifier = Modifier.fillMaxWidth()
            ) {

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("YES, STARTED")
            }

            // NOT YET button
            OutlinedButton(
                onClick = onRejectStarted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NOT YET")
            }
        }
    }
}