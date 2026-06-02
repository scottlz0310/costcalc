package com.example.shoptools.feature.unitprice.ui.ocr

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OcrScoreAccumulatorTest {
    private lateinit var accumulator: OcrScoreAccumulator

    private val key1 = CandidateKey(OcrStep.PRICE, "198", regionBucket = 10)
    private val key2 = CandidateKey(OcrStep.PRICE, "298", regionBucket = 20)

    private fun entry(
        text: String,
        score: Float = 0.8f,
    ) = OcrScoreAccumulator.Entry(text, score, viewRect = null)

    @Before
    fun setUp() {
        accumulator = OcrScoreAccumulator()
    }

    @Test
    fun `score increases with repeated detection`() {
        accumulator.update(mapOf(key1 to entry("198")))
        accumulator.update(mapOf(key1 to entry("198")))
        accumulator.update(mapOf(key1 to entry("198")))

        val visible = accumulator.getVisible()
        assertEquals(1, visible.size)
        assertTrue(visible[0].second.score > 0.8f * 3 * 0.9f) // roughly 3 * baseScore
    }

    @Test
    fun `score is capped at maxScore`() {
        repeat(50) { accumulator.update(mapOf(key1 to entry("198"))) }

        val visible = accumulator.getVisible()
        assertTrue(visible[0].second.score <= accumulator.maxScore)
    }

    @Test
    fun `score decays when candidate not detected`() {
        accumulator.update(mapOf(key1 to entry("198", score = 8f)))
        accumulator.update(emptyMap()) // 非検出
        accumulator.update(emptyMap())

        val visible = accumulator.getVisible()
        // 8 * 0.85 * 0.85 ≈ 5.78 > minVisibleScore
        assertTrue(visible.isNotEmpty())
        assertTrue(visible[0].second.score < 8f)
    }

    @Test
    fun `candidate removed when score falls below prune threshold`() {
        accumulator.update(mapOf(key1 to entry("198", score = 0.05f)))
        // score = 0.05 < pruneThreshold(0.1) なので次フレームで削除対象
        // まず1フレーム検出されることで 0.05 が積まれ、次に非検出で 0.05*0.85=0.0425 < 0.1 → 削除
        accumulator.update(emptyMap())

        assertEquals(0, accumulator.size)
    }

    @Test
    fun `only candidates above minVisibleScore are visible`() {
        // key1: score が minVisibleScore 未満になるよう小さくセット
        accumulator.update(mapOf(key1 to entry("198", score = 0.5f)))
        // 0.5 < minVisibleScore(1.5) → 表示されない
        assertEquals(0, accumulator.getVisible().size)

        accumulator.update(mapOf(key1 to entry("198", score = 0.5f)))
        accumulator.update(mapOf(key1 to entry("198", score = 0.5f)))
        accumulator.update(mapOf(key1 to entry("198", score = 0.5f)))
        // 0.5*4 = 2.0 > 1.5 → 表示される
        assertEquals(1, accumulator.getVisible().size)
    }

    @Test
    fun `different region bucket creates separate candidate`() {
        val keyA = CandidateKey(OcrStep.PRICE, "198", regionBucket = 0)
        val keyB = CandidateKey(OcrStep.PRICE, "198", regionBucket = 5)

        repeat(4) {
            accumulator.update(
                mapOf(
                    keyA to entry("198"),
                    keyB to entry("198"),
                ),
            )
        }

        assertEquals(2, accumulator.getVisible().size)
    }

    @Test
    fun `reset clears all scores`() {
        repeat(5) { accumulator.update(mapOf(key1 to entry("198"))) }
        assertTrue(accumulator.getVisible().isNotEmpty())

        accumulator.reset()

        assertEquals(0, accumulator.size)
        assertEquals(0, accumulator.getVisible().size)
    }

    @Test
    fun `visible results are sorted by score descending`() {
        repeat(5) { accumulator.update(mapOf(key1 to entry("198", score = 1.0f))) }
        repeat(3) { accumulator.update(mapOf(key2 to entry("298", score = 1.0f))) }

        val visible = accumulator.getVisible()
        assertEquals(2, visible.size)
        assertTrue(visible[0].second.score >= visible[1].second.score)
        assertEquals("198", visible[0].second.text)
    }

    @Test
    fun `normalizedScore returns value between 0 and 1`() {
        val score = accumulator.normalizedScore(7f)
        assertTrue(score in 0f..1f)
        assertEquals(0.7f, score, 0.001f)
    }
}
