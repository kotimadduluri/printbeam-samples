package com.freshcart.data

import com.freshcart.model.CartItem
import com.freshcart.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for the cart. In-memory on purpose — the cart does not survive
 * a process restart, and that's fine for a printer-SDK sample.
 */
class CartStore {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    fun add(product: Product) {
        _items.update { current ->
            val existing = current.find { it.product.id == product.id }
            if (existing == null) {
                current + CartItem(product, quantity = 1)
            } else {
                current.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            }
        }
    }

    fun increment(productId: String) {
        _items.update { current ->
            current.map { if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    /** Decrement removes the line entirely once quantity would drop below 1. */
    fun decrement(productId: String) {
        _items.update { current ->
            current.mapNotNull {
                when {
                    it.product.id != productId -> it
                    it.quantity > 1 -> it.copy(quantity = it.quantity - 1)
                    else -> null
                }
            }
        }
    }

    fun clear() {
        _items.value = emptyList()
    }
}
