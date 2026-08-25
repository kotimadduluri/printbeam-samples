package com.brewlog.pos.data

/** Fixed café menu — five items, no DB. Prices in cents to avoid floating-point drift. */
data class MenuItem(val name: String, val priceCents: Int) {
    val priceLabel: String get() = formatPrice(priceCents)
}

object Menu {
    val items: List<MenuItem> = listOf(
        MenuItem("Latte", 450),
        MenuItem("Cappuccino", 425),
        MenuItem("Espresso", 300),
        MenuItem("Croissant", 325),
        MenuItem("Muffin", 350),
    )
}

fun formatPrice(cents: Int): String {
    val dollars = cents / 100
    val remainder = cents % 100
    return "$" + dollars + "." + remainder.toString().padStart(2, '0')
}
