import SwiftUI

struct OrderView: View {
    @State private var tableNumber: String = ""
    @State private var itemsText: String = ""
    @State private var isPrinting = false
    @State private var alertTitle = ""
    @State private var alertMessage = ""
    @State private var showAlert = false
    @State private var displayedOrderNumber: Int = Settings.shared.peekNextOrderNumber()

    private let service = PrinterService()

    var body: some View {
        Form {
            Section("Order") {
                HStack {
                    Text("Order #")
                    Spacer()
                    Text(String(format: "%03d", displayedOrderNumber))
                        .monospacedDigit()
                        .foregroundStyle(.secondary)
                }
                TextField("Table number", text: $tableNumber)
                    .keyboardType(.numberPad)
            }

            Section("Items (one per line)") {
                TextEditor(text: $itemsText)
                    .frame(minHeight: 140)
                    .autocorrectionDisabled()
            }

            Section {
                Button(action: print) {
                    if isPrinting {
                        HStack {
                            ProgressView()
                            Text("Printing…")
                        }
                    } else {
                        Text("Print Ticket")
                    }
                }
                .disabled(isPrinting || tableNumber.isEmpty || itemsText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .alert(alertTitle, isPresented: $showAlert) {
            Button("OK") { }
        } message: {
            Text(alertMessage)
        }
    }

    private func print() {
        let items = itemsText
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
        let orderNumber = Settings.shared.consumeNextOrderNumber()
        let table = tableNumber
        isPrinting = true
        Task {
            do {
                try await service.printKitchenTicket(
                    tableNumber: table,
                    orderNumber: orderNumber,
                    items: items
                )
                await MainActor.run {
                    alertTitle = "Sent"
                    alertMessage = "Order #\(String(format: "%03d", orderNumber)) sent to the kitchen printer."
                    showAlert = true
                    tableNumber = ""
                    itemsText = ""
                    displayedOrderNumber = Settings.shared.peekNextOrderNumber()
                    isPrinting = false
                }
            } catch {
                await MainActor.run {
                    alertTitle = "Print failed"
                    alertMessage = error.localizedDescription
                    showAlert = true
                    isPrinting = false
                }
            }
        }
    }
}

#Preview {
    NavigationStack { OrderView() }
}
