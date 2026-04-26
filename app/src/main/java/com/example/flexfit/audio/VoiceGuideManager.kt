package com.example.flexfit.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.flexfit.R
import kotlinx.coroutines.*

/**
 * Voice guide manager for workout audio feedback.
 * Plays voice prompts based on workout state and error conditions.
 */
class VoiceGuideManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Error counters for confirmation mechanism
    private val errorCounters = mutableMapOf<VoiceType, Int>()
    private val errorCooldowns = mutableMapOf<VoiceType, Int>()

    // Configuration
    private val confirmFrames = 5  // Consecutive frames to confirm error
    private val cooldownFrames = 30 // Cooldown frames after warning (~1 second at 30fps)

    init {
        // Initialize counters
        VoiceType.entries.forEach {
            errorCounters[it] = 0
            errorCooldowns[it] = 0
        }
    }

    /**
     * Play a voice guide sound.
     * Uses async playback to avoid blocking the main thread.
     */
    fun playVoice(voiceType: VoiceType) {
        scope.launch {
            try {
                // Stop any currently playing audio
                stopCurrent()

                val resourceId = getResourceId(voiceType)
                if (resourceId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resourceId)
                    mediaPlayer?.setOnCompletionListener { mp ->
                        mp.release()
                    }
                    mediaPlayer?.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Check and trigger warning with confirmation mechanism.
     * Only triggers if error is detected for consecutive frames.
     */
    fun triggerWarning(voiceType: VoiceType): Boolean {
        if (voiceType == VoiceType.START || voiceType == VoiceType.SUCCESS || voiceType == VoiceType.FAIL) {
            // These are instant triggers, no confirmation needed
            playVoice(voiceType)
            return true
        }

        // Check cooldown
        val cooldown = errorCooldowns[voiceType] ?: 0
        if (cooldown > 0) {
            errorCooldowns[voiceType] = cooldown - 1
            return false
        }

        // Increment counter
        val count = (errorCounters[voiceType] ?: 0) + 1
        errorCounters[voiceType] = count

        // Confirm if reached threshold
        if (count >= confirmFrames) {
            playVoice(voiceType)
            errorCounters[voiceType] = 0
            errorCooldowns[voiceType] = cooldownFrames
            return true
        }

        return false
    }

    /**
     * Reset warning counter when error is not detected.
     */
    fun resetWarning(voiceType: VoiceType) {
        errorCounters[voiceType] = 0
    }

    /**
     * Reset all warning states (called when workout starts).
     */
    fun resetAll() {
        VoiceType.entries.forEach {
            errorCounters[it] = 0
            errorCooldowns[it] = 0
        }
    }

    private fun getResourceId(voiceType: VoiceType): Int {
        return when (voiceType) {
            VoiceType.START -> R.raw.start
            VoiceType.SUCCESS -> R.raw.success
            VoiceType.FAIL -> R.raw.fail
            VoiceType.SWINGING -> R.raw.swinging
            VoiceType.SHRUGGING -> R.raw.shrugging
            VoiceType.NOT_HIGH -> R.raw.not_high
        }
    }

    private fun stopCurrent() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Release resources when no longer needed.
     */
    fun release() {
        scope.cancel()
        stopCurrent()
    }
}

/**
 * Voice guide types corresponding to audio files.
 */
enum class VoiceType {
    START,       // Workout started, ready position confirmed
    SUCCESS,      // Successful rep completed
    FAIL,         // Rep failed (not pulled high enough)
    SWINGING,     // Body swinging detected
    SHRUGGING,    // Shrugging detected at top
    NOT_HIGH      // Not pulled high enough
}
