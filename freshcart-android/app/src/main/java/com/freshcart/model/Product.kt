package com.freshcart.model

enum class Category(val label: String) {
    FRUITS("Fruits"),
    VEGETABLES("Vegetables"),
}

/** One catalog entry. Prices are whole rupees — the catalog has no paise anywhere. */
data class Product(
    val id: String,
    val emoji: String,
    val name: String,
    val category: Category,
    val weight: String,
    val price: Int,
    val mrp: Int,
) {
    val discounted: Boolean get() = mrp > price
}

data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    val lineTotal: Int get() = product.price * quantity
    val lineSaved: Int get() = (product.mrp - product.price) * quantity
}

fun formatRupees(amount: Int): String = "₹$amount"
