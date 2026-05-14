package com.jumpmaster.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumpmaster.app.ui.history.HistoryScreen
import com.jumpmaster.app.ui.main.CounterHomeScreen
import com.jumpmaster.app.ui.main.CounterTrainingScreen
import com.jumpmaster.app.ui.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpMasterNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val hideBottomBar = currentRoute == AppRoutes.COUNTER_TRAINING

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets =
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar {
                    navItems.forEach { navItem ->
                        val isSelected =
                            currentDestination?.hierarchy?.any { it.route == navItem.route } == true ||
                                (navItem.route == AppRoutes.COUNTER &&
                                    currentRoute == AppRoutes.COUNTER_TRAINING)
                        NavigationBarItem(
                            icon = {
                                BadgedIcon(
                                    icon = {
                                        Icon(
                                            navItem.icon,
                                            contentDescription = navItem.label,
                                        )
                                    },
                                    badgeCount = navItem.badgeCount,
                                )
                            },
                            label = {
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Text(
                                        text = navItem.label,
                                        color = LocalContentColor.current,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            },
                            selected = isSelected,
                            alwaysShowLabel = false,
                            onClick = {
                                navController.navigate(navItem.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.COUNTER,
            modifier =
                if (hideBottomBar) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.padding(innerPadding).fillMaxSize()
                },
        ) {
            composable(
                route = AppRoutes.COUNTER,
                exitTransition = { counterHomeExitToTraining() },
                popEnterTransition = { counterHomePopEnterFromTraining() },
            ) {
                CounterHomeScreen(
                    onOpenTraining = { navController.navigate(AppRoutes.COUNTER_TRAINING) },
                )
            }

            composable(
                route = AppRoutes.COUNTER_TRAINING,
                enterTransition = { trainingEnter() },
                popExitTransition = { trainingPopExit() },
            ) {
                CounterTrainingScreen(
                    onNavigateUp = { navController.popBackStack() },
                )
            }

            composable(AppRoutes.HISTORY) {
                HistoryScreen()
            }

            composable(AppRoutes.PROFILE) {
                ProfileScreen()
            }
        }
    }
}

@Composable
private fun BadgedIcon(
    icon: @Composable () -> Unit,
    badgeCount: Int? = null,
) {
    if (badgeCount != null && badgeCount > 0) {
        Badge {
            icon()
            Text(badgeCount.toString())
        }
    } else {
        icon()
    }
}

private val navItems =
    listOf(
        NavItem(
            route = AppRoutes.COUNTER,
            label = "开始",
            icon = Icons.Outlined.FitnessCenter,
        ),
        NavItem(
            route = AppRoutes.HISTORY,
            label = "历史",
            icon = Icons.Outlined.History,
        ),
        NavItem(
            route = AppRoutes.PROFILE,
            label = "我的",
            icon = Icons.Outlined.Person,
        ),
    )

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int? = null,
)
