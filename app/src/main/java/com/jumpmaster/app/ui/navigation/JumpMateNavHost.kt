package com.jumpmaster.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jumpmaster.app.ui.history.HistoryScreen
import com.jumpmaster.app.ui.main.MainScreen

@Composable
fun JumpMateNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.COUNTER,
        modifier = modifier,
    ) {
        composable(AppRoutes.COUNTER) {
            MainScreen(
                onOpenHistory = {
                    navController.navigate(AppRoutes.HISTORY)
                },
            )
        }

        composable(AppRoutes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
    }
}
