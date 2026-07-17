package com.example.chologo.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var reason by remember {
        mutableStateOf("Unsafe riding")
    }

    var details by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Report Rider")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                val reasons = listOf(
                    "Unsafe riding",
                    "Did not show up",
                    "Bad behaviour",
                    "Wrong destination",
                    "Other"
                )

                reasons.forEach { item ->
                    Row {
                        RadioButton(
                            selected = reason == item,
                            onClick = {
                                reason = item
                            }
                        )

                        Text(item)
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = {
                        details = it
                    },
                    label = {
                        Text("Details")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(reason, details)
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}