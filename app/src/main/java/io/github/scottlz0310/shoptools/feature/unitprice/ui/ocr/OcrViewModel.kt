package io.github.scottlz0310.shoptools.feature.unitprice.ui.ocr

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OcrViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(OcrUiState())
        val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

        private val accumulator = OcrScoreAccumulator()

        /**
         * CameraOcrScreen の analyzer callback（メインスレッド）から呼ばれる。
         * CameraX / Android 依存の変換は CameraOcrScreen 側で完了済みであること。
         */
        fun onCandidatesDetected(
            candidates: List<OcrCandidate>,
            imageWidth: Int,
            imageHeight: Int,
        ) {
            val step = _uiState.value.currentStep
            val frame = buildFrame(candidates, step, imageWidth, imageHeight)
            accumulator.update(frame)

            val visible =
                accumulator.getVisible().map { (key, entry) ->
                    OcrCandidate(
                        text = entry.text,
                        confidence = accumulator.normalizedScore(entry.score),
                        boundingBox = null,
                        boundingBoxView = entry.viewRect,
                    )
                }
            _uiState.update { it.copy(candidates = visible) }
        }

        fun onEvent(event: OcrEvent) {
            when (event) {
                is OcrEvent.CandidateTapped -> confirmCandidate(event.candidate)
                is OcrEvent.SkipStep -> skipStep()
                is OcrEvent.Reset -> reset()
            }
        }

        private fun buildFrame(
            candidates: List<OcrCandidate>,
            step: OcrStep,
            imageWidth: Int,
            imageHeight: Int,
        ): Map<CandidateKey, OcrScoreAccumulator.Entry> =
            candidates
                .filter { it.meetsThreshold }
                .associate { candidate ->
                    val normalizedText =
                        candidate.text
                            .trim()
                            .lowercase()
                            .replace(",", "")
                    // PRICE のみ位置区別が必要（同一数値が複数箇所に存在しうる）。
                    // QUANTITY / COUNT は 1 ラベルに 1 つのみ存在するため bucket=0 固定とし、
                    // カメラのわずかな揺れで CandidateKey が変わりスコアが分散するのを防ぐ。
                    val bucket =
                        if (step == OcrStep.PRICE) {
                            candidate.boundingBox
                                ?.toOcrRect()
                                ?.regionBucket(imageWidth, imageHeight)
                                ?: 0
                        } else {
                            0
                        }
                    val key = CandidateKey(step, normalizedText, bucket)
                    val entry =
                        OcrScoreAccumulator.Entry(
                            text = candidate.text,
                            score = candidate.confidence,
                            viewRect = candidate.boundingBoxView,
                        )
                    key to entry
                }

        private fun confirmCandidate(candidate: OcrCandidate) {
            when (_uiState.value.currentStep) {
                OcrStep.PRICE -> {
                    val price = TextParser.extractPriceValue(candidate.text) ?: candidate.text
                    accumulator.reset()
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
                        accumulator.reset()
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
                    accumulator.reset()
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
            if (_uiState.value.currentStep == OcrStep.COUNT) {
                accumulator.reset()
                _uiState.update { it.copy(candidates = emptyList(), parseError = "", isComplete = true) }
            }
        }

        private fun reset() {
            accumulator.reset()
            _uiState.value = OcrUiState()
        }
    }

private fun Rect.toOcrRect() = OcrRect(left, top, right, bottom)
