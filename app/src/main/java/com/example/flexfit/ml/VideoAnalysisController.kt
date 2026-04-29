package com.example.flexfit.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoAnalysisController(private val context: Context) {

    sealed class AnalysisState {
        data object Idle : AnalysisState()
        data class MetadataReady(val metadata: VideoFrameExtractor.VideoMetadata) : AnalysisState()
        data class Analyzing(
            val progress: Float,
            val currentFrame: Int,
            val totalFrames: Int,
            val frameTimestampMs: Long,
            val statusMessage: String = "Analyzing frames"
        ) : AnalysisState()
        data class Completed(val result: ExerciseAnalysisResult) : AnalysisState()
        data class Error(val message: String) : AnalysisState()
    }

    enum class AnalysisSpeed(val frameIntervalMs: Long) {
        FAST(500L),
        BALANCED(VideoFrameExtractor.DEFAULT_FRAME_INTERVAL_MS),
        ACCURATE(100L)
    }

    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var frameExtractor: VideoFrameExtractor? = null
    private var analysisScope: CoroutineScope? = null
    private var analysisJob: Job? = null
    private var currentVideoUri: Uri? = null
    private var currentMetadata: VideoFrameExtractor.VideoMetadata? = null

    suspend fun initialize(videoUri: Uri): VideoFrameExtractor.VideoMetadata {
        currentVideoUri = videoUri
        currentMetadata = null
        _state.value = AnalysisState.Idle

        val extractor = VideoFrameExtractor(videoUri, context)
        frameExtractor = extractor

        return extractor.initialize().also { metadata ->
            currentMetadata = metadata
            _state.value = AnalysisState.MetadataReady(metadata)
        }
    }

    fun buildPlayer(): ExoPlayer? {
        val uri = currentVideoUri ?: return null
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
        }
        exoPlayer = player
        return player
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun pausePlayback() {
        exoPlayer?.pause()
    }

    fun resumePlayback() {
        exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun startAnalysis(
        poseDetector: PoseDetectorWrapper,
        analyzer: ExerciseAnalyzer,
        frameIntervalMs: Long = AnalysisSpeed.BALANCED.frameIntervalMs,
        onFrame: (suspend (Bitmap, FloatArray, FloatArray, ExerciseAnalysisResult) -> Unit)? = null
    ) {
        val extractor = frameExtractor

        if (currentVideoUri == null || extractor == null) {
            _state.value = AnalysisState.Error("Video not initialized. Please choose a video first.")
            return
        }

        analysisJob?.cancel()
        analysisScope?.cancel()
        analysisScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        analysisJob = analysisScope!!.launch {
            val metadata = try {
                currentMetadata ?: extractor.initialize().also { currentMetadata = it }
            } catch (e: Exception) {
                _state.value = AnalysisState.Error(e.message ?: "Selected video file cannot be opened.")
                return@launch
            }

            val totalFrames = VideoFrameExtractor.estimateFrameCount(
                durationMs = metadata.durationMs,
                intervalMs = frameIntervalMs
            ).coerceAtLeast(1)

            _state.value = AnalysisState.Analyzing(
                progress = 0f,
                currentFrame = 0,
                totalFrames = totalFrames,
                frameTimestampMs = 0L,
                statusMessage = "Preparing video analysis"
            )
            exoPlayer?.pause()

            try {
                var framesRequested = 0
                var posesDetected = 0
                var lastResult: ExerciseAnalysisResult? = null

                extractor.extractFrames(
                    intervalMs = frameIntervalMs,
                    targetHeight = VideoFrameExtractor.DEFAULT_TARGET_HEIGHT,
                    emitFirstFrame = true
                ) { frame ->
                    if (!isActive) return@extractFrames

                    framesRequested = frame.frameIndex + 1
                    _state.value = AnalysisState.Analyzing(
                        progress = ((framesRequested - 1).coerceAtLeast(0) / totalFrames.toFloat())
                            .coerceIn(0f, 1f),
                        currentFrame = (framesRequested - 1).coerceAtLeast(0),
                        totalFrames = totalFrames,
                        frameTimestampMs = frame.timestampMs.coerceIn(0L, metadata.durationMs),
                        statusMessage = "Detecting pose"
                    )

                    try {
                        val poseFrame = processFrameSync(poseDetector, frame.bitmap)

                        if (poseFrame != null) {
                            posesDetected++
                            val result = analyzer.analyze(poseFrame.keypoints, frame.timestampMs)
                            lastResult = result
                            onFrame?.invoke(
                                frame.bitmap,
                                poseFrame.keypoints,
                                poseFrame.landmarkConfidences,
                                result
                            )
                        }
                    } finally {
                        frame.bitmap.recycle()
                    }

                    _state.value = AnalysisState.Analyzing(
                        progress = (framesRequested / totalFrames.toFloat()).coerceIn(0f, 1f),
                        currentFrame = framesRequested.coerceAtMost(totalFrames),
                        totalFrames = totalFrames,
                        frameTimestampMs = frame.timestampMs.coerceIn(0L, metadata.durationMs),
                        statusMessage = if (posesDetected > 0) {
                            "Analyzing movement"
                        } else {
                            "Searching for body pose"
                        }
                    )
                }.collect { frameIndex ->
                    _state.value = AnalysisState.Analyzing(
                        progress = ((frameIndex + 1) / totalFrames.toFloat()).coerceIn(0f, 1f),
                        currentFrame = (frameIndex + 1).coerceAtMost(totalFrames),
                        totalFrames = totalFrames,
                        frameTimestampMs = (frameIndex * frameIntervalMs).coerceIn(0L, metadata.durationMs),
                        statusMessage = if (posesDetected > 0) {
                            "Analyzing movement"
                        } else {
                            "Searching for body pose"
                        }
                    )
                }

                if (framesRequested == 0) {
                    _state.value = AnalysisState.Error(
                        "No video frames could be extracted. Please choose another video."
                    )
                    return@launch
                }

                if (posesDetected == 0 || lastResult == null) {
                    _state.value = AnalysisState.Error(
                        "MediaPipe did not detect a human pose in this video. Please choose a clearer video."
                    )
                    return@launch
                }

                _state.value = AnalysisState.Analyzing(
                    progress = 1f,
                    currentFrame = totalFrames,
                    totalFrames = totalFrames,
                    frameTimestampMs = metadata.durationMs,
                    statusMessage = "Finalizing results"
                )
                _state.value = AnalysisState.Completed(lastResult!!)
            } catch (e: CancellationException) {
                _state.value = AnalysisState.Error("Video analysis canceled.")
                throw e
            } catch (e: Exception) {
                _state.value = AnalysisState.Error(e.message ?: "Analysis failed unexpectedly.")
            }
        }
    }

    private suspend fun processFrameSync(
        poseDetector: PoseDetectorWrapper,
        bitmap: Bitmap
    ): PoseFrame? = kotlinx.coroutines.withContext(Dispatchers.Default) {
        var resultFrame: PoseFrame? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        poseDetector.processBitmap(bitmap, object : PoseDetectorCallback {
            override fun onPoseDetected(keypoints: FloatArray, confidence: Float) {
                resultFrame = PoseFrame(keypoints, PoseKeypoints.emptyConfidences())
                latch.countDown()
            }

            override fun onPoseDetected(
                keypoints: FloatArray,
                landmarkConfidences: FloatArray,
                confidence: Float
            ) {
                resultFrame = PoseFrame(keypoints, landmarkConfidences)
                latch.countDown()
            }

            override fun onPoseNotDetected() {
                latch.countDown()
            }

            override fun onError(error: String) {
                latch.countDown()
            }
        })

        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        resultFrame
    }

    private data class PoseFrame(
        val keypoints: FloatArray,
        val landmarkConfidences: FloatArray
    )

    fun stopAnalysis() {
        analysisJob?.cancel()
        analysisScope?.cancel()
        analysisJob = null
        analysisScope = null
        _state.value = AnalysisState.Idle
    }

    fun release() {
        analysisJob?.cancel()
        analysisScope?.cancel()
        analysisScope = null
        analysisJob = null

        try {
            exoPlayer?.release()
        } catch (_: Exception) {
        }
        exoPlayer = null

        frameExtractor?.close()
        frameExtractor = null
        currentVideoUri = null
        currentMetadata = null
        _state.value = AnalysisState.Idle
    }
}
