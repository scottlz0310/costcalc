package com.example.shoptools.feature.unitprice.ui.ocr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.shoptools.feature.unitprice.UnitPriceEvent
import com.example.shoptools.feature.unitprice.UnitPriceViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import java.util.concurrent.Executors

@Composable
fun CameraOcrScreen(
    rowId: String,
    unitPriceViewModel: UnitPriceViewModel,
    ocrViewModel: OcrViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    val uiState by ocrViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            unitPriceViewModel.onEvent(
                UnitPriceEvent.ApplyOcrResult(
                    rowId = rowId,
                    price = uiState.confirmedPrice,
                    quantity = uiState.confirmedQuantity,
                    unit = uiState.confirmedUnit,
                    count = uiState.confirmedCount,
                ),
            )
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(ocrViewModel = ocrViewModel)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("カメラの権限が必要です", color = Color.White)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            StepIndicator(step = uiState.currentStep, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.parseError.isNotBlank()) {
                Text(
                    text = uiState.parseError,
                    color = Color.Yellow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xAA000000))
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            CandidateChipRow(
                candidates = uiState.candidates,
                onTap = { ocrViewModel.onEvent(OcrEvent.CandidateTapped(it)) },
                modifier = Modifier.fillMaxWidth(),
            )

            BottomControls(
                step = uiState.currentStep,
                onReset = { ocrViewModel.onEvent(OcrEvent.Reset) },
                onSkip = { ocrViewModel.onEvent(OcrEvent.SkipStep) },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun CameraPreview(ocrViewModel: OcrViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees,
                                )
                                recognizer.process(image)
                                    .addOnSuccessListener { result ->
                                        ocrViewModel.onTextRecognized(
                                            result,
                                            imageProxy.width,
                                            imageProxy.height,
                                        )
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun StepIndicator(step: OcrStep, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xBB000000))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            OcrStep.PRICE to "① 価格",
            OcrStep.QUANTITY to "② 内容量",
            OcrStep.COUNT to "③ 入数",
        ).forEach { (s, label) ->
            Text(
                text = label,
                color = when {
                    s == step -> Color.Yellow
                    s.ordinal < step.ordinal -> Color(0xFF88FF88)
                    else -> Color(0x88FFFFFF)
                },
                style = if (s == step) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.bodySmall
                },
            )
        }
    }
}

@Composable
private fun CandidateChipRow(
    candidates: List<OcrCandidate>,
    onTap: (OcrCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) return
    LazyRow(
        modifier = modifier
            .background(Color(0xBB000000))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(candidates) { candidate ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC333333))
                    .border(
                        width = 1.dp,
                        color = if (candidate.isHighConfidence) Color.Yellow else Color.White,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onTap(candidate) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = candidate.text,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    step: OcrStep,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xBB000000))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "閉じる", tint = Color.White)
        }
        if (step == OcrStep.COUNT) {
            TextButton(onClick = onSkip) {
                Text("スキップ", color = Color.White)
            }
        }
        IconButton(onClick = onReset) {
            Icon(Icons.Default.Refresh, contentDescription = "最初から", tint = Color.White)
        }
    }
}
