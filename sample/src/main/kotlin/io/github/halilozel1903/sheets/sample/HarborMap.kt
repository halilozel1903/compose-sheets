package io.github.halilozel1903.sheets.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

private class MapColors(
    val land: Color,
    val block: Color,
    val water: Color,
    val park: Color,
    val road: Color,
    val majorRoad: Color,
    val label: Color,
    val labelBackground: Color,
    val location: Color,
) {
    companion object {
        fun of(dark: Boolean): MapColors = if (dark) {
            MapColors(
                land = Color(0xFF1C2024),
                block = Color(0xFF23282D),
                water = Color(0xFF0F2A40),
                park = Color(0xFF1E3326),
                road = Color(0xFF3A4148),
                majorRoad = Color(0xFF5A5340),
                label = Color(0xFFE2E2E9),
                labelBackground = Color(0xFF2A2D33),
                location = Color(0xFF8AB4FF),
            )
        } else {
            MapColors(
                land = Color(0xFFF1F0EA),
                block = Color(0xFFE6E4DC),
                water = Color(0xFFAAD3F2),
                park = Color(0xFFC8E6C1),
                road = Color(0xFFFFFFFF),
                majorRoad = Color(0xFFFFE4A3),
                label = Color(0xFF191C20),
                labelBackground = Color(0xFFFFFFFF),
                location = Color(0xFF2B5BD7),
            )
        }
    }
}

/**
 * A drawn map of the fictional Harbor Square area with a pin per place. Pins are placed in the left
 * [usableWidthFraction] of the width, so a side sheet on the right does not cover them. The [selectedId] pin is
 * larger and labelled.
 */
@Composable
fun HarborMap(
    places: List<Place>,
    selectedId: Int?,
    usableWidthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val colors = remember(dark) { MapColors.of(dark) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = colors.label, fontWeight = FontWeight.SemiBold)
    val selected = places.firstOrNull { it.id == selectedId }

    Canvas(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = "Map of $AREA_NAME with ${places.size} coffee places" },
    ) {
        val w = size.width
        val h = size.height
        drawRect(colors.land)

        // City blocks between the streets.
        val streetsY = listOf(0.12f, 0.27f, 0.44f, 0.58f, 0.74f, 0.9f)
        val streetsX = listOf(0.2f, 0.36f, 0.52f, 0.68f, 0.84f, 1f)
        val inset = 7.dp.toPx()
        for (row in 0 until streetsY.size - 1) {
            for (column in 0 until streetsX.size - 1) {
                drawRoundRect(
                    color = colors.block,
                    topLeft = Offset(streetsX[column] * w + inset, streetsY[row] * h + inset),
                    size = Size((streetsX[column + 1] - streetsX[column]) * w - 2 * inset, (streetsY[row + 1] - streetsY[row]) * h - 2 * inset),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }

        // Parks.
        drawRoundRect(
            color = colors.park,
            topLeft = Offset(0.52f * w + inset, 0.44f * h + inset),
            size = Size(0.16f * w - 2 * inset, 0.14f * h - 2 * inset),
            cornerRadius = CornerRadius(10.dp.toPx()),
        )
        drawRoundRect(
            color = colors.park,
            topLeft = Offset(0.2f * w + inset, 0.74f * h + inset),
            size = Size(0.32f * w - 2 * inset, 0.16f * h - 2 * inset),
            cornerRadius = CornerRadius(10.dp.toPx()),
        )

        // The harbor along the left edge, with a pier.
        val coast = Path().apply {
            moveTo(0f, 0f)
            lineTo(0.13f * w, 0f)
            cubicTo(0.09f * w, 0.2f * h, 0.16f * w, 0.36f * h, 0.1f * w, 0.52f * h)
            cubicTo(0.05f * w, 0.66f * h, 0.14f * w, 0.82f * h, 0.07f * w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(coast, colors.water)
        drawRoundRect(
            color = colors.road,
            topLeft = Offset(0.02f * w, 0.33f * h),
            size = Size(0.12f * w, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )

        // Streets, then the two avenues on top.
        val street = 7.dp.toPx()
        for (y in streetsY) drawLine(colors.road, Offset(0.12f * w, y * h), Offset(w, y * h), street)
        for (x in streetsX) drawLine(colors.road, Offset(x * w, 0f), Offset(x * w, h), street)
        val avenue = 11.dp.toPx()
        drawLine(colors.majorRoad, Offset(0.16f * w, h), Offset(w, 0.06f * h), avenue, StrokeCap.Round)
        drawLine(colors.majorRoad, Offset(0.15f * w, 0f), Offset(0.15f * w, h), avenue)

        // You are here.
        val me = Offset(0.48f * usableWidthFraction * w, 0.36f * h)
        drawCircle(colors.location.copy(alpha = 0.18f), radius = 26.dp.toPx(), center = me)
        drawCircle(Color.White, radius = 10.dp.toPx(), center = me)
        drawCircle(colors.location, radius = 7.dp.toPx(), center = me)

        for (place in places) {
            if (place.id != selectedId) drawPin(pinPosition(place, usableWidthFraction), place.tint, large = false)
        }
        if (selected != null) {
            val position = pinPosition(selected, usableWidthFraction)
            drawPin(position, selected.tint, large = true)
            val layout = textMeasurer.measure(selected.name, labelStyle, maxLines = 1)
            val padH = 10.dp.toPx()
            val padV = 6.dp.toPx()
            val labelWidth = layout.size.width + 2 * padH
            val labelHeight = layout.size.height + 2 * padV
            val gap = 22.dp.toPx()
            val left = if (position.x + gap + labelWidth < w) position.x + gap else position.x - gap - labelWidth
            val top = position.y - 30.dp.toPx() - labelHeight / 2
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.12f),
                topLeft = Offset(left, top + 2.dp.toPx()),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelHeight / 2),
            )
            drawRoundRect(
                color = colors.labelBackground,
                topLeft = Offset(left, top),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelHeight / 2),
            )
            drawText(layout, topLeft = Offset(left + padH, top + padV))
        }
    }
}

private fun DrawScope.pinPosition(place: Place, usableWidthFraction: Float): Offset =
    Offset(place.mapX * usableWidthFraction * size.width, place.mapY * size.height)

/** A teardrop pin whose tip is at [tip]. */
private fun DrawScope.drawPin(tip: Offset, color: Color, large: Boolean) {
    val radius = (if (large) 17.dp else 12.dp).toPx()
    val center = Offset(tip.x, tip.y - radius * 1.75f)
    val body = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(center.x - radius * 0.72f, center.y + radius * 0.7f)
        lineTo(center.x + radius * 0.72f, center.y + radius * 0.7f)
        close()
    }
    drawCircle(Color.Black.copy(alpha = 0.18f), radius = radius * 0.45f, center = Offset(tip.x, tip.y + 1.dp.toPx()))
    drawPath(body, color)
    drawCircle(color, radius = radius, center = center)
    drawCircle(Color.White, radius = radius * 0.4f, center = center)
}
