package com.example.chologo.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DialogSurface = Color(0xFF161B20)
private val TextHigh = Color(0xFFF1F5F9)
private val TextMed = Color(0xFF8B96A5)
private val Lime = Color(0xFFC6F135)
private val BgDeep = Color(0xFF0A0D0F)
private val FieldBorder = Color(0xFF2A3548)

@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text("Rate Your Rider", color = TextHigh, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "How was the ride? Your rating helps other students pick a good rider.",
                    color = TextMed,
                    fontSize = 13.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { index ->
                        val filled = index < rating
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "${index + 1} star${if (index == 0) "" else "s"}",
                                tint = if (filled) Lime else TextMed,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Text(
                    text = ratingLabel(rating),
                    color = Lime,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Add a comment (optional)") },
                    placeholder = { Text("e.g. On time, friendly, safe riding") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextHigh,
                        unfocusedTextColor = TextHigh,
                        focusedBorderColor = Lime,
                        unfocusedBorderColor = FieldBorder,
                        focusedLabelColor = Lime,
                        unfocusedLabelColor = TextMed,
                        cursorColor = Lime,
                        focusedPlaceholderColor = TextMed,
                        unfocusedPlaceholderColor = TextMed
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, comment) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Lime,
                    contentColor = BgDeep
                )
            ) {
                Text("Submit Rating", fontWeight = FontWeight.Bold)
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

private fun ratingLabel(rating: Int): String = when (rating) {
    1 -> "1 star — Poor"
    2 -> "2 stars — Below average"
    3 -> "3 stars — Okay"
    4 -> "4 stars — Good"
    else -> "5 stars — Excellent"
}
