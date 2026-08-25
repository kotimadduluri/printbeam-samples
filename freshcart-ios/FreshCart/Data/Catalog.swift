import Foundation
import Observation

/// Where products come from. In-memory here — a real shop would back this with an API,
/// and this seam is where that swap happens.
protocol ProductRepository {
    func allProducts() -> [Product]
}

/// The shared 8-product catalog from DESIGN.md, identical across all three sample apps.
struct InMemoryProductRepository: ProductRepository {
    func allProducts() -> [Product] {
        [
            Product(id: "oranges", emoji: "🍊", name: "Sweet Oranges", category: .fruits, weight: "300g", price: 30, mrp: 40),
            Product(id: "apples", emoji: "🍎", name: "Fresh Apples", category: .fruits, weight: "300g", price: 75, mrp: 90),
            Product(id: "bananas", emoji: "🍌", name: "Ripe Bananas", category: .fruits, weight: "500g", price: 45, mrp: 55),
            Product(id: "grapes", emoji: "🍇", name: "Green Grapes", category: .fruits, weight: "500g", price: 65, mrp: 80),
            Product(id: "tomatoes", emoji: "🍅", name: "Fresh Tomatoes", category: .vegetables, weight: "300g", price: 20, mrp: 30),
            Product(id: "lettuce", emoji: "🥬", name: "Organic Lettuce", category: .vegetables, weight: "300g", price: 25, mrp: 30),
            Product(id: "spinach", emoji: "🌿", name: "Baby Spinach", category: .vegetables, weight: "250g", price: 35, mrp: 45),
            Product(id: "corn", emoji: "🌽", name: "Sweet Corn", category: .vegetables, weight: "2 pc", price: 40, mrp: 50),
        ]
    }
}

/// Shop screen state holder: the product list plus the search/chip filters applied to it.
/// Views read `visibleProducts` and send explicit events; they never filter on their own.
@Observable
final class Catalog {

    enum SortOrder {
        case none
        case priceLowHigh
        case priceHighLow
    }

    enum CategoryFilter: Equatable {
        case all
        case only(ProductCategory)
    }

    private let products: [Product]

    var query: String = ""
    private(set) var sortOrder: SortOrder = .none
    private(set) var categoryFilter: CategoryFilter = .all
    private(set) var offersOnly: Bool = false
    private(set) var favorites: Set<String> = []

    init(repository: ProductRepository) {
        self.products = repository.allProducts()
    }

    /// Search → category → offers → sort, in that order.
    var visibleProducts: [Product] {
        var result = products

        let trimmed = query.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty {
            result = result.filter { $0.name.localizedCaseInsensitiveContains(trimmed) }
        }
        if case .only(let category) = categoryFilter {
            result = result.filter { $0.category == category }
        }
        if offersOnly {
            result = result.filter(\.isDiscounted)
        }
        switch sortOrder {
        case .none: break
        case .priceLowHigh: result.sort { $0.price < $1.price }
        case .priceHighLow: result.sort { $0.price > $1.price }
        }
        return result
    }

    // MARK: Events

    /// Low→high → high→low → off.
    func toggleSort() {
        switch sortOrder {
        case .none: sortOrder = .priceLowHigh
        case .priceLowHigh: sortOrder = .priceHighLow
        case .priceHighLow: sortOrder = .none
        }
    }

    /// All → Fruits → Vegetables → All.
    func cycleCategory() {
        switch categoryFilter {
        case .all: categoryFilter = .only(.fruits)
        case .only(.fruits): categoryFilter = .only(.vegetables)
        case .only(.vegetables): categoryFilter = .all
        }
    }

    func toggleOffers() {
        offersOnly.toggle()
    }

    func clearSearch() {
        query = ""
    }

    func isFavorite(_ product: Product) -> Bool {
        favorites.contains(product.id)
    }

    func toggleFavorite(_ product: Product) {
        if !favorites.insert(product.id).inserted {
            favorites.remove(product.id)
        }
    }
}
