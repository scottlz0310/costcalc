package com.example.shoptools.feature.unitprice.ui.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextParserTest {
    // --- parseQuantity ---

    @Test
    fun `parseQuantity - grams`() {
        val result = TextParser.parseQuantity("500g")
        assertEquals("500", result?.value)
        assertEquals("g", result?.unit)
    }

    @Test
    fun `parseQuantity - decimal kg`() {
        val result = TextParser.parseQuantity("1.5kg")
        assertEquals("1.5", result?.value)
        assertEquals("kg", result?.unit)
    }

    @Test
    fun `parseQuantity - mL with space`() {
        val result = TextParser.parseQuantity("350 mL")
        assertEquals("350", result?.value)
        assertEquals("mL", result?.unit)
    }

    @Test
    fun `parseQuantity - lowercase ml`() {
        val result = TextParser.parseQuantity("200ml")
        assertEquals("200", result?.value)
        assertEquals("ml", result?.unit)
    }

    @Test
    fun `parseQuantity - Japanese unit 個`() {
        val result = TextParser.parseQuantity("6個")
        assertEquals("6", result?.value)
        assertEquals("個", result?.unit)
    }

    @Test
    fun `parseQuantity - inside longer text`() {
        val result = TextParser.parseQuantity("内容量 400g 税込")
        assertEquals("400", result?.value)
        assertEquals("g", result?.unit)
    }

    @Test
    fun `parseQuantity - no match returns null`() {
        assertNull(TextParser.parseQuantity("¥198"))
    }

    @Test
    fun `parseQuantity - plain number returns null`() {
        assertNull(TextParser.parseQuantity("100"))
    }

    // --- extractPriceValue ---

    @Test
    fun `extractPriceValue - plain number`() {
        assertEquals("198", TextParser.extractPriceValue("198"))
    }

    @Test
    fun `extractPriceValue - comma-separated`() {
        assertEquals("1980", TextParser.extractPriceValue("1,980"))
    }

    @Test
    fun `extractPriceValue - non-numeric returns null`() {
        assertNull(TextParser.extractPriceValue("abc"))
    }

    // --- extractPriceCandidates ---

    @Test
    fun `extractPriceCandidates - prefers blocks with yen context when confidence is similar`() {
        // "¥198": 0.6 + 3*0.05 = 0.75  vs  "500": 0.65 → ¥コンテキストで逆転することを確認
        val blocks =
            listOf(
                TextBlock("¥198", 0.6f, null),
                TextBlock("500", 0.65f, null),
            )
        val candidates = TextParser.extractPriceCandidates(blocks, null)
        assertEquals("198", candidates.first().text)
    }

    @Test
    fun `extractPriceCandidates - excludes values with unit suffix`() {
        val blocks =
            listOf(
                TextBlock("500g", 0.9f, null),
                TextBlock("¥298", 0.7f, null),
            )
        val candidates = TextParser.extractPriceCandidates(blocks, null)
        assertEquals(1, candidates.size)
        assertEquals("298", candidates.first().text)
    }

    @Test
    fun `extractPriceCandidates - keeps price when quantity appears in same OCR block on another line`() {
        val blocks = listOf(TextBlock("税込198円\n500g", 0.8f, null))
        val candidates = TextParser.extractPriceCandidates(blocks, null)
        assertEquals("198", candidates.first().text)
    }

    @Test
    fun `extractPriceCandidates - excludes only number directly followed by quantity unit`() {
        val blocks = listOf(TextBlock("税込198円 500g", 0.8f, null))
        val candidates = TextParser.extractPriceCandidates(blocks, null)
        assertEquals(listOf("198"), candidates.map { it.text })
    }

    @Test
    fun `extractPriceCandidates - excludes out of range values`() {
        val blocks =
            listOf(
                TextBlock("0", 0.9f, null),
                TextBlock("9999999", 0.9f, null),
                TextBlock("100", 0.7f, null),
            )
        val candidates = TextParser.extractPriceCandidates(blocks, null)
        assertEquals(1, candidates.size)
        assertEquals("100", candidates.first().text)
    }

    // --- extractQuantityCandidates ---

    @Test
    fun `extractQuantityCandidates - returns match`() {
        val blocks = listOf(TextBlock("内容量 400g", 0.8f, null))
        val candidates = TextParser.extractQuantityCandidates(blocks)
        assertEquals(1, candidates.size)
        assertEquals("400g", candidates.first().text)
    }

    @Test
    fun `extractQuantityCandidates - no match returns empty`() {
        val blocks = listOf(TextBlock("¥198", 0.9f, null))
        val candidates = TextParser.extractQuantityCandidates(blocks)
        assertEquals(0, candidates.size)
    }

    // --- extractCountCandidates ---

    @Test
    fun `extractCountCandidates - valid count`() {
        val blocks = listOf(TextBlock("6", 0.85f, null))
        val candidates = TextParser.extractCountCandidates(blocks)
        assertEquals(1, candidates.size)
        assertEquals("6", candidates.first().text)
    }

    @Test
    fun `extractCountCandidates - out of range excluded`() {
        val blocks = listOf(TextBlock("200", 0.9f, null))
        val candidates = TextParser.extractCountCandidates(blocks)
        assertEquals(0, candidates.size)
    }
}
