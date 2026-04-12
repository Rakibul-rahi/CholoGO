package com.example.chologo.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun LevelCard(
    level: Int,
    levelTitle: String,
    currentXp: Long,
    xpNeededForNextLevel: Long,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val goldLight = Color(0xFFFFD27F)
    val goldDark = Color(0xFFB8860B)
    val goldAccent = Color(0xFFFFC84A)
    val cardBorder = Color(0xFF2E2E4E)

    val safeProgress = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1E35),
                        Color(0xFF14142A),
                        Color(0xFF1A1A30)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        goldAccent.copy(alpha = 0.35f),
                        cardBorder.copy(alpha = 0.35f),
                        goldAccent.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(goldAccent, goldDark)
                            )
                        )
                ) {
                    Text(
                        text = "$level",
                        color = Color(0xFF1A1200),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column {
                    Text(
                        text = levelTitle.uppercase(),
                        color = goldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Level $level",
                        color = Color(0xFFF0EAD6),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${"%,d".format(currentXp)} XP",
                    color = Color(0xFFF0EAD6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(safeProgress * 100).toInt()}%",
                    color = goldAccent.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2A45))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(safeProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(goldDark, goldAccent, goldLight)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${"%,d".format(xpNeededForNextLevel)} XP to next level",
                color = Color(0xFF8888AA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp
            )
        }
    }
}