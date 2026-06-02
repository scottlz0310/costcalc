package com.example.shoptools.feature.unitprice.ui.ocr

import android.graphics.Rect

object TextParser {

    private val PRICE_EXTRACT_REGEX = Regex("""[\d,]+(?:\.\d+)?""")
    private val QUANTITY_REGEX = Regex("""(\d+\.?\d*)\s*(g|kg|mL|ml|L|l|個|本|枚|袋|缶|箱|pack|Pack)""")

    private val PRICE_CONTEXT_WORDS = setOf("¥", "円", "税込", "本体", "税抜", "price", "Price")
    private val NEGATIVE_PRICE_WORDS = setOf("g", "kg", "mL", "ml", "L", "個", "本", "枚", "%", "℃")

    fun extractPriceCandidates(
        blocks: List<TextBlock>,
        imageBounds: Rect?,
    ): List<OcrCandidate> {
        return blocks
            .flatMap { block ->
                PRICE_EXTRACT_REGEX.findAll(block.text)
                    .mapNotNull { match ->
                        val raw = match.value.replace(",", "")
                        val value = raw.toDoubleOrNull() ?: return@mapNotNull null
                        if (value < 1.0 || value > 999_999.0) return@mapNotNull null
                        if (NEGATIVE_PRICE_WORDS.any { block.text.contains(it) }) return@mapNotNull null
                        val score = scorePriceCandidate(block.text, block.boundingBox, imageBounds)
                        OcrCandidate(
                            text = raw,
                            confidence = block.confidence + score * 0.05f,
                            boundingBox = block.boundingBox,
                        )
                    }.toList()
            }
            .sortedByDescending { it.confidence }
    }

    fun extractQuantityCandidates(blocks: List<TextBlock>): List<OcrCandidate> {
        return blocks.mapNotNull { block ->
            val match = QUANTITY_REGEX.find(block.text) ?: return@mapNotNull null
            OcrCandidate(
                text = match.value.trim(),
                confidence = block.confidence,
                boundingBox = block.boundingBox,
            )
        }.sortedByDescending { it.confidence }
    }

    fun extractCountCandidates(blocks: List<TextBlock>): List<OcrCandidate> {
        return blocks.mapNotNull { block ->
            val text = block.text.trim()
            val num = text.toIntOrNull() ?: return@mapNotNull null
            if (num < 1 || num > 100) return@mapNotNull null
            OcrCandidate(
                text = text,
                confidence = block.confidence,
                boundingBox = block.boundingBox,
            )
        }.sortedByDescending { it.confidence }
    }

    fun parseQuantity(text: String): ParsedQuantity? {
        val match = QUANTITY_REGEX.find(text.trim()) ?: return null
        return ParsedQuantity(
            value = match.groupValues[1],
            unit = match.groupValues[2],
        )
    }

    fun extractPriceValue(text: String): String? {
        val raw = text.replace(",", "")
        return if (raw.toDoubleOrNull() != null) raw else null
    }

    private fun scorePriceCandidate(text: String, box: Rect?, imageBounds: Rect?): Int {
        var score = 0
        if (PRICE_CONTEXT_WORDS.any { text.contains(it) }) score += 3
        if (imageBounds != null && box != null) {
            val centerX = imageBounds.width() / 2
            val centerY = imageBounds.height() / 2
            val boxCenterX = (box.left + box.right) / 2
            val boxCenterY = (box.top + box.bottom) / 2
            val dx = Math.abs(boxCenterX - centerX).toFloat() / imageBounds.width()
            val dy = Math.abs(boxCenterY - centerY).toFloat() / imageBounds.height()
            if (dx < 0.3f && dy < 0.3f) score += 1
        }
        return score
    }
}

data class TextBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect?,
)
