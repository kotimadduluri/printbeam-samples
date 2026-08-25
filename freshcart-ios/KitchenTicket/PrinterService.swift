import Foundation
import CoreBluetooth
import PrintBeam

enum PrinterServiceError: LocalizedError {
    case printerFailure(String)

    var errorDescription: String? {
        switch self {
        case .printerFailure(let msg): return msg
        }
    }
}

/// Thin wrapper around the `PrintBeam` facade. The facade holds one connection per printer
/// across tickets, so back-to-back orders don't pay a fresh TCP connect each time; a dead
/// link is reopened and retried once automatically.
struct PrinterService {

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

    func printKitchenTicket(
        tableNumber: String,
        orderNumber: Int,
        items: [String]
    ) async throws {
        let settings = Settings.shared
        let endpoint = PrinterEndpoint.Network(host: settings.host, port: Int32(settings.port))
        let paper: PaperWidth
        switch settings.paperWidth {
        case .mm58: paper = PaperWidth.mm58
        case .mm80: paper = PaperWidth.mm80
        }

        // Same endpoint → same stable id, and re-registering just refreshes the entry, so
        // this is safe to call per ticket. PrintBeam keeps the id's connection open between
        // calls — printing ticket #2 skips the connect entirely.
        let printerId = try PrintBeam.shared.addManualPrinter(
            endpoint: endpoint,
            name: settings.printerName,
            paperWidth: paper
        )

        let result = try await PrintBeam.shared.print(printerId: printerId, block: { builder in
            builder.align(alignment: Alignment.center)
            builder.bold { _ in
                builder.text(value: "KITCHEN")
            }
            builder.divider(char: "-")
            builder.align(alignment: Alignment.left)
            builder.text(value: "Table \(tableNumber)")
            builder.feed(lines: 1)
            builder.size(width: 2, height: 2) { _ in
                builder.text(value: String(format: "Order #%03d", orderNumber))
            }
            builder.divider(char: "-")
            for item in items where !item.isEmpty {
                builder.text(value: item)
            }
            builder.feed(lines: 1)
            builder.cut(partial: false)
        })

        if let failure = result as? PrintResult.Failure {
            throw PrinterServiceError.printerFailure(failure.exception.message ?? "Unknown printer error")
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
                transports: [Transport.network],
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
