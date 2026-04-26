package com.example.flexfit.ui.screens.workout

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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.flexfit.audio.VoiceGuideManager
import com.example.flexfit.audio.VoiceType
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ml.FeedbackType
import com.example.flexfit.ml.PullUpAnalysisResult
import com.example.flexfit.ml.PullUpAnalyzer
import com.example.flexfit.ml.PullUpState
import com.example.flexfit.ml.PullUpType
import com.example.flexfit.ml.PoseDetectorWrapper
import com.example.flexfit.ml.PoseDetectorCallback
import com.example.flexfit.ml.VoiceAction
import com.example.flexfit.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private val BackgroundDark = Color(0xFF1A1A2E)
private val CardBackground = Color(0xFF252542)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WorkoutScreen() {
    val context = LocalContext.current
    val pullUpType = remember { PullUpType.NORMAL }
    val analyzer = remember { PullUpAnalyzer(pullUpType) }
    val poseDetector = remember { PoseDetectorWrapper() }
    val voiceGuideManager = remember { VoiceGuideManager(context) }

    // Video mode states
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isVideoMode by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }

    var analysisResult by remember { mutableStateOf(PullUpAnalysisResult()) }
    var showFeedback by remember { mutableStateOf<String?>(null) }
    var feedbackType by remember { mutableStateOf(FeedbackType.INFO) }
    var isWorkoutActive by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var lastKeypoints by remember { mutableStateOf<FloatArray?>(null) }
    var feedbackVisible by remember { mutableStateOf(false) }

    // Workout result states
    var showResultDialog by remember { mutableStateOf(false) }
    var workoutResult by remember { mutableStateOf<WorkoutResult?>(null) }
    var totalErrors by remember { mutableStateOf(0) }
    var totalWarnings by remember { mutableStateOf(0) }
    var accuracySum by remember { mutableStateOf(0f) }
    var accuracyCount by remember { mutableStateOf(0) }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            videoUri = it
            isAnalyzing = true
        }
    }

    // Initialize pose detector
    LaunchedEffect(Unit) {
        try {
            poseDetector.initialize()
        } catch (e: Exception) {
            // Handle initialization error
        }
    }

    // Release resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceGuideManager.release()
            poseDetector.close()
        }
    }

    LaunchedEffect(showFeedback) {
        if (showFeedback != null) {
            feedbackVisible = true
            delay(2000)
            feedbackVisible = false
            delay(300)
            showFeedback = null
        }
    }

    // Timer
    LaunchedEffect(isWorkoutActive, isPaused) {
        if (isWorkoutActive && !isPaused) {
            while (true) {
                delay(1000)
                elapsedTime++
            }
        }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when {
            // Video mode with video selected
            isVideoMode && videoUri != null && isAnalyzing -> {
                VideoTrainingView(
                    videoUri = videoUri!!,
                    analyzer = analyzer,
                    onResultUpdate = { result ->
                        analysisResult = result
                        result.feedback?.let { feedback ->
                            showFeedback = feedback.message
                            feedbackType = feedback.type
                        }
                        // Handle voice action - play English audio file only
                        result.voiceAction?.let { action ->
                            when (action) {
                                VoiceAction.START -> voiceGuideManager.playVoice(VoiceType.START)
                                VoiceAction.SUCCESS -> voiceGuideManager.playVoice(VoiceType.SUCCESS)
                                VoiceAction.FAIL -> voiceGuideManager.playVoice(VoiceType.FAIL)
                                VoiceAction.SWINGING -> voiceGuideManager.triggerWarning(VoiceType.SWINGING)
                                VoiceAction.SHRUGGING -> voiceGuideManager.triggerWarning(VoiceType.SHRUGGING)
                                VoiceAction.NOT_HIGH -> voiceGuideManager.triggerWarning(VoiceType.NOT_HIGH)
                            }
                        }
                        // Update accuracy tracking
                        if (isWorkoutActive && result.accuracy > 0) {
                            accuracySum += result.accuracy
                            accuracyCount++
                            when (result.feedback?.type) {
                                FeedbackType.ERROR -> totalErrors++
                                FeedbackType.WARNING -> totalWarnings++
                                else -> {}
                            }
                        }
                    },
                    onKeypointsUpdate = { keypoints ->
                        lastKeypoints = keypoints
                    },
                    isPlaying = !isPaused && isWorkoutActive,
                    onPlaybackStateChange = { playing ->
                        if (playing) {
                            isWorkoutActive = true
                            isPaused = false
                        } else {
                            isPaused = true
                        }
                    },
                    onAnalysisComplete = {
                        isWorkoutActive = false
                        isAnalyzing = false
                    }
                )

                // Top bar
                WorkoutTopBar(
                    onBack = {
                        isAnalyzing = false
                        videoUri = null
                        isVideoMode = false
                    },
                    onReset = {
                        analyzer.reset()
                        analysisResult = PullUpAnalysisResult()
                        isWorkoutActive = false
                        isPaused = false
                        elapsedTime = 0
                    }
                )

                // Bottom panel
                WorkoutBottomPanel(
                    result = analysisResult,
                    elapsedTime = elapsedTime,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Video mode - show picker
            isVideoMode && videoUri == null -> {
                VideoModeView(
                    onVideoSelected = { uri ->
                        videoUri = uri
                    },
                    onBack = { isVideoMode = false },
                    videoPickerLauncher = videoPickerLauncher
                )
            }

            // Camera mode
            cameraPermissionState.status.isGranted -> {
                CameraTrainingView(
                    analyzer = analyzer,
                    poseDetector = poseDetector,
                    isWorkoutActive = isWorkoutActive,
                    isPaused = isPaused,
                    onResultUpdate = { result ->
                        analysisResult = result
                        result.feedback?.let { feedback ->
                            showFeedback = feedback.message
                            feedbackType = feedback.type
                        }
                        // Handle voice action - play English audio file only
                        result.voiceAction?.let { action ->
                            when (action) {
                                VoiceAction.START -> voiceGuideManager.playVoice(VoiceType.START)
                                VoiceAction.SUCCESS -> voiceGuideManager.playVoice(VoiceType.SUCCESS)
                                VoiceAction.FAIL -> voiceGuideManager.playVoice(VoiceType.FAIL)
                                VoiceAction.SWINGING -> voiceGuideManager.triggerWarning(VoiceType.SWINGING)
                                VoiceAction.SHRUGGING -> voiceGuideManager.triggerWarning(VoiceType.SHRUGGING)
                                VoiceAction.NOT_HIGH -> voiceGuideManager.triggerWarning(VoiceType.NOT_HIGH)
                            }
                        }
                        // Update accuracy tracking
                        if (isWorkoutActive && result.accuracy > 0) {
                            accuracySum += result.accuracy
                            accuracyCount++
                            when (result.feedback?.type) {
                                FeedbackType.ERROR -> totalErrors++
                                FeedbackType.WARNING -> totalWarnings++
                                else -> {}
                            }
                        }
                    },
                    onKeypointsUpdate = { keypoints ->
                        lastKeypoints = keypoints
                    }
                )

                // Top bar
                WorkoutTopBar(
                    onBack = { /* Navigate back */ },
                    onReset = {
                        analyzer.reset()
                        analysisResult = PullUpAnalysisResult()
                        isWorkoutActive = false
                        isPaused = false
                        elapsedTime = 0
                    }
                )

                // Bottom panel - positioned above controls
                WorkoutBottomPanel(
                    result = analysisResult,
                    elapsedTime = elapsedTime,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                )

                // Control buttons - at the very bottom
                WorkoutControls(
                    isWorkoutActive = isWorkoutActive,
                    isPaused = isPaused,
                    isVideoMode = isVideoMode,
                    onStart = {
                        // Reset counters when starting
                        totalErrors = 0
                        totalWarnings = 0
                        accuracySum = 0f
                        accuracyCount = 0
                        voiceGuideManager.resetAll()
                        isWorkoutActive = true
                    },
                    onPause = { isPaused = !isPaused },
                    onEnd = {
                        // Generate workout result
                        val avgAccuracy = if (accuracyCount > 0) accuracySum / accuracyCount else 0f
                        workoutResult = WorkoutResult(
                            exerciseType = "Pull Up",
                            durationSeconds = elapsedTime,
                            totalReps = analysisResult.count + (0..2).random(),
                            completedReps = analysisResult.count,
                            averageAccuracy = avgAccuracy,
                            caloriesBurned = (elapsedTime / 60f * 8f),
                            errorsCount = totalErrors,
                            warningsCount = totalWarnings,
                            feedbackMessages = listOf(
                                if (avgAccuracy >= 85) "Great form!" else "Keep practicing!",
                                if (totalErrors == 0) "No errors!" else "Watch your form",
                                if (totalWarnings <= 3) "Good stability!" else "Try to stay more stable"
                            )
                        )
                        showResultDialog = true
                        isWorkoutActive = false
                        isPaused = false
                    },
                    onVideoModeToggle = {
                        if (isVideoMode) {
                            isVideoMode = false
                            isAnalyzing = false
                        } else {
                            isVideoMode = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            cameraPermissionState.status.shouldShowRationale -> {
                WorkoutPermissionRationale(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }

            else -> {
                WorkoutPermissionRequest(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
        }

        // Skeleton overlay (above camera, below controls)
        WorkoutSkeletonOverlay(
            keypoints = lastKeypoints,
            modifier = Modifier.fillMaxSize()
        )

        // Feedback overlay (above everything)
        WorkoutFeedbackOverlay(
            visible = feedbackVisible && showFeedback != null,
            message = showFeedback ?: "",
            type = feedbackType
        )

        // Workout Result Dialog
        if (showResultDialog && workoutResult != null) {
            WorkoutResultDialog(
                result = workoutResult!!,
                onDismiss = {
                    showResultDialog = false
                    workoutResult = null
                    elapsedTime = 0
                    analyzer.reset()
                    analysisResult = PullUpAnalysisResult()
                },
                onSaveAndClose = {
                    // Save to repository
                    workoutResult?.let { result ->
                        WorkoutRecordRepository.addWorkoutResult(result)
                    }
                    showResultDialog = false
                    workoutResult = null
                    elapsedTime = 0
                    analyzer.reset()
                    analysisResult = PullUpAnalysisResult()
                }
            )
        }
    }
}

@Composable
private fun WorkoutTopBar(
    onBack: () -> Unit,
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
            text = "Pull-up Training",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Reset",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun WorkoutBottomPanel(
    result: PullUpAnalysisResult,
    elapsedTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
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
                // Count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${result.count}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                    Text(
                        text = "Completed",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // State
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (result.state) {
                            PullUpState.PREPARING -> "Ready"
                            PullUpState.DOWN -> "Arms Extended"
                            PullUpState.UP -> "Pulling Up"
                            PullUpState.TOP -> "Top Position"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (result.state) {
                            PullUpState.PREPARING -> TextSecondary
                            PullUpState.DOWN -> WarningOrange
                            PullUpState.UP -> AccentPurple
                            PullUpState.TOP -> SuccessGreen
                        }
                    )
                    Text(
                        text = "Current State",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTimeWorkout(elapsedTime),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Duration",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when {
                    result.isReady -> SuccessGreen
                    result.state != PullUpState.PREPARING -> AccentPurple
                    else -> WarningOrange
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (result.isReady) "Position confirmed" else "Waiting for starting position...",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun WorkoutFeedbackOverlay(
    visible: Boolean,
    message: String,
    type: FeedbackType
) {
    if (visible && message.isNotEmpty()) {
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

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor.copy(alpha = 0.9f)
                ),
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
private fun WorkoutSkeletonOverlay(
    keypoints: FloatArray?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        keypoints?.let { kp ->
            if (kp.size >= 57) {
                val strokeWidth = 4.dp.toPx()
                val pointRadius = 8.dp.toPx()

                val connections = listOf(
                    Pair(11, 12), Pair(12, 13), Pair(13, 14),
                    Pair(11, 15), Pair(15, 16), Pair(16, 17),
                    Pair(11, 4), Pair(4, 1),
                    Pair(4, 5), Pair(5, 6),
                    Pair(1, 2), Pair(2, 3)
                )

                connections.forEach { (start, end) ->
                    val startX = size.width * (0.5f + kp[start * 3])
                    val startY = size.height * (0.5f - kp[start * 3 + 1])
                    val endX = size.width * (0.5f + kp[end * 3])
                    val endY = size.height * (0.5f - kp[end * 3 + 1])

                    drawLine(
                        color = AccentPurple,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                for (i in 0 until 19) {
                    val x = size.width * (0.5f + kp[i * 3])
                    val y = size.height * (0.5f - kp[i * 3 + 1])

                    drawCircle(
                        color = DeepPurple,
                        radius = pointRadius,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = pointRadius * 0.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutControls(
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
            // Video mode toggle
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

            // Start button
            Button(
                onClick = onStart,
                modifier = Modifier
                    .height(56.dp)
                    .width(150.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start", fontWeight = FontWeight.SemiBold)
            }
        } else {
            // End button
            Button(
                onClick = onEnd,
                modifier = Modifier
                    .height(56.dp)
                    .width(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "End Workout",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("End", fontWeight = FontWeight.SemiBold)
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
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraTrainingView(
    analyzer: PullUpAnalyzer,
    poseDetector: PoseDetectorWrapper,
    isWorkoutActive: Boolean,
    isPaused: Boolean,
    onResultUpdate: (PullUpAnalysisResult) -> Unit,
    onKeypointsUpdate: (FloatArray?) -> Unit
) {
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            poseDetector.close()
        }
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
                                        val timestamp = System.currentTimeMillis()
                                        val result = analyzer.analyze(keypoints, timestamp)
                                        onResultUpdate(result)
                                        onKeypointsUpdate(keypoints)
                                    }
                                    override fun onPoseNotDetected() {}
                                    override fun onError(error: String) {}
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun VideoModeView(
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
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select a video to analyze your pull-up form",
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
private fun VideoTrainingView(
    videoUri: Uri,
    analyzer: PullUpAnalyzer,
    onResultUpdate: (PullUpAnalysisResult) -> Unit,
    onKeypointsUpdate: (FloatArray?) -> Unit,
    isPlaying: Boolean,
    onPlaybackStateChange: (Boolean) -> Unit,
    onAnalysisComplete: () -> Unit
) {
    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying, isVideoPlaying) {
        if (isPlaying && isVideoPlaying) {
            var frameCount = 0L
            while (isVideoPlaying && isPlaying) {
                delay(100)
                frameCount++

                val mockKeypoints = generateMockKeypointsWorkout(frameCount)
                val timestamp = System.currentTimeMillis()
                val result = analyzer.analyze(mockKeypoints, timestamp)

                onResultUpdate(result)
                onKeypointsUpdate(mockKeypoints)
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
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
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
            }

            Spacer(modifier = Modifier.height(8.dp))

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
private fun WorkoutPermissionRequest(onRequestPermission: () -> Unit) {
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
            tint = AccentPurple,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please grant camera permission for motion recognition",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun WorkoutPermissionRationale(onRequestPermission: () -> Unit) {
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
            tint = WarningOrange,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Camera Permission Denied",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Motion recognition requires camera permission. Please enable it in settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text("Request Again")
        }
    }
}

private fun formatTimeWorkout(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

private fun generateMockKeypointsWorkout(frameCount: Long): FloatArray {
    val keypoints = FloatArray(19 * 3)
    val cyclePhase = (frameCount % 180) / 180f * 2 * Math.PI

    keypoints[11 * 3] = -0.15f
    keypoints[11 * 3 + 1] = 0.25f + 0.05f * kotlin.math.sin(cyclePhase).toFloat()
    keypoints[11 * 3 + 2] = 0f
    keypoints[14 * 3] = 0.15f
    keypoints[14 * 3 + 1] = 0.25f + 0.05f * kotlin.math.sin(cyclePhase).toFloat()
    keypoints[14 * 3 + 2] = 0f

    val elbowBend = kotlin.math.cos(cyclePhase).toFloat() * 0.15f + 0.3f
    keypoints[12 * 3] = -0.2f - elbowBend * 0.3f
    keypoints[12 * 3 + 1] = 0.15f + elbowBend * 0.1f
    keypoints[12 * 3 + 2] = 0.1f
    keypoints[15 * 3] = 0.2f + elbowBend * 0.3f
    keypoints[15 * 3 + 1] = 0.15f + elbowBend * 0.1f
    keypoints[15 * 3 + 2] = -0.1f

    val wristLift = (kotlin.math.cos(cyclePhase).toFloat() + 1f) * 0.2f
    keypoints[13 * 3] = -0.25f - elbowBend * 0.4f
    keypoints[13 * 3 + 1] = 0.1f + wristLift
    keypoints[13 * 3 + 2] = 0.15f
    keypoints[16 * 3] = 0.25f + elbowBend * 0.4f
    keypoints[16 * 3 + 1] = 0.1f + wristLift
    keypoints[16 * 3 + 2] = -0.15f

    keypoints[1 * 3] = 0.08f
    keypoints[1 * 3 + 1] = -0.15f
    keypoints[1 * 3 + 2] = 0f
    keypoints[4 * 3] = -0.08f
    keypoints[4 * 3 + 1] = -0.15f
    keypoints[4 * 3 + 2] = 0f

    val chinUp = (kotlin.math.cos(cyclePhase).toFloat() + 1f) * 0.1f
    keypoints[10 * 3] = 0f
    keypoints[10 * 3 + 1] = 0.38f + chinUp
    keypoints[10 * 3 + 2] = 0f
    keypoints[17 * 3] = -0.06f
    keypoints[17 * 3 + 1] = 0.36f + chinUp
    keypoints[17 * 3 + 2] = 0.05f
    keypoints[18 * 3] = 0.06f
    keypoints[18 * 3 + 1] = 0.36f + chinUp
    keypoints[18 * 3 + 2] = -0.05f

    return keypoints
}
