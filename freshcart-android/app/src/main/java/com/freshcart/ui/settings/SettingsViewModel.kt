package com.freshcart.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.freshcart.data.PrinterSettings
import com.freshcart.data.ScanScope
import com.freshcart.data.SettingsRepository
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val transport: Transport = Transport.NETWORK,
    val host: String = "",
    val port: Int = 9100,
    val bleDeviceId: String? = null,
    val paperWidth: PaperWidth = PaperWidth.MM_80,
    val printerName: String? = null,
    val manufacturer: String? = null,
    val scanScope: ScanScope = ScanScope.ALL,
    val scanning: Boolean = false,
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
) {
    val isConnected: Boolean
        get() = when (transport) {
            Transport.NETWORK -> host.isNotBlank()
            Transport.BLE -> !bleDeviceId.isNullOrBlank()
        }
}

class SettingsViewModel(
    private val repo: SettingsRepository,
) : ViewModel() {

    private var scanHandle: ScanHandle? = null

    private val _state: MutableStateFlow<SettingsUiState>
    val state: StateFlow<SettingsUiState>

    init {
        val loaded = repo.load()
        _state = MutableStateFlow(
            SettingsUiState(
                transport = loaded.transport,
                host = loaded.host,
                port = loaded.port,
                bleDeviceId = loaded.bleDeviceId,
                paperWidth = loaded.paperWidth,
                printerName = loaded.printerName,
                manufacturer = loaded.manufacturer,
                scanScope = repo.loadScanScope(),
            ),
        )
        state = _state.asStateFlow()
    }

    fun onPaperWidthChange(width: PaperWidth) {
        val s = _state.value
        _state.value = s.copy(paperWidth = width)
        persist()
    }

    fun onScanScopeChange(scope: ScanScope) {
        if (_state.value.scanScope == scope) return
        _state.value = _state.value.copy(scanScope = scope)
        repo.saveScanScope(scope)
        // The caller (SettingsScreen) restarts the scan through its permission gate, so a
        // scope that newly includes Bluetooth can prompt before the BLE leg starts.
    }

    fun startScan() {
        // Restart-friendly: changing the scan scope mid-scan calls this again. Cancel our
        // handle so the old listener stops mutating state; PrintBeam serializes the rest.
        scanHandle?.cancel()
        _state.value = _state.value.copy(
            scanning = true,
            scanResults = emptyList(),
            scanError = null,
            showScanSheet = true,
        )
        // PrintBeam.scan streams: printers appear in the sheet as they respond instead of
        // all at once when the window closes. Callbacks arrive on the main dispatcher, so
        // they can touch UI state directly.
        scanHandle = PrintBeam.scan(
            transports = _state.value.scanScope.transports,
            listener = object : ScanListener {
                override fun onPrinterFound(printer: DiscoveredPrinter) {
                    // Key by id — a later source can re-emit the same printer with richer
                    // fields (mDNS resolves a name for a bare port-scan hit), and the
                    // re-emission should replace the row, not append a duplicate.
                    _state.value = _state.value.copy(
                        scanResults = _state.value.scanResults
                            .filterNot { it.id == printer.id } + printer,
                    )
                }

                override fun onTransportFailed(transport: Transport, cause: PrinterException) {
                    _state.value = _state.value.copy(
                        scanError = cause.message ?: "Scan failed",
                    )
                }

                override fun onFinished(printers: List<DiscoveredPrinter>) {
                    _state.value = _state.value.copy(scanning = false, scanResults = printers)
                }
            },
        )
    }

    fun dismissScan() {
        scanHandle?.cancel()
        scanHandle = null
        _state.value = _state.value.copy(
            scanning = false,
            showScanSheet = false,
            scanResults = emptyList(),
            scanError = null,
        )
    }

    fun pickDiscovered(printer: DiscoveredPrinter) {
        val s = _state.value
        val newState = when (val ep = printer.endpoint) {
            is PrinterEndpoint.Network -> s.copy(
                transport = Transport.NETWORK,
                host = ep.host,
                port = ep.port,
                bleDeviceId = null,
                printerName = printer.name,
                manufacturer = printer.manufacturer,
                showScanSheet = false,
                scanResults = emptyList(),
                scanError = null,
            )
            is PrinterEndpoint.Ble -> s.copy(
                transport = Transport.BLE,
                bleDeviceId = ep.deviceId,
                host = "",
                printerName = printer.name,
                manufacturer = printer.manufacturer,
                showScanSheet = false,
                scanResults = emptyList(),
                scanError = null,
            )
        }
        _state.value = newState
        persist()
    }

    fun openManualSheet() {
        val s = _state.value
        _state.value = s.copy(
            showManualSheet = true,
            manualTransport = s.transport,
            manualHost = s.host,
            manualPort = s.port.toString(),
            manualBleDeviceId = s.bleDeviceId.orEmpty(),
            manualHostError = null,
            manualPortError = null,
            manualBleError = null,
        )
    }

    fun dismissManualSheet() {
        _state.value = _state.value.copy(
            showManualSheet = false,
            manualHostError = null,
            manualPortError = null,
            manualBleError = null,
        )
    }

    fun onManualTransportChange(transport: Transport) {
        _state.value = _state.value.copy(
            manualTransport = transport,
            manualHostError = null,
            manualPortError = null,
            manualBleError = null,
        )
    }

    fun onManualHostChange(value: String) {
        _state.value = _state.value.copy(manualHost = value, manualHostError = null)
    }

    fun onManualPortChange(value: String) {
        _state.value = _state.value.copy(
            manualPort = value.filter { it.isDigit() }.take(5),
            manualPortError = null,
        )
    }

    fun onManualBleDeviceIdChange(value: String) {
        _state.value = _state.value.copy(
            // Auto-uppercase MACs as the user types — visual consistency with the canonical
            // hex format that scanners and printer labels use.
            manualBleDeviceId = value.uppercase().take(17),
            manualBleError = null,
        )
    }

    fun commitManual(): Boolean {
        val s = _state.value
        return when (s.manualTransport) {
            Transport.NETWORK -> commitNetwork(s)
            Transport.BLE -> commitBle(s)
        }
    }

    private fun commitNetwork(s: SettingsUiState): Boolean {
        val host = s.manualHost.trim()
        val parsedPort = s.manualPort.toIntOrNull()
        val hostErr = if (host.isBlank()) "Required" else null
        val portErr = when {
            parsedPort == null -> "Number required"
            parsedPort !in 1..65535 -> "1-65535"
            else -> null
        }
        if (hostErr != null || portErr != null) {
            _state.value = s.copy(manualHostError = hostErr, manualPortError = portErr)
            return false
        }
        _state.value = s.copy(
            transport = Transport.NETWORK,
            host = host,
            port = parsedPort!!,
            bleDeviceId = null,
            printerName = null,
            manufacturer = null,
            showManualSheet = false,
            manualHostError = null,
            manualPortError = null,
            manualBleError = null,
        )
        persist()
        return true
    }

    private fun commitBle(s: SettingsUiState): Boolean {
        val deviceId = s.manualBleDeviceId.trim().uppercase()
        val err = when {
            deviceId.isBlank() -> "Required"
            !MAC_REGEX.matches(deviceId) -> "Must be a MAC address (XX:XX:XX:XX:XX:XX)"
            else -> null
        }
        if (err != null) {
            _state.value = s.copy(manualBleError = err)
            return false
        }
        _state.value = s.copy(
            transport = Transport.BLE,
            bleDeviceId = deviceId,
            host = "",
            printerName = null,
            manufacturer = null,
            showManualSheet = false,
            manualHostError = null,
            manualPortError = null,
            manualBleError = null,
        )
        persist()
        return true
    }

    fun disconnect() {
        val s = _state.value
        // Also close the connection PrintBeam may be holding for the old printer —
        // addManualPrinter on the same endpoint returns the same stable id the print
        // path registered, so this addresses exactly that held session.
        if (s.isConnected) {
            val oldEndpoint = when (s.transport) {
                Transport.NETWORK -> PrinterEndpoint.Network(host = s.host, port = s.port)
                Transport.BLE -> PrinterEndpoint.Ble(deviceId = s.bleDeviceId.orEmpty())
            }
            viewModelScope.launch {
                PrintBeam.disconnect(PrintBeam.addManualPrinter(oldEndpoint))
            }
        }
        _state.value = s.copy(
            transport = Transport.NETWORK,
            host = "",
            port = 9100,
            bleDeviceId = null,
            printerName = null,
            manufacturer = null,
        )
        persist()
    }

    override fun onCleared() {
        scanHandle?.cancel()
        scanHandle = null
    }

    private fun persist() {
        val s = _state.value
        repo.save(
            PrinterSettings(
                transport = s.transport,
                host = s.host,
                port = s.port,
                bleDeviceId = s.bleDeviceId,
                paperWidth = s.paperWidth,
                printerName = s.printerName,
                manufacturer = s.manufacturer,
            ),
        )
    }

    companion object {
        // Canonical colon-separated MAC: 6 hex octets, e.g. 60:6E:41:01:46:B2.
        private val MAC_REGEX = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}\$")

        fun factory(repo: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(repo) as T
            }
    }
}
