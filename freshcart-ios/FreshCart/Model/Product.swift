import Foundation

enum ProductCategory: String, CaseIterable {
    case fruits = "Fruits"
    case vegetables = "Vegetables"
}

/// One catalog entry. Prices are whole rupees — the catalog has no paise anywhere,
/// so integer math keeps totals exact.
struct Product: Identifiable, Equatable {
    let id: String
    let emoji: String
    let name: String
    let category: ProductCategory
    let weight: String
    let price: Int
    let mrp: Int

    var isDiscounted: Bool { mrp > price }
}

/// A product plus how many of it sits in the cart.
struct CartItem: Identifiable, Equatable {
    let product: Product
    var quantity: Int

    var id: String { product.id }
    var lineTotal: Int { product.price * quantity }
    var lineMrpTotal: Int { product.mrp * quantity }
}
