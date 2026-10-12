package io.github.halilozel1903.sheets.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetentResolverTest {
    private val peek = SheetDetent.peek(120f)

    @Test
    fun resolvesFractionsFixedHeightsAndFitContent() {
        val anchors = DetentResolver.resolve(
            detents = listOf(SheetDetent.Full, peek, SheetDetent.Half, SheetDetent.FitContent),
            availableHeight = 2000f,
            contentHeight = 640f,
            density = 2.5f,
        )
        assertEquals(listOf(300f, 640f, 1000f, 2000f), anchors.all.map { it.height })
        assertEquals(listOf("peek", "fit-content", "half", "full"), anchors.all.map { it.detent.key })
        assertEquals(300f, anchors.minHeight)
        assertEquals(2000f, anchors.maxHeight)
    }

    @Test
    fun clampsToTheAvailableHeight() {
        val anchors = DetentResolver.resolve(
            detents = listOf(SheetDetent.Fixed(900f, "tall"), SheetDetent.FitContent),
            availableHeight = 1000f,
            contentHeight = 4000f,
            density = 2f,
        )
        assertEquals(listOf(1000f, 1000f), anchors.all.map { it.height })
        // Both resolve to the same height: one snap point, the one declared first.
        assertEquals(1, anchors.snapPoints.size)
        assertEquals("tall", anchors.snapPoints.single().detent.key)
        assertEquals(1000f, anchors.heightOf(SheetDetent.FitContent))
    }

    @Test
    fun negativeAvailableHeightResolvesToZero() {
        val anchors = DetentResolver.resolve(listOf(SheetDetent.Half), availableHeight = -10f, contentHeight = 0f, density = 1f)
        assertEquals(0f, anchors.heightOf(SheetDetent.Half))
    }

    @Test
    fun hiddenIsAlwaysZeroEvenWhenNotListed() {
        val anchors = DetentResolver.resolve(listOf(SheetDetent.Half), 1000f, 0f, 1f)
        assertEquals(0f, anchors.heightOf(SheetDetent.Hidden))
        assertNull(anchors.heightOf(SheetDetent.Full))
        assertTrue(anchors.contains(SheetDetent.Half))
    }

    @Test
    fun stepsBetweenDetents() {
        val anchors = DetentResolver.resolve(listOf(SheetDetent.Hidden, peek, SheetDetent.Half, SheetDetent.Full), 2000f, 0f, 2f)
        assertEquals(SheetDetent.Half, anchors.stepUp(peek)?.detent)
        assertEquals(peek, anchors.stepDown(SheetDetent.Half)?.detent)
        assertEquals(SheetDetent.Hidden, anchors.stepDown(peek)?.detent)
        assertNull(anchors.stepUp(SheetDetent.Full))
        assertNull(anchors.stepDown(SheetDetent.Hidden))
        assertEquals(peek, anchors.closest(300f)?.detent)
    }

    @Test
    fun duplicateKeysAreResolvedOnce() {
        val anchors = DetentResolver.resolve(listOf(SheetDetent.Half, SheetDetent.Fraction(0.5f, "half")), 1000f, 0f, 1f)
        assertEquals(1, anchors.all.size)
    }

    @Test
    fun maxFixedHeightIgnoresFitContent() {
        assertEquals(500f, DetentResolver.maxFixedHeight(listOf(SheetDetent.Half, SheetDetent.FitContent), 1000f, 2f))
        assertEquals(0f, DetentResolver.maxFixedHeight(listOf(SheetDetent.FitContent), 1000f, 2f))
    }

    @Test
    fun rejectsInvalidDetents() {
        assertFailsWith<IllegalArgumentException> { SheetDetent.Fraction(1.5f) }
        assertFailsWith<IllegalArgumentException> { SheetDetent.Fixed(-1f) }
    }

    @Test
    fun anchorsCompareByContent() {
        val a = DetentResolver.resolve(listOf(SheetDetent.Half), 1000f, 0f, 1f)
        val b = DetentResolver.resolve(listOf(SheetDetent.Half), 1000f, 0f, 1f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
