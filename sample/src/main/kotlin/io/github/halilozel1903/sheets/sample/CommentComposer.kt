package io.github.halilozel1903.sheets.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val MAX_COMMENT_LENGTH = 280

/** The content of the modal comment sheet. It sits on the keyboard, so the field and the button stay visible. */
@Composable
fun CommentComposer(
    place: Place,
    comment: String,
    onCommentChange: (String) -> Unit,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    focusRequester: FocusRequester,
    onPost: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Add a comment",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = place.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (star in 1..5) {
                val filled = star <= rating
                Text(
                    text = if (filled) "★" else "☆",
                    fontSize = 30.sp,
                    color = if (filled) Color(0xFFF2A600) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onRatingChange(star) }
                        .semantics { contentDescription = "$star of 5 stars" }
                        .padding(4.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { if (it.length <= MAX_COMMENT_LENGTH) onCommentChange(it) },
            label = { Text("Your comment") },
            minLines = 3,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Public · ${comment.length}/$MAX_COMMENT_LENGTH",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = onPost, enabled = comment.isNotBlank()) {
                Text("Post comment", maxLines = 1)
            }
        }
    }
}
