package com.freshcart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshcart.data.CartStore
import com.freshcart.data.SettingsStore
import com.freshcart.model.CartItem
import com.freshcart.model.Order
import com.freshcart.printing.ReceiptPrinter
import com.freshcart.printing.ReceiptResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OrderStage {
    data object Idle : OrderStage
    data object Printing : OrderStage
    data class Success(val orderNumber: Int) : OrderStage
    data class Failure(val message: String) : OrderStage
}

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val stage: OrderStage = OrderStage.Idle,
) {
    val itemsTotal: Int get() = items.sumOf { it.lineTotal }
    val savings: Int get() = items.sumOf { it.lineMrpTotal } - itemsTotal
}

/**
 * Cart screen state holder. Printing goes through the [ReceiptPrinter] seam — no PrintBeam
 * types here. The order number is peeked before the print (the receipt carries it) and only
 * consumed on success, so a failed print retries with the same number.
 */
class CartViewModel(
    private val cartStore: CartStore,
    private val settingsStore: SettingsStore,
    private val receiptPrinter: ReceiptPrinter,
) : ViewModel() {

    private val stage = MutableStateFlow<OrderStage>(OrderStage.Idle)

    val uiState: StateFlow<CartUiState> =
        combine(cartStore.items, stage) { items, s -> CartUiState(items = items, stage = s) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CartUiState(items = cartStore.items.value),
            )

    fun increment(productId: String) = cartStore.increment(productId)

    fun decrement(productId: String) = cartStore.decrement(productId)

    /**
     * Kicks off the order. When no printer is configured yet, [onPrinterNeeded] runs instead
     * (the screen navigates to Printer settings) and nothing else happens.
     */
    fun placeOrder(onPrinterNeeded: () -> Unit) {
        if (stage.value is OrderStage.Printing) return
        val items = cartStore.items.value
        if (items.isEmpty()) return
        val settings = settingsStore.load()
        if (!settings.isConfigured) {
            onPrinterNeeded()
            return
        }

        val order = Order(number = settingsStore.nextOrderNumber(), items = items)
        stage.value = OrderStage.Printing
        viewModelScope.launch {
            when (val result = receiptPrinter.printReceipt(settings, order)) {
                is ReceiptResult.Success -> {
                    settingsStore.consumeOrderNumber(order.number)
                    cartStore.clear()
                    stage.value = OrderStage.Success(order.number)
                }
                is ReceiptResult.Failure -> {
                    // Cart stays intact so the user can retry or keep shopping.
                    stage.value = OrderStage.Failure(result.message)
                }
            }
        }
    }

    fun dismissError() = stage.update { if (it is OrderStage.Failure) OrderStage.Idle else it }

    /** Leaves the success state, e.g. when heading back to the shop. */
    fun resetStage() {
        stage.value = OrderStage.Idle
    }
}
