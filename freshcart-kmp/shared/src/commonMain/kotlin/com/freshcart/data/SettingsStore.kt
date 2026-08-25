package com.freshcart.data

import dev.printbeam.PaperWidth
import dev.printbeam.Transport

/**
 * Persisted printer settings plus the order-number counter. Backed by SharedPreferences on
 * Android and NSUserDefaults on iOS via `expect class SettingsStore`. The store carries both
 * transport variants — the active one is determined by [PrinterSettings.transport].
 *
 * Order numbers are peek/consume: [nextOrderNumber] tells you the number the next order will
 * carry (the receipt needs it before printing), and [consumeOrderNumber] persists it only
 * once the print actually succeeded — a failed print retries with the same number.
 */
expect class SettingsStore {
    fun load(): PrinterSettings
    fun save(settings: PrinterSettings)
    fun nextOrderNumber(): Int
    fun consumeOrderNumber(number: Int)
}

data class PrinterSettings(
    val transport: Transport = Transport.NETWORK,
    val host: String = "",
    val port: Int = 9100,
    val bleDeviceId: String? = null,
    val paperWidth: PaperWidth = PaperWidth.MM_58,
    val printerName: String? = null,
    val manufacturer: String? = null,
) {
    val isConfigured: Boolean
        get() = when (transport) {
            Transport.NETWORK -> host.isNotBlank()
            Transport.BLE -> !bleDeviceId.isNullOrBlank()
        }
}
