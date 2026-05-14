package com.jumpmaster.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterTrainingScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberCameraPermissionState()
    var useFrontCamera by remember { mutableStateOf(true) }
    var cameraBindError by remember { mutableStateOf<String?>(null) }
    var hasAnalyzerFrames by remember { mutableStateOf(false) }
    var overlayRenderMode by remember { mutableStateOf(OverlayRenderMode.SKELETON) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraRebindNonce by remember { mutableStateOf(0) }
    var showEndConfirm by remember { mutableStateOf(false) }

    val latestViewModel by rememberUpdatedState(viewModel)
    val leaveTraining: () -> Unit = {
        latestViewModel.onScreenHiddenResetIfInSession()
        showEndConfirm = false
        onNavigateUp()
    }

    DisposableEffect(lifecycleOwner) {
        val activity = context.findActivity()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isChangingConfigurations != true) {
                latestViewModel.onScreenHiddenResetIfInSession()
                showEndConfirm = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (context.findActivity()?.isChangingConfigurations != true) {
                latestViewModel.onScreenHiddenResetIfInSession()
            }
        }
    }
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

    val previewView =
        remember(context) {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }
    val analysisExecutor = rememberAnalysisExecutor()

    val jumpCount by viewModel.jumpCount.collectAsStateWithLifecycle()
    val hint by viewModel.hint.collectAsStateWithLifecycle()
    val poseOverlayPoints by viewModel.poseOverlayPoints.collectAsStateWithLifecycle()
    val trainingState by viewModel.trainingState.collectAsStateWithLifecycle()
    val sessionSummary by viewModel.sessionSummary.collectAsStateWithLifecycle()
    val poseEngineRetrySuggested by viewModel.poseEngineRetrySuggested.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    SessionPersistSnackbarEffect(viewModel = viewModel, snackbarHostState = snackbarHostState)

    BackHandler {
        when {
            showEndConfirm -> showEndConfirm = false
            sessionSummary != null -> viewModel.dismissSessionSummary()
            else -> leaveTraining()
        }
    }

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
        rebindNonce = cameraRebindNonce,
    )

    Box(modifier = modifier.fillMaxSize()) {
        MainCounterSessionLayout(
            onToggleCamera = { useFrontCamera = !uiState.useFrontCamera },
            overlayRenderMode = overlayRenderMode,
            onToggleOverlay = { overlayRenderMode = overlayRenderMode.toggle() },
            onNavigateUp = leaveTraining,
            modifier = Modifier.fillMaxSize(),
        ) {
            MainCameraContent(
                cameraPermissionGranted = uiState.cameraPermissionGranted,
                onRequestPermission = permissionState.requestPermission,
                previewView = previewView,
                cameraBindError = uiState.cameraBindError,
                hasAnalyzerFrames = uiState.hasAnalyzerFrames,
                poseOverlayPoints = poseOverlayPoints,
                overlayRenderMode = overlayRenderMode,
                hint = hint,
                jumpCount = jumpCount,
                trainingState = trainingState,
                poseEngineRetrySuggested = poseEngineRetrySuggested,
                onRetryPoseEngine = { viewModel.retryPoseEngine() },
                onRetryCameraBind = {
                    cameraBindError = null
                    cameraRebindNonce++
                },
                onToggleActivePause = { viewModel.toggleActivePause() },
                onRequestEndTraining = { showEndConfirm = true },
            )
        }

        sessionSummary?.let { summary ->
            SessionSummaryBottomSheet(
                summary = summary,
                onDismiss = { viewModel.dismissSessionSummary() },
            )
        }

        if (showEndConfirm) {
            EndTrainingConfirmDialog(
                onDismiss = { showEndConfirm = false },
                onConfirm = {
                    viewModel.confirmEndTraining()
                    showEndConfirm = false
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun rememberAnalysisExecutor() =
    remember { Executors.newSingleThreadExecutor() }.also { executor ->
        DisposableEffect(Unit) {
            onDispose { executor.shutdown() }
        }
    }

private fun OverlayRenderMode.toggle(): OverlayRenderMode =
    when (this) {
        OverlayRenderMode.OFF -> OverlayRenderMode.SKELETON
        OverlayRenderMode.SKELETON -> OverlayRenderMode.OFF
    }
