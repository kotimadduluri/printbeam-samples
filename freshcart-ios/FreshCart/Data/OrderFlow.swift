import Foundation
import Observation

/// Drives the Place Order → print → success/failure cycle on the Cart screen.
/// One phase at a time; the view renders whatever phase it finds.
@Observable
@MainActor
final class OrderFlow {

    enum Phase: Equatable {
        case idle
        case printing
        case success(orderNumber: Int)
        case failure(message: String)
    }

    private(set) var phase: Phase = .idle
    private let printer: ReceiptPrinting

    init(printer: ReceiptPrinting) {
        self.printer = printer
    }

    var isPrinting: Bool { phase == .printing }

    /// True when there's no saved printer endpoint — the CTA routes to Settings instead.
    var needsPrinterSetup: Bool {
        !Settings.shared.isConfigured
    }

    /// Prints the receipt and, on success, clears the cart. The order number is only
    /// consumed once the print succeeds, so a failed print retries with the same number.
    func placeOrder(cart: CartStore) async {
        guard !cart.isEmpty, phase != .printing else { return }
        let items = cart.items
        let orderNumber = Settings.shared.peekNextOrderNumber()
        phase = .printing
        do {
            try await printer.printReceipt(items: items, orderNumber: orderNumber)
            _ = Settings.shared.consumeNextOrderNumber()
            cart.clear()
            phase = .success(orderNumber: orderNumber)
        } catch {
            phase = .failure(message: error.localizedDescription)
        }
    }

    /// Back to a clean slate — leaving the success screen or dismissing an error.
    func reset() {
        phase = .idle
    }
}
