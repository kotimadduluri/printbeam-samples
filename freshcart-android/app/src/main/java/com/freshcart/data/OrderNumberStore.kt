package com.freshcart.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persisted incrementing order counter. [peek] gives the number the next receipt will
 * carry; [consume] commits it once the print actually succeeded, so a failed print
 * retries with the same order number instead of burning one per attempt.
 */
class OrderNumberStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun peek(): Int = prefs.getInt(KEY_NEXT_ORDER, FIRST_ORDER)

    fun consume(): Int {
        val number = peek()
        prefs.edit().putInt(KEY_NEXT_ORDER, number + 1).apply()
        return number
    }

    companion object {
        private const val PREFS_NAME = "freshcart_orders"
        private const val KEY_NEXT_ORDER = "next_order_number"
        private const val FIRST_ORDER = 1
    }
}
