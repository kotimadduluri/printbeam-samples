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
        vm.startScan()
    }
    return {
        if (BluetoothPermissions.allGranted(context)) {
            vm.startScan()
        } else {
            launcher.launch(BluetoothPermissions.required())
        }
    }
}
