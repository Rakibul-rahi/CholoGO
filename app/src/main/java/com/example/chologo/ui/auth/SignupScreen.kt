package com.example.chologo.ui.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkCharcoal = Color(0xFF222831)
private val LimeGreen = Color(0xFF9BCB2D)
private val SoftWhite = Color(0xFFF5F5F5)

enum class UserRole {
    PASSENGER, RIDER
}

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
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Join ChoLoGO Campus Ride Sharing",
                    fontSize = 14.sp,
                    color = DarkCharcoal.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Choose Role",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkCharcoal
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoleButton(
                        text = "Passenger",
                        isSelected = selectedRole == UserRole.PASSENGER,
                        onClick = { selectedRole = UserRole.PASSENGER },
                        modifier = Modifier.weight(1f)
                    )

                    RoleButton(
                        text = "Rider",
                        isSelected = selectedRole == UserRole.RIDER,
                        onClick = { selectedRole = UserRole.RIDER },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = ""
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorText = ""
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorText = ""
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = studentId,
                    onValueChange = {
                        studentId = it
                        errorText = ""
                    },
                    label = { Text("Student ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = university,
                    onValueChange = {
                        university = it
                        errorText = ""
                    },
                    label = { Text("University") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = homeLocation,
                    onValueChange = {
                        homeLocation = it
                        errorText = ""
                    },
                    label = { Text("Home Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorText = ""
                    },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            )
                        }
                    },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorText = ""
                    },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                        ) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (confirmPasswordVisible) {
                                    "Hide confirm password"
                                } else {
                                    "Show confirm password"
                                }
                            )
                        }
                    },
                    enabled = !isLoading
                )

                val finalErrorMessage =
                    if (errorText.isNotEmpty()) errorText else externalErrorMessage

                if (!finalErrorMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = finalErrorMessage,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        when {
                            name.isBlank() ||
                                    email.isBlank() ||
                                    phone.isBlank() ||
                                    studentId.isBlank() ||
                                    university.isBlank() ||
                                    homeLocation.isBlank() ||
                                    password.isBlank() ||
                                    confirmPassword.isBlank() -> {
                                errorText = "Please fill all fields"
                            }

                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeGreen,
                        contentColor = Color.White
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Sign Up",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = onLoginClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Already have an account? Login",
                        color = DarkCharcoal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RoleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) LimeGreen else Color(0xFFF1F1F1),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else DarkCharcoal,
            fontWeight = FontWeight.SemiBold
        )
    }
}