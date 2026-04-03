package com.example.chologo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chologo.ui.auth.AuthChoiceScreen
import com.example.chologo.ui.auth.LoginScreen
import com.example.chologo.ui.auth.RoleSelectionScreen
import com.example.chologo.ui.auth.SignupScreen
import com.example.chologo.ui.passenger.PassengerDashboardScreen
import com.example.chologo.ui.profile.ProfileScreen
import com.example.chologo.ui.rider.RiderDashboardScreen
import com.example.chologo.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(startDestination: String) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            Screen.PassengerHome.route -> {
                navController.navigate(Screen.PassengerHome.route) {
                    popUpTo(Screen.AuthChoice.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                authViewModel.clearNavigation()
            }

            Screen.RiderHome.route -> {
                navController.navigate(Screen.RiderHome.route) {
                    popUpTo(Screen.AuthChoice.route) {
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
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.login(email, password)
                },
                onSignupClick = {
                    navController.navigate(Screen.Signup.route) {
                        launchSingleTop = true
                    }
                },
                isLoading = uiState.isLoading,
                externalErrorMessage = uiState.errorMessage
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
                isLoading = uiState.isLoading,
                externalErrorMessage = uiState.errorMessage
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onPassengerSelected = {
                    navController.navigate(Screen.PassengerHome.route) {
                        popUpTo(Screen.RoleSelection.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onRiderSelected = {
                    navController.navigate(Screen.RiderHome.route) {
                        popUpTo(Screen.RoleSelection.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.PassengerHome.route) {
            PassengerDashboardScreen(navController = navController)
        }

        composable(Screen.RiderHome.route) {
            RiderDashboardScreen(navController = navController)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
    }
}