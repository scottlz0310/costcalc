package com.example.shoptools.feature.unitprice.ui.ocr

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.text.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    fun onTextRecognized(result: Text, imageWidth: Int, imageHeight: Int) {
        val blocks = result.textBlocks.map { block ->
            val confidence = block.lines
                .flatMap { it.elements }
                .mapNotNull { it.confidence }
                .let { confidences ->
                    if (confidences.isEmpty()) 0.5f else confidences.average().toFloat()
                }
            TextBlock(
                text = block.text,
                confidence = confidence,
                boundingBox = block.boundingBox,
            )
        }

        val step = _uiState.value.currentStep
        val candidates = when (step) {
            OcrStep.PRICE -> {
                val bounds = Rect(0, 0, imageWidth, imageHeight)
                TextParser.extractPriceCandidates(blocks, bounds)
            }
            OcrStep.QUANTITY -> TextParser.extractQuantityCandidates(blocks)
            OcrStep.COUNT -> TextParser.extractCountCandidates(blocks)
        }.filter { it.meetsThreshold }

        _uiState.update { it.copy(candidates = candidates) }
    }

    fun onEvent(event: OcrEvent) {
        when (event) {
            is OcrEvent.CandidateTapped -> confirmCandidate(event.candidate)
            is OcrEvent.SkipStep -> skipStep()
            is OcrEvent.Reset -> reset()
        }
    }

    private fun confirmCandidate(candidate: OcrCandidate) {
        val state = _uiState.value
        when (state.currentStep) {
            OcrStep.PRICE -> {
                val price = TextParser.extractPriceValue(candidate.text) ?: candidate.text
                _uiState.update {
                    it.copy(
                        confirmedPrice = price,
                        currentStep = OcrStep.QUANTITY,
                        candidates = emptyList(),
                        parseError = "",
                    )
                }
            }
            OcrStep.QUANTITY -> {
                val parsed = TextParser.parseQuantity(candidate.text)
                if (parsed != null) {
                    _uiState.update {
                        it.copy(
                            confirmedQuantity = parsed.value,
                            confirmedUnit = parsed.unit,
                            currentStep = OcrStep.COUNT,
                            candidates = emptyList(),
                            parseError = "",
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            parseError = "内容量の形式を認識できませんでした。手入力してください。",
                            candidates = emptyList(),
                        )
                    }
                }
            }
            OcrStep.COUNT -> {
                _uiState.update {
                    it.copy(
                        confirmedCount = candidate.text,
                        candidates = emptyList(),
                        parseError = "",
                        isComplete = true,
                    )
                }
            }
        }
    }

    private fun skipStep() {
        val step = _uiState.value.currentStep
        if (step == OcrStep.COUNT) {
            _uiState.update { it.copy(candidates = emptyList(), parseError = "", isComplete = true) }
        }
    }

    private fun reset() {
        _uiState.value = OcrUiState()
    }
}
