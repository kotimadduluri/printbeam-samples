package com.freshcart.printing

import com.freshcart.data.PrinterSettings
import com.freshcart.model.Order
import dev.printbeam.PrintResult
import dev.printbeam.PrinterEndpoint
import dev.printbeam.Transport
import dev.printbeam.escpos.Alignment
import dev.printbeam.sdk.PrintBeam

/**
 * Builds and sends the FreshCart order receipt through the [PrintBeam] facade. The facade
 * was initialized by the platform entry point, so no platform handles are needed here — and it
 * holds the connection across prints, so back-to-back orders pay the connection handshake once.
 */
class PrintBeamReceiptPrinter : ReceiptPrinter {

    override suspend fun printReceipt(settings: PrinterSettings, order: Order): ReceiptResult {
        // Same endpoint → same stable id, and re-registering just refreshes the entry, so
        // this is safe to call per print.
        val printerId = PrintBeam.addManualPrinter(
            endpoint = settings.toEndpoint(),
            name = settings.printerName,
            paperWidth = settings.paperWidth,
        )
        val result = runCatching {
            PrintBeam.print(printerId) {
                align(Alignment.CENTER)
                bold { text("FRESHCART") }
                text("Fresh groceries, printed fast")
                align(Alignment.LEFT)
                divider("-")
                order.items.forEach { item ->
                    line(
                        "${item.quantity}x ${item.product.name} ${item.product.weight}",
                        "₹${item.lineTotal}",
                    )
                }
                divider("-")
                line("Items", "₹${order.itemsTotal}")
                if (order.savings > 0) {
                    line("You saved", "₹${order.savings}")
                }
                bold { line("TOTAL", "₹${order.itemsTotal}") }
                feed(1)
                align(Alignment.CENTER)
                text("Order #${order.number.toString().padStart(3, '0')}")
                text("Thank you!")
                feed(2)
                cut()
            }
        }
        return result.fold(
            onSuccess = { r ->
                when (r) {
                    is PrintResult.Success -> ReceiptResult.Success
                    is PrintResult.Failure ->
                        ReceiptResult.Failure(r.exception.message ?: "unknown error")
                }
            },
            onFailure = { e -> ReceiptResult.Failure(e.message ?: e::class.simpleName ?: "error") },
        )
    }
}

private fun PrinterSettings.toEndpoint(): PrinterEndpoint = when (transport) {
    Transport.NETWORK -> PrinterEndpoint.Network(host = host, port = port)
    Transport.BLE -> PrinterEndpoint.Ble(deviceId = bleDeviceId.orEmpty())
}
