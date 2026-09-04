package com.example.chologo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.chologo.utils.Greeting

@Composable
fun LevelCard(
    level: Int,
    levelTitle: String,
    currentXp: Long,
    xpNeededForNextLevel: Long,
    progress: Float,
    modifier: Modifier = Modifier,
    userName: String = "Rakib"
) {
    val isDark = LocalIsDarkTheme.current
    val bgCard = if (isDark) Color(0xFF161B20) else Color(0xFFFFFFFF)
    val bgSurface = if (isDark) Color(0xFF111418) else Color(0xFFEFF2F5)
    val lime = if (isDark) Color(0xFFC6F135) else Color(0xFF5E7A17)
    val limeDim = if (isDark) Color(0xFF9DC429) else Color(0xFF4C6412)
    val limeDeep = Color(0xFF6F8F1A)
    val textHigh = if (isDark) Color(0xFFF1F5F9) else Color(0xFF10151B)
    val textMed = if (isDark) Color(0xFF8B96A5) else Color(0xFF4B5563)
    val border = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.08f)

    val safeProgress = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)

    val greeting = Greeting.forHour().uppercase()

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
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = greeting,
                color = limeDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$userName 👋",
                color = textHigh,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(lime, limeDim)
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LVL $level",
                            color = if (isDark) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = levelTitle,
                        color = textMed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "${"%,d".format(currentXp)} / ${"%,d".format(xpNeededForNextLevel)} XP",
                    color = limeDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(safeProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(limeDeep, lime)
                            )
                        )
                )
            }
        }
    }
}