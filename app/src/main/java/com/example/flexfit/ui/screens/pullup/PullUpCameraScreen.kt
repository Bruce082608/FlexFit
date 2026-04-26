package com.example.flexfit.ui.screens.pullup

import android.Manifest
import android.graphics.Bitmap
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.flexfit.ml.FeedbackType
import com.example.flexfit.ml.PullUpAnalysisResult
import com.example.flexfit.ml.PullUpAnalyzer
import com.example.flexfit.ml.PullUpState
import com.example.flexfit.ml.PullUpType
import com.example.flexfit.ml.PoseDetectorWrapper
import com.example.flexfit.ml.PoseDetectorCallback
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private val DeepPurple = Color(0xFF5E35B1)
private val AccentPurple = Color(0xFF7E57C2)
private val LightPurple = Color(0xFFB39DDB)
private val BackgroundDark = Color(0xFF1A1A2E)
private val CardBackground = Color(0xFF252542)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0BEC5)
private val SuccessGreen = Color(0xFF4CAF50)
private val WarningOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFF44336)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PullUpCameraScreen(
    exerciseType: String,
    mode: String,  // "camera" or "video"
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val pullUpType = remember(exerciseType) {
        when (exerciseType.lowercase()) {
            "wide" -> PullUpType.WIDE
            "normal" -> PullUpType.NORMAL
            "narrow" -> PullUpType.NARROW
            else -> PullUpType.NORMAL
        }
    }

    val analyzer = remember { PullUpAnalyzer(pullUpType) }
    val poseDetector = remember { PoseDetectorWrapper() }

    // Video mode states
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isVideoMode by remember { mutableStateOf(mode == "video") }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            videoUri = it
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

    var analysisResult by remember { mutableStateOf(PullUpAnalysisResult()) }
    var showFeedback by remember { mutableStateOf<String?>(null) }
    var feedbackType by remember { mutableStateOf(FeedbackType.INFO) }
    var isWorkoutActive by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var cameraFrameCount by remember { mutableStateOf(0L) }
    var lastKeypoints by remember { mutableStateOf<FloatArray?>(null) }

    // Feedback animation
    var feedbackVisible by remember { mutableStateOf(false) }
    val feedbackAlpha by animateFloatAsState(
        targetValue = if (feedbackVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "feedbackAlpha"
    )

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

    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when {
            // Video mode with selected video
            isVideoMode && videoUri != null && isAnalyzing -> {
                // Video analysis view
                VideoAnalysisView(
                    videoUri = videoUri!!,
                    analyzer = analyzer,
                    onResultUpdate = { result ->
                        analysisResult = result
                        result.feedback?.let { feedback ->
                            showFeedback = feedback.message
                            feedbackType = feedback.type
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

                // Overlay UI
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TopOverlay(
                        exerciseType = pullUpType,
                        isActive = isWorkoutActive,
                        onBack = {
                            isAnalyzing = false
                            videoUri = null
                        },
                        onStart = { isWorkoutActive = true; isPaused = false },
                        onPause = { isPaused = !isPaused },
                        onReset = {
                            analyzer.reset()
                            analysisResult = PullUpAnalysisResult()
                            isWorkoutActive = false
                            isPaused = false
                            elapsedTime = 0
                            cameraFrameCount = 0
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    BottomInfoPanel(
                        result = analysisResult,
                        elapsedTime = elapsedTime
                    )
                }

                // Feedback overlay
                AnimatedVisibility(
                    visible = feedbackVisible && showFeedback != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                ) {
                    FeedbackCard(
                        message = showFeedback ?: "",
                        type = feedbackType
                    )
                }

                // Skeleton overlay
                SkeletonOverlay(
                    keypoints = lastKeypoints,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Video mode - show video picker
            isVideoMode && videoUri == null -> {
                VideoPickerView(
                    onVideoSelected = { uri ->
                        videoUri = uri
                        isAnalyzing = true
                    },
                    onBack = onNavigateBack
                )
            }

            // Camera mode with permission granted
            cameraPermissionState.status.isGranted && !isVideoMode -> {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrameAnalyzed = { imageProxy ->
                        if (isWorkoutActive && !isPaused) {
                            poseDetector.processFrame(imageProxy, object : PoseDetectorCallback {
                                override fun onPoseDetected(keypoints: FloatArray, confidence: Float) {
                                    val timestamp = System.currentTimeMillis()
                                    val result = analyzer.analyze(keypoints, timestamp)
                                    analysisResult = result
                                    lastKeypoints = keypoints

                                    result.feedback?.let { feedback ->
                                        showFeedback = feedback.message
                                        feedbackType = feedback.type
                                    }
                                }

                                override fun onPoseNotDetected() {
                                    // Pose not detected
                                }

                                override fun onError(error: String) {
                                    // Handle error
                                }
                            })
                        } else {
                            imageProxy.close()
                        }
                    }
                )

                // Overlay UI
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TopOverlay(
                        exerciseType = pullUpType,
                        isActive = isWorkoutActive,
                        onBack = onNavigateBack,
                        onStart = { isWorkoutActive = true },
                        onPause = { isPaused = !isPaused },
                        onReset = {
                            analyzer.reset()
                            analysisResult = PullUpAnalysisResult()
                            isWorkoutActive = false
                            isPaused = false
                            elapsedTime = 0
                            cameraFrameCount = 0
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    BottomInfoPanel(
                        result = analysisResult,
                        elapsedTime = elapsedTime
                    )
                }

                // Feedback overlay
                AnimatedVisibility(
                    visible = feedbackVisible && showFeedback != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                ) {
                    FeedbackCard(
                        message = showFeedback ?: "",
                        type = feedbackType
                    )
                }

                // Skeleton overlay
                SkeletonOverlay(
                    keypoints = lastKeypoints,
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
    }
}

@Composable
private fun VideoPickerView(
    onVideoSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onVideoSelected(it) }
    }

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
            Icon(
                Icons.Default.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
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
private fun VideoAnalysisView(
    videoUri: Uri,
    analyzer: PullUpAnalyzer,
    onResultUpdate: (PullUpAnalysisResult) -> Unit,
    onKeypointsUpdate: (FloatArray?) -> Unit,
    isPlaying: Boolean,
    onPlaybackStateChange: (Boolean) -> Unit,
    onAnalysisComplete: () -> Unit
) {
    val context = LocalContext.current

    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf(0f) }
    var videoDuration by remember { mutableStateOf(0L) }

    // Simulated video analysis using mock data
    LaunchedEffect(isPlaying, isVideoPlaying) {
        if (isPlaying && isVideoPlaying) {
            // Simulate video playback with frame analysis
            var frameCount = 0L
            while (isVideoPlaying && isPlaying) {
                delay(100)
                frameCount++

                // Generate mock keypoints for demo
                val mockKeypoints = generateMockKeypoints(frameCount)
                val timestamp = System.currentTimeMillis()
                val result = analyzer.analyze(mockKeypoints, timestamp)

                onResultUpdate(result)
                onKeypointsUpdate(mockKeypoints)

                // Update progress
                videoProgress = (frameCount % 1800) / 1800f

                if (frameCount >= 1800) { // Simulate end of video
                    isVideoPlaying = false
                    onPlaybackStateChange(false)
                    onAnalysisComplete()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video placeholder (in real app, use ExoPlayer)
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
                Text(
                    text = "Video Playing...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Progress bar at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
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

            // Progress indicator
            LinearProgressIndicator(
                progress = { videoProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentPurple,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun TopOverlay(
    exerciseType: PullUpType,
    isActive: Boolean,
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
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = exerciseType.displayName + " Pull-up",
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
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (false) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
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
                modifier = Modifier
                    .background(SuccessGreen, CircleShape)
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
private fun BottomInfoPanel(
    result: PullUpAnalysisResult,
    elapsedTime: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                // Count display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                // State indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                // Time display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatTime(elapsedTime),
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

            // Status bar
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
private fun FeedbackCard(
    message: String,
    type: com.example.flexfit.ml.FeedbackType
) {
    val backgroundColor = when (type) {
        com.example.flexfit.ml.FeedbackType.SUCCESS -> SuccessGreen
        com.example.flexfit.ml.FeedbackType.WARNING -> WarningOrange
        com.example.flexfit.ml.FeedbackType.ERROR -> ErrorRed
        com.example.flexfit.ml.FeedbackType.INFO -> AccentPurple
    }

    val icon = when (type) {
        com.example.flexfit.ml.FeedbackType.SUCCESS -> Icons.Default.CheckCircle
        com.example.flexfit.ml.FeedbackType.WARNING -> Icons.Default.Warning
        com.example.flexfit.ml.FeedbackType.ERROR -> Icons.Default.Cancel
        com.example.flexfit.ml.FeedbackType.INFO -> Icons.Default.Info
    }

    Card(
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

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrameAnalyzed: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val poseDetector = remember { PoseDetectorWrapper() }

    // Initialize pose detector
    LaunchedEffect(Unit) {
        try {
            poseDetector.initialize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
        modifier = modifier,
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
                            onFrameAnalyzed(imageProxy)
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
private fun SkeletonOverlay(
    keypoints: FloatArray?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Draw skeleton lines when keypoints are available
        keypoints?.let { kp ->
            if (kp.size >= 57) { // 19 keypoints * 3
                val strokeWidth = 4.dp.toPx()
                val pointRadius = 8.dp.toPx()

                val purpleColor = AccentPurple

                // Draw connections
                val connections = listOf(
                    // Left arm
                    Pair(11, 12), Pair(12, 13), Pair(13, 14),
                    // Right arm
                    Pair(11, 15), Pair(15, 16), Pair(16, 17),
                    // Body
                    Pair(11, 4), Pair(4, 1),
                    // Left leg
                    Pair(4, 5), Pair(5, 6),
                    // Right leg
                    Pair(1, 2), Pair(2, 3)
                )

                connections.forEach { (start, end) ->
                    val startX = size.width * (0.5f + kp[start * 3])
                    val startY = size.height * (0.5f - kp[start * 3 + 1])
                    val endX = size.width * (0.5f + kp[end * 3])
                    val endY = size.height * (0.5f - kp[end * 3 + 1])

                    drawLine(
                        color = purpleColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Draw keypoints
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
private fun PermissionRequest(
    onRequestPermission: () -> Unit
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
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple
            )
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit
) {
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
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple
            )
        ) {
            Text("Request Again")
        }
    }
}

// Helper functions
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

// Mock keypoint generator for simulation (replace with real pose detection)
private fun generateMockKeypoints(frameCount: Long): FloatArray {
    val keypoints = FloatArray(19 * 3)

    // Simulate a pull-up cycle based on frame count
    val cyclePhase = (frameCount % 180) / 180f * 2 * Math.PI

    // Basic body proportions
    // Shoulders (roughly at x=0, y=0.3)
    keypoints[11 * 3] = -0.15f  // Left shoulder x
    keypoints[11 * 3 + 1] = 0.25f + 0.05f * kotlin.math.sin(cyclePhase).toFloat()
    keypoints[11 * 3 + 2] = 0f

    keypoints[14 * 3] = 0.15f  // Right shoulder x
    keypoints[14 * 3 + 1] = 0.25f + 0.05f * kotlin.math.sin(cyclePhase).toFloat()
    keypoints[14 * 3 + 2] = 0f

    // Elbows (arms bent during pull)
    val elbowBend = kotlin.math.cos(cyclePhase).toFloat() * 0.15f + 0.3f
    keypoints[12 * 3] = -0.2f - elbowBend * 0.3f  // Left elbow
    keypoints[12 * 3 + 1] = 0.15f + elbowBend * 0.1f
    keypoints[12 * 3 + 2] = 0.1f

    keypoints[15 * 3] = 0.2f + elbowBend * 0.3f  // Right elbow
    keypoints[15 * 3 + 1] = 0.15f + elbowBend * 0.1f
    keypoints[15 * 3 + 2] = -0.1f

    // Wrists (move up and down with pull)
    val wristLift = (kotlin.math.cos(cyclePhase).toFloat() + 1f) * 0.2f
    keypoints[13 * 3] = -0.25f - elbowBend * 0.4f  // Left wrist
    keypoints[13 * 3 + 1] = 0.1f + wristLift
    keypoints[13 * 3 + 2] = 0.15f

    keypoints[16 * 3] = 0.25f + elbowBend * 0.4f  // Right wrist
    keypoints[16 * 3 + 1] = 0.1f + wristLift
    keypoints[16 * 3 + 2] = -0.15f

    // Hips (stable position)
    keypoints[1 * 3] = 0.08f  // Right hip
    keypoints[1 * 3 + 1] = -0.15f
    keypoints[1 * 3 + 2] = 0f

    keypoints[4 * 3] = -0.08f  // Left hip
    keypoints[4 * 3 + 1] = -0.15f
    keypoints[4 * 3 + 2] = 0f

    // Head (nose position)
    val chinUp = (kotlin.math.cos(cyclePhase).toFloat() + 1f) * 0.1f
    keypoints[10 * 3] = 0f  // Nose
    keypoints[10 * 3 + 1] = 0.38f + chinUp
    keypoints[10 * 3 + 2] = 0f

    // Ears
    keypoints[17 * 3] = -0.06f  // Left ear
    keypoints[17 * 3 + 1] = 0.36f + chinUp
    keypoints[17 * 3 + 2] = 0.05f

    keypoints[18 * 3] = 0.06f  // Right ear
    keypoints[18 * 3 + 1] = 0.36f + chinUp
    keypoints[18 * 3 + 2] = -0.05f

    return keypoints
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
}
