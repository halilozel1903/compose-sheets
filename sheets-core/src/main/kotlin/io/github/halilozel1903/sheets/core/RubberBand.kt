package io.github.halilozel1903.sheets.core

/**
 * Resistance past the first and last detent: the further a sheet is pulled beyond them, the less it moves, and it
 * never goes further than [dimension] pixels past the edge.
 *
 * Uses `(1 - 1 / (x * c / d + 1)) * d`, the curve scroll views use for overscroll.
 */
public object RubberBand {
    /** The default coefficient: lower is stiffer. */
    public const val DEFAULT_COEFFICIENT: Float = 0.55f

    /** How far the sheet moves for a pull of [overshoot] pixels past the edge. Always below [dimension]. */
    public fun resist(overshoot: Float, dimension: Float, coefficient: Float = DEFAULT_COEFFICIENT): Float {
        if (overshoot <= 0f || dimension <= 0f) return 0f
        return (1f - 1f / (overshoot * coefficient / dimension + 1f)) * dimension
    }

    /** The pull that [resist] turns into [resisted]; the inverse of [resist]. */
    public fun pullFor(resisted: Float, dimension: Float, coefficient: Float = DEFAULT_COEFFICIENT): Float {
        if (resisted <= 0f || dimension <= 0f) return 0f
        val ratio = (resisted / dimension).coerceAtMost(0.999f)
        return dimension / coefficient * (1f / (1f - ratio) - 1f)
    }

    /** [raw] kept inside `min..max`, with resistance outside it. */
    public fun clamp(
        raw: Float,
        min: Float,
        max: Float,
        dimension: Float,
        coefficient: Float = DEFAULT_COEFFICIENT,
    ): Float = when {
        raw > max -> max + resist(raw - max, dimension, coefficient)
        raw < min -> min - resist(min - raw, dimension, coefficient)
        else -> raw
    }

    /** The raw position that [clamp] displays as [displayed]; the inverse of [clamp]. */
    public fun rawFor(
        displayed: Float,
        min: Float,
        max: Float,
        dimension: Float,
        coefficient: Float = DEFAULT_COEFFICIENT,
    ): Float = when {
        displayed > max -> max + pullFor(displayed - max, dimension, coefficient)
        displayed < min -> min - pullFor(min - displayed, dimension, coefficient)
        else -> displayed
    }
}
