package com.jumpmaster.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jumpmaster.app.data.camera.awaitProcessCameraProvider
import java.util.concurrent.Executors

private data class CameraUiState(
    val cameraPermissionGranted: Boolean,
    val useFrontCamera: Boolean,
    val cameraBindError: String?,
    val hasAnalyzerFrames: Boolean,
    val cameraProvider: ProcessCameraProvider?,
)

private data class CameraPermissionState(
    val granted: Boolean,
    val requestPermission: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberCameraPermissionState(context)
    var useFrontCamera by remember { mutableStateOf(true) }
    var cameraBindError by remember { mutableStateOf<String?>(null) }
    var hasAnalyzerFrames by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(permissionState.granted) {
        cameraProvider = if (permissionState.granted) context.awaitProcessCameraProvider() else null
    }

    val uiState =
        CameraUiState(
            cameraPermissionGranted = permissionState.granted,
            useFrontCamera = useFrontCamera,
            cameraBindError = cameraBindError,
            hasAnalyzerFrames = hasAnalyzerFrames,
            cameraProvider = cameraProvider,
        )

    val previewView = remember(context) { PreviewView(context) }
    val analysisExecutor = rememberAnalysisExecutor()

    val jumpCount by viewModel.jumpCount.collectAsStateWithLifecycle()
    val hint by viewModel.hint.collectAsStateWithLifecycle()

    CameraBindingEffect(
        cameraProvider = uiState.cameraProvider,
        cameraPermissionGranted = uiState.cameraPermissionGranted,
        lifecycleOwner = lifecycleOwner,
        useFrontCamera = uiState.useFrontCamera,
        previewView = previewView,
        analysisExecutor = analysisExecutor,
        onAnalyzeFrame = { imageProxy, lensFacingFront ->
            viewModel.processCameraFrame(imageProxy, lensFacingFront)
        },
        onCameraBindErrorChange = { cameraBindError = it },
        onFirstAnalyzerFrame = { hasAnalyzerFrames = true },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "JumpMaster · Demo") },
                actions = {
                    IconButton(onClick = { useFrontCamera = !uiState.useFrontCamera }) {
                        Icon(
                            Icons.Outlined.Cameraswitch,
                            contentDescription = "切换摄像头",
                        )
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = "历史记录",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        MainCameraContent(
            innerPadding = innerPadding,
            cameraPermissionGranted = uiState.cameraPermissionGranted,
            onRequestPermission = permissionState.requestPermission,
            previewView = previewView,
            cameraBindError = uiState.cameraBindError,
            hasAnalyzerFrames = uiState.hasAnalyzerFrames,
            hint = hint,
            jumpCount = jumpCount,
        )
    }
}

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
private fun rememberCameraPermissionState(context: android.content.Context): CameraPermissionState {
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

@Composable
private fun rememberAnalysisExecutor() =
    remember { Executors.newSingleThreadExecutor() }.also { executor ->
        DisposableEffect(Unit) {
            onDispose { executor.shutdown() }
        }
    }
