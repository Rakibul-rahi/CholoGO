package com.example.chologo.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.ui.theme.LocalIsDarkTheme

private val DialogSurface: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF161B20) else Color(0xFFFFFFFF)
private val TextHigh: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF1F5F9) else Color(0xFF10151B)
private val TextMed: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8B96A5) else Color(0xFF4B5563)
private val AccentAmber: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFBBF24) else Color(0xFFA6720A)

@Composable
fun CancelRideDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("Emergency") }

    val reasons = listOf(
        "Emergency",
        "Schedule changed",
        "Found another ride",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(title, color = TextHigh, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "This will free up the spot for someone else. What's the reason?",
                    color = TextMed,
                    fontSize = 13.sp
                )

                Column {
                    reasons.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = reason == item,
                                onClick = { reason = item },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentAmber,
                                    unselectedColor = TextMed
                                )
                            )
                            Text(item, color = TextHigh, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentAmber,
                    contentColor = if (LocalIsDarkTheme.current) Color(0xFF0A0D0F) else Color.White
                )
            ) {
                Text("Confirm Cancel", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMed)
            ) {
                Text("Keep Ride")
            }
        }
    )
}
