package com.example.flexfit.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoulderPressAnalyzerTest {

    @Test
    fun startPose_confirmsStartingPosition() {
        val analyzer = ShoulderPressAnalyzer()

        val result = analyzer.analyze(startPose(), timestamp = 1L)

        assertEquals(true, result.isReady)
        assertEquals("shoulder_press_down", result.phase.key)
        assertEquals(0, result.count)
        assertEquals(FeedbackType.SUCCESS, result.feedback?.type)
        assertEquals(VoiceAction.SHP_START, result.voiceAction)
    }

    @Test
    fun completeShoulderPressSequence_countsOneRep() {
        val analyzer = ShoulderPressAnalyzer()

        analyzer.analyze(startPose(), timestamp = 1L)
        val pressing = analyzer.analyze(pressingPose(), timestamp = 2L)
        val top = analyzer.analyze(lockoutPose(), timestamp = 3L)
        val completed = analyzer.analyze(startPose(), timestamp = 4L)

        assertEquals("shoulder_press_up", pressing.phase.key)
        assertEquals("shoulder_press_top", top.phase.key)
        assertEquals("shoulder_press_down", completed.phase.key)
        assertEquals(1, completed.count)
        assertEquals(1, completed.attemptedReps)
        assertEquals(FeedbackType.SUCCESS, completed.feedback?.type)
        assertEquals(VoiceAction.SHP_SUCCESS, completed.voiceAction)
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
    fun incompleteLockout_doesNotCountAndReportsHeightError() {
        val analyzer = ShoulderPressAnalyzer()

        analyzer.analyze(startPose(), timestamp = 1L)
        analyzer.analyze(pressingPose(), timestamp = 2L)
        val completed = analyzer.analyze(startPose(), timestamp = 3L)

        assertEquals(0, completed.count)
        assertEquals(1, completed.attemptedReps)
        assertEquals(FeedbackType.ERROR, completed.feedback?.type)
        assertEquals("not_high", completed.issues.first().key)
        assertEquals(VoiceAction.SHP_FAIL, completed.voiceAction)
    }

    @Test
    fun elbowFlareFrames_emitWarning() {
        val analyzer = ShoulderPressAnalyzer()
        analyzer.analyze(startPose(), timestamp = 1L)

        var warningResult: ExerciseAnalysisResult? = null
        for (frame in 2L..8L) {
            val result = analyzer.analyze(flaredPressingPose(), timestamp = frame)
            if (result.feedback?.type == FeedbackType.WARNING) {
                warningResult = result
            }
        }

        val warning = requireNotNull(warningResult)
        assertEquals("elbow_flare", warning.issues.first().key)
    }

    @Test
    fun bodyLeanFrames_emitWarning() {
        val analyzer = ShoulderPressAnalyzer()
        analyzer.analyze(startPose(), timestamp = 1L)

        var warningResult: ExerciseAnalysisResult? = null
        for (frame in 2L..8L) {
            val result = analyzer.analyze(leaningPressingPose(), timestamp = frame)
            if (result.feedback?.type == FeedbackType.WARNING) {
                warningResult = result
            }
        }

        val warning = requireNotNull(warningResult)
        assertEquals("body_lean", warning.issues.first().key)
    }

    @Test
    fun shruggingAtTop_emitWarning() {
        val analyzer = ShoulderPressAnalyzer()
        analyzer.analyze(startPose(), timestamp = 1L)
        analyzer.analyze(pressingPose(), timestamp = 2L)
        analyzer.analyze(lockoutPose(), timestamp = 3L)

        var warningResult: ExerciseAnalysisResult? = null
        for (frame in 4L..10L) {
            val result = analyzer.analyze(shruggingLockoutPose(), timestamp = frame)
            if (result.feedback?.type == FeedbackType.WARNING) {
                warningResult = result
            }
        }

        val warning = requireNotNull(warningResult)
        assertEquals("shrugging", warning.issues.first().key)
        assertEquals(VoiceAction.SHP_SHRUGGING, warning.voiceAction)
    }

    private fun startPose(): FloatArray = basePose(
        leftElbow = Point(-0.32f, 0.28f),
        rightElbow = Point(0.32f, 0.28f),
        leftWrist = Point(-0.32f, 0.48f),
        rightWrist = Point(0.32f, 0.48f)
    )

    private fun pressingPose(): FloatArray = basePose(
        leftElbow = Point(-0.32f, 0.52f),
        rightElbow = Point(0.32f, 0.52f),
        leftWrist = Point(-0.26f, 0.72f),
        rightWrist = Point(0.26f, 0.72f)
    )

    private fun flaredPressingPose(): FloatArray = basePose(
        leftElbow = Point(-0.40f, 0.52f),
        rightElbow = Point(0.40f, 0.52f),
        leftWrist = Point(-0.30f, 0.72f),
        rightWrist = Point(0.30f, 0.72f)
    )

    private fun lockoutPose(): FloatArray = basePose(
        leftElbow = Point(-0.24f, 0.72f),
        rightElbow = Point(0.24f, 0.72f),
        leftWrist = Point(-0.22f, 1.04f),
        rightWrist = Point(0.22f, 1.04f)
    )

    private fun shruggingLockoutPose(): FloatArray = lockoutPose().apply {
        setPoint(7, -0.08f, 0.44f)
        setPoint(8, 0.08f, 0.44f)
    }

    private fun leaningPressingPose(): FloatArray = pressingPose().apply {
        setPoint(7, -0.08f, 1.02f)
        setPoint(8, 0.08f, 1.02f)
        setPoint(23, -0.13f, -0.28f)
        setPoint(24, 0.13f, -0.02f)
    }

    private fun basePose(
        leftShoulder: Point = Point(-0.18f, 0.28f),
        rightShoulder: Point = Point(0.18f, 0.28f),
        leftElbow: Point,
        rightElbow: Point,
        leftWrist: Point,
        rightWrist: Point
    ): FloatArray = PoseKeypoints.empty().apply {
        setPoint(0, 0f, 0.82f)
        setPoint(7, -0.08f, 0.78f)
        setPoint(8, 0.08f, 0.78f)
        setPoint(11, leftShoulder.x, leftShoulder.y)
        setPoint(12, rightShoulder.x, rightShoulder.y)
        setPoint(13, leftElbow.x, leftElbow.y)
        setPoint(14, rightElbow.x, rightElbow.y)
        setPoint(15, leftWrist.x, leftWrist.y)
        setPoint(16, rightWrist.x, rightWrist.y)
        setPoint(23, -0.13f, -0.16f)
        setPoint(24, 0.13f, -0.16f)
    }

    private fun FloatArray.setPoint(index: Int, x: Float, y: Float, z: Float = 0f) {
        PoseKeypoints.set(this, index, x, y, z)
    }

    private data class Point(val x: Float, val y: Float)
}
