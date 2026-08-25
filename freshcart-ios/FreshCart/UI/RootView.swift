import SwiftUI

enum Route: Hashable {
    case cart
    case printerSettings
}

/// Owns the one NavigationStack. Shop is the root; Cart and Printer settings push onto it.
struct RootView: View {
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            ShopView(
                onOpenCart: { path.append(.cart) },
                onOpenPrinterSettings: { path.append(.printerSettings) }
            )
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .cart:
                    CartView(
                        onOpenPrinterSettings: { path.append(.printerSettings) },
                        onBackToShop: { path.removeAll() }
                    )
                case .printerSettings:
                    SettingsView()
                }
            }
        }
        .tint(Color.fcAccent)
    }
}

#Preview {
    RootView()
        .environment(Catalog(repository: InMemoryProductRepository()))
        .environment(CartStore())
        .environment(OrderFlow(printer: PrintBeamReceiptPrinter()))
}
