package io.github.halilozel1903.sheets.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutAndScrollTest {
    @Test
    fun keyboardShrinksTheAvailableHeight() {
        assertEquals(2200f, SheetLayoutMath.availableHeight(2400f, topInset = 200f, imeHeight = 0f))
        assertEquals(1400f, SheetLayoutMath.availableHeight(2400f, topInset = 200f, imeHeight = 800f))
        assertEquals(0f, SheetLayoutMath.availableHeight(500f, topInset = 200f, imeHeight = 800f))
    }

    @Test
    fun sheetRestsOnTheKeyboard() {
        assertEquals(1000f, SheetLayoutMath.sheetTop(2400f, imeHeight = 800f, height = 600f))
        assertEquals(1800f, SheetLayoutMath.sheetTop(2400f, imeHeight = 0f, height = 600f))
    }

    @Test
    fun visibleFraction() {
        assertEquals(0.5f, SheetLayoutMath.visibleFraction(1000f, 2000f))
        assertEquals(0f, SheetLayoutMath.visibleFraction(Float.NaN, 2000f))
        assertEquals(0f, SheetLayoutMath.visibleFraction(100f, 0f))
    }

    @Test
    fun sheetGrowsBeforeTheListScrolls() {
        assertEquals(40f, SheetNestedScroll.preScroll(40f, height = 1000f, maxHeight = 2000f))
        assertEquals(10f, SheetNestedScroll.preScroll(40f, height = 1990f, maxHeight = 2000f))
        assertEquals(0f, SheetNestedScroll.preScroll(40f, height = 2000f, maxHeight = 2000f))
        assertEquals(0f, SheetNestedScroll.preScroll(-40f, height = 1000f, maxHeight = 2000f))
    }

    @Test
    fun sheetShrinksAfterTheListReachesItsTop() {
        assertEquals(-40f, SheetNestedScroll.postScroll(-40f, height = 1000f, minHeight = 200f))
        assertEquals(-20f, SheetNestedScroll.postScroll(-40f, height = 220f, minHeight = 200f))
        assertEquals(0f, SheetNestedScroll.postScroll(-40f, height = 200f, minHeight = 200f))
        assertEquals(0f, SheetNestedScroll.postScroll(40f, height = 1000f, minHeight = 200f))
    }

    @Test
    fun flingGoesToTheSheetOnlyWhenItShould() {
        assertTrue(SheetNestedScroll.sheetTakesFling(800f, height = 1000f, maxHeight = 2000f, sheetMoved = false))
        assertFalse(SheetNestedScroll.sheetTakesFling(800f, height = 2000f, maxHeight = 2000f, sheetMoved = true))
        assertFalse(SheetNestedScroll.sheetTakesFling(-800f, height = 2000f, maxHeight = 2000f, sheetMoved = false))
        assertTrue(SheetNestedScroll.sheetTakesFling(-800f, height = 1500f, maxHeight = 2000f, sheetMoved = true))
    }

    @Test
    fun predictiveBackShrinksTheSheet() {
        assertEquals(SheetBackTransform(1f, 1f), SheetBackMath.transform(0f, 1000f, 500f, 120f, 60f))
        val half = SheetBackMath.transform(0.5f, 1000f, 500f, 120f, 60f)
        assertEquals(0.94f, half.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(0.94f, half.scaleY, absoluteTolerance = 0.0001f)
        assertEquals(SheetBackMath.transform(1f, 1000f, 500f, 120f, 60f), SheetBackMath.transform(3f, 1000f, 500f, 120f, 60f))
        assertEquals(SheetBackTransform(1f, 1f), SheetBackMath.transform(1f, 0f, 0f, 120f, 60f))
    }
}
