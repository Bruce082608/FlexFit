package com.example.flexfit.ml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

class ShoulderPressAnalyzer : ExerciseAnalyzer {
    override val exerciseName: String = "Shoulder Press"

    private enum class ShoulderPressState {
        PREPARING,
        RACK,
        PRESSING,
        LOCKOUT,
        RETURNING
    }

    private var state = ShoulderPressState.PREPARING
    private var count = 0
    private var attemptedReps = 0
    private var isReady = false
    private var validLockout = false
    private var currentRepBlocked = false
    private var minRackDelta = Float.POSITIVE_INFINITY
    private var maxWristAboveNose = Float.NEGATIVE_INFINITY
    private var maxElbowAngle = Float.NEGATIVE_INFINITY
    private var prevMetrics: ShoulderPressMetrics? = null
    private var lastPreparationHint: String? = null
    private var hintCooldown = 0
    private var pendingVoiceAction: VoiceAction? = null

    override fun analyze(keypoints: FloatArray, timestamp: Long): ExerciseAnalysisResult {
        pendingVoiceAction = null
        if (hintCooldown > 0) hintCooldown--

        if (!PoseKeypoints.isValid(keypoints) || !hasRequiredPoints(keypoints)) {
            val issue = ShoulderPressIssue.POSE_INCOMPLETE.toExerciseIssue()
            return buildResult(
                timestamp = timestamp,
                metrics = null,
                feedback = issue.toFeedback(),
                issues = listOf(issue)
            )
        }

        val metrics = buildMetrics(keypoints)
        val issues = mutableListOf<ExerciseIssue>()
        val feedback = if (!isReady) {
            handlePreparation(metrics, issues)
        } else {
            updateRepRange(metrics)
            val transitionFeedback = handleStateTransitions(metrics, issues)
            val errorFeedback = handleErrorDetection(metrics, issues)
            errorFeedback ?: transitionFeedback
        }

        prevMetrics = metrics

        return buildResult(
            timestamp = timestamp,
            metrics = metrics,
            feedback = feedback,
            issues = issues,
            voiceAction = pendingVoiceAction
        )
    }

    override fun mockFrame(frameCount: Long): FloatArray {
        return MockPoseSequence.shoulderPressFrame(frameCount)
    }

    override fun reset() {
        state = ShoulderPressState.PREPARING
        count = 0
        attemptedReps = 0
        isReady = false
        validLockout = false
        currentRepBlocked = false
        minRackDelta = Float.POSITIVE_INFINITY
        maxWristAboveNose = Float.NEGATIVE_INFINITY
        maxElbowAngle = Float.NEGATIVE_INFINITY
        prevMetrics = null
        lastPreparationHint = null
        hintCooldown = 0
        pendingVoiceAction = null
    }

    private fun handlePreparation(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback? {
        val issue = when {
            !metrics.isRackPosition -> ShoulderPressIssue.RANGE_OF_MOTION
            !metrics.isArmBalanced -> ShoulderPressIssue.LEFT_RIGHT_IMBALANCE
            !metrics.isTrunkStable -> ShoulderPressIssue.TRUNK_LEAN
            !metrics.isShoulderStable -> ShoulderPressIssue.SHOULDER_INSTABILITY
            else -> null
        }

        if (issue != null) {
            val exerciseIssue = issue.toExerciseIssue()
            issues.add(exerciseIssue)
            return throttledPreparationHint(exerciseIssue)
        }

        isReady = true
        state = ShoulderPressState.RACK
        resetCurrentRep(metrics)
        pendingVoiceAction = VoiceAction.START
        return ExerciseFeedback("Rack position confirmed. Ready to press!", FeedbackType.SUCCESS)
    }

    private fun handleStateTransitions(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback? {
        return when {
            state == ShoulderPressState.RACK && metrics.isPressing -> {
                state = ShoulderPressState.PRESSING
                resetCurrentRep(metrics)
                null
            }

            state == ShoulderPressState.PRESSING && metrics.isLockout -> {
                state = ShoulderPressState.LOCKOUT
                validLockout = true
                null
            }

            state == ShoulderPressState.PRESSING && metrics.isRackPosition -> {
                completeRep(metrics, issues)
            }

            state == ShoulderPressState.LOCKOUT && metrics.isReturning -> {
                state = ShoulderPressState.RETURNING
                null
            }

            state == ShoulderPressState.RETURNING && metrics.isRackPosition -> {
                completeRep(metrics, issues)
            }

            else -> null
        }
    }

    private fun handleErrorDetection(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback? {
        if (state == ShoulderPressState.PREPARING || state == ShoulderPressState.RACK) return null

        val issue = when {
            metrics.hasSevereArmImbalance -> ShoulderPressIssue.LEFT_RIGHT_IMBALANCE
            metrics.hasSevereTrunkLean -> ShoulderPressIssue.TRUNK_LEAN
            metrics.hasSevereShoulderInstability -> ShoulderPressIssue.SHOULDER_INSTABILITY
            else -> null
        } ?: return null

        currentRepBlocked = true
        val exerciseIssue = issue.toExerciseIssue()
        issues.add(exerciseIssue)
        return exerciseIssue.toFeedback()
    }

    private fun completeRep(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback {
        attemptedReps++
        val fullRange =
            validLockout &&
                maxWristAboveNose >= LOCKOUT_WRIST_ABOVE_NOSE &&
                maxElbowAngle >= LOCKOUT_ELBOW_ANGLE &&
                minRackDelta <= RACK_MAX_WRIST_ABOVE_SHOULDER

        return if (fullRange && !currentRepBlocked) {
            count++
            state = ShoulderPressState.RACK
            resetCurrentRep(metrics)
            pendingVoiceAction = VoiceAction.SUCCESS
            ExerciseFeedback("Shoulder press completed! Count: $count", FeedbackType.SUCCESS)
        } else {
            val issue = ShoulderPressIssue.RANGE_OF_MOTION.toExerciseIssue()
            issues.add(issue)
            state = ShoulderPressState.RACK
            resetCurrentRep(metrics)
            pendingVoiceAction = VoiceAction.FAIL
            issue.toFeedback()
        }
    }

    private fun resetCurrentRep(metrics: ShoulderPressMetrics) {
        validLockout = false
        currentRepBlocked = false
        minRackDelta = metrics.wristAboveShoulder
        maxWristAboveNose = metrics.wristAboveNose
        maxElbowAngle = metrics.averageElbowAngle
    }

    private fun updateRepRange(metrics: ShoulderPressMetrics) {
        minRackDelta = minOf(minRackDelta, metrics.wristAboveShoulder)
        maxWristAboveNose = max(maxWristAboveNose, metrics.wristAboveNose)
        maxElbowAngle = max(maxElbowAngle, metrics.averageElbowAngle)
    }

    private fun buildResult(
        timestamp: Long,
        metrics: ShoulderPressMetrics?,
        feedback: ExerciseFeedback?,
        issues: List<ExerciseIssue>,
        voiceAction: VoiceAction? = null
    ): ExerciseAnalysisResult {
        val scores = metrics?.let { calculateScores(it) } ?: ExerciseScoreBreakdown()

        return ExerciseAnalysisResult(
            count = count,
            phase = state.toExercisePhase(),
            isReady = isReady,
            feedback = feedback,
            accuracy = scores.total,
            scores = scores,
            issues = issues,
            attemptedReps = attemptedReps.coerceAtLeast(count),
            elapsedTime = timestamp,
            voiceAction = voiceAction
        )
    }

    private fun calculateScores(metrics: ShoulderPressMetrics): ExerciseScoreBreakdown {
        val depth = if (!isReady) {
            0f
        } else {
            val heightScore = ((max(metrics.wristAboveNose, maxWristAboveNose) + 0.18f) /
                (LOCKOUT_WRIST_ABOVE_NOSE + 0.18f) * 100f).coerceIn(0f, 100f)
            val extensionScore = ((max(metrics.averageElbowAngle, maxElbowAngle) - 65f) /
                (LOCKOUT_ELBOW_ANGLE - 65f) * 100f).coerceIn(0f, 100f)
            val rackScore = if (minRackDelta <= RACK_MAX_WRIST_ABOVE_SHOULDER) 100f else 70f
            (heightScore * 0.50f + extensionScore * 0.35f + rackScore * 0.15f).coerceIn(0f, 100f)
        }

        return ExerciseScoreBreakdown(
            depth = depth,
            alignment = scoreAlignment(metrics),
            stability = scoreStability(metrics)
        )
    }

    private fun scoreAlignment(metrics: ShoulderPressMetrics): Float {
        val wristPenalty = metrics.wristHeightDeltaRatio * 130f
        val elbowPenalty = metrics.elbowAngleDelta * 1.1f
        val stackPenalty = metrics.forearmStackRatio * 70f
        return (100f - wristPenalty - elbowPenalty - stackPenalty).coerceIn(0f, 100f)
    }

    private fun scoreStability(metrics: ShoulderPressMetrics): Float {
        val trunkPenalty = metrics.trunkLeanRatio * 120f
        val shoulderBalancePenalty = metrics.shoulderBalanceRatio * 130f
        val shoulderDriftPenalty = metrics.shoulderDriftRatio * 160f
        return (100f - trunkPenalty - shoulderBalancePenalty - shoulderDriftPenalty).coerceIn(0f, 100f)
    }

    private fun throttledPreparationHint(issue: ExerciseIssue): ExerciseFeedback? {
        if (issue.label == lastPreparationHint || hintCooldown > 0) return null
        lastPreparationHint = issue.label
        hintCooldown = PREPARATION_HINT_COOLDOWN_FRAMES
        return issue.toFeedback()
    }

    private fun ShoulderPressState.toExercisePhase(): ExercisePhase {
        return when (this) {
            ShoulderPressState.PREPARING -> ExercisePhase("shoulder_press_preparing", "Ready", ExercisePhaseTone.NEUTRAL)
            ShoulderPressState.RACK -> ExercisePhase("shoulder_press_rack", "Rack Position", ExercisePhaseTone.SUCCESS)
            ShoulderPressState.PRESSING -> ExercisePhase("shoulder_press_pressing", "Pressing", ExercisePhaseTone.ACTIVE)
            ShoulderPressState.LOCKOUT -> ExercisePhase("shoulder_press_lockout", "Lockout", ExercisePhaseTone.SUCCESS)
            ShoulderPressState.RETURNING -> ExercisePhase("shoulder_press_returning", "Return to Rack", ExercisePhaseTone.ACTIVE)
        }
    }

    private fun hasRequiredPoints(keypoints: FloatArray): Boolean {
        return REQUIRED_LANDMARKS.all { PoseKeypoints.hasPoint(keypoints, it) }
    }

    private data class ShoulderPressMetrics(
        val shoulderWidth: Float,
        val leftElbowAngle: Float,
        val rightElbowAngle: Float,
        val averageElbowAngle: Float,
        val elbowAngleDelta: Float,
        val wristHeightDeltaRatio: Float,
        val wristAboveShoulder: Float,
        val wristAboveNose: Float,
        val forearmStackRatio: Float,
        val trunkLeanRatio: Float,
        val shoulderBalanceRatio: Float,
        val shoulderDriftRatio: Float,
        val shoulderCenterY: Float,
        val isRackPosition: Boolean,
        val isPressing: Boolean,
        val isLockout: Boolean,
        val isReturning: Boolean,
        val isArmBalanced: Boolean,
        val isTrunkStable: Boolean,
        val isShoulderStable: Boolean,
        val hasSevereArmImbalance: Boolean,
        val hasSevereTrunkLean: Boolean,
        val hasSevereShoulderInstability: Boolean
    )

    private fun buildMetrics(keypoints: FloatArray): ShoulderPressMetrics {
        val shoulderWidth = distance(keypoints, LEFT_SHOULDER, RIGHT_SHOULDER).coerceAtLeast(EPSILON)
        val shoulderCenter = center(keypoints, LEFT_SHOULDER, RIGHT_SHOULDER)
        val wristCenter = center(keypoints, LEFT_WRIST, RIGHT_WRIST)
        val hipCenter = center(keypoints, LEFT_HIP, RIGHT_HIP)
        val leftElbowAngle = angle(keypoints, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST)
        val rightElbowAngle = angle(keypoints, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST)
        val averageElbowAngle = (leftElbowAngle + rightElbowAngle) / 2f
        val elbowAngleDelta = abs(leftElbowAngle - rightElbowAngle)
        val wristHeightDeltaRatio = abs(keypoints[LEFT_WRIST.y] - keypoints[RIGHT_WRIST.y]) / shoulderWidth
        val wristAboveShoulder = wristCenter.y - shoulderCenter.y
        val wristAboveNose = wristCenter.y - keypoints[NOSE.y]
        val leftStack = abs(keypoints[LEFT_WRIST.x] - keypoints[LEFT_ELBOW.x])
        val rightStack = abs(keypoints[RIGHT_WRIST.x] - keypoints[RIGHT_ELBOW.x])
        val forearmStackRatio = (leftStack + rightStack) / 2f / shoulderWidth
        val trunkLeanRatio = abs(shoulderCenter.x - hipCenter.x) / shoulderWidth +
            abs(shoulderCenter.z - hipCenter.z) * 0.45f
        val shoulderBalanceRatio = abs(keypoints[LEFT_SHOULDER.y] - keypoints[RIGHT_SHOULDER.y]) / shoulderWidth
        val shoulderDriftRatio = prevMetrics?.let { previous ->
            abs(shoulderCenter.y - previous.shoulderCenterY) / shoulderWidth
        } ?: 0f

        val isRackPosition =
            wristAboveShoulder in RACK_MIN_WRIST_ABOVE_SHOULDER..RACK_MAX_WRIST_ABOVE_SHOULDER &&
                averageElbowAngle in RACK_MIN_ELBOW_ANGLE..RACK_MAX_ELBOW_ANGLE
        val isPressing = wristAboveShoulder >= PRESSING_WRIST_ABOVE_SHOULDER
        val isLockout = wristAboveNose >= LOCKOUT_WRIST_ABOVE_NOSE && averageElbowAngle >= LOCKOUT_ELBOW_ANGLE
        val isReturning = wristAboveNose < RETURNING_WRIST_ABOVE_NOSE || averageElbowAngle < RETURNING_ELBOW_ANGLE
        val isArmBalanced =
            wristHeightDeltaRatio <= WRIST_HEIGHT_WARNING_RATIO &&
                elbowAngleDelta <= ELBOW_DELTA_WARNING_DEGREES &&
                forearmStackRatio <= FOREARM_STACK_WARNING_RATIO
        val isTrunkStable = trunkLeanRatio <= TRUNK_LEAN_WARNING_RATIO
        val isShoulderStable =
            shoulderBalanceRatio <= SHOULDER_BALANCE_WARNING_RATIO &&
                shoulderDriftRatio <= SHOULDER_DRIFT_WARNING_RATIO

        return ShoulderPressMetrics(
            shoulderWidth = shoulderWidth,
            leftElbowAngle = leftElbowAngle,
            rightElbowAngle = rightElbowAngle,
            averageElbowAngle = averageElbowAngle,
            elbowAngleDelta = elbowAngleDelta,
            wristHeightDeltaRatio = wristHeightDeltaRatio,
            wristAboveShoulder = wristAboveShoulder,
            wristAboveNose = wristAboveNose,
            forearmStackRatio = forearmStackRatio,
            trunkLeanRatio = trunkLeanRatio,
            shoulderBalanceRatio = shoulderBalanceRatio,
            shoulderDriftRatio = shoulderDriftRatio,
            shoulderCenterY = shoulderCenter.y,
            isRackPosition = isRackPosition,
            isPressing = isPressing,
            isLockout = isLockout,
            isReturning = isReturning,
            isArmBalanced = isArmBalanced,
            isTrunkStable = isTrunkStable,
            isShoulderStable = isShoulderStable,
            hasSevereArmImbalance =
                wristHeightDeltaRatio >= WRIST_HEIGHT_ERROR_RATIO ||
                    elbowAngleDelta >= ELBOW_DELTA_ERROR_DEGREES ||
                    forearmStackRatio >= FOREARM_STACK_ERROR_RATIO,
            hasSevereTrunkLean = trunkLeanRatio >= TRUNK_LEAN_ERROR_RATIO,
            hasSevereShoulderInstability =
                shoulderBalanceRatio >= SHOULDER_BALANCE_ERROR_RATIO ||
                    shoulderDriftRatio >= SHOULDER_DRIFT_ERROR_RATIO
        )
    }

    private enum class ShoulderPressIssue(
        val key: String,
        val label: String,
        val severity: FeedbackType,
        val suggestion: String
    ) {
        RANGE_OF_MOTION(
            "range_of_motion",
            "Use full shoulder press range",
            FeedbackType.ERROR,
            "Lower dumbbells to shoulder height and press to a clear overhead lockout."
        ),
        LEFT_RIGHT_IMBALANCE(
            "left_right_imbalance",
            "Keep both arms synchronized",
            FeedbackType.WARNING,
            "Press both dumbbells evenly so wrist height and elbow angle stay matched."
        ),
        TRUNK_LEAN(
            "trunk_lean",
            "Keep your torso upright",
            FeedbackType.WARNING,
            "Brace your core and avoid leaning back or shifting to one side."
        ),
        SHOULDER_INSTABILITY(
            "shoulder_instability",
            "Stabilize your shoulders",
            FeedbackType.WARNING,
            "Keep shoulder height level and avoid shrugging or wobbling during the press."
        ),
        POSE_INCOMPLETE(
            "pose_incomplete",
            "Pose data incomplete",
            FeedbackType.ERROR,
            "Keep your head, shoulders, elbows, wrists, and hips in the camera frame."
        )
    }

    private fun ShoulderPressIssue.toExerciseIssue(): ExerciseIssue {
        return ExerciseIssue(
            key = key,
            label = label,
            severity = severity,
            suggestion = suggestion
        )
    }

    private fun ExerciseIssue.toFeedback(): ExerciseFeedback {
        return ExerciseFeedback(label, severity)
    }

    private data class Point(val x: Float, val y: Float, val z: Float)

    private fun center(keypoints: FloatArray, first: Int, second: Int): Point {
        val a = point(keypoints, first)
        val b = point(keypoints, second)
        return Point((a.x + b.x) / 2f, (a.y + b.y) / 2f, (a.z + b.z) / 2f)
    }

    private fun point(keypoints: FloatArray, index: Int): Point {
        return Point(keypoints[index.x], keypoints[index.y], keypoints[index.z])
    }

    private fun distance(keypoints: FloatArray, first: Int, second: Int): Float {
        val a = point(keypoints, first)
        val b = point(keypoints, second)
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun angle(keypoints: FloatArray, first: Int, middle: Int, last: Int): Float {
        val a = point(keypoints, first)
        val b = point(keypoints, middle)
        val c = point(keypoints, last)

        val baX = a.x - b.x
        val baY = a.y - b.y
        val baZ = a.z - b.z
        val bcX = c.x - b.x
        val bcY = c.y - b.y
        val bcZ = c.z - b.z

        val dot = baX * bcX + baY * bcY + baZ * bcZ
        val magBA = sqrt(baX * baX + baY * baY + baZ * baZ)
        val magBC = sqrt(bcX * bcX + bcY * bcY + bcZ * bcZ)
        if (magBA <= EPSILON || magBC <= EPSILON) return 0f

        val cosAngle = (dot / (magBA * magBC)).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle)).toFloat()
    }

    private val Int.x: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK
    private val Int.y: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK + 1
    private val Int.z: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK + 2

    private companion object {
        const val NOSE = 0
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_ELBOW = 13
        const val RIGHT_ELBOW = 14
        const val LEFT_WRIST = 15
        const val RIGHT_WRIST = 16
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24

        val REQUIRED_LANDMARKS = intArrayOf(
            NOSE,
            LEFT_SHOULDER,
            RIGHT_SHOULDER,
            LEFT_ELBOW,
            RIGHT_ELBOW,
            LEFT_WRIST,
            RIGHT_WRIST,
            LEFT_HIP,
            RIGHT_HIP
        )

        const val EPSILON = 1e-6f
        const val RACK_MIN_WRIST_ABOVE_SHOULDER = -0.08f
        const val RACK_MAX_WRIST_ABOVE_SHOULDER = 0.20f
        const val RACK_MIN_ELBOW_ANGLE = 35f
        const val RACK_MAX_ELBOW_ANGLE = 125f
        const val PRESSING_WRIST_ABOVE_SHOULDER = 0.28f
        const val LOCKOUT_WRIST_ABOVE_NOSE = 0.08f
        const val LOCKOUT_ELBOW_ANGLE = 145f
        const val RETURNING_WRIST_ABOVE_NOSE = -0.05f
        const val RETURNING_ELBOW_ANGLE = 135f
        const val WRIST_HEIGHT_WARNING_RATIO = 0.20f
        const val WRIST_HEIGHT_ERROR_RATIO = 0.30f
        const val ELBOW_DELTA_WARNING_DEGREES = 24f
        const val ELBOW_DELTA_ERROR_DEGREES = 36f
        const val FOREARM_STACK_WARNING_RATIO = 0.35f
        const val FOREARM_STACK_ERROR_RATIO = 0.48f
        const val TRUNK_LEAN_WARNING_RATIO = 0.30f
        const val TRUNK_LEAN_ERROR_RATIO = 0.45f
        const val SHOULDER_BALANCE_WARNING_RATIO = 0.18f
        const val SHOULDER_BALANCE_ERROR_RATIO = 0.28f
        const val SHOULDER_DRIFT_WARNING_RATIO = 0.16f
        const val SHOULDER_DRIFT_ERROR_RATIO = 0.25f
        const val PREPARATION_HINT_COOLDOWN_FRAMES = 10
    }
}
