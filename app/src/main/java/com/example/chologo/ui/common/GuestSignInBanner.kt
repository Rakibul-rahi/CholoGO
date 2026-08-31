package com.example.chologo.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shown in place of [com.example.chologo.ui.components.LevelCard] for a
 * signed-out user browsing the app - explains that they're looking around
 * without an account and gives a single entry point into the auth flow.
 */
@Composable
fun GuestSignInBanner(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgCard = Color(0xFF161B20)
    val lime = Color(0xFFC6F135)
    val limeDim = Color(0xFF9DC429)
    val textHigh = Color(0xFFF1F5F9)
    val textMed = Color(0xFF8B96A5)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A2410),
                        Color(0xFF0D1A0A),
                        bgCard
                    )
                )
            )
            .border(
                width = 1.dp,
                color = lime.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BROWSING AS GUEST",
                    color = limeDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Welcome to CholoGO 👋",
                    color = textHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sign in to save a Tomorrow Ride or request a ride now.",
                    color = textMed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Button(
                onClick = onSignInClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = lime,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Sign In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
