package com.freshcart.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freshcart.data.CartStore
import com.freshcart.data.InMemoryProductRepository
import com.freshcart.data.ProductRepository
import com.freshcart.data.SettingsStore
import com.freshcart.printing.PrintBeamReceiptPrinter
import com.freshcart.printing.ReceiptPrinter
import com.freshcart.ui.theme.FreshTheme

enum class Screen { Shop, Cart, Settings }

/**
 * App-scoped dependency container — plain constructor injection, no framework. Created once
 * per process-lifetime of the composition; the ViewModels capture these instances at creation
 * and keep them across Android configuration changes.
 */
class AppContainer(val settingsStore: SettingsStore) {
    val productRepository: ProductRepository = InMemoryProductRepository()
    val cartStore: CartStore = CartStore()
    val receiptPrinter: ReceiptPrinter = PrintBeamReceiptPrinter()
}

/**
 * Root composable. Accepts a [SettingsStore] from the platform host; printing itself goes
 * through the `PrintBeam` facade, which the platform entry point already initialized — no
 * printer plumbing crosses this boundary.
 *
 * @param supportsBleManual surfaces the BLE option in the manual-entry dialog. Android passes
 *   true (MAC addresses are user-visible on printer labels); iOS passes false (CBPeripheral
 *   identifiers are OS-generated and not on any label).
 */
@Composable
fun App(
    settingsStore: SettingsStore,
    supportsBleManual: Boolean = false,
) {
    FreshTheme {
        val container = remember { AppContainer(settingsStore) }
        val shopVm: ShopViewModel = viewModel {
            ShopViewModel(container.productRepository, container.cartStore)
        }
        val cartVm: CartViewModel = viewModel {
            CartViewModel(container.cartStore, container.settingsStore, container.receiptPrinter)
        }
        val settingsVm: SettingsViewModel = viewModel {
            SettingsViewModel(container.settingsStore, supportsBleManual)
        }

        // Three screens, one back stack shape — a nav library would be more ceremony than
        // navigation here. Saved as names so both survive recreation. Settings is reachable
        // from Shop (top bar) and Cart (Place Order without a printer), so back returns to
        // whichever screen opened it.
        var screenName by rememberSaveable { mutableStateOf(Screen.Shop.name) }
        var settingsReturnName by rememberSaveable { mutableStateOf(Screen.Shop.name) }
        val screen = Screen.valueOf(screenName)
        fun openSettings(from: Screen) {
            settingsReturnName = from.name
            screenName = Screen.Settings.name
        }

        when (screen) {
            Screen.Shop -> ShopScreen(
                vm = shopVm,
                onOpenCart = { screenName = Screen.Cart.name },
                onOpenSettings = { openSettings(from = Screen.Shop) },
            )
            Screen.Cart -> CartScreen(
                vm = cartVm,
                onBack = { screenName = Screen.Shop.name },
                onOpenSettings = { openSettings(from = Screen.Cart) },
            )
            Screen.Settings -> SettingsScreen(
                vm = settingsVm,
                onBack = { screenName = settingsReturnName },
            )
        }
    }
}
