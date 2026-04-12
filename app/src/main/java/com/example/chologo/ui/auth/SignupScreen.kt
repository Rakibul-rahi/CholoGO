package com.example.chologo.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Design Tokens ───────────────────────────────────────────────────────────────

private val BgDeep       = Color(0xFF080C10)
private val CardBase     = Color(0xFF141A21)
private val CardElevated = Color(0xFF1A2130)

private val Lime         = Color(0xFF9FD63F)
private val LimeDeep     = Color(0xFF6FAF1A)
private val LimeDim      = Color(0xFF2A3E18)

private val AccentBlue   = Color(0xFF4D9FFF)
private val AccentRed    = Color(0xFFFF5461)

private val TextHigh     = Color(0xFFF0F4F8)
private val TextMed      = Color(0xFF8B9AB0)
private val TextLow      = Color(0xFF4A5568)

private val BorderSubtle = Color(0xFF1E2D3D)

private val GradientLime = Brush.linearGradient(listOf(Lime, LimeDeep))

// ─── Enums ───────────────────────────────────────────────────────────────────────

enum class UserRole { PASSENGER, RIDER }

// ─── Screen ──────────────────────────────────────────────────────────────────────

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
    var selectedRole     by rememberSaveable { mutableStateOf(UserRole.PASSENGER) }
    var name             by rememberSaveable { mutableStateOf("") }
    var email            by rememberSaveable { mutableStateOf("") }
    var phone            by rememberSaveable { mutableStateOf("") }
    var studentId        by rememberSaveable { mutableStateOf("") }
    var university       by rememberSaveable { mutableStateOf("AUST") }
    var homeLocation     by rememberSaveable { mutableStateOf("Mirpur") }
    var password         by rememberSaveable { mutableStateOf("") }
    var confirmPassword  by rememberSaveable { mutableStateOf("") }
    var errorText        by rememberSaveable { mutableStateOf("") }
    var passwordVisible  by rememberSaveable { mutableStateOf(false) }
    var confirmPwVisible by rememberSaveable { mutableStateOf(false) }

    val finalError = if (errorText.isNotEmpty()) errorText else externalErrorMessage

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = Lime,
        unfocusedBorderColor    = BorderSubtle,
        focusedTextColor        = TextHigh,
        unfocusedTextColor      = TextHigh,
        cursorColor             = Lime,
        focusedContainerColor   = CardElevated,
        unfocusedContainerColor = CardBase,
        disabledContainerColor  = CardBase,
        focusedLeadingIconColor   = Lime,
        unfocusedLeadingIconColor = TextLow,
        focusedLabelColor       = Lime,
        unfocusedLabelColor     = TextLow
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Subtle top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(LimeDim.copy(alpha = 0.3f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY / 2, 0f),
                        radius = 650f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // ── Header ───────────────────────────────────────────────────────────
            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextHigh,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join CholoGO campus ride sharing",
                fontSize = 14.sp,
                color = TextMed,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Role Selector ─────────────────────────────────────────────────────
            SectionLabel("I am a")
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCard(
                    label = "Passenger",
                    icon = Icons.Default.Person,
                    isSelected = selectedRole == UserRole.PASSENGER,
                    onClick = { selectedRole = UserRole.PASSENGER },
                    modifier = Modifier.weight(1f)
                )
                RoleCard(
                    label = "Rider",
                    icon = Icons.Default.DirectionsCar,
                    isSelected = selectedRole == UserRole.RIDER,
                    onClick = { selectedRole = UserRole.RIDER },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Personal Info ─────────────────────────────────────────────────────
            SectionLabel("Personal Info")
            Spacer(modifier = Modifier.height(12.dp))

            SignupField(
                value = name,
                onValueChange = { name = it; errorText = "" },
                placeholder = "Full name",
                icon = Icons.Default.Person,
                enabled = !isLoading,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            SignupField(
                value = email,
                onValueChange = { email = it; errorText = "" },
                placeholder = "Email address",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                enabled = !isLoading,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            SignupField(
                value = phone,
                onValueChange = { phone = it; errorText = "" },
                placeholder = "Phone number",
                icon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone,
                enabled = !isLoading,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(24.dp))

            // ── Academic Info ─────────────────────────────────────────────────────
            SectionLabel("Academic Info")
            Spacer(modifier = Modifier.height(12.dp))

            SignupField(
                value = studentId,
                onValueChange = { studentId = it; errorText = "" },
                placeholder = "Student ID",
                icon = Icons.Default.Badge,
                enabled = !isLoading,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            SignupField(
                value = university,
                onValueChange = { university = it; errorText = "" },
                placeholder = "University",
                icon = Icons.Default.School,
                enabled = !isLoading,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            SignupField(
                value = homeLocation,
                onValueChange = { homeLocation = it; errorText = "" },
                placeholder = "Home location",
                icon = Icons.Default.Home,
                enabled = !isLoading,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(24.dp))

            // ── Password ──────────────────────────────────────────────────────────
            SectionLabel("Security")
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorText = "" },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Password", color = TextLow, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextLow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                enabled = !isLoading,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorText = "" },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Confirm password", color = TextLow, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPwVisible = !confirmPwVisible }) {
                        Icon(
                            imageVector = if (confirmPwVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextLow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (confirmPwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                enabled = !isLoading,
                colors = fieldColors
            )

            // ── Error ─────────────────────────────────────────────────────────────
            if (!finalError.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A0E10))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = finalError,
                        color = AccentRed,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Sign Up Button ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = if (isLoading)
                            Brush.linearGradient(listOf(LimeDim, LimeDim))
                        else
                            GradientLime
                    )
                    .clickable(enabled = !isLoading) {
                        when {
                            name.isBlank() || email.isBlank() || phone.isBlank() ||
                                    studentId.isBlank() || university.isBlank() ||
                                    homeLocation.isBlank() || password.isBlank() ||
                                    confirmPassword.isBlank() ->
                                errorText = "Please fill in all fields"

                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                                errorText = "Please enter a valid email address"

                            phone.trim().length < 11 ->
                                errorText = "Please enter a valid phone number"

                            password.length < 6 ->
                                errorText = "Password must be at least 6 characters"

                            password != confirmPassword ->
                                errorText = "Passwords do not match"

                            else -> {
                                errorText = ""
                                onSignupClick(
                                    selectedRole,
                                    name.trim(), email.trim(), phone.trim(),
                                    studentId.trim(), university.trim(),
                                    homeLocation.trim(), password
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
                            color = Lime,
                            strokeWidth = 2.dp
                        )
                        Text("Creating account…", color = BgDeep, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Text(
                        text = "Create Account",
                        color = BgDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Login Link ────────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Already have an account? ", color = TextMed, fontSize = 14.sp)
                Text(
                    text = "Sign In",
                    color = Lime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !isLoading) { onLoginClick() }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ─── Role Card ───────────────────────────────────────────────────────────────────

@Composable
private fun RoleCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) LimeDim else CardBase,
        animationSpec = tween(200),
        label = "role_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Lime else BorderSubtle,
        animationSpec = tween(200),
        label = "role_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Lime else TextMed,
        animationSpec = tween(200),
        label = "role_content"
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color = bgColor)
            .clickable { onClick() }
            .then(
                Modifier.padding(1.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Border ring via outer box trick
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .background(color = borderColor.copy(alpha = if (isSelected) 0.35f else 0.15f))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

// ─── Generic Field ────────────────────────────────────────────────────────────────

@Composable
private fun SignupField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    enabled: Boolean,
    colors: androidx.compose.material3.TextFieldColors,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        placeholder = { Text(placeholder, color = TextLow, fontSize = 14.sp) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        enabled = enabled,
        colors = colors
    )
}

// ─── Section Label ────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextLow,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.fillMaxWidth()
    )
}