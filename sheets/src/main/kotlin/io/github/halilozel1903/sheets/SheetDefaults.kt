package io.github.halilozel1903.sheets

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.sheets.core.PositionalThreshold
import io.github.halilozel1903.sheets.core.SheetDetent

/** Defaults for [DetentSheet], [AdaptiveSheet] and [rememberSheetState]. */
public object SheetDefaults {
    /** Peek (120 dp), half and full. */
    public val Detents: List<SheetDetent> = listOf(SheetDetent.peek(120f), SheetDetent.Half, SheetDetent.Full)

    /** A release faster than this moves to the next detent. */
    public val VelocityThreshold: Dp = 125.dp

    /** A slow release settles at the next detent once the sheet is halfway there. */
    public val DefaultPositionalThreshold: PositionalThreshold = PositionalThreshold.fraction(0.5f)

    /** A critically damped spring: quick, no bounce. */
    public val SheetAnimationSpec: AnimationSpec<Float> = spring<Float>(dampingRatio = 1f, stiffness = 500f)

    /** Bottom sheets are at most this wide and centered on wider windows. */
    public val MaxWidth: Dp = 640.dp

    /** Rounded top corners. */
    public val BottomSheetShape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** A floating side sheet with rounded corners (persistent). */
    public val FloatingSideSheetShape: Shape = RoundedCornerShape(24.dp)

    /** A side sheet rounded on its inner edge (modal). */
    public fun modalSideSheetShape(edge: SideSheetEdge): Shape = when (edge) {
        SideSheetEdge.End -> RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
        SideSheetEdge.Start -> RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    }

    /** The sheet's background. */
    public val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow

    /** The scrim's color at full strength. */
    public val scrimColor: Color
        @Composable get() = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)

    /** Shadow under the sheet. */
    public val ShadowElevation: Dp = 8.dp

    /** Tonal elevation of the sheet. */
    public val TonalElevation: Dp = 1.dp

    /** The Material 3 drag handle: a 32 by 4 dp pill. */
    @Composable
    public fun DragHandle(
        modifier: Modifier = Modifier,
        width: Dp = 32.dp,
        height: Dp = 4.dp,
        color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = width, height = height)
                    .background(color, RoundedCornerShape(50)),
            )
        }
    }
}

/** The edge a side sheet is attached to. Start and end follow the layout direction. */
public enum class SideSheetEdge { Start, End }

/** Creates a [SheetDetent.Fixed] from a [Dp] height. */
public fun SheetDetent.Companion.fixed(height: Dp, key: String = "fixed:${height.value}"): SheetDetent =
    SheetDetent.Fixed(height.value, key)

/** A peek detent of [height], keyed `peek`. */
public fun SheetDetent.Companion.peek(height: Dp): SheetDetent = SheetDetent.peek(height.value)
