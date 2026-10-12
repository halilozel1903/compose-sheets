package io.github.halilozel1903.sheets.core

/** Where a bottom sheet sits for a given container, insets and keyboard. All values are pixels. */
public object SheetLayoutMath {
    /**
     * The height detents are resolved against: the [containerHeight] minus the [topInset] (status bar) and the
     * [imeHeight] (keyboard), never negative. With the keyboard open, a Full detent ends under the status bar and a
     * Half detent is half of what is left above the keyboard.
     */
    public fun availableHeight(containerHeight: Float, topInset: Float, imeHeight: Float): Float =
        (containerHeight - topInset.coerceAtLeast(0f) - imeHeight.coerceAtLeast(0f)).coerceAtLeast(0f)

    /** The sheet's top edge for a visible [height]: it rests on the keyboard when one is open. */
    public fun sheetTop(containerHeight: Float, imeHeight: Float, height: Float): Float =
        containerHeight - imeHeight.coerceAtLeast(0f) - height.coerceAtLeast(0f)

    /** The share of the available height the sheet shows, 0 to 1 (more while rubber-banding). */
    public fun visibleFraction(height: Float, availableHeight: Float): Float =
        if (availableHeight <= 0f || height.isNaN()) 0f else (height / availableHeight).coerceAtLeast(0f)
}
