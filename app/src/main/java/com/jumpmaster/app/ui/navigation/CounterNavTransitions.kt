package com.jumpmaster.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

private const val D_MS = 320
private const val D_OUT_MS = 280

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.counterHomeExitToTraining() =
    if (targetState.destination.route == AppRoutes.COUNTER_TRAINING) {
        fadeOut(animationSpec = tween(D_OUT_MS)) +
            slideOutHorizontally(
                animationSpec = tween(D_MS, easing = FastOutSlowInEasing),
            ) { w -> -(w / 5) }
    } else {
        null
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.counterHomePopEnterFromTraining() =
    if (initialState.destination.route == AppRoutes.COUNTER_TRAINING) {
        fadeIn(animationSpec = tween(D_MS)) +
            slideInHorizontally(
                animationSpec = tween(D_MS, easing = FastOutSlowInEasing),
            ) { w -> -(w / 5) }
    } else {
        null
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.trainingEnter() =
    fadeIn(animationSpec = tween(D_MS, delayMillis = 30)) +
        slideInHorizontally(
            animationSpec = tween(D_MS, easing = FastOutSlowInEasing),
        ) { it }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.trainingPopExit() =
    fadeOut(animationSpec = tween(D_OUT_MS)) +
        slideOutHorizontally(
            animationSpec = tween(D_MS, easing = FastOutSlowInEasing),
        ) { it }
