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
import com.example.chologo.ui.theme.LocalIsDarkTheme

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
    val isDark = LocalIsDarkTheme.current
    val bgCard = if (isDark) Color(0xFF161B20) else Color(0xFFFFFFFF)
    val lime = if (isDark) Color(0xFFC6F135) else Color(0xFF5E7A17)
    val limeDim = if (isDark) Color(0xFF9DC429) else Color(0xFF4C6412)
    val textHigh = if (isDark) Color(0xFFF1F5F9) else Color(0xFF10151B)
    val textMed = if (isDark) Color(0xFF8B96A5) else Color(0xFF4B5563)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF1A2410),
                            Color(0xFF0D1A0A),
                            bgCard
                        )
                    } else {
                        listOf(
                            Color(0xFFEDF6DC),
                            Color(0xFFF6FAF0),
                            bgCard
                        )
                    }
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
                    contentColor = if (isDark) Color.Black else Color.White
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
