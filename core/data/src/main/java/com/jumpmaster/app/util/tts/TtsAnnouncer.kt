package com.jumpmaster.app.util.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Minimal Text-to-Speech helper for count announcements.
 */
class TtsAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    private val ttsRef = AtomicReference<TextToSpeech?>()

    init {
        ttsRef.set(TextToSpeech(context.applicationContext, this))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsRef.get()?.language = Locale.getDefault()
        }
    }

    fun speak(message: String) {
        ttsRef.get()?.speak(message, TextToSpeech.QUEUE_FLUSH, null, message.hashCode().toString())
    }

    fun shutdown() {
        ttsRef.getAndSet(null)?.shutdown()
    }
}
