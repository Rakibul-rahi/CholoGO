package com.example.chologo.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.R

// ─── Palette matching LoginScreen ────────────────────────────────────────────
private val Void        = Color(0xFF050709)
private val Obsidian    = Color(0xFF090D12)
private val Graphite    = Color(0xFF0F141C)
private val SteelDark   = Color(0xFF141B26)
private val SteelMid    = Color(0xFF1C2535)
private val SteelLight  = Color(0xFF232E42)

private val Volt        = Color(0xFFB8FF35)
private val VoltGlow    = Color(0xFF8FD620)
private val VoltDeep    = Color(0xFF4A7A0A)
private val VoltGhost   = Color(0x1AB8FF35)
private val VoltMist    = Color(0x08B8FF35)

private val SnowWhite   = Color(0xFFF8FAFC)
private val Mist        = Color(0xFFB0BDD0)
private val Fog         = Color(0xFF5A6880)
private val Ghost       = Color(0xFF2A3548)

private val AccentBlue  = Color(0xFF4D9FFF)
private val AccentGreen = Color(0xFF30D878)

private val GradVolt = Brush.linearGradient(listOf(Volt, VoltGlow))

// ─── Screen ─────────────────────────────────────────────────────────────────
@Composable
fun AuthChoiceScreen(
    onLoginClick: () -> Unit = {},
    onSignupClick: () -> Unit = {}
) {
    val pulse = rememberInfiniteTransition(label = "auth_choice_pulse")

    val orbAlpha by pulse.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_alpha"
    )

    val orb2Alpha by pulse.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        // ── Background atmosphere ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset((-60).dp, (-80).dp)
                .blur(90.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            Volt.copy(alpha = orbAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.BottomEnd)
                .offset(80.dp, 60.dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            VoltGlow.copy(alpha = orb2Alpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Subtle scanline texture like LoginScreen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val lineColor = Color(0x06FFFFFF)
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += 4f
                    }
                }
        )

        // ── Content ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Logo badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(SteelMid, SteelDark)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Volt.copy(alpha = 0.6f),
                                VoltGlow.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chologologo),
                    contentDescription = "CholoGO",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "WELCOME TO",
                fontSize = 13.sp,
                color = Fog,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "CholoGO",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SnowWhite,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GradVolt)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Smart, safe and affordable campus rides\nfor AUST students",
                fontSize = 13.sp,
                color = Fog,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            )

            Spacer(modifier = Modifier.height(34.dp))

            // ── Premium glass info card ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                SteelDark.copy(alpha = 0.90f),
                                Graphite.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                SteelLight.copy(alpha = 0.8f),
                                Ghost.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose your campus ride mode",
                        color = SnowWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sign in or create an account, then select whether you want to ride as a passenger or rider.",
                        color = Fog,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SteelMid.copy(alpha = 0.72f))
                            .border(
                                width = 1.dp,
                                color = Ghost.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuthFeatureItem(
                            icon = Icons.Default.Person,
                            title = "Passenger",
                            subtitle = "Book a seat",
                            tint = AccentBlue
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(52.dp)
                                .background(Ghost)
                        )

                        AuthFeatureItem(
                            icon = Icons.Default.DirectionsCar,
                            title = "Rider",
                            subtitle = "Offer a ride",
                            tint = Volt
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sign In button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GradVolt)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Volt.copy(alpha = 0.15f)
                                )
                            )
                        )
                    }
                    .clickable { onLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sign In",
                        color = Void,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.8.sp
                    )


                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Ghost,
                    thickness = 0.5.dp
                )

                Text(
                    text = "new here?",
                    color = Fog,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Medium
                )

                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Ghost,
                    thickness = 0.5.dp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Create Account button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SteelDark)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                SteelLight,
                                Ghost.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSignupClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account",
                    color = Mist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Your role can be selected after signup",
                color = Fog,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(52.dp))
        }
    }
}

// ─── Small feature item ──────────────────────────────────────────────────────
@Composable
private fun AuthFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = tint.copy(alpha = 0.25f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(21.dp)
            )
        }

        Text(
            text = title,
            color = SnowWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        Text(
            text = subtitle,
            color = Fog,
            fontSize = 11.sp
        )
    }
}