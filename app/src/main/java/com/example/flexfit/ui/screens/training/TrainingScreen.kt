package com.example.flexfit.ui.screens.training

import android.Manifest
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.flexfit.audio.VoiceGuideManager
import com.example.flexfit.audio.VoiceType
import com.example.flexfit.ml.ExerciseAnalysisResult
import com.example.flexfit.ml.ExerciseAnalyzer
import com.example.flexfit.ml.ExerciseDebugProvider
import com.example.flexfit.ml.ExerciseDebugSnapshot
import com.example.flexfit.ml.ExercisePhaseTone
import com.example.flexfit.ml.FeedbackType
import com.example.flexfit.ml.PoseDetectorCallback
import com.example.flexfit.ml.PoseDetectorWrapper
import com.example.flexfit.ml.VideoAnalysisController
import com.example.flexfit.ml.VoiceAction
import com.example.flexfit.ui.i18n.LocalAppLanguage
import com.example.flexfit.ui.i18n.l10n
import com.example.flexfit.ui.i18n.workoutName
import com.example.flexfit.ui.screens.workout.WorkoutResultDialog
import com.example.flexfit.ui.theme.AccentPurple
import com.example.flexfit.ui.theme.DeepPurple
import com.example.flexfit.ui.theme.ErrorRed
import com.example.flexfit.ui.theme.LightPurple
import com.example.flexfit.ui.theme.SuccessGreen
import com.example.flexfit.ui.theme.TextPrimary
import com.example.flexfit.ui.theme.TextSecondary
import com.example.flexfit.ui.theme.WarningOrange
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private val BackgroundDark = Color(0xFF1A1A2E)
private val PanelBackground = Color.Black.copy(alpha = 0.7f)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrainingScreen(
    analyzer: ExerciseAnalyzer,
    initialMode: TrainingMode = TrainingMode.CAMERA,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLanguage = LocalAppLanguage.current
    val viewModel: TrainingSessionViewModel = viewModel(
        factory = TrainingSessionViewModel.Factory(context.applicationContext)
    )
    val uiState by viewModel.uiState.collectAsState()

    // ── Camera permission ──────────────────────────────────────────────────────
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // ── All state variables (declared first so launchers can capture them) ────────
    var hasVideoPermission by remember { mutableStateOf(false) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAnalysisController by remember {
        mutableStateOf<VideoAnalysisController?>(null)
    }
    var trainingMode by remember(initialMode) { mutableStateOf(initialMode) }
    var showAnalysisOverlay by remember { mutableStateOf(false) }
    var showDebugMetrics by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showVideoPermissionDialog by remember { mutableStateOf(false) }

    val videoPermission = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    // ── Video picker launcher ─────────────────────────────────────────────────
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            scope.launch {
                try {
                    // Initialize the video analysis controller
                    val controller = VideoAnalysisController(context)
                    videoAnalysisController?.release()
                    videoAnalysisController = controller

                    val metadata = controller.initialize(uri)
                    viewModel.setVideoMetadata(
                        durationMs = metadata.durationMs,
                        frameRate = metadata.frameRate,
                        width = metadata.videoWidth,
                        height = metadata.videoHeight,
                        estimatedFrames = metadata.estimatedTotalFrames
                    )

                    // Build ExoPlayer for preview
                    exoPlayer?.release()
                    exoPlayer = controller.buildPlayer()

                    trainingMode = TrainingMode.VIDEO
                    viewModel.setVideoMode(true)
                } catch (e: Exception) {
                    videoAnalysisController?.release()
                    videoAnalysisController = null
                    exoPlayer?.release()
                    exoPlayer = null
                    videoUri = null
                    viewModel.showFeedback(
                        e.message ?: "Selected video file cannot be opened.",
                        FeedbackType.ERROR
                    )
                }
            }
        }
    }

    // ── Video permission launcher ─────────────────────────────────────────────
    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasVideoPermission = granted
        if (granted) {
            videoPickerLauncher.launch("video/*")
        } else {
            viewModel.showFeedback(
                "Video permission denied. Please grant video access and try again.",
                FeedbackType.ERROR
            )
        }
    }

    // ── Pose detector ──────────────────────────────────────────────────────────
    val poseDetector = remember { PoseDetectorWrapper(context.applicationContext) }
    var poseDetectorInitialized by remember { mutableStateOf(false) }
    var poseDetectorError by remember { mutableStateOf<String?>(null) }

    // ── Voice guide ────────────────────────────────────────────────────────────
    val voiceGuideManager = remember { VoiceGuideManager(context) }

    // ── Initialize pose detector ──────────────────────────────────────────────
    LaunchedEffect(Unit) {
        try {
            poseDetector.initialize()
            poseDetectorInitialized = true
        } catch (e: Exception) {
            poseDetectorError = e.message ?: "Failed to initialize pose detector"
        }
    }

    // ── Cleanup on dispose ─────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            voiceGuideManager.release()
            poseDetector.close()
            exoPlayer?.release()
            videoAnalysisController?.release()
        }
    }

    // ── Elapsed time ticker (camera mode) ─────────────────────────────────────
    LaunchedEffect(uiState.isWorkoutActive, uiState.isPaused) {
        while (uiState.isWorkoutActive && !uiState.isPaused && trainingMode == TrainingMode.CAMERA) {
            delay(1000)
            viewModel.tickElapsedSecond()
        }
    }

    // ── Clear feedback message ─────────────────────────────────────────────────
    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            delay(2300)
            viewModel.clearFeedback()
        }
    }

    // ── Collect video analysis state ───────────────────────────────────────────
    LaunchedEffect(videoAnalysisController) {
        val controller = videoAnalysisController ?: return@LaunchedEffect
        controller.state.collectLatest { state ->
            when (state) {
                is VideoAnalysisController.AnalysisState.Idle -> {
                    showAnalysisOverlay = false
                }
                is VideoAnalysisController.AnalysisState.Analyzing -> {
                    showAnalysisOverlay = true
                    viewModel.updateVideoAnalysisProgress(
                        framesAnalyzed = state.currentFrame,
                        totalFrames = state.totalFrames,
                        currentTimeMs = state.frameTimestampMs,
                        progress = state.progress,
                        status = state.statusMessage
                    )
                }
                is VideoAnalysisController.AnalysisState.Completed -> {
                    showAnalysisOverlay = false
                    viewModel.setAnalyzingVideo(false)
                }
                is VideoAnalysisController.AnalysisState.Error -> {
                    showAnalysisOverlay = false
                    viewModel.setAnalyzingVideo(false)
                    viewModel.showFeedback(state.message, FeedbackType.ERROR)
                }
                is VideoAnalysisController.AnalysisState.MetadataReady -> { }
            }
        }
    }

    // ── Camera mode keypoint handler ───────────────────────────────────────────
    fun handleCameraKeypoints(keypoints: FloatArray) {
        val result = analyzer.analyze(keypoints, System.currentTimeMillis())
        viewModel.recordAnalysis(result, keypoints)
        handleVoiceAction(result.voiceAction, voiceGuideManager)
    }

    fun handleCameraKeypoints(keypoints: FloatArray, landmarkConfidences: FloatArray) {
        val result = analyzer.analyze(keypoints, System.currentTimeMillis())
        viewModel.recordAnalysis(result, keypoints, landmarkConfidences)
        handleVoiceAction(result.voiceAction, voiceGuideManager)
    }

    // ── Camera switch function ────────────────────────────────────────────────
    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
    }

    // ── Main UI ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when {
            // ── Video picker screen ────────────────────────────────────────────
            trainingMode == TrainingMode.VIDEO && videoUri == null -> {
                VideoPickerView(
                    onVideoSelected = {
                        if (hasVideoPermission) {
                            videoPickerLauncher.launch("video/*")
                        } else {
                            showVideoPermissionDialog = true
                        }
                    },
                    onBack = {
                        if (initialMode == TrainingMode.VIDEO) {
                            onNavigateBack()
                        } else {
                            trainingMode = TrainingMode.CAMERA
                        }
                    },
                    hasVideoPermission = hasVideoPermission,
                    onRequestVideoPermission = { showVideoPermissionDialog = true }
                )
            }

            // ── Video training view ────────────────────────────────────────────
            trainingMode == TrainingMode.VIDEO && videoUri != null -> {
                VideoTrainingView(
                    exoPlayer = exoPlayer,
                    videoUri = videoUri,
                    analysisController = videoAnalysisController,
                    poseDetector = poseDetector,
                    analyzer = analyzer,
                    isAnalyzing = uiState.isAnalyzingVideo,
                    onAnalysisStarted = {
                        viewModel.setAnalyzingVideo(true)
                    },
                    onKeypointsDetected = { keypoints, result ->
                        viewModel.recordAnalysis(result, keypoints)
                        handleVoiceAction(result.voiceAction, voiceGuideManager)
                    },
                    onAnalysisComplete = {
                        viewModel.setAnalyzingVideo(false)
                        val videoMs = uiState.videoMetadata?.durationMs ?: 0L
                        viewModel.endWorkoutWithVideoDuration(analyzer.exerciseName, videoMs)
                    }
                )
            }

            // ── Camera training view ───────────────────────────────────────────
            cameraPermissionState.status.isGranted && poseDetectorInitialized -> {
                CameraTrainingView(
                    poseDetector = poseDetector,
                    isWorkoutActiveFlow = viewModel.isWorkoutActiveFlow,
                    isPausedFlow = viewModel.isPausedFlow,
                    lensFacing = lensFacing,
                    onKeypointsDetected = ::handleCameraKeypoints
                )
            }

            // ── Pose detector error ───────────────────────────────────────────
            poseDetectorError != null -> {
                ErrorView(message = poseDetectorError!!)
            }

            // ── Camera permission rationale ──────────────────────────────────
            cameraPermissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequestPermission = { showCameraPermissionDialog = true }
                )
            }

            // ── Request camera permission ─────────────────────────────────────
            else -> {
                PermissionRequest(
                    onRequestPermission = { showCameraPermissionDialog = true }
                )
            }
        }

        // ── Pose overlay (always on top of camera/video) ────────────────────────
        if (showCameraPermissionDialog) {
            PermissionRequestDialog(
                title = l10n("Camera Permission Required"),
                message = l10n("Please grant camera permission for motion recognition"),
                confirmText = l10n("Grant Permission"),
                onDismiss = { showCameraPermissionDialog = false },
                onConfirm = {
                    showCameraPermissionDialog = false
                    cameraPermissionState.launchPermissionRequest()
                }
            )
        }

        if (showVideoPermissionDialog) {
            PermissionRequestDialog(
                title = l10n("Video Permission Required"),
                message = l10n("FlexFit needs access to your videos to analyze your workout form."),
                confirmText = l10n("Grant Permission"),
                onDismiss = { showVideoPermissionDialog = false },
                onConfirm = {
                    showVideoPermissionDialog = false
                    videoPermissionLauncher.launch(videoPermission)
                }
            )
        }

        PoseOverlay(
            keypoints = uiState.lastKeypoints,
            landmarkConfidences = uiState.lastLandmarkConfidences,
            phaseTone = uiState.result.phase.tone,
            feedbackType = uiState.result.feedback?.type,
            modifier = Modifier.fillMaxSize()
        )

        // ── Video analysis progress overlay ──────────────────────────────────
        if (showAnalysisOverlay && uiState.isAnalyzingVideo) {
            VideoAnalysisProgressOverlay(
                framesAnalyzed = uiState.videoFramesAnalyzed,
                totalFrames = uiState.videoTotalFrames,
                currentTimeMs = uiState.videoCurrentTimeMs,
                progress = uiState.videoAnalysisProgress,
                status = uiState.videoAnalysisStatus,
                metadata = uiState.videoMetadata,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        val debugSnapshot = (analyzer as? ExerciseDebugProvider)?.debugSnapshot()
        if (showDebugMetrics && trainingMode == TrainingMode.CAMERA && debugSnapshot != null) {
            DebugMetricsOverlay(
                snapshot = debugSnapshot,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // ── Top bar ───────────────────────────────────────────────────────────
        TrainingTopBar(
            title = appLanguage.workoutName(analyzer.exerciseName),
            isActive = uiState.isWorkoutActive,
            isPaused = uiState.isPaused,
            isVideoMode = trainingMode == TrainingMode.VIDEO,
            showDebugToggle = analyzer is ExerciseDebugProvider && trainingMode == TrainingMode.CAMERA,
            isDebugVisible = showDebugMetrics,
            onBack = {
                videoAnalysisController?.stopAnalysis()
                onNavigateBack()
            },
            onStart = {
                analyzer.reset()
                voiceGuideManager.resetAll()
                viewModel.startWorkout()
            },
            onPause = viewModel::togglePause,
            onReset = {
                analyzer.reset()
                viewModel.resetSession()
            },
            onToggleDebug = { showDebugMetrics = !showDebugMetrics },
            onSwitchCamera = ::switchCamera
        )

        // ── Bottom stats panel ────────────────────────────────────────────────
        TrainingBottomPanel(
            result = uiState.result,
            elapsedTime = uiState.elapsedTime,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 132.dp)
        )

        // ── Control buttons ───────────────────────────────────────────────────
        TrainingControls(
            isWorkoutActive = uiState.isWorkoutActive,
            isPaused = uiState.isPaused,
            isVideoMode = trainingMode == TrainingMode.VIDEO,
            hasVideo = videoUri != null,
            onStart = {
                analyzer.reset()
                voiceGuideManager.resetAll()
                viewModel.startWorkout()
            },
            onPause = viewModel::togglePause,
            onEnd = {
                if (trainingMode == TrainingMode.VIDEO && uiState.isAnalyzingVideo) {
                    videoAnalysisController?.stopAnalysis()
                    viewModel.setAnalyzingVideo(false)
                    viewModel.showFeedback("Video analysis canceled.", FeedbackType.INFO)
                } else {
                    viewModel.endWorkout(analyzer.exerciseName)
                }
            },
            onVideoModeToggle = {
                val shouldReturnToPicker = trainingMode == TrainingMode.VIDEO && videoUri != null
                videoUri = null
                exoPlayer?.release()
                exoPlayer = null
                videoAnalysisController?.release()
                videoAnalysisController = null
                viewModel.resetVideoState()
                trainingMode = if (shouldReturnToPicker) {
                    TrainingMode.VIDEO
                } else if (trainingMode == TrainingMode.VIDEO) {
                    TrainingMode.CAMERA
                } else {
                    TrainingMode.VIDEO
                }
            },
            onAnalyzeVideo = {
                val controller = videoAnalysisController ?: return@TrainingControls
                if (!poseDetectorInitialized) return@TrainingControls

                analyzer.reset()
                voiceGuideManager.resetAll()
                viewModel.resetSessionKeepsVideo()
                viewModel.startWorkout()
                controller.startAnalysis(
                    poseDetector = poseDetector,
                    analyzer = analyzer,
                    frameIntervalMs = VideoAnalysisController.AnalysisSpeed.BALANCED.frameIntervalMs
                ) { _, keypoints, landmarkConfidences, result ->
                    viewModel.recordAnalysis(result, keypoints, landmarkConfidences)
                    handleVoiceAction(result.voiceAction, voiceGuideManager)
                }
                viewModel.setAnalyzingVideo(true)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ── Feedback overlay ─────────────────────────────────────────────────
        FeedbackOverlay(
            visible = uiState.feedbackMessage != null,
            message = uiState.feedbackMessage.orEmpty(),
            type = uiState.feedbackType
        )

        // ── Result dialog ────────────────────────────────────────────────────
        val result = uiState.workoutResult
        val llmState = uiState.llmAnalysisState
        if (uiState.showResultDialog && result != null) {
            LaunchedEffect(result.id) {
                if (llmState is com.example.flexfit.data.llm.LlmAnalysisState.Idle) {
                    viewModel.requestLlmAnalysis(result, appLanguage)
                }
            }

            WorkoutResultDialog(
                result = result,
                llmAnalysisState = llmState,
                onDismiss = {
                    analyzer.reset()
                    viewModel.closeResultAndReset()
                },
                onSaveAndClose = {
                    viewModel.saveWorkoutResult(result)
                    analyzer.reset()
                    viewModel.closeResultAndReset()
                },
                onRequestLlmAnalysis = { viewModel.requestLlmAnalysis(result, appLanguage) },
                onRetryLlmAnalysis = { viewModel.retryLlmAnalysis(result, appLanguage) }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Supporting composables
// ──────────────────────────────────────────────────────────────────────────────

private fun handleVoiceAction(
    action: VoiceAction?,
    voiceGuideManager: VoiceGuideManager
) {
    when (action) {
        VoiceAction.START -> voiceGuideManager.playVoice(VoiceType.START)
        VoiceAction.SUCCESS -> voiceGuideManager.playVoice(VoiceType.SUCCESS)
        VoiceAction.FAIL -> voiceGuideManager.playVoice(VoiceType.FAIL)
        VoiceAction.SWINGING -> voiceGuideManager.triggerWarning(VoiceType.SWINGING)
        VoiceAction.SHRUGGING -> voiceGuideManager.triggerWarning(VoiceType.SHRUGGING)
        VoiceAction.NOT_HIGH -> voiceGuideManager.triggerWarning(VoiceType.NOT_HIGH)
        VoiceAction.SHP_ADJUST_GRIP -> voiceGuideManager.playVoice(VoiceType.SHP_ADJUST_GRIP)
        VoiceAction.SHP_ARMS_BALANCE -> voiceGuideManager.playVoice(VoiceType.SHP_ARMS_BALANCE)
        VoiceAction.SHP_BODY_UPRIGHT -> voiceGuideManager.playVoice(VoiceType.SHP_BODY_UPRIGHT)
        VoiceAction.SHP_START_POSITION -> voiceGuideManager.playVoice(VoiceType.SHP_START_POSITION)
        VoiceAction.SHP_START -> voiceGuideManager.playVoice(VoiceType.SHP_START)
        VoiceAction.SHP_BRACE_CORE -> voiceGuideManager.playVoice(VoiceType.SHP_BRACE_CORE)
        VoiceAction.SHP_SHRUGGING -> voiceGuideManager.playVoice(VoiceType.SHP_SHRUGGING)
        VoiceAction.SHP_NOT_HIGH -> voiceGuideManager.playVoice(VoiceType.SHP_NOT_HIGH)
        VoiceAction.SHP_BODY_LEAN -> voiceGuideManager.playVoice(VoiceType.SHP_BODY_LEAN)
        VoiceAction.SHP_ELBOW_FLARE -> voiceGuideManager.playVoice(VoiceType.SHP_ELBOW_FLARE)
        VoiceAction.SHP_BAD_WRIST -> voiceGuideManager.playVoice(VoiceType.SHP_BAD_WRIST)
        VoiceAction.SHP_SUCCESS -> voiceGuideManager.playVoice(VoiceType.SHP_SUCCESS)
        VoiceAction.SHP_FAIL -> voiceGuideManager.playVoice(VoiceType.SHP_FAIL)
        null -> Unit
    }
}

@Composable
private fun VideoAnalysisProgressOverlay(
    framesAnalyzed: Int,
    totalFrames: Int,
    currentTimeMs: Long,
    progress: Float,
    status: String,
    metadata: VideoAnalysisMetadata?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AccentPurple,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentPurple,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Frame ${framesAnalyzed.coerceAtLeast(0)} / ${totalFrames.coerceAtLeast(framesAnalyzed).coerceAtLeast(1)}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Frame $framesAnalyzed  •  ${formatVideoTime(currentTimeMs)} / ${formatVideoTime(metadata?.durationMs ?: 0L)}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}

@Composable
private fun TrainingTopBar(
    title: String,
    isActive: Boolean,
    isPaused: Boolean,
    isVideoMode: Boolean,
    showDebugToggle: Boolean,
    isDebugVisible: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onToggleDebug: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = l10n("Back"),
                tint = Color.White
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isActive) {
            Row {
                if (showDebugToggle) {
                    IconButton(
                        onClick = onToggleDebug,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isDebugVisible) AccentPurple.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = l10n("Debug Metrics"),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Camera switch button (only in camera mode)
                if (!isVideoMode) {
                    IconButton(
                        onClick = onSwitchCamera,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = l10n("Switch Camera"),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onPause,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) l10n("Resume") else l10n("Pause"),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onReset,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = l10n("Reset"),
                        tint = Color.White
                    )
                }
            }
        } else {
            Row {
                if (showDebugToggle) {
                    IconButton(
                        onClick = onToggleDebug,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isDebugVisible) AccentPurple.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = l10n("Debug Metrics"),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Camera switch button (only in camera mode, shown when not active)
                if (!isVideoMode) {
                    IconButton(
                        onClick = onSwitchCamera,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = l10n("Switch Camera"),
                            tint = Color.White
                        )
                    }
                } else {
                    // Placeholder for alignment when in video mode
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }
        }
    }
}

@Composable
private fun DebugMetricsOverlay(
    snapshot: ExerciseDebugSnapshot,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 86.dp, start = 16.dp, end = 16.dp)
            .widthIn(max = 430.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.78f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = snapshot.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            snapshot.values.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.95f)
                    )
                    Text(
                        text = item.value,
                        color = if (item.passed) SuccessGreen else WarningOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = item.threshold,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingBottomPanel(
    result: ExerciseAnalysisResult,
    elapsedTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main stats row - compact design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rep count with status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (result.isReady) SuccessGreen else WarningOrange,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${result.count}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                        Text(
                            text = l10n("Reps"),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Current phase
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = result.phase.label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = phaseColor(result.phase.tone)
                    )
                    Text(
                        text = l10n("Phase"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(elapsedTime),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = l10n("Duration"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = l10n("Accuracy"),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${result.accuracy.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = phaseColor(result.phase.tone)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { result.accuracy / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = phaseColor(result.phase.tone),
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Score badges - compact inline layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreBadge(label = l10n("Depth"), value = result.scores.depth)
                ScoreBadge(label = l10n("Align"), value = result.scores.alignment)
                ScoreBadge(label = l10n("Stable"), value = result.scores.stability)
            }
        }
    }
}

@Composable
private fun ScoreBadge(label: String, value: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${value.toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun TrainingControls(
    isWorkoutActive: Boolean,
    isPaused: Boolean,
    isVideoMode: Boolean,
    hasVideo: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
    onVideoModeToggle: () -> Unit,
    onAnalyzeVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 32.dp, end = 32.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isWorkoutActive) {
            // Video mode toggle
            IconButton(
                onClick = onVideoModeToggle,
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isVideoMode) AccentPurple else LightPurple, CircleShape)
            ) {
                Icon(
                    if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideoLibrary,
                    contentDescription = l10n("Video Mode"),
                    tint = if (isVideoMode) Color.White else DeepPurple
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            if (isVideoMode && hasVideo) {
                // Analyze video button
                Button(
                    onClick = onAnalyzeVideo,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(l10n("Analyze Video"), fontWeight = FontWeight.SemiBold)
                }
            } else {
                // Start camera button
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .height(56.dp)
                        .width(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(l10n("Start"), fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // End workout button
            Button(
                onClick = onEnd,
                modifier = Modifier
                    .height(56.dp)
                    .width(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(Icons.Default.Stop, contentDescription = l10n("End Workout"), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(l10n("End"), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Pause/Resume button
            Button(
                onClick = onPause,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) SuccessGreen else WarningOrange
                )
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) l10n("Resume") else l10n("Pause"),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraTrainingView(
    poseDetector: PoseDetectorWrapper,
    isWorkoutActiveFlow: kotlinx.coroutines.flow.StateFlow<Boolean>,
    isPausedFlow: kotlinx.coroutines.flow.StateFlow<Boolean>,
    lensFacing: Int,
    onKeypointsDetected: (FloatArray, FloatArray) -> Unit
) {
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Re-bind camera when lensFacing changes
    LaunchedEffect(previewView, lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            // Read latest state from StateFlow to avoid stale closure
                            val isActive = isWorkoutActiveFlow.value
                            val isPaused = isPausedFlow.value
                            if (isActive && !isPaused) {
                                poseDetector.processFrame(imageProxy, object : PoseDetectorCallback {
                                    override fun onPoseDetected(keypoints: FloatArray, confidence: Float) {
                                        onKeypointsDetected(keypoints, com.example.flexfit.ml.PoseKeypoints.emptyConfidences())
                                    }

                                    override fun onPoseDetected(
                                        keypoints: FloatArray,
                                        landmarkConfidences: FloatArray,
                                        confidence: Float
                                    ) {
                                        onKeypointsDetected(keypoints, landmarkConfidences)
                                    }

                                    override fun onPoseNotDetected() = Unit
                                    override fun onError(error: String) = Unit
                                })
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build(),
                    preview,
                    imageAnalysis
                )
            } catch (_: Exception) {
                // Camera errors are non-fatal for the shared training surface.
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoTrainingView(
    exoPlayer: ExoPlayer?,
    videoUri: Uri?,
    analysisController: VideoAnalysisController?,
    poseDetector: PoseDetectorWrapper,
    analyzer: ExerciseAnalyzer,
    isAnalyzing: Boolean,
    onAnalysisStarted: () -> Unit,
    onKeypointsDetected: (FloatArray, ExerciseAnalysisResult) -> Unit,
    onAnalysisComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isAnalysisComplete by remember { mutableStateOf(false) }

    // ── ExoPlayer preview ─────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        if (exoPlayer != null && videoUri != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Loading state — show metadata from controller
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentPurple)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = l10n("Loading video..."),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // ── "Analyze Video" trigger overlay (before analysis starts) ───────────
        if (!isAnalyzing && !isAnalysisComplete && exoPlayer != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = l10n("Ready to analyze"),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = l10n("Tap \"Analyze Video\" below to start frame-by-frame analysis"),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Analysis in progress indicator ────────────────────────────────────
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentPurple)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = l10n("Analyzing frames..."),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = l10n("Pose detection in progress"),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ── Auto-complete when analysis state is Completed ─────────────────────
        LaunchedEffect(analysisController) {
            val controller = analysisController ?: return@LaunchedEffect
            controller.state.collectLatest { state ->
                if (state is VideoAnalysisController.AnalysisState.Completed) {
                    isAnalysisComplete = true
                    onAnalysisComplete()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VideoPickerView(
    onVideoSelected: () -> Unit,
    onBack: () -> Unit,
    hasVideoPermission: Boolean,
    onRequestVideoPermission: () -> Unit
) {
    // Track if permission dialog is showing - hide Card to prevent blocking system dialog
    var isPermissionDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(hasVideoPermission) {
        isPermissionDialogVisible = false
    }

    // When permission request is launched, hide the Card to avoid blocking the system dialog
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        if (!isPermissionDialogVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = l10n("Video Analysis"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = l10n("Select a workout video to analyze your form"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!hasVideoPermission) {
                    // Show permission request UI inline
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = DeepPurple.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = l10n("Permission Required"),
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = l10n("FlexFit needs access to your videos to analyze your workout form."),
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onRequestVideoPermission()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(l10n("Grant Permission"))
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onVideoSelected,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(l10n("Choose Video"))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBack) {
                    Text(l10n("Back"), color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FeedbackOverlay(
    visible: Boolean,
    message: String,
    type: FeedbackType
) {
    AnimatedVisibility(
        visible = visible && message.isNotEmpty(),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            val backgroundColor = when (type) {
                FeedbackType.SUCCESS -> SuccessGreen
                FeedbackType.WARNING -> WarningOrange
                FeedbackType.ERROR -> ErrorRed
                FeedbackType.INFO -> AccentPurple
            }
            val icon = when (type) {
                FeedbackType.SUCCESS -> Icons.Default.CheckCircle
                FeedbackType.WARNING -> Icons.Default.Warning
                FeedbackType.ERROR -> Icons.Default.Cancel
                FeedbackType.INFO -> Icons.Default.Info
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AccentPurple
            )
        },
        title = {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(l10n("Not Now"))
            }
        }
    )
}

@Composable
private fun PermissionRequest(onRequestPermission: () -> Unit) {
    PermissionMessage(
        iconColor = AccentPurple,
        title = l10n("Camera Permission Required"),
        message = l10n("Please grant camera permission for motion recognition"),
        action = l10n("Grant Permission"),
        onAction = onRequestPermission
    )
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    PermissionMessage(
        iconColor = WarningOrange,
        title = l10n("Camera Permission Denied"),
        message = l10n("Motion recognition requires camera permission. Please enable it in settings."),
        action = l10n("Request Again"),
        onAction = onRequestPermission
    )
}

@Composable
private fun PermissionMessage(
    iconColor: Color,
    title: String,
    message: String,
    action: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text(action)
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = l10n("Initialization Error"),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun phaseColor(tone: ExercisePhaseTone): Color {
    return when (tone) {
        ExercisePhaseTone.NEUTRAL -> TextSecondary
        ExercisePhaseTone.WARNING -> WarningOrange
        ExercisePhaseTone.ACTIVE -> AccentPurple
        ExercisePhaseTone.SUCCESS -> SuccessGreen
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
