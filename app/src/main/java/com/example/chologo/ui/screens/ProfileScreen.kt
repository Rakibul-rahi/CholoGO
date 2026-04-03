package com.example.chologo.ui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chologo.model.User
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth

private val BgDark = Color(0xFF0D1117)
private val CardDark = Color(0xFF161B22)
private val GreenPrimary = Color(0xFF8DC63F)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val DangerRed = Color(0xFFE53935)
private val HeaderButtonBg = Color(0xFF111827)

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val repository = remember { UserRepository() }

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getCurrentUserData { result ->
            result.onSuccess { fetchedUser ->
                user = fetchedUser
                isLoading = false
            }.onFailure { e ->
                errorMessage = e.message ?: "Failed to load profile"
                isLoading = false
            }
        }
    }

    val userName = user?.name ?: "User"
    val userEmail = user?.email ?: auth.currentUser?.email ?: "No email found"
    val phone = user?.phone ?: "Not provided"
    val role = user?.role?.replaceFirstChar { it.uppercase() } ?: "Not set"
    val studentId = user?.studentId ?: "Not provided"
    val university = user?.university ?: "Not provided"
    val homeLocation = user?.homeLocation ?: "Not provided"

    fun goBackSafely() {
        val popped = navController.popBackStack()
        if (!popped) {
            val prefs = navController.context.getSharedPreferences("chologo_prefs", Context.MODE_PRIVATE)
            val savedRole = prefs.getString("user_role", null)

            val fallbackRoute = when (savedRole?.lowercase()) {
                "rider" -> Screen.RiderHome.route
                else -> Screen.PassengerHome.route
            }

            navController.navigate(fallbackRoute) {
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ProfileHeader(
            title = "My Profile",
            onBackClick = { goBackSafely() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Icon",
                        tint = BgDark,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = GreenPrimary)
                } else {
                    Text(
                        text = userName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = role,
                        color = GreenPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Account Information",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage ?: "Unknown error",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            ProfileInfoCard(
                icon = { Icon(Icons.Default.Email, contentDescription = null, tint = GreenPrimary) },
                label = "Email",
                value = userEmail
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileInfoCard(
                icon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GreenPrimary) },
                label = "Phone",
                value = phone
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileInfoCard(
                icon = { Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary) },
                label = "Role",
                value = role
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileInfoCard(
                icon = { Icon(Icons.Default.School, contentDescription = null, tint = GreenPrimary) },
                label = "Student ID",
                value = studentId
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileInfoCard(
                icon = { Icon(Icons.Default.School, contentDescription = null, tint = GreenPrimary) },
                label = "University",
                value = university
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileInfoCard(
                icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary) },
                label = "Home Location",
                value = homeLocation
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                auth.signOut()

                val prefs = navController.context.getSharedPreferences("chologo_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("user_role").apply()

                navController.navigate(Screen.AuthChoice.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = HeaderButtonBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
fun ProfileInfoCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = label,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}