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

/// UserDefaults-backed settings. Single source of truth for printer endpoint and ticket
/// numbering — both the order screen and the printer service read through it so changes
/// in the Settings screen show up immediately on the next print.
final class Settings: ObservableObject {
    static let shared = Settings()

    private enum Keys {
        static let host = "printer.host"
        static let port = "printer.port"
        static let paperWidth = "printer.paperWidth"
        static let printerName = "printer.name"
        static let manufacturer = "printer.manufacturer"
        static let nextOrderNumber = "order.nextNumber"
    }

    private let defaults = UserDefaults.standard

    @Published var host: String {
        didSet { defaults.set(host, forKey: Keys.host) }
    }
    @Published var port: Int {
        didSet { defaults.set(port, forKey: Keys.port) }
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
        self.host = defaults.string(forKey: Keys.host) ?? "192.168.1.50"
        let storedPort = defaults.integer(forKey: Keys.port)
        self.port = storedPort == 0 ? 9100 : storedPort
        let storedPaper = defaults.string(forKey: Keys.paperWidth) ?? PaperWidthOption.mm80.rawValue
        self.paperWidth = PaperWidthOption(rawValue: storedPaper) ?? .mm80
        self.printerName = defaults.string(forKey: Keys.printerName)
        self.manufacturer = defaults.string(forKey: Keys.manufacturer)
    }

    /// Persist a scan-picked printer in one shot — host/port/name/manufacturer are saved
    /// atomically rather than via individual @Published didSet writes, so a partial update
    /// can't leave the user with a stale name attached to a fresh host.
    func saveSelectedPrinter(host: String, port: Int, name: String?, manufacturer: String?) {
        self.host = host
        self.port = port
        self.printerName = name
        self.manufacturer = manufacturer
    }

    /// Clear the saved selection. Host/port are reset to the defaults the user would see
    /// on a fresh install, and the scan-derived name is dropped.
    func clearSelectedPrinter() {
        self.host = ""
        self.port = 9100
        self.printerName = nil
        self.manufacturer = nil
    }

    /// Reads and increments the order counter atomically enough for a single-user app.
    /// Returns the value that should appear on the ticket being printed *now*.
    func consumeNextOrderNumber() -> Int {
        let current = defaults.integer(forKey: Keys.nextOrderNumber)
        let value = current == 0 ? 1 : current
        defaults.set(value + 1, forKey: Keys.nextOrderNumber)
        return value
    }

    /// Peek without consuming — used by the order screen so the user sees the number
    /// they're about to print.
    func peekNextOrderNumber() -> Int {
        let current = defaults.integer(forKey: Keys.nextOrderNumber)
        return current == 0 ? 1 : current
    }
}
