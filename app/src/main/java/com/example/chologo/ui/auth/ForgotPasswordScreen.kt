package com.example.chologo.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

// 🎨 Colors (same theme as your LoginScreen)
private val BgDeep = Color(0xFF080C10)
private val CardBase = Color(0xFF141A21)
private val CardElevated = Color(0xFF1A2130)
private val Lime = Color(0xFF9FD63F)
private val LimeDeep = Color(0xFF6FAF1A)
private val TextHigh = Color(0xFFF0F4F8)
private val TextMed = Color(0xFF8B9AB0)
private val TextLow = Color(0xFF4A5568)

private val GradientLime = Brush.linearGradient(listOf(Lime, LimeDeep))

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // 🔙 Back Button
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onBackClick() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextHigh
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Forgot Password",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextHigh
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter your email and we will send you a reset link",
                fontSize = 14.sp,
                color = TextMed
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 📧 Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text("you@aust.edu", color = TextLow)
                },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lime,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = TextHigh,
                    unfocusedTextColor = TextHigh,
                    focusedContainerColor = CardElevated,
                    unfocusedContainerColor = CardBase
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 🔘 Send Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(GradientLime, RoundedCornerShape(16.dp))
                    .clickable(enabled = !isLoading) {

                        if (email.isEmpty()) {
                            Toast.makeText(context, "Enter email first", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }

                        isLoading = true

                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Reset link sent to your email",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        task.exception?.message ?: "Error occurred",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = BgDeep, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Send Reset Link",
                        color = BgDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}