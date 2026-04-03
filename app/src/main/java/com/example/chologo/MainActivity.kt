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
import com.example.chologo.navigation.AppNavGraph
import com.example.chologo.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainAppEntry()
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
            Screen.AuthChoice.route
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