package com.labelmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LabelScreen(vm: AppViewModel, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("LabelMate", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Print retail price labels to your thermal printer.")

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = vm.productName,
            onValueChange = vm::onProductNameChange,
            label = { Text("Product name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = vm.price,
            onValueChange = vm::onPriceChange,
            label = { Text("Price (e.g. \$9.99)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = vm.ean13,
            onValueChange = vm::onEan13Change,
            label = { Text("EAN-13 (12 or 13 digits)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = vm::printLabel,
            enabled = !vm.isPrinting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (vm.isPrinting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Print Label")
            }
        }

        OutlinedButton(
            onClick = { vm.navigate(Screen.Settings) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Settings")
        }

        // Show the configured target so users can confirm before printing without leaving the screen.
        val endpointDescription = when (vm.savedTransport) {
            dev.printbeam.Transport.NETWORK ->
                vm.savedHost.takeIf { it.isNotBlank() }?.let { "$it:${vm.savedPort}" }
            dev.printbeam.Transport.BLE -> vm.savedBleDeviceId
        }
        val label = vm.selectedPrinterName ?: endpointDescription ?: "Not connected"
        Text(
            text = "Printer: $label • ${vm.paperWidth.name}",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
    }
}
