package com.freshcart.printing

import com.freshcart.model.CartItem

/** Outcome of a receipt print, expressed without any PrintBeam types leaking upward. */
sealed interface PrintOutcome {
    data object Success : PrintOutcome
    data class Failure(val message: String) : PrintOutcome
}

/**
 * The printing seam. ViewModels depend on this interface only — the PrintBeam SDK is an
 * implementation detail behind [PrintBeamReceiptPrinter], so screens never import
 * dev.printbeam.* types.
 */
interface ReceiptPrinter {

    /** True once the user has picked or typed a printer in Settings. */
    val isConfigured: Boolean

    /** Prints the FreshCart receipt for [items] under [orderNumber]. Never throws. */
    suspend fun printOrder(orderNumber: Int, items: List<CartItem>): PrintOutcome
}
