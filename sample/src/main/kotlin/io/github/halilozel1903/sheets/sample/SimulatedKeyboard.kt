package io.github.halilozel1903.sheets.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Height of the simulated keyboard above the navigation bar. */
val SimulatedKeyboardHeight = 264.dp

/**
 * The insets a real keyboard of [SimulatedKeyboardHeight] would report: like `WindowInsets.ime`, they include the
 * navigation bar.
 */
@Composable
fun simulatedImeInsets(): WindowInsets =
    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).add(WindowInsets(bottom = SimulatedKeyboardHeight))

/**
 * A drawn keyboard for the `keyboard` screenshot scene. The system keyboard can't be opened at a reliable moment
 * on CI emulators, so that scene passes [simulatedImeInsets] to the sheet instead of `WindowInsets.ime` and draws
 * this in the keyboard's place. The layout is the one the sheet takes with a real keyboard of the same height.
 */
@Composable
fun SimulatedKeyboard(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val background = if (dark) Color(0xFF1F2125) else Color(0xFFE4E7EE)
    val key = if (dark) Color(0xFF3B3E44) else Color.White
    val special = if (dark) Color(0xFF2D3035) else Color(0xFFC8CDD8)
    val label = if (dark) Color(0xFFE3E3E8) else Color(0xFF1B1C1F)
    val action = MaterialTheme.colorScheme.primary
    val onAction = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier
            .fillMaxWidth()
            .background(background)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .clearAndSetSemantics { },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(SimulatedKeyboardHeight)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (word in listOf("wait", "wait.", "waiting")) {
                    Text(
                        text = word,
                        color = label,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            KeyRow {
                for (c in "qwertyuiop") Key(c.toString(), key, label)
            }
            KeyRow {
                Spacer(Modifier.weight(0.5f))
                for (c in "asdfghjkl") Key(c.toString(), key, label)
                Spacer(Modifier.weight(0.5f))
            }
            KeyRow {
                Key("⇧", special, label, weight = 1.5f)
                for (c in "zxcvbnm") Key(c.toString(), key, label)
                Key("⌫", special, label, weight = 1.5f)
            }
            KeyRow {
                Key("?123", special, label, weight = 1.5f, small = true)
                Key(",", special, label)
                Key("", key, label, weight = 5f)
                Key(".", special, label)
                Key("↵", action, onAction, weight = 1.5f)
            }
        }
    }
}

@Composable
private fun ColumnScope.KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .weight(1f),
        content = content,
    )
}

@Composable
private fun RowScope.Key(text: String, color: Color, textColor: Color, weight: Float = 1f, small: Boolean = false) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .background(color, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = textColor, fontSize = if (small) 14.sp else 21.sp, maxLines = 1)
    }
}
