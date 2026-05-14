package com.jumpmaster.app.ui.main

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

internal fun copyImageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val bitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
    try {
        imageProxy.planes[0].buffer.rewind()
        bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
    } finally {
        imageProxy.close()
    }
    return bitmap
}

internal fun orientBitmapForLens(bitmap: Bitmap, imageProxy: ImageProxy, lensFacingFront: Boolean): Bitmap {
    val matrix =
        Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (lensFacingFront) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
