package com.freshcart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshcart.ui.theme.FreshShapes
import com.freshcart.ui.theme.FreshTokens
import dev.printbeam.PaperWidth
import dev.printbeam.PrinterEndpoint
import dev.printbeam.Transport
import dev.printbeam.discovery.DiscoveredPrinter

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val saveManual = rememberManualSaveAction(vm)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.dismissSnackbar()
        }
    }

    Scaffold(
        containerColor = FreshTokens.Ground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("Back", color = FreshTokens.Accent) }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Printer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FreshTokens.Ink,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.isConfigured) {
                ConnectedHero(
                    transport = state.saved.transport,
                    name = state.saved.printerName,
                    manufacturer = state.saved.manufacturer,
                    host = state.saved.host,
                    port = state.saved.port,
                    deviceId = state.saved.bleDeviceId,
                    onChange = vm::startScan,
                    onDisconnect = vm::disconnect,
                )
                PaperWidthSection(
                    selected = state.saved.paperWidth,
                    onSelect = vm::onPaperWidthChange,
                )
            } else {
                EmptyHero(
                    supportsBleManual = vm.supportsBleManual,
                    onScan = vm::startScan,
                    onManual = vm::openManualDialog,
                )
            }
        }

        if (state.showScanDialog) {
            ScanDialog(
                scanning = state.isScanning,
                results = state.scanResults,
                error = state.scanError,
                onPick = vm::pickDiscovered,
                onRetry = vm::startScan,
                onManual = {
                    vm.dismissScan()
                    vm.openManualDialog()
                },
                onDismiss = vm::dismissScan,
            )
        }

        if (state.showManualDialog) {
            ManualDialog(
                supportsBleManual = vm.supportsBleManual,
                transport = state.manualTransport,
                host = state.manualHost,
                port = state.manualPort,
                bleDeviceId = state.manualBleDeviceId,
                hostError = state.manualHostError,
                portError = state.manualPortError,
                bleError = state.manualBleError,
                onTransportChange = vm::onManualTransportChange,
                onHostChange = vm::onManualHostChange,
                onPortChange = vm::onManualPortChange,
                onBleDeviceIdChange = vm::onManualBleDeviceIdChange,
                onSave = saveManual,
                onDismiss = vm::dismissManualDialog,
            )
        }
    }
}

@Composable
private fun ConnectedHero(
    transport: Transport,
    name: String?,
    manufacturer: String?,
    host: String,
    port: Int,
    deviceId: String?,
    onChange: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val endpointSubtitle = when (transport) {
        Transport.NETWORK -> "$host : $port"
        Transport.BLE -> deviceId ?: "-"
    }
    val transportLabel = when (transport) {
        Transport.NETWORK -> "WiFi"
        Transport.BLE -> "Bluetooth"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FreshShapes.Card,
        color = FreshTokens.Surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckBadge(
                    background = FreshTokens.Accent,
                    tint = FreshTokens.OnAccent,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "CONNECTED · $transportLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = FreshTokens.Accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        name ?: defaultDisplayNameFor(transport),
                        style = MaterialTheme.typography.titleLarge,
                        color = FreshTokens.Ink,
                    )
                    Text(
                        endpointSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FreshTokens.Muted,
                    )
                    if (manufacturer != null) {
                        Text(
                            manufacturer,
                            style = MaterialTheme.typography.labelSmall,
                            color = FreshTokens.Muted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onChange,
                    shape = FreshShapes.Pill,
                    modifier = Modifier.weight(1f),
                ) { Text("Change", color = FreshTokens.Accent) }
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = FreshShapes.Pill,
                    modifier = Modifier.weight(1f),
                ) { Text("Disconnect", color = FreshTokens.Accent) }
            }
        }
    }
}

private fun defaultDisplayNameFor(transport: Transport): String = when (transport) {
    Transport.NETWORK -> "Manual network printer"
    Transport.BLE -> "Manual Bluetooth printer"
}

@Composable
private fun EmptyHero(
    supportsBleManual: Boolean,
    onScan: () -> Unit,
    onManual: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FreshShapes.Card,
        color = FreshTokens.Surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyBadge(glyph = "?")
            Spacer(Modifier.height(16.dp))
            Text("No printer connected", style = MaterialTheme.typography.titleLarge, color = FreshTokens.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                if (supportsBleManual)
                    "Find a printer on your WiFi network, or enter the address of a network or Bluetooth printer manually."
                else
                    "Find a printer on your WiFi network, or enter its IP address manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = FreshTokens.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onScan,
                shape = FreshShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshTokens.Accent,
                    contentColor = FreshTokens.OnAccent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("Find Printer", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onManual) {
                Text(
                    if (supportsBleManual) "Enter address manually" else "Enter IP address manually",
                    color = FreshTokens.Accent,
                )
            }
        }
    }
}

@Composable
private fun PaperWidthSection(
    selected: PaperWidth,
    onSelect: (PaperWidth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Paper width", style = MaterialTheme.typography.titleMedium, color = FreshTokens.Ink)
        Text(
            "The width of the roll loaded in your printer.",
            style = MaterialTheme.typography.bodySmall,
            color = FreshTokens.Muted,
        )
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selected == PaperWidth.MM_58,
                onClick = { onSelect(PaperWidth.MM_58) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = paperWidthColors(),
            ) { Text("58 mm") }
            SegmentedButton(
                selected = selected == PaperWidth.MM_80,
                onClick = { onSelect(PaperWidth.MM_80) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = paperWidthColors(),
            ) { Text("80 mm") }
        }
    }
}

@Composable
private fun paperWidthColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = FreshTokens.Accent,
    activeContentColor = FreshTokens.OnAccent,
    inactiveContainerColor = FreshTokens.Surface,
    inactiveContentColor = FreshTokens.Ink,
)

@Composable
private fun ScanDialog(
    scanning: Boolean,
    results: List<DiscoveredPrinter>,
    error: String?,
    onPick: (DiscoveredPrinter) -> Unit,
    onRetry: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    scanning -> "Looking for printers…"
                    error != null -> "Scan failed"
                    results.isEmpty() -> "No printers found"
                    else -> "Choose your printer"
                },
            )
        },
        text = {
            when {
                scanning -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FreshTokens.Accent)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Make sure your printer is on and connected to the same WiFi.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = FreshTokens.Muted,
                    )
                }
                error != null -> EmptyStateColumn(
                    title = "Something went wrong",
                    subtitle = error,
                )
                results.isEmpty() -> EmptyStateColumn(
                    title = "No printers responded",
                    subtitle = "Check that your printer is powered on and connected to the same WiFi network as this device.",
                )
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id }) { p ->
                        DiscoveredCard(p, onPick = { onPick(p) })
                    }
                }
            }
        },
        confirmButton = {
            if (!scanning && (error != null || results.isEmpty())) {
                TextButton(onClick = onRetry) { Text("Try again", color = FreshTokens.Accent) }
            }
        },
        dismissButton = {
            Row {
                if (!scanning) {
                    TextButton(onClick = onManual) { Text("Enter manually", color = FreshTokens.Accent) }
                }
                TextButton(onClick = onDismiss) { Text("Close", color = FreshTokens.Accent) }
            }
        },
    )
}

@Composable
private fun EmptyStateColumn(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = FreshTokens.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DiscoveredCard(printer: DiscoveredPrinter, onPick: () -> Unit) {
    val endpoint = printer.endpoint
    val subtitle = when (endpoint) {
        is PrinterEndpoint.Network -> "${endpoint.host} : ${endpoint.port}"
        is PrinterEndpoint.Ble -> endpoint.deviceId
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FreshShapes.Card)
            .clickable(onClick = onPick),
        color = FreshTokens.Ground,
        shape = FreshShapes.Card,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FreshTokens.Accent.copy(alpha = 0.12f)),
            ) {
                CheckBadge(
                    background = androidx.compose.ui.graphics.Color.Transparent,
                    tint = FreshTokens.Accent,
                    size = 40.dp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(printer.name ?: subtitle, style = MaterialTheme.typography.bodyLarge, color = FreshTokens.Ink)
                if (printer.name != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FreshTokens.Muted,
                    )
                }
                printer.manufacturer?.let { mfr ->
                    Text(
                        mfr,
                        style = MaterialTheme.typography.labelSmall,
                        color = FreshTokens.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualDialog(
    supportsBleManual: Boolean,
    transport: Transport,
    host: String,
    port: String,
    bleDeviceId: String,
    hostError: String?,
    portError: String?,
    bleError: String?,
    onTransportChange: (Transport) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onBleDeviceIdChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter printer address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (supportsBleManual) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = transport == Transport.NETWORK,
                            onClick = { onTransportChange(Transport.NETWORK) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = paperWidthColors(),
                        ) { Text("WiFi") }
                        SegmentedButton(
                            selected = transport == Transport.BLE,
                            onClick = { onTransportChange(Transport.BLE) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = paperWidthColors(),
                        ) { Text("Bluetooth") }
                    }
                }

                when (transport) {
                    Transport.NETWORK -> {
                        Text(
                            "Type the printer's network address. You'll find this on the printer's display or a printed self-test page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FreshTokens.Muted,
                        )
                        OutlinedTextField(
                            value = host,
                            onValueChange = onHostChange,
                            label = { Text("IP address") },
                            placeholder = { Text("192.168.1.100") },
                            singleLine = true,
                            isError = hostError != null,
                            supportingText = { hostError?.let { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = onPortChange,
                            label = { Text("Port") },
                            placeholder = { Text("9100") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = portError != null,
                            supportingText = { portError?.let { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Transport.BLE -> {
                        Text(
                            "Type the printer's Bluetooth MAC address. You'll find it on the device's barcode label or in the manufacturer's app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FreshTokens.Muted,
                        )
                        OutlinedTextField(
                            value = bleDeviceId,
                            onValueChange = onBleDeviceIdChange,
                            label = { Text("MAC address") },
                            placeholder = { Text("60:6E:41:01:46:B2") },
                            singleLine = true,
                            isError = bleError != null,
                            supportingText = {
                                Text(bleError ?: "Bluetooth permission will be requested on Save.")
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = FreshShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshTokens.Accent,
                    contentColor = FreshTokens.OnAccent,
                ),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FreshTokens.Accent) }
        },
    )
}
