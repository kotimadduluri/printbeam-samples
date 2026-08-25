import SwiftUI

struct ShopView: View {
    @Environment(Catalog.self) private var catalog
    @Environment(CartStore.self) private var cart

    let onOpenCart: () -> Void
    let onOpenPrinterSettings: () -> Void

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        @Bindable var catalog = catalog

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                SearchPill(text: $catalog.query)

                FilterChipRow()

                Text("Fresh Items")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(Color.fcInk)
                    .padding(.top, 4)

                if catalog.visibleProducts.isEmpty {
                    NoMatchesView(query: catalog.query) {
                        catalog.clearSearch()
                    }
                } else {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(catalog.visibleProducts) { product in
                            ProductCard(product: product)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(Color.fcGround)
        .navigationTitle("FreshCart")
        .toolbarBackground(Color.fcGround, for: .navigationBar)
        .toolbar {
            // Persistent entry point for configuring or switching printers, ahead of the cart.
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: onOpenPrinterSettings) {
                    Image(systemName: "printer")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color.fcInk)
                }
                .accessibilityLabel("Printer settings")
            }
            ToolbarItem(placement: .topBarTrailing) {
                CartButton(count: cart.totalQuantity, action: onOpenCart)
            }
        }
    }
}

// MARK: - Cart button with count badge

private struct CartButton: View {
    let count: Int
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "cart")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Color.fcInk)
                .overlay(alignment: .topTrailing) {
                    if count > 0 {
                        Text("\(count)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Color.fcOnAccent)
                            .padding(4)
                            .frame(minWidth: 18)
                            .background(Color.fcAccent, in: Circle())
                            .offset(x: 10, y: -10)
                            .contentTransition(.numericText())
                    }
                }
                .animation(.easeOut(duration: 0.2), value: count)
        }
        .accessibilityLabel(count == 0 ? "Cart, empty" : "Cart, \(count) items")
    }
}

// MARK: - Search

private struct SearchPill: View {
    @Binding var text: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Color.fcMuted)
            TextField("Search for groceries…", text: $text)
                .font(.system(size: 15))
                .foregroundStyle(Color.fcInk)
                .autocorrectionDisabled()
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Color.fcMuted)
                }
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.fcSurface, in: Capsule())
        .shadow(color: Color.fcInk.opacity(0.04), radius: 8, y: 3)
    }
}

// MARK: - Empty search result

private struct NoMatchesView: View {
    let query: String
    let onClear: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("🥕")
                .font(.system(size: 48))
            Text("No groceries match \u{201C}\(query)\u{201D}")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.fcInk)
                .multilineTextAlignment(.center)
            Text("Try a different name, or start over.")
                .font(.system(size: 13))
                .foregroundStyle(Color.fcMuted)
            Button("Clear search", action: onClear)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.fcAccent)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }
}

#Preview {
    NavigationStack {
        ShopView(onOpenCart: {}, onOpenPrinterSettings: {})
    }
    .environment(Catalog(repository: InMemoryProductRepository()))
    .environment(CartStore())
}
