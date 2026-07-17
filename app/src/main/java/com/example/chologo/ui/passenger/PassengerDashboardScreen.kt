@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.passenger


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.example.chologo.ui.common.CholoGoTabRow
import com.example.chologo.ui.common.CholoGoTopBar
import com.example.chologo.ui.components.LevelCard
import com.example.chologo.ui.components.LocalAdCarouselBanner
import com.example.chologo.viewmodel.AuthViewModel

private val DashboardBg = Color(0xFF0A0D0F)

@Composable
fun PassengerDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val authState by authViewModel.uiState.collectAsState()
    val userRepository = remember { UserRepository() }

    var passengerXp by remember { mutableStateOf(0L) }
    var isLevelLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser()

        userRepository.getCurrentUserData { result ->
            result.onSuccess { user ->
                passengerXp = user.xp
                isLevelLoading = false
            }.onFailure {
                passengerXp = 0L
                isLevelLoading = false
            }
        }
    }

    val level = remember(passengerXp) {
        calculatePassengerLevel(passengerXp)
    }

    val levelTitle = remember(level) {
        getPassengerLevelTitle(level)
    }

    val xpNeededForNextLevel = remember(level) {
        getNextLevelXp(level)
    }

    val progress = remember(passengerXp, level, xpNeededForNextLevel) {
        val previousLevelXp = getPreviousLevelXp(level)
        val xpInCurrentLevel = passengerXp - previousLevelXp
        val xpRange = xpNeededForNextLevel - previousLevelXp

        if (xpRange <= 0L) {
            0f
        } else {
            (xpInCurrentLevel.toFloat() / xpRange.toFloat()).coerceIn(0f, 1f)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBg),
        color = DashboardBg
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                CholoGoTopBar(
                    onLogoClick = {
                        navController.navigate(Screen.PassengerHome.route) {
                            popUpTo(Screen.PassengerHome.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onRideHistoryClick = {
                        navController.navigate(Screen.RideHistory.createRoute("passenger"))
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.createRoute("passenger")) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                LevelCard(
                    level = if (isLevelLoading) 1 else level,
                    levelTitle = if (isLevelLoading) "Campus Starter" else levelTitle,
                    currentXp = if (isLevelLoading) 0L else passengerXp,
                    xpNeededForNextLevel = if (isLevelLoading) 150L else xpNeededForNextLevel,
                    progress = if (isLevelLoading) 0f else progress,
                    userName = authState.userName.ifBlank { "Passenger" }
                )
            }

            item {
                LocalAdCarouselBanner()
            }

            item {
                CholoGoTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (
                                fadeIn(animationSpec = tween(260)) +
                                        slideInVertically(animationSpec = tween(260)) { it / 10 }
                                ) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "passenger_tab_content"
                ) { tab ->
                    Box {
                        when (tab) {
                            0 -> PassengerRideNowScreen(
                                passengerName = authState.userName
                            )

                            1 -> PassengerTomorrowTab(
                                authViewModel = authViewModel,
                                userRepository = userRepository,
                                onXpUpdated = { updatedXp ->
                                    passengerXp = updatedXp
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculatePassengerLevel(xp: Long): Int {
    return when {
        xp >= 5000L -> 10
        xp >= 4000L -> 9
        xp >= 3000L -> 8
        xp >= 2200L -> 7
        xp >= 1600L -> 6
        xp >= 1100L -> 5
        xp >= 700L -> 4
        xp >= 400L -> 3
        xp >= 150L -> 2
        else -> 1
    }
}

private fun getPreviousLevelXp(level: Int): Long {
    return when (level) {
        1 -> 0L
        2 -> 150L
        3 -> 400L
        4 -> 700L
        5 -> 1100L
        6 -> 1600L
        7 -> 2200L
        8 -> 3000L
        9 -> 4000L
        10 -> 5000L
        else -> 0L
    }
}

private fun getNextLevelXp(level: Int): Long {
    return when (level) {
        1 -> 150L
        2 -> 400L
        3 -> 700L
        4 -> 1100L
        5 -> 1600L
        6 -> 2200L
        7 -> 3000L
        8 -> 4000L
        9 -> 5000L
        else -> 6000L
    }
}

private fun getPassengerLevelTitle(level: Int): String {
    return when (level) {
        1 -> "Campus Starter"
        2 -> "Route Explorer"
        3 -> "Daily Passenger"
        4 -> "Campus Regular"
        5 -> "Smart Commuter"
        6 -> "Ride Pro"
        7 -> "AUST Traveler"
        8 -> "Priority Passenger"
        9 -> "Elite Commuter"
        else -> "CholoGO Legend"
    }
}