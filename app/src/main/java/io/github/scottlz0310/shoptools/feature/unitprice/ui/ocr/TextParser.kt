package io.github.scottlz0310.shoptools.feature.unitprice.ui.ocr

import android.graphics.Rect

object TextParser {
    private val PRICE_EXTRACT_REGEX = Regex("""[\d,]+(?:\.\d+)?""")
    private val QUANTITY_REGEX =
        Regex(
            """(\d+\.?\d*)\s*(g|kg|㎏|mL|ml|㎖|㍑|ℓ|cc|L|l|個|本|枚|袋|缶|箱|pack)""",
            RegexOption.IGNORE_CASE,
        )

    // OCR ノイズ対応: ㎖ の ℓ 部分が 2/!/& 等に誤読される場合のフォールバック。
    // 3桁以上の数値 + m/n（n は m の誤読）+ ノイズ文字（任意）にマッチ。
    // (?![a-zA-Z]) で後続が英字の場合（例: 500mg, 900mAh）を除外し、
    // mL 以外の単位ブロックを誤って mL 候補に正規化しないよう防ぐ。
    private val QUANTITY_REGEX_NOISY_ML = Regex("""(\d{3,}\.?\d*)\s*[mMnN][0-9ℓlL!&?1]?(?![a-zA-Z])""")

    // OCR 誤読補正: 数字列の末尾 O/o を数値文脈（後続が単位文字）のときに 0 へ置換
    // 例: "90Om" → "900m"
    private val OCR_O_FOR_ZERO = Regex("""(\d+)[Oo](?=[mMnNgGkKlL])""")

    private val PRICE_CONTEXT_WORDS = setOf("¥", "円", "税込", "本体", "税抜", "price", "Price")

    // 数値直後にこれらが続く場合は価格ではなく内容量・単位とみなす
    // "m" は mL/ml/m2 等の OCR ノイズを含む一括除外。QUANTITY_REGEX の単位と同期させること
    private val UNIT_SUFFIXES =
        listOf("kg", "㎏", "mL", "ml", "㎖", "㍑", "ℓ", "cc", "m", "個", "本", "枚", "袋", "缶", "箱", "g", "L", "l", "%", "℃")

    fun extractPriceCandidates(
        blocks: List<TextBlock>,
        imageBounds: Rect?,
    ): List<OcrCandidate> {
        return blocks
            .flatMap { block ->
                PRICE_EXTRACT_REGEX
                    .findAll(block.text)
                    .mapNotNull { match ->
                        val raw = match.value.replace(",", "")
                        val value = raw.toDoubleOrNull() ?: return@mapNotNull null
                        if (value < 1.0 || value > 999_999.0) return@mapNotNull null
                        // 直前が ASCII 英字またはドットなら単位に続く数字（"m2" の "2"、".04" の "04" 等）→ 除外
                        if (match.range.first > 0) {
                            val charBefore = block.text[match.range.first - 1]
                            if (charBefore in 'A'..'Z' || charBefore in 'a'..'z' || charBefore == '.') {
                                return@mapNotNull null
                            }
                        }
                        // block 全体ではなくマッチした数値直後のサフィックスで判定（複合ブロック対策）
                        val textAfterMatch = block.text.substring(match.range.last + 1).trimStart()
                        if (UNIT_SUFFIXES.any {
                                textAfterMatch.startsWith(
                                    it,
                                    ignoreCase = true,
                                )
                            }
                        ) {
                            return@mapNotNull null
                        }
                        val score = scorePriceCandidate(block.text, block.boundingBox, imageBounds)
                        OcrCandidate(
                            text = raw,
                            confidence = block.confidence + score * 0.05f,
                            boundingBox = block.boundingBox,
                        )
                    }.toList()
            }.sortedByDescending { it.confidence }
    }

    fun extractQuantityCandidates(blocks: List<TextBlock>): List<OcrCandidate> {
        return blocks
            .mapNotNull { block ->
                // OCR 誤読補正（O→0）を適用してから厳密マッチ
                val preprocessed = OCR_O_FOR_ZERO.replace(block.text) { mr -> mr.groupValues[1] + "0" }
                val strictMatch = QUANTITY_REGEX.find(preprocessed)
                if (strictMatch != null) {
                    return@mapNotNull OcrCandidate(
                        text = strictMatch.value.trim(),
                        confidence = block.confidence,
                        boundingBox = block.boundingBox,
                    )
                }
                // フォールバック: ㎖ が m2/m!/m& 等に誤読された場合は "900mL" に正規化
                val noisyMatch = QUANTITY_REGEX_NOISY_ML.find(preprocessed) ?: return@mapNotNull null
                OcrCandidate(
                    text = "${noisyMatch.groupValues[1]}mL",
                    confidence = block.confidence,
                    boundingBox = block.boundingBox,
                )
            }.sortedByDescending { it.confidence }
    }

    fun extractCountCandidates(blocks: List<TextBlock>): List<OcrCandidate> {
        return blocks
            .mapNotNull { block ->
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

    private fun scorePriceCandidate(
        text: String,
        box: Rect?,
        imageBounds: Rect?,
    ): Int {
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
