package com.freshcart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshcart.data.PrinterSettings
import com.freshcart.data.ScanScope
import com.freshcart.data.SettingsStore
import dev.printbeam.PaperWidth
import dev.printbeam.PrinterEndpoint
import dev.printbeam.PrinterException
import dev.printbeam.Transport
import dev.printbeam.discovery.DiscoveredPrinter
import dev.printbeam.sdk.PrintBeam
import dev.printbeam.sdk.ScanHandle
import dev.printbeam.sdk.ScanListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val saved: PrinterSettings = PrinterSettings(),
    val scanScope: ScanScope = ScanScope.ALL,
    val isScanning: Boolean = false,
    val scanResults: List<DiscoveredPrinter> = emptyList(),
    val scanError: String? = null,
    val showScanSheet: Boolean = false,
    val showManualSheet: Boolean = false,
    val manualTransport: Transport = Transport.NETWORK,
    val manualHost: String = "",
    val manualPort: String = "9100",
    val manualBleDeviceId: String = "",
    val manualHostError: String? = null,
    val manualPortError: String? = null,
    val manualBleError: String? = null,
    val snackbarMessage: String? = null,
) {
    val isConfigured: Boolean get() = saved.isConfigured
}

/**
 * Printer settings state holder — the SDK-demo core of this sample, so PrintBeam types are
 * allowed here (scan results, transports, endpoints). All printing elsewhere goes through the
 * `ReceiptPrinter` seam instead.
 *
 * @param supportsBleManual whether manual BLE entry is exposed in the UI. True on Android
 *   (users can read a MAC off the printer label); false on iOS (CBPeripheral identifiers
 *   are OS-generated and not on any label, so manual entry is impractical there).
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    val supportsBleManual: Boolean = false,
) : ViewModel() {

    private var scanHandle: ScanHandle? = null

    private val _uiState = MutableStateFlow(
        SettingsUiState(saved = settingsStore.load(), scanScope = settingsStore.loadScanScope()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onPaperWidthChange(value: PaperWidth) {
        persist(_uiState.value.saved.copy(paperWidth = value))
    }

    fun dismissSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    /**
     * Persists the scan-scope choice. The caller (the sheet's segmented control) follows up
     * with the platform scan action so the restart goes through the permission gate — the new
     * scope may pull Bluetooth in, and only the UI layer can prompt for it.
     */
    fun onScanScopeChange(scope: ScanScope) {
        if (_uiState.value.scanScope == scope) return
        settingsStore.saveScanScope(scope)
        _uiState.update { it.copy(scanScope = scope) }
    }

    fun startScan() {
        // No in-flight guard: changing the scan scope restarts the scan, and PrintBeam
        // serializes scans anyway (a new scan cancels its predecessor).
        scanHandle?.cancel()
        _uiState.update {
            it.copy(isScanning = true, scanResults = emptyList(), scanError = null, showScanSheet = true)
        }
        // PrintBeam.scan streams: printers appear in the sheet as they respond. Callbacks
        // arrive on the main dispatcher, so they can update UI state directly.
        scanHandle = PrintBeam.scan(
            transports = _uiState.value.scanScope.toTransports(),
            listener = object : ScanListener {
                override fun onPrinterFound(printer: DiscoveredPrinter) {
                    // Key by id — a later source can re-emit the same printer with richer
                    // fields; the re-emission replaces the row rather than appending.
                    _uiState.update { s ->
                        s.copy(scanResults = s.scanResults.filterNot { it.id == printer.id } + printer)
                    }
                }

                override fun onTransportFailed(transport: Transport, cause: PrinterException) {
                    _uiState.update { it.copy(scanError = cause.message ?: "Scan failed") }
                }

                override fun onFinished(printers: List<DiscoveredPrinter>) {
                    _uiState.update { it.copy(scanResults = printers, isScanning = false) }
                }
            },
        )
    }

    fun dismissScan() {
        scanHandle?.cancel()
        scanHandle = null
        _uiState.update {
            it.copy(isScanning = false, showScanSheet = false, scanResults = emptyList(), scanError = null)
        }
    }

    fun pickDiscovered(printer: DiscoveredPrinter) {
        val saved = when (val ep = printer.endpoint) {
            is PrinterEndpoint.Network -> _uiState.value.saved.copy(
                transport = Transport.NETWORK,
                host = ep.host,
                port = ep.port,
                bleDeviceId = null,
                printerName = printer.name,
                manufacturer = printer.manufacturer,
            )
            is PrinterEndpoint.Ble -> _uiState.value.saved.copy(
                transport = Transport.BLE,
                bleDeviceId = ep.deviceId,
                host = "",
                printerName = printer.name,
                manufacturer = printer.manufacturer,
            )
        }
        persist(saved)
        resolveNameFromDevice()
        _uiState.update {
            it.copy(
                showScanSheet = false,
                scanResults = emptyList(),
                scanError = null,
                snackbarMessage = "Connected to ${printer.name ?: printer.endpoint.id}",
            )
        }
    }

    /**
     * Discovery couldn't name this printer (many network printers don't advertise mDNS at
     * all) — so ask the printer itself: `queryDeviceInfo` reads ESC/POS `GS I` identity
     * (manufacturer + model) over the facade's held session. Best-effort: printers that
     * ignore `GS I` (typical for BLE) or a dropped link keep the transport fallback label,
     * and a name the scan DID provide is never overwritten.
     */
    private fun resolveNameFromDevice() {
        val saved = _uiState.value.saved
        if (saved.printerName != null || !saved.isConfigured) return
        val endpoint = when (saved.transport) {
            Transport.NETWORK -> PrinterEndpoint.Network(host = saved.host, port = saved.port)
            Transport.BLE -> PrinterEndpoint.Ble(deviceId = saved.bleDeviceId.orEmpty())
        }
        viewModelScope.launch {
            val info = try {
                val id = PrintBeam.addManualPrinter(endpoint, null, saved.paperWidth)
                PrintBeam.queryDeviceInfo(id)
            } catch (e: PrinterException) {
                return@launch
            }
            val name = listOfNotNull(info.manufacturer, info.model)
                .joinToString(" ")
                .ifBlank { null } ?: return@launch
            // Re-check against the latest state — the user may have switched printers
            // while the query was in flight.
            val current = _uiState.value.saved
            if (current.printerName != null || current.transport != saved.transport ||
                current.host != saved.host || current.bleDeviceId != saved.bleDeviceId
            ) {
                return@launch
            }
            // Refresh the facade registry with the resolved name so a rescan in this
            // session lists the printer named, not as a bare endpoint.
            runCatching { PrintBeam.addManualPrinter(endpoint, name, saved.paperWidth) }
            persist(
                current.copy(
                    printerName = name,
                    manufacturer = info.manufacturer ?: current.manufacturer,
                ),
            )
        }
    }

    fun openManualSheet() {
        _uiState.update {
            it.copy(
                manualTransport = it.saved.transport,
                manualHost = it.saved.host,
                manualPort = it.saved.port.toString(),
                manualBleDeviceId = it.saved.bleDeviceId.orEmpty(),
                manualHostError = null,
                manualPortError = null,
                manualBleError = null,
                showManualSheet = true,
            )
        }
    }

    fun dismissManualSheet() = _uiState.update {
        it.copy(showManualSheet = false, manualHostError = null, manualPortError = null, manualBleError = null)
    }

    fun onManualTransportChange(transport: Transport) = _uiState.update {
        it.copy(manualTransport = transport, manualHostError = null, manualPortError = null, manualBleError = null)
    }

    fun onManualHostChange(value: String) = _uiState.update {
        it.copy(manualHost = value, manualHostError = null)
    }

    fun onManualPortChange(value: String) = _uiState.update {
        it.copy(manualPort = value.filter { c -> c.isDigit() }.take(5), manualPortError = null)
    }

    fun onManualBleDeviceIdChange(value: String) = _uiState.update {
        // Auto-uppercase as the user types so the field always matches canonical MAC format.
        it.copy(manualBleDeviceId = value.uppercase().take(17), manualBleError = null)
    }

    fun commitManual(): Boolean = when (_uiState.value.manualTransport) {
        Transport.NETWORK -> commitNetwork()
        Transport.BLE -> commitBle()
    }

    private fun commitNetwork(): Boolean {
        val state = _uiState.value
        val host = state.manualHost.trim()
        val parsedPort = state.manualPort.toIntOrNull()
        val hostErr = if (host.isBlank()) "Required" else null
        val portErr = when {
            parsedPort == null -> "Number required"
            parsedPort !in 1..65535 -> "1-65535"
            else -> null
        }
        if (hostErr != null || portErr != null) {
            _uiState.update { it.copy(manualHostError = hostErr, manualPortError = portErr) }
            return false
        }
        persist(
            state.saved.copy(
                transport = Transport.NETWORK,
                host = host,
                port = parsedPort!!,
                bleDeviceId = null,
                printerName = null,
                manufacturer = null,
            ),
        )
        _uiState.update { it.copy(showManualSheet = false) }
        resolveNameFromDevice()
        return true
    }

    private fun commitBle(): Boolean {
        val state = _uiState.value
        val deviceId = state.manualBleDeviceId.trim().uppercase()
        val err = when {
            deviceId.isBlank() -> "Required"
            !MAC_REGEX.matches(deviceId) -> "Must be a MAC address (XX:XX:XX:XX:XX:XX)"
            else -> null
        }
        if (err != null) {
            _uiState.update { it.copy(manualBleError = err) }
            return false
        }
        persist(
            state.saved.copy(
                transport = Transport.BLE,
                bleDeviceId = deviceId,
                host = "",
                printerName = null,
                manufacturer = null,
            ),
        )
        _uiState.update { it.copy(showManualSheet = false) }
        resolveNameFromDevice()
        return true
    }

    fun disconnect() {
        // Close the connection PrintBeam may be holding for the old printer — same endpoint
        // → same stable id, so this addresses exactly the session the print path opened.
        val saved = _uiState.value.saved
        if (saved.isConfigured) {
            val oldEndpoint = when (saved.transport) {
                Transport.NETWORK -> PrinterEndpoint.Network(host = saved.host, port = saved.port)
                Transport.BLE -> PrinterEndpoint.Ble(deviceId = saved.bleDeviceId.orEmpty())
            }
            viewModelScope.launch {
                PrintBeam.disconnect(PrintBeam.addManualPrinter(oldEndpoint))
            }
        }
        persist(PrinterSettings(paperWidth = saved.paperWidth))
    }

    override fun onCleared() {
        scanHandle?.cancel()
        scanHandle = null
    }

    private fun persist(saved: PrinterSettings) {
        settingsStore.save(saved)
        _uiState.update { it.copy(saved = saved) }
    }

    companion object {
        // Canonical colon-separated MAC: 6 hex octets.
        private val MAC_REGEX = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}\$")
    }
}
