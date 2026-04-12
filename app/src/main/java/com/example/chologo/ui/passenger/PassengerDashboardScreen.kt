@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chologo.ui.passenger

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chologo.R
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.example.chologo.ui.components.LocalAdCarouselBanner
import com.example.chologo.utils.LevelSystem
import com.example.chologo.viewmodel.AuthViewModel

@Composable
fun PassengerDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ride Now", "Tomorrow")
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

    val levelInfo = remember(passengerXp) {
        LevelSystem.getLevelInfo(passengerXp)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDeep
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                PassengerTopBar(navController = navController)
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    PassengerHeroCard(
                        passengerName = authState.userName,
                        levelInfo = levelInfo,
                        isLevelLoading = isLevelLoading
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    LocalAdCarouselBanner()
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                PremiumTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
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
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
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

@Composable
fun PassengerTopBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(BgSurface, BgDeep)
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.chologologo),
            contentDescription = "CholoGO",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(92.dp)
                .wrapContentWidth()
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CardElevated)
                .border(
                    width = 1.dp,
                    color = BorderFocus,
                    shape = CircleShape
                )
                .clickable {
                    navController.navigate(Screen.Profile.createRoute("passenger")) {
                        launchSingleTop = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Lime,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}