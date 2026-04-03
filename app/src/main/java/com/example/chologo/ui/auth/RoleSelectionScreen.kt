package com.example.chologo.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkCharcoal = Color(0xFF222831)
private val LimeGreen = Color(0xFF9BCB2D)
private val SoftWhite = Color(0xFFF5F5F5)

@Composable
fun RoleSelectionScreen(
    onPassengerSelected: () -> Unit = {},
    onRiderSelected: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Your Role",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DarkCharcoal
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Select how you want to continue",
                fontSize = 14.sp,
                color = DarkCharcoal.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPassengerSelected() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Passenger",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoal
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Book rides and travel easily around campus",
                        fontSize = 14.sp,
                        color = DarkCharcoal.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRiderSelected() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LimeGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Rider",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Offer rides and earn while helping students",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}