package com.jumpmaster.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jumpmaster.app.data.camera.awaitProcessCameraProvider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analysisExecutor =
        remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            cameraPermissionGranted = granted
        }

    LaunchedEffect(Unit) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView =
        remember(context) {
            PreviewView(context)
        }

    var useFrontCamera by remember { mutableStateOf(true) }
    var bindError by remember { mutableStateOf<String?>(null) }
    var analyzerHasFrames by remember { mutableStateOf(false) }

    var cameraProvider by remember {
        mutableStateOf<ProcessCameraProvider?>(null)
    }

    LaunchedEffect(cameraPermissionGranted) {
        cameraProvider =
            if (cameraPermissionGranted) {
                context.awaitProcessCameraProvider()
            } else {
                null
            }
    }

    val jumpCount by viewModel.jumpCount.collectAsStateWithLifecycle()
    val hint by viewModel.hint.collectAsStateWithLifecycle()

    DisposableEffect(cameraProvider, cameraPermissionGranted, lifecycleOwner, useFrontCamera) {
        val provider = cameraProvider
        if (provider == null || !cameraPermissionGranted) {
            return@DisposableEffect onDispose { }
        }

        bindError = null
        analyzerHasFrames = false
        val selector =
            CameraSelector.Builder()
                .requireLensFacing(
                    if (useFrontCamera) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    },
                )
                .build()

        if (!provider.hasCamera(selector)) {
            bindError =
                if (useFrontCamera) {
                    "当前设备/模拟器没有可用的前置摄像头源（请到 Emulator → Camera 里配置）"
                } else {
                    "当前设备/模拟器没有可用的后置摄像头源（请到 Emulator → Camera 里配置）"
                }
            return@DisposableEffect onDispose { }
        }

        val previewUseCase =
            Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        val analysisUseCase =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    val frameSeen = AtomicBoolean(false)
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (frameSeen.compareAndSet(false, true)) {
                            previewView.post {
                                analyzerHasFrames = true
                            }
                        }
                        viewModel.processCameraFrame(
                            imageProxy,
                            lensFacingFront = useFrontCamera,
                        )
                    }
                }

        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                previewUseCase,
                analysisUseCase,
            )
        }.onFailure { e ->
            bindError = "相机绑定失败：${e.javaClass.simpleName}: ${e.message}"
        }

        onDispose {
            provider.unbindAll()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "JumpMaster · Demo") },
                actions = {
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                    ) {
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
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            if (!cameraPermissionGranted) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                    ) {
                        Text("授予摄像头权限")
                    }
                }
                return@Box
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            bindError?.let { message ->
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                    tonalElevation = 6.dp,
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (bindError == null && !analyzerHasFrames) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        text = "相机已绑定，等待视频帧…（若持续不变，说明模拟器未向 CameraX 供帧）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                tonalElevation = 6.dp,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    text = "计数：$jumpCount",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
