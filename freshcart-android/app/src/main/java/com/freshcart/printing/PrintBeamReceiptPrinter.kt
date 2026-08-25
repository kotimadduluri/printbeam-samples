package com.freshcart.printing

import com.freshcart.data.PrinterSettings
import com.freshcart.data.SettingsRepository
import com.freshcart.model.CartItem
import com.freshcart.model.formatRupees
import dev.printbeam.PrintResult
import dev.printbeam.PrinterEndpoint
import dev.printbeam.PrinterException
import dev.printbeam.Transport
import dev.printbeam.escpos.Alignment
import dev.printbeam.sdk.PrintBeam

/**
 * ReceiptPrinter backed by the PrintBeam facade. This is the only place outside the
 * settings/scan feature that touches the SDK.
 */
class PrintBeamReceiptPrinter(
    private val settings: SettingsRepository,
) : ReceiptPrinter {

    override val isConfigured: Boolean
        get() = settings.load().isConfigured

    override suspend fun printOrder(orderNumber: Int, items: List<CartItem>): PrintOutcome {
        val cfg = settings.load()
        if (!cfg.isConfigured) {
            return PrintOutcome.Failure("No printer is configured. Open Settings first.")
        }

        val itemsTotal = items.sumOf { it.lineTotal }
        val saved = items.sumOf { it.lineSaved }

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
                bold { text("FRESHCART") }
                text("Fresh groceries, printed fast")
                divider()
                align(Alignment.LEFT)
                for (item in items) {
                    line(
                        "${item.quantity}x ${item.product.name} ${item.product.weight}",
                        formatRupees(item.lineTotal),
                    )
                }
                divider()
                line("Items", formatRupees(itemsTotal))
                if (saved > 0) {
                    line("You saved", formatRupees(saved))
                }
                bold { line("TOTAL", formatRupees(itemsTotal)) }
                feed(1)
                align(Alignment.CENTER)
                text("Order #${formatOrderNumber(orderNumber)}")
                text("Thank you!")
                feed(2)
                cut()
            }
        } catch (e: PrinterException) {
            // print() only throws for invalid receipt content or an unknown id —
            // transport failures come back as PrintResult.Failure.
            PrintResult.Failure(e)
        }

        return when (result) {
            is PrintResult.Success -> PrintOutcome.Success
            is PrintResult.Failure ->
                PrintOutcome.Failure(result.exception.message ?: "Print failed")
        }
    }
}

/** "42" prints as "042" — three digits until the counter grows past 999. */
fun formatOrderNumber(number: Int): String = number.toString().padStart(3, '0')

private fun PrinterSettings.toEndpoint(): PrinterEndpoint = when (transport) {
    Transport.NETWORK -> PrinterEndpoint.Network(host = host, port = port)
    Transport.BLE -> PrinterEndpoint.Ble(deviceId = bleDeviceId.orEmpty())
}
