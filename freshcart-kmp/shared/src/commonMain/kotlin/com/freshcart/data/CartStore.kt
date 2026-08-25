package com.freshcart.data

import com.freshcart.model.CartItem
import com.freshcart.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for the cart, shared by the Shop and Cart screens. In-memory on
 * purpose — a sample cart doesn't need to survive a process death.
 */
class CartStore {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items

    fun add(product: Product) {
        _items.update { items ->
            val existing = items.find { it.product.id == product.id }
            if (existing == null) {
                items + CartItem(product, quantity = 1)
            } else {
                items.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            }
        }
    }

    fun increment(productId: String) {
        _items.update { items ->
            items.map { if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    /** Decrementing at quantity 1 removes the line entirely. */
    fun decrement(productId: String) {
        _items.update { items ->
            items.mapNotNull {
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
