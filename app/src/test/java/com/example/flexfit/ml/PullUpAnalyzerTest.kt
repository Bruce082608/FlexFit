package com.example.flexfit.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PullUpAnalyzerTest {

    @Test
    fun readyPose_confirmsStartingPosition() {
        val analyzer = PullUpAnalyzer(PullUpType.NORMAL)

        val result = analyzer.analyze(readyPose(), timestamp = 1L)

        assertEquals(true, result.isReady)
        assertEquals("pullup_down", result.phase.key)
        assertEquals(0, result.count)
        assertEquals(FeedbackType.SUCCESS, result.feedback?.type)
        assertEquals(VoiceAction.START, result.voiceAction)
    }

    @Test
    fun completePullUpSequence_countsOneRep() {
        val analyzer = PullUpAnalyzer(PullUpType.NORMAL)

        analyzer.analyze(readyPose(), timestamp = 1L)
        val pulling = analyzer.analyze(pullingPose(), timestamp = 2L)
        val top = analyzer.analyze(topPose(), timestamp = 3L)
        val completed = analyzer.analyze(readyPose(), timestamp = 4L)

        assertEquals("pullup_up", pulling.phase.key)
        assertEquals("pullup_top", top.phase.key)
        assertEquals("pullup_down", completed.phase.key)
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
    fun legacyShortKeypointArray_returnsErrorFeedbackWithoutCrashing() {
        val analyzer = PullUpAnalyzer(PullUpType.NORMAL)

        val result = analyzer.analyze(FloatArray(19 * 3), timestamp = 1L)

        assertNotNull(result.feedback)
        assertEquals(FeedbackType.ERROR, result.feedback?.type)
        assertEquals("Pose data incomplete", result.feedback?.message)
        assertEquals(0, result.count)
    }

    @Test
    fun selectedGripWidth_acceptsOnlyMatchingReadyPose() {
        val wideAnalyzer = PullUpAnalyzer(PullUpType.WIDE)
        val narrowAnalyzer = PullUpAnalyzer(PullUpType.NARROW)
        val normalAnalyzer = PullUpAnalyzer(PullUpType.NORMAL)

        assertEquals(true, wideAnalyzer.analyze(readyPose(wristX = 0.55f), timestamp = 1L).isReady)
        assertEquals(true, narrowAnalyzer.analyze(readyPose(wristX = 0.25f), timestamp = 1L).isReady)

        val normalWithWideGrip = normalAnalyzer.analyze(readyPose(wristX = 0.55f), timestamp = 1L)
        assertEquals(false, normalWithWideGrip.isReady)
        assertEquals("grip_width", normalWithWideGrip.issues.first().key)
        assertEquals(FeedbackType.WARNING, normalWithWideGrip.feedback?.type)
    }

    @Test
    fun lowTopPosition_doesNotCountAndReportsHeightError() {
        val analyzer = PullUpAnalyzer(PullUpType.NORMAL)

        analyzer.analyze(readyPose(), timestamp = 1L)
        analyzer.analyze(pullingPose(), timestamp = 2L)
        analyzer.analyze(lowTopPose(), timestamp = 3L)
        val completed = analyzer.analyze(readyPose(), timestamp = 4L)

        assertEquals(0, completed.count)
        assertEquals(1, completed.attemptedReps)
        assertEquals(FeedbackType.ERROR, completed.feedback?.type)
        assertEquals("not_high", completed.issues.first().key)
        assertEquals(VoiceAction.FAIL, completed.voiceAction)
    }

    @Test
    fun swingingFrames_emitWarning() {
        val analyzer = PullUpAnalyzer(PullUpType.NORMAL)

        analyzer.analyze(readyPose(), timestamp = 1L)

        var result = analyzer.analyze(pullingPose(), timestamp = 2L)
        var warningResult: ExerciseAnalysisResult? = null
        for (frame in 3L..7L) {
            val tilt = if (frame % 2L == 0L) 0.08f else -0.08f
            result = analyzer.analyze(pullingPose(hipTilt = tilt), timestamp = frame)
            if (result.feedback?.type == FeedbackType.WARNING) {
                warningResult = result
            }
        }

        val warning = requireNotNull(warningResult)
        assertEquals(FeedbackType.WARNING, warning.feedback?.type)
        assertEquals("swinging", warning.issues.first().key)
        assertEquals(VoiceAction.SWINGING, warning.voiceAction)
    }

    @Test
    fun shruggingAtTop_emitWarning() {
        val analyzer = PullUpAnalyzer(PullUpType.NORMAL)

        analyzer.analyze(readyPose(), timestamp = 1L)
        analyzer.analyze(pullingPose(), timestamp = 2L)
        analyzer.analyze(topPose(), timestamp = 3L)

        var result = analyzer.analyze(shruggingTopPose(), timestamp = 4L)
        var warningResult: ExerciseAnalysisResult? = null
        for (frame in 5L..9L) {
            result = analyzer.analyze(shruggingTopPose(), timestamp = frame)
            if (result.feedback?.type == FeedbackType.WARNING) {
                warningResult = result
            }
        }

        val warning = requireNotNull(warningResult)
        assertEquals(FeedbackType.WARNING, warning.feedback?.type)
        assertEquals("shrugging", warning.issues.first().key)
        assertEquals(VoiceAction.SHRUGGING, warning.voiceAction)
    }

    private fun readyPose(wristX: Float = 0.38f): FloatArray = PoseKeypoints.empty().apply {
        val elbowX = (0.20f + wristX) / 2f
        setPoint(0, 0f, 0.92f)
        setPoint(7, -0.08f, 0.88f)
        setPoint(8, 0.08f, 0.88f)

        setPoint(11, -0.20f, 0.20f)
        setPoint(12, 0.20f, 0.20f)
        setPoint(13, -elbowX, 0.52f)
        setPoint(14, elbowX, 0.52f)
        setPoint(15, -wristX, 0.84f)
        setPoint(16, wristX, 0.84f)

        setPoint(23, -0.15f, -0.20f)
        setPoint(24, 0.15f, -0.20f)
    }

    private fun pullingPose(hipShiftX: Float = 0f, hipTilt: Float = 0f): FloatArray = PoseKeypoints.empty().apply {
        setPoint(0, 0f, 0.88f)
        setPoint(7, -0.08f, 0.84f)
        setPoint(8, 0.08f, 0.84f)

        setPoint(11, -0.20f, 0.45f)
        setPoint(12, 0.20f, 0.45f)
        setPoint(13, -0.35f, 0.35f)
        setPoint(14, 0.35f, 0.35f)
        setPoint(15, -0.32f, 0.75f)
        setPoint(16, 0.32f, 0.75f)

        setPoint(23, -0.15f + hipShiftX, -0.20f + hipTilt)
        setPoint(24, 0.15f + hipShiftX, -0.20f - hipTilt)
    }

    private fun topPose(): FloatArray = PoseKeypoints.empty().apply {
        setPoint(0, 0f, 0.90f)
        setPoint(7, -0.08f, 0.86f)
        setPoint(8, 0.08f, 0.86f)

        setPoint(11, -0.20f, 0.55f)
        setPoint(12, 0.20f, 0.55f)
        setPoint(13, -0.35f, 0.45f)
        setPoint(14, 0.35f, 0.45f)
        setPoint(15, -0.32f, 0.85f)
        setPoint(16, 0.32f, 0.85f)

        setPoint(23, -0.15f, 0.15f)
        setPoint(24, 0.15f, 0.15f)
    }

    private fun lowTopPose(): FloatArray = topPose().apply {
        setPoint(0, 0f, 0.20f)
        setPoint(7, -0.08f, 0.16f)
        setPoint(8, 0.08f, 0.16f)
    }

    private fun shruggingTopPose(): FloatArray = topPose().apply {
        setPoint(7, -0.08f, 0.64f)
        setPoint(8, 0.08f, 0.64f)
    }

    private fun FloatArray.setPoint(index: Int, x: Float, y: Float, z: Float = 0f) {
        PoseKeypoints.set(this, index, x, y, z)
    }
}
