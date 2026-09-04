package com.example.chologo.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.ui.theme.LocalIsDarkTheme

private val DialogSurface: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF161B20) else Color(0xFFFFFFFF)
private val TextHigh: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF1F5F9) else Color(0xFF10151B)
private val TextMed: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8B96A5) else Color(0xFF4B5563)
private val AccentRed: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFF4D6A) else Color(0xFFC81E3A)
private val FieldBorder: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A3548) else Color(0xFFD3D9E0)

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var reason by remember { mutableStateOf("Unsafe riding") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text("Report a Problem", color = TextHigh, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Tell us what went wrong. Reports are reviewed and don't notify the rider directly.",
                    color = TextMed,
                    fontSize = 13.sp
                )

                val reasons = listOf(
                    "Unsafe riding",
                    "Did not show up",
                    "Bad behaviour",
                    "Wrong destination",
                    "Other"
                )

                Column {
                    reasons.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = reason == item,
                                onClick = { reason = item },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentRed,
                                    unselectedColor = TextMed
                                )
                            )
                            Text(item, color = TextHigh, fontSize = 14.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("What happened? (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextHigh,
                        unfocusedTextColor = TextHigh,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = FieldBorder,
                        focusedLabelColor = AccentRed,
                        unfocusedLabelColor = TextMed,
                        cursorColor = AccentRed
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason, details) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                )
            ) {
                Text("Submit Report", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMed)
            ) {
                Text("Cancel")
            }
        }
    )
}
