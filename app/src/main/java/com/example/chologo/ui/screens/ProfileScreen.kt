package com.example.chologo.ui.profile

import android.content.Context
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.example.chologo.data.model.User
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth

// ─── Theme Tokens matching previous screens ──────────────────────────────────

private val BgDeep        = Color(0xFF080C10)
private val BgSurface     = Color(0xFF0E1318)
private val CardBase      = Color(0xFF141A21)
private val CardElevated  = Color(0xFF1A2130)
private val CardGlass     = Color(0xFF1E2736)

private val Lime          = Color(0xFF9FD63F)
private val LimeDeep      = Color(0xFF6FAF1A)
private val LimeDim       = Color(0xFF2A3E18)

private val AccentBlue    = Color(0xFF4D9FFF)
private val AccentEmerald = Color(0xFF30D878)
private val AccentRed     = Color(0xFFFF5461)

private val TextHigh      = Color(0xFFF0F4F8)
private val TextMed       = Color(0xFF8B9AB0)
private val TextLow       = Color(0xFF4A5568)

private val BorderSubtle  = Color(0xFF1E2D3D)
private val BorderFocus   = Color(0xFF2D4060)

private val GradientLime  = Brush.linearGradient(listOf(Lime, Color(0xFF6FBA2A)))
private val GradientHero  = Brush.linearGradient(
    listOf(Color(0xFF1A2233), Color(0xFF101620))
)
private val GradientDanger = Brush.linearGradient(
    listOf(Color(0xFF40202A), Color(0xFF241015))
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val repository = remember { UserRepository() }
    val scrollState = rememberScrollState()

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDeep)
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            ProfileTopBar(onBackClick = { goBackSafely() })

            Spacer(modifier = Modifier.height(18.dp))

            ProfileHeroCard(
                userName = userName,
                role = role,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(18.dp))

            SectionLabel(text = "Account Information")

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isLoading -> {
                    PremiumLoadingCard("Loading profile…")
                }

                !errorMessage.isNullOrEmpty() -> {
                    MessageCard(
                        title = "Could not load profile",
                        message = errorMessage ?: "Unknown error",
                        accent = AccentRed
                    )
                }

                else -> {
                    ProfileInfoCard(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = userEmail
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoCard(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = phone
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoCard(
                        icon = Icons.Default.Person,
                        label = "Role",
                        value = role
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoCard(
                        icon = Icons.Default.School,
                        label = "Student ID",
                        value = studentId
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoCard(
                        icon = Icons.Default.School,
                        label = "University",
                        value = university
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoCard(
                        icon = Icons.Default.LocationOn,
                        label = "Home Location",
                        value = homeLocation
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LogoutCard(
                onLogout = {
                    auth.signOut()

                    val prefs = navController.context.getSharedPreferences("chologo_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("user_role").apply()

                    navController.navigate(Screen.AuthChoice.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BgSurface, BgDeep)))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardElevated)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextHigh,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "My Profile",
            color = TextHigh,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
    }
}

// ─── Hero Card ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroCard(
    userName: String,
    role: String,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BorderFocus)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .drawBehind {
                            drawCircle(
                                brush = GradientLime,
                                radius = size.minDimension / 2f
                            )
                        }
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(CardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Icon",
                        tint = Lime,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = Lime,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = userName,
                        color = TextHigh,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RoleBadge(role = role)
                }
            }
        }
    }
}

// ─── Info Cards ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBase),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardGlass),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Lime,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = TextMed,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = value,
                    color = TextHigh,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Logout Section ──────────────────────────────────────────────────────────

@Composable
private fun LogoutCard(
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.22f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GradientDanger)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Session",
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sign out from your current account safely.",
                    color = TextMed,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
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
    }
}

// ─── Small UI Helpers ────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextHigh,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    )
}

@Composable
private fun RoleBadge(role: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(LimeDim)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = role,
            color = Lime,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PremiumLoadingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBase),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Lime,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = TextMed,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBase),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(color = BorderSubtle)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                color = TextMed,
                fontSize = 13.sp
            )
        }
    }
}