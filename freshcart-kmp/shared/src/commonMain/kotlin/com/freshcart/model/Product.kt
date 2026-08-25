package com.freshcart.model

enum class Category(val label: String) {
    FRUITS("Fruits"),
    VEGETABLES("Vegetables"),
}

/**
 * One catalog entry. Prices are whole rupees — the catalog has no fractional prices, so
 * integer math keeps totals exact with zero currency-formatting machinery.
 */
data class Product(
    val id: String,
    val emoji: String,
    val name: String,
    val category: Category,
    val weight: String,
    val price: Int,
    val mrp: Int,
) {
    val hasDiscount: Boolean get() = price < mrp
}

data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    val lineTotal: Int get() = product.price * quantity
    val lineMrpTotal: Int get() = product.mrp * quantity
}

/** A placed order: the persisted number plus a snapshot of the cart it was printed from. */
data class Order(
    val number: Int,
    val items: List<CartItem>,
) {
    val itemsTotal: Int get() = items.sumOf { it.lineTotal }
    val savings: Int get() = items.sumOf { it.lineMrpTotal } - itemsTotal
}
