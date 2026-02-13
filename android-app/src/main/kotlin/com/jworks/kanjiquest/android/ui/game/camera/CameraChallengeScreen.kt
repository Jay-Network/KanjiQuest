package com.jworks.kanjiquest.android.ui.game.camera

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraChallengeScreen(
    onBack: () -> Unit,
    targetKanjiId: Int? = null,
    viewModel: CameraChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            viewModel.startSession(targetKanjiId)
        }
    }

    LaunchedEffect(Unit) {
        if (hasCameraPermission) {
            viewModel.startSession(targetKanjiId)
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Challenge") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (!hasCameraPermission) {
                PermissionDeniedContent(onRequestPermission = {
                    launcher.launch(Manifest.permission.CAMERA)
                })
            } else {
                when (val state = uiState.state) {
                    is CameraChallengeState.Loading -> LoadingContent()
                    is CameraChallengeState.ShowTarget,
                    is CameraChallengeState.Success -> {
                        // Keep camera running throughout session, overlay UI on top
                        CameraWithOverlay(
                            state = uiState.state,
                            onTextRecognized = { viewModel.onTextRecognized(it) },
                            onNext = { viewModel.nextChallenge() },
                            lifecycleOwner = lifecycleOwner
                        )
                    }
                    is CameraChallengeState.SessionComplete -> {
                        SessionCompleteContent(
                            state = state,
                            onDone = {
                                viewModel.reset()
                                onBack()
                            }
                        )
                    }
                    is CameraChallengeState.Error -> {
                        ErrorContent(message = state.message, onBack = onBack)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraWithOverlay(
    state: CameraChallengeState,
    onTextRecognized: (String) -> Unit,
    onNext: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val textRecognizer = remember {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            textRecognizer.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview - stays alive throughout session
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(executor) { imageProxy ->
                                // Only process if in ShowTarget state
                                if (state is CameraChallengeState.ShowTarget) {
                                    processImageProxy(imageProxy, textRecognizer, onTextRecognized)
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI based on state
        when (state) {
            is CameraChallengeState.ShowTarget -> {
                TargetOverlay(state = state)
            }
            is CameraChallengeState.Success -> {
                SuccessOverlay(state = state, onNext = onNext)
            }
            else -> {}
        }
    }
}

@Composable
private fun TargetOverlay(state: CameraChallengeState.ShowTarget) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress and stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${state.challengeNumber} / ${state.totalChallenges}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Text(
                text = "${state.sessionXp} XP",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Target kanji card
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Find and scan this kanji:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                com.jworks.kanjiquest.android.ui.theme.KanjiText(
                    text = state.targetKanji.literal,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.targetKanji.primaryMeaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Scanning indicator
        Text(
            text = "Scanning...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .padding(32.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SuccessOverlay(
    state: CameraChallengeState.Success,
    onNext: () -> Unit
) {
    // Semi-transparent green overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4CAF50).copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✓",
                        fontSize = 120.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Success!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+${state.xpGained} XP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    com.jworks.kanjiquest.android.ui.theme.KanjiText(
                        text = state.targetKanji.literal,
                        fontSize = 80.sp,
                        color = Color.White
                    )
                    Text(
                        text = state.targetKanji.primaryMeaning,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (state.challengeNumber < state.totalChallenges) "Next Challenge" else "Finish",
                            fontSize = 18.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCompleteContent(
    state: CameraChallengeState.SessionComplete,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Challenge Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow("Total Challenges", "${state.totalChallenges}")
                StatRow("Successfully Scanned", "${state.successCount}")
                StatRow("Accuracy", "${state.accuracy}%")
                StatRow("XP Earned", "+${state.totalXp}")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", fontSize = 18.sp)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Preparing camera challenges...",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera permission is required for this mode",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) {
            Text("Go Back")
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    onTextRecognized: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                if (recognizedText.isNotEmpty()) {
                    onTextRecognized(recognizedText)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
