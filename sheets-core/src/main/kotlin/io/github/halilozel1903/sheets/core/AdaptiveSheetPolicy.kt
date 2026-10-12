package io.github.halilozel1903.sheets.core

/** Material 3 window width classes. */
public enum class WindowWidthClass {
    /** Below 600 dp: phones in portrait. */
    Compact,

    /** 600 dp to 840 dp: foldables unfolded, small tablets in portrait. */
    Medium,

    /** 840 dp and wider: tablets in landscape, desktops. */
    Expanded,
    ;

    public companion object {
        /** The width class of a window [widthDp] wide. */
        public fun fromWidth(widthDp: Float): WindowWidthClass = when {
            widthDp < 600f -> Compact
            widthDp < 840f -> Medium
            else -> Expanded
        }
    }
}

/** How an adaptive sheet is shown. */
public enum class SheetPresentation {
    /** Anchored to the bottom edge with detents. */
    BottomSheet,

    /** Anchored to the start or end edge at full height. */
    SideSheet,
}

/**
 * Decides between a bottom sheet and a side sheet from the width the sheet gets.
 *
 * @property sideSheetFrom The narrowest width class that gets a side sheet; Medium by default.
 * @property sideSheetWidthFraction The side sheet's share of the width...
 * @property minSideSheetWidthDp ...kept at least this wide...
 * @property maxSideSheetWidthDp ...and at most this wide.
 */
public data class AdaptiveSheetPolicy(
    public val sideSheetFrom: WindowWidthClass = WindowWidthClass.Medium,
    public val sideSheetWidthFraction: Float = 0.36f,
    public val minSideSheetWidthDp: Float = 320f,
    public val maxSideSheetWidthDp: Float = 420f,
) {
    init {
        require(sideSheetWidthFraction in 0f..1f) { "sideSheetWidthFraction must be between 0 and 1" }
        require(minSideSheetWidthDp <= maxSideSheetWidthDp) { "minSideSheetWidthDp must not exceed maxSideSheetWidthDp" }
    }

    /** The presentation for a container [widthDp] wide. */
    public fun presentation(widthDp: Float): SheetPresentation =
        if (WindowWidthClass.fromWidth(widthDp) >= sideSheetFrom) SheetPresentation.SideSheet else SheetPresentation.BottomSheet

    /** The side sheet's width in dp for a container [widthDp] wide, never wider than the container. */
    public fun sideSheetWidth(widthDp: Float): Float =
        (widthDp * sideSheetWidthFraction).coerceIn(minSideSheetWidthDp, maxSideSheetWidthDp).coerceAtMost(widthDp)
}
