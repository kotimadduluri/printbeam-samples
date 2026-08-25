import SwiftUI

/// The three functional chips: price sort (tri-state), category (cycles), offers (toggle).
struct FilterChipRow: View {
    @Environment(Catalog.self) private var catalog

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChip(
                    label: sortLabel,
                    systemImage: sortIcon,
                    isSelected: catalog.sortOrder != .none,
                    action: { catalog.toggleSort() }
                )
                FilterChip(
                    label: categoryLabel,
                    systemImage: nil,
                    isSelected: catalog.categoryFilter != .all,
                    action: { catalog.cycleCategory() }
                )
                FilterChip(
                    label: "Offers",
                    systemImage: nil,
                    isSelected: catalog.offersOnly,
                    action: { catalog.toggleOffers() }
                )
            }
        }
    }

    private var sortLabel: String {
        switch catalog.sortOrder {
        case .none: return "Sort By"
        case .priceLowHigh: return "Price"
        case .priceHighLow: return "Price"
        }
    }

    private var sortIcon: String? {
        switch catalog.sortOrder {
        case .none: return "arrow.up.arrow.down"
        case .priceLowHigh: return "arrow.up"
        case .priceHighLow: return "arrow.down"
        }
    }

    private var categoryLabel: String {
        switch catalog.categoryFilter {
        case .all: return "Category"
        case .only(let category): return category.rawValue
        }
    }
}

private struct FilterChip: View {
    let label: String
    let systemImage: String?
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Text(label)
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.system(size: 11, weight: .semibold))
                }
            }
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(isSelected ? Color.fcOnAccent : Color.fcInk)
            .padding(.horizontal, 14)
            .padding(.vertical, 9)
            .background(isSelected ? Color.fcAccent : Color.fcSurface, in: Capsule())
            .shadow(color: Color.fcInk.opacity(isSelected ? 0 : 0.04), radius: 6, y: 2)
        }
        .buttonStyle(PressableButtonStyle())
        .animation(.easeOut(duration: 0.15), value: isSelected)
    }
}
