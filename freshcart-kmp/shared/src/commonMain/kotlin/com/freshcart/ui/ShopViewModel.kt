package com.freshcart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshcart.data.CartStore
import com.freshcart.data.ProductRepository
import com.freshcart.model.CartItem
import com.freshcart.model.Category
import com.freshcart.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class SortOrder { OFF, PRICE_LOW_HIGH, PRICE_HIGH_LOW }

data class ShopUiState(
    val query: String = "",
    val sort: SortOrder = SortOrder.OFF,
    val category: Category? = null,
    val offersOnly: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val products: List<Product> = emptyList(),
    val cartQuantities: Map<String, Int> = emptyMap(),
) {
    val cartCount: Int get() = cartQuantities.values.sum()
}

/**
 * Shop screen state holder. Filter inputs live in one private flow; the visible grid is a
 * pure function of those inputs plus the shared [CartStore], so the add-button counts and
 * the cart badge can never drift from what the Cart screen shows.
 */
class ShopViewModel(
    repository: ProductRepository,
    private val cartStore: CartStore,
) : ViewModel() {

    private data class Filters(
        val query: String = "",
        val sort: SortOrder = SortOrder.OFF,
        val category: Category? = null,
        val offersOnly: Boolean = false,
        val favorites: Set<String> = emptySet(),
    )

    private val catalog = repository.products()
    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<ShopUiState> =
        combine(filters, cartStore.items) { f, cart -> buildState(f, cart) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = buildState(Filters(), cartStore.items.value),
            )

    fun onQueryChange(value: String) = filters.update { it.copy(query = value) }

    fun clearSearch() = filters.update { it.copy(query = "") }

    /** Sort chip: off → price low→high → price high→low → off. */
    fun cycleSort() = filters.update {
        it.copy(
            sort = when (it.sort) {
                SortOrder.OFF -> SortOrder.PRICE_LOW_HIGH
                SortOrder.PRICE_LOW_HIGH -> SortOrder.PRICE_HIGH_LOW
                SortOrder.PRICE_HIGH_LOW -> SortOrder.OFF
            },
        )
    }

    /** Category chip: All → Fruits → Vegetables → All. */
    fun cycleCategory() = filters.update {
        it.copy(
            category = when (it.category) {
                null -> Category.FRUITS
                Category.FRUITS -> Category.VEGETABLES
                Category.VEGETABLES -> null
            },
        )
    }

    fun toggleOffers() = filters.update { it.copy(offersOnly = !it.offersOnly) }

    fun toggleFavorite(productId: String) = filters.update {
        val favorites = if (productId in it.favorites) it.favorites - productId else it.favorites + productId
        it.copy(favorites = favorites)
    }

    fun addToCart(product: Product) = cartStore.add(product)

    private fun buildState(f: Filters, cart: List<CartItem>): ShopUiState {
        val filtered = catalog
            .filter { f.query.isBlank() || it.name.contains(f.query.trim(), ignoreCase = true) }
            .filter { f.category == null || it.category == f.category }
            .filter { !f.offersOnly || it.hasDiscount }
        val sorted = when (f.sort) {
            SortOrder.OFF -> filtered
            SortOrder.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
            SortOrder.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
        }
        return ShopUiState(
            query = f.query,
            sort = f.sort,
            category = f.category,
            offersOnly = f.offersOnly,
            favorites = f.favorites,
            products = sorted,
            cartQuantities = cart.associate { it.product.id to it.quantity },
        )
    }
}
