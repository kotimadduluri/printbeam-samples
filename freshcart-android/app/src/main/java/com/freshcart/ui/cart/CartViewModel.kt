package com.freshcart.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.freshcart.data.CartStore
import com.freshcart.data.OrderNumberStore
import com.freshcart.model.CartItem
import com.freshcart.printing.PrintOutcome
import com.freshcart.printing.ReceiptPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the Place Order flow currently stands. */
sealed interface OrderPhase {
    data object Idle : OrderPhase
    data object Printing : OrderPhase
    data class Success(val orderNumber: Int) : OrderPhase
    data class Failure(val message: String) : OrderPhase
}

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val phase: OrderPhase = OrderPhase.Idle,
    /** One-shot: Place Order was tapped with no printer configured. */
    val openPrinterSettings: Boolean = false,
) {
    val itemsTotal: Int get() = items.sumOf { it.lineTotal }
    val saved: Int get() = items.sumOf { it.lineSaved }
    val total: Int get() = itemsTotal
}

class CartViewModel(
    private val cartStore: CartStore,
    private val orderNumbers: OrderNumberStore,
    private val printer: ReceiptPrinter,
) : ViewModel() {

    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cartStore.items.collect { items ->
                _state.update { s ->
                    // Editing the cart invalidates a stale failure message; a shown
                    // success state stays until "Back to shop".
                    val phase = if (s.phase is OrderPhase.Failure) OrderPhase.Idle else s.phase
                    s.copy(items = items, phase = phase)
                }
            }
        }
    }

    fun increment(productId: String) = cartStore.increment(productId)

    fun decrement(productId: String) = cartStore.decrement(productId)

    fun placeOrder() {
        val current = _state.value
        if (current.items.isEmpty() || current.phase is OrderPhase.Printing) return

        if (!printer.isConfigured) {
            _state.update { it.copy(openPrinterSettings = true) }
            return
        }

        _state.update { it.copy(phase = OrderPhase.Printing) }
        viewModelScope.launch {
            // Peek, don't consume: a failed print retries under the same order number.
            val orderNumber = orderNumbers.peek()
            when (val outcome = printer.printOrder(orderNumber, current.items)) {
                is PrintOutcome.Success -> {
                    orderNumbers.consume()
                    cartStore.clear()
                    _state.update { it.copy(phase = OrderPhase.Success(orderNumber)) }
                }
                is PrintOutcome.Failure -> {
                    // Cart stays untouched so the user can retry the exact same order.
                    _state.update { it.copy(phase = OrderPhase.Failure(outcome.message)) }
                }
            }
        }
    }

    fun retry() = placeOrder()

    /** Leaving the success state (or the screen) resets the flow for the next order. */
    fun resetPhase() {
        _state.update { it.copy(phase = OrderPhase.Idle) }
    }

    fun consumeOpenPrinterSettings() {
        _state.update { it.copy(openPrinterSettings = false) }
    }

    companion object {
        fun factory(
            cartStore: CartStore,
            orderNumbers: OrderNumberStore,
            printer: ReceiptPrinter,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CartViewModel(cartStore, orderNumbers, printer) as T
        }
    }
}
