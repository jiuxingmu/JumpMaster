package com.jumpmaster.app.ui.main

import android.os.SystemClock

/** C-01：训练会话 UI 状态（不计入 Room，与 `counter-01` 一致）。 */
enum class TrainingSessionState {
    Idle,
    Active,
    Paused,
}

/** 结束摘要展示用快照（暂停不计入的「有效训练时长」）。 */
data class SessionSummary(
    val jumpCount: Int,
    val effectiveDurationMs: Long,
)

/**
 * 有效时长：自首次进入 [TrainingSessionState.Active] 起累计处于 Active 的毫秒数；
 * [TrainingSessionState.Paused] 期间不计入（与 C-01 子 PRD 一致）。
 */
internal class ActiveDurationTracker {
    private var accumulatedActiveMs: Long = 0L
    private var activeSegmentStartElapsed: Long? = null

    fun reset() {
        accumulatedActiveMs = 0L
        activeSegmentStartElapsed = null
    }

    fun startOrResumeSegment(nowElapsed: Long) {
        if (activeSegmentStartElapsed == null) activeSegmentStartElapsed = nowElapsed
    }

    fun pauseOpenSegment(nowElapsed: Long) {
        val start = activeSegmentStartElapsed ?: return
        accumulatedActiveMs += (nowElapsed - start).coerceAtLeast(0L)
        activeSegmentStartElapsed = null
    }

    fun effectiveMsAt(nowElapsed: Long): Long {
        val start = activeSegmentStartElapsed ?: return accumulatedActiveMs
        return accumulatedActiveMs + (nowElapsed - start).coerceAtLeast(0L)
    }
}

internal fun formatEffectiveDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}

internal fun nowElapsedRealtime(): Long = SystemClock.elapsedRealtime()

/** 不向用户暴露技术类名/堆栈的文案（日志仍记录详情）。 */
internal object TrainingFriendlyCopy {
    const val ENGINE_PREPARING = "正在准备运动引擎…"
    const val ENGINE_ASSISTANCE = "请确保光线充足，全身在取景框内"
    const val COACHING_RHYTHM = "保持节奏，继续跳跃"
    const val CAMERA_GENERIC = "暂时无法连接到相机"
    const val CAMERA_HINT = "请检查权限或稍后重试"
}
