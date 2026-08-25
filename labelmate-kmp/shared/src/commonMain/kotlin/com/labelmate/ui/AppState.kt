package com.labelmate.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labelmate.data.LabelPrinter
import com.labelmate.data.PrinterSettings
import com.labelmate.data.SettingsStore
import com.labelmate.data.isValidEan13
import dev.printbeam.PaperWidth
import dev.printbeam.PrintResult
import dev.printbeam.PrinterEndpoint
import dev.printbeam.PrinterException
import dev.printbeam.Transport
import dev.printbeam.discovery.DiscoveredPrinter
import dev.printbeam.sdk.PrintBeam
import dev.printbeam.sdk.ScanHandle
import dev.printbeam.sdk.ScanListener
import kotlinx.coroutines.launch

/**
 * Single-screen-pair app state. ViewModel rather than plain state holder so it survives
 * Android config changes for free — Compose Multiplatform pulls the lifecycle-viewmodel
 * artifact into iosMain too.
 *
 * All printing goes through the [PrintBeam] facade, which the platform entry point initialized
 * — no platform handles reach shared code.
 *
 * @param supportsBleManual whether manual BLE entry is exposed in the UI. True on Android
 *   (users can read a MAC off the printer label); false on iOS (CBPeripheral identifiers
 *   are OS-generated and not on any label, so manual entry is impractical there).
 */
class AppViewModel(
    private val settingsStore: SettingsStore,
    val supportsBleManual: Boolean = false,
    private val labelPrinter: LabelPrinter = LabelPrinter(),
) : ViewModel() {

    private var scanHandle: ScanHandle? = null

    var screen: Screen by mutableStateOf(Screen.Label)
        private set

    var savedTransport: Transport by mutableStateOf(Transport.NETWORK)
        private set
    var savedHost: String by mutableStateOf("")
        private set
    var savedPort: Int by mutableStateOf(9100)
        private set
    var savedBleDeviceId: String? by mutableStateOf(null)
        private set
    var paperWidth: PaperWidth by mutableStateOf(PaperWidth.MM_58)
        private set

    var selectedPrinterName: String? by mutableStateOf(null)
        private set
    var selectedPrinterManufacturer: String? by mutableStateOf(null)
        private set

    var productName: String by mutableStateOf("")
        private set
    var price: String by mutableStateOf("")
        private set
    var ean13: String by mutableStateOf("")
        private set

    var isPrinting: Boolean by mutableStateOf(false)
        private set
    var snackbarMessage: String? by mutableStateOf(null)
        private set

    var isScanning: Boolean by mutableStateOf(false)
        private set
    var scanResults: List<DiscoveredPrinter> by mutableStateOf(emptyList())
        private set
    var scanError: String? by mutableStateOf(null)
        private set
    var showScanDialog: Boolean by mutableStateOf(false)
        private set

    var showManualDialog: Boolean by mutableStateOf(false)
        private set
    var manualTransport: Transport by mutableStateOf(Transport.NETWORK)
        private set
    var manualHost: String by mutableStateOf("")
        private set
    var manualPort: String by mutableStateOf("9100")
        private set
    var manualBleDeviceId: String by mutableStateOf("")
        private set
    var manualHostError: String? by mutableStateOf(null)
        private set
    var manualPortError: String? by mutableStateOf(null)
        private set
    var manualBleError: String? by mutableStateOf(null)
        private set

    init {
        val loaded = settingsStore.load()
        savedTransport = loaded.transport
        savedHost = loaded.host
        savedPort = loaded.port
        savedBleDeviceId = loaded.bleDeviceId
        paperWidth = loaded.paperWidth
        selectedPrinterName = loaded.printerName
        selectedPrinterManufacturer = loaded.manufacturer
    }

    val isConnected: Boolean get() = when (savedTransport) {
        Transport.NETWORK -> savedHost.isNotBlank()
        Transport.BLE -> !savedBleDeviceId.isNullOrBlank()
    }

    fun navigate(target: Screen) { screen = target }

    fun onProductNameChange(value: String) { productName = value }
    fun onPriceChange(value: String) { price = value }
    fun onEan13Change(value: String) {
        ean13 = value.filter { it.isDigit() }.take(13)
    }

    fun onPaperWidthChange(value: PaperWidth) {
        paperWidth = value
        persist()
    }

    fun dismissSnackbar() { snackbarMessage = null }

    fun startScan() {
        if (isScanning) return
        isScanning = true
        scanResults = emptyList()
        scanError = null
        showScanDialog = true
        // PrintBeam.scan streams: printers appear in the dialog as they respond. Callbacks
        // arrive on the main dispatcher, so they can touch Compose state directly.
        scanHandle = PrintBeam.scan(
            transports = setOf(Transport.NETWORK),
            listener = object : ScanListener {
                override fun onPrinterFound(printer: DiscoveredPrinter) {
                    // Key by id — a later source can re-emit the same printer with richer
                    // fields; the re-emission replaces the row rather than appending.
                    scanResults = scanResults.filterNot { it.id == printer.id } + printer
                }

                override fun onTransportFailed(transport: Transport, cause: PrinterException) {
                    scanError = cause.message ?: "Scan failed"
                }

                override fun onFinished(printers: List<DiscoveredPrinter>) {
                    scanResults = printers
                    isScanning = false
                }
            },
        )
    }

    fun dismissScan() {
        scanHandle?.cancel()
        scanHandle = null
        isScanning = false
        showScanDialog = false
        scanResults = emptyList()
        scanError = null
    }

    fun pickDiscovered(printer: DiscoveredPrinter) {
        when (val ep = printer.endpoint) {
            is PrinterEndpoint.Network -> {
                savedTransport = Transport.NETWORK
                savedHost = ep.host
                savedPort = ep.port
                savedBleDeviceId = null
            }
            is PrinterEndpoint.Ble -> {
                savedTransport = Transport.BLE
                savedBleDeviceId = ep.deviceId
                savedHost = ""
            }
        }
        selectedPrinterName = printer.name
        selectedPrinterManufacturer = printer.manufacturer
        showScanDialog = false
        scanResults = emptyList()
        scanError = null
        persist()
        snackbarMessage = "Connected to ${printer.name ?: printer.endpoint.id}"
    }

    fun openManualDialog() {
        manualTransport = savedTransport
        manualHost = savedHost
        manualPort = savedPort.toString()
        manualBleDeviceId = savedBleDeviceId.orEmpty()
        manualHostError = null
        manualPortError = null
        manualBleError = null
        showManualDialog = true
    }

    fun dismissManualDialog() {
        showManualDialog = false
        manualHostError = null
        manualPortError = null
        manualBleError = null
    }

    fun onManualTransportChange(transport: Transport) {
        manualTransport = transport
        manualHostError = null
        manualPortError = null
        manualBleError = null
    }

    fun onManualHostChange(value: String) {
        manualHost = value
        manualHostError = null
    }

    fun onManualPortChange(value: String) {
        manualPort = value.filter { it.isDigit() }.take(5)
        manualPortError = null
    }

    fun onManualBleDeviceIdChange(value: String) {
        // Auto-uppercase as the user types so the field always matches canonical MAC format.
        manualBleDeviceId = value.uppercase().take(17)
        manualBleError = null
    }

    fun commitManual(): Boolean = when (manualTransport) {
        Transport.NETWORK -> commitNetwork()
        Transport.BLE -> commitBle()
    }

    private fun commitNetwork(): Boolean {
        val host = manualHost.trim()
        val parsedPort = manualPort.toIntOrNull()
        val hostErr = if (host.isBlank()) "Required" else null
        val portErr = when {
            parsedPort == null -> "Number required"
            parsedPort !in 1..65535 -> "1–65535"
            else -> null
        }
        if (hostErr != null || portErr != null) {
            manualHostError = hostErr
            manualPortError = portErr
            return false
        }
        savedTransport = Transport.NETWORK
        savedHost = host
        savedPort = parsedPort!!
        savedBleDeviceId = null
        selectedPrinterName = null
        selectedPrinterManufacturer = null
        showManualDialog = false
        persist()
        return true
    }

    private fun commitBle(): Boolean {
        val deviceId = manualBleDeviceId.trim().uppercase()
        val err = when {
            deviceId.isBlank() -> "Required"
            !MAC_REGEX.matches(deviceId) -> "Must be a MAC address (XX:XX:XX:XX:XX:XX)"
            else -> null
        }
        if (err != null) {
            manualBleError = err
            return false
        }
        savedTransport = Transport.BLE
        savedBleDeviceId = deviceId
        savedHost = ""
        selectedPrinterName = null
        selectedPrinterManufacturer = null
        showManualDialog = false
        persist()
        return true
    }

    fun disconnect() {
        // Close the connection PrintBeam may be holding for the old printer — same endpoint
        // → same stable id, so this addresses exactly the session the print path opened.
        if (isConnected) {
            val oldEndpoint = when (savedTransport) {
                Transport.NETWORK -> PrinterEndpoint.Network(host = savedHost, port = savedPort)
                Transport.BLE -> PrinterEndpoint.Ble(deviceId = savedBleDeviceId.orEmpty())
            }
            viewModelScope.launch {
                PrintBeam.disconnect(PrintBeam.addManualPrinter(oldEndpoint))
            }
        }
        savedTransport = Transport.NETWORK
        savedHost = ""
        savedPort = 9100
        savedBleDeviceId = null
        selectedPrinterName = null
        selectedPrinterManufacturer = null
        persist()
    }

    override fun onCleared() {
        scanHandle?.cancel()
        scanHandle = null
    }

    private fun persist() {
        settingsStore.save(
            PrinterSettings(
                transport = savedTransport,
                host = savedHost,
                port = savedPort,
                bleDeviceId = savedBleDeviceId,
                paperWidth = paperWidth,
                printerName = selectedPrinterName,
                manufacturer = selectedPrinterManufacturer,
            ),
        )
    }

    fun printLabel() {
        if (isPrinting) return
        if (!isConnected) {
            snackbarMessage = "Connect a printer in Settings first"
            return
        }
        if (productName.isBlank()) {
            snackbarMessage = "Product name required"
            return
        }
        if (price.isBlank()) {
            snackbarMessage = "Price required"
            return
        }
        if (!isValidEan13(ean13)) {
            snackbarMessage = "EAN-13 must be 12 or 13 digits"
            return
        }

        val settings = PrinterSettings(
            transport = savedTransport,
            host = savedHost,
            port = savedPort,
            bleDeviceId = savedBleDeviceId,
            paperWidth = paperWidth,
        )

        isPrinting = true
        viewModelScope.launch {
            val result = runCatching {
                labelPrinter.printLabel(
                    settings = settings,
                    name = productName.trim(),
                    price = price.trim(),
                    ean13 = ean13,
                )
            }
            isPrinting = false
            snackbarMessage = result.fold(
                onSuccess = { r ->
                    when (r) {
                        is PrintResult.Success -> "Label printed"
                        is PrintResult.Failure -> "Print failed: ${r.exception.message ?: "unknown error"}"
                    }
                },
                onFailure = { e -> "Print failed: ${e.message ?: e::class.simpleName ?: "error"}" },
            )
        }
    }

    companion object {
        // Canonical colon-separated MAC: 6 hex octets.
        private val MAC_REGEX = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}\$")
    }
}

enum class Screen { Label, Settings }
