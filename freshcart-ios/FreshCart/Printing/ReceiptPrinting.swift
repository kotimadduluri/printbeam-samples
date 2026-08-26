import Foundation
import PrintBeam

enum ReceiptPrintError: LocalizedError {
    case printerFailure(String)

    var errorDescription: String? {
        switch self {
        case .printerFailure(let msg): return msg
        }
    }
}

/// The printing seam: order flow and views talk to this, never to PrintBeam types.
/// Swapping the thermal printer for a PDF or a mock test double happens here.
protocol ReceiptPrinting {
    func printReceipt(items: [CartItem], orderNumber: Int) async throws
}

/// PrintBeam-backed implementation. The facade holds one connection per printer across
/// orders, so back-to-back receipts don't pay a fresh TCP connect each time; a dead
/// link is reopened and retried once automatically.
struct PrintBeamReceiptPrinter: ReceiptPrinting {

    func printReceipt(items: [CartItem], orderNumber: Int) async throws {
        let settings = Settings.shared
        let endpoint: PrinterEndpoint
        switch settings.transport {
        case .network:
            endpoint = PrinterEndpoint.Network(host: settings.host, port: Int32(settings.port))
        case .ble:
            // Kotlin's default profile argument doesn't bridge to Swift — pass NORDIC_UART
            // explicitly, matching the Kotlin-side default. The SDK still auto-detects the
            // writable characteristic at connect time if the printer uses different UUIDs.
            endpoint = PrinterEndpoint.Ble(
                deviceId: settings.bleDeviceId ?? "",
                profile: BleProfile.companion.NORDIC_UART
            )
        }
        let paper: PaperWidth
        switch settings.paperWidth {
        case .mm58: paper = PaperWidth.mm58
        case .mm80: paper = PaperWidth.mm80
        }

        // Same endpoint → same stable id, and re-registering just refreshes the entry, so
        // this is safe to call per order. PrintBeam keeps the id's connection open between
        // calls — printing order #2 skips the connect entirely.
        let printerId = try PrintBeam.shared.addManualPrinter(
            endpoint: endpoint,
            name: settings.printerName,
            paperWidth: paper
        )

        let itemsTotal = items.reduce(0) { $0 + $1.lineTotal }
        let savings = items.reduce(0) { $0 + $1.lineMrpTotal } - itemsTotal

        // Receipt layout per DESIGN.md — identical bytes-intent across all three sample apps.
        let result = try await PrintBeam.shared.print(printerId: printerId, block: { builder in
            builder.align(alignment: Alignment.center)
            builder.bold { _ in
                builder.text(value: "FRESHCART")
            }
            builder.text(value: "Fresh groceries, printed fast")
            builder.divider(char: "-")
            builder.align(alignment: Alignment.left)
            for item in items {
                builder.line(
                    left: "\(item.quantity)x \(item.product.name) \(item.product.weight)",
                    right: "₹\(item.lineTotal)"
                )
            }
            builder.divider(char: "-")
            builder.line(left: "Items", right: "₹\(itemsTotal)")
            if savings > 0 {
                builder.line(left: "You saved", right: "₹\(savings)")
            }
            builder.bold { _ in
                builder.line(left: "TOTAL", right: "₹\(itemsTotal)")
            }
            builder.feed(lines: 1)
            builder.align(alignment: Alignment.center)
            builder.text(value: String(format: "Order #%03d", orderNumber))
            builder.text(value: "Thank you!")
            builder.feed(lines: 2)
            builder.cut(partial: false)
        })

        if let failure = result as? PrintResult.Failure {
            throw ReceiptPrintError.printerFailure(failure.exception.message ?? "Unknown printer error")
        }
    }
}
