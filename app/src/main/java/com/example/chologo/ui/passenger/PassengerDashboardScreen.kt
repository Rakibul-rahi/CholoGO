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
import androidx.compose.runtime.DisposableEffect
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
import com.example.chologo.data.repository.XpRepository
import com.example.chologo.utils.LevelSystem
import com.example.chologo.ui.common.CholoGoTabRow
import com.example.chologo.ui.common.CholoGoTopBar
import com.example.chologo.ui.common.GuestSignInBanner
import com.example.chologo.ui.components.LevelCard
import com.example.chologo.ui.components.LocalAdCarouselBanner
import com.example.chologo.ui.theme.LocalIsDarkTheme
import com.example.chologo.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

private val DashboardBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0A0D0F) else Color(0xFFF7F9FA)

@Composable
fun PassengerDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val authState by authViewModel.uiState.collectAsState()

    // Checked once per composition: this screen is now also the app's
    // signed-out landing page, so reads/writes that need an account are
    // gated behind this instead of assuming a user is always present.
    val isGuest = remember { FirebaseAuth.getInstance().currentUser == null }

    var passengerXp by remember { mutableStateOf(0L) }
    var isLevelLoading by remember { mutableStateOf(true) }

    val onRequireLogin: () -> Unit = {
        navController.navigate(Screen.AuthChoice.route) {
            launchSingleTop = true
        }
    }

    val xpRepository = remember { XpRepository() }

    LaunchedEffect(Unit) {
        if (!isGuest) {
            authViewModel.loadCurrentUser()
        } else {
            isLevelLoading = false
        }
    }

    // XP comes from the ledger, not from users/{uid}.xp - see the note on
    // the rider dashboard.
    DisposableEffect(authState.userId, isGuest) {
        if (isGuest || authState.userId.isBlank()) {
            return@DisposableEffect onDispose { }
        }

        val registration = xpRepository.listenTotalXp(
            userId = authState.userId,
            onData = { total ->
                passengerXp = total
                isLevelLoading = false
            },
            onError = {
                isLevelLoading = false
            }
        )

        onDispose { registration.remove() }
    }

    LaunchedEffect(authState.userId, isGuest) {
        if (!isGuest && authState.userId.isNotBlank()) {
            xpRepository.claimTripXpFor(authState.userId, isRider = false)
        }
    }

    // One curve for both roles. The passenger side used to carry its own
    // private thresholds (150/400/700/...) alongside LevelSystem's
    // (100/250/450/...), so the same XP showed a different level depending
    // on which screen you happened to be looking at.
    val levelInfo = remember(passengerXp) {
        LevelSystem.getLevelInfo(passengerXp)
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
                        if (isGuest) {
                            onRequireLogin()
                        } else {
                            navController.navigate(Screen.RideHistory.createRoute("passenger"))
                        }
                    },
                    onProfileClick = {
                        if (isGuest) {
                            onRequireLogin()
                        } else {
                            navController.navigate(Screen.Profile.createRoute("passenger")) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                if (isGuest) {
                    GuestSignInBanner(onSignInClick = onRequireLogin)
                } else {
                    LevelCard(
                        level = if (isLevelLoading) 1 else levelInfo.level,
                        levelTitle = if (isLevelLoading) {
                            LevelSystem.getLevelTitle(1)
                        } else {
                            levelInfo.levelTitle
                        },
                        currentXp = if (isLevelLoading) 0L else levelInfo.currentXp,
                        xpNeededForNextLevel = if (isLevelLoading) {
                            100L
                        } else {
                            levelInfo.xpNeededForNextLevel
                        },
                        progress = if (isLevelLoading) 0f else levelInfo.progressFraction,
                        userName = authState.userName.ifBlank { "Passenger" }
                    )
                }
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
                                passengerName = authState.userName,
                                onRequireLogin = onRequireLogin
                            )

                            1 -> PassengerTomorrowTab(
                                authViewModel = authViewModel,
                                onRequireLogin = onRequireLogin
                            )
                        }
                    }
                }
            }
        }
    }
}
