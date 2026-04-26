package com.example.flexfit.ml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class PullUpType(
    val displayName: String,
    val gripMin: Float,
    val gripMax: Float,
    val startAngle: Float,
    val targetGrip: Float
) {
    WIDE("Wide Grip", 2.15f, 3.30f, 154f, 2.65f),
    NORMAL("Normal Grip", 1.55f, 2.15f, 157f, 1.85f),
    NARROW("Narrow Grip", 0.90f, 1.60f, 155f, 1.25f)
}

enum class PullUpState {
    PREPARING,
    DOWN,
    UP,
    TOP
}

typealias PullUpFeedback = ExerciseFeedback
typealias PullUpAnalysisResult = ExerciseAnalysisResult

class PullUpAnalyzer(private val pullUpType: PullUpType) : ExerciseAnalyzer {
    override val exerciseName: String = "${pullUpType.displayName} Pull-up"

    private var state: PullUpState = PullUpState.PREPARING
    private var count = 0
    private var attemptedReps = 0
    private var isReady = false
    private var validTop = false
    private var currentRepBlocked = false
    private var maxHeightRatio = Float.NEGATIVE_INFINITY
    private var minElbowAngle = Float.POSITIVE_INFINITY
    private var prevMetrics: PullUpMetrics? = null
    private var lastHint: String? = null
    private var hintCooldown = 0
    private var pendingVoiceAction: VoiceAction? = null

    private var issueCounters = PullUpIssue.entries.associateWith { 0 }.toMutableMap()
    private var issueCooldowns = PullUpIssue.entries.associateWith { 0 }.toMutableMap()

    override fun analyze(keypoints: FloatArray, timestamp: Long): ExerciseAnalysisResult {
        pendingVoiceAction = null
        decrementCooldowns()
        if (hintCooldown > 0) hintCooldown--

        if (!PoseKeypoints.isValid(keypoints) || !hasRequiredPoints(keypoints)) {
            val issue = PullUpIssue.POSE_INCOMPLETE.toExerciseIssue()
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
        return MockPoseSequence.pullUpFrame(frameCount)
    }

    override fun reset() {
        state = PullUpState.PREPARING
        count = 0
        attemptedReps = 0
        isReady = false
        validTop = false
        currentRepBlocked = false
        maxHeightRatio = Float.NEGATIVE_INFINITY
        minElbowAngle = Float.POSITIVE_INFINITY
        prevMetrics = null
        lastHint = null
        hintCooldown = 0
        pendingVoiceAction = null
        issueCounters = PullUpIssue.entries.associateWith { 0 }.toMutableMap()
        issueCooldowns = PullUpIssue.entries.associateWith { 0 }.toMutableMap()
    }

    private fun handlePreparation(
        metrics: PullUpMetrics,
        issues: MutableList<ExerciseIssue>
    ): PullUpFeedback? {
        val issue = when {
            !metrics.isGripCorrect -> PullUpIssue.GRIP_WIDTH
            !metrics.isStartPosition -> PullUpIssue.START_POSITION
            !metrics.isArmBalanced -> PullUpIssue.ARM_ALIGNMENT
            !metrics.isBodyAligned -> PullUpIssue.BODY_ALIGNMENT
            else -> null
        }

        if (issue != null) {
            val exerciseIssue = issue.toExerciseIssue()
            issues.add(exerciseIssue)
            return throttledHint(exerciseIssue)
        }

        isReady = true
        state = PullUpState.DOWN
        resetCurrentRep(metrics)
        pendingVoiceAction = VoiceAction.START
        return PullUpFeedback("Starting position confirmed. Ready to begin!", FeedbackType.SUCCESS)
    }

    private fun handleStateTransitions(
        metrics: PullUpMetrics,
        issues: MutableList<ExerciseIssue>
    ): PullUpFeedback? {
        return when {
            state == PullUpState.DOWN && metrics.isPulling -> {
                state = PullUpState.UP
                resetCurrentRep(metrics)
                null
            }

            state == PullUpState.UP && metrics.isTopPosition -> {
                state = PullUpState.TOP
                validTop = true
                null
            }

            state == PullUpState.UP && metrics.isNearTopButLow -> {
                currentRepBlocked = true
                val feedback = emitIssue(PullUpIssue.NOT_HIGH, issues, requireConfirmation = true)
                if (feedback != null) {
                    pendingVoiceAction = VoiceAction.NOT_HIGH
                }
                feedback
            }

            (state == PullUpState.UP || state == PullUpState.TOP) && metrics.isStartPosition -> {
                completeRep(metrics, issues)
            }

            state == PullUpState.TOP && metrics.isLoweringButNotExtended -> {
                emitIssue(PullUpIssue.RANGE_OF_MOTION, issues, requireConfirmation = true)
            }

            else -> null
        }
    }

    private fun handleErrorDetection(
        metrics: PullUpMetrics,
        issues: MutableList<ExerciseIssue>
    ): PullUpFeedback? {
        if (state != PullUpState.UP && state != PullUpState.TOP) return null

        val swingFeedback = if (metrics.isSwinging) {
            currentRepBlocked = true
            val feedback = emitIssue(PullUpIssue.SWINGING, issues, requireConfirmation = true)
            if (feedback != null) {
                pendingVoiceAction = VoiceAction.SWINGING
            }
            feedback
        } else {
            resetIssueCounter(PullUpIssue.SWINGING)
            null
        }

        val shrugFeedback = if (metrics.isShrugging) {
            currentRepBlocked = true
            val feedback = emitIssue(PullUpIssue.SHRUGGING, issues, requireConfirmation = true)
            if (feedback != null) {
                pendingVoiceAction = VoiceAction.SHRUGGING
            }
            feedback
        } else {
            resetIssueCounter(PullUpIssue.SHRUGGING)
            null
        }

        return shrugFeedback ?: swingFeedback
    }

    private fun completeRep(
        metrics: PullUpMetrics,
        issues: MutableList<ExerciseIssue>
    ): PullUpFeedback {
        attemptedReps++
        val hasFullDepth = maxHeightRatio >= TOP_HEIGHT_RATIO && minElbowAngle <= TOP_ELBOW_ANGLE
        val shouldCount = validTop && hasFullDepth && !currentRepBlocked

        return if (shouldCount) {
            count++
            state = PullUpState.DOWN
            resetCurrentRep(metrics)
            pendingVoiceAction = VoiceAction.SUCCESS
            PullUpFeedback("Pull-up completed! Count: $count", FeedbackType.SUCCESS)
        } else {
            val issue = if (!validTop || maxHeightRatio < TOP_HEIGHT_RATIO) {
                PullUpIssue.NOT_HIGH
            } else {
                PullUpIssue.RANGE_OF_MOTION
            }.toExerciseIssue()

            issues.add(issue)
            state = PullUpState.DOWN
            resetCurrentRep(metrics)
            pendingVoiceAction = VoiceAction.FAIL
            issue.toFeedback()
        }
    }

    private fun resetCurrentRep(metrics: PullUpMetrics) {
        validTop = false
        currentRepBlocked = false
        maxHeightRatio = metrics.heightRatio
        minElbowAngle = metrics.averageElbowAngle
    }

    private fun updateRepRange(metrics: PullUpMetrics) {
        maxHeightRatio = max(maxHeightRatio, metrics.heightRatio)
        minElbowAngle = min(minElbowAngle, metrics.averageElbowAngle)
    }

    private fun buildResult(
        timestamp: Long,
        metrics: PullUpMetrics?,
        feedback: PullUpFeedback?,
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

    private fun calculateScores(metrics: PullUpMetrics): ExerciseScoreBreakdown {
        if (!isReady) {
            val alignment = scoreAlignment(metrics)
            return ExerciseScoreBreakdown(depth = 0f, alignment = alignment, stability = 0f)
        }

        val depth = when (state) {
            PullUpState.DOWN -> scoreExtension(metrics)
            PullUpState.UP, PullUpState.TOP -> {
                val heightScore = ((metrics.heightRatio + 0.28f) / (TOP_HEIGHT_RATIO + 0.28f) * 100f)
                    .coerceIn(0f, 100f)
                val bendScore = ((START_ELBOW_REFERENCE - metrics.averageElbowAngle) /
                    (START_ELBOW_REFERENCE - TOP_ELBOW_ANGLE) * 100f).coerceIn(0f, 100f)
                (heightScore * 0.65f + bendScore * 0.35f).coerceIn(0f, 100f)
            }
            PullUpState.PREPARING -> 0f
        }

        return ExerciseScoreBreakdown(
            depth = depth,
            alignment = scoreAlignment(metrics),
            stability = scoreStability(metrics)
        )
    }

    private fun scoreExtension(metrics: PullUpMetrics): Float {
        return ((metrics.averageElbowAngle - 120f) / (pullUpType.startAngle - 120f) * 100f)
            .coerceIn(0f, 100f)
    }

    private fun scoreAlignment(metrics: PullUpMetrics): Float {
        val gripPenalty = abs(metrics.gripRatio - pullUpType.targetGrip) * 28f
        val armPenalty = max(0f, metrics.armSymmetryDelta - ARM_SYMMETRY_WARNING_DEGREES) * 1.5f
        val torsoPenalty = max(0f, metrics.torsoLeanRatio - TORSO_LEAN_WARNING_RATIO) * 120f
        val shoulderPenalty = max(0f, metrics.shoulderBalanceRatio - SHOULDER_BALANCE_WARNING_RATIO) * 100f
        return (100f - gripPenalty - armPenalty - torsoPenalty - shoulderPenalty).coerceIn(0f, 100f)
    }

    private fun scoreStability(metrics: PullUpMetrics): Float {
        val swingPenalty = metrics.centerShiftRatio * 220f
        val leanPenalty = max(0f, metrics.torsoLeanRatio - TORSO_LEAN_WARNING_RATIO) * 100f
        return (100f - swingPenalty - leanPenalty).coerceIn(0f, 100f)
    }

    private fun emitIssue(
        issue: PullUpIssue,
        issues: MutableList<ExerciseIssue>,
        requireConfirmation: Boolean
    ): PullUpFeedback? {
        if (requireConfirmation && !triggerIssue(issue)) return null

        val exerciseIssue = issue.toExerciseIssue()
        issues.add(exerciseIssue)
        return exerciseIssue.toFeedback()
    }

    private fun throttledHint(issue: ExerciseIssue): PullUpFeedback? {
        if (issue.label == lastHint || hintCooldown > 0) return null

        lastHint = issue.label
        hintCooldown = PREPARATION_HINT_COOLDOWN_FRAMES
        return issue.toFeedback()
    }

    private fun triggerIssue(issue: PullUpIssue): Boolean {
        val cooldown = issueCooldowns[issue] ?: 0
        if (cooldown > 0) return false

        val count = (issueCounters[issue] ?: 0) + 1
        issueCounters[issue] = count

        if (count < CONFIRM_FRAMES) return false

        issueCounters[issue] = 0
        issueCooldowns[issue] = COOLDOWN_FRAMES
        return true
    }

    private fun resetIssueCounter(issue: PullUpIssue) {
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

    private fun PullUpState.toExercisePhase(): ExercisePhase {
        return when (this) {
            PullUpState.PREPARING -> ExercisePhase("pullup_preparing", "Ready", ExercisePhaseTone.NEUTRAL)
            PullUpState.DOWN -> ExercisePhase("pullup_down", "Arms Extended", ExercisePhaseTone.SUCCESS)
            PullUpState.UP -> ExercisePhase("pullup_up", "Pulling Up", ExercisePhaseTone.ACTIVE)
            PullUpState.TOP -> ExercisePhase("pullup_top", "Top Position", ExercisePhaseTone.SUCCESS)
        }
    }

    private fun hasRequiredPoints(keypoints: FloatArray): Boolean {
        return REQUIRED_LANDMARKS.all { PoseKeypoints.hasPoint(keypoints, it) }
    }

    private data class PullUpMetrics(
        val shoulderWidth: Float,
        val gripRatio: Float,
        val leftElbowAngle: Float,
        val rightElbowAngle: Float,
        val averageElbowAngle: Float,
        val armSymmetryDelta: Float,
        val heightRatio: Float,
        val torsoLeanRatio: Float,
        val shoulderBalanceRatio: Float,
        val centerShiftRatio: Float,
        val earShoulderRatio: Float,
        val hipCenterX: Float,
        val isGripCorrect: Boolean,
        val isStartPosition: Boolean,
        val isArmBalanced: Boolean,
        val isBodyAligned: Boolean,
        val isPulling: Boolean,
        val isTopPosition: Boolean,
        val isNearTopButLow: Boolean,
        val isLoweringButNotExtended: Boolean,
        val isSwinging: Boolean,
        val isShrugging: Boolean
    ) {
    }

    private fun buildMetrics(keypoints: FloatArray): PullUpMetrics {
        val shoulderWidth = distance(keypoints, LEFT_SHOULDER, RIGHT_SHOULDER).coerceAtLeast(EPSILON)
        val gripRatio = distance(keypoints, LEFT_WRIST, RIGHT_WRIST) / shoulderWidth
        val leftElbowAngle = angle(keypoints, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST)
        val rightElbowAngle = angle(keypoints, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST)
        val averageElbowAngle = (leftElbowAngle + rightElbowAngle) / 2f
        val armSymmetryDelta = abs(leftElbowAngle - rightElbowAngle)
        val shoulderCenter = center(keypoints, LEFT_SHOULDER, RIGHT_SHOULDER)
        val hipCenter = center(keypoints, LEFT_HIP, RIGHT_HIP)
        val wristCenter = center(keypoints, LEFT_WRIST, RIGHT_WRIST)
        val earCenter = center(keypoints, LEFT_EAR, RIGHT_EAR)
        val torsoLeanRatio = abs(shoulderCenter.x - hipCenter.x) / shoulderWidth
        val shoulderBalanceRatio = abs(keypoints[LEFT_SHOULDER.y] - keypoints[RIGHT_SHOULDER.y]) / shoulderWidth
        val centerShiftRatio = prevMetrics?.let { previous ->
            abs(hipCenter.x - previous.hipCenterX) / shoulderWidth
        } ?: 0f
        val earShoulderRatio = (earCenter.y - shoulderCenter.y) / shoulderWidth
        val heightRatio = (keypoints[NOSE.y] - wristCenter.y) / shoulderWidth
        val isGripCorrect = gripRatio in pullUpType.gripMin..pullUpType.gripMax
        val isStartPosition = averageElbowAngle >= pullUpType.startAngle
        val isArmBalanced = armSymmetryDelta <= ARM_SYMMETRY_WARNING_DEGREES
        val isBodyAligned =
            torsoLeanRatio <= TORSO_LEAN_WARNING_RATIO &&
                shoulderBalanceRatio <= SHOULDER_BALANCE_WARNING_RATIO
        val isPulling = averageElbowAngle <= PULLING_ELBOW_ANGLE
        val isTopPosition = averageElbowAngle <= TOP_ELBOW_ANGLE && heightRatio >= TOP_HEIGHT_RATIO
        val isNearTopButLow =
            averageElbowAngle <= TOP_ELBOW_ANGLE &&
                heightRatio >= NEAR_TOP_HEIGHT_RATIO &&
                heightRatio < TOP_HEIGHT_RATIO
        val isLoweringButNotExtended =
            averageElbowAngle >= LOWERING_ELBOW_WARNING_ANGLE &&
                averageElbowAngle < pullUpType.startAngle
        val isSwinging = centerShiftRatio >= SWING_CENTER_SHIFT_RATIO || torsoLeanRatio >= TORSO_LEAN_ERROR_RATIO
        val isShrugging = averageElbowAngle <= TOP_ELBOW_ANGLE && earShoulderRatio <= SHRUG_EAR_SHOULDER_RATIO

        return PullUpMetrics(
            shoulderWidth = shoulderWidth,
            gripRatio = gripRatio,
            leftElbowAngle = leftElbowAngle,
            rightElbowAngle = rightElbowAngle,
            averageElbowAngle = averageElbowAngle,
            armSymmetryDelta = armSymmetryDelta,
            heightRatio = heightRatio,
            torsoLeanRatio = torsoLeanRatio,
            shoulderBalanceRatio = shoulderBalanceRatio,
            centerShiftRatio = centerShiftRatio,
            earShoulderRatio = earShoulderRatio,
            hipCenterX = hipCenter.x,
            isGripCorrect = isGripCorrect,
            isStartPosition = isStartPosition,
            isArmBalanced = isArmBalanced,
            isBodyAligned = isBodyAligned,
            isPulling = isPulling,
            isTopPosition = isTopPosition,
            isNearTopButLow = isNearTopButLow,
            isLoweringButNotExtended = isLoweringButNotExtended,
            isSwinging = isSwinging,
            isShrugging = isShrugging
        )
    }

    private fun PullUpIssue.toExerciseIssue(): ExerciseIssue {
        return ExerciseIssue(
            key = key,
            label = label,
            severity = severity,
            suggestion = suggestion
        )
    }

    private fun ExerciseIssue.toFeedback(): PullUpFeedback {
        return PullUpFeedback(label, severity)
    }

    private enum class PullUpIssue(
        val key: String,
        val label: String,
        val severity: FeedbackType,
        val suggestion: String
    ) {
        POSE_INCOMPLETE(
            "pose_incomplete",
            "Pose data incomplete",
            FeedbackType.ERROR,
            "Keep your full upper body in the camera frame."
        ),
        GRIP_WIDTH(
            "grip_width",
            "Adjust grip width",
            FeedbackType.WARNING,
            "Match your hand spacing to the selected grip type before starting."
        ),
        START_POSITION(
            "start_position",
            "Start from full arm extension",
            FeedbackType.WARNING,
            "Begin each rep with both elbows nearly straight and hands overhead."
        ),
        ARM_ALIGNMENT(
            "arm_alignment",
            "Arms not balanced",
            FeedbackType.WARNING,
            "Pull with both arms evenly so left and right elbow angles stay close."
        ),
        BODY_ALIGNMENT(
            "body_alignment",
            "Keep body vertical",
            FeedbackType.WARNING,
            "Keep shoulders stacked over hips and avoid leaning to one side."
        ),
        SWINGING(
            "swinging",
            "Body swinging detected",
            FeedbackType.WARNING,
            "Brace your core and pause at the bottom before the next pull."
        ),
        SHRUGGING(
            "shrugging",
            "Avoid shrugging at the top",
            FeedbackType.WARNING,
            "Keep shoulders down and pull elbows toward your ribs."
        ),
        NOT_HIGH(
            "not_high",
            "Pull higher before counting",
            FeedbackType.ERROR,
            "Pull until your chin clearly passes the bar line."
        ),
        RANGE_OF_MOTION(
            "range_of_motion",
            "Use full range of motion",
            FeedbackType.ERROR,
            "Reach full extension at the bottom and a clear top position before counting."
        )
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
        const val START_ELBOW_REFERENCE = 175f
        const val PULLING_ELBOW_ANGLE = 135f
        const val TOP_ELBOW_ANGLE = 95f
        const val TOP_HEIGHT_RATIO = 0.08f
        const val NEAR_TOP_HEIGHT_RATIO = -0.18f
        const val LOWERING_ELBOW_WARNING_ANGLE = 120f
        const val ARM_SYMMETRY_WARNING_DEGREES = 28f
        const val TORSO_LEAN_WARNING_RATIO = 0.30f
        const val TORSO_LEAN_ERROR_RATIO = 0.45f
        const val SHOULDER_BALANCE_WARNING_RATIO = 0.25f
        const val SWING_CENTER_SHIFT_RATIO = 0.22f
        const val SHRUG_EAR_SHOULDER_RATIO = 0.45f
        const val CONFIRM_FRAMES = 5
        const val COOLDOWN_FRAMES = 30
        const val PREPARATION_HINT_COOLDOWN_FRAMES = 10
    }
}
