package com.example.chologo.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
fun CholoGoTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgCard = Color(0xFF161B20)
    val bgElevated = Color(0xFF1C2228)
    val lime = Color(0xFFC6F135)
    val emerald = Color(0xFF34D399)
    val textLow = Color(0xFF4E5A66)
    val border = Color.White.copy(alpha = 0.07f)
    val borderFocus = lime.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgCard)
            .border(
                width = 1.dp,
                color = border,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabButton(
            text = "Ride Now",
            icon = "⚡",
            selected = selectedTab == 0,
            lime = lime,
            textLow = textLow,
            showLiveDot = true,
            emerald = emerald,
            borderFocus = borderFocus,
            bgElevated = bgElevated,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clickable { onTabSelected(0) }
        )

        TabButton(
            text = "Tomorrow",
            icon = "📅",
            selected = selectedTab == 1,
            lime = lime,
            textLow = textLow,
            showLiveDot = false,
            emerald = emerald,
            borderFocus = borderFocus,
            bgElevated = bgElevated,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clickable { onTabSelected(1) }
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: String,
    selected: Boolean,
    lime: Color,
    textLow: Color,
    showLiveDot: Boolean,
    emerald: Color,
    borderFocus: Color,
    bgElevated: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    bgElevated,
                                    lime.copy(alpha = 0.06f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = borderFocus,
                            shape = RoundedCornerShape(14.dp)
                        )
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            fontSize = 14.sp
        )

        Text(
            text = " $text",
            color = if (selected) lime else textLow,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (showLiveDot && selected) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(emerald)
            )
        }
    }
}