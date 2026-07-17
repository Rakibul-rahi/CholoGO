package com.example.chologo.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Palette matching LoginScreen / AuthChoiceScreen ─────────────────────────
private val Void        = Color(0xFF050709)
private val Obsidian    = Color(0xFF090D12)
private val Graphite    = Color(0xFF0F141C)
private val SteelDark   = Color(0xFF141B26)
private val SteelMid    = Color(0xFF1C2535)
private val SteelLight  = Color(0xFF232E42)

private val Volt        = Color(0xFFB8FF35)
private val VoltGlow    = Color(0xFF8FD620)
private val VoltDeep    = Color(0xFF4A7A0A)
private val VoltMist    = Color(0x08B8FF35)

private val SnowWhite   = Color(0xFFF8FAFC)
private val Mist        = Color(0xFFB0BDD0)
private val Fog         = Color(0xFF5A6880)
private val Ghost       = Color(0xFF2A3548)

private val ErrorRed    = Color(0xFFFF4D6A)
private val ErrorTint   = Color(0x1AFF4D6A)

private val GradVolt = Brush.linearGradient(listOf(Volt, VoltGlow))
private val GradVoltDisabled = Brush.linearGradient(listOf(VoltDeep, VoltDeep))

// ─── Enums ───────────────────────────────────────────────────────────────────
enum class UserRole {
    PASSENGER,
    RIDER
}

// ─── Screen ──────────────────────────────────────────────────────────────────
@Composable
fun SignupScreen(
    onSignupClick: (
        role: UserRole,
        name: String,
        email: String,
        phone: String,
        studentId: String,
        university: String,
        homeLocation: String,
        password: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onLoginClick: () -> Unit = {},
    isLoading: Boolean = false,
    externalErrorMessage: String? = null
) {
    val universityOptions = listOf(
        "AUST",
        "NSU",
        "BRAC University",
        "IUB",
        "AIUB",
        "EWU",
        "UIU",
        "ULAB",
        "SEU",
        "DIU",
        "BUBT",
        "Stamford University"
    )

    var selectedRole by rememberSaveable { mutableStateOf(UserRole.PASSENGER) }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var studentId by rememberSaveable { mutableStateOf("") }
    var university by rememberSaveable { mutableStateOf("AUST") }
    var homeLocation by rememberSaveable { mutableStateOf("Mirpur") }

    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var errorText by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPwVisible by rememberSaveable { mutableStateOf(false) }

    val finalError = if (errorText.isNotEmpty()) errorText else externalErrorMessage

    val pulse = rememberInfiniteTransition(label = "signup_pulse")

    val orbAlpha by pulse.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "orb_alpha"
    )

    val orb2Alpha by pulse.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            tween(4400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(58.dp))

            Text(
                text = "JOIN CHOLOGO",
                fontSize = 13.sp,
                color = Fog,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Create Account",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SnowWhite,
                letterSpacing = 0.4.sp
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
                text = "Start your smart campus ride journey\nwith a verified student account",
                fontSize = 13.sp,
                color = Fog,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            )

            Spacer(modifier = Modifier.height(34.dp))

            // ── Role Selector ──────────────────────────────────────────────
            PremiumGlassCard {
                PremiumSectionLabel("I am a")

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumRoleCard(
                        label = "Passenger",
                        icon = Icons.Default.Person,
                        isSelected = selectedRole == UserRole.PASSENGER,
                        onClick = { selectedRole = UserRole.PASSENGER },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )

                    PremiumRoleCard(
                        label = "Rider",
                        icon = Icons.Default.DirectionsCar,
                        isSelected = selectedRole == UserRole.RIDER,
                        onClick = { selectedRole = UserRole.RIDER },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Personal Info ──────────────────────────────────────────────
            PremiumGlassCard {
                PremiumSectionLabel("Personal Info")

                Spacer(modifier = Modifier.height(14.dp))

                PremiumSignupField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = ""
                    },
                    placeholder = "Full name",
                    icon = Icons.Default.Person,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                PremiumSignupField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorText = ""
                    },
                    placeholder = "Email address",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                PremiumSignupField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorText = ""
                    },
                    placeholder = "Phone number",
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Academic Info ──────────────────────────────────────────────
            PremiumGlassCard {
                PremiumSectionLabel("Academic Info")

                Spacer(modifier = Modifier.height(14.dp))

                PremiumSignupField(
                    value = studentId,
                    onValueChange = {
                        studentId = it
                        errorText = ""
                    },
                    placeholder = "Student ID",
                    icon = Icons.Default.Badge,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                PremiumDropdownField(
                    value = university,
                    placeholder = "University",
                    icon = Icons.Default.School,
                    options = universityOptions,
                    enabled = !isLoading,
                    onOptionSelected = {
                        university = it
                        errorText = ""
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                PremiumSignupField(
                    value = homeLocation,
                    onValueChange = {
                        homeLocation = it
                        errorText = ""
                    },
                    placeholder = "Home location",
                    icon = Icons.Default.Home,
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Security ───────────────────────────────────────────────────
            PremiumGlassCard {
                PremiumSectionLabel("Security")

                Spacer(modifier = Modifier.height(14.dp))

                PremiumSignupField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorText = ""
                    },
                    placeholder = "Password",
                    icon = Icons.Default.Lock,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = null,
                                tint = Fog,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                PremiumSignupField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorText = ""
                    },
                    placeholder = "Confirm password",
                    icon = Icons.Default.Lock,
                    visualTransformation = if (confirmPwVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { confirmPwVisible = !confirmPwVisible },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (confirmPwVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = null,
                                tint = Fog,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    enabled = !isLoading
                )
            }

            AnimatedVisibility(
                visible = !finalError.isNullOrEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorTint)
                            .border(
                                width = 1.dp,
                                color = ErrorRed.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
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

            Spacer(modifier = Modifier.height(26.dp))

            // ── Create Account Button ──────────────────────────────────────
            val btnScale by animateFloatAsState(
                targetValue = if (isLoading) 0.97f else 1f,
                animationSpec = spring(),
                label = "signup_btn_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = if (isLoading) GradVoltDisabled else GradVolt
                    )
                    .drawBehind {
                        if (!isLoading) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Volt.copy(alpha = 0.15f)
                                    )
                                )
                            )
                        }
                    }
                    .clickable(enabled = !isLoading) {
                        when {
                            name.isBlank() ||
                                    email.isBlank() ||
                                    phone.isBlank() ||
                                    studentId.isBlank() ||
                                    university.isBlank() ||
                                    homeLocation.isBlank() ||
                                    password.isBlank() ||
                                    confirmPassword.isBlank() -> {
                                errorText = "Please fill in all fields"
                            }

                            !android.util.Patterns.EMAIL_ADDRESS
                                .matcher(email.trim())
                                .matches() -> {
                                errorText = "Please enter a valid email address"
                            }

                            phone.trim().length < 11 -> {
                                errorText = "Please enter a valid phone number"
                            }

                            password.length < 6 -> {
                                errorText = "Password must be at least 6 characters"
                            }

                            password != confirmPassword -> {
                                errorText = "Passwords do not match"
                            }

                            else -> {
                                errorText = ""

                                onSignupClick(
                                    selectedRole,
                                    name.trim(),
                                    email.trim(),
                                    phone.trim(),
                                    studentId.trim(),
                                    university.trim(),
                                    homeLocation.trim(),
                                    password
                                )
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
                            text = "Creating account…",
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
                            text = "Create Account",
                            color = Void,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.8.sp
                        )


                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

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
                    text = "already joined?",
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

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
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
                    .clickable(enabled = !isLoading) {
                        onLoginClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    color = Mist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(modifier = Modifier.height(52.dp))
        }
    }
}

// ─── Glass Card ──────────────────────────────────────────────────────────────
@Composable
private fun PremiumGlassCard(
    content: @Composable ColumnScope.() -> Unit
) {
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
        Column(content = content)
    }
}

// ─── Role Card ───────────────────────────────────────────────────────────────
@Composable
private fun PremiumRoleCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Volt.copy(alpha = 0.14f) else SteelMid,
        animationSpec = tween(200),
        label = "role_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Volt.copy(alpha = 0.85f) else Ghost,
        animationSpec = tween(200),
        label = "role_border"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Volt else Mist,
        animationSpec = tween(200),
        label = "role_content"
    )

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = if (isSelected) {
                        Brush.radialGradient(
                            listOf(
                                Volt.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    }
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = label,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// ─── Premium Text Field ──────────────────────────────────────────────────────
@Composable
private fun PremiumSignupField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
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
            Text(
                text = placeholder,
                color = Ghost,
                fontSize = 14.sp,
                letterSpacing = 0.2.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Volt else Fog,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
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
            unfocusedTrailingIconColor = Fog
        )
    )
}

// ─── Premium Dropdown Field ──────────────────────────────────────────────────
@Composable
private fun PremiumDropdownField(
    value: String,
    placeholder: String,
    icon: ImageVector,
    options: List<String>,
    enabled: Boolean,
    onOptionSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    expanded = true
                },
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Ghost,
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Fog,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (enabled) expanded = true
                    },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Fog,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
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
                focusedContainerColor = SteelMid,
                unfocusedContainerColor = SteelMid,
                disabledContainerColor = SteelDark,
                focusedLeadingIconColor = Volt,
                unfocusedLeadingIconColor = Fog,
                focusedTrailingIconColor = Mist,
                unfocusedTrailingIconColor = Fog
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(SteelDark)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == value) Volt else SnowWhite,
                            fontSize = 14.sp,
                            fontWeight = if (option == value) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─── Section Label ───────────────────────────────────────────────────────────
@Composable
private fun PremiumSectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(Volt)
        )

        Text(
            text = text.uppercase(),
            color = Mist,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}