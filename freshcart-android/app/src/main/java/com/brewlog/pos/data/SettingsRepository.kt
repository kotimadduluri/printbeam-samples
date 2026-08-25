package com.brewlog.pos.data

import android.content.Context
import android.content.SharedPreferences
import dev.printbeam.PaperWidth
import dev.printbeam.Transport

/**
 * Thin wrapper over SharedPreferences for the printer settings the user can configure.
 * The store carries both transport variants (NETWORK host/port, BLE deviceId) — the
 * active one is determined by [PrinterSettings.transport].
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PrinterSettings = PrinterSettings(
        transport = readTransport(),
        host = prefs.getString(KEY_HOST, "") ?: "",
        port = prefs.getInt(KEY_PORT, DEFAULT_PORT),
        bleDeviceId = prefs.getString(KEY_BLE_DEVICE_ID, null),
        paperWidth = readPaperWidth(),
        printerName = prefs.getString(KEY_PRINTER_NAME, null),
        manufacturer = prefs.getString(KEY_MANUFACTURER, null),
    )

    fun save(settings: PrinterSettings) {
        prefs.edit()
            .putString(KEY_TRANSPORT, settings.transport.name)
            .putString(KEY_HOST, settings.host)
            .putInt(KEY_PORT, settings.port)
            .putString(KEY_BLE_DEVICE_ID, settings.bleDeviceId)
            .putString(KEY_PAPER_WIDTH, settings.paperWidth.name)
            .putString(KEY_PRINTER_NAME, settings.printerName)
            .putString(KEY_MANUFACTURER, settings.manufacturer)
            .apply()
    }

    private fun readTransport(): Transport {
        val raw = prefs.getString(KEY_TRANSPORT, Transport.NETWORK.name)
        return runCatching { Transport.valueOf(raw ?: Transport.NETWORK.name) }
            .getOrDefault(Transport.NETWORK)
    }

    private fun readPaperWidth(): PaperWidth {
        val raw = prefs.getString(KEY_PAPER_WIDTH, PaperWidth.MM_80.name)
        return runCatching { PaperWidth.valueOf(raw ?: PaperWidth.MM_80.name) }
            .getOrDefault(PaperWidth.MM_80)
    }

    companion object {
        private const val PREFS_NAME = "brewlog_printer_settings"
        private const val KEY_TRANSPORT = "transport"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_BLE_DEVICE_ID = "ble_device_id"
        private const val KEY_PAPER_WIDTH = "paper_width"
        private const val KEY_PRINTER_NAME = "printer_name"
        private const val KEY_MANUFACTURER = "manufacturer"
        const val DEFAULT_PORT = 9100
    }
}

data class PrinterSettings(
    val transport: Transport = Transport.NETWORK,
    val host: String = "",
    val port: Int = 9100,
    val bleDeviceId: String? = null,
    val paperWidth: PaperWidth = PaperWidth.MM_80,
    val printerName: String? = null,
    val manufacturer: String? = null,
) {
    val isConfigured: Boolean
        get() = when (transport) {
            Transport.NETWORK -> host.isNotBlank()
            Transport.BLE -> !bleDeviceId.isNullOrBlank()
        }
}
