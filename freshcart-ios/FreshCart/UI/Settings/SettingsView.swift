import SwiftUI
import PrintBeam

/// Printer setup: scan the LAN for ESC/POS printers or type an address by hand.
/// This screen (plus the scanner bridge) is the SDK-demo core — restyled to the
/// FreshCart tokens but functionally unchanged.
struct SettingsView: View {
    @ObservedObject private var settings = Settings.shared
    @State private var scanner = PrinterScanner()
    @State private var scanning: Bool = false
    @State private var scanResults: [DiscoveredPrinter] = []
    @State private var scanError: String? = nil
    @State private var showScanSheet: Bool = false
    @State private var showManualSheet: Bool = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if !settings.isConfigured {
                    EmptyHero(
                        onScan: { startScan() },
                        onManual: { showManualSheet = true }
                    )
                } else {
                    ConnectedHero(
                        transport: settings.transport,
                        name: settings.printerName,
                        manufacturer: settings.manufacturer,
                        host: settings.host,
                        port: settings.port,
                        bleDeviceId: settings.bleDeviceId,
                        onChange: { startScan() },
                        onDisconnect: { settings.clearSelectedPrinter() }
                    )
                    PaperWidthSection(selection: $settings.paperWidth)
                }
            }
            .padding(16)
        }
        .background(Color.fcGround)
        .navigationTitle("Printer")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.fcGround, for: .navigationBar)
        .sheet(isPresented: $showScanSheet) {
            ScanResultsSheet(
                scanning: scanning,
                results: scanResults,
                error: scanError,
                onPick: { picked in
                    if let net = picked.endpoint as? PrinterEndpoint.Network {
                        settings.saveSelectedPrinter(
                            host: net.host,
                            port: Int(net.port),
                            name: picked.name,
                            manufacturer: picked.manufacturer
                        )
                    } else if let ble = picked.endpoint as? PrinterEndpoint.Ble {
                        settings.saveSelectedBlePrinter(
                            deviceId: ble.deviceId,
                            name: picked.name,
                            manufacturer: picked.manufacturer
                        )
                    }
                    showScanSheet = false
                },
                onRetry: { startScan() },
                onManual: {
                    showScanSheet = false
                    showManualSheet = true
                },
                onDismiss: {
                    scanner.cancel()
                    scanning = false
                    showScanSheet = false
                }
            )
            .tint(Color.fcAccent)
        }
        .sheet(isPresented: $showManualSheet) {
            ManualEntrySheet(
                initialHost: settings.host,
                initialPort: settings.port,
                onSave: { host, port in
                    settings.saveSelectedPrinter(
                        host: host,
                        port: port,
                        name: nil,
                        manufacturer: nil
                    )
                    showManualSheet = false
                },
                onCancel: { showManualSheet = false }
            )
            .tint(Color.fcAccent)
        }
    }

    private func startScan() {
        scanning = true
        scanResults = []
        scanError = nil
        showScanSheet = true
        // PrintBeam.scan streams: printers appear in the sheet as they respond instead of
        // all at once when the window closes. Callbacks land on the main thread.
        scanner.onUpdate = { found in scanResults = found }
        scanner.onFailed = { message in scanError = message }
        scanner.onDone = { found in
            scanResults = found
            scanning = false
        }
        scanner.start()
    }
}

// MARK: - Hero cards

private struct ConnectedHero: View {
    let transport: TransportOption
    let name: String?
    let manufacturer: String?
    let host: String
    let port: Int
    let bleDeviceId: String?
    let onChange: () -> Void
    let onDisconnect: () -> Void

    private var transportLabel: String {
        switch transport {
        case .network: return "WiFi"
        case .ble: return "Bluetooth"
        }
    }

    private var endpointSubtitle: String {
        switch transport {
        case .network: return "\(host) : \(port)"
        case .ble: return bleDeviceId ?? "-"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                StatusBadge(systemName: "checkmark.circle.fill", tint: Color.fcOnAccent, background: Color.fcAccent)
                VStack(alignment: .leading, spacing: 2) {
                    Text("CONNECTED · \(transportLabel)")
                        .font(.system(size: 11, weight: .semibold))
                        .kerning(0.5)
                        .foregroundStyle(Color.fcAccent)
                    Text(name ?? "Manual configuration")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color.fcInk)
                    Text(endpointSubtitle)
                        .font(.system(size: 14))
                        .foregroundStyle(Color.fcMuted)
                    if let manufacturer = manufacturer {
                        Text(manufacturer)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.fcAccent)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.fcAccent.opacity(0.12), in: Capsule())
                            .padding(.top, 4)
                    }
                }
                Spacer()
            }
            HStack(spacing: 10) {
                Button(action: onChange) {
                    Label("Change", systemImage: "arrow.triangle.2.circlepath")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.fcAccent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(Color.fcAccent.opacity(0.1), in: Capsule())
                }
                .buttonStyle(PressableButtonStyle())

                Button(action: onDisconnect) {
                    Label("Disconnect", systemImage: "xmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(Color.red.opacity(0.08), in: Capsule())
                }
                .buttonStyle(PressableButtonStyle())
            }
        }
        .padding(20)
        .fcCard()
    }
}

private struct EmptyHero: View {
    let onScan: () -> Void
    let onManual: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            StatusBadge(
                systemName: "printer",
                tint: Color.fcMuted,
                background: Color.fcGround,
                size: 72
            )
            VStack(spacing: 4) {
                Text("No printer connected")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Color.fcInk)
                Text("Scan for printers on your WiFi network or nearby over Bluetooth to start printing receipts.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.fcMuted)
                    .multilineTextAlignment(.center)
            }
            Button {
                onScan()
            } label: {
                Label("Find Printer", systemImage: "magnifyingglass")
            }
            .buttonStyle(AccentPillButtonStyle())

            Button("Enter IP address manually", action: onManual)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Color.fcAccent)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .fcCard()
    }
}

private struct StatusBadge: View {
    let systemName: String
    let tint: Color
    let background: Color
    var size: CGFloat = 48

    var body: some View {
        ZStack {
            Circle()
                .fill(background)
                .frame(width: size, height: size)
            Image(systemName: systemName)
                .font(.system(size: size * 0.45, weight: .semibold))
                .foregroundStyle(tint)
        }
    }
}

// MARK: - Paper width

private struct PaperWidthSection: View {
    @Binding var selection: PaperWidthOption

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Paper width")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.fcInk)
            Text("The width of the roll loaded in your printer.")
                .font(.system(size: 12))
                .foregroundStyle(Color.fcMuted)
            Picker("Paper width", selection: $selection) {
                ForEach(PaperWidthOption.allCases) { opt in
                    Text(opt.displayName).tag(opt)
                }
            }
            .pickerStyle(.segmented)
            .padding(.top, 4)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .fcCard()
    }
}

// MARK: - Scan sheet

private struct ScanResultsSheet: View {
    let scanning: Bool
    let results: [DiscoveredPrinter]
    let error: String?
    let onPick: (DiscoveredPrinter) -> Void
    let onRetry: () -> Void
    let onManual: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            Group {
                if scanning {
                    VStack(spacing: 16) {
                        ProgressView()
                            .controlSize(.large)
                        Text("Looking for printers…")
                            .font(.headline)
                        Text("Make sure your printer is on and either connected to the same WiFi as this device or in Bluetooth range.")
                            .font(.subheadline)
                            .foregroundStyle(Color.fcMuted)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = error {
                    EmptyState(
                        systemImage: "exclamationmark.triangle.fill",
                        tint: .orange,
                        title: "Something went wrong",
                        message: error,
                        primaryButtonTitle: "Try again",
                        onPrimary: onRetry,
                        secondaryButtonTitle: "Enter manually",
                        onSecondary: onManual
                    )
                } else if results.isEmpty {
                    EmptyState(
                        systemImage: "wifi.exclamationmark",
                        tint: Color.fcMuted,
                        title: "No printers responded",
                        message: "Check that your printer is powered on and either connected to the same WiFi network as this device or in Bluetooth range.",
                        primaryButtonTitle: "Try again",
                        onPrimary: onRetry,
                        secondaryButtonTitle: "Enter manually",
                        onSecondary: onManual
                    )
                } else {
                    List {
                        Section {
                            ForEach(results, id: \.id) { p in
                                Button { onPick(p) } label: {
                                    DiscoveredRow(printer: p)
                                }
                                .buttonStyle(.plain)
                            }
                        } header: {
                            Text("Tap a printer to connect")
                                .textCase(nil)
                        } footer: {
                            Button("Don't see your printer? Enter IP manually", action: onManual)
                                .font(.subheadline)
                                .foregroundStyle(Color.fcAccent)
                                .padding(.vertical, 4)
                        }
                    }
                }
            }
            .navigationTitle(scanning ? "Scanning" : "Choose your printer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close", action: onDismiss)
                }
            }
        }
    }
}

private struct DiscoveredRow: View {
    let printer: DiscoveredPrinter

    private var subtitle: String {
        if let net = printer.endpoint as? PrinterEndpoint.Network {
            return "\(net.host) : \(net.port)"
        }
        if let ble = printer.endpoint as? PrinterEndpoint.Ble {
            return ble.deviceId
        }
        return printer.id
    }

    var body: some View {
        HStack(spacing: 12) {
            StatusBadge(
                systemName: "printer.fill",
                tint: Color.fcAccent,
                background: Color.fcAccent.opacity(0.12),
                size: 40
            )
            VStack(alignment: .leading, spacing: 2) {
                Text(printer.name ?? subtitle)
                    .font(.body)
                if printer.name != nil {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                if let mfr = printer.manufacturer {
                    Text(mfr)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }
}

private struct EmptyState: View {
    let systemImage: String
    let tint: Color
    let title: String
    let message: String
    let primaryButtonTitle: String
    let onPrimary: () -> Void
    let secondaryButtonTitle: String
    let onSecondary: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: systemImage)
                .font(.system(size: 48))
                .foregroundStyle(tint)
            VStack(spacing: 4) {
                Text(title).font(.headline)
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(Color.fcMuted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
            VStack(spacing: 8) {
                Button(primaryButtonTitle, action: onPrimary)
                    .buttonStyle(AccentPillButtonStyle())

                Button(secondaryButtonTitle, action: onSecondary)
                    .font(.subheadline)
                    .foregroundStyle(Color.fcAccent)
            }
            .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Manual entry

private struct ManualEntrySheet: View {
    let initialHost: String
    let initialPort: Int
    let onSave: (String, Int) -> Void
    let onCancel: () -> Void

    @State private var host: String = ""
    @State private var portText: String = "9100"
    @State private var hostError: String? = nil
    @State private var portError: String? = nil

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("IP address", text: $host, prompt: Text("192.168.1.100"))
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                    if let hostError = hostError {
                        Text(hostError).font(.caption).foregroundStyle(.red)
                    }
                    TextField("Port", text: $portText, prompt: Text("9100"))
                        .keyboardType(.numberPad)
                    if let portError = portError {
                        Text(portError).font(.caption).foregroundStyle(.red)
                    }
                } header: {
                    Text("Printer address")
                } footer: {
                    Text("Type the printer's network address. You'll find this on the printer's display or a printed self-test page.")
                }
            }
            .navigationTitle("Enter IP address")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel", action: onCancel)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") { commit() }
                        .fontWeight(.semibold)
                }
            }
            .onAppear {
                host = initialHost
                portText = String(initialPort)
            }
        }
    }

    private func commit() {
        let trimmed = host.trimmingCharacters(in: .whitespaces)
        let port = Int(portText) ?? -1
        hostError = trimmed.isEmpty ? "Required" : nil
        portError = (port < 1 || port > 65535) ? "1-65535" : nil
        if hostError != nil || portError != nil { return }
        onSave(trimmed, port)
    }
}

#Preview {
    NavigationStack { SettingsView() }
}
