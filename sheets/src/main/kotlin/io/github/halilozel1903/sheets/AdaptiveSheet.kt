package io.github.halilozel1903.sheets

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.sheets.core.AdaptiveSheetPolicy
import io.github.halilozel1903.sheets.core.SheetBackMath
import io.github.halilozel1903.sheets.core.SheetDetent
import io.github.halilozel1903.sheets.core.SheetPresentation
import kotlinx.coroutines.launch

/** How the closest [AdaptiveSheet] is shown, for content that adapts (for example a close button on side sheets). */
public val LocalSheetPresentation: ProvidableCompositionLocal<SheetPresentation> =
    staticCompositionLocalOf { SheetPresentation.BottomSheet }

/**
 * A sheet that is a [DetentSheet] on compact widths and a side sheet on medium and expanded widths (by default),
 * decided by [policy] from the width it gets, so it also adapts in split screen and when a foldable unfolds.
 *
 * Both share [state]: the side sheet is shown unless the state rests at [SheetDetent.Hidden], and coming back to a
 * bottom sheet keeps the detent. Content can read [LocalSheetPresentation] to adapt.
 *
 * A persistent side sheet ([modal] `false`) floats next to the content behind it, like the list next to the map in
 * a maps app on a tablet. A modal one covers the full height with a scrim, can be swiped towards its edge to close,
 * and closes with back (with the predictive back animation).
 */
@Composable
public fun AdaptiveSheet(
    state: SheetState,
    modifier: Modifier = Modifier,
    modal: Boolean = false,
    onDismissRequest: () -> Unit = {},
    policy: AdaptiveSheetPolicy = AdaptiveSheetPolicy(),
    sideSheetEdge: SideSheetEdge = SideSheetEdge.End,
    sideSheetShape: Shape = if (modal) SheetDefaults.modalSideSheetShape(sideSheetEdge) else SheetDefaults.FloatingSideSheetShape,
    bottomSheetShape: Shape = SheetDefaults.BottomSheetShape,
    containerColor: Color = SheetDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    shadowElevation: Dp = SheetDefaults.ShadowElevation,
    scrimColor: Color = SheetDefaults.scrimColor,
    dragHandle: (@Composable () -> Unit)? = { SheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val presentation = policy.presentation(maxWidth.value)
        CompositionLocalProvider(LocalSheetPresentation provides presentation) {
            when (presentation) {
                SheetPresentation.BottomSheet -> DetentSheet(
                    state = state,
                    modal = modal,
                    onDismissRequest = onDismissRequest,
                    shape = bottomSheetShape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    shadowElevation = shadowElevation,
                    scrimColor = scrimColor,
                    dragHandle = dragHandle,
                    content = content,
                )
                SheetPresentation.SideSheet -> SideSheet(
                    state = state,
                    width = policy.sideSheetWidth(maxWidth.value).dp,
                    edge = sideSheetEdge,
                    modal = modal,
                    onDismissRequest = onDismissRequest,
                    shape = sideSheetShape,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    shadowElevation = shadowElevation,
                    scrimColor = scrimColor,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun SideSheet(
    state: SheetState,
    width: Dp,
    edge: SideSheetEdge,
    modal: Boolean,
    onDismissRequest: () -> Unit,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    shadowElevation: Dp,
    scrimColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    SideEffect { state.presentation = SheetPresentation.SideSheet }
    DismissOnHidden(state, onDismissRequest)

    val visible = state.targetDetent != SheetDetent.Hidden
    // Starts at its resting value, so a sheet that is open on first composition does not slide in.
    val shown = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) { shown.animateTo(if (visible) 1f else 0f, SheetDefaults.SheetAnimationSpec) }

    val backProgress = sheetBackProgress(enabled = modal && visible) { scope.launch { state.hide() } }
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // +1 when the sheet leaves towards the right edge of the screen.
    val outwards = if ((edge == SideSheetEdge.End) != rtl) 1f else -1f
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        // Only towards the edge: the sheet does not open further than its width.
        dragOffset = (dragOffset + delta * outwards).coerceAtLeast(0f)
    }

    Box(Modifier.fillMaxSize()) {
        if (modal) {
            Scrim(
                color = scrimColor,
                fraction = { shown.value * (1f - backProgress.value * 0.5f) },
                active = visible,
                onTap = { scope.launch { state.hide() } },
            )
        }
        if (visible || shown.value > 0f) {
            val outer = if (modal) {
                Modifier.fillMaxHeight()
            } else {
                Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
            }
            Surface(
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                shadowElevation = shadowElevation,
                modifier = Modifier
                    .align(if (edge == SideSheetEdge.End) Alignment.CenterEnd else Alignment.CenterStart)
                    .then(outer)
                    .width(width)
                    .graphicsLayer {
                        val slide = (1f - shown.value) * (size.width + 24.dp.toPx())
                        translationX = (slide + dragOffset) * outwards
                        val progress = backProgress.value
                        if (progress > 0f) {
                            val transform = SheetBackMath.transform(progress, size.width, size.height, 24.dp.toPx(), 48.dp.toPx())
                            scaleX = transform.scaleX
                            scaleY = transform.scaleY
                            transformOrigin = TransformOrigin(if (outwards > 0f) 1f else 0f, 0.5f)
                        } else {
                            scaleX = 1f
                            scaleY = 1f
                        }
                    }
                    .then(
                        if (modal) {
                            Modifier.draggable(
                                state = dragState,
                                orientation = Orientation.Horizontal,
                                onDragStopped = { velocity ->
                                    scope.launch {
                                        val towardsEdge = velocity * outwards
                                        val widthPx = with(density) { width.toPx() }
                                        if (towardsEdge > 600f || dragOffset > widthPx * 0.4f) {
                                            state.hide()
                                        }
                                        Animatable(dragOffset).animateTo(0f, SheetDefaults.SheetAnimationSpec) { dragOffset = value }
                                    }
                                },
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    val insets = if (modal) {
                        WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.ime)
                            .only(WindowInsetsSides.Vertical)
                    } else {
                        WindowInsets(0.dp)
                    }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(insets),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
