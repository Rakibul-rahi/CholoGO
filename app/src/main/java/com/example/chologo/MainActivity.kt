package com.example.chologo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.chologo.navigation.AppNavGraph
import com.example.chologo.navigation.Screen
import com.example.chologo.notifications.ReminderNotifications
import com.example.chologo.ui.theme.CholoGOTheme
import com.example.chologo.ui.theme.ThemeController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Registering the channel is a cheap no-op if it already exists -
        // safe to do unconditionally on every launch, before anything ever
        // tries to schedule a Tomorrow Ride reminder.
        ReminderNotifications.ensureChannel(applicationContext)
        ThemeController.init(applicationContext)

        setContent {
            // Every hand-styled screen picks its own colors via
            // LocalIsDarkTheme (provided here), not MaterialTheme directly -
            // this wrapper is still needed for the few plain Material3
            // components (AlertDialogs, default ripples) that DO read
            // MaterialTheme, so they stay in sync with the chosen mode.
            CholoGOTheme {
                // Keeps status bar icons legible against each screen's top
                // bar, which is near-black in dark mode and near-white in
                // light mode.
                val view = LocalView.current
                val isDarkTheme = ThemeController.isDarkTheme
                SideEffect {
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
                }

                MainAppEntry()
            }
        }
    }
}

@Composable
fun MainAppEntry() {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        startDestination = if (currentUser == null) {
            // No account yet: let people see the app (browse rides) before
            // asking them to sign in. Sign-in is prompted only when they
            // try to do something that needs an account (save a Tomorrow
            // Ride, request a Ride Now, view profile/history).
            Screen.PassengerHome.route
        } else {
            try {
                val document = db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val role = document.getString("role")

                when {
                    role.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                    role.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                    // Authenticated (e.g. a first-time Google sign-in) but no
                    // role yet - AuthChoice would be a dead end since
                    // "Login" only makes sense for a signed-out user.
                    else -> Screen.RoleSelection.route
                }
            } catch (e: Exception) {
                Screen.RoleSelection.route
            }
        }
    }

    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        AppNavGraph(startDestination = startDestination!!)
    }
}