package com.example.chologo.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

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
        title = {
            Text(title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("This will free up the spot for someone else. What's the reason?")

                reasons.forEach { item ->
                    Row {
                        RadioButton(
                            selected = reason == item,
                            onClick = { reason = item }
                        )
                        Text(item)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) }
            ) {
                Text("Confirm Cancel")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Keep Ride")
            }
        }
    )
}