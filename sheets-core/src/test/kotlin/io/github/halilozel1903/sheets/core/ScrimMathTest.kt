package io.github.halilozel1903.sheets.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScrimMathTest {
    private val peek = SheetDetent.peek(100f)

    @Test
    fun alphaIsLinearInsideTheRange() {
        val range = ScrimRange(1000f, 2000f)
        assertEquals(0f, ScrimMath.alpha(900f, range, 0.4f))
        assertEquals(0.2f, ScrimMath.alpha(1500f, range, 0.4f), absoluteTolerance = 0.0001f)
        assertEquals(0.4f, ScrimMath.alpha(2100f, range, 0.4f))
        assertEquals(0f, ScrimMath.alpha(Float.NaN, range, 0.4f))
        assertEquals(0f, ScrimMath.alpha(1500f, null, 0.4f))
    }

    @Test
    fun persistentSheetsDimOnlyTowardsTheTopDetent() {
        val anchors = DetentResolver.resolve(listOf(peek, SheetDetent.Half, SheetDetent.Full), 2000f, 0f, 2f)
        assertEquals(ScrimRange(1000f, 2000f), ScrimMath.defaultRange(anchors, modal = false))
        val single = DetentResolver.resolve(listOf(SheetDetent.Half), 2000f, 0f, 2f)
        assertNull(ScrimMath.defaultRange(single, modal = false))
    }

    @Test
    fun modalSheetsAreFullyDimmedWhenOpen() {
        val anchors = DetentResolver.resolve(listOf(SheetDetent.Hidden, SheetDetent.FitContent), 2000f, 600f, 2f)
        val range = ScrimMath.defaultRange(anchors, modal = true)
        assertEquals(ScrimRange(0f, 600f), range)
        assertEquals(0.32f, ScrimMath.alpha(600f, range, 0.32f))
        assertEquals(0.16f, ScrimMath.alpha(300f, range, 0.32f), absoluteTolerance = 0.0001f)
        assertEquals(0f, ScrimMath.alpha(0f, range, 0.32f))
    }

    @Test
    fun degenerateRangeIsAStep() {
        assertEquals(0.3f, ScrimMath.alpha(500f, ScrimRange(500f, 500f), 0.3f))
        assertEquals(0f, ScrimMath.alpha(499f, ScrimRange(500f, 500f), 0.3f))
    }
}
