package com.freshcart.data

import dev.printbeam.PaperWidth
import dev.printbeam.Transport
import platform.Foundation.NSUserDefaults

private const val KEY_TRANSPORT = "freshcart.transport"
private const val KEY_HOST = "freshcart.host"
private const val KEY_PORT = "freshcart.port"
private const val KEY_BLE_DEVICE_ID = "freshcart.bleDeviceId"
private const val KEY_PAPER_WIDTH = "freshcart.paperWidth"
private const val KEY_PRINTER_NAME = "freshcart.printerName"
private const val KEY_MANUFACTURER = "freshcart.manufacturer"
private const val KEY_LAST_ORDER_NUMBER = "freshcart.lastOrderNumber"

actual class SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): PrinterSettings {
        val transport = defaults.stringForKey(KEY_TRANSPORT)
            ?.let { runCatching { Transport.valueOf(it) }.getOrNull() }
            ?: Transport.NETWORK
        val host = defaults.stringForKey(KEY_HOST) ?: ""
        // NSUserDefaults returns 0 for an unset integer — fall back to 9100 in that case so
        // a fresh install lands on the ESC/POS conventional port instead of an invalid 0.
        val port = defaults.integerForKey(KEY_PORT).toInt().takeIf { it != 0 } ?: 9100
        val paper = defaults.stringForKey(KEY_PAPER_WIDTH)
            ?.let { runCatching { PaperWidth.valueOf(it) }.getOrNull() }
            ?: PaperWidth.MM_58
        return PrinterSettings(
            transport = transport,
            host = host,
            port = port,
            bleDeviceId = defaults.stringForKey(KEY_BLE_DEVICE_ID),
            paperWidth = paper,
            printerName = defaults.stringForKey(KEY_PRINTER_NAME),
            manufacturer = defaults.stringForKey(KEY_MANUFACTURER),
        )
    }

    actual fun save(settings: PrinterSettings) {
        defaults.setObject(settings.transport.name, KEY_TRANSPORT)
        defaults.setObject(settings.host, KEY_HOST)
        defaults.setInteger(settings.port.toLong(), KEY_PORT)
        defaults.setObject(settings.bleDeviceId, KEY_BLE_DEVICE_ID)
        defaults.setObject(settings.paperWidth.name, KEY_PAPER_WIDTH)
        defaults.setObject(settings.printerName, KEY_PRINTER_NAME)
        defaults.setObject(settings.manufacturer, KEY_MANUFACTURER)
    }

    // An unset integer reads as 0, which is exactly the "no orders yet" baseline we want.
    actual fun nextOrderNumber(): Int = defaults.integerForKey(KEY_LAST_ORDER_NUMBER).toInt() + 1

    actual fun consumeOrderNumber(number: Int) {
        defaults.setInteger(number.toLong(), KEY_LAST_ORDER_NUMBER)
    }
}
