package com.freshcart

import android.app.Application
import android.content.Context
import com.freshcart.data.CartStore
import com.freshcart.data.InMemoryProductRepository
import com.freshcart.data.OrderNumberStore
import com.freshcart.data.ProductRepository
import com.freshcart.data.SettingsRepository
import com.freshcart.printing.PrintBeamReceiptPrinter
import com.freshcart.printing.ReceiptPrinter
import dev.printbeam.discovery.PrinterContext
import dev.printbeam.sdk.PrintBeam
import dev.printbeam.sdk.PrintBeamConfig

/**
 * Initializes the PrintBeam facade once for the whole process, then wires the app-scoped
 * dependency container. Every screen talks to [PrintBeam] through the printing seam —
 * no printer objects or platform handles threaded through ViewModels.
 */
class FreshCartApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PrintBeam.initialize(PrintBeamConfig(context = PrinterContext(this)))
        container = AppContainer(this)
    }
}

/**
 * Manual constructor injection, one instance per process. Deliberately no DI framework —
 * five dependencies don't need one.
 */
class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val productRepository: ProductRepository = InMemoryProductRepository()
    val cartStore = CartStore()
    val orderNumberStore = OrderNumberStore(context)
    val receiptPrinter: ReceiptPrinter = PrintBeamReceiptPrinter(settingsRepository)
}
