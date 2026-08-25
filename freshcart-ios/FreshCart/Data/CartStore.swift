import Foundation
import Observation

/// Single source of truth for the cart. In-memory by design — a sample app has no
/// reason to survive relaunch with three bananas in it.
@Observable
final class CartStore {

    private(set) var items: [CartItem] = []

    var isEmpty: Bool { items.isEmpty }

    /// Total number of units — the count on the cart badge.
    var totalQuantity: Int {
        items.reduce(0) { $0 + $1.quantity }
    }

    var itemsTotal: Int {
        items.reduce(0) { $0 + $1.lineTotal }
    }

    /// MRP minus price across every line; shown only when positive.
    var savings: Int {
        items.reduce(0) { $0 + $1.lineMrpTotal } - itemsTotal
    }

    func quantity(of product: Product) -> Int {
        items.first(where: { $0.id == product.id })?.quantity ?? 0
    }

    /// Adds one unit, appending a new line the first time. Insertion order is kept
    /// so the cart reads in the order things were added.
    func add(_ product: Product) {
        if let index = items.firstIndex(where: { $0.id == product.id }) {
            items[index].quantity += 1
        } else {
            items.append(CartItem(product: product, quantity: 1))
        }
    }

    /// Removes one unit; at quantity 1 the line disappears.
    func decrement(_ product: Product) {
        guard let index = items.firstIndex(where: { $0.id == product.id }) else { return }
        if items[index].quantity > 1 {
            items[index].quantity -= 1
        } else {
            items.remove(at: index)
        }
    }

    func clear() {
        items = []
    }
}
