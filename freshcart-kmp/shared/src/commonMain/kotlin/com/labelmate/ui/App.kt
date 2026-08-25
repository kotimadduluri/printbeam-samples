package com.labelmate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.labelmate.data.SettingsStore

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
    MaterialTheme {
        val vm: AppViewModel = viewModel {
            AppViewModel(settingsStore, supportsBleManual)
        }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(vm.snackbarMessage) {
            val msg = vm.snackbarMessage
            if (msg != null) {
                snackbarHostState.showSnackbar(msg)
                vm.dismissSnackbar()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                when (vm.screen) {
                    Screen.Label -> LabelScreen(vm, padding)
                    Screen.Settings -> SettingsScreen(vm, padding)
                }
            }
        }
    }
}
