package com.jumpmaster.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal data class CameraPermissionState(
    val granted: Boolean,
    val requestPermission: () -> Unit,
)

@Composable
internal fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    val permissionLauncher = rememberPermissionLauncher { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    return CameraPermissionState(
        granted = granted,
        requestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
    )
}

@Composable
private fun rememberPermissionLauncher(
    onResult: (Boolean) -> Unit,
): ActivityResultLauncher<String> =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
    )

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
