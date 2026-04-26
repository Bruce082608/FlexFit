package com.example.flexfit.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals(FeedbackType.SUCCESS, completed.feedback?.type)
        assertEquals(VoiceAction.SUCCESS, completed.voiceAction)
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

    private fun readyPose(): FloatArray = PoseKeypoints.empty().apply {
        setPoint(0, 0f, 0.92f)
        setPoint(7, -0.08f, 0.88f)
        setPoint(8, 0.08f, 0.88f)

        setPoint(11, -0.20f, 0.20f)
        setPoint(12, 0.20f, 0.20f)
        setPoint(13, -0.29f, 0.52f)
        setPoint(14, 0.29f, 0.52f)
        setPoint(15, -0.38f, 0.84f)
        setPoint(16, 0.38f, 0.84f)

        setPoint(23, -0.15f, -0.20f)
        setPoint(24, 0.15f, -0.20f)
    }

    private fun pullingPose(): FloatArray = PoseKeypoints.empty().apply {
        setPoint(0, 0f, 0.88f)
        setPoint(7, -0.08f, 0.84f)
        setPoint(8, 0.08f, 0.84f)

        setPoint(11, -0.20f, 0.45f)
        setPoint(12, 0.20f, 0.45f)
        setPoint(13, -0.35f, 0.35f)
        setPoint(14, 0.35f, 0.35f)
        setPoint(15, -0.32f, 0.75f)
        setPoint(16, 0.32f, 0.75f)

        setPoint(23, -0.15f, -0.20f)
        setPoint(24, 0.15f, -0.20f)
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

    private fun FloatArray.setPoint(index: Int, x: Float, y: Float, z: Float = 0f) {
        PoseKeypoints.set(this, index, x, y, z)
    }
}
