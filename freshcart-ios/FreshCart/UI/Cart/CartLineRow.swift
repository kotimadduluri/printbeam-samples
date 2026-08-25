import SwiftUI

/// One cart line: emoji, name + weight, unit price, stepper, line total.
struct CartLineRow: View {
    @Environment(CartStore.self) private var cart

    let item: CartItem

    var body: some View {
        HStack(spacing: 12) {
            Text(item.product.emoji)
                .font(.system(size: 28))
                .frame(width: 44, height: 44)
                .background(Color.fcGround, in: RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(item.product.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.fcInk)
                    .lineLimit(1)
                Text("\(item.product.weight) · ₹\(item.product.price) each")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.fcMuted)
            }

            Spacer(minLength: 8)

            VStack(alignment: .trailing, spacing: 6) {
                Text("₹\(item.lineTotal)")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Color.fcInk)
                    .contentTransition(.numericText())
                    .animation(.easeOut(duration: 0.15), value: item.lineTotal)
                QuantityStepper(
                    quantity: item.quantity,
                    onDecrement: { cart.decrement(item.product) },
                    onIncrement: { cart.add(item.product) }
                )
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
    }
}

/// − / count / + pill. Minus at quantity 1 removes the line.
private struct QuantityStepper: View {
    let quantity: Int
    let onDecrement: () -> Void
    let onIncrement: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            stepButton(systemName: "minus", action: onDecrement)
                .accessibilityLabel(quantity > 1 ? "Remove one" : "Remove from cart")
            Text("\(quantity)")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.fcInk)
                .frame(minWidth: 24)
                .contentTransition(.numericText())
                .animation(.easeOut(duration: 0.15), value: quantity)
            stepButton(systemName: "plus", action: onIncrement)
                .accessibilityLabel("Add one")
        }
        .background(Color.fcGround, in: Capsule())
    }

    private func stepButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color.fcAccent)
                .frame(width: 30, height: 28)
        }
        .buttonStyle(PressableButtonStyle())
    }
}
