package com.example.chologo.ui.passenger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chologo.data.model.RideNowRequest

@Composable
fun PassengerRideCompletedCard(
    request: RideNowRequest,
    onRateRide: (() -> Unit)? = null,
    onReportRide: (() -> Unit)? = null
) {
    val ratingDisabled =
        request.riderRated || request.issueReported

    val reportDisabled =
        request.issueReported || request.riderRated

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Ride Completed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "You safely reached your destination.",
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rider: ${request.matchedRiderName.ifBlank { "Unknown rider" }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(text = "Pickup: ${request.pickup}")
                Text(text = "Destination: ${request.destination}")
                Text(text = "Time: ${request.tripTime}")
            }

            if (request.riderRated) {
                Text(
                    text = "You rated this rider ${request.rating}/5",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (request.issueReported) {
                Text(
                    text = "Issue reported. Our team will review it.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (onRateRide != null || onReportRide != null) {
                HorizontalDivider()
            }

            if (onRateRide != null) {
                Button(
                    onClick = onRateRide,
                    enabled = !ratingDisabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (request.riderRated)
                            "Rating Submitted"
                        else
                            "Rate Rider"
                    )
                }
            }

            if (onReportRide != null) {
                OutlinedButton(
                    onClick = onReportRide,
                    enabled = !reportDisabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (request.issueReported)
                            "Issue Reported"
                        else
                            "Report Rider"
                    )
                }
            }
        }
    }
}