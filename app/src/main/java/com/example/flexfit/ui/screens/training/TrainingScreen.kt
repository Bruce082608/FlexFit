package com.example.flexfit.ui.screens.training

import android.Manifest
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexfit.audio.VoiceGuideManager
import com.example.flexfit.audio.VoiceType
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ml.ExerciseAnalysisResult
import com.example.flexfit.ml.ExerciseAnalyzer
import com.example.flexfit.ml.ExercisePhaseTone
import com.example.flexfit.ml.FeedbackType
import com.example.flexfit.ml.PoseDetectorCallback
import com.example.flexfit.ml.PoseDetectorWrapper
import com.example.flexfit.ml.VoiceAction
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
    val viewModel: TrainingSessionViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val poseDetector = remember { PoseDetectorWrapper() }
    val voiceGuideManager = remember { VoiceGuideManager(context) }

    var trainingMode by remember(initialMode) { mutableStateOf(initialMode) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        videoUri = uri
    }

    LaunchedEffect(Unit) {
        try {
            poseDetector.initialize()
        } catch (_: Exception) {
            // Permission and model initialization errors are surfaced by frame callbacks.
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceGuideManager.release()
            poseDetector.close()
        }
    }

    LaunchedEffect(uiState.isWorkoutActive, uiState.isPaused) {
        while (uiState.isWorkoutActive && !uiState.isPaused) {
            delay(1000)
            viewModel.tickElapsedSecond()
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            delay(2300)
            viewModel.clearFeedback()
        }
    }

    fun handleKeypoints(keypoints: FloatArray) {
        val result = analyzer.analyze(keypoints, System.currentTimeMillis())
        viewModel.recordAnalysis(result, keypoints)
        handleVoiceAction(result.voiceAction, voiceGuideManager)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when {
            trainingMode == TrainingMode.VIDEO && videoUri == null -> {
                VideoPickerView(
                    onVideoSelected = { videoUri = it },
                    onBack = {
                        if (initialMode == TrainingMode.VIDEO) {
                            onNavigateBack()
                        } else {
                            trainingMode = TrainingMode.CAMERA
                        }
                    },
                    videoPickerLauncher = videoPickerLauncher
                )
            }

            trainingMode == TrainingMode.VIDEO && videoUri != null -> {
                VideoTrainingView(
                    onFrame = ::handleKeypoints,
                    mockFrame = analyzer::mockFrame,
                    isPlaying = uiState.isWorkoutActive && !uiState.isPaused,
                    onPlaybackStateChange = viewModel::setPlaybackActive,
                    onAnalysisComplete = { viewModel.endWorkout(analyzer.exerciseName) }
                )

                PoseOverlay(
                    keypoints = uiState.lastKeypoints,
                    phaseTone = uiState.result.phase.tone,
                    feedbackType = uiState.result.feedback?.type,
                    modifier = Modifier.fillMaxSize()
                )
            }

            cameraPermissionState.status.isGranted -> {
                CameraTrainingView(
                    poseDetector = poseDetector,
                    isWorkoutActive = uiState.isWorkoutActive,
                    isPaused = uiState.isPaused,
                    onKeypointsDetected = ::handleKeypoints
                )

                PoseOverlay(
                    keypoints = uiState.lastKeypoints,
                    phaseTone = uiState.result.phase.tone,
                    feedbackType = uiState.result.feedback?.type,
                    modifier = Modifier.fillMaxSize()
                )
            }

            cameraPermissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }

            else -> {
                PermissionRequest(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
        }

        TrainingTopBar(
            title = analyzer.exerciseName,
            isActive = uiState.isWorkoutActive,
            isPaused = uiState.isPaused,
            onBack = onNavigateBack,
            onStart = {
                analyzer.reset()
                voiceGuideManager.resetAll()
                viewModel.startWorkout()
            },
            onPause = viewModel::togglePause,
            onReset = {
                analyzer.reset()
                viewModel.resetSession()
            }
        )

        TrainingBottomPanel(
            result = uiState.result,
            elapsedTime = uiState.elapsedTime,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        TrainingControls(
            isWorkoutActive = uiState.isWorkoutActive,
            isPaused = uiState.isPaused,
            isVideoMode = trainingMode == TrainingMode.VIDEO,
            onStart = {
                analyzer.reset()
                voiceGuideManager.resetAll()
                viewModel.startWorkout()
            },
            onPause = viewModel::togglePause,
            onEnd = { viewModel.endWorkout(analyzer.exerciseName) },
            onVideoModeToggle = {
                videoUri = null
                trainingMode = if (trainingMode == TrainingMode.VIDEO) {
                    TrainingMode.CAMERA
                } else {
                    TrainingMode.VIDEO
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        FeedbackOverlay(
            visible = uiState.feedbackMessage != null,
            message = uiState.feedbackMessage.orEmpty(),
            type = uiState.feedbackType
        )

        val result = uiState.workoutResult
        if (uiState.showResultDialog && result != null) {
            TrainingResultDialog(
                result = result,
                onDismiss = {
                    analyzer.reset()
                    viewModel.closeResultAndReset()
                },
                onSaveAndClose = {
                    WorkoutRecordRepository.addWorkoutResult(result)
                    analyzer.reset()
                    viewModel.closeResultAndReset()
                }
            )
        }
    }
}

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
        null -> Unit
    }
}

@Composable
private fun TrainingTopBar(
    title: String,
    isActive: Boolean,
    isPaused: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                Icons.Default.ArrowBack,
                contentDescription = "Back",
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
                IconButton(
                    onClick = onPause,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
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
                        contentDescription = "Reset",
                        tint = Color.White
                    )
                }
            }
        } else {
            IconButton(
                onClick = onStart,
                modifier = Modifier.background(SuccessGreen, CircleShape)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    tint = Color.White
                )
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
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(value = "${result.count}", label = "Completed", color = AccentPurple)
                StatColumn(
                    value = result.phase.label,
                    label = "Current State",
                    color = phaseColor(result.phase.tone)
                )
                StatColumn(value = formatTime(elapsedTime), label = "Duration", color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (result.isReady) SuccessGreen else WarningOrange, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (result.isReady) "Position confirmed" else "Waiting for starting position...",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { result.accuracy / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = phaseColor(result.phase.tone),
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniScore(label = "Depth", value = result.scores.depth)
                MiniScore(label = "Alignment", value = result.scores.alignment)
                MiniScore(label = "Stability", value = result.scores.stability)
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = if (value.length <= 2) 48.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun MiniScore(
    label: String,
    value: Float
) {
    Text(
        text = "$label ${value.toInt()}%",
        fontSize = 12.sp,
        color = Color.White.copy(alpha = 0.82f)
    )
}

@Composable
private fun TrainingControls(
    isWorkoutActive: Boolean,
    isPaused: Boolean,
    isVideoMode: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
    onVideoModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isWorkoutActive) {
            IconButton(
                onClick = onVideoModeToggle,
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isVideoMode) AccentPurple else LightPurple, CircleShape)
            ) {
                Icon(
                    if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideoLibrary,
                    contentDescription = "Video Mode",
                    tint = if (isVideoMode) Color.White else DeepPurple
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

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
                Text("Start", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = onEnd,
                modifier = Modifier
                    .height(56.dp)
                    .width(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "End Workout", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("End", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(16.dp))

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
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraTrainingView(
    poseDetector: PoseDetectorWrapper,
    isWorkoutActive: Boolean,
    isPaused: Boolean,
    onKeypointsDetected: (FloatArray) -> Unit
) {
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (isWorkoutActive && !isPaused) {
                                poseDetector.processFrame(imageProxy, object : PoseDetectorCallback {
                                    override fun onPoseDetected(keypoints: FloatArray, confidence: Float) {
                                        onKeypointsDetected(keypoints)
                                    }

                                    override fun onPoseNotDetected() = Unit
                                    override fun onError(error: String) = Unit
                                })
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                    // Camera errors are non-fatal for the shared training surface.
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun VideoTrainingView(
    onFrame: (FloatArray) -> Unit,
    mockFrame: (Long) -> FloatArray,
    isPlaying: Boolean,
    onPlaybackStateChange: (Boolean) -> Unit,
    onAnalysisComplete: () -> Unit
) {
    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying, isVideoPlaying) {
        if (isPlaying && isVideoPlaying) {
            var frameCount = 0L
            while (isPlaying && isVideoPlaying) {
                delay(100)
                frameCount++
                onFrame(mockFrame(frameCount))
                videoProgress = (frameCount % 1800) / 1800f

                if (frameCount >= 1800) {
                    isVideoPlaying = false
                    onPlaybackStateChange(false)
                    onAnalysisComplete()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Video Playing...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = {
                    isVideoPlaying = !isVideoPlaying
                    onPlaybackStateChange(isVideoPlaying)
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(AccentPurple, CircleShape)
            ) {
                Icon(
                    if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isVideoPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { videoProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 32.dp),
                color = AccentPurple,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun VideoPickerView(
    onVideoSelected: (Uri) -> Unit,
    onBack: () -> Unit,
    videoPickerLauncher: ActivityResultLauncher<String>
) {
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
            text = "Video Analysis",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select a video to analyze your form",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { videoPickerLauncher.launch("video/*") },
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Video")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Back", color = TextSecondary)
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
private fun PermissionRequest(
    onRequestPermission: () -> Unit
) {
    PermissionMessage(
        iconColor = AccentPurple,
        title = "Camera Permission Required",
        message = "Please grant camera permission for motion recognition",
        action = "Grant Permission",
        onAction = onRequestPermission
    )
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit
) {
    PermissionMessage(
        iconColor = WarningOrange,
        title = "Camera Permission Denied",
        message = "Motion recognition requires camera permission. Please enable it in settings.",
        action = "Request Again",
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
