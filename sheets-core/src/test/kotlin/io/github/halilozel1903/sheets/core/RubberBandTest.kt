package io.github.halilozel1903.sheets.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RubberBandTest {
    @Test
    fun resistanceGrowsSlowerThanThePullAndNeverReachesTheDimension() {
        val small = RubberBand.resist(10f, dimension = 200f)
        val large = RubberBand.resist(10_000f, dimension = 200f)
        assertTrue(small in 0f..10f)
        assertTrue(large < 200f)
        assertTrue(large > 190f)
        assertEquals(0f, RubberBand.resist(-5f, 200f))
        assertEquals(0f, RubberBand.resist(5f, 0f))
    }

    @Test
    fun clampPassesValuesInsideTheRange() {
        assertEquals(500f, RubberBand.clamp(500f, min = 100f, max = 1000f, dimension = 200f))
    }

    @Test
    fun clampResistsOnBothSides() {
        val above = RubberBand.clamp(1100f, min = 100f, max = 1000f, dimension = 200f)
        val below = RubberBand.clamp(0f, min = 100f, max = 1000f, dimension = 200f)
        assertTrue(above in 1000f..1100f)
        assertTrue(below in 0f..100f)
        assertEquals(1000f - above, below - 100f, absoluteTolerance = 0.001f)
    }

    @Test
    fun rawForInvertsClamp() {
        for (raw in listOf(-400f, 40f, 100f, 640f, 1000f, 1080f, 1900f)) {
            val shown = RubberBand.clamp(raw, 100f, 1000f, 200f)
            assertEquals(raw, RubberBand.rawFor(shown, 100f, 1000f, 200f), absoluteTolerance = 0.05f)
        }
    }
}
