package com.jumpmaster.app.ui.main

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun SessionPersistSnackbarEffect(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.sessionPersistEvents.collect { event ->
            when (event) {
                SessionPersistEvent.Saved -> snackbarHostState.showSnackbar("已保存")
                SessionPersistEvent.Failed -> {
                    when (
                        snackbarHostState.showSnackbar(
                            message = "保存失败，请重试",
                            actionLabel = "重试",
                            duration = SnackbarDuration.Long,
                        )
                    ) {
                        SnackbarResult.ActionPerformed -> viewModel.retryPersistSession()
                        SnackbarResult.Dismissed -> Unit
                    }
                }
            }
        }
    }
}
