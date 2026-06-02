package com.example.shoptools.feature.unitprice.ui.ocr

import android.graphics.Rect

data class OcrCandidate(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect?,
) {
    val isHighConfidence: Boolean get() = confidence >= 0.75f
    val meetsThreshold: Boolean get() = confidence >= 0.5f
}

enum class OcrStep {
    PRICE,
    QUANTITY,
    COUNT,
}

data class ParsedQuantity(
    val value: String,
    val unit: String,
)

data class OcrUiState(
    val currentStep: OcrStep = OcrStep.PRICE,
    val candidates: List<OcrCandidate> = emptyList(),
    val confirmedPrice: String = "",
    val confirmedQuantity: String = "",
    val confirmedUnit: String = "",
    val confirmedCount: String = "",
    val parseError: String = "",
    val isComplete: Boolean = false,
)

sealed interface OcrEvent {
    data class CandidateTapped(val candidate: OcrCandidate) : OcrEvent
    object SkipStep : OcrEvent
    object Reset : OcrEvent
}
