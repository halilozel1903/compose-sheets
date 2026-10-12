package io.github.halilozel1903.sheets

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import io.github.halilozel1903.sheets.core.PositionalThreshold
import io.github.halilozel1903.sheets.core.RubberBand
import io.github.halilozel1903.sheets.core.ScrimMath
import io.github.halilozel1903.sheets.core.SheetAnchors
import io.github.halilozel1903.sheets.core.SheetDetent
import io.github.halilozel1903.sheets.core.SheetLayoutMath
import io.github.halilozel1903.sheets.core.SheetPresentation
import io.github.halilozel1903.sheets.core.SheetSnapper
import io.github.halilozel1903.sheets.core.SnapConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The position of a [DetentSheet] or [AdaptiveSheet]: which detent it rests at, where it is heading and how much of
 * it is visible. Create one with [rememberSheetState].
 *
 * The sheet starts settled at [initialDetent]: the first layout places it there without an animation.
 *
 * @param detents The detents the sheet can rest at, in any order. [SheetDetent.Hidden] may be left out; the sheet
 * can still be hidden with [hide], but users cannot swipe it away.
 * @param initialDetent One of [detents], or [SheetDetent.Hidden].
 * @param animationSpec The animation used to settle at a detent.
 * @param velocityThreshold A release faster than this moves to the next detent.
 * @param positionalThreshold How far to drag towards the next detent for a slow release to settle there.
 * @param confirmDetentChange Called before the sheet settles at a new detent after a gesture; return `false` to stay.
 */
@Stable
public class SheetState(
    detents: List<SheetDetent>,
    initialDetent: SheetDetent = detents.first(),
    internal val animationSpec: AnimationSpec<Float> = SheetDefaults.SheetAnimationSpec,
    internal val velocityThreshold: Dp = SheetDefaults.VelocityThreshold,
    internal val positionalThreshold: PositionalThreshold = SheetDefaults.DefaultPositionalThreshold,
    internal val confirmDetentChange: (SheetDetent) -> Boolean = { true },
) {
    init {
        require(detents.isNotEmpty()) { "A sheet needs at least one detent" }
        require(initialDetent == SheetDetent.Hidden || detents.any { it.key == initialDetent.key }) {
            "initialDetent ${initialDetent.key} is not one of the detents"
        }
    }

    /** The detents this sheet can rest at, without duplicates. */
    public val detents: List<SheetDetent> = detents.distinctBy { it.key }

    /** The detent the sheet last settled at. */
    public var currentDetent: SheetDetent by mutableStateOf(initialDetent)
        private set

    /** The detent the sheet is moving to: the animation's target, or where a drag would settle if released now. */
    public var targetDetent: SheetDetent by mutableStateOf(initialDetent)
        private set

    /** `true` while the sheet animates to a detent. */
    public var isAnimating: Boolean by mutableStateOf(false)
        private set

    /** `true` while the user drags the sheet, directly or through a list inside it. */
    public var isDragging: Boolean by mutableStateOf(false)
        private set

    private val heightState = mutableFloatStateOf(Float.NaN)

    /** How many pixels of the sheet are visible above the bottom edge (or the keyboard). 0 before the first layout. */
    public val visibleHeight: Float
        get() = heightState.floatValue.let { if (it.isNaN()) 0f else it }

    /** [visibleHeight] as a share of the height detents are resolved against, 0 to 1 (a little more while overscrolling). */
    public val visibleFraction: Float
        get() = SheetLayoutMath.visibleFraction(heightState.floatValue, availableHeight)

    /** `true` unless the sheet rests hidden. */
    public val isVisible: Boolean
        get() = currentDetent != SheetDetent.Hidden || targetDetent != SheetDetent.Hidden || visibleHeight > 0.5f

    /** The resolved detents of the last layout, `null` before the first one. */
    public var anchors: SheetAnchors? by mutableStateOf<SheetAnchors?>(null)
        private set

    /** The pixel height of [detent] in the last layout, or `null` before the first layout. */
    public fun heightOf(detent: SheetDetent): Float? = anchors?.heightOf(detent)

    internal var availableHeight: Float by mutableFloatStateOf(0f)
        private set

    internal var presentation: SheetPresentation = SheetPresentation.BottomSheet

    private var density = 1f
    private val mutex = MutatorMutex()
    private var rawHeight = 0f
    private var dragStartHeight = 0f
    private var dragMin = 0f
    private var dragMax = 0f

    /** How far the sheet can be pulled past its first and last detent. */
    internal val overshootRoom: Float get() = max(availableHeight * 0.08f, 1f)

    /** Animates to [detent] with an optional initial [velocity] (pixels per second, positive grows the sheet). */
    public suspend fun animateTo(detent: SheetDetent, velocity: Float = 0f) {
        requireKnown(detent)
        val target = anchors?.heightOf(detent)
        if (target == null || presentation == SheetPresentation.SideSheet) {
            snapTo(detent)
            return
        }
        mutex.mutate(MutatePriority.Default) {
            targetDetent = detent
            isAnimating = true
            try {
                val start = heightState.floatValue.let { if (it.isNaN()) target else it }
                animate(start, target, velocity, animationSpec) { value, _ -> heightState.floatValue = value }
                currentDetent = detent
                heightState.floatValue = anchors?.heightOf(detent) ?: target
            } finally {
                isAnimating = false
            }
        }
    }

    /** Moves to [detent] without an animation. */
    public suspend fun snapTo(detent: SheetDetent) {
        requireKnown(detent)
        mutex.mutate(MutatePriority.Default) {
            currentDetent = detent
            targetDetent = detent
            anchors?.heightOf(detent)?.let { heightState.floatValue = it }
        }
    }

    /** Animates to the highest detent. */
    public suspend fun expand() {
        animateTo(highestDetent())
    }

    /** Animates to the lowest detent that is not [SheetDetent.Hidden]. */
    public suspend fun collapse() {
        animateTo(lowestVisibleDetent())
    }

    /** Animates off screen. Works whether or not [SheetDetent.Hidden] is one of the detents. */
    public suspend fun hide() {
        animateTo(SheetDetent.Hidden)
    }

    /** Animates to [detent], by default the lowest detent that is not hidden. */
    public suspend fun show(detent: SheetDetent = lowestVisibleDetent()) {
        animateTo(detent)
    }

    /** Animates one detent up from the current one; does nothing at the top. */
    public suspend fun stepUp() {
        val next = anchors?.stepUp(currentDetent)?.detent ?: return
        animateTo(next)
    }

    /** Animates one detent down from the current one; does nothing at the bottom. */
    public suspend fun stepDown() {
        val next = anchors?.stepDown(currentDetent)?.detent ?: return
        animateTo(next)
    }

    private fun highestDetent(): SheetDetent =
        anchors?.highest?.detent ?: detents.last { it != SheetDetent.Hidden }

    private fun lowestVisibleDetent(): SheetDetent =
        anchors?.snapPoints?.firstOrNull { it.detent != SheetDetent.Hidden }?.detent
            ?: detents.firstOrNull { it != SheetDetent.Hidden }
            ?: detents.first()

    private fun requireKnown(detent: SheetDetent) {
        require(detent == SheetDetent.Hidden || detents.any { it.key == detent.key }) {
            "${detent.key} is not one of this sheet's detents"
        }
    }

    // Layout

    /** Called from the sheet's measure pass with the detents resolved for this layout. */
    internal fun updateAnchors(newAnchors: SheetAnchors, available: Float, density: Float) {
        this.density = density
        if (availableHeight != available) availableHeight = available
        val unchanged = newAnchors == anchors
        if (!unchanged) anchors = newAnchors
        if (unchanged && !heightState.floatValue.isNaN()) return
        if (isAnimating || isDragging) return
        val height = newAnchors.heightOf(currentDetent)
        if (height != null) {
            heightState.floatValue = height
        } else {
            // The current detent is gone (the detents changed): rest at the closest one.
            val closest = newAnchors.closest(visibleHeight) ?: return
            currentDetent = closest.detent
            targetDetent = closest.detent
            heightState.floatValue = closest.height
        }
    }

    // Gestures

    /**
     * Moves the sheet by [delta] pixels (positive grows it) and returns how far it moved. With [rubberBand] it moves
     * past the first and last detent with resistance; without, it stops at them.
     */
    internal fun dragSheetBy(delta: Float, rubberBand: Boolean = true): Float {
        val a = anchors ?: return 0f
        if (a.isEmpty) return 0f
        val current = visibleHeight
        if (!isDragging) {
            isDragging = true
            dragStartHeight = current
            dragMin = min(a.minHeight, current)
            dragMax = max(a.maxHeight, current)
            rawHeight = RubberBand.rawFor(current, dragMin, dragMax, overshootRoom)
        }
        val next = if (rubberBand) {
            rawHeight += delta
            RubberBand.clamp(rawHeight, dragMin, dragMax, overshootRoom)
        } else {
            val clamped = (current + delta).coerceIn(dragMin, dragMax)
            rawHeight = RubberBand.rawFor(clamped, dragMin, dragMax, overshootRoom)
            clamped
        }
        heightState.floatValue = next
        SheetSnapper.target(a, next, 0f, snapConfig(), dragStartHeight)?.let { targetDetent = it.detent }
        return next - current
    }

    /** Ends a drag and settles at the detent the release picks, with [velocity] in pixels per second (positive grows). */
    internal suspend fun settle(velocity: Float) {
        val wasDragging = isDragging
        isDragging = false
        val a = anchors ?: return
        val picked = SheetSnapper.target(
            anchors = a,
            height = visibleHeight,
            velocity = velocity,
            config = snapConfig(),
            startHeight = if (wasDragging) dragStartHeight else null,
        )?.detent ?: return
        val target = if (picked.key == currentDetent.key || confirmDetentChange(picked)) picked else currentDetent
        animateTo(target, velocity)
    }

    /** Ends a drag that left the sheet resting exactly on a detent, without an animation. */
    internal fun finishDragAtRest() {
        isDragging = false
        val a = anchors ?: return
        val resting = a.closest(visibleHeight) ?: return
        if (abs(resting.height - visibleHeight) <= 0.5f) {
            currentDetent = resting.detent
            targetDetent = resting.detent
        }
    }

    /** The scrim's strength from 0 to 1 at the current height. */
    internal fun scrimFraction(modal: Boolean): Float {
        val a = anchors ?: return 0f
        return ScrimMath.alpha(visibleHeight, ScrimMath.defaultRange(a, modal), 1f)
    }

    /** One detent up, or the lowest visible one at the top: what a tap on the drag handle does. */
    internal suspend fun cycle() {
        val a = anchors ?: return
        val up = a.stepUp(currentDetent)?.detent
        if (up != null) animateTo(up) else collapse()
    }

    private fun snapConfig(): SnapConfig = SnapConfig(
        velocityThreshold = velocityThreshold.value * density,
        positionalThreshold = positionalThreshold,
    )

    /** Drags from the sheet's surface; drag deltas are in screen coordinates (positive moves down). */
    internal val draggableState: DraggableState = object : DraggableState {
        private val scope = object : DragScope {
            override fun dragBy(pixels: Float) {
                dragSheetBy(-pixels)
            }
        }

        override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) {
            mutex.mutate(dragPriority) { scope.block() }
        }

        override fun dispatchRawDelta(delta: Float) {
            dragSheetBy(-delta)
        }
    }

    public companion object {
        /** Saves the detent the sheet rests at; the other parameters must match the ones the state was created with. */
        public fun Saver(
            detents: List<SheetDetent>,
            animationSpec: AnimationSpec<Float>,
            velocityThreshold: Dp,
            positionalThreshold: PositionalThreshold,
            confirmDetentChange: (SheetDetent) -> Boolean,
        ): Saver<SheetState, String> = Saver<SheetState, String>(
            save = { state -> state.currentDetent.key },
            restore = { key ->
                val detent = detents.firstOrNull { it.key == key }
                    ?: if (key == SheetDetent.Hidden.key) SheetDetent.Hidden else detents.first()
                SheetState(detents, detent, animationSpec, velocityThreshold, positionalThreshold, confirmDetentChange)
            },
        )
    }
}

/**
 * Creates a [SheetState] that survives configuration changes and process death (it saves the current detent).
 *
 * ```
 * val state = rememberSheetState(
 *     detents = listOf(SheetDetent.peek(120.dp), SheetDetent.Half, SheetDetent.Full),
 *     initialDetent = SheetDetent.Half,
 * )
 * ```
 */
@Composable
public fun rememberSheetState(
    detents: List<SheetDetent> = SheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
    animationSpec: AnimationSpec<Float> = SheetDefaults.SheetAnimationSpec,
    velocityThreshold: Dp = SheetDefaults.VelocityThreshold,
    positionalThreshold: PositionalThreshold = SheetDefaults.DefaultPositionalThreshold,
    confirmDetentChange: (SheetDetent) -> Boolean = { true },
): SheetState = rememberSaveable(
    detents,
    saver = SheetState.Saver(detents, animationSpec, velocityThreshold, positionalThreshold, confirmDetentChange),
) {
    SheetState(detents, initialDetent, animationSpec, velocityThreshold, positionalThreshold, confirmDetentChange)
}
