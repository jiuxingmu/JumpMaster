package com.jumpmaster.app.data.camera

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

internal suspend fun Context.awaitProcessCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation: CancellableContinuation<ProcessCameraProvider> ->
        val future = ProcessCameraProvider.getInstance(this)
        continuation.invokeOnCancellation {
            future.cancel(true)
        }
        val executor = ContextCompat.getMainExecutor(this)
        future.addListener(
            Runnable {
                try {
                    continuation.resume(future.get())
                } catch (e: Throwable) {
                    continuation.resumeWithException(e)
                }
            },
            executor,
        )
    }
