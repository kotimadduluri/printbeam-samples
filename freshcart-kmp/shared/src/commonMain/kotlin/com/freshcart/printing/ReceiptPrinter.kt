package com.freshcart.printing

import com.freshcart.data.PrinterSettings
import com.freshcart.model.Order

/**
 * The printing seam. ViewModels and screens depend on this interface only — the sole place
 * PrintBeam types appear outside the printer-settings feature is [PrintBeamReceiptPrinter].
 */
interface ReceiptPrinter {
    suspend fun printReceipt(settings: PrinterSettings, order: Order): ReceiptResult
}

/** SDK-neutral outcome so callers never handle PrintBeam's own result types. */
sealed interface ReceiptResult {
    data object Success : ReceiptResult
    data class Failure(val message: String) : ReceiptResult
}
