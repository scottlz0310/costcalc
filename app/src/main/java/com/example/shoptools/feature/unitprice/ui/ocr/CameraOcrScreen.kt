package com.example.shoptools.feature.unitprice.ui.ocr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.OutputTransform
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.shoptools.feature.unitprice.UnitPriceEvent
import com.example.shoptools.feature.unitprice.UnitPriceViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

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
            CameraPreviewWithOverlay(
                ocrViewModel = ocrViewModel,
                candidates = uiState.candidates,
            )
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
private fun CameraPreviewWithOverlay(
    ocrViewModel: OcrViewModel,
    candidates: List<OcrCandidate>,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    val viewTransformRef = remember { AtomicReference<OutputTransform?>(null) }
    val textMeasurer = rememberTextMeasurer()

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // getOutputTransform() は COMPATIBLE mode（TextureView）で正確に動作する
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setResolutionSelector(
                                androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                                    .setAspectRatioStrategy(
                                        androidx.camera.core.resolutionselector.AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
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
                                        val currentStep = ocrViewModel.uiState.value.currentStep
                                        recognizer.process(image)
                                            .addOnSuccessListener { result ->
                                                // メインスレッド — viewTransform を取得して座標変換
                                                val blocks = result.textBlocks.map { block ->
                                                    val confidence = block.lines
                                                        .flatMap { it.elements }
                                                        .mapNotNull { it.confidence }
                                                        .let { cs ->
                                                            if (cs.isEmpty()) 0.5f
                                                            else cs.average().toFloat()
                                                        }
                                                    TextBlock(block.text, confidence, block.boundingBox)
                                                }
                                                val rawCandidates = when (currentStep) {
                                                    OcrStep.PRICE -> TextParser.extractPriceCandidates(
                                                        blocks,
                                                        android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
                                                    )
                                                    OcrStep.QUANTITY -> TextParser.extractQuantityCandidates(blocks)
                                                    OcrStep.COUNT -> TextParser.extractCountCandidates(blocks)
                                                }
                                                val viewTransform = viewTransformRef.get()
                                                val mappedCandidates = OcrCoordinateMapper.mapCandidates(
                                                    rawCandidates,
                                                    imageProxy,
                                                    viewTransform,
                                                )
                                                ocrViewModel.onCandidatesDetected(
                                                    mappedCandidates,
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
                }
            },
            update = { previewView ->
                // メインスレッドで outputTransform を更新
                viewTransformRef.set(OcrCoordinateMapper.getViewTransform(previewView))
            },
            modifier = Modifier.fillMaxSize(),
        )

        // AR オーバーレイ: バウンディングボックス + テキストチップ
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(candidates) {
                    detectTapGestures { offset ->
                        val tapped = candidates.firstOrNull { candidate ->
                            candidate.boundingBoxView?.contains(offset.x, offset.y) == true
                        }
                        tapped?.let { ocrViewModel.onEvent(OcrEvent.CandidateTapped(it)) }
                    }
                },
        ) {
            candidates.forEach { candidate ->
                val rect = candidate.boundingBoxView ?: return@forEach
                val isHigh = candidate.isHighConfidence
                val borderColor = if (isHigh) Color.Yellow else Color.White
                val fillAlpha = (candidate.confidence * 0.35f).coerceIn(0.1f, 0.35f)

                // バウンディングボックス（塗り）
                drawRect(
                    color = borderColor.copy(alpha = fillAlpha),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width(), rect.height()),
                )
                // バウンディングボックス（枠線）
                drawRect(
                    color = borderColor,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width(), rect.height()),
                    style = Stroke(width = if (isHigh) 3f else 1.5f),
                )
                // テキストラベル
                val textLayout = textMeasurer.measure(
                    candidate.text,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isHigh) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                val labelX = rect.left.coerceAtLeast(0f)
                val labelY = (rect.top - textLayout.size.height).coerceAtLeast(0f)
                drawRect(
                    color = Color(0xCC000000),
                    topLeft = Offset(labelX, labelY),
                    size = Size(textLayout.size.width.toFloat(), textLayout.size.height.toFloat()),
                )
                drawText(textLayout, topLeft = Offset(labelX, labelY))
            }
        }
    }
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
