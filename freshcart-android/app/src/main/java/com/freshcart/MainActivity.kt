package com.freshcart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.freshcart.ui.cart.CartScreen
import com.freshcart.ui.cart.CartViewModel
import com.freshcart.ui.settings.SettingsScreen
import com.freshcart.ui.settings.SettingsViewModel
import com.freshcart.ui.shop.ShopScreen
import com.freshcart.ui.shop.ShopViewModel
import com.freshcart.ui.theme.FreshCartTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FreshCartApp).container
        setContent {
            FreshCartTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FreshCartNavHost(container)
                }
            }
        }
    }
}

@Composable
private fun FreshCartNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "shop") {
        composable("shop") {
            val vm: ShopViewModel = viewModel(
                factory = ShopViewModel.factory(container.productRepository, container.cartStore),
            )
            ShopScreen(
                viewModel = vm,
                onOpenCart = { navController.navigate("cart") },
                onOpenPrinterSettings = { navController.navigate("settings") },
            )
        }
        composable("cart") {
            val vm: CartViewModel = viewModel(
                factory = CartViewModel.factory(
                    container.cartStore,
                    container.orderNumberStore,
                    container.receiptPrinter,
                ),
            )
            CartScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenPrinterSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(container.settingsRepository),
            )
            SettingsScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
