package io.github.halilozel1903.sheets.core

/**
 * How a scrollable list inside a sheet shares a drag with the sheet. Deltas and velocities are in the sheet's
 * direction: positive grows the sheet (the finger moves up), negative shrinks it.
 *
 * - Moving up, the sheet grows to its highest detent first, then the list scrolls.
 * - Moving down, the list scrolls back to its top first, then the sheet shrinks.
 */
public object SheetNestedScroll {
    /** The part of [delta] the sheet takes before the list scrolls. */
    public fun preScroll(delta: Float, height: Float, maxHeight: Float): Float =
        if (delta > 0f && height < maxHeight) minOf(delta, maxHeight - height) else 0f

    /** The part of the [delta] the list left over that the sheet takes. */
    public fun postScroll(delta: Float, height: Float, minHeight: Float): Float =
        if (delta < 0f && height > minHeight) maxOf(delta, minHeight - height) else 0f

    /**
     * `true` when a fling with [velocity] should settle the sheet instead of flinging the list: the sheet was moved
     * during this gesture ([sheetMoved]) and is not resting at its highest detent while flinging up.
     */
    public fun sheetTakesFling(velocity: Float, height: Float, maxHeight: Float, sheetMoved: Boolean): Boolean {
        val belowTop = height < maxHeight - 0.5f
        return when {
            velocity > 0f && belowTop -> true
            sheetMoved -> belowTop || velocity < 0f
            else -> false
        }
    }
}
