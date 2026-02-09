package com.jworks.kanjiquest.android.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("kanjiquest_settings", Context.MODE_PRIVATE)

    enum class SoundEffect { CORRECT, INCORRECT, LEVEL_UP, SESSION_COMPLETE, TAP }

    fun play(effect: SoundEffect) {
        if (!prefs.getBoolean("sound_enabled", true)) return
        val toneType = when (effect) {
            SoundEffect.CORRECT -> ToneGenerator.TONE_PROP_ACK
            SoundEffect.INCORRECT -> ToneGenerator.TONE_PROP_NACK
            SoundEffect.LEVEL_UP -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            SoundEffect.SESSION_COMPLETE -> ToneGenerator.TONE_PROP_BEEP2
            SoundEffect.TAP -> ToneGenerator.TONE_PROP_BEEP
        }
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            toneGen.startTone(toneType, 150)
            // ToneGenerator is released after tone finishes internally
        } catch (_: Exception) {
            // Silently ignore audio errors (e.g., no audio focus)
        }
    }
}
