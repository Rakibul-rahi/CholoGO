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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.RideNowRequest

@Composable
fun RideCompletedCard(
    request: RideNowRequest,
    isRider: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.dp,
            AccentEmerald.copy(alpha = 0.35f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientSuccess)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                //-----------------------------------
                // TOP ICON
                //-----------------------------------

                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(LimeDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                //-----------------------------------
                // TITLE
                //-----------------------------------

                Text(
                    text = if (isRider) {
                        "Trip Finished"
                    } else {
                        "Ride Completed 🎉"
                    },
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isRider) {
                        "Passenger safely reached destination."
                    } else {
                        "Thanks for riding with CholoGO."
                    },
                    color = TextMed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                //-----------------------------------
                // INFO CARD
                //-----------------------------------

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardElevated.copy(alpha = 0.70f))
                        .padding(14.dp)
                ) {

                    RideCompletedInfoRow(
                        label = if (isRider) {
                            "Passenger"
                        } else {
                            "Rider"
                        },
                        value = if (isRider) {
                            request.passengerName.ifBlank { "Passenger" }
                        } else {
                            request.matchedRiderName.ifBlank { "Rider" }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    RideCompletedInfoRow(
                        label = "Route",
                        value = "${request.pickup} → ${request.destination}"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    RideCompletedInfoRow(
                        label = if (isRider) {
                            "Earned"
                        } else {
                            "Fare"
                        },
                        value = "${getCompletedRideFare(request)} Tk"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    RideCompletedInfoRow(
                        label = if (isRider) {
                            "Distance"
                        } else {
                            "XP Earned"
                        },
                        value = if (isRider) {
                            "5.2 km"
                        } else {
                            "+10 XP"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                //-----------------------------------
                // RATING SECTION
                //-----------------------------------

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    repeat(5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRider) {
                        "Passenger rated you 5⭐"
                    } else {
                        "Rate your rider"
                    },
                    color = TextMed,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                //-----------------------------------
                // FOOTER MESSAGE
                //-----------------------------------

                Text(
                    text = if (isRider) {
                        "You are now available to receive new Ride Now requests."
                    } else {
                        "You saved 65 Tk compared to traditional ride sharing."
                    },
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RideCompletedInfoRow(
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
            textAlign = TextAlign.End
        )
    }
}

private fun getCompletedRideFare(
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