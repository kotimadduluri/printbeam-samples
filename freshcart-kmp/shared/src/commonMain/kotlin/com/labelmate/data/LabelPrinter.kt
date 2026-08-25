package com.labelmate.data

import dev.printbeam.PrintResult
import dev.printbeam.PrinterEndpoint
import dev.printbeam.Transport
import dev.printbeam.escpos.Alignment
import dev.printbeam.escpos.BarcodeType
import dev.printbeam.sdk.PrintBeam

/**
 * Builds and sends the LabelMate price-label layout through the [PrintBeam] facade. The facade
 * was initialized by the platform entry point, so no platform handles are needed here — and it
 * holds the connection across prints, so printing a batch of labels pays the BLE handshake once.
 */
class LabelPrinter {
    suspend fun printLabel(
        settings: PrinterSettings,
        name: String,
        price: String,
        ean13: String,
    ): PrintResult {
        // Same endpoint → same stable id, and re-registering just refreshes the entry, so
        // this is safe to call per print.
        val printerId = PrintBeam.addManualPrinter(
            endpoint = settings.toEndpoint(),
            name = settings.printerName,
            paperWidth = settings.paperWidth,
        )
        return PrintBeam.print(printerId) {
            align(Alignment.CENTER)
            bold { text(name) }
            feed(1)
            size(2, 2) { text(price) }
            feed(1)
            barcode(ean13, BarcodeType.EAN13, height = 80, moduleWidth = 3)
            feed(2)
            cut()
        }
    }
}

private fun PrinterSettings.toEndpoint(): PrinterEndpoint = when (transport) {
    Transport.NETWORK -> PrinterEndpoint.Network(host = host, port = port)
    Transport.BLE -> PrinterEndpoint.Ble(deviceId = bleDeviceId.orEmpty())
}

/**
 * EAN-13 client-side validation. The library re-checks (and throws), but failing fast in the UI
 * gives the user a clearer message than a `PrinterException.InvalidInput` round-trip.
 */
fun isValidEan13(input: String): Boolean {
    if (input.length != 12 && input.length != 13) return false
    return input.all { it in '0'..'9' }
}
