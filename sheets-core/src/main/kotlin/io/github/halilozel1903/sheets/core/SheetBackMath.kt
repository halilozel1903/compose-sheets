package io.github.halilozel1903.sheets.core

/** Scale of a sheet during a predictive back gesture. */
public data class SheetBackTransform(public val scaleX: Float, public val scaleY: Float)

/** The predictive back animation of a sheet: it shrinks a little as the back gesture progresses. */
public object SheetBackMath {
    /**
     * The scale at [progress] (0 to 1) for a sheet [width] by [height] pixels that shrinks by at most [maxShrinkX]
     * horizontally and [maxShrinkY] vertically. Material 3 uses 48 dp and 24 dp for bottom sheets.
     */
    public fun transform(
        progress: Float,
        width: Float,
        height: Float,
        maxShrinkX: Float,
        maxShrinkY: Float,
    ): SheetBackTransform {
        val p = progress.coerceIn(0f, 1f)
        val scaleX = if (width > 0f) 1f - p * (maxShrinkX.coerceIn(0f, width) / width) else 1f
        val scaleY = if (height > 0f) 1f - p * (maxShrinkY.coerceIn(0f, height) / height) else 1f
        return SheetBackTransform(scaleX, scaleY)
    }
}
