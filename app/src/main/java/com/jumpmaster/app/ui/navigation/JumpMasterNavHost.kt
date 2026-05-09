package com.jumpmaster.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumpmaster.app.ui.history.HistoryScreen
import com.jumpmaster.app.ui.main.MainScreen
import com.jumpmaster.app.ui.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpMasterNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItems.forEach { navItem ->
                    val isSelected =
                        currentDestination?.hierarchy?.any { it.route == navItem.route } == true

                    NavigationBarItem(
                        icon = {
                            BadgedIcon(
                                icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                                badgeCount = navItem.badgeCount,
                            )
                        },
                        label = { Text(navItem.label) },
                        selected = isSelected,
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.COUNTER,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoutes.COUNTER) {
                MainScreen()
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

private val navItems = listOf(
    NavItem(
        route = AppRoutes.COUNTER,
        label = "开始",
        icon = Icons.Default.FitnessCenter,
    ),
    NavItem(
        route = AppRoutes.HISTORY,
        label = "历史",
        icon = Icons.Default.History,
    ),
    NavItem(
        route = AppRoutes.PROFILE,
        label = "我的",
        icon = Icons.Default.Person,
    ),
)

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int? = null,
)