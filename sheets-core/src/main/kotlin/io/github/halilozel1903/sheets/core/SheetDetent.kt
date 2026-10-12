package io.github.halilozel1903.sheets.core

/**
 * A height a sheet can rest at.
 *
 * - [Fraction]: a share of the height available to the sheet (the container minus the top inset and the keyboard).
 * - [Fixed]: a height in dp, for example a peek detent that shows a header.
 * - [FitContent]: the measured height of the sheet's content.
 * - [Hidden]: fully off screen.
 *
 * Every detent has a [key], which is what a sheet state saves and restores. Detents with the same key are the same
 * detent, so give your own detents distinct keys.
 */
public sealed class SheetDetent {
    /** A stable name for this detent, used to save and restore the sheet's position. */
    public abstract val key: String

    /** [fraction] of the available height, from 0 to 1. */
    public data class Fraction(
        public val fraction: Float,
        override val key: String = "fraction:$fraction",
    ) : SheetDetent() {
        init {
            require(fraction in 0f..1f) { "fraction must be between 0 and 1, was $fraction" }
        }
    }

    /** A fixed [heightDp] in density independent pixels, capped at the available height. */
    public data class Fixed(
        public val heightDp: Float,
        override val key: String = "fixed:$heightDp",
    ) : SheetDetent() {
        init {
            require(heightDp >= 0f) { "heightDp must not be negative, was $heightDp" }
        }
    }

    /** The height of the sheet's content (drag handle included), capped at the available height. */
    public data object FitContent : SheetDetent() {
        override val key: String get() = "fit-content"
    }

    /** Off screen. A sheet can always be hidden programmatically; list it in the detents to let users swipe it away. */
    public data object Hidden : SheetDetent() {
        override val key: String get() = "hidden"
    }

    public companion object {
        /** Half of the available height. */
        public val Half: SheetDetent = Fraction(0.5f, key = "half")

        /** All of the available height. */
        public val Full: SheetDetent = Fraction(1f, key = "full")

        /** A peek detent of [heightDp] dp, keyed `peek`. */
        public fun peek(heightDp: Float): SheetDetent = Fixed(heightDp, key = "peek")
    }
}

/** A [detent] with its [height] in pixels for one layout. */
public data class ResolvedDetent(public val detent: SheetDetent, public val height: Float)

/**
 * The resolved detents of a sheet, sorted from the lowest to the highest. Detents that resolve to the same height
 * (within half a pixel) share one snap point, the one declared first.
 */
public class SheetAnchors(detents: List<ResolvedDetent>) {
    /** Every resolved detent, lowest first. */
    public val all: List<ResolvedDetent> = detents.sortedBy { it.height }

    /** One detent per distinct height, lowest first. These are the snap points. */
    public val snapPoints: List<ResolvedDetent> = buildList<ResolvedDetent> {
        for (detent in detents.sortedBy { it.height }) {
            val previous = lastOrNull()
            if (previous == null || detent.height - previous.height > SAME_HEIGHT) add(detent)
        }
    }

    /** `true` when there is nothing to snap to. */
    public val isEmpty: Boolean get() = snapPoints.isEmpty()

    /** The lowest snap point's height, 0 when there is none. */
    public val minHeight: Float get() = snapPoints.firstOrNull()?.height ?: 0f

    /** The highest snap point's height, 0 when there is none. */
    public val maxHeight: Float get() = snapPoints.lastOrNull()?.height ?: 0f

    /** The lowest snap point, or `null`. */
    public val lowest: ResolvedDetent? get() = snapPoints.firstOrNull()

    /** The highest snap point, or `null`. */
    public val highest: ResolvedDetent? get() = snapPoints.lastOrNull()

    /** The height of [detent], 0 for [SheetDetent.Hidden] even when it is not listed, `null` when it is unknown. */
    public fun heightOf(detent: SheetDetent): Float? {
        all.firstOrNull { it.detent.key == detent.key }?.let { return it.height }
        return if (detent == SheetDetent.Hidden) 0f else null
    }

    /** `true` when [detent] is one of these anchors. */
    public fun contains(detent: SheetDetent): Boolean = all.any { it.detent.key == detent.key }

    /** The snap point closest to [height]. */
    public fun closest(height: Float): ResolvedDetent? = snapPoints.minByOrNull { kotlin.math.abs(it.height - height) }

    /** The next snap point above [height] (strictly higher), or `null`. */
    public fun above(height: Float): ResolvedDetent? = snapPoints.firstOrNull { it.height > height + SAME_HEIGHT }

    /** The next snap point below [height] (strictly lower), or `null`. */
    public fun below(height: Float): ResolvedDetent? = snapPoints.lastOrNull { it.height < height - SAME_HEIGHT }

    /** The snap point one step above [detent], or `null` at the top. */
    public fun stepUp(detent: SheetDetent): ResolvedDetent? = heightOf(detent)?.let { above(it) }

    /** The snap point one step below [detent], or `null` at the bottom. */
    public fun stepDown(detent: SheetDetent): ResolvedDetent? = heightOf(detent)?.let { below(it) }

    override fun equals(other: Any?): Boolean = other is SheetAnchors && other.all == all

    override fun hashCode(): Int = all.hashCode()

    override fun toString(): String = all.joinToString(prefix = "SheetAnchors(", postfix = ")") { "${it.detent.key}=${it.height}" }

    private companion object {
        const val SAME_HEIGHT = 0.5f
    }
}

/** Turns detents into heights for one layout. */
public object DetentResolver {
    /**
     * Resolves [detents] against [availableHeight] pixels (the container minus the top inset and the keyboard),
     * the measured [contentHeight] for [SheetDetent.FitContent] and the screen [density] for [SheetDetent.Fixed].
     * Every height is clamped to `0..availableHeight`.
     */
    public fun resolve(
        detents: List<SheetDetent>,
        availableHeight: Float,
        contentHeight: Float,
        density: Float,
    ): SheetAnchors {
        val available = availableHeight.coerceAtLeast(0f)
        val resolved = detents.distinctBy { it.key }.map { detent ->
            val height = when (detent) {
                is SheetDetent.Fraction -> detent.fraction * available
                is SheetDetent.Fixed -> detent.heightDp * density
                SheetDetent.FitContent -> contentHeight
                SheetDetent.Hidden -> 0f
            }
            ResolvedDetent(detent, height.coerceIn(0f, available))
        }
        return SheetAnchors(resolved)
    }

    /**
     * The largest height any detent other than [SheetDetent.FitContent] can take, which is how tall the sheet's
     * surface needs to be before its content is measured.
     */
    public fun maxFixedHeight(detents: List<SheetDetent>, availableHeight: Float, density: Float): Float {
        val available = availableHeight.coerceAtLeast(0f)
        return detents.maxOfOrNull { detent ->
            when (detent) {
                is SheetDetent.Fraction -> detent.fraction * available
                is SheetDetent.Fixed -> detent.heightDp * density
                SheetDetent.FitContent, SheetDetent.Hidden -> 0f
            }.coerceIn(0f, available)
        } ?: 0f
    }
}
