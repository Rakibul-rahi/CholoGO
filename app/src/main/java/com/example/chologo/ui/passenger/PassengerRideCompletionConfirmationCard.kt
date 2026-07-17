package com.example.chologo.ui.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chologo.data.model.RideNowRequest

@Composable
fun PassengerRideCompletionConfirmationCard(
    request: RideNowRequest,
    onConfirmCompleted: () -> Unit,
    onReportIssue: () -> Unit
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
                text = "Ride Completion Confirmation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Rider info
            Text(
                text = "Rider: ${request.matchedRiderName}",
                style = MaterialTheme.typography.titleMedium
            )

            // Ride info
            Text("Pickup: ${request.pickup}")

            Text("Destination: ${request.destination}")

            Text("Time: ${request.tripTime}")

            // Info box
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(14.dp)
            ) {

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "Did you safely reach your destination?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Please confirm before completing the trip.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Confirm button
            Button(
                onClick = onConfirmCompleted,
                modifier = Modifier.fillMaxWidth()
            ) {

                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("YES, COMPLETED")
            }

            // Report issue button
            OutlinedButton(
                onClick = onReportIssue,
                modifier = Modifier.fillMaxWidth()
            ) {

                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("REPORT ISSUE")
            }
        }
    }
}