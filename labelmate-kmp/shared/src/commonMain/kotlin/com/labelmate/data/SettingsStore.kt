package com.labelmate.data

import dev.printbeam.PaperWidth
import dev.printbeam.Transport

/**
 * Persisted printer settings. Backed by SharedPreferences on Android and NSUserDefaults on iOS
 * via `expect class SettingsStore`. The store carries both transport variants — the active one
 * is determined by [transport].
 */
expect class SettingsStore {
    fun load(): PrinterSettings
    fun save(settings: PrinterSettings)
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
