package com.example.chologo.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.R

// ─── Design Tokens ───────────────────────────────────────────────────────────────

private val BgDeep       = Color(0xFF080C10)
private val CardBase     = Color(0xFF141A21)
private val CardElevated = Color(0xFF1A2130)

private val Lime         = Color(0xFF9FD63F)
private val LimeDeep     = Color(0xFF6FAF1A)
private val LimeDim      = Color(0xFF2A3E18)

private val AccentBlue   = Color(0xFF4D9FFF)
private val AccentEmerald = Color(0xFF30D878)

private val TextHigh     = Color(0xFFF0F4F8)
private val TextMed      = Color(0xFF8B9AB0)
private val TextLow      = Color(0xFF4A5568)

private val BorderSubtle = Color(0xFF1E2D3D)

private val GradientLime = Brush.linearGradient(listOf(Lime, LimeDeep))

// ─── Screen ──────────────────────────────────────────────────────────────────────

@Composable
fun AuthChoiceScreen(
    onLoginClick: () -> Unit = {},
    onSignupClick: () -> Unit = {}
) {
    // Breathing glow animation
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.75f,
        animationSpec = infiniteRepeatable(
            tween(2800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {

        // ── Background ambient glows ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopCenter)
                .blur(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(LimeDim.copy(alpha = pulse), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .blur(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF0D1A2E).copy(alpha = 0.8f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // ── Content ───────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top section — logo + tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 72.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chologologo),
                    contentDescription = "CholoGO",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(200.dp)
                        .wrapContentWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Campus Ride Sharing",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHigh,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pill tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill("Smart", AccentBlue)
                    TagPill("Safe", AccentEmerald)
                    TagPill("Affordable", Lime)
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Feature row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color = CardBase)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeaturePill(
                        icon = Icons.Default.Person,
                        label = "Passenger",
                        sub = "Book a seat",
                        tint = AccentBlue
                    )
                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .size(width = 1.dp, height = 40.dp)
                            .background(color = BorderSubtle)
                    )
                    FeaturePill(
                        icon = Icons.Default.DirectionsCar,
                        label = "Rider",
                        sub = "Offer a ride",
                        tint = Lime
                    )
                }
            }

            // Bottom section — CTA buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sign In — primary gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(brush = GradientLime)
                        .clickable { onLoginClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = BgDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 0.3.sp
                    )
                }

                // Sign Up — elevated card style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(color = CardElevated)
                        .clickable { onSignupClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Account",
                        color = TextHigh,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Choose rider or passenger after sign up",
                    fontSize = 12.sp,
                    color = TextLow,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Sub-composables ─────────────────────────────────────────────────────────────

@Composable
private fun TagPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color = color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color = tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = label, color = TextHigh, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(text = sub, color = TextMed, fontSize = 11.sp)
    }
}