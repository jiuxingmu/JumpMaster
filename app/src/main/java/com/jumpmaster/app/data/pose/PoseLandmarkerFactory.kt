package com.jumpmaster.app.data.pose

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 封装 MediaPipe Pose Landmarker（VIDEO 帧序推断）创建与生命周期。 */
@Singleton
class PoseLandmarkerFactory @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    private var instance: PoseLandmarker? = null

    /** 幂等 acquire；在未显式 [`release`] 前复用单例。 */
    fun acquire(): PoseLandmarker =
        synchronized(this) {
            if (instance == null) {
                val baseOptions =
                    BaseOptions.builder()
                        .setModelAssetPath(MODEL_ASSET_PATH)
                        .build()

                val options =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setRunningMode(RunningMode.VIDEO)
                        .setMinPoseDetectionConfidence(0.30f)
                        .setMinPosePresenceConfidence(0.30f)
                        .setMinTrackingConfidence(0.30f)
                        .build()

                instance = PoseLandmarker.createFromOptions(appContext, options)
            }
            instance!!
        }

    /** 同步释放原生句柄（例如停用摄像头后调用）。 */
    fun releaseLocked() {
        synchronized(this) {
            instance?.close()
            instance = null
        }
    }

    companion object {
        const val MODEL_ASSET_PATH: String = "pose_landmarker_lite.task"

        /** BlazePose（33 landmarks）关键点索引。 */
        const val NOSE: Int = 0
        const val LEFT_SHOULDER: Int = 11
        const val RIGHT_SHOULDER: Int = 12
        const val LEFT_HIP: Int = 23
        const val RIGHT_HIP: Int = 24
    }
}
