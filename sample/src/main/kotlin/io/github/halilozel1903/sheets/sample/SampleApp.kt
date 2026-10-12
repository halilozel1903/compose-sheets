package io.github.halilozel1903.sheets.sample

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.sheets.AdaptiveSheet
import io.github.halilozel1903.sheets.DetentSheet
import io.github.halilozel1903.sheets.core.AdaptiveSheetPolicy
import io.github.halilozel1903.sheets.core.SheetDetent
import io.github.halilozel1903.sheets.core.SheetPresentation
import io.github.halilozel1903.sheets.peek
import io.github.halilozel1903.sheets.rememberSheetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The peek detent shows the sheet's title, place count and filters above the navigation bar, and ends at the
 * divider under the filters (138 dp of header plus the 24 dp gesture bar), so no half cut place row peeks out
 * under the navigation handle.
 */
val PeekDetent: SheetDetent = SheetDetent.peek(164.dp)

/**
 * Waypoint: a map with a persistent places sheet (peek, half and full on phones, a side sheet on tablets) and a
 * modal comment sheet that sits on the keyboard. Tap a place to write a comment.
 */
@Composable
fun SampleApp(scene: Scene?) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val policy = remember { AdaptiveSheetPolicy() }

    val placesDetents = remember { listOf(PeekDetent, SheetDetent.Half, SheetDetent.Full) }
    val placesState = rememberSheetState(
        detents = placesDetents,
        initialDetent = when (scene) {
            Scene.Peek, Scene.Keyboard -> PeekDetent
            Scene.Full -> SheetDetent.Full
            else -> SheetDetent.Half
        },
    )

    val composerDetents = remember { listOf(SheetDetent.Hidden, SheetDetent.FitContent) }
    val composerState = rememberSheetState(
        detents = composerDetents,
        initialDetent = if (scene == Scene.Keyboard) SheetDetent.FitContent else SheetDetent.Hidden,
    )
    var commentPlaceId by rememberSaveable { mutableIntStateOf(places.first().id) }
    var comment by rememberSaveable { mutableStateOf(if (scene == Scene.Keyboard) SAMPLE_COMMENT else "") }
    var rating by rememberSaveable { mutableIntStateOf(if (scene == Scene.Keyboard) 5 else 0) }
    val commentPlace = places.firstOrNull { it.id == commentPlaceId } ?: places.first()

    // The keyboard scene opens the real system keyboard on the comment field, as tapping a place does.
    // scripts/screenshots.sh waits until the system reports the keyboard as shown before it captures.
    if (scene == Scene.Keyboard) {
        LaunchedEffect(Unit) {
            delay(800)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideSheet = policy.presentation(maxWidth.value) == SheetPresentation.SideSheet
        // Keep the pins clear of the side sheet on tablets.
        val usableWidth = if (sideSheet) {
            ((maxWidth.value - policy.sideSheetWidth(maxWidth.value) - 24f) / maxWidth.value).coerceIn(0.4f, 1f)
        } else {
            1f
        }

        HarborMap(places = places, selectedId = places.first().id, usableWidthFraction = usableWidth)
        MapTopBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .then(if (sideSheet) Modifier.widthIn(max = 440.dp) else Modifier.fillMaxWidth()),
        )

        // Persistent: the map stays interactive; the sheet only dims it on the way to full.
        AdaptiveSheet(state = placesState, policy = policy) {
            PlacesSheetContent(
                state = placesState,
                onPlaceClick = { place ->
                    commentPlaceId = place.id
                    comment = ""
                    rating = 0
                    scope.launch {
                        composerState.show(SheetDetent.FitContent)
                        // Opens the keyboard; the sheet moves up with it.
                        runCatching { focusRequester.requestFocus() }
                    }
                },
            )
        }

        // Modal, as tall as its content, resting on the keyboard.
        DetentSheet(
            state = composerState,
            modal = true,
            onDismissRequest = {
                keyboard?.hide()
                focusManager.clearFocus()
            },
            imeInsets = WindowInsets.ime,
        ) {
            CommentComposer(
                place = commentPlace,
                comment = comment,
                onCommentChange = { comment = it },
                rating = rating,
                onRatingChange = { rating = it },
                focusRequester = focusRequester,
                onPost = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    scope.launch { composerState.hide() }
                    comment = ""
                    rating = 0
                },
            )
        }
    }
}
