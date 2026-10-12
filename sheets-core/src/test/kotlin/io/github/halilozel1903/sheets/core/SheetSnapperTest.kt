package io.github.halilozel1903.sheets.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SheetSnapperTest {
    private val peek = SheetDetent.peek(100f)
    private val anchors = DetentResolver.resolve(listOf(peek, SheetDetent.Half, SheetDetent.Full), 2000f, 0f, 2f)
    // peek = 200, half = 1000, full = 2000
    private val config = SnapConfig(velocityThreshold = 300f, positionalThreshold = PositionalThreshold.fraction(0.5f))

    private fun target(height: Float, velocity: Float = 0f, start: Float? = null) =
        SheetSnapper.target(anchors, height, velocity, config, start)?.detent

    @Test
    fun slowReleaseNeedsToPassTheThresholdFromTheStartingDetent() {
        assertEquals(peek, target(590f, start = 200f))
        assertEquals(SheetDetent.Half, target(610f, start = 200f))
        // Coming down from half, the same position settles the other way.
        assertEquals(SheetDetent.Half, target(610f, start = 1000f))
        assertEquals(peek, target(590f, start = 1000f))
    }

    @Test
    fun withoutAStartTheClosestDetentWins() {
        assertEquals(SheetDetent.Half, target(800f))
        assertEquals(peek, target(500f))
    }

    @Test
    fun flingMovesOneDetentInItsDirection() {
        assertEquals(SheetDetent.Half, target(260f, velocity = 1200f, start = 200f))
        assertEquals(peek, target(940f, velocity = -1200f, start = 1000f))
        // A fling up from below half goes to half, not full.
        assertEquals(SheetDetent.Half, target(900f, velocity = 5000f, start = 200f))
    }

    @Test
    fun flingFromRestMovesToTheNextDetent() {
        assertEquals(SheetDetent.Full, target(1000f, velocity = 800f))
        assertEquals(peek, target(1000f, velocity = -800f))
        assertEquals(SheetDetent.Half, target(1000f, velocity = 100f))
    }

    @Test
    fun slowFlingFallsBackToThePositionalThreshold() {
        assertEquals(peek, target(400f, velocity = 200f, start = 200f))
    }

    @Test
    fun outsideTheDetentsSettlesAtTheEdge() {
        assertEquals(SheetDetent.Full, target(2100f))
        assertEquals(peek, target(150f))
        assertEquals(SheetDetent.Half, target(2000f, velocity = -900f))
        assertEquals(SheetDetent.Half, target(150f, velocity = 900f))
    }

    @Test
    fun fixedPositionalThreshold() {
        val fixed = SnapConfig(velocityThreshold = 300f, positionalThreshold = PositionalThreshold.fixed(100f))
        assertEquals(SheetDetent.Half, SheetSnapper.target(anchors, 310f, 0f, fixed, 200f)?.detent)
        assertEquals(peek, SheetSnapper.target(anchors, 290f, 0f, fixed, 200f)?.detent)
    }

    @Test
    fun singleOrNoDetent() {
        val one = DetentResolver.resolve(listOf(SheetDetent.FitContent), 1000f, 400f, 1f)
        assertEquals(SheetDetent.FitContent, SheetSnapper.target(one, 10f, -9000f, config)?.detent)
        assertNull(SheetSnapper.target(SheetAnchors(emptyList()), 10f, 0f, config))
    }

    @Test
    fun hiddenDetentCanBeFlungTo() {
        val modal = DetentResolver.resolve(listOf(SheetDetent.Hidden, SheetDetent.FitContent), 1000f, 400f, 1f)
        assertEquals(SheetDetent.Hidden, SheetSnapper.target(modal, 380f, -900f, config, 400f)?.detent)
        assertEquals(SheetDetent.FitContent, SheetSnapper.target(modal, 380f, 0f, config, 400f)?.detent)
    }
}
