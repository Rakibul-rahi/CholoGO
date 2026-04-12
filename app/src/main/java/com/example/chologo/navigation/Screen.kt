package com.example.chologo.navigation

sealed class Screen(val route: String) {
    data object AuthChoice : Screen("auth_choice")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    data object RoleSelection : Screen("role_selection")
    data object PassengerHome : Screen("passenger_home")
    data object RiderHome : Screen("rider_home")
    object RiderRideNow : Screen("rider_ride_now")
    data object Profile : Screen("profile/{source}") {
        fun createRoute(source: String): String = "profile/$source"
    }
}