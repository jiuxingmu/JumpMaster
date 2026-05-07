package com.jumpmaster.app.ui.main

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraBindingEffect(
    cameraProvider: ProcessCameraProvider?,
    cameraPermissionGranted: Boolean,
    lifecycleOwner: LifecycleOwner,
    useFrontCamera: Boolean,
    previewView: PreviewView,
    analysisExecutor: ExecutorService,
    onAnalyzeFrame: (ImageProxy, Boolean) -> Unit,
    onCameraBindErrorChange: (String?) -> Unit,
    onFirstAnalyzerFrame: () -> Unit,
) {
    DisposableEffect(cameraProvider, cameraPermissionGranted, lifecycleOwner, useFrontCamera) {
        val provider = cameraProvider
        if (provider == null || !cameraPermissionGranted) return@DisposableEffect onDispose { }

        onCameraBindErrorChange(null)
        val selector = buildSelector(useFrontCamera)
        if (!provider.hasCamera(selector)) {
            onCameraBindErrorChange(buildNoCameraMessage(useFrontCamera))
            return@DisposableEffect onDispose { }
        }

        val previewUseCase = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysisUseCase =
            buildAnalysisUseCase(
                analysisExecutor = analysisExecutor,
                useFrontCamera = useFrontCamera,
                previewView = previewView,
                onAnalyzeFrame = onAnalyzeFrame,
                onFirstAnalyzerFrame = onFirstAnalyzerFrame,
            )
        bindUseCases(provider, lifecycleOwner, selector, previewUseCase, analysisUseCase, onCameraBindErrorChange)
        onDispose { provider.unbindAll() }
    }
}

private fun buildSelector(useFrontCamera: Boolean): CameraSelector =
    CameraSelector.Builder()
        .requireLensFacing(
            if (useFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK,
        )
        .build()

private fun buildNoCameraMessage(useFrontCamera: Boolean): String =
    if (useFrontCamera) {
        "当前设备/模拟器没有可用的前置摄像头源（请到 Emulator → Camera 里配置）"
    } else {
        "当前设备/模拟器没有可用的后置摄像头源（请到 Emulator → Camera 里配置）"
    }

private fun buildAnalysisUseCase(
    analysisExecutor: ExecutorService,
    useFrontCamera: Boolean,
    previewView: PreviewView,
    onAnalyzeFrame: (ImageProxy, Boolean) -> Unit,
    onFirstAnalyzerFrame: () -> Unit,
): ImageAnalysis {
    val frameSeen = AtomicBoolean(false)
    return ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .build()
        .also { analysis ->
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                if (frameSeen.compareAndSet(false, true)) {
                    previewView.post { onFirstAnalyzerFrame() }
                }
                onAnalyzeFrame(imageProxy, useFrontCamera)
            }
        }
}

private fun bindUseCases(
    provider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    selector: CameraSelector,
    previewUseCase: Preview,
    analysisUseCase: ImageAnalysis,
    onCameraBindErrorChange: (String?) -> Unit,
) {
    runCatching {
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, previewUseCase, analysisUseCase)
    }.onFailure { e ->
        onCameraBindErrorChange("相机绑定失败：${e.javaClass.simpleName}: ${e.message}")
    }
}
