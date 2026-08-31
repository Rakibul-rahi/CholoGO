package com.example.chologo.ui.auth

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Palette matching LoginScreen / AuthChoiceScreen / SignupScreen ──────────
private val Obsidian    = Color(0xFF090D12)
private val Graphite    = Color(0xFF0F141C)
private val SteelDark   = Color(0xFF141B26)
private val SteelMid    = Color(0xFF1C2535)
private val SteelLight  = Color(0xFF232E42)

private val Volt        = Color(0xFFB8FF35)
private val VoltGlow    = Color(0xFF8FD620)
private val VoltDeep    = Color(0xFF4A7A0A)

private val SnowWhite   = Color(0xFFF8FAFC)
private val Mist        = Color(0xFFB0BDD0)
private val Fog         = Color(0xFF5A6880)
private val Ghost       = Color(0xFF2A3548)

private val ErrorRed    = Color(0xFFFF4D6A)
private val ErrorTint   = Color(0x1AFF4D6A)

private val GradVolt = Brush.linearGradient(listOf(Volt, VoltGlow))
private val GradVoltDisabled = Brush.linearGradient(listOf(VoltDeep, VoltDeep))

@Composable
fun RoleSelectionScreen(
    onCompleteProfile: (role: UserRole, phone: String) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
    externalErrorMessage: String? = null
) {
    var selectedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var phone by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf("") }

    val finalError = if (localError.isNotEmpty()) localError else externalErrorMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Text(
                text = "ALMOST THERE",
                fontSize = 13.sp,
                color = Fog,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Complete Your Profile",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SnowWhite,
                letterSpacing = 0.4.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "One quick step - tell us how you'll ride\nand where we can reach you.",
                fontSize = 13.sp,
                color = Fog,
                letterSpacing = 0.1.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            RoleOptionCard(
                label = "Passenger",
                description = "Book rides and travel easily around campus",
                icon = Icons.Default.Person,
                isSelected = selectedRole == UserRole.PASSENGER,
                enabled = !isLoading,
                onClick = {
                    selectedRole = UserRole.PASSENGER
                    localError = ""
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            RoleOptionCard(
                label = "Rider",
                description = "Offer rides and earn while helping students",
                icon = Icons.Default.DirectionsCar,
                isSelected = selectedRole == UserRole.RIDER,
                enabled = !isLoading,
                onClick = {
                    selectedRole = UserRole.RIDER
                    localError = ""
                }
            )

            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    localError = ""
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(text = "Phone number", color = Ghost, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Fog,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = TextStyle(color = SnowWhite, fontSize = 14.sp),
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
                    unfocusedLeadingIconColor = Fog
                )
            )

            if (!finalError.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorTint)
                        .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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

            Spacer(modifier = Modifier.height(26.dp))

            val canSubmit = selectedRole != null && isValidSignupPhone(phone)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(brush = if (canSubmit && !isLoading) GradVolt else GradVoltDisabled)
                    .clickable(enabled = canSubmit && !isLoading) {
                        val role = selectedRole
                        when {
                            role == null -> localError = "Please choose Passenger or Rider"
                            !isValidSignupPhone(phone) -> localError = "Please enter a valid phone number"
                            else -> onCompleteProfile(role, phone.trim())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Obsidian,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        color = Obsidian,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RoleOptionCard(
    label: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        (if (isSelected) Volt.copy(alpha = 0.10f) else SteelDark).copy(alpha = 0.90f),
                        Graphite.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Volt.copy(alpha = 0.85f) else SteelLight.copy(alpha = 0.6f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background((if (isSelected) Volt else Mist).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Volt else Mist,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = label,
                color = SnowWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = Fog,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
