package com.jworks.kanjiquest.android.data

import android.content.Context
import com.jworks.kanjiquest.core.domain.model.GameMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tracks daily preview trial usage for free-tier users.
 * Free users can try premium modes a limited number of times per day.
 */
class PreviewTrialManager(context: Context) {

    private val prefs = context.getSharedPreferences("kanjiquest_preview_trials", Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    companion object {
        /** Max daily preview trials per mode */
        val TRIAL_LIMITS = mapOf(
            GameMode.WRITING to 3,
            GameMode.VOCABULARY to 3,
            GameMode.CAMERA_CHALLENGE to 1
        )
    }

    private fun todayKey(mode: GameMode): String {
        val today = LocalDate.now().format(dateFormatter)
        return "${mode.name}_$today"
    }

    /** Get remaining preview trials for a mode today */
    fun getRemainingTrials(mode: GameMode): Int {
        val limit = TRIAL_LIMITS[mode] ?: return 0
        val used = prefs.getInt(todayKey(mode), 0)
        return (limit - used).coerceAtLeast(0)
    }

    /** Use one preview trial. Returns true if trial was available and consumed. */
    fun usePreviewTrial(mode: GameMode): Boolean {
        val remaining = getRemainingTrials(mode)
        if (remaining <= 0) return false
        val key = todayKey(mode)
        val used = prefs.getInt(key, 0)
        prefs.edit().putInt(key, used + 1).apply()
        return true
    }

    /** Check if a mode has any remaining preview trials today */
    fun hasTrialsRemaining(mode: GameMode): Boolean {
        return getRemainingTrials(mode) > 0
    }

    /** Get total trial limit for a mode */
    fun getTrialLimit(mode: GameMode): Int {
        return TRIAL_LIMITS[mode] ?: 0
    }
}
