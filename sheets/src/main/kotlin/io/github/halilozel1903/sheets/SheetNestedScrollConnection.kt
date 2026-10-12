package io.github.halilozel1903.sheets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import io.github.halilozel1903.sheets.core.SheetNestedScroll

/**
 * Lets a list inside the sheet share drags with the sheet: dragging up grows the sheet to its highest detent before
 * the list scrolls, dragging down scrolls the list to its top before the sheet shrinks. Screen deltas are positive
 * downwards, sheet deltas positive upwards, hence the sign flips.
 */
internal class SheetNestedScrollConnection(private val state: SheetState) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val anchors = state.anchors ?: return Offset.Zero
        val take = SheetNestedScroll.preScroll(-available.y, state.visibleHeight, anchors.maxHeight)
        if (take == 0f) return Offset.Zero
        val moved = state.dragSheetBy(take, rubberBand = false)
        return Offset(0f, -moved)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val anchors = state.anchors ?: return Offset.Zero
        val take = SheetNestedScroll.postScroll(-available.y, state.visibleHeight, anchors.minHeight)
        if (take == 0f) return Offset.Zero
        val moved = state.dragSheetBy(take, rubberBand = false)
        return Offset(0f, -moved)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val anchors = state.anchors ?: return Velocity.Zero
        val velocity = -available.y
        val moved = state.isDragging
        return if (SheetNestedScroll.sheetTakesFling(velocity, state.visibleHeight, anchors.maxHeight, moved)) {
            state.settle(velocity)
            available
        } else {
            if (moved) state.finishDragAtRest()
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!state.isDragging) return Velocity.Zero
        state.settle(-available.y)
        return available
    }
}
