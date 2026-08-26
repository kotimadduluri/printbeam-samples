package com.freshcart.ui

import androidx.compose.runtime.Composable

/**
 * Returns the action to invoke when the user presses Save in the manual-entry dialog.
 *
 * Android wraps [SettingsViewModel.commitManual] with a runtime `BLUETOOTH_CONNECT` permission
 * request when the manual transport is `Transport.BLE` — otherwise the GATT connect at print
 * time throws SecurityException with no actionable UX. iOS just delegates to `commitManual`
 * directly since BLE manual entry is not surfaced there.
 */
@Composable
expect fun rememberManualSaveAction(vm: SettingsViewModel): () -> Unit

/**
 * Returns the action to invoke when the user starts a printer scan.
 *
 * Android gates [SettingsViewModel.startScan] behind the SDK's [BluetoothPermissions] runtime
 * set (SCAN+CONNECT on 12+, FINE_LOCATION before) — without it the BLE leg of the hybrid scan
 * silently finds nothing. Denial still starts the scan: network discovery needs no runtime
 * permission. iOS delegates straight through; the OS raises its Bluetooth / Local Network
 * prompts on first use, driven by the Info.plist usage strings.
 */
@Composable
expect fun rememberScanAction(vm: SettingsViewModel): () -> Unit
