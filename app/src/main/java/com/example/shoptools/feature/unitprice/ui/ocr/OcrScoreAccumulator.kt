package com.example.shoptools.feature.unitprice.ui.ocr

/**
 * フレームをまたいで OCR 候補のスコアを蓄積・減衰するクラス。
 * Android 非依存 — OcrRect / OcrRectF のみ使用。
 */
class OcrScoreAccumulator(
    val decayFactor: Float = DECAY_FACTOR_DEFAULT,
    val maxScore: Float = MAX_SCORE_DEFAULT,
    val minVisibleScore: Float = MIN_VISIBLE_SCORE_DEFAULT,
    val pruneThreshold: Float = PRUNE_THRESHOLD_DEFAULT,
) {
    data class Entry(
        val text: String,
        val score: Float,
        val viewRect: OcrRectF?,
    )

    private val scores = mutableMapOf<CandidateKey, Entry>()

    val size: Int get() = scores.size

    /**
     * @param frame 今フレームで検出された候補。未検出キーは減衰・剪定対象。
     */
    fun update(frame: Map<CandidateKey, Entry>) {
        val detectedKeys = frame.keys

        // 非検出候補を減衰し、閾値未満を削除
        val toRemove = mutableListOf<CandidateKey>()
        scores.forEach { (key, entry) ->
            if (key !in detectedKeys) {
                val decayed = entry.score * decayFactor
                if (decayed < pruneThreshold) {
                    toRemove.add(key)
                } else {
                    scores[key] = entry.copy(score = decayed)
                }
            }
        }
        toRemove.forEach { scores.remove(it) }

        // 検出候補のスコアを加算し、viewRect を最新フレームで更新
        frame.forEach { (key, entry) ->
            val current = scores[key]?.score ?: 0f
            scores[key] = entry.copy(score = (current + entry.score).coerceAtMost(maxScore))
        }
    }

    /** minVisibleScore 以上の候補をスコア降順で返す。 */
    fun getVisible(): List<Pair<CandidateKey, Entry>> =
        scores.entries
            .filter { it.value.score >= minVisibleScore }
            .map { it.toPair() }
            .sortedByDescending { it.second.score }

    fun normalizedScore(score: Float): Float = (score / maxScore).coerceIn(0f, 1f)

    fun reset() = scores.clear()

    companion object {
        const val DECAY_FACTOR_DEFAULT = 0.85f
        const val MAX_SCORE_DEFAULT = 10f
        const val MIN_VISIBLE_SCORE_DEFAULT = 1.5f
        const val PRUNE_THRESHOLD_DEFAULT = 0.1f
    }
}
