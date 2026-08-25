package com.freshcart.data

import android.content.Context
import dev.printbeam.PaperWidth
import dev.printbeam.Transport

private const val PREFS_NAME = "freshcart_settings"
private const val KEY_TRANSPORT = "transport"
private const val KEY_HOST = "host"
private const val KEY_PORT = "port"
private const val KEY_BLE_DEVICE_ID = "ble_device_id"
private const val KEY_PAPER_WIDTH = "paper_width"
private const val KEY_PRINTER_NAME = "printer_name"
private const val KEY_MANUFACTURER = "manufacturer"
private const val KEY_LAST_ORDER_NUMBER = "last_order_number"

actual class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun load(): PrinterSettings {
        val transport = prefs.getString(KEY_TRANSPORT, Transport.NETWORK.name)
            ?.let { runCatching { Transport.valueOf(it) }.getOrNull() }
            ?: Transport.NETWORK
        return PrinterSettings(
            transport = transport,
            host = prefs.getString(KEY_HOST, "") ?: "",
            port = prefs.getInt(KEY_PORT, 9100),
            bleDeviceId = prefs.getString(KEY_BLE_DEVICE_ID, null),
            paperWidth = prefs.getString(KEY_PAPER_WIDTH, PaperWidth.MM_58.name)
                ?.let { runCatching { PaperWidth.valueOf(it) }.getOrNull() }
                ?: PaperWidth.MM_58,
            printerName = prefs.getString(KEY_PRINTER_NAME, null),
            manufacturer = prefs.getString(KEY_MANUFACTURER, null),
        )
    }

    actual fun save(settings: PrinterSettings) {
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

    actual fun nextOrderNumber(): Int = prefs.getInt(KEY_LAST_ORDER_NUMBER, 0) + 1

    actual fun consumeOrderNumber(number: Int) {
        prefs.edit().putInt(KEY_LAST_ORDER_NUMBER, number).apply()
    }
}
