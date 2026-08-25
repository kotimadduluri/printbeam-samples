package com.freshcart.data

import com.freshcart.model.Category
import com.freshcart.model.Product

interface ProductRepository {
    fun products(): List<Product>
}

/**
 * The fixed 8-product FreshCart catalog. A sample doesn't need a backend; the interface is
 * here so a real data source can slot in without touching the ViewModels.
 */
class InMemoryProductRepository : ProductRepository {
    override fun products(): List<Product> = CATALOG

    private companion object {
        val CATALOG = listOf(
            Product("oranges", "🍊", "Sweet Oranges", Category.FRUITS, "300g", price = 30, mrp = 40),
            Product("apples", "🍎", "Fresh Apples", Category.FRUITS, "300g", price = 75, mrp = 90),
            Product("bananas", "🍌", "Ripe Bananas", Category.FRUITS, "500g", price = 45, mrp = 55),
            Product("grapes", "🍇", "Green Grapes", Category.FRUITS, "500g", price = 65, mrp = 80),
            Product("tomatoes", "🍅", "Fresh Tomatoes", Category.VEGETABLES, "300g", price = 20, mrp = 30),
            Product("lettuce", "🥬", "Organic Lettuce", Category.VEGETABLES, "300g", price = 25, mrp = 30),
            Product("spinach", "🌿", "Baby Spinach", Category.VEGETABLES, "250g", price = 35, mrp = 45),
            Product("corn", "🌽", "Sweet Corn", Category.VEGETABLES, "2 pc", price = 40, mrp = 50),
        )
    }
}
