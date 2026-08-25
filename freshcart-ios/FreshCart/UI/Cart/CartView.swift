import SwiftUI

struct CartView: View {
    @Environment(CartStore.self) private var cart
    @Environment(OrderFlow.self) private var orderFlow

    let onOpenPrinterSettings: () -> Void
    let onBackToShop: () -> Void

    var body: some View {
        Group {
            if case .success(let orderNumber) = orderFlow.phase {
                OrderSuccessView(orderNumber: orderNumber) {
                    orderFlow.reset()
                    onBackToShop()
                }
            } else if cart.isEmpty {
                EmptyCartView(onBrowse: onBackToShop)
            } else {
                filledCart
            }
        }
        .background(Color.fcGround)
        .navigationTitle("Cart")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.fcGround, for: .navigationBar)
        .onDisappear {
            // Leaving the success screen via the back gesture must not freeze the flow
            // in "success" — the next visit starts from a clean cart, not an old order.
            if case .success = orderFlow.phase { orderFlow.reset() }
        }
    }

    private var filledCart: some View {
        ScrollView {
            VStack(spacing: 16) {
                itemsCard
                CartSummaryCard(
                    itemsTotal: cart.itemsTotal,
                    savings: cart.savings
                )
                if case .failure(let message) = orderFlow.phase {
                    PrintErrorCard(message: message) {
                        Task { await orderFlow.placeOrder(cart: cart) }
                    }
                }
            }
            .padding(16)
        }
        .safeAreaInset(edge: .bottom) { checkoutBar }
    }

    private var itemsCard: some View {
        VStack(spacing: 0) {
            ForEach(cart.items) { item in
                CartLineRow(item: item)
                if item.id != cart.items.last?.id {
                    Divider()
                        .overlay(Color.fcGround)
                        .padding(.leading, 60)
                }
            }
        }
        .padding(.vertical, 4)
        .fcCard()
    }

    private var checkoutBar: some View {
        VStack(spacing: 10) {
            Button {
                placeOrder()
            } label: {
                if orderFlow.isPrinting {
                    HStack(spacing: 10) {
                        ProgressView()
                            .tint(Color.fcOnAccent)
                        Text("Printing receipt…")
                    }
                } else {
                    Text("Place Order")
                }
            }
            .buttonStyle(AccentPillButtonStyle())
            .disabled(orderFlow.isPrinting)
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 8)
        .background(Color.fcGround)
    }

    private func placeOrder() {
        if orderFlow.needsPrinterSetup {
            onOpenPrinterSettings()
        } else {
            Task { await orderFlow.placeOrder(cart: cart) }
        }
    }
}

// MARK: - Summary

private struct CartSummaryCard: View {
    let itemsTotal: Int
    let savings: Int

    var body: some View {
        VStack(spacing: 10) {
            row(label: "Items", value: "₹\(itemsTotal)")
            if savings > 0 {
                row(label: "You saved", value: "₹\(savings)")
            }
            Divider().overlay(Color.fcGround)
            HStack {
                Text("Total")
                    .font(.system(size: 17, weight: .bold))
                Spacer()
                Text("₹\(itemsTotal)")
                    .font(.system(size: 17, weight: .bold))
            }
            .foregroundStyle(Color.fcInk)
        }
        .padding(16)
        .fcCard()
    }

    private func row(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 14))
                .foregroundStyle(Color.fcMuted)
            Spacer()
            Text(value)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.fcInk)
        }
    }
}

// MARK: - Failure

private struct PrintErrorCard: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.orange)
                Text("Couldn't print the receipt")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.fcInk)
            }
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(Color.fcMuted)
                .fixedSize(horizontal: false, vertical: true)
            Button("Retry", action: onRetry)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.fcAccent)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .fcCard()
    }
}

// MARK: - Success

private struct OrderSuccessView: View {
    let orderNumber: Int
    let onBackToShop: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "checkmark")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(Color.fcOnAccent)
                .frame(width: 72, height: 72)
                .background(Color.fcAccent, in: Circle())
            Text("Order #\(String(format: "%03d", orderNumber)) placed")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(Color.fcInk)
            Text("Receipt printed")
                .font(.system(size: 15))
                .foregroundStyle(Color.fcMuted)
            Spacer()
            Button("Back to shop", action: onBackToShop)
                .buttonStyle(AccentPillButtonStyle())
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
        }
        .navigationBarBackButtonHidden()
    }
}

// MARK: - Empty

private struct EmptyCartView: View {
    let onBrowse: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Spacer()
            Text("🛒")
                .font(.system(size: 56))
            Text("Your cart is empty")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Color.fcInk)
            Text("Fresh fruit and veg are two taps away.")
                .font(.system(size: 14))
                .foregroundStyle(Color.fcMuted)
            Spacer()
            Button("Browse groceries", action: onBrowse)
                .buttonStyle(AccentPillButtonStyle())
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
        }
    }
}

#Preview {
    NavigationStack {
        CartView(onOpenPrinterSettings: {}, onBackToShop: {})
    }
    .environment(CartStore())
    .environment(OrderFlow(printer: PrintBeamReceiptPrinter()))
}
