package com.brewlog.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brewlog.pos.data.Menu
import com.brewlog.pos.data.MenuItem
import com.brewlog.pos.data.PrinterSettings
import com.brewlog.pos.data.SettingsRepository
import com.brewlog.pos.data.formatPrice
import dev.printbeam.PrintResult
import dev.printbeam.PrinterEndpoint
import dev.printbeam.PrinterException
import dev.printbeam.Transport
import dev.printbeam.escpos.Alignment
import dev.printbeam.sdk.PrintBeam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderUiState(
    val items: List<MenuItem> = emptyList(),
    val totalCents: Int = 0,
    val printing: Boolean = false,
) {
    val totalLabel: String get() = formatPrice(totalCents)
}

/** Transient one-shot UI messages — Snackbar shows then clears. */
sealed interface OrderEvent {
    data class PrintSucceeded(val message: String) : OrderEvent
    data class PrintFailed(val message: String) : OrderEvent
    data class ConfigError(val message: String) : OrderEvent
}

class OrderViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrderUiState())
    val state: StateFlow<OrderUiState> = _state.asStateFlow()

    private val _events = MutableStateFlow<OrderEvent?>(null)
    val events: StateFlow<OrderEvent?> = _events.asStateFlow()

    fun addItem(item: MenuItem) {
        _state.update { current ->
            val next = current.items + item
            current.copy(items = next, totalCents = next.sumOf { it.priceCents })
        }
    }

    fun clear() {
        _state.update { it.copy(items = emptyList(), totalCents = 0) }
    }

    fun consumeEvent() {
        _events.value = null
    }

    fun printReceipt() {
        val current = _state.value
        if (current.items.isEmpty() || current.printing) return

        val cfg = settings.load()
        if (!cfg.isConfigured) {
            _events.value = OrderEvent.ConfigError("No printer is configured. Open Settings first.")
            return
        }

        _state.update { it.copy(printing = true) }
        viewModelScope.launch {
            val result = try {
                // Registering the same endpoint again just replaces the entry, so this is
                // safe per print: the id is endpoint-derived and stable, and the connection
                // PrintBeam holds for it survives across prints — no reconnect per receipt.
                val printerId = PrintBeam.addManualPrinter(
                    endpoint = cfg.toEndpoint(),
                    name = cfg.printerName,
                    paperWidth = cfg.paperWidth,
                )
                PrintBeam.print(printerId) {
                    align(Alignment.CENTER)
                    bold { text("BREWLOG CAFÉ") }
                    text("123 Bean Street")
                    divider()
                    align(Alignment.LEFT)
                    for (item in current.items) {
                        line(item.name, item.priceLabel)
                    }
                    divider()
                    align(Alignment.RIGHT)
                    bold { line("TOTAL", current.totalLabel) }
                    feed(2)
                    align(Alignment.CENTER)
                    text("Thank you!")
                    cut()
                }
            } catch (e: PrinterException) {
                // print() only throws for invalid receipt content or an unknown id —
                // transport failures come back as PrintResult.Failure.
                PrintResult.Failure(e)
            }
            _events.value = when (result) {
                is PrintResult.Success ->
                    OrderEvent.PrintSucceeded("Receipt sent to printer")
                is PrintResult.Failure ->
                    OrderEvent.PrintFailed(result.exception.message ?: "Print failed")
            }
            _state.update { it.copy(printing = false) }
        }
    }

    companion object {
        fun factory(settings: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OrderViewModel(settings) as T
            }
    }
}

private fun PrinterSettings.toEndpoint(): PrinterEndpoint = when (transport) {
    Transport.NETWORK -> PrinterEndpoint.Network(host = host, port = port)
    Transport.BLE -> PrinterEndpoint.Ble(deviceId = bleDeviceId.orEmpty())
}
