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

    func start(timeoutSeconds: Int = 6) {
        cancel()
        byId = [:]
        order = []
        do {
            handle = try PrintBeam.shared.scan(
                transports: [Transport.network, Transport.ble],
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
