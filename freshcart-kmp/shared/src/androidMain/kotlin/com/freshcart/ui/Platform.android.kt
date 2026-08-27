package com.freshcart.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.printbeam.Transport
import dev.printbeam.permissions.BluetoothPermissions

@Composable
actual fun rememberManualSaveAction(vm: SettingsViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) vm.commitManual() }
    return {
        val needsBlePerm = vm.uiState.value.manualTransport == Transport.BLE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsBlePerm) {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            vm.commitManual()
        }
    }
}

@Composable
actual fun rememberScanAction(vm: SettingsViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        BluetoothPermissions.notifyChanged()
        // Start regardless of the outcome — a denial degrades the scan to network-only
        // (the SDK reports the BLE leg via onTransportFailed), it never blocks it.
        vm.startScan()
    }
    return {
        // Only a scope that includes Bluetooth needs the runtime permissions; a network-only
        // scan must never prompt. The scope is read at invocation time, not composition
        // time, so a scope change immediately before this call is honored.
        val needsBlePerms = vm.uiState.value.scanScope.includesBluetooth &&
            !BluetoothPermissions.allGranted(context)
        if (needsBlePerms) {
            launcher.launch(BluetoothPermissions.required())
        } else {
            vm.startScan()
        }
    }
}
