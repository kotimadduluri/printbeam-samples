import SwiftUI

/// The app-scoped dependency container: every store is built once here and handed
/// down through the environment. Plain constructor injection — no framework.
@MainActor
final class AppContainer {
    let catalog: Catalog
    let cart: CartStore
    let orderFlow: OrderFlow

    init() {
        catalog = Catalog(repository: InMemoryProductRepository())
        cart = CartStore()
        orderFlow = OrderFlow(printer: PrintBeamReceiptPrinter())
    }
}

@main
struct FreshCartApp: App {
    private let container: AppContainer

    init() {
        // One-time facade setup — everything else in the app just calls PrintBeam.shared.
        PrinterService.initializeLibrary()
        container = AppContainer()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(container.catalog)
                .environment(container.cart)
                .environment(container.orderFlow)
                // The DESIGN.md palette is a single light scheme (fixed hex surfaces on a
                // near-white ground) with no dark variants, so dark mode is opted out of
                // rather than half-supported.
                .preferredColorScheme(.light)
        }
    }
}
