package com.example.chologo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chologo.ui.auth.AuthChoiceScreen
import com.example.chologo.ui.auth.ForgotPasswordScreen
import com.example.chologo.ui.auth.LoginScreen
import com.example.chologo.ui.auth.RoleSelectionScreen
import com.example.chologo.ui.auth.SignupScreen
import com.example.chologo.ui.common.RideHistoryScreen
import com.example.chologo.ui.passenger.PassengerDashboardScreen
import com.example.chologo.ui.rider.RiderDashboardScreen
import com.example.chologo.ui.rider.RiderRideNowScreen
import com.example.chologo.ui.screens.ProfileScreen
import com.example.chologo.viewmodel.AuthViewModel
import com.example.chologo.viewmodel.RideNowViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel()
    val rideNowViewModel: RideNowViewModel = viewModel()

    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            Screen.PassengerHome.route -> {
                navController.navigate(Screen.PassengerHome.route) {
                    // Clear the whole back stack (not just up to AuthChoice) -
                    // a signed-out user may have reached AuthChoice from a
                    // Tomorrow Ride/Ride Now prompt while already sitting on
                    // PassengerHome, and that stale guest entry shouldn't
                    // linger underneath the freshly-authenticated one.
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                authViewModel.clearNavigation()
            }

            Screen.RiderHome.route -> {
                navController.navigate(Screen.RiderHome.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                authViewModel.clearNavigation()
            }

            Screen.RoleSelection.route -> {
                // A first-time Google sign-in: authenticated, but no
                // Firestore profile yet. Clear the login/signup back stack
                // so there's nothing to navigate "back" into mid-completion.
                navController.navigate(Screen.RoleSelection.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                authViewModel.clearNavigation()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.AuthChoice.route) {
            AuthChoiceScreen(
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        launchSingleTop = true
                    }
                },
                onSignupClick = {
                    navController.navigate(Screen.Signup.route) {
                        launchSingleTop = true
                    }
                },
                // Only offer a way back when there's actually somewhere to
                // return to (a guest who browsed here) - not when this is
                // genuinely the graph's start destination (e.g. a signed-in
                // account with no role set yet).
                onBackClick = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else {
                    null
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.login(email, password)
                },
                onGoogleSignInClick = {
                    authViewModel.signInWithGoogle(context)
                },
                onSignupClick = {
                    navController.navigate(Screen.Signup.route) {
                        launchSingleTop = true
                    }
                },
                onForgotPasswordClick = {
                    navController.navigate(Screen.ForgotPassword.route) {
                        launchSingleTop = true
                    }
                },
                onBackClick = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else {
                    null
                },
                isLoading = uiState.isLoading,
                externalErrorMessage = uiState.errorMessage
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupClick = { role, name, email, phone, studentId, university, homeLocation, password ->
                    authViewModel.signup(
                        role = role,
                        name = name,
                        email = email,
                        phone = phone,
                        studentId = studentId,
                        university = university,
                        homeLocation = homeLocation,
                        password = password
                    )
                },
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        launchSingleTop = true
                    }
                },
                onBackClick = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else {
                    null
                },
                isLoading = uiState.isLoading,
                externalErrorMessage = uiState.errorMessage
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onCompleteProfile = { role, phone ->
                    authViewModel.completeGoogleProfile(role, phone)
                },
                isLoading = uiState.isLoading,
                externalErrorMessage = uiState.errorMessage
            )
        }

        composable(Screen.PassengerHome.route) {
            PassengerDashboardScreen(navController = navController)
        }

        composable(Screen.RiderHome.route) {
            RiderDashboardScreen(navController = navController)
        }

        composable(Screen.RiderRideNow.route) {
            RiderRideNowScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(Screen.RideHistory.route) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "passenger"
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            RideHistoryScreen(
                userId = currentUserId,
                source = source,
                viewModel = rideNowViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}