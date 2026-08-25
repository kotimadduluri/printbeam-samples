package com.brewlog.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.brewlog.pos.data.SettingsRepository
import com.brewlog.pos.ui.OrderScreen
import com.brewlog.pos.ui.OrderViewModel
import com.brewlog.pos.ui.SettingsScreen
import com.brewlog.pos.ui.SettingsViewModel

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)
        setContent {
            MaterialTheme {
                Surface {
                    BrewLogApp(settingsRepository)
                }
            }
        }
    }
}

@Composable
private fun BrewLogApp(repo: SettingsRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "order") {
        composable("order") {
            val vm: OrderViewModel = viewModel(
                factory = OrderViewModel.factory(repo),
            )
            OrderScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(repo),
            )
            SettingsScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
