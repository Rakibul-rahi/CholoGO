package com.example.chologo.ui.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.R
import kotlin.math.sin

// ─── Palette ────────────────────────────────────────────────────────────────
private val Void        = Color(0xFF050709)
private val Obsidian    = Color(0xFF090D12)
private val Graphite    = Color(0xFF0F141C)
private val SteelDark   = Color(0xFF141B26)
private val SteelMid    = Color(0xFF1C2535)
private val SteelLight  = Color(0xFF232E42)

// Accent – electric lime family
private val Volt        = Color(0xFFB8FF35)   // pure electric
private val VoltGlow    = Color(0xFF8FD620)   // mid
private val VoltDeep    = Color(0xFF4A7A0A)   // deep
private val VoltGhost   = Color(0x1AB8FF35)   // 10 % tint fill
private val VoltMist    = Color(0x08B8FF35)   // 5 % atmospheric

// Text
private val SnowWhite   = Color(0xFFF8FAFC)
private val Mist        = Color(0xFFB0BDD0)
private val Fog         = Color(0xFF5A6880)
private val Ghost       = Color(0xFF2A3548)

// State
private val ErrorRed    = Color(0xFFFF4D6A)
private val ErrorTint   = Color(0x1AFF4D6A)

// Gradients
private val GradVolt    = Brush.linearGradient(listOf(Volt, VoltGlow))
private val GradVoltDisabled = Brush.linearGradient(listOf(VoltDeep, VoltDeep))


// ─── Screen ─────────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onGoogleSignInClick: () -> Unit = {},
    onSignupClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    isLoading: Boolean = false,
    externalErrorMessage: String? = null
) {
    var email    by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorText by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val finalError = if (errorText.isNotEmpty()) errorText else externalErrorMessage

    // Ambient orb pulse
    val pulse = rememberInfiniteTransition(label = "pulse")
    val orbAlpha by pulse.animateFloat(
        initialValue = 0.18f, targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "orb"
    )
    val orb2Alpha by pulse.animateFloat(
        initialValue = 0.08f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            tween(4400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "orb2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {

        // ── Background atmosphere ──────────────────────────────────────────
        // Primary glow – top-left
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset((-60).dp, (-80).dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Volt.copy(alpha = orbAlpha), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Secondary glow – bottom-right
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.BottomEnd)
                .offset(80.dp, 60.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(VoltGlow.copy(alpha = orb2Alpha), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // Subtle noise grain overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Lightweight scanline texture
                    val lineColor = Color(0x06FFFFFF)
                    var y = 0f
                    while (y < size.height) {
                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1f)
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

            Spacer(Modifier.height(64.dp))

            // Logo badge
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SteelMid, SteelDark)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Volt.copy(alpha = 0.6f), VoltGlow.copy(alpha = 0.2f))
                        ),
                        shape = RoundedCornerShape(28.dp)
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

            Spacer(Modifier.height(36.dp))

            // Heading
            Text(
                text = "Welcome back,",
                fontSize = 15.sp,
                color = Fog,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "AUSTIAN",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SnowWhite,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(6.dp))
            // Volt accent underline
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GradVolt)
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sign in to continue your campus ride",
                fontSize = 13.sp,
                color = Fog,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                letterSpacing = 0.1.sp
            )

            Spacer(Modifier.height(44.dp))

            // ── Glass card container ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SteelDark.copy(alpha = 0.90f), Graphite.copy(alpha = 0.95f))
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
                    .padding(24.dp)
            ) {
                Column {

                    // Email field
                    PremiumFieldLabel("Email address")
                    Spacer(Modifier.height(8.dp))
                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it; errorText = "" },
                        placeholder = "you@aust.edu",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email, null,
                                tint = Fog, modifier = Modifier.size(17.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isLoading
                    )

                    Spacer(Modifier.height(20.dp))

                    // Password field
                    PremiumFieldLabel("Password")
                    Spacer(Modifier.height(8.dp))
                    PremiumTextField(
                        value = password,
                        onValueChange = { password = it; errorText = "" },
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock, null,
                                tint = Fog, modifier = Modifier.size(17.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Fog,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        enabled = !isLoading
                    )

                    Spacer(Modifier.height(14.dp))

                    // Forgot password
                    Text(
                        text = "Forgot password?",
                        color = Volt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable(enabled = !isLoading) { onForgotPasswordClick() }
                    )

                    // Error
                    AnimatedVisibility(
                        visible = !finalError.isNullOrEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit  = fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ErrorTint)
                                    .border(
                                        1.dp,
                                        ErrorRed.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ErrorRed)
                                )
                                Text(
                                    text = finalError ?: "",
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }
            // ── End glass card ─────────────────────────────────────────────

            Spacer(Modifier.height(24.dp))

            // ── Sign In button ─────────────────────────────────────────────
            val btnScale by animateFloatAsState(
                targetValue = if (isLoading) 0.97f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "btnScale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .graphicsLayer { scaleX = btnScale; scaleY = btnScale }
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isLoading) GradVoltDisabled else GradVolt)
                    // outer glow shadow
                    .drawBehind {
                        if (!isLoading) {
                            drawRect(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Volt.copy(alpha = 0.15f))
                                )
                            )
                        }
                    }
                    .clickable(enabled = !isLoading) {
                        when {
                            email.isBlank() || password.isBlank() ->
                                errorText = "Please enter email and password"
                            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                                errorText = "Please enter a valid email address"
                            password.length < 6 ->
                                errorText = "Password must be at least 6 characters"
                            else -> {
                                errorText = ""
                                onLoginClick(email.trim(), password)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Void,
                            strokeWidth = 2.dp,
                            trackColor = VoltDeep
                        )
                        Text(
                            "Signing in…",
                            color = Void,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.4.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Login",
                            color = Void,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.8.sp
                        )

                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Divider
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
                    "or continue with",
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

            Spacer(Modifier.height(20.dp))

            // ── Google button ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SteelDark)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(SteelLight, Ghost.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = !isLoading) {
                        errorText = ""
                        onGoogleSignInClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google "G" styled chip
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1F2E))
                            .border(
                                0.5.dp,
                                Color(0xFF4285F4).copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "Continue with Google",
                        color = Mist,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Sign up link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "New to CholoGO? ",
                    color = Fog,
                    fontSize = 13.sp
                )
                Text(
                    "Create account",
                    color = Volt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.clickable(enabled = !isLoading) { onSignupClick() }
                )
            }

            Spacer(Modifier.height(52.dp))
        }
    }
}


// ─── Reusable premium text field ─────────────────────────────────────────────
@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        placeholder = {
            Text(placeholder, color = Ghost, fontSize = 14.sp, letterSpacing = 0.2.sp)
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        interactionSource = interactionSource,
        textStyle = LocalTextStyle.current.copy(
            color = SnowWhite,
            fontSize = 14.sp,
            letterSpacing = 0.2.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Volt.copy(alpha = 0.85f),
            unfocusedBorderColor = Ghost,
            focusedTextColor = SnowWhite,
            unfocusedTextColor = SnowWhite,
            cursorColor = Volt,
            focusedContainerColor = if (isFocused) VoltMist else SteelMid,
            unfocusedContainerColor = SteelMid,
            disabledContainerColor = SteelDark,
            focusedLeadingIconColor = Volt,
            unfocusedLeadingIconColor = Fog,
            focusedTrailingIconColor = Mist,
            unfocusedTrailingIconColor = Fog,
        )
    )
}


// ─── Field label ─────────────────────────────────────────────────────────────
@Composable
private fun PremiumFieldLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(Volt)
        )
        Text(
            text = text,
            color = Mist,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}