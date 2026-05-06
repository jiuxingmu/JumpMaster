package com.jumpmaster.app.util.sound

import android.content.Context
import android.media.SoundPool
import androidx.annotation.RawRes

/**
 * Thin wrapper around [SoundPool] for jump feedback SFX.
 */
class SoundEffectPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val pool = SoundPool.Builder().setMaxStreams(2).build()

    fun load(@RawRes resId: Int): Int = pool.load(appContext, resId, 1)

    fun play(soundId: Int) {
        pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
