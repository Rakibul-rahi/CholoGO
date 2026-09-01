package com.example.chologo.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.VehicleType

// Same auth palette LoginScreen / SignupScreen / RoleSelectionScreen use.
private val SteelDark  = Color(0xFF141B26)
private val SteelMid   = Color(0xFF1C2535)
private val SteelLight = Color(0xFF232E42)

private val Volt       = Color(0xFFB8FF35)

private val SnowWhite  = Color(0xFFF8FAFC)
private val Mist       = Color(0xFFB0BDD0)
private val Fog        = Color(0xFF5A6880)
private val Ghost      = Color(0xFF2A3548)

/**
 * The vehicle half of a rider's profile, shared by the email signup form and
 * the Google post-sign-in profile step so both collect exactly the same
 * thing.
 *
 * Car details are optional on purpose - a rider shouldn't be blocked from
 * signing up because they don't have their plate number to hand - but they
 * are what a passenger looks for at the kerb, so the copy nudges toward
 * filling them in. Only shown at all once "Car" is picked.
 */
@Composable
fun VehicleSelectionSection(
    vehicleType: String,
    onVehicleTypeChange: (String) -> Unit,
    vehicleModel: String,
    onVehicleModelChange: (String) -> Unit,
    vehicleNumber: String,
    onVehicleNumberChange: (String) -> Unit,
    vehicleColor: String,
    onVehicleColorChange: (String) -> Unit,
    enabled: Boolean = true
) {
    val isCar = VehicleType.isCar(vehicleType)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VehicleOptionCard(
                label = "Bike",
                caption = "1 seat",
                icon = Icons.Default.TwoWheeler,
                isSelected = !isCar,
                enabled = enabled,
                onClick = { onVehicleTypeChange(VehicleType.BIKE) },
                modifier = Modifier.weight(1f)
            )

            VehicleOptionCard(
                label = "Car",
                caption = "Up to ${VehicleType.MAX_CAR_SEATS} seats",
                icon = Icons.Default.DirectionsCar,
                isSelected = isCar,
                enabled = enabled,
                onClick = { onVehicleTypeChange(VehicleType.CAR) },
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(visible = isCar) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Car details help your passengers spot you. Optional, " +
                            "and you can add them later from your profile.",
                    color = Fog,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                VehicleDetailField(
                    value = vehicleModel,
                    onValueChange = onVehicleModelChange,
                    placeholder = "Model (e.g. Toyota Axio)",
                    icon = Icons.Default.DirectionsCar,
                    enabled = enabled
                )

                Spacer(modifier = Modifier.height(10.dp))

                VehicleDetailField(
                    value = vehicleNumber,
                    onValueChange = onVehicleNumberChange,
                    placeholder = "Plate number",
                    icon = Icons.Default.Numbers,
                    enabled = enabled,
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(10.dp))

                VehicleDetailField(
                    value = vehicleColor,
                    onValueChange = onVehicleColorChange,
                    placeholder = "Colour",
                    icon = Icons.Default.Palette,
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun VehicleOptionCard(
    label: String,
    caption: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Volt.copy(alpha = 0.14f) else SteelMid,
        animationSpec = tween(200),
        label = "vehicle_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Volt.copy(alpha = 0.85f) else Ghost,
        animationSpec = tween(200),
        label = "vehicle_border"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Volt else Mist,
        animationSpec = tween(200),
        label = "vehicle_content"
    )

    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                color = if (isSelected) SnowWhite else Mist,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = caption,
                color = Fog,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun VehicleDetailField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        placeholder = {
            Text(text = placeholder, color = Ghost, fontSize = 14.sp)
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Fog,
                modifier = Modifier.size(18.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
}

/**
 * Label + card shell around the picker, matching the "I am a" section on the
 * signup form. Kept separate so RoleSelectionScreen, which lays its options
 * out as full-width rows rather than glass cards, can use the picker without
 * this chrome.
 */
@Composable
fun VehicleSelectionCard(
    vehicleType: String,
    onVehicleTypeChange: (String) -> Unit,
    vehicleModel: String,
    onVehicleModelChange: (String) -> Unit,
    vehicleNumber: String,
    onVehicleNumberChange: (String) -> Unit,
    vehicleColor: String,
    onVehicleColorChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SteelDark.copy(alpha = 0.90f))
            .border(1.dp, SteelLight.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {
        Column {
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
                    text = "I RIDE A",
                    color = Mist,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            VehicleSelectionSection(
                vehicleType = vehicleType,
                onVehicleTypeChange = onVehicleTypeChange,
                vehicleModel = vehicleModel,
                onVehicleModelChange = onVehicleModelChange,
                vehicleNumber = vehicleNumber,
                onVehicleNumberChange = onVehicleNumberChange,
                vehicleColor = vehicleColor,
                onVehicleColorChange = onVehicleColorChange,
                enabled = enabled
            )
        }
    }
}
