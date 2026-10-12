package io.github.halilozel1903.sheets.sample

import androidx.compose.ui.graphics.Color

/** A fictional coffee place on the map. [mapX] and [mapY] are fractions of the map's size. */
data class Place(
    val id: Int,
    val name: String,
    val category: String,
    val rating: Float,
    val reviews: Int,
    val distance: String,
    val open: Boolean,
    val hours: String,
    val tint: Color,
    val mapX: Float,
    val mapY: Float,
)

const val AREA_NAME = "Harbor Square"

val places: List<Place> = listOf(
    Place(1, "Lighthouse Roasters", "Coffee roaster", 4.8f, 412, "120 m", true, "Closes 18:00", Color(0xFF8F4E22), 0.52f, 0.30f),
    Place(2, "Tidewater Coffee Bar", "Café", 4.6f, 238, "210 m", true, "Closes 20:00", Color(0xFF2B5BD7), 0.64f, 0.22f),
    Place(3, "The Long Pier Espresso Collective & Neighbourhood Bakery", "Café · Bakery", 4.5f, 97, "350 m", true, "Closes 17:00", Color(0xFF00897B), 0.34f, 0.40f),
    Place(4, "Saltbox Café", "Café", 4.4f, 156, "400 m", true, "Closes 19:00", Color(0xFF7B5EA7), 0.74f, 0.36f),
    Place(5, "Ferry Street Beans", "Coffee shop", 4.7f, 301, "450 m", true, "Closes 18:30", Color(0xFFC2185B), 0.44f, 0.16f),
    Place(6, "Anchor & Oak", "Coffee shop", 4.3f, 88, "520 m", false, "Opens 07:00", Color(0xFF5D6B2E), 0.22f, 0.26f),
    Place(7, "Gull's Nest Kiosk", "Kiosk", 4.1f, 54, "600 m", true, "Closes 16:00", Color(0xFF0277BD), 0.86f, 0.18f),
    Place(8, "Northlight Tea & Coffee", "Tea house", 4.6f, 190, "650 m", true, "Closes 21:00", Color(0xFF6D4C41), 0.60f, 0.48f),
    Place(9, "Marina Steam", "Café", 4.2f, 67, "720 m", false, "Opens 08:00", Color(0xFF455A64), 0.16f, 0.52f),
    Place(10, "Driftwood Brew Lab", "Coffee roaster", 4.5f, 143, "800 m", true, "Closes 18:00", Color(0xFFAD5A00), 0.80f, 0.56f),
    Place(11, "Copper Kettle", "Café", 4.0f, 39, "870 m", true, "Closes 15:00", Color(0xFF8D6E63), 0.30f, 0.62f),
    Place(12, "Harbor Mill Bakery", "Bakery · Coffee", 4.7f, 520, "950 m", true, "Closes 19:30", Color(0xFFD84315), 0.70f, 0.68f),
)

data class Review(val author: String, val rating: Int, val text: String, val tint: Color)

val reviews: List<Review> = listOf(
    Review("Maya R.", 5, "The single origin pour-over is the best in the area, and they roast on Thursdays so the whole street smells amazing.", Color(0xFF2B5BD7)),
    Review("Jonas K.", 4, "Busy at lunch but the line moves fast. Window seats look straight at the lighthouse.", Color(0xFF00897B)),
    Review("Priya S.", 5, "Friendly baristas, great oat flat white, and the cardamom bun sells out by ten.", Color(0xFFC2185B)),
)

/** The comment the keyboard scene shows as already typed. */
const val SAMPLE_COMMENT = "Great flat white and a quiet corner by the window. The oat milk cortado is worth the short wait."
