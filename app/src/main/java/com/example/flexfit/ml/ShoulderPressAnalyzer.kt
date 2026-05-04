package com.example.flexfit.ml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import java.util.Locale

class ShoulderPressAnalyzer : ExerciseAnalyzer, ExerciseDebugProvider {
    override val exerciseName: String = "Shoulder Press"

    private enum class ShoulderPressState {
        PREPARING,
        DOWN,
        UP,
        TOP
    }

    private var state = ShoulderPressState.PREPARING
    private var count = 0
    private var attemptedReps = 0
    private var isReady = false
    private var validTop = false
    private var maxElbowAngle = Float.NEGATIVE_INFINITY
    private var minStartDelta = Float.POSITIVE_INFINITY
    private var prevMetrics: ShoulderPressMetrics? = null
    private var calibratedLeftEarShoulderHipRatio: Float? = null
    private var calibratedRightEarShoulderHipRatio: Float? = null
    private var lastMetrics: ShoulderPressMetrics? = null
    private var lastPreparationHint: String? = null
    private var hintCooldown = 0
    private var pendingVoiceAction: VoiceAction? = null
    private var issueCounters = ShoulderPressIssue.entries.associateWith { 0 }.toMutableMap()
    private var issueCooldowns = ShoulderPressIssue.entries.associateWith { 0 }.toMutableMap()

    override fun analyze(keypoints: FloatArray, timestamp: Long): ExerciseAnalysisResult {
        pendingVoiceAction = null
        decrementCooldowns()
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
        lastMetrics = metrics
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

    override fun debugSnapshot(): ExerciseDebugSnapshot? {
        val metrics = lastMetrics ?: return null
        return ExerciseDebugSnapshot(
            title = "Shoulder Press Debug",
            values = listOf(
                debugValue("Ready", if (isReady) "yes" else "no", "start confirmed", isReady),
                debugValue("State", state.name.lowercase(), "preparing/down/up/top", true),
                debugValue("Left elbow", metrics.leftElbowAngle.degrees(), "88-95 start, >150 top, >160 lockout", metrics.isStartPosition || metrics.isTopPosition),
                debugValue("Right elbow", metrics.rightElbowAngle.degrees(), "88-95 start, >150 top, >160 lockout", metrics.isStartPosition || metrics.isTopPosition),
                debugValue("Grip ratio", metrics.gripRatio.decimal(), "1.8-2.1", metrics.isGripCorrect),
                debugValue("Arm symmetry", metrics.armSymmetryDelta.degrees(), "<30", metrics.isArmSymmetric),
                debugValue("Body upright", metrics.bodyUprightDelta.degrees(), "<38", metrics.isBodyUpright),
                debugValue("Elbow flare", metrics.elbowFlareRatio.decimal(), "<=2.1", !metrics.hasElbowFlare),
                debugValue("Left shrug", metrics.leftEarShoulderHipRatio.decimal(), ">=${metrics.leftShrugThreshold.decimal()}", !metrics.isShrugging),
                debugValue("Right shrug", metrics.rightEarShoulderHipRatio.decimal(), ">=${metrics.rightShrugThreshold.decimal()}", !metrics.isShrugging),
                debugValue("Wrist angle L/R", "${metrics.leftForearmAngle.degrees()} / ${metrics.rightForearmAngle.degrees()}", "74-80", !metrics.hasBadWrist),
                debugValue("Body lean", metrics.bodyLeanAngle.degrees(), "<=38", !metrics.isBodyLeaning)
            )
        )
    }

    override fun reset() {
        state = ShoulderPressState.PREPARING
        count = 0
        attemptedReps = 0
        isReady = false
        validTop = false
        maxElbowAngle = Float.NEGATIVE_INFINITY
        minStartDelta = Float.POSITIVE_INFINITY
        prevMetrics = null
        calibratedLeftEarShoulderHipRatio = null
        calibratedRightEarShoulderHipRatio = null
        lastMetrics = null
        lastPreparationHint = null
        hintCooldown = 0
        pendingVoiceAction = null
        issueCounters = ShoulderPressIssue.entries.associateWith { 0 }.toMutableMap()
        issueCooldowns = ShoulderPressIssue.entries.associateWith { 0 }.toMutableMap()
    }

    private fun handlePreparation(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback? {
        val issue = when {
            !metrics.isGripCorrect -> ShoulderPressIssue.ADJUST_GRIP
            !metrics.isArmSymmetric -> ShoulderPressIssue.ARMS_BALANCE
            !metrics.isBodyUpright -> ShoulderPressIssue.BODY_UPRIGHT
            !metrics.isStartPosition -> ShoulderPressIssue.START_POSITION
            else -> null
        }

        if (issue != null) {
            val exerciseIssue = issue.toExerciseIssue()
            issues.add(exerciseIssue)
            return throttledPreparationHint(exerciseIssue)?.also {
                pendingVoiceAction = issue.voiceAction
            }
        }

        isReady = true
        state = ShoulderPressState.DOWN
        calibratedLeftEarShoulderHipRatio = metrics.leftEarShoulderHipRatio
        calibratedRightEarShoulderHipRatio = metrics.rightEarShoulderHipRatio
        resetCurrentRep(metrics)
        pendingVoiceAction = VoiceAction.SHP_START
        return ExerciseFeedback("Start position confirmed. Ready to press!", FeedbackType.SUCCESS)
    }

    private fun handleStateTransitions(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback? {
        return when {
            state == ShoulderPressState.DOWN && metrics.isPushing -> {
                state = ShoulderPressState.UP
                validTop = false
                resetCurrentRep(metrics)
                null
            }

            state == ShoulderPressState.UP && metrics.isTopPosition -> {
                state = ShoulderPressState.TOP
                validTop = metrics.isLockoutValid
                if (!validTop) {
                    val feedback = emitIssue(ShoulderPressIssue.NOT_HIGH, issues, requireConfirmation = true)
                    if (feedback != null) pendingVoiceAction = VoiceAction.SHP_NOT_HIGH
                    feedback
                } else {
                    null
                }
            }

            state == ShoulderPressState.UP && metrics.isStartPosition -> {
                completeRep(metrics, issues)
            }

            state == ShoulderPressState.TOP && metrics.isStartPosition -> {
                completeRep(metrics, issues)
            }

            else -> null
        }
    }

    private fun handleErrorDetection(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback? {
        if (state != ShoulderPressState.UP && state != ShoulderPressState.TOP) return null

        val issue = when {
            metrics.isShrugging -> ShoulderPressIssue.SHRUGGING
            metrics.hasElbowFlare -> ShoulderPressIssue.ELBOW_FLARE
            metrics.isBodyLeaning -> ShoulderPressIssue.BODY_LEAN
            state == ShoulderPressState.UP && metrics.hasBadWrist -> ShoulderPressIssue.BAD_WRIST
            state == ShoulderPressState.TOP && metrics.notLockedOut -> ShoulderPressIssue.NOT_HIGH
            else -> null
        }

        if (issue == null) {
            resetIssueCounter(ShoulderPressIssue.SHRUGGING)
            resetIssueCounter(ShoulderPressIssue.ELBOW_FLARE)
            resetIssueCounter(ShoulderPressIssue.BAD_WRIST)
            resetIssueCounter(ShoulderPressIssue.NOT_HIGH)
            resetIssueCounter(ShoulderPressIssue.BODY_LEAN)
            return null
        }

        val feedback = emitIssue(issue, issues, requireConfirmation = true)
        if (feedback != null) {
            pendingVoiceAction = issue.voiceAction
        }
        return feedback
    }

    private fun completeRep(
        metrics: ShoulderPressMetrics,
        issues: MutableList<ExerciseIssue>
    ): ExerciseFeedback {
        attemptedReps++
        val fullRange = validTop && maxElbowAngle >= LOCKOUT_VALID_ELBOW_ANGLE &&
            minStartDelta <= START_POSITION_MAX_DELTA

        return if (fullRange) {
            count++
            state = ShoulderPressState.DOWN
            resetCurrentRep(metrics)
            pendingVoiceAction = VoiceAction.SHP_SUCCESS
            ExerciseFeedback("Shoulder press completed! Count: $count", FeedbackType.SUCCESS)
        } else {
            val issue = ShoulderPressIssue.NOT_HIGH.toExerciseIssue()
            issues.add(issue)
            state = ShoulderPressState.DOWN
            resetCurrentRep(metrics)
            pendingVoiceAction = VoiceAction.SHP_FAIL
            issue.toFeedback()
        }
    }

    private fun resetCurrentRep(metrics: ShoulderPressMetrics) {
        validTop = false
        maxElbowAngle = metrics.minElbowAngle
        minStartDelta = metrics.startPositionDelta
    }

    private fun updateRepRange(metrics: ShoulderPressMetrics) {
        maxElbowAngle = max(maxElbowAngle, metrics.minElbowAngle)
        minStartDelta = min(minStartDelta, metrics.startPositionDelta)
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
            val topScore = ((max(metrics.minElbowAngle, maxElbowAngle) - START_MAX_ELBOW_ANGLE) /
                (LOCKOUT_VALID_ELBOW_ANGLE - START_MAX_ELBOW_ANGLE) * 100f).coerceIn(0f, 100f)
            val startScore = if (minStartDelta <= START_POSITION_MAX_DELTA) 100f else 72f
            (topScore * 0.82f + startScore * 0.18f).coerceIn(0f, 100f)
        }

        return ExerciseScoreBreakdown(
            depth = depth,
            alignment = scoreAlignment(metrics),
            stability = scoreStability(metrics)
        )
    }

    private fun scoreAlignment(metrics: ShoulderPressMetrics): Float {
        val gripPenalty = abs(metrics.gripRatio - TARGET_GRIP_RATIO) * 48f
        val armPenalty = max(0f, metrics.armSymmetryDelta - ARM_SYMMETRY_WARNING_DEGREES) * 1.4f
        val elbowPenalty = max(0f, metrics.elbowFlareRatio - ELBOW_FLARE_RATIO) * 90f
        val wristPenalty = if (metrics.hasBadWrist) 16f else 0f
        return (100f - gripPenalty - armPenalty - elbowPenalty - wristPenalty).coerceIn(0f, 100f)
    }

    private fun scoreStability(metrics: ShoulderPressMetrics): Float {
        val bodyLeanPenalty = max(0f, metrics.bodyLeanAngle - BODY_LEAN_ANGLE) * 2.0f
        val uprightPenalty = max(0f, metrics.bodyUprightDelta - BODY_UPRIGHT_ANGLE) * 1.4f
        val shrugPenalty = if (metrics.isShrugging) 18f else 0f
        return (100f - bodyLeanPenalty - uprightPenalty - shrugPenalty).coerceIn(0f, 100f)
    }

    private fun emitIssue(
        issue: ShoulderPressIssue,
        issues: MutableList<ExerciseIssue>,
        requireConfirmation: Boolean
    ): ExerciseFeedback? {
        if (requireConfirmation && !triggerIssue(issue)) return null

        val exerciseIssue = issue.toExerciseIssue()
        issues.add(exerciseIssue)
        return exerciseIssue.toFeedback()
    }

    private fun throttledPreparationHint(issue: ExerciseIssue): ExerciseFeedback? {
        if (issue.label == lastPreparationHint || hintCooldown > 0) return null
        lastPreparationHint = issue.label
        hintCooldown = PREPARATION_HINT_COOLDOWN_FRAMES
        return issue.toFeedback()
    }

    private fun triggerIssue(issue: ShoulderPressIssue): Boolean {
        val cooldown = issueCooldowns[issue] ?: 0
        if (cooldown > 0) return false

        val count = (issueCounters[issue] ?: 0) + 1
        issueCounters[issue] = count

        if (count < CONFIRM_FRAMES) return false

        issueCounters[issue] = 0
        issueCooldowns[issue] = COOLDOWN_FRAMES
        return true
    }

    private fun resetIssueCounter(issue: ShoulderPressIssue) {
        issueCounters[issue] = 0
    }

    private fun decrementCooldowns() {
        issueCooldowns.keys.forEach { issue ->
            val cooldown = issueCooldowns[issue] ?: 0
            if (cooldown > 0) {
                issueCooldowns[issue] = cooldown - 1
            }
        }
    }

    private fun ShoulderPressState.toExercisePhase(): ExercisePhase {
        return when (this) {
            ShoulderPressState.PREPARING -> ExercisePhase("shoulder_press_preparing", "Ready", ExercisePhaseTone.NEUTRAL)
            ShoulderPressState.DOWN -> ExercisePhase("shoulder_press_down", "Start Position", ExercisePhaseTone.SUCCESS)
            ShoulderPressState.UP -> ExercisePhase("shoulder_press_up", "Pressing", ExercisePhaseTone.ACTIVE)
            ShoulderPressState.TOP -> ExercisePhase("shoulder_press_top", "Lockout", ExercisePhaseTone.SUCCESS)
        }
    }

    private fun hasRequiredPoints(keypoints: FloatArray): Boolean {
        return REQUIRED_LANDMARKS.all { PoseKeypoints.hasPoint(keypoints, it) }
    }

    private data class ShoulderPressMetrics(
        val gripRatio: Float,
        val leftElbowAngle: Float,
        val rightElbowAngle: Float,
        val minElbowAngle: Float,
        val startPositionDelta: Float,
        val elbowFlareRatio: Float,
        val armSymmetryDelta: Float,
        val leftEarShoulderHipRatio: Float,
        val rightEarShoulderHipRatio: Float,
        val leftShrugThreshold: Float,
        val rightShrugThreshold: Float,
        val leftForearmAngle: Float,
        val rightForearmAngle: Float,
        val bodyUprightDelta: Float,
        val bodyLeanAngle: Float,
        val isGripCorrect: Boolean,
        val isArmSymmetric: Boolean,
        val isBodyUpright: Boolean,
        val isStartPosition: Boolean,
        val isPushing: Boolean,
        val isTopPosition: Boolean,
        val isLockoutValid: Boolean,
        val notLockedOut: Boolean,
        val isShrugging: Boolean,
        val hasElbowFlare: Boolean,
        val hasBadWrist: Boolean,
        val isBodyLeaning: Boolean
    )

    private fun buildMetrics(keypoints: FloatArray): ShoulderPressMetrics {
        val leftShoulder = point(keypoints, LEFT_SHOULDER)
        val rightShoulder = point(keypoints, RIGHT_SHOULDER)
        val leftElbow = point(keypoints, LEFT_ELBOW)
        val rightElbow = point(keypoints, RIGHT_ELBOW)
        val leftWrist = point(keypoints, LEFT_WRIST)
        val rightWrist = point(keypoints, RIGHT_WRIST)
        val leftHip = point(keypoints, LEFT_HIP)
        val rightHip = point(keypoints, RIGHT_HIP)
        val shoulderWidth = distance(leftShoulder, rightShoulder).coerceAtLeast(EPSILON)
        val hipWidth = distance(leftHip, rightHip).coerceAtLeast(EPSILON)
        val gripRatio = distance(leftWrist, rightWrist) / shoulderWidth
        val leftElbowAngle = angle(leftShoulder, leftElbow, leftWrist)
        val rightElbowAngle = angle(rightShoulder, rightElbow, rightWrist)
        val minElbowAngle = min(leftElbowAngle, rightElbowAngle)
        val startPositionDelta = max(
            abs(leftElbowAngle - START_TARGET_ELBOW_ANGLE),
            abs(rightElbowAngle - START_TARGET_ELBOW_ANGLE)
        )
        val elbowFlareRatio = distance(leftElbow, rightElbow) / shoulderWidth
        val bodyAxis = (center(leftHip, rightHip) - center(leftShoulder, rightShoulder)).normalized()
        val leftUpperArm = (leftElbow - leftShoulder).normalized()
        val rightUpperArm = (rightElbow - rightShoulder).normalized()
        val leftArmAxisAngle = angleBetween(leftUpperArm, bodyAxis)
        val rightArmAxisAngle = angleBetween(rightUpperArm, bodyAxis)
        val armSymmetryDelta = abs(leftArmAxisAngle - rightArmAxisAngle)
        val leftEarShoulderHipRatio = distance(point(keypoints, LEFT_EAR), leftShoulder) / hipWidth
        val rightEarShoulderHipRatio = distance(point(keypoints, RIGHT_EAR), rightShoulder) / hipWidth
        val bodyUprightDelta = lineAngleDelta(leftHip, rightHip, leftShoulder, rightShoulder)
        val bodyLeanAngle = bodyUprightDelta
        val previous = prevMetrics
        val isPushing = previous != null &&
            leftElbowAngle > previous.leftElbowAngle &&
            rightElbowAngle > previous.rightElbowAngle
        val topPosition = leftElbowAngle > TOP_POSITION_ELBOW_ANGLE && rightElbowAngle > TOP_POSITION_ELBOW_ANGLE
        val lockoutValid = leftElbowAngle > LOCKOUT_VALID_ELBOW_ANGLE && rightElbowAngle > LOCKOUT_VALID_ELBOW_ANGLE
        val leftShrugReference = calibratedLeftEarShoulderHipRatio ?: DEFAULT_LEFT_EAR_SHOULDER_HIP_RATIO
        val rightShrugReference = calibratedRightEarShoulderHipRatio ?: DEFAULT_RIGHT_EAR_SHOULDER_HIP_RATIO
        val leftShrugThreshold = leftShrugReference * SHRUG_CALIBRATION_FACTOR
        val rightShrugThreshold = rightShrugReference * SHRUG_CALIBRATION_FACTOR
        val leftForearmAngle = lineAngle(leftWrist, leftElbow, leftShoulder, rightShoulder)
        val rightForearmAngle = lineAngle(rightWrist, rightElbow, leftShoulder, rightShoulder)
        val badWrist = leftForearmAngle !in FOREARM_LINE_MIN_ANGLE..FOREARM_LINE_MAX_ANGLE ||
            rightForearmAngle !in FOREARM_LINE_MIN_ANGLE..FOREARM_LINE_MAX_ANGLE

        return ShoulderPressMetrics(
            gripRatio = gripRatio,
            leftElbowAngle = leftElbowAngle,
            rightElbowAngle = rightElbowAngle,
            minElbowAngle = minElbowAngle,
            startPositionDelta = startPositionDelta,
            elbowFlareRatio = elbowFlareRatio,
            armSymmetryDelta = armSymmetryDelta,
            leftEarShoulderHipRatio = leftEarShoulderHipRatio,
            rightEarShoulderHipRatio = rightEarShoulderHipRatio,
            leftShrugThreshold = leftShrugThreshold,
            rightShrugThreshold = rightShrugThreshold,
            leftForearmAngle = leftForearmAngle,
            rightForearmAngle = rightForearmAngle,
            bodyUprightDelta = bodyUprightDelta,
            bodyLeanAngle = bodyLeanAngle,
            isGripCorrect = gripRatio in GRIP_MIN_RATIO..GRIP_MAX_RATIO,
            isArmSymmetric = armSymmetryDelta < ARM_SYMMETRY_WARNING_DEGREES,
            isBodyUpright = bodyUprightDelta < BODY_UPRIGHT_ANGLE,
            isStartPosition = leftElbowAngle in START_MIN_ELBOW_ANGLE..START_MAX_ELBOW_ANGLE &&
                rightElbowAngle in START_MIN_ELBOW_ANGLE..START_MAX_ELBOW_ANGLE,
            isPushing = isPushing,
            isTopPosition = topPosition,
            isLockoutValid = lockoutValid,
            notLockedOut = !lockoutValid,
            isShrugging = leftEarShoulderHipRatio < leftShrugThreshold &&
                rightEarShoulderHipRatio < rightShrugThreshold,
            hasElbowFlare = elbowFlareRatio > ELBOW_FLARE_RATIO,
            hasBadWrist = badWrist,
            isBodyLeaning = bodyLeanAngle > BODY_LEAN_ANGLE
        )
    }

    private enum class ShoulderPressIssue(
        val key: String,
        val label: String,
        val severity: FeedbackType,
        val suggestion: String,
        val voiceAction: VoiceAction?
    ) {
        ADJUST_GRIP(
            "adjust_grip",
            "Adjust grip width",
            FeedbackType.WARNING,
            "Hold the dumbbells about 1.8 to 2.1 times shoulder width apart.",
            VoiceAction.SHP_ADJUST_GRIP
        ),
        ARMS_BALANCE(
            "arms_balance",
            "Keep both arms balanced",
            FeedbackType.WARNING,
            "Keep both upper arms moving symmetrically before starting.",
            VoiceAction.SHP_ARMS_BALANCE
        ),
        BODY_UPRIGHT(
            "body_upright",
            "Keep your body upright",
            FeedbackType.WARNING,
            "Sit tall and keep shoulder and hip lines aligned.",
            VoiceAction.SHP_BODY_UPRIGHT
        ),
        START_POSITION(
            "start_position",
            "Set dumbbells at shoulder level",
            FeedbackType.WARNING,
            "Start with both elbows close to 90 degrees before pressing.",
            VoiceAction.SHP_START_POSITION
        ),
        SHRUGGING(
            "shrugging",
            "Avoid shrugging",
            FeedbackType.WARNING,
            "Keep your shoulders down while pressing overhead.",
            VoiceAction.SHP_SHRUGGING
        ),
        ELBOW_FLARE(
            "elbow_flare",
            "Elbows flaring too wide",
            FeedbackType.WARNING,
            "Keep elbow width controlled instead of drifting far beyond shoulder width.",
            VoiceAction.SHP_ELBOW_FLARE
        ),
        BAD_WRIST(
            "bad_wrist",
            "Stack forearms vertically",
            FeedbackType.WARNING,
            "Keep wrists over elbows through the press.",
            VoiceAction.SHP_BAD_WRIST
        ),
        NOT_HIGH(
            "not_high",
            "Lock out fully overhead",
            FeedbackType.ERROR,
            "Press until both elbows are fully extended.",
            VoiceAction.SHP_NOT_HIGH
        ),
        BODY_LEAN(
            "body_lean",
            "Avoid leaning back",
            FeedbackType.WARNING,
            "Brace your core and keep the torso still.",
            VoiceAction.SHP_BODY_LEAN
        ),
        POSE_INCOMPLETE(
            "pose_incomplete",
            "Pose data incomplete",
            FeedbackType.ERROR,
            "Keep your head, ears, shoulders, elbows, wrists, and hips in the camera frame.",
            null
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

    private fun debugValue(
        label: String,
        value: String,
        threshold: String,
        passed: Boolean
    ): ExerciseDebugValue {
        return ExerciseDebugValue(label, value, threshold, passed)
    }

    private fun Float.degrees(): String = "${String.format(Locale.US, "%.1f", this)} deg"

    private fun Float.decimal(): String = String.format(Locale.US, "%.2f", this)

    private data class Point(val x: Float, val y: Float, val z: Float)

    private operator fun Point.minus(other: Point): Point {
        return Point(x - other.x, y - other.y, z - other.z)
    }

    private fun Point.length(): Float {
        return sqrt(x * x + y * y + z * z)
    }

    private fun Point.normalized(): Point {
        val length = length().coerceAtLeast(EPSILON)
        return Point(x / length, y / length, z / length)
    }

    private fun dot(first: Point, second: Point): Float {
        return first.x * second.x + first.y * second.y + first.z * second.z
    }

    private fun center(first: Point, second: Point): Point {
        return Point((first.x + second.x) / 2f, (first.y + second.y) / 2f, (first.z + second.z) / 2f)
    }

    private fun point(keypoints: FloatArray, index: Int): Point {
        return Point(keypoints[index.x], keypoints[index.y], keypoints[index.z])
    }

    private fun distance(first: Point, second: Point): Float {
        val dx = first.x - second.x
        val dy = first.y - second.y
        val dz = first.z - second.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun angle(first: Point, middle: Point, last: Point): Float {
        val ba = first - middle
        val bc = last - middle
        val denominator = (ba.length() * bc.length()).coerceAtLeast(EPSILON)
        val cosAngle = (dot(ba, bc) / denominator).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle)).toFloat()
    }

    private fun angleBetween(first: Point, second: Point): Float {
        val denominator = (first.length() * second.length()).coerceAtLeast(EPSILON)
        val cosAngle = (dot(first, second) / denominator).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle)).toFloat()
    }

    private fun lineAngle(
        firstStart: Point,
        firstEnd: Point,
        secondStart: Point,
        secondEnd: Point
    ): Float {
        val first = firstEnd - firstStart
        val second = secondEnd - secondStart
        return angleBetween(first, second)
    }

    private fun lineAngleDelta(
        firstStart: Point,
        firstEnd: Point,
        secondStart: Point,
        secondEnd: Point
    ): Float {
        val firstAngle = Math.toDegrees(
            atan2((firstEnd.y - firstStart.y).toDouble(), (firstEnd.x - firstStart.x).toDouble())
        ).toFloat()
        val secondAngle = Math.toDegrees(
            atan2((secondEnd.y - secondStart.y).toDouble(), (secondEnd.x - secondStart.x).toDouble())
        ).toFloat()
        val diff = abs(firstAngle - secondAngle)
        return min(diff, 180f - diff)
    }

    private val Int.x: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK
    private val Int.y: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK + 1
    private val Int.z: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK + 2

    private companion object {
        const val NOSE = 0
        const val LEFT_EAR = 7
        const val RIGHT_EAR = 8
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
            LEFT_EAR,
            RIGHT_EAR,
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
        const val START_TARGET_ELBOW_ANGLE = 91.5f
        const val START_MIN_ELBOW_ANGLE = 88f
        const val START_MAX_ELBOW_ANGLE = 95f
        const val START_POSITION_MAX_DELTA = 3.5f
        const val TOP_POSITION_ELBOW_ANGLE = 150f
        const val LOCKOUT_VALID_ELBOW_ANGLE = 160f
        const val GRIP_MIN_RATIO = 1.8f
        const val GRIP_MAX_RATIO = 2.1f
        const val TARGET_GRIP_RATIO = 1.95f
        const val ARM_SYMMETRY_WARNING_DEGREES = 30f
        const val BODY_UPRIGHT_ANGLE = 38f
        const val BODY_LEAN_ANGLE = 38f
        const val ELBOW_FLARE_RATIO = 2.1f
        const val FOREARM_LINE_MIN_ANGLE = 74f
        const val FOREARM_LINE_MAX_ANGLE = 80f
        const val DEFAULT_LEFT_EAR_SHOULDER_HIP_RATIO = 0.85f
        const val DEFAULT_RIGHT_EAR_SHOULDER_HIP_RATIO = 0.85f
        const val SHRUG_CALIBRATION_FACTOR = 0.8f
        const val CONFIRM_FRAMES = 5
        const val COOLDOWN_FRAMES = 30
        const val PREPARATION_HINT_COOLDOWN_FRAMES = 10
    }
}
