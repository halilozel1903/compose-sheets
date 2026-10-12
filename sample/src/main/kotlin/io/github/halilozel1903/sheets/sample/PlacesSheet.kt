package io.github.halilozel1903.sheets.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.sheets.LocalSheetPresentation
import io.github.halilozel1903.sheets.SheetState
import io.github.halilozel1903.sheets.core.SheetDetent
import io.github.halilozel1903.sheets.core.SheetPresentation
import kotlinx.coroutines.launch

/**
 * The persistent places sheet: a header that fits the peek detent, filters and a list of places. The list shares
 * drags with the sheet (it grows to full before the list scrolls). On a tablet the same content is a side sheet.
 */
@Composable
fun ColumnScope.PlacesSheetContent(state: SheetState, onPlaceClick: (Place) -> Unit) {
    val scope = rememberCoroutineScope()
    val side = LocalSheetPresentation.current == SheetPresentation.SideSheet

    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = if (side) 20.dp else 0.dp, bottom = 4.dp)) {
        Text(
            text = "Coffee near $AREA_NAME",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${places.size} places · Sorted by distance",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill("Open now", selected = true)
        Pill("Top rated", selected = false)
        Pill("Roasters", selected = false)
        Pill("Wi-Fi", selected = false)
        Pill("Outdoor seating", selected = false)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)) {
            items(places, key = { it.id }) { place ->
                PlaceRow(place, onClick = { onPlaceClick(place) })
            }
        }
        // Like a maps app: at the top detent a button brings the map back.
        if (!side && state.targetDetent == SheetDetent.Full) {
            Button(
                onClick = { scope.launch { state.animateTo(SheetDetent.Half) } },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
            ) {
                Text("Show map", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun PlaceRow(place: Place, onClick: () -> Unit) {
    val statusColor = openColor(place.open)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Comment on ${place.name}", onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(place.tint, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = place.name.first().toString(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(16.dp))
        // weight + ellipsis: long names never push the distance off the row.
        Column(Modifier.weight(1f)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "★ ${place.rating} (${place.reviews}) · ${place.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.SemiBold)) {
                        append(if (place.open) "Open" else "Closed")
                    }
                    append(" · ${place.hours}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = place.distance,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
