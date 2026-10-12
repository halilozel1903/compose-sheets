package io.github.halilozel1903.sheets.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Waypoint, a fictional maps app built with compose-sheets. `scripts/screenshots.sh` starts it with
 * `--es scene <scene>` to open a fixed screen for README screenshots, with the sheet already settled:
 *
 * - `peek`, `half`, `full`: the map with the persistent places sheet at that detent (phones)
 * - `keyboard`: a place with the comment composer sheet open above the system keyboard
 * - `tablet`: the map with the places list in a side sheet (tablets in landscape)
 *
 * Without a scene the app starts at the map with the sheet at half.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            SampleTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    SampleApp(scene = scene)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
