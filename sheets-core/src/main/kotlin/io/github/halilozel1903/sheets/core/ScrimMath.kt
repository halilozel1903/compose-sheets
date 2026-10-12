package io.github.halilozel1903.sheets.core

/** The sheet heights over which the scrim fades from transparent ([start]) to fully opaque ([end]). */
public data class ScrimRange(public val start: Float, public val end: Float)

/** The scrim behind a sheet, as a function of the sheet's height. */
public object ScrimMath {
    /** The scrim's alpha at [height]: 0 up to [ScrimRange.start], [maxAlpha] from [ScrimRange.end], linear between. */
    public fun alpha(height: Float, range: ScrimRange?, maxAlpha: Float): Float {
        if (range == null || height.isNaN()) return 0f
        if (range.end <= range.start) return if (height >= range.end && height > 0f) maxAlpha else 0f
        val t = ((height - range.start) / (range.end - range.start)).coerceIn(0f, 1f)
        return t * maxAlpha
    }

    /**
     * The default fade range.
     *
     * - Modal: from hidden (0) to the lowest visible detent, so the scrim is fully on whenever the sheet is open.
     * - Persistent: from the second highest detent to the highest, so the content behind (a map) only dims as the
     *   sheet is pulled to its top detent. `null` (no scrim) with fewer than two visible detents.
     */
    public fun defaultRange(anchors: SheetAnchors, modal: Boolean): ScrimRange? {
        val visible = anchors.snapPoints.filter { it.height > 0.5f }
        if (visible.isEmpty()) return null
        return if (modal) {
            ScrimRange(start = 0f, end = visible.first().height)
        } else {
            if (visible.size < 2) return null
            ScrimRange(start = visible[visible.size - 2].height, end = visible.last().height)
        }
    }
}
