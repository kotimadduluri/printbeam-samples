package com.freshcart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshcart.data.ScanScope
import com.freshcart.ui.theme.FreshShapes
import com.freshcart.ui.theme.FreshTokens
import dev.printbeam.PaperWidth
import dev.printbeam.PrinterEndpoint
import dev.printbeam.Transport
import dev.printbeam.discovery.DiscoveredPrinter

// The one red in the app — Disconnect and field errors only, matching the theme error slot.
private val DangerRed = Color(0xFFB3261E)

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val saveManual = rememberManualSaveAction(vm)
    val startScan = rememberScanAction(vm)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isConfigured) {
                ConnectedHero(
                    transport = state.saved.transport,
                    name = state.saved.printerName,
                    manufacturer = state.saved.manufacturer,
                    host = state.saved.host,
                    port = state.saved.port,
                    deviceId = state.saved.bleDeviceId,
                    onChange = startScan,
                    onDisconnect = vm::disconnect,
                )
                PaperWidthCard(
                    selected = state.saved.paperWidth,
                    onSelect = vm::onPaperWidthChange,
                )
            } else {
                EmptyHero(
                    supportsBleManual = vm.supportsBleManual,
                    onScan = startScan,
                    onManual = vm::openManualSheet,
                )
            }
        }

        if (state.showScanSheet) {
            ScanSheet(
                scope = state.scanScope,
                scanning = state.isScanning,
                results = state.scanResults,
                error = state.scanError,
                onScopeChange = { scope ->
                    // Persist first, then restart through the platform action so a scope that
                    // now pulls Bluetooth in goes through the permission gate.
                    vm.onScanScopeChange(scope)
                    startScan()
                },
                onPick = vm::pickDiscovered,
                onRetry = startScan,
                onManual = {
                    vm.dismissScan()
                    vm.openManualSheet()
                },
                onDismiss = vm::dismissScan,
            )
        }

        if (state.showManualSheet) {
            ManualSheet(
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
                onDismiss = vm::dismissManualSheet,
            )
        }
    }
}

// Hero cards -------------------------------------------------------------------------------

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
        Transport.NETWORK -> "Network"
        Transport.BLE -> "Bluetooth"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FreshShapes.Card,
        color = FreshTokens.Surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                CheckBadge(background = FreshTokens.Accent, tint = FreshTokens.OnAccent)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "CONNECTED · $transportLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = FreshTokens.Accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        name ?: "$transportLabel printer",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FreshTokens.Ink,
                    )
                    Text(endpointSubtitle, fontSize = 14.sp, color = FreshTokens.Muted)
                    if (manufacturer != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            manufacturer,
                            fontSize = 12.sp,
                            color = FreshTokens.Accent,
                            modifier = Modifier
                                .clip(FreshShapes.Pill)
                                .background(FreshTokens.Accent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TintedPillButton(
                    label = "Change",
                    tint = FreshTokens.Accent,
                    containerAlpha = 0.10f,
                    onClick = onChange,
                    modifier = Modifier.weight(1f),
                )
                TintedPillButton(
                    label = "Disconnect",
                    tint = DangerRed,
                    containerAlpha = 0.08f,
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TintedPillButton(
    label: String,
    tint: Color,
    containerAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = FreshShapes.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = tint.copy(alpha = containerAlpha),
            contentColor = tint,
        ),
        elevation = null,
        modifier = modifier,
    ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
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
            CircleGlyphBadge(size = 72.dp, background = FreshTokens.Ground) {
                PrinterIcon(color = FreshTokens.Muted, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "No printer connected",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshTokens.Ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Scan for printers on your network or nearby over Bluetooth to start printing receipts.",
                fontSize = 14.sp,
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
            ) {
                MagnifierIcon(color = FreshTokens.OnAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Find Printer", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onManual) {
                Text(
                    if (supportsBleManual) "Enter address manually" else "Enter IP address manually",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FreshTokens.Accent,
                )
            }
        }
    }
}

@Composable
private fun CircleGlyphBadge(
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// Paper width ------------------------------------------------------------------------------

@Composable
private fun PaperWidthCard(
    selected: PaperWidth,
    onSelect: (PaperWidth) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FreshShapes.Card,
        color = FreshTokens.Surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Paper width",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshTokens.Ink,
            )
            Text(
                "The width of the roll loaded in your printer.",
                fontSize = 12.sp,
                color = FreshTokens.Muted,
            )
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selected == PaperWidth.MM_58,
                    onClick = { onSelect(PaperWidth.MM_58) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = segmentColors(),
                ) { Text("58 mm") }
                SegmentedButton(
                    selected = selected == PaperWidth.MM_80,
                    onClick = { onSelect(PaperWidth.MM_80) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = segmentColors(),
                ) { Text("80 mm") }
            }
        }
    }
}

@Composable
private fun segmentColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = FreshTokens.Accent,
    activeContentColor = FreshTokens.OnAccent,
    inactiveContainerColor = FreshTokens.Surface,
    inactiveContentColor = FreshTokens.Ink,
)

// Scan sheet -------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanSheet(
    scope: ScanScope,
    scanning: Boolean,
    results: List<DiscoveredPrinter>,
    error: String?,
    onScopeChange: (ScanScope) -> Unit,
    onPick: (DiscoveredPrinter) -> Unit,
    onRetry: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Full height, no drag handle, no Close button — swipe down (or back) dismisses,
    // which is the Material-native gesture; chrome on top of that is just noise.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FreshTokens.Surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        ) {
            Text(
                if (scanning) "Scanning" else "Choose your printer",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshTokens.Ink,
            )
            Spacer(Modifier.height(4.dp))
            ScanScopeSelector(scope = scope, onScopeChange = onScopeChange)
            Spacer(Modifier.height(12.dp))
            // The states region takes all remaining sheet height, so spinner/empty states
            // sit centered in a stable, full-height sheet instead of jumping around.
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    scanning -> ScanningState(scope = scope)
                    error != null -> ScanEmptyState(
                        badgeTint = Color(0xFFB8860B),
                        glyph = "!",
                        title = "Something went wrong",
                        message = error,
                        onRetry = onRetry,
                        onManual = onManual,
                    )
                    results.isEmpty() -> ScanEmptyState(
                        badgeTint = FreshTokens.Muted,
                        glyph = null,
                        title = "No printers responded",
                        message = noResultsMessage(scope),
                        onRetry = onRetry,
                        onManual = onManual,
                    )
                    else -> ScanResultsList(results = results, onPick = onPick, onManual = onManual)
                }
            }
        }
    }
}

@Composable
private fun ScanScopeSelector(scope: ScanScope, onScopeChange: (ScanScope) -> Unit) {
    val entries = listOf(
        ScanScope.ALL to "All",
        ScanScope.NETWORK to "Network",
        ScanScope.BLUETOOTH to "Bluetooth",
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = scope == value,
                onClick = { onScopeChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = entries.size),
                colors = segmentColors(),
            ) { Text(label) }
        }
    }
}

@Composable
private fun ScanningState(scope: ScanScope) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = FreshTokens.Accent, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Looking for printers…",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = FreshTokens.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            scanGuidance(scope),
            fontSize = 14.sp,
            color = FreshTokens.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScanResultsList(
    results: List<DiscoveredPrinter>,
    onPick: (DiscoveredPrinter) -> Unit,
    onManual: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Text(
                "Tap a printer to connect",
                fontSize = 13.sp,
                color = FreshTokens.Muted,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(results, key = { it.id }) { p ->
            DiscoveredRow(printer = p, onPick = { onPick(p) })
        }
        item(key = "footer") {
            TextButton(onClick = onManual, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "Don't see your printer? Enter IP manually",
                    fontSize = 14.sp,
                    color = FreshTokens.Accent,
                )
            }
        }
    }
}

@Composable
private fun DiscoveredRow(printer: DiscoveredPrinter, onPick: () -> Unit) {
    val subtitle = when (val endpoint = printer.endpoint) {
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
            CircleGlyphBadge(size = 40.dp, background = FreshTokens.Accent.copy(alpha = 0.12f)) {
                PrinterIcon(color = FreshTokens.Accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    printer.name ?: subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = FreshTokens.Ink,
                )
                if (printer.name != null) {
                    Text(subtitle, fontSize = 12.sp, color = FreshTokens.Muted)
                }
                printer.manufacturer?.let { mfr ->
                    Text(mfr, fontSize = 11.sp, color = FreshTokens.Muted)
                }
            }
            Spacer(Modifier.width(8.dp))
            ChevronIcon(color = FreshTokens.Muted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ScanEmptyState(
    badgeTint: Color,
    glyph: String?,
    title: String,
    message: String,
    onRetry: () -> Unit,
    onManual: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircleGlyphBadge(size = 56.dp, background = badgeTint.copy(alpha = 0.12f)) {
            if (glyph != null) {
                Text(glyph, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = badgeTint)
            } else {
                PrinterIcon(color = badgeTint, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = FreshTokens.Ink)
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 14.sp, color = FreshTokens.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = FreshShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = FreshTokens.Accent,
                contentColor = FreshTokens.OnAccent,
            ),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) { Text("Try again", fontWeight = FontWeight.SemiBold) }
        TextButton(onClick = onManual) {
            Text("Enter manually", fontSize = 14.sp, color = FreshTokens.Accent)
        }
    }
}

private fun scanGuidance(scope: ScanScope): String = when (scope) {
    ScanScope.ALL ->
        "Make sure your printer is on and either connected to the same network as this device or in Bluetooth range."
    ScanScope.NETWORK ->
        "Make sure your printer is on and connected to the same network as this device."
    ScanScope.BLUETOOTH ->
        "Make sure your printer is on and in Bluetooth range."
}

private fun noResultsMessage(scope: ScanScope): String = when (scope) {
    ScanScope.ALL ->
        "Check that your printer is powered on and either connected to the same network as this device or in Bluetooth range."
    ScanScope.NETWORK ->
        "Check that your printer is powered on and connected to the same network as this device."
    ScanScope.BLUETOOTH ->
        "Check that your printer is powered on and in Bluetooth range."
}

// Manual entry sheet -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualSheet(
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FreshTokens.Surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = FreshTokens.Accent) }
                Text(
                    if (supportsBleManual) "Enter printer address" else "Enter IP address",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FreshTokens.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSave) {
                    Text("Save", fontWeight = FontWeight.SemiBold, color = FreshTokens.Accent)
                }
            }

            if (supportsBleManual) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = transport == Transport.NETWORK,
                        onClick = { onTransportChange(Transport.NETWORK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = segmentColors(),
                    ) { Text("Network") }
                    SegmentedButton(
                        selected = transport == Transport.BLE,
                        onClick = { onTransportChange(Transport.BLE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = segmentColors(),
                    ) { Text("Bluetooth") }
                }
            }

            when (transport) {
                Transport.NETWORK -> {
                    Text(
                        "Type the printer's network address. You'll find this on the printer's display or a printed self-test page.",
                        fontSize = 12.sp,
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
                        fontSize = 12.sp,
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
            Spacer(Modifier.height(4.dp))
        }
    }
}
