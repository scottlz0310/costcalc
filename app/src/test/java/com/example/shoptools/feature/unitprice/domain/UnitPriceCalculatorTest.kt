package com.example.shoptools.feature.unitprice.domain

import com.example.shoptools.core.formatUnitPrice
import org.junit.Assert.*
import org.junit.Test

class UnitPriceCalculatorTest {

    @Test
    fun testBasicCalculation() {
        // 198 / (0.9 * 1) = 220.0
        val result = UnitPriceCalculator.calculate(198.0, 0.9, 1)
        assertEquals(220.0, result, 0.005)
    }

    @Test
    fun testIntegerQuantity() {
        // 178 / (1.0 * 1) = 178.0
        val result = UnitPriceCalculator.calculate(178.0, 1.0, 1)
        assertEquals(178.0, result, 0.005)
    }

    @Test
    fun testWithCount() {
        // 300 / (1.5 * 2) = 100.0
        val result = UnitPriceCalculator.calculate(300.0, 1.5, 2)
        assertEquals(100.0, result, 0.005)
    }

    @Test
    fun testFormatOutput() {
        val result = UnitPriceCalculator.calculate(198.0, 0.9, 1)
        val formatted = formatUnitPrice(result)
        assertEquals("220.00", formatted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidationZeroPrice() {
        UnitPriceCalculator.calculate(0.0, 1.0, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidationZeroQuantity() {
        UnitPriceCalculator.calculate(100.0, 0.0, 1)
    }
}
