package com.jworks.kanjiquest.android.audio

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("kanjiquest_settings", Context.MODE_PRIVATE)

    enum class HapticType { SUCCESS, ERROR, TAP }

    fun vibrate(type: HapticType) {
        if (!prefs.getBoolean("vibrations_enabled", true)) return
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        val (duration, amplitude) = when (type) {
            HapticType.SUCCESS -> 50L to 128
            HapticType.ERROR -> 100L to 200
            HapticType.TAP -> 20L to 64
        }
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } catch (_: Exception) {
            // Silently ignore vibration errors
        }
    }
}
