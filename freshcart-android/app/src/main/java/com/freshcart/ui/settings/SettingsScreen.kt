package com.freshcart.ui.settings

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.PrintDisabled
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.freshcart.data.ScanScope
import com.freshcart.ui.theme.FreshCartColors
import com.freshcart.ui.theme.FreshCartShapes
import dev.printbeam.PaperWidth
import dev.printbeam.PrinterEndpoint
import dev.printbeam.Transport
import dev.printbeam.discovery.DiscoveredPrinter
import dev.printbeam.permissions.BluetoothPermissions
import kotlinx.coroutines.launch

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
    // the sheet stays open and they can retry.
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

    // BLE runtime perms (SCAN+CONNECT on 12+, FINE_LOCATION before that). Denial isn't
    // fatal: the scan still runs and finds network printers — only the BLE leg stays dark.
    val scanPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        BluetoothPermissions.notifyChanged()
        viewModel.startScan()
    }

    // Scope-aware gate: only a scan that actually covers Bluetooth is allowed to prompt.
    // A WiFi-only scan starts straight away, no permission dance.
    fun tryScan() {
        val scope = viewModel.state.value.scanScope
        if (!scope.includesBluetooth || BluetoothPermissions.allGranted(context)) {
            viewModel.startScan()
        } else {
            scanPermLauncher.launch(BluetoothPermissions.required())
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Printer", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isConnected) {
                ConnectedHero(
                    transport = state.transport,
                    name = state.printerName,
                    manufacturer = state.manufacturer,
                    host = state.host,
                    port = state.port,
                    deviceId = state.bleDeviceId,
                    onChange = ::tryScan,
                    onDisconnect = viewModel::disconnect,
                )
                PaperWidthCard(
                    selected = state.paperWidth,
                    onSelect = viewModel::onPaperWidthChange,
                )
            } else {
                EmptyHero(
                    onScan = ::tryScan,
                    onManual = viewModel::openManualSheet,
                )
            }
        }

        if (state.showScanSheet) {
            ScanSheet(
                scanning = state.scanning,
                scope = state.scanScope,
                results = state.scanResults,
                error = state.scanError,
                onScopeChange = { scope ->
                    viewModel.onScanScopeChange(scope)
                    tryScan()
                },
                onPick = viewModel::pickDiscovered,
                onRetry = ::tryScan,
                onManual = {
                    viewModel.dismissScan()
                    viewModel.openManualSheet()
                },
                onDismiss = viewModel::dismissScan,
            )
        }

        if (state.showManualSheet) {
            ManualEntrySheet(
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
                onDismiss = viewModel::dismissManualSheet,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hero cards
// ---------------------------------------------------------------------------

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
        Transport.BLE -> deviceId ?: "not set"
    }
    val transportLabel = when (transport) {
        Transport.NETWORK -> "WiFi"
        Transport.BLE -> "Bluetooth"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FreshCartColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = FreshCartShapes.Card,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                StatusBadge(
                    icon = Icons.Outlined.Check,
                    background = FreshCartColors.Accent,
                    tint = FreshCartColors.OnAccent,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "CONNECTED · $transportLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = FreshCartColors.Accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        name ?: "$transportLabel printer",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FreshCartColors.Ink,
                    )
                    Text(
                        endpointSubtitle,
                        fontSize = 14.sp,
                        color = FreshCartColors.Muted,
                    )
                    if (manufacturer != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = FreshCartColors.Accent.copy(alpha = 0.12f),
                        ) {
                            Text(
                                manufacturer,
                                fontSize = 12.sp,
                                color = FreshCartColors.Accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TintedPillButton(
                    text = "Change",
                    icon = Icons.Outlined.Refresh,
                    contentColor = FreshCartColors.Accent,
                    onClick = onChange,
                    modifier = Modifier.weight(1f),
                )
                TintedPillButton(
                    text = "Disconnect",
                    icon = Icons.Outlined.Close,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TintedPillButton(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = contentColor.copy(alpha = 0.10f),
            contentColor = contentColor,
        ),
        elevation = null,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyHero(
    onScan: () -> Unit,
    onManual: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FreshCartColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = FreshCartShapes.Card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusBadge(
                icon = Icons.Outlined.PrintDisabled,
                background = FreshCartColors.Ground,
                tint = FreshCartColors.Muted,
                size = 72.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No printer connected",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshCartColors.Ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Scan for printers on your WiFi network or nearby over Bluetooth to start printing receipts.",
                fontSize = 14.sp,
                color = FreshCartColors.Muted,
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
                Text("Enter IP address manually", fontWeight = FontWeight.Medium)
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
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun PaperWidthCard(
    selected: PaperWidth,
    onSelect: (PaperWidth) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FreshCartColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = FreshCartShapes.Card,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Paper width", style = MaterialTheme.typography.titleMedium)
            Text(
                "The width of the roll loaded in your printer.",
                style = MaterialTheme.typography.bodySmall,
                color = FreshCartColors.Muted,
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
}

// ---------------------------------------------------------------------------
// Scan sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanSheet(
    scanning: Boolean,
    scope: ScanScope,
    results: List<DiscoveredPrinter>,
    error: String?,
    onScopeChange: (ScanScope) -> Unit,
    onPick: (DiscoveredPrinter) -> Unit,
    onRetry: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val animScope = rememberCoroutineScope()

    // Play the sheet's exit animation before the state flips it out of composition.
    fun close(then: () -> Unit) {
        animScope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FreshCartColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (scanning && results.isEmpty()) "Scanning" else "Choose your printer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { close(onDismiss) }) { Text("Close") }
            }
            Spacer(Modifier.height(8.dp))

            // What kind of printer to look for. Switching restarts the scan (through the
            // permission gate, so widening to Bluetooth can prompt first).
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ScanScope.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = scope == entry,
                        onClick = { onScopeChange(entry) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ScanScope.entries.size,
                        ),
                    ) {
                        Text(
                            when (entry) {
                                ScanScope.ALL -> "All"
                                ScanScope.WIFI -> "WiFi"
                                ScanScope.BLUETOOTH -> "Bluetooth"
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.heightIn(min = 260.dp)) {
                when {
                    // Streamed results show as they arrive — even mid-scan, and even when
                    // one transport failed (a denied BLE leg must not hide WiFi finds).
                    results.isNotEmpty() -> ScanResultsList(
                        results = results,
                        onPick = { close { onPick(it) } },
                        onManual = { close(onManual) },
                    )
                    scanning -> ScanProgress(scope = scope)
                    error != null -> ScanEmptyState(
                        icon = Icons.Outlined.Warning,
                        title = "Something went wrong",
                        message = error,
                        onRetry = onRetry,
                        onManual = { close(onManual) },
                    )
                    else -> ScanEmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No printers responded",
                        message = when (scope) {
                            ScanScope.ALL ->
                                "Check that your printer is powered on and either connected to the same WiFi network as this device or in Bluetooth range."
                            ScanScope.WIFI ->
                                "Check that your printer is powered on and connected to the same WiFi network as this device."
                            ScanScope.BLUETOOTH ->
                                "Check that your printer is powered on and within Bluetooth range."
                        },
                        onRetry = onRetry,
                        onManual = { close(onManual) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanProgress(scope: ScanScope) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Looking for printers…",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when (scope) {
                ScanScope.ALL ->
                    "Make sure your printer is on and either connected to the same WiFi as this device or in Bluetooth range."
                ScanScope.WIFI ->
                    "Make sure your printer is on and connected to the same WiFi network as this device."
                ScanScope.BLUETOOTH ->
                    "Make sure your printer is on and within Bluetooth range."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = FreshCartColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ScanResultsList(
    results: List<DiscoveredPrinter>,
    onPick: (DiscoveredPrinter) -> Unit,
    onManual: () -> Unit,
) {
    Column {
        Text(
            "Tap a printer to connect",
            style = MaterialTheme.typography.bodySmall,
            color = FreshCartColors.Muted,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(results, key = { it.id }) { p ->
                DiscoveredRow(p, onPick = { onPick(p) })
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onManual) {
            Text("Don't see your printer? Enter IP manually")
        }
    }
}

@Composable
private fun DiscoveredRow(printer: DiscoveredPrinter, onPick: () -> Unit) {
    val subtitle = when (val ep = printer.endpoint) {
        is PrinterEndpoint.Network -> "${ep.host} : ${ep.port}"
        is PrinterEndpoint.Ble -> ep.deviceId
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FreshCartShapes.Card)
            .clickable(onClick = onPick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(
            icon = Icons.Outlined.Print,
            background = FreshCartColors.Accent.copy(alpha = 0.12f),
            tint = FreshCartColors.Accent,
            size = 40.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                printer.name ?: subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = FreshCartColors.Ink,
            )
            if (printer.name != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FreshCartColors.Muted,
                )
            }
            if (printer.manufacturer != null) {
                Text(
                    printer.manufacturer!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = FreshCartColors.Muted,
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = FreshCartColors.Muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ScanEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    onRetry: () -> Unit,
    onManual: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = FreshCartColors.Muted,
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = FreshCartColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try again") }
        TextButton(onClick = onManual) { Text("Enter manually") }
    }
}

// ---------------------------------------------------------------------------
// Manual entry sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntrySheet(
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val animScope = rememberCoroutineScope()

    fun cancel() {
        animScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FreshCartColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Enter printer address",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

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
                        color = FreshCartColors.Muted,
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
                        color = FreshCartColors.Muted,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { cancel() }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave) { Text("Save") }
            }
        }
    }
}
