package com.example.shoptools.feature.stamps.domain

import org.junit.Assert.*
import org.junit.Test

class BoundedSubsetSumTest {

    private val defaultInventory = listOf(
        StampItem(84, 5),
        StampItem(63, 3),
        StampItem(10, 10),
        StampItem(1, 50),
    )

    @Test
    fun testExactMatch() {
        // 84*1 + 63*2 = 84 + 126 = 210... try target=240: 84*1 + 63*1 + 10*9 + 1*3 = 84+63+90+3=240
        val (exact, _, _) = BoundedSubsetSum.solve(defaultInventory, 240)
        assertNotNull("Exact match should exist for target=240", exact)
        assertEquals(240, exact!!.total)
        assertEquals(0, exact.diff)
    }

    @Test
    fun testUnderAndOver() {
        val (_, under, over) = BoundedSubsetSum.solve(defaultInventory, 240)
        // Under results should all be < 240 and sorted by diff ascending
        for (combo in under) {
            assertTrue("Under combo total ${combo.total} should be < 240", combo.total < 240)
        }
        for (i in 0 until under.size - 1) {
            assertTrue("Under list should be sorted by diff ascending",
                under[i].diff <= under[i + 1].diff)
        }
        // Over results should all be > 240 and sorted by diff ascending
        for (combo in over) {
            assertTrue("Over combo total ${combo.total} should be > 240", combo.total > 240)
        }
        for (i in 0 until over.size - 1) {
            assertTrue("Over list should be sorted by diff ascending",
                over[i].diff <= over[i + 1].diff)
        }
    }

    @Test
    fun testEmptyInventory() {
        val (exact, under, over) = BoundedSubsetSum.solve(emptyList(), 240)
        assertNull(exact)
        assertTrue(under.isEmpty())
        assertTrue(over.isEmpty())
    }

    @Test
    fun testZeroStock() {
        val inventory = listOf(
            StampItem(84, 0),
            StampItem(63, 0),
            StampItem(1, 50),
        )
        val (exact, _, _) = BoundedSubsetSum.solve(inventory, 84)
        // Zero stock items should be ignored, so 84 cannot be reached exactly from 1x50=50 max
        assertNull("84 is not reachable from 50x1 stamps", exact)
    }

    @Test
    fun testCompositionString() {
        val composition = mapOf(84 to 1, 63 to 2)
        val result = BoundedSubsetSum.compositionToString(composition)
        assertEquals("84x1, 63x2", result)
    }

    @Test
    fun testZeroTarget() {
        val (exact, under, over) = BoundedSubsetSum.solve(defaultInventory, 0)
        assertNull(exact)
        assertTrue(under.isEmpty())
        assertTrue(over.isEmpty())
    }

    @Test
    fun testMaxThreeResults() {
        val (_, under, over) = BoundedSubsetSum.solve(defaultInventory, 240)
        assertTrue("Under list should have at most 3 results", under.size <= 3)
        assertTrue("Over list should have at most 3 results", over.size <= 3)
    }
}
