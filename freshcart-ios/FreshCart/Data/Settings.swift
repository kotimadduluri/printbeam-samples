import Foundation

/// User-facing paper-roll widths. Mirrors `PaperWidth` enum cases the demo cares about.
enum PaperWidthOption: String, CaseIterable, Identifiable {
    case mm58
    case mm80

    var id: String { rawValue }
    var displayName: String {
        switch self {
        case .mm58: return "58 mm"
        case .mm80: return "80 mm"
        }
    }
}

/// How the saved printer is reached. Mirrors PrintBeam's `Transport` without leaking the
/// SDK type into views and stores.
enum TransportOption: String {
    case network
    case ble
}

/// What kinds of printer a scan should look for. Drives the segmented control at the top
/// of the scan sheet; the mapping to SDK transports lives with the scanner so this store
/// stays PrintBeam-free.
enum ScanScopeOption: String, CaseIterable, Identifiable {
    case all
    case network
    case ble

    var id: String { rawValue }
    var displayName: String {
        switch self {
        case .all: return "All"
        case .network: return "Network"
        case .ble: return "Bluetooth"
        }
    }
}

/// UserDefaults-backed settings. Single source of truth for printer endpoint and order
/// numbering — both the order flow and the receipt printer read through it so changes
/// in the Settings screen show up immediately on the next print.
final class Settings: ObservableObject {
    static let shared = Settings()

    private enum Keys {
        static let transport = "printer.transport"
        static let host = "printer.host"
        static let port = "printer.port"
        static let bleDeviceId = "printer.bleDeviceId"
        static let scanScope = "printer.scanScope"
        static let paperWidth = "printer.paperWidth"
        static let printerName = "printer.name"
        static let manufacturer = "printer.manufacturer"
        static let nextOrderNumber = "order.nextNumber"
    }

    private let defaults = UserDefaults.standard

    @Published var transport: TransportOption {
        didSet { defaults.set(transport.rawValue, forKey: Keys.transport) }
    }
    @Published var host: String {
        didSet { defaults.set(host, forKey: Keys.host) }
    }
    @Published var port: Int {
        didSet { defaults.set(port, forKey: Keys.port) }
    }
    @Published var bleDeviceId: String? {
        didSet { defaults.set(bleDeviceId, forKey: Keys.bleDeviceId) }
    }
    @Published var scanScope: ScanScopeOption {
        didSet { defaults.set(scanScope.rawValue, forKey: Keys.scanScope) }
    }
    @Published var paperWidth: PaperWidthOption {
        didSet { defaults.set(paperWidth.rawValue, forKey: Keys.paperWidth) }
    }
    @Published var printerName: String? {
        didSet { defaults.set(printerName, forKey: Keys.printerName) }
    }
    @Published var manufacturer: String? {
        didSet { defaults.set(manufacturer, forKey: Keys.manufacturer) }
    }

    private init() {
        let storedTransport = defaults.string(forKey: Keys.transport) ?? TransportOption.network.rawValue
        self.transport = TransportOption(rawValue: storedTransport) ?? .network
        self.bleDeviceId = defaults.string(forKey: Keys.bleDeviceId)
        let storedScope = defaults.string(forKey: Keys.scanScope) ?? ScanScopeOption.all.rawValue
        self.scanScope = ScanScopeOption(rawValue: storedScope) ?? .all
        self.host = defaults.string(forKey: Keys.host) ?? "192.168.1.50"
        let storedPort = defaults.integer(forKey: Keys.port)
        self.port = storedPort == 0 ? 9100 : storedPort
        let storedPaper = defaults.string(forKey: Keys.paperWidth) ?? PaperWidthOption.mm80.rawValue
        self.paperWidth = PaperWidthOption(rawValue: storedPaper) ?? .mm80
        self.printerName = defaults.string(forKey: Keys.printerName)
        self.manufacturer = defaults.string(forKey: Keys.manufacturer)
    }

    /// True when a printer endpoint is saved and printable — a host for network, a device
    /// id for BLE. The order flow and the settings hero both key off this.
    var isConfigured: Bool {
        switch transport {
        case .network: return !host.isEmpty
        case .ble: return !(bleDeviceId ?? "").isEmpty
        }
    }

    /// Persist a scan-picked network printer in one shot — every field is saved together
    /// rather than via individual @Published didSet writes, so a partial update can't
    /// leave the user with a stale name attached to a fresh host.
    func saveSelectedPrinter(host: String, port: Int, name: String?, manufacturer: String?) {
        self.transport = .network
        self.host = host
        self.port = port
        self.bleDeviceId = nil
        self.printerName = name
        self.manufacturer = manufacturer
    }

    /// Persist a scan-picked BLE printer. The device id is CoreBluetooth's per-device UUID —
    /// only obtainable by scanning, which is why BLE has no manual-entry path on iOS.
    func saveSelectedBlePrinter(deviceId: String, name: String?, manufacturer: String?) {
        self.transport = .ble
        self.bleDeviceId = deviceId
        self.host = ""
        self.printerName = name
        self.manufacturer = manufacturer
    }

    /// Clear the saved selection. Host/port are reset to the defaults the user would see
    /// on a fresh install, and the scan-derived name is dropped.
    func clearSelectedPrinter() {
        self.transport = .network
        self.host = ""
        self.port = 9100
        self.bleDeviceId = nil
        self.printerName = nil
        self.manufacturer = nil
    }

    /// Reads and increments the order counter atomically enough for a single-user app.
    /// Returns the value that should appear on the receipt being printed *now*.
    func consumeNextOrderNumber() -> Int {
        let current = defaults.integer(forKey: Keys.nextOrderNumber)
        let value = current == 0 ? 1 : current
        defaults.set(value + 1, forKey: Keys.nextOrderNumber)
        return value
    }

    /// Peek without consuming — the order flow prints with the peeked number and only
    /// consumes it once the print succeeds, so a failed print retries the same number.
    func peekNextOrderNumber() -> Int {
        let current = defaults.integer(forKey: Keys.nextOrderNumber)
        return current == 0 ? 1 : current
    }
}
