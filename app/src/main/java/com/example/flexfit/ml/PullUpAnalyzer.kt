package com.example.flexfit.ml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

enum class PullUpType(val displayName: String, val gripMin: Float, val gripMax: Float, val startAngle: Float) {
    WIDE("Wide Grip", 1.8f, 3.0f, 152f),
    NORMAL("Normal Grip", 1.3f, 1.9f, 157f),
    NARROW("Narrow Grip", 0.7f, 1.4f, 155f)
}

enum class PullUpState {
    PREPARING,  // Preparation phase
    DOWN,       // Arms extended
    UP,         // Pulling up
    TOP         // Top position
}

data class PullUpFeedback(
    val message: String,
    val type: FeedbackType
)

enum class FeedbackType {
    INFO,       // Information
    SUCCESS,    // Success
    WARNING,    // Warning
    ERROR       // Error
}

data class PullUpAnalysisResult(
    val count: Int = 0,
    val state: PullUpState = PullUpState.PREPARING,
    val isReady: Boolean = false,
    val feedback: PullUpFeedback? = null,
    val accuracy: Float = 0f,
    val elapsedTime: Long = 0L,
    val voiceAction: VoiceAction? = null
)

enum class VoiceAction {
    START,       // Workout started
    SUCCESS,      // Rep completed successfully
    FAIL,         // Rep failed
    SWINGING,     // Body swinging
    SHRUGGING,    // Shrugging
    NOT_HIGH      // Not pulled high enough
}

class PullUpAnalyzer(private val pullUpType: PullUpType) {
    
    private var state: PullUpState = PullUpState.PREPARING
    private var count: Int = 0
    private var prevKeypoints: FloatArray? = null
    private var isReady: Boolean = false
    private var validTop: Boolean = false
    private var lastHint: String? = null
    private var hintCooldown: Int = 0
    private var pendingVoiceAction: VoiceAction? = null
    
    private var errorCounters = mutableMapOf(
        "swinging" to 0,
        "shrugging" to 0,
        "not_high" to 0
    )
    
    private var errorCooldowns = mutableMapOf(
        "swinging" to 0,
        "shrugging" to 0,
        "not_high" to 0
    )
    
    private val confirmFrames = 5
    private val cooldownFrames = 30
    
    // Keypoint indices (MediaPipe format)
    companion object {
        const val NOSE = 0
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_ELBOW = 13
        const val RIGHT_ELBOW = 14
        const val LEFT_WRIST = 15
        const val RIGHT_WRIST = 16
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_EAR = 7
        const val RIGHT_EAR = 8
        
        // Coefficients (default values, will be updated from calibration)
        var coefLeftEarLeftShoulder: Float = 0.5f
        var coefRightEarRightShoulder: Float = 0.5f
    }
    
    fun analyze(keypoints: FloatArray, timestamp: Long): PullUpAnalysisResult {
        val feedback = mutableListOf<PullUpFeedback>()
        
        // Reset voice action for this frame
        pendingVoiceAction = null
        
        // Decrement cooldowns
        errorCooldowns.keys.forEach { key ->
            if (errorCooldowns[key]!! > 0) {
                errorCooldowns[key] = errorCooldowns[key]!! - 1
            }
        }
        
        if (hintCooldown > 0) hintCooldown--
        
        // === Preparation Phase ===
        if (!isReady) {
            val preparationFeedback = handlePreparation(keypoints)
            preparationFeedback?.let { feedback.add(it) }
            prevKeypoints = keypoints
            return PullUpAnalysisResult(
                count = count,
                state = state,
                isReady = isReady,
                feedback = feedback.lastOrNull(),
                accuracy = calculateAccuracy(keypoints),
                elapsedTime = timestamp,
                voiceAction = pendingVoiceAction
            )
        }
        
        // === State Transitions ===
        val stateFeedback = handleStateTransitions(keypoints)
        stateFeedback?.let { feedback.add(it) }
        
        // === Error Detection ===
        val errorFeedback = handleErrorDetection(keypoints)
        errorFeedback?.let { feedback.add(it) }
        
        prevKeypoints = keypoints
        
        return PullUpAnalysisResult(
            count = count,
            state = state,
            isReady = isReady,
            feedback = feedback.lastOrNull(),
            accuracy = calculateAccuracy(keypoints),
            elapsedTime = timestamp,
            voiceAction = pendingVoiceAction
        )
    }
    
    private fun handlePreparation(keypoints: FloatArray): PullUpFeedback? {
        val msg = when {
            !correctScale(keypoints) -> PullUpFeedback("Adjust grip width", FeedbackType.WARNING)
            !isArmSymmetric(keypoints, 30f) -> PullUpFeedback("Arms not balanced", FeedbackType.WARNING)
            !isVertical(keypoints) -> PullUpFeedback("Body tilted", FeedbackType.WARNING)
            !isStartPosition(keypoints) -> PullUpFeedback("Hands not in position", FeedbackType.WARNING)
            else -> {
                isReady = true
                state = PullUpState.DOWN
                pendingVoiceAction = VoiceAction.START
                return PullUpFeedback("Starting position confirmed. Ready to begin!", FeedbackType.SUCCESS)
            }
        }

        if (msg.message != lastHint && hintCooldown == 0) {
            lastHint = msg.message
            hintCooldown = 10
            return msg
        }

        return null
    }
    
    private fun handleStateTransitions(keypoints: FloatArray): PullUpFeedback? {
        return when {
            // DOWN -> UP
            state == PullUpState.DOWN && isPulling(keypoints) -> {
                state = PullUpState.UP
                validTop = false
                null
            }
            
            // UP -> TOP
            state == PullUpState.UP && isTopPosition(keypoints) -> {
                state = PullUpState.TOP
                if (!notHighEnough(keypoints)) {
                    validTop = true
                } else {
                    validTop = false
                    pendingVoiceAction = VoiceAction.NOT_HIGH
                }
                null
            }
            
            // TOP -> DOWN (completed rep)
            state == PullUpState.TOP && isStartPosition(keypoints) -> {
                val feedback = if (validTop) {
                    count++
                    validTop = false
                    state = PullUpState.DOWN
                    pendingVoiceAction = VoiceAction.SUCCESS
                    PullUpFeedback("Pull-up completed! Count: $count", FeedbackType.SUCCESS)
                } else {
                    validTop = false
                    state = PullUpState.DOWN
                    pendingVoiceAction = VoiceAction.FAIL
                    PullUpFeedback("Incomplete rep - not counted", FeedbackType.ERROR)
                }
                feedback
            }

            // UP -> DOWN (incomplete)
            state == PullUpState.UP && isStartPosition(keypoints) -> {
                state = PullUpState.DOWN
                validTop = false
                PullUpFeedback("Incomplete rep, resetting", FeedbackType.WARNING)
            }
            
            else -> null
        }
    }
    
    private fun handleErrorDetection(keypoints: FloatArray): PullUpFeedback? {
        // Swinging detection (only during UP and TOP states)
        if ((state == PullUpState.UP || state == PullUpState.TOP) && prevKeypoints != null) {
            if (isSwinging(prevKeypoints!!, keypoints)) {
                if (triggerWarning("swinging")) {
                    pendingVoiceAction = VoiceAction.SWINGING
                }
            } else {
                errorCounters["swinging"] = 0
            }
        }
        
        // Shrugging detection (only at TOP)
        if (state == PullUpState.TOP) {
            if (isShrugging(keypoints)) {
                if (triggerWarning("shrugging")) {
                    pendingVoiceAction = VoiceAction.SHRUGGING
                }
            } else {
                errorCounters["shrugging"] = 0
            }
        }
        
        // Not high enough detection (at TOP)
        if (state == PullUpState.TOP) {
            if (notHighEnough(keypoints)) {
                if (triggerWarning("not_high")) {
                    pendingVoiceAction = VoiceAction.NOT_HIGH
                }
            } else {
                errorCounters["not_high"] = 0
            }
        }
        
        return null
    }
    
    private fun triggerWarning(errorType: String): Boolean {
        if (errorCooldowns[errorType]!! > 0) return false
        
        errorCounters[errorType] = errorCounters[errorType]!! + 1
        
        if (errorCounters[errorType]!! >= confirmFrames) {
            errorCounters[errorType] = 0
            errorCooldowns[errorType] = cooldownFrames
            return true
        }
        return false
    }
    
    private fun calculateAccuracy(keypoints: FloatArray): Float {
        if (!isReady) return 0f
        
        val elbowAngle = minOf(
            getAngle(keypoints, LEFT_SHOULDER, LEFT_ELBOW, RIGHT_WRIST),
            getAngle(keypoints, RIGHT_SHOULDER, RIGHT_ELBOW, LEFT_WRIST)
        )
        
        return when (state) {
            PullUpState.PREPARING -> 50f
            PullUpState.DOWN -> 70f
            PullUpState.UP -> 85f
            PullUpState.TOP -> 95f
        }
    }
    
    fun reset() {
        state = PullUpState.PREPARING
        count = 0
        isReady = false
        validTop = false
        prevKeypoints = null
        lastHint = null
        hintCooldown = 0
        errorCounters = errorCounters.keys.associateWith { 0 }.toMutableMap()
        errorCooldowns = errorCooldowns.keys.associateWith { 0 }.toMutableMap()
    }
    
    // =========================
    // Core Detection Functions
    // =========================
    
    private fun getAngle(kp: FloatArray, idx1: Int, idx2: Int, idx3: Int): Float {
        val p1 = floatArrayOf(kp[idx1 * 3], kp[idx1 * 3 + 1], kp[idx1 * 3 + 2])
        val p2 = floatArrayOf(kp[idx2 * 3], kp[idx2 * 3 + 1], kp[idx2 * 3 + 2])
        val p3 = floatArrayOf(kp[idx3 * 3], kp[idx3 * 3 + 1], kp[idx3 * 3 + 2])
        
        val ba = floatArrayOf(p1[0] - p2[0], p1[1] - p2[1], p1[2] - p2[2])
        val bc = floatArrayOf(p3[0] - p2[0], p3[1] - p2[1], p3[2] - p2[2])
        
        val dotProduct = ba[0] * bc[0] + ba[1] * bc[1] + ba[2] * bc[2]
        val magBA = sqrt(ba[0] * ba[0] + ba[1] * ba[1] + ba[2] * ba[2])
        val magBC = sqrt(bc[0] * bc[0] + bc[1] * bc[1] + bc[2] * bc[2])
        
        val cosAngle = (dotProduct / (magBA * magBC)).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle)).toFloat()
    }
    
    private fun getDistance(kp: FloatArray, idx1: Int, idx2: Int): Float {
        val dx = kp[idx1 * 3] - kp[idx2 * 3]
        val dy = kp[idx1 * 3 + 1] - kp[idx2 * 3 + 1]
        val dz = kp[idx1 * 3 + 2] - kp[idx2 * 3 + 2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    private fun isArmSymmetric(kp: FloatArray, threshold: Float): Boolean {
        val shoulderL = floatArrayOf(kp[LEFT_SHOULDER * 3], kp[LEFT_SHOULDER * 3 + 1], kp[LEFT_SHOULDER * 3 + 2])
        val shoulderR = floatArrayOf(kp[RIGHT_SHOULDER * 3], kp[RIGHT_SHOULDER * 3 + 1], kp[RIGHT_SHOULDER * 3 + 2])
        val hipL = floatArrayOf(kp[LEFT_HIP * 3], kp[LEFT_HIP * 3 + 1], kp[LEFT_HIP * 3 + 2])
        val hipR = floatArrayOf(kp[RIGHT_HIP * 3], kp[RIGHT_HIP * 3 + 1], kp[RIGHT_HIP * 3 + 2])
        val wristL = floatArrayOf(kp[LEFT_WRIST * 3], kp[LEFT_WRIST * 3 + 1], kp[LEFT_WRIST * 3 + 2])
        val elbowL = floatArrayOf(kp[LEFT_ELBOW * 3], kp[LEFT_ELBOW * 3 + 1], kp[LEFT_ELBOW * 3 + 2])
        val elbowR = floatArrayOf(kp[RIGHT_ELBOW * 3], kp[RIGHT_ELBOW * 3 + 1], kp[RIGHT_ELBOW * 3 + 2])
        
        // Body axis
        var bodyAxis = floatArrayOf(
            (hipL[0] + hipR[0]) / 2 - (shoulderL[0] + shoulderR[0]) / 2,
            (hipL[1] + hipR[1]) / 2 - (shoulderL[1] + shoulderR[1]) / 2,
            (hipL[2] + hipR[2]) / 2 - (shoulderL[2] + shoulderR[2]) / 2
        )
        val bodyMag = sqrt(bodyAxis[0] * bodyAxis[0] + bodyAxis[1] * bodyAxis[1] + bodyAxis[2] * bodyAxis[2])
        if (bodyMag > 0) {
            bodyAxis = floatArrayOf(bodyAxis[0] / bodyMag, bodyAxis[1] / bodyMag, bodyAxis[2] / bodyMag)
        }
        
        // Upper arm directions
        val upperArmL = floatArrayOf(elbowL[0] - shoulderL[0], elbowL[1] - shoulderL[1], elbowL[2] - shoulderL[2])
        val upperArmR = floatArrayOf(elbowR[0] - shoulderR[0], elbowR[1] - shoulderR[1], elbowR[2] - shoulderR[2])
        
        val upperArmLMag = sqrt(upperArmL[0] * upperArmL[0] + upperArmL[1] * upperArmL[1] + upperArmL[2] * upperArmL[2])
        val upperArmRMag = sqrt(upperArmR[0] * upperArmR[0] + upperArmR[1] * upperArmR[1] + upperArmR[2] * upperArmR[2])
        
        if (upperArmLMag > 0 && upperArmRMag > 0) {
            val upperArmLN = floatArrayOf(upperArmL[0] / upperArmLMag, upperArmL[1] / upperArmLMag, upperArmL[2] / upperArmLMag)
            val upperArmRN = floatArrayOf(upperArmR[0] / upperArmRMag, upperArmR[1] / upperArmRMag, upperArmR[2] / upperArmRMag)
            
            val dotProdL = (upperArmLN[0] * bodyAxis[0] + upperArmLN[1] * bodyAxis[1] + upperArmLN[2] * bodyAxis[2]).toDouble().coerceIn(-1.0, 1.0)
            val dotProdR = (upperArmRN[0] * bodyAxis[0] + upperArmRN[1] * bodyAxis[1] + upperArmRN[2] * bodyAxis[2]).toDouble().coerceIn(-1.0, 1.0)
            val angleL = Math.toDegrees(acos(dotProdL)).toFloat()
            val angleR = Math.toDegrees(acos(dotProdR)).toFloat()
            
            return abs(angleL - angleR) < threshold
        }
        
        return false
    }
    
    private fun isStartPosition(kp: FloatArray): Boolean {
        val leftElbowAngle = getAngle(kp, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST)
        val rightElbowAngle = getAngle(kp, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST)
        return maxOf(leftElbowAngle, rightElbowAngle) >= pullUpType.startAngle
    }
    
    private fun isVertical(kp: FloatArray): Boolean {
        val leftShoulder = floatArrayOf(kp[LEFT_SHOULDER * 3], kp[LEFT_SHOULDER * 3 + 1], kp[LEFT_SHOULDER * 3 + 2])
        val rightShoulder = floatArrayOf(kp[RIGHT_SHOULDER * 3], kp[RIGHT_SHOULDER * 3 + 1], kp[RIGHT_SHOULDER * 3 + 2])
        val leftHip = floatArrayOf(kp[LEFT_HIP * 3], kp[LEFT_HIP * 3 + 1], kp[LEFT_HIP * 3 + 2])
        val rightHip = floatArrayOf(kp[RIGHT_HIP * 3], kp[RIGHT_HIP * 3 + 1], kp[RIGHT_HIP * 3 + 2])
        
        // Calculate angle between hip-shoulder line and a reference vertical line
        val dx = rightShoulder[0] - leftShoulder[0]
        val dy = rightShoulder[1] - leftShoulder[1]
        val shoulderAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        return abs(shoulderAngle) < 25f
    }
    
    private fun correctScale(kp: FloatArray): Boolean {
        val wristDistance = getDistance(kp, LEFT_WRIST, RIGHT_WRIST)
        val shoulderDistance = getDistance(kp, LEFT_SHOULDER, RIGHT_SHOULDER)
        
        if (shoulderDistance <= 0) return false
        
        val scale = wristDistance / shoulderDistance
        return scale >= pullUpType.gripMin && scale <= pullUpType.gripMax
    }
    
    private fun isPulling(kp: FloatArray): Boolean {
        val shoulderL = floatArrayOf(kp[LEFT_SHOULDER * 3], kp[LEFT_SHOULDER * 3 + 1], kp[LEFT_SHOULDER * 3 + 2])
        val shoulderR = floatArrayOf(kp[RIGHT_SHOULDER * 3], kp[RIGHT_SHOULDER * 3 + 1], kp[RIGHT_SHOULDER * 3 + 2])
        val hipL = floatArrayOf(kp[LEFT_HIP * 3], kp[LEFT_HIP * 3 + 1], kp[LEFT_HIP * 3 + 2])
        val hipR = floatArrayOf(kp[RIGHT_HIP * 3], kp[RIGHT_HIP * 3 + 1], kp[RIGHT_HIP * 3 + 2])
        val wristL = floatArrayOf(kp[LEFT_WRIST * 3], kp[LEFT_WRIST * 3 + 1], kp[LEFT_WRIST * 3 + 2])
        val wristR = floatArrayOf(kp[RIGHT_WRIST * 3], kp[RIGHT_WRIST * 3 + 1], kp[RIGHT_WRIST * 3 + 2])
        
        // Body axis
        var bodyAxis = floatArrayOf(
            (hipL[0] + hipR[0]) / 2 - (shoulderL[0] + shoulderR[0]) / 2,
            (hipL[1] + hipR[1]) / 2 - (shoulderL[1] + shoulderR[1]) / 2,
            (hipL[2] + hipR[2]) / 2 - (shoulderL[2] + shoulderR[2]) / 2
        )
        val bodyMag = sqrt(bodyAxis[0] * bodyAxis[0] + bodyAxis[1] * bodyAxis[1] + bodyAxis[2] * bodyAxis[2])
        if (bodyMag > 0) {
            bodyAxis = floatArrayOf(bodyAxis[0] / bodyMag, bodyAxis[1] / bodyMag, bodyAxis[2] / bodyMag)
        }
        
        // Wrist position relative to shoulder
        val wl = floatArrayOf(wristL[0] - shoulderL[0], wristL[1] - shoulderL[1], wristL[2] - shoulderL[2])
        val wr = floatArrayOf(wristR[0] - shoulderR[0], wristR[1] - shoulderR[1], wristR[2] - shoulderR[2])
        
        val projL = wl[0] * bodyAxis[0] + wl[1] * bodyAxis[1] + wl[2] * bodyAxis[2]
        val projR = wr[0] * bodyAxis[0] + wr[1] * bodyAxis[1] + wr[2] * bodyAxis[2]
        
        val wristUp = projL < 0 && projR < 0
        
        // Arm contraction
        val elbowL = getAngle(kp, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST)
        val elbowR = getAngle(kp, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST)
        val armContract = minOf(elbowL, elbowR) < 120f
        
        // Body stability
        var bodyStable = true
        prevKeypoints?.let { prev ->
            val prevHipL = floatArrayOf(prev[LEFT_HIP * 3], prev[LEFT_HIP * 3 + 1], prev[LEFT_HIP * 3 + 2])
            val prevHipR = floatArrayOf(prev[RIGHT_HIP * 3], prev[RIGHT_HIP * 3 + 1], prev[RIGHT_HIP * 3 + 2])
            
            val prevCenter = floatArrayOf((prevHipL[0] + prevHipR[0]) / 2, (prevHipL[1] + prevHipR[1]) / 2, (prevHipL[2] + prevHipR[2]) / 2)
            val currCenter = floatArrayOf((hipL[0] + hipR[0]) / 2, (hipL[1] + hipR[1]) / 2, (hipL[2] + hipR[2]) / 2)
            
            val shoulderWidth = getDistance(kp, LEFT_SHOULDER, RIGHT_SHOULDER) + 1e-6f
            val displacement = sqrt(
                (currCenter[0] - prevCenter[0]) * (currCenter[0] - prevCenter[0]) +
                (currCenter[1] - prevCenter[1]) * (currCenter[1] - prevCenter[1]) +
                (currCenter[2] - prevCenter[2]) * (currCenter[2] - prevCenter[2])
            )
            
            bodyStable = displacement / shoulderWidth < 0.3f
        }
        
        return wristUp && armContract && bodyStable
    }
    
    private fun isTopPosition(kp: FloatArray): Boolean {
        val head = floatArrayOf(kp[NOSE * 3], kp[NOSE * 3 + 1], kp[NOSE * 3 + 2])
        val wristL = floatArrayOf(kp[LEFT_WRIST * 3], kp[LEFT_WRIST * 3 + 1], kp[LEFT_WRIST * 3 + 2])
        val wristR = floatArrayOf(kp[RIGHT_WRIST * 3], kp[RIGHT_WRIST * 3 + 1], kp[RIGHT_WRIST * 3 + 2])
        val shoulderL = floatArrayOf(kp[LEFT_SHOULDER * 3], kp[LEFT_SHOULDER * 3 + 1], kp[LEFT_SHOULDER * 3 + 2])
        val shoulderR = floatArrayOf(kp[RIGHT_SHOULDER * 3], kp[RIGHT_SHOULDER * 3 + 1], kp[RIGHT_SHOULDER * 3 + 2])
        val hipL = floatArrayOf(kp[LEFT_HIP * 3], kp[LEFT_HIP * 3 + 1], kp[LEFT_HIP * 3 + 2])
        val hipR = floatArrayOf(kp[RIGHT_HIP * 3], kp[RIGHT_HIP * 3 + 1], kp[RIGHT_HIP * 3 + 2])
        
        // Body axis
        var bodyAxis = floatArrayOf(
            (hipL[0] + hipR[0]) / 2 - (shoulderL[0] + shoulderR[0]) / 2,
            (hipL[1] + hipR[1]) / 2 - (shoulderL[1] + shoulderR[1]) / 2,
            (hipL[2] + hipR[2]) / 2 - (shoulderL[2] + shoulderR[2]) / 2
        )
        val bodyMag = sqrt(bodyAxis[0] * bodyAxis[0] + bodyAxis[1] * bodyAxis[1] + bodyAxis[2] * bodyAxis[2])
        if (bodyMag > 0) {
            bodyAxis = floatArrayOf(bodyAxis[0] / bodyMag, bodyAxis[1] / bodyMag, bodyAxis[2] / bodyMag)
        }
        
        // Shoulder center as reference
        val shoulderCenter = floatArrayOf(
            (shoulderL[0] + shoulderR[0]) / 2,
            (shoulderL[1] + shoulderR[1]) / 2,
            (shoulderL[2] + shoulderR[2]) / 2
        )
        val wristCenter = floatArrayOf(
            (wristL[0] + wristR[0]) / 2,
            (wristL[1] + wristR[1]) / 2,
            (wristL[2] + wristR[2]) / 2
        )
        
        val noseProj = (head[0] - shoulderCenter[0]) * bodyAxis[0] +
                       (head[1] - shoulderCenter[1]) * bodyAxis[1] +
                       (head[2] - shoulderCenter[2]) * bodyAxis[2]
        
        val wristProj = (wristCenter[0] - shoulderCenter[0]) * bodyAxis[0] +
                        (wristCenter[1] - shoulderCenter[1]) * bodyAxis[1] +
                        (wristCenter[2] - shoulderCenter[2]) * bodyAxis[2]
        
        val shoulderWidth = getDistance(kp, LEFT_SHOULDER, RIGHT_SHOULDER) + 1e-6f
        val chinAboveWrist = (noseProj - wristProj) < 0.9f * shoulderWidth
        
        // Arm bending
        val leftElbow = getAngle(kp, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST)
        val rightElbow = getAngle(kp, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST)
        val armsBent = minOf(leftElbow, rightElbow) < 83f
        
        return chinAboveWrist && armsBent
    }
    
    private fun notHighEnough(kp: FloatArray): Boolean {
        val head = floatArrayOf(kp[NOSE * 3], kp[NOSE * 3 + 1], kp[NOSE * 3 + 2])
        val wristL = floatArrayOf(kp[LEFT_WRIST * 3], kp[LEFT_WRIST * 3 + 1], kp[LEFT_WRIST * 3 + 2])
        val wristR = floatArrayOf(kp[RIGHT_WRIST * 3], kp[RIGHT_WRIST * 3 + 1], kp[RIGHT_WRIST * 3 + 2])
        val shoulderL = floatArrayOf(kp[LEFT_SHOULDER * 3], kp[LEFT_SHOULDER * 3 + 1], kp[LEFT_SHOULDER * 3 + 2])
        val shoulderR = floatArrayOf(kp[RIGHT_SHOULDER * 3], kp[RIGHT_SHOULDER * 3 + 1], kp[RIGHT_SHOULDER * 3 + 2])
        val hipL = floatArrayOf(kp[LEFT_HIP * 3], kp[LEFT_HIP * 3 + 1], kp[LEFT_HIP * 3 + 2])
        val hipR = floatArrayOf(kp[RIGHT_HIP * 3], kp[RIGHT_HIP * 3 + 1], kp[RIGHT_HIP * 3 + 2])
        
        var bodyAxis = floatArrayOf(
            (hipL[0] + hipR[0]) / 2 - (shoulderL[0] + shoulderR[0]) / 2,
            (hipL[1] + hipR[1]) / 2 - (shoulderL[1] + shoulderR[1]) / 2,
            (hipL[2] + hipR[2]) / 2 - (shoulderL[2] + shoulderR[2]) / 2
        )
        val bodyMag = sqrt(bodyAxis[0] * bodyAxis[0] + bodyAxis[1] * bodyAxis[1] + bodyAxis[2] * bodyAxis[2])
        if (bodyMag > 0) {
            bodyAxis = floatArrayOf(bodyAxis[0] / bodyMag, bodyAxis[1] / bodyMag, bodyAxis[2] / bodyMag)
        }
        
        val shoulderCenter = floatArrayOf(
            (shoulderL[0] + shoulderR[0]) / 2,
            (shoulderL[1] + shoulderR[1]) / 2,
            (shoulderL[2] + shoulderR[2]) / 2
        )
        val wristCenter = floatArrayOf(
            (wristL[0] + wristR[0]) / 2,
            (wristL[1] + wristR[1]) / 2,
            (wristL[2] + wristR[2]) / 2
        )
        
        val headProj = (head[0] - shoulderCenter[0]) * bodyAxis[0] +
                       (head[1] - shoulderCenter[1]) * bodyAxis[1] +
                       (head[2] - shoulderCenter[2]) * bodyAxis[2]
        
        val wristProj = (wristCenter[0] - shoulderCenter[0]) * bodyAxis[0] +
                        (wristCenter[1] - shoulderCenter[1]) * bodyAxis[1] +
                        (wristCenter[2] - shoulderCenter[2]) * bodyAxis[2]
        
        val shoulderWidth = getDistance(kp, LEFT_SHOULDER, RIGHT_SHOULDER) + 1e-6f
        return (headProj - wristProj) > 1.0f * shoulderWidth
    }
    
    private fun isSwinging(kpPrev: FloatArray, kpNow: FloatArray): Boolean {
        val hipRPrev = floatArrayOf(kpPrev[RIGHT_HIP * 3], kpPrev[RIGHT_HIP * 3 + 1], kpPrev[RIGHT_HIP * 3 + 2])
        val hipLPrev = floatArrayOf(kpPrev[LEFT_HIP * 3], kpPrev[LEFT_HIP * 3 + 1], kpPrev[LEFT_HIP * 3 + 2])
        val hipRNow = floatArrayOf(kpNow[RIGHT_HIP * 3], kpNow[RIGHT_HIP * 3 + 1], kpNow[RIGHT_HIP * 3 + 2])
        val hipLNow = floatArrayOf(kpNow[LEFT_HIP * 3], kpNow[LEFT_HIP * 3 + 1], kpNow[LEFT_HIP * 3 + 2])
        
        val vPrev = floatArrayOf(hipRPrev[0] - hipLPrev[0], hipRPrev[1] - hipLPrev[1], hipRPrev[2] - hipLPrev[2])
        val vNow = floatArrayOf(hipRNow[0] - hipLNow[0], hipRNow[1] - hipLNow[1], hipRNow[2] - hipLNow[2])
        
        val vPrevMag = sqrt(vPrev[0] * vPrev[0] + vPrev[1] * vPrev[1] + vPrev[2] * vPrev[2])
        val vNowMag = sqrt(vNow[0] * vNow[0] + vNow[1] * vNow[1] + vNow[2] * vNow[2])
        
        if (vPrevMag <= 0 || vNowMag <= 0) return false
        
        val vPrevN = floatArrayOf(vPrev[0] / vPrevMag, vPrev[1] / vPrevMag, vPrev[2] / vPrevMag)
        val vNowN = floatArrayOf(vNow[0] / vNowMag, vNow[1] / vNowMag, vNow[2] / vNowMag)
        
        val cosAngle = (vPrevN[0] * vNowN[0] + vPrevN[1] * vNowN[1] + vPrevN[2] * vNowN[2]).toDouble().coerceIn(-1.0, 1.0)
        val angle = Math.toDegrees(acos(cosAngle)).toFloat()
        
        return angle > 10f
    }
    
    private fun isShrugging(kp: FloatArray): Boolean {
        val dis1 = getDistance(kp, LEFT_EAR, LEFT_SHOULDER)
        val dis2 = getDistance(kp, RIGHT_EAR, RIGHT_SHOULDER)
        val dis = getDistance(kp, LEFT_HIP, RIGHT_HIP) + 1e-6f
        
        val coef1 = dis1 / dis
        val coef2 = dis2 / dis
        
        return coef1 < coefLeftEarLeftShoulder * 0.9f && coef2 < coefRightEarRightShoulder * 0.9f
    }
}
