import SwiftUI

/// One grid cell: emoji art, heart, weight badge, name, delivery line, price row + add.
struct ProductCard: View {
    @Environment(Catalog.self) private var catalog
    @Environment(CartStore.self) private var cart

    let product: Product

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            artwork
            weightBadge
            Text(product.name)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.fcInk)
                .lineLimit(1)
            deliveryLine
            priceRow
        }
        .padding(12)
        .fcCard()
    }

    // Large emoji stands in for product photography — see the README note.
    private var artwork: some View {
        Text(product.emoji)
            .font(.system(size: 64))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .overlay(alignment: .topTrailing) { favoriteButton }
    }

    private var favoriteButton: some View {
        let isFavorite = catalog.isFavorite(product)
        return Button {
            catalog.toggleFavorite(product)
        } label: {
            Image(systemName: isFavorite ? "heart.fill" : "heart")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(isFavorite ? Color.fcFavorite : Color.fcMuted)
                .padding(4) // widens the tap target past the bare glyph
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityLabel(isFavorite ? "Remove \(product.name) from favorites" : "Add \(product.name) to favorites")
    }

    private var weightBadge: some View {
        Text(product.weight)
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(Color.fcBadgeText)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(Color.fcBadgeBg, in: RoundedRectangle(cornerRadius: FCRadius.badge, style: .continuous))
    }

    private var deliveryLine: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(Color.fcFresh)
                .frame(width: 5, height: 5)
            Text("10 MINS")
                .font(.system(size: 10, weight: .medium))
                .kerning(0.5)
                .foregroundStyle(Color.fcMuted)
        }
    }

    private var priceRow: some View {
        HStack(alignment: .center) {
            HStack(alignment: .firstTextBaseline, spacing: 5) {
                Text("₹\(product.price)")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.fcInk)
                Text("₹\(product.mrp)")
                    .font(.system(size: 12))
                    .strikethrough()
                    .foregroundStyle(Color.fcMuted)
            }
            Spacer()
            addButton
        }
    }

    /// "+" until the product is in the cart, then the current count. Tapping always adds one.
    private var addButton: some View {
        let count = cart.quantity(of: product)
        return Button {
            cart.add(product)
        } label: {
            Group {
                if count == 0 {
                    Image(systemName: "plus")
                        .font(.system(size: 14, weight: .semibold))
                } else {
                    Text("\(count)")
                        .font(.system(size: 14, weight: .bold))
                        .contentTransition(.numericText())
                }
            }
            .foregroundStyle(Color.fcOnAccent)
            .frame(width: 32, height: 32)
            .background(Color.fcAccent, in: Circle())
            .animation(.easeOut(duration: 0.15), value: count)
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityLabel("Add \(product.name) to cart")
        .accessibilityValue(count == 0 ? "" : "\(count) in cart")
    }
}

#Preview {
    ProductCard(product: InMemoryProductRepository().allProducts()[0])
        .frame(width: 180)
        .padding()
        .background(Color.fcGround)
        .environment(Catalog(repository: InMemoryProductRepository()))
        .environment(CartStore())
}
