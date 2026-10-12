package io.github.halilozel1903.sheets.core

import kotlin.math.abs

/** How far a sheet has to be dragged between two detents before it settles at the next one. */
public fun interface PositionalThreshold {
    /** The distance in pixels to drag, given the [distance] between the two detents. */
    public fun threshold(distance: Float): Float

    public companion object {
        /** A share of the distance between the detents, 0.5 meaning halfway. */
        public fun fraction(fraction: Float): PositionalThreshold {
            require(fraction in 0f..1f) { "fraction must be between 0 and 1, was $fraction" }
            return PositionalThreshold { distance -> distance * fraction }
        }

        /** A fixed number of pixels, at most the distance between the detents. */
        public fun fixed(pixels: Float): PositionalThreshold = PositionalThreshold { distance -> minOf(pixels, distance) }
    }
}

/**
 * Snapping configuration.
 *
 * @property velocityThreshold A release faster than this (pixels per second) moves to the next detent in the
 * direction of the fling, however short the drag.
 * @property positionalThreshold How far to drag towards the next detent for a slow release to settle there.
 */
public data class SnapConfig(
    public val velocityThreshold: Float,
    public val positionalThreshold: PositionalThreshold = PositionalThreshold.fraction(0.5f),
)

/** Picks the detent a released sheet settles at. Heights grow upwards; a positive velocity grows the sheet. */
public object SheetSnapper {
    /**
     * The detent a sheet released at [height] with [velocity] (pixels per second, positive = upwards) settles at.
     *
     * [startHeight] is where the drag began; it decides the direction for the positional threshold. Without it the
     * closest detent wins a slow release. Returns `null` when [anchors] is empty.
     */
    public fun target(
        anchors: SheetAnchors,
        height: Float,
        velocity: Float,
        config: SnapConfig,
        startHeight: Float? = null,
    ): ResolvedDetent? {
        val points = anchors.snapPoints
        if (points.isEmpty()) return null
        if (points.size == 1) return points.first()

        val lowest = points.first()
        val highest = points.last()
        if (height <= lowest.height) {
            return if (velocity >= config.velocityThreshold) anchors.above(lowest.height) ?: lowest else lowest
        }
        if (height >= highest.height) {
            return if (-velocity >= config.velocityThreshold) anchors.below(highest.height) ?: highest else highest
        }

        // Exactly on a detent: only a fling moves it.
        val resting = points.firstOrNull { abs(it.height - height) <= 0.5f }
        val fling = abs(velocity) >= config.velocityThreshold
        if (resting != null) {
            if (!fling) return resting
            return if (velocity > 0) anchors.above(height) ?: resting else anchors.below(height) ?: resting
        }

        val lower = points.last { it.height < height }
        val upper = points.first { it.height > height }
        if (fling) return if (velocity > 0) upper else lower

        val distance = upper.height - lower.height
        val threshold = config.positionalThreshold.threshold(distance).coerceIn(0f, distance)
        return when {
            startHeight != null && startHeight <= lower.height + 0.5f ->
                if (height >= lower.height + threshold) upper else lower
            startHeight != null && startHeight >= upper.height - 0.5f ->
                if (height <= upper.height - threshold) lower else upper
            else -> if (height - lower.height < upper.height - height) lower else upper
        }
    }
}
