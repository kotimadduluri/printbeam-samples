import Foundation
import CoreBluetooth
import PrintBeam

/// One-time PrintBeam setup plus the scan bridge. The receipt path lives behind
/// `ReceiptPrinting` — this file is the only other place the SDK is touched.
enum PrinterService {

    /// Call once from the app root before anything prints or scans. Kotlin default arguments
    /// don't carry across the Swift bridge, so every config parameter is spelled out.
    static func initializeLibrary() {
        let logger = PrinterLoggerCompanion.shared.NoOp
        let config = PrintBeamConfig(
            // PrinterContext on iOS owns an optional CBCentralManager — pass nil so the
            // library lazily builds one if BLE is ever needed. Network printing works without it.
            context: PrinterContext(externalCentralManager: nil),
            logger: logger,
            connectionFactory: DefaultConnectionFactory(logger: logger),
            defaultPaperWidth: PaperWidth.mm80
        )
        do {
            try PrintBeam.shared.initialize(config: config)
        } catch {
            // Only throws when re-initializing while printers are connected — impossible at
            // app launch, so surface loudly in debug rather than limping along unprinted.
            assertionFailure("PrintBeam.initialize failed: \(error)")
        }
    }
}

/// Bridges PrintBeam's streaming `ScanListener` to SwiftUI closures. Kotlin interfaces surface
/// as Obj-C protocols, so the conforming class inherits NSObject. All callbacks arrive on the
/// main dispatcher, so the closures may touch view state directly.
final class PrinterScanner: NSObject, ScanListener {

    /// Fires per discovery with the printers found so far, in first-seen order — the list
    /// fills in live while the scan window is still open.
    var onUpdate: (([DiscoveredPrinter]) -> Void)?
    /// Fires when a scan source fails (informational — remaining transports keep scanning).
    var onFailed: ((String) -> Void)?
    /// Fires once with the final deduplicated snapshot. Not fired for cancelled scans.
    var onDone: (([DiscoveredPrinter]) -> Void)?

    private var handle: ScanHandle?
    private var byId: [String: DiscoveredPrinter] = [:]
    private var order: [String] = []

    func start(transports: Set<Transport>, timeoutSeconds: Int = 6) {
        cancel()
        byId = [:]
        order = []
        do {
            handle = try PrintBeam.shared.scan(
                transports: transports,
                timeoutMs: Int64(timeoutSeconds) * 1000,
                listener: self
            )
        } catch {
            onFailed?(error.localizedDescription)
            onDone?([])
        }
    }

    func cancel() {
        handle?.cancel()
        handle = nil
    }

    // MARK: ScanListener

    func onPrinterFound(printer: DiscoveredPrinter) {
        // Keyed by id — a later source can re-emit the same printer with richer fields
        // (mDNS resolves a name for a bare port-scan hit); replace, don't append.
        if byId[printer.id] == nil { order.append(printer.id) }
        byId[printer.id] = printer
        onUpdate?(order.compactMap { byId[$0] })
    }

    func onTransportFailed(transport: Transport, cause: PrinterException) {
        onFailed?(cause.message ?? "Scan failed")
    }

    func onFinished(printers: [DiscoveredPrinter]) {
        onDone?(printers)
    }
}

/// The SDK transports a scan scope translates to. Lives here (not in Settings) so the
/// settings store never imports PrintBeam types.
extension ScanScopeOption {
    var transports: Set<Transport> {
        switch self {
        case .all: return [Transport.network, Transport.ble]
        case .network: return [Transport.network]
        case .ble: return [Transport.ble]
        }
    }
}

/// Discovery couldn't name this printer (many network printers don't advertise mDNS at
/// all) — so ask the printer itself: `queryDeviceInfo` reads ESC/POS `GS I` identity
/// (manufacturer + model) over the facade's held session. Best-effort: printers that
/// ignore `GS I` (typical for BLE) or a dropped link keep the transport fallback label,
/// and a name the scan DID provide is never overwritten.
enum PrinterIdentity {

    static func resolveNameIfNeeded() {
        let settings = Settings.shared
        guard settings.isConfigured, settings.printerName == nil else { return }
        let transport = settings.transport
        let host = settings.host
        let bleDeviceId = settings.bleDeviceId

        let endpoint: PrinterEndpoint
        switch transport {
        case .network:
            endpoint = PrinterEndpoint.Network(host: host, port: Int32(settings.port))
        case .ble:
            endpoint = PrinterEndpoint.Ble(
                deviceId: bleDeviceId ?? "",
                profile: BleProfile.companion.NORDIC_UART
            )
        }
        let paper: PaperWidth = settings.paperWidth == .mm58 ? PaperWidth.mm58 : PaperWidth.mm80

        Task {
            guard let id = try? PrintBeam.shared.addManualPrinter(
                endpoint: endpoint, name: nil, paperWidth: paper
            ) else { return }
            guard let info = try? await PrintBeam.shared.queryDeviceInfo(printerId: id) else { return }
            let name = [info.manufacturer, info.model].compactMap { $0 }.joined(separator: " ")
            guard !name.isEmpty else { return }
            await MainActor.run {
                // Re-check — the user may have switched printers while the query ran.
                let current = Settings.shared
                guard current.printerName == nil, current.transport == transport,
                      current.host == host, current.bleDeviceId == bleDeviceId else { return }
                current.printerName = name
                if current.manufacturer == nil { current.manufacturer = info.manufacturer }
            }
        }
    }
}
