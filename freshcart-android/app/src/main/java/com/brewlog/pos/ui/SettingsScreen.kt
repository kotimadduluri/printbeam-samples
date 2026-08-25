package com.brewlog.pos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.PrintDisabled
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.printbeam.PaperWidth
import dev.printbeam.PrinterEndpoint
import dev.printbeam.Transport
import dev.printbeam.discovery.DiscoveredPrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // BLUETOOTH_CONNECT runtime perm is needed before we can connect to a manually-typed
    // device id on Android 12+. If user denies, we surface that as a manual-field error so
    // the dialog stays open and they can retry.
    val blePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.commitManual()
    }

    fun trySaveManual() {
        val needsBlePerm = state.manualTransport == Transport.BLE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsBlePerm) {
            blePermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            viewModel.commitManual()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (state.isConnected) {
                ConnectedHero(
                    transport = state.transport,
                    name = state.printerName,
                    manufacturer = state.manufacturer,
                    host = state.host,
                    port = state.port,
                    deviceId = state.bleDeviceId,
                    onChange = viewModel::startScan,
                    onDisconnect = viewModel::disconnect,
                )
                PaperWidthSection(
                    selected = state.paperWidth,
                    onSelect = viewModel::onPaperWidthChange,
                )
            } else {
                EmptyHero(
                    onScan = viewModel::startScan,
                    onManual = viewModel::openManualDialog,
                )
            }
        }

        if (state.showScanDialog) {
            ScanDialog(
                scanning = state.scanning,
                results = state.scanResults,
                error = state.scanError,
                onPick = viewModel::pickDiscovered,
                onRetry = viewModel::startScan,
                onManual = {
                    viewModel.dismissScan()
                    viewModel.openManualDialog()
                },
                onDismiss = viewModel::dismissScan,
            )
        }

        if (state.showManualDialog) {
            ManualDialog(
                transport = state.manualTransport,
                host = state.manualHost,
                port = state.manualPort,
                bleDeviceId = state.manualBleDeviceId,
                hostError = state.manualHostError,
                portError = state.manualPortError,
                bleError = state.manualBleError,
                onTransportChange = viewModel::onManualTransportChange,
                onHostChange = viewModel::onManualHostChange,
                onPortChange = viewModel::onManualPortChange,
                onBleDeviceIdChange = viewModel::onManualBleDeviceIdChange,
                onSave = { trySaveManual() },
                onDismiss = viewModel::dismissManualDialog,
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
        Transport.BLE -> deviceId ?: "—"
    }
    val transportLabel = when (transport) {
        Transport.NETWORK -> "WiFi"
        Transport.BLE -> "Bluetooth"
    }
    val transportIcon = when (transport) {
        Transport.NETWORK -> Icons.Outlined.Wifi
        Transport.BLE -> Icons.Outlined.Bluetooth
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(
                    icon = transportIcon,
                    background = MaterialTheme.colorScheme.primary,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "CONNECTED · $transportLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        name ?: defaultDisplayNameFor(transport),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        endpointSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                    if (manufacturer != null) {
                        Spacer(Modifier.height(4.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text(manufacturer) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onChange,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Change")
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.PrintDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Disconnect")
                }
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
    onScan: () -> Unit,
    onManual: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusBadge(
                icon = Icons.Outlined.PrintDisabled,
                background = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 64.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No printer connected",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Find a printer on your WiFi network, or enter the address of a network or Bluetooth printer manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Find Printer")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onManual) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Enter address manually")
            }
        }
    }
}

@Composable
private fun StatusBadge(
    icon: ImageVector,
    background: Color,
    tint: Color,
    size: Dp = 48.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun PaperWidthSection(
    selected: PaperWidth,
    onSelect: (PaperWidth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Paper width", style = MaterialTheme.typography.titleMedium)
        Text(
            "The width of the roll loaded in your printer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selected == PaperWidth.MM_58,
                onClick = { onSelect(PaperWidth.MM_58) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("58 mm") }
            SegmentedButton(
                selected = selected == PaperWidth.MM_80,
                onClick = { onSelect(PaperWidth.MM_80) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("80 mm") }
        }
    }
}

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Make sure your printer is on and connected to the same WiFi.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error != null -> EmptyStateColumn(
                    icon = Icons.Outlined.Warning,
                    title = "Something went wrong",
                    subtitle = error,
                )
                results.isEmpty() -> EmptyStateColumn(
                    icon = Icons.Outlined.SearchOff,
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
                TextButton(onClick = onRetry) { Text("Try again") }
            }
        },
        dismissButton = {
            Row {
                if (!scanning) {
                    TextButton(onClick = onManual) { Text("Enter manually") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
private fun EmptyStateColumn(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DiscoveredCard(printer: DiscoveredPrinter, onPick: () -> Unit) {
    val endpoint = printer.endpoint
    val (subtitle, icon) = when (endpoint) {
        is PrinterEndpoint.Network -> "${endpoint.host} : ${endpoint.port}" to Icons.Outlined.Wifi
        is PrinterEndpoint.Ble -> endpoint.deviceId to Icons.Outlined.Bluetooth
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBadge(
                icon = icon,
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                tint = MaterialTheme.colorScheme.primary,
                size = 40.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    printer.name ?: subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (printer.name != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (printer.manufacturer != null) {
                    Text(
                        printer.manufacturer!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualDialog(
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
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = transport == Transport.NETWORK,
                        onClick = { onTransportChange(Transport.NETWORK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(Icons.Outlined.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    ) { Text("WiFi") }
                    SegmentedButton(
                        selected = transport == Transport.BLE,
                        onClick = { onTransportChange(Transport.BLE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(Icons.Outlined.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    ) { Text("Bluetooth") }
                }

                when (transport) {
                    Transport.NETWORK -> {
                        Text(
                            "Type the printer's network address. You'll find this on the printer's display or a printed self-test page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
