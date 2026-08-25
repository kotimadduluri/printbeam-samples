package com.freshcart.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.freshcart.data.CartStore
import com.freshcart.data.ProductRepository
import com.freshcart.model.Category
import com.freshcart.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sort chip cycles off → low-to-high → high-to-low → off. */
enum class SortMode { OFF, PRICE_LOW_HIGH, PRICE_HIGH_LOW }

/** Category chip cycles All → Fruits → Vegetables → All. */
enum class CategoryFilter(val category: Category?) {
    ALL(null),
    FRUITS(Category.FRUITS),
    VEGETABLES(Category.VEGETABLES),
}

data class ShopUiState(
    val allProducts: List<Product> = emptyList(),
    val query: String = "",
    val sort: SortMode = SortMode.OFF,
    val category: CategoryFilter = CategoryFilter.ALL,
    val offersOnly: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val cartQuantities: Map<String, Int> = emptyMap(),
) {
    val cartCount: Int get() = cartQuantities.values.sum()

    val visibleProducts: List<Product>
        get() {
            val filtered = allProducts.filter { product ->
                (query.isBlank() || product.name.contains(query.trim(), ignoreCase = true)) &&
                    (category.category == null || product.category == category.category) &&
                    (!offersOnly || product.discounted)
            }
            return when (sort) {
                SortMode.OFF -> filtered
                SortMode.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
                SortMode.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
            }
        }
}

class ShopViewModel(
    productRepository: ProductRepository,
    private val cartStore: CartStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopUiState(allProducts = productRepository.products()))
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cartStore.items.collect { items ->
                _state.update { s ->
                    s.copy(cartQuantities = items.associate { it.product.id to it.quantity })
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun clearSearch() {
        _state.update { it.copy(query = "") }
    }

    fun cycleSort() {
        _state.update {
            it.copy(
                sort = when (it.sort) {
                    SortMode.OFF -> SortMode.PRICE_LOW_HIGH
                    SortMode.PRICE_LOW_HIGH -> SortMode.PRICE_HIGH_LOW
                    SortMode.PRICE_HIGH_LOW -> SortMode.OFF
                },
            )
        }
    }

    fun cycleCategory() {
        _state.update {
            it.copy(
                category = when (it.category) {
                    CategoryFilter.ALL -> CategoryFilter.FRUITS
                    CategoryFilter.FRUITS -> CategoryFilter.VEGETABLES
                    CategoryFilter.VEGETABLES -> CategoryFilter.ALL
                },
            )
        }
    }

    fun toggleOffers() {
        _state.update { it.copy(offersOnly = !it.offersOnly) }
    }

    fun toggleFavorite(productId: String) {
        _state.update {
            it.copy(
                favorites = if (productId in it.favorites) it.favorites - productId
                else it.favorites + productId,
            )
        }
    }

    /** Both the "+" state and the count state of the add button increment. */
    fun addToCart(product: Product) {
        cartStore.add(product)
    }

    companion object {
        fun factory(
            productRepository: ProductRepository,
            cartStore: CartStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ShopViewModel(productRepository, cartStore) as T
        }
    }
}
