package io.github.halilozel1903.sheets.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdaptiveSheetPolicyTest {
    @Test
    fun widthClasses() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(412f))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(600f))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(839f))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(840f))
    }

    @Test
    fun phonesGetABottomSheetTabletsASideSheet() {
        val policy = AdaptiveSheetPolicy()
        assertEquals(SheetPresentation.BottomSheet, policy.presentation(412f))
        assertEquals(SheetPresentation.SideSheet, policy.presentation(673f))
        assertEquals(SheetPresentation.SideSheet, policy.presentation(1280f))
    }

    @Test
    fun sideSheetsCanBeLimitedToExpandedWidths() {
        val policy = AdaptiveSheetPolicy(sideSheetFrom = WindowWidthClass.Expanded)
        assertEquals(SheetPresentation.BottomSheet, policy.presentation(700f))
        assertEquals(SheetPresentation.SideSheet, policy.presentation(900f))
    }

    @Test
    fun sideSheetWidthIsClamped() {
        val policy = AdaptiveSheetPolicy()
        assertEquals(400f, policy.sideSheetWidth(1111.11f), absoluteTolerance = 0.01f)
        assertEquals(420f, policy.sideSheetWidth(1600f))
        assertEquals(320f, policy.sideSheetWidth(700f))
        assertEquals(300f, policy.sideSheetWidth(300f))
    }

    @Test
    fun rejectsInvalidPolicies() {
        assertFailsWith<IllegalArgumentException> { AdaptiveSheetPolicy(minSideSheetWidthDp = 500f, maxSideSheetWidthDp = 400f) }
        assertFailsWith<IllegalArgumentException> { AdaptiveSheetPolicy(sideSheetWidthFraction = 2f) }
    }
}
