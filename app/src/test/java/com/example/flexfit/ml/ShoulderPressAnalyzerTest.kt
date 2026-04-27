package com.example.flexfit.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoulderPressAnalyzerTest {

    @Test
    fun rackPose_confirmsStartingPosition() {
        val analyzer = ShoulderPressAnalyzer()

        val result = analyzer.analyze(rackPose(), timestamp = 1L)

        assertEquals(true, result.isReady)
        assertEquals("shoulder_press_rack", result.phase.key)
        assertEquals(0, result.count)
        assertEquals(FeedbackType.SUCCESS, result.feedback?.type)
        assertEquals(VoiceAction.START, result.voiceAction)
    }

    @Test
    fun completeShoulderPressSequence_countsOneRep() {
        val analyzer = ShoulderPressAnalyzer()

        analyzer.analyze(rackPose(), timestamp = 1L)
        val pressing = analyzer.analyze(pressingPose(), timestamp = 2L)
        val lockout = analyzer.analyze(lockoutPose(), timestamp = 3L)
        val returning = analyzer.analyze(returningPose(), timestamp = 4L)
        val completed = analyzer.analyze(rackPose(), timestamp = 5L)

        assertEquals("shoulder_press_pressing", pressing.phase.key)
        assertEquals("shoulder_press_lockout", lockout.phase.key)
        assertEquals("shoulder_press_returning", returning.phase.key)
        assertEquals("shoulder_press_rack", completed.phase.key)
        assertEquals(1, completed.count)
        assertEquals(1, completed.attemptedReps)
        assertEquals(FeedbackType.SUCCESS, completed.feedback?.type)
        assertEquals(VoiceAction.SUCCESS, completed.voiceAction)
        assertTrue(completed.scores.depth > 0f)
        assertTrue(completed.scores.alignment > 0f)
        assertTrue(completed.scores.stability > 0f)
        assertEquals(completed.scores.total, completed.accuracy, 0.001f)
    }

    @Test
    fun shortKeypointArray_returnsErrorFeedbackWithoutCrashing() {
        val analyzer = ShoulderPressAnalyzer()

        val result = analyzer.analyze(FloatArray(19 * 3), timestamp = 1L)

        assertNotNull(result.feedback)
        assertEquals(FeedbackType.ERROR, result.feedback?.type)
        assertEquals("Pose data incomplete", result.feedback?.message)
        assertEquals("pose_incomplete", result.issues.first().key)
        assertEquals(0, result.count)
    }

    @Test
    fun incompleteRange_doesNotCountAndReportsRangeOfMotion() {
        val analyzer = ShoulderPressAnalyzer()

        analyzer.analyze(rackPose(), timestamp = 1L)
        analyzer.analyze(pressingPose(), timestamp = 2L)
        val completed = analyzer.analyze(rackPose(), timestamp = 3L)

        assertEquals(0, completed.count)
        assertEquals(1, completed.attemptedReps)
        assertEquals(FeedbackType.ERROR, completed.feedback?.type)
        assertEquals("range_of_motion", completed.issues.first().key)
        assertEquals(VoiceAction.FAIL, completed.voiceAction)
    }

    @Test
    fun leftRightImbalance_emitsWarningAndBlocksRep() {
        val analyzer = ShoulderPressAnalyzer()

        analyzer.analyze(rackPose(), timestamp = 1L)
        val warning = analyzer.analyze(imbalancedPressingPose(), timestamp = 2L)
        analyzer.analyze(lockoutPose(), timestamp = 3L)
        analyzer.analyze(returningPose(), timestamp = 4L)
        val completed = analyzer.analyze(rackPose(), timestamp = 5L)

        assertEquals(FeedbackType.WARNING, warning.feedback?.type)
        assertEquals("left_right_imbalance", warning.issues.first().key)
        assertEquals(0, completed.count)
        assertEquals(1, completed.attemptedReps)
    }

    @Test
    fun trunkLean_emitsWarningAndBlocksRep() {
        val analyzer = ShoulderPressAnalyzer()

        analyzer.analyze(rackPose(), timestamp = 1L)
        val warning = analyzer.analyze(pressingPose(hipShiftX = 0.24f), timestamp = 2L)
        analyzer.analyze(lockoutPose(), timestamp = 3L)
        analyzer.analyze(returningPose(), timestamp = 4L)
        val completed = analyzer.analyze(rackPose(), timestamp = 5L)

        assertEquals(FeedbackType.WARNING, warning.feedback?.type)
        assertEquals("trunk_lean", warning.issues.first().key)
        assertEquals(0, completed.count)
    }

    @Test
    fun shoulderInstability_emitsWarning() {
        val analyzer = ShoulderPressAnalyzer()

        analyzer.analyze(rackPose(), timestamp = 1L)
        val warning = analyzer.analyze(pressingPose(leftShoulderY = 0.42f, rightShoulderY = 0.28f), timestamp = 2L)

        assertEquals(FeedbackType.WARNING, warning.feedback?.type)
        assertEquals("shoulder_instability", warning.issues.first().key)
    }

    private fun rackPose(): FloatArray = basePose(
        leftElbow = Point(-0.36f, 0.22f),
        rightElbow = Point(0.36f, 0.22f),
        leftWrist = Point(-0.28f, 0.43f),
        rightWrist = Point(0.28f, 0.43f)
    )

    private fun pressingPose(
        hipShiftX: Float = 0f,
        leftShoulderY: Float = 0.28f,
        rightShoulderY: Float = 0.28f
    ): FloatArray = basePose(
        leftShoulder = Point(-0.18f, leftShoulderY),
        rightShoulder = Point(0.18f, rightShoulderY),
        leftElbow = Point(-0.32f, 0.52f),
        rightElbow = Point(0.32f, 0.52f),
        leftWrist = Point(-0.26f, 0.72f),
        rightWrist = Point(0.26f, 0.72f),
        hipShiftX = hipShiftX
    )

    private fun imbalancedPressingPose(): FloatArray = basePose(
        leftElbow = Point(-0.32f, 0.52f),
        rightElbow = Point(0.32f, 0.46f),
        leftWrist = Point(-0.26f, 0.78f),
        rightWrist = Point(0.26f, 0.62f)
    )

    private fun lockoutPose(): FloatArray = basePose(
        leftElbow = Point(-0.22f, 0.62f),
        rightElbow = Point(0.22f, 0.62f),
        leftWrist = Point(-0.24f, 0.96f),
        rightWrist = Point(0.24f, 0.96f)
    )

    private fun returningPose(): FloatArray = basePose(
        leftElbow = Point(-0.30f, 0.44f),
        rightElbow = Point(0.30f, 0.44f),
        leftWrist = Point(-0.26f, 0.64f),
        rightWrist = Point(0.26f, 0.64f)
    )

    private fun basePose(
        leftShoulder: Point = Point(-0.18f, 0.28f),
        rightShoulder: Point = Point(0.18f, 0.28f),
        leftElbow: Point,
        rightElbow: Point,
        leftWrist: Point,
        rightWrist: Point,
        hipShiftX: Float = 0f
    ): FloatArray = PoseKeypoints.empty().apply {
        setPoint(0, 0f, 0.78f)
        setPoint(11, leftShoulder.x, leftShoulder.y)
        setPoint(12, rightShoulder.x, rightShoulder.y)
        setPoint(13, leftElbow.x, leftElbow.y)
        setPoint(14, rightElbow.x, rightElbow.y)
        setPoint(15, leftWrist.x, leftWrist.y)
        setPoint(16, rightWrist.x, rightWrist.y)
        setPoint(23, -0.13f + hipShiftX, -0.16f)
        setPoint(24, 0.13f + hipShiftX, -0.16f)
    }

    private fun FloatArray.setPoint(index: Int, x: Float, y: Float, z: Float = 0f) {
        PoseKeypoints.set(this, index, x, y, z)
    }

    private data class Point(val x: Float, val y: Float)
}
