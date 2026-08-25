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
