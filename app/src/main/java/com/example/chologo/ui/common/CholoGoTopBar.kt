package com.example.chologo.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.R

@Composable
fun CholoGoTopBar(
    modifier: Modifier = Modifier,
    onLogoClick: () -> Unit = {},
    onRideHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val bgSurface = Color(0xFF111418)
    val bgDeep = Color(0xFF0A0D0F)
    val bgElevated = Color(0xFF1C2228)

    val lime = Color(0xFFC6F135)
    val textHigh = Color(0xFFF1F5F9)
    val borderFocus = lime.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgSurface,
                        bgDeep
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CholoGoLogoButton(
            lime = lime,
            textHigh = textHigh,
            onClick = onLogoClick
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TopBarIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Ride History",
                        tint = lime,
                        modifier = Modifier.size(21.dp)
                    )
                },
                backgroundColor = bgElevated,
                borderColor = borderFocus,
                onClick = onRideHistoryClick
            )

            TopBarIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = textHigh,
                        modifier = Modifier.size(21.dp)
                    )
                },
                backgroundColor = bgElevated,
                borderColor = borderFocus,
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun CholoGoLogoButton(
    lime: Color,
    textHigh: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(lime),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.chologologo),
                contentDescription = "CholoGO Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(28.dp)
                    .padding(2.dp)
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cholo",
                color = textHigh,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "GO",
                color = lime,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}