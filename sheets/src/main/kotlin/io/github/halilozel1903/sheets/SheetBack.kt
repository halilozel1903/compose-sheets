package io.github.halilozel1903.sheets

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

/**
 * Handles system back for a sheet with the predictive back gesture: returns the gesture's progress (0 to 1) for the
 * shrink animation, and calls [onBack] when the gesture commits. On Android 13 and lower (or without
 * `android:enableOnBackInvokedCallback`) back works the same without the progress.
 */
@Composable
internal fun sheetBackProgress(enabled: Boolean, onBack: () -> Unit): State<Float> {
    val scope = rememberCoroutineScope()
    val progress = remember { mutableFloatStateOf(0f) }
    val currentOnBack by rememberUpdatedState(onBack)
    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { event -> progress.floatValue = event.progress }
            currentOnBack()
        } finally {
            scope.launch {
                animate(progress.floatValue, 0f) { value, _ -> progress.floatValue = value }
            }
        }
    }
    return progress
}
