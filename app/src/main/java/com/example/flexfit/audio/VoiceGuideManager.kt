package com.example.flexfit.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.flexfit.R
import kotlinx.coroutines.*
import kotlin.random.Random

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
                stopCurrent()

                val resourceId = getResourceId(voiceType)
                if (resourceId != 0 && isResourceAvailable(resourceId)) {
                    mediaPlayer = MediaPlayer.create(context, resourceId)
                    mediaPlayer?.setOnCompletionListener { mp ->
                        mp.release()
                    }
                    mediaPlayer?.start()
                }
                // Silently skip if resource is missing — no crash
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
        if (voiceType.instantPlayback) {
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

    private val VoiceType.instantPlayback: Boolean
        get() = when (this) {
            VoiceType.START,
            VoiceType.SUCCESS,
            VoiceType.FAIL,
            VoiceType.SHP_START,
            VoiceType.SHP_SUCCESS,
            VoiceType.SHP_FAIL -> true
            else -> false
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
        val name = when (voiceType) {
            VoiceType.START -> "start"
            VoiceType.SUCCESS -> "success"
            VoiceType.FAIL -> "fail"
            VoiceType.SWINGING -> "swinging"
            VoiceType.SHRUGGING -> "shrugging"
            VoiceType.NOT_HIGH -> "not_high"
            VoiceType.SHP_ADJUST_GRIP -> "shp_adjust_grip"
            VoiceType.SHP_ARMS_BALANCE -> "shp_arms_balance"
            VoiceType.SHP_BODY_UPRIGHT -> "shp_body_upright"
            VoiceType.SHP_START_POSITION -> "shp_start_position"
            VoiceType.SHP_START -> "shp_start"
            VoiceType.SHP_BRACE_CORE -> "shp_brace_core"
            VoiceType.SHP_SHRUGGING -> "shp_shrugging"
            VoiceType.SHP_NOT_HIGH -> "shp_not_high"
            VoiceType.SHP_BODY_LEAN -> "shp_body_lean"
            VoiceType.SHP_ELBOW_FLARE -> "shp_elbow_flare"
            VoiceType.SHP_BAD_WRIST -> "shp_bad_wrist"
            VoiceType.SHP_SUCCESS -> "shp_success_${Random.nextInt(1, 4)}"
            VoiceType.SHP_FAIL -> "shp_fail_${Random.nextInt(1, 3)}"
        }
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    private fun isResourceAvailable(resourceId: Int): Boolean {
        return try {
            context.resources.openRawResourceFd(resourceId).close()
            true
        } catch (_: Exception) {
            false
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
    NOT_HIGH,     // Not pulled high enough
    SHP_ADJUST_GRIP,
    SHP_ARMS_BALANCE,
    SHP_BODY_UPRIGHT,
    SHP_START_POSITION,
    SHP_START,
    SHP_BRACE_CORE,
    SHP_SHRUGGING,
    SHP_NOT_HIGH,
    SHP_BODY_LEAN,
    SHP_ELBOW_FLARE,
    SHP_BAD_WRIST,
    SHP_SUCCESS,
    SHP_FAIL
}
