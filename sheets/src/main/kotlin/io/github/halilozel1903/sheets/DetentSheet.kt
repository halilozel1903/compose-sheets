package io.github.halilozel1903.sheets

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.sheets.core.DetentResolver
import io.github.halilozel1903.sheets.core.SheetBackMath
import io.github.halilozel1903.sheets.core.SheetDetent
import io.github.halilozel1903.sheets.core.SheetLayoutMath
import io.github.halilozel1903.sheets.core.SheetPresentation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A bottom sheet that rests at any number of detents (for example peek, half and full).
 *
 * Place it last in a `Box` that fills the screen, over the content it covers: it fills its parent, draws the scrim
 * and places the sheet at the bottom. Only the sheet itself takes touches, so in persistent mode ([modal] `false`)
 * the content behind stays interactive, like the map in a maps app.
 *
 * - Drag the sheet, or a list inside it: the sheet grows to its highest detent before the list scrolls, and the
 *   list scrolls back to its top before the sheet shrinks.
 * - Back (with the predictive back animation) steps down one detent in persistent mode and hides a modal sheet.
 * - The keyboard pushes the sheet up and detents shrink to the space above it, so a text field at the bottom of
 *   the sheet stays visible. This needs `adjustResize` (or edge-to-edge) and no `imePadding()` on a parent.
 *
 * @param state The sheet's position, from [rememberSheetState].
 * @param modal `true` for a modal sheet: the scrim is fully on whenever the sheet is open, a tap on it hides the
 * sheet and back hides it. `false` (persistent) dims the content only while the sheet moves to its top detent.
 * @param onDismissRequest Called when the sheet settles hidden after a gesture, back or a scrim tap.
 * @param sheetMaxWidth The sheet is centered and at most this wide on wide windows.
 * @param dragHandle Drawn at the top of the sheet; tap it to step through the detents. `null` for none.
 * @param gesturesEnabled `false` to stop the sheet from following drags (programmatic moves still work).
 * @param topInsets Kept free at the top: a full sheet ends below them.
 * @param imeInsets The keyboard. The sheet rests on it.
 * @param contentWindowInsets Added as space below the content (the navigation bar) unless the keyboard covers it.
 * @param content The sheet's content. It is measured as tall as the highest detent, so `Modifier.weight(1f)` on a
 * list fills the rest; with [SheetDetent.FitContent] it is measured at its own height.
 */
@Composable
public fun DetentSheet(
    state: SheetState,
    modifier: Modifier = Modifier,
    modal: Boolean = false,
    onDismissRequest: () -> Unit = {},
    sheetMaxWidth: Dp = SheetDefaults.MaxWidth,
    shape: Shape = SheetDefaults.BottomSheetShape,
    containerColor: Color = SheetDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = SheetDefaults.TonalElevation,
    shadowElevation: Dp = SheetDefaults.ShadowElevation,
    scrimColor: Color = SheetDefaults.scrimColor,
    dragHandle: (@Composable () -> Unit)? = { SheetDefaults.DragHandle() },
    gesturesEnabled: Boolean = true,
    topInsets: WindowInsets = WindowInsets.statusBars,
    imeInsets: WindowInsets = WindowInsets.ime,
    contentWindowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    SideEffect { state.presentation = SheetPresentation.BottomSheet }
    DismissOnHidden(state, onDismissRequest)

    val backEnabled by remember(state, modal) {
        derivedStateOf {
            if (modal) {
                state.currentDetent != SheetDetent.Hidden || state.targetDetent != SheetDetent.Hidden
            } else {
                state.anchors?.stepDown(state.currentDetent) != null
            }
        }
    }
    val backProgress = sheetBackProgress(enabled = backEnabled) {
        scope.launch { if (modal) state.hide() else state.stepDown() }
    }

    val scrimActive by remember(state, modal) { derivedStateOf { state.scrimFraction(modal) > 0.01f } }
    val hiddenForAccessibility = modal &&
        state.currentDetent == SheetDetent.Hidden &&
        !state.isAnimating &&
        !state.isDragging

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Scrim(
                color = scrimColor,
                fraction = { state.scrimFraction(modal) },
                active = scrimActive,
                onTap = { scope.launch { if (modal) state.hide() else state.stepDown() } },
            )
            SheetBody(
                state = state,
                scope = scope,
                backProgress = backProgress,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
                dragHandle = dragHandle,
                gesturesEnabled = gesturesEnabled,
                hiddenForAccessibility = hiddenForAccessibility,
                topInsets = topInsets,
                imeInsets = imeInsets,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        },
    ) { measurables, constraints ->
        check(constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
            "DetentSheet needs a bounded size: put it in a Box that fills the screen, not in a scrolling container"
        }
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val scrim = measurables[0].measure(Constraints.fixed(width, height))
        val sheet = measurables[1].measure(
            Constraints(maxWidth = min(width, sheetMaxWidth.roundToPx()), maxHeight = height),
        )
        val measureScope = this
        layout(width, height) {
            scrim.place(0, 0)
            val ime = imeInsets.getBottom(measureScope).toFloat()
            val top = SheetLayoutMath.sheetTop(height.toFloat(), ime, state.visibleHeight)
            sheet.place((width - sheet.width) / 2, top.roundToInt())
        }
    }
}

@Composable
private fun SheetBody(
    state: SheetState,
    scope: CoroutineScope,
    backProgress: State<Float>,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    dragHandle: (@Composable () -> Unit)?,
    gesturesEnabled: Boolean,
    hiddenForAccessibility: Boolean,
    topInsets: WindowInsets,
    imeInsets: WindowInsets,
    contentWindowInsets: WindowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    val connection = remember(state) { SheetNestedScrollConnection(state) }
    Layout(
        modifier = Modifier
            .graphicsLayer {
                // A hidden sheet would still cast its shadow above the bottom edge.
                alpha = if (state.visibleHeight > 0f) 1f else 0f
                val progress = backProgress.value
                if (progress > 0f) {
                    val visible = state.visibleHeight
                    val transform = SheetBackMath.transform(
                        progress = progress,
                        width = size.width,
                        height = visible,
                        maxShrinkX = 48.dp.toPx(),
                        maxShrinkY = 24.dp.toPx(),
                    )
                    scaleX = transform.scaleX
                    scaleY = transform.scaleY
                    // Shrink towards the bottom of the visible part, which is the screen's bottom edge.
                    val pivotY = if (size.height > 0f) (visible / size.height).coerceIn(0f, 1f) else 1f
                    transformOrigin = TransformOrigin(0.5f, pivotY)
                } else {
                    scaleX = 1f
                    scaleY = 1f
                }
            }
            .nestedScroll(connection)
            .draggable(
                state = state.draggableState,
                orientation = Orientation.Vertical,
                enabled = gesturesEnabled,
                startDragImmediately = state.isAnimating,
                onDragStopped = { velocity -> scope.launch { state.settle(-velocity) } },
            )
            .then(if (hiddenForAccessibility) Modifier.clearAndSetSemantics { } else Modifier),
        content = {
            Surface(
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
            ) { }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape),
                ) {
                    if (dragHandle != null) {
                        DragHandleBox(state, scope, dragHandle)
                    }
                    content()
                    Spacer(Modifier.windowInsetsBottomHeight(contentWindowInsets.exclude(imeInsets)))
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val containerHeight = constraints.maxHeight.toFloat()
        val available = SheetLayoutMath.availableHeight(
            containerHeight = containerHeight,
            topInset = topInsets.getTop(this).toFloat(),
            imeHeight = imeInsets.getBottom(this).toFloat(),
        )
        val detents = state.detents
        val fitsContent = detents.any { it == SheetDetent.FitContent }
        val fixedMax = DetentResolver.maxFixedHeight(detents, available, density)
        val limit = (if (fitsContent) available else fixedMax).toInt().coerceAtLeast(0)
        val contentPlaceable = measurables[1].measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = if (fitsContent) 0 else limit,
                maxHeight = limit,
            ),
        )
        val anchors = DetentResolver.resolve(detents, available, contentPlaceable.height.toFloat(), density)
        state.updateAnchors(anchors, available, density)
        // The surface reaches below the screen so it never shows a gap while overscrolling.
        val surfaceHeight = (max(contentPlaceable.height.toFloat(), anchors.maxHeight) + state.overshootRoom)
            .roundToInt()
            .coerceAtLeast(1)
        val background = measurables[0].measure(Constraints.fixed(width, surfaceHeight))
        layout(width, min(surfaceHeight, constraints.maxHeight)) {
            background.place(0, 0)
            contentPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun DragHandleBox(state: SheetState, scope: CoroutineScope, dragHandle: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Drag handle"
                onClick(label = "Move the sheet to the next detent") {
                    scope.launch { state.cycle() }
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction("Expand") {
                        scope.launch { state.expand() }
                        true
                    },
                    CustomAccessibilityAction("Collapse") {
                        scope.launch { state.collapse() }
                        true
                    },
                )
            }
            .pointerInput(state) {
                detectTapGestures(onTap = { scope.launch { state.cycle() } })
            },
    ) {
        dragHandle()
    }
}

/** A full size scrim whose alpha follows the sheet; taps on it are handled only while it is visible. */
@Composable
internal fun Scrim(color: Color, fraction: () -> Float, active: Boolean, onTap: () -> Unit) {
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val f = fraction()
                if (f > 0f) drawRect(color.copy(alpha = color.alpha * f))
            }
            .then(
                if (active) {
                    Modifier
                        .pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) }
                        .semantics {
                            contentDescription = "Close sheet"
                            onClick {
                                currentOnTap()
                                true
                            }
                        }
                } else {
                    Modifier
                },
            ),
    )
}

/** Calls [onDismissRequest] each time the sheet settles hidden. */
@Composable
internal fun DismissOnHidden(state: SheetState, onDismissRequest: () -> Unit) {
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    LaunchedEffect(state) {
        snapshotFlow { state.currentDetent }
            .drop(1)
            .collect { detent -> if (detent == SheetDetent.Hidden) currentOnDismiss() }
    }
}
