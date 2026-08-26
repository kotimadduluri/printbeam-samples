# FreshCart — PrintBeam on native iOS (Swift)

A quick-commerce grocery app (SwiftUI) that prints order receipts to networked ESC/POS
printers. This is the reference integration of PrintBeam in a **pure-Swift iOS app via
Swift Package Manager** — including every Kotlin→Swift bridge detail you'd otherwise
discover the hard way.

The app itself: an 8-product shop (search, sort/category/offers chips, favorites), a cart
with steppers and savings, and a Place Order flow that prints the receipt on a thermal
printer. Product art is deliberately large emoji — a dependency-free stand-in for
photography, shared across all three FreshCart samples. UI spec lives in
[`../DESIGN.md`](../DESIGN.md).

## The dependency

Xcode → File → **Add Package Dependencies…** →

```
https://github.com/kotimadduluri/printbeam-sdk
```

pinned to the exact version (`0.1.0-alpha01`). The package serves a prebuilt XCFramework
(arm64 device + arm64 simulator); nothing else to configure.

## Where PrintBeam is touched

| What | Where | The pattern |
|---|---|---|
| One-time init | [`FreshCartApp.swift`](FreshCart/FreshCartApp.swift) → [`PrinterService.swift`](FreshCart/Printing/PrinterService.swift) | `PrintBeam.shared.initialize(config:)` from the `App` initializer |
| Print | [`ReceiptPrinting.swift`](FreshCart/Printing/ReceiptPrinting.swift) | `try PrintBeam.shared.addManualPrinter(…)` → `try await PrintBeam.shared.print(printerId:block:)` — Kotlin suspend functions arrive as `async`; the receipt DSL closure gets the builder (`align`/`bold`/`text`/`line`/`divider`/`feed`/`cut`) |
| Streaming scan | [`PrinterService.swift`](FreshCart/Printing/PrinterService.swift) (`PrinterScanner`) + [`SettingsView.swift`](FreshCart/UI/Settings/SettingsView.swift) | A Swift class conforming to the Kotlin `ScanListener` interface, streaming results into SwiftUI `@State` |

Everything above the `Printing/` folder stays SDK-free: screens and stores talk to the
`ReceiptPrinting` protocol, and `PrintBeamReceiptPrinter` is the only implementation.
The one exception is the printer-settings flow, which handles `DiscoveredPrinter` and
`PrinterEndpoint` values directly because picking a printer *is* the SDK demo.

## The bridge facts this sample encodes

- Kotlin `object PrintBeam` → **`PrintBeam.shared`** in Swift.
- Kotlin **default arguments don't cross the bridge** — every parameter of
  `PrintBeamConfig` and `addManualPrinter` is spelled out.
- `PrinterLogger.NoOp` is reached as **`PrinterLoggerCompanion.shared.NoOp`**.
- Any file constructing `PrinterContext(externalCentralManager:)` must
  **`import CoreBluetooth`** — without it the initializer is invisible and the compiler
  error ("takes no arguments") is misleading.
- A Swift class conforming to `ScanListener` must inherit **`NSObject`** (Kotlin interfaces
  surface as Obj-C protocols). Callbacks arrive on the **main thread**.
- Sealed results are checked with casts: `if let failure = result as? PrintResult.Failure`.
- Kotlin companion members are reached through the class: `BleProfile.companion.NORDIC_UART`.

## Required Info.plist keys

The scan covers both WiFi and BLE, and iOS gates both behind privacy prompts driven by
[`Info.plist`](FreshCart/Info.plist):

- `NSLocalNetworkUsageDescription` + `NSBonjourServices` (`_pdl-datastream._tcp`,
  `_printer._tcp`, `_ipp._tcp`) — without the service list, iOS 14+ silently blocks the
  mDNS browse.
- `NSBluetoothAlwaysUsageDescription` — without it the app **crashes** the first time the
  SDK creates its `CBCentralManager` for a BLE scan.

The prompts appear on first scan; no permission code is needed on iOS.

## App structure

```
FreshCart/
  FreshCartApp.swift        app entry, PrintBeam init, DI container, light-only lock
  Model/Product.swift       Product, CartItem
  Data/Catalog.swift        product repository + shop filter state (@Observable)
  Data/CartStore.swift      cart source of truth (@Observable)
  Data/OrderFlow.swift      Place Order → print → success/failure phases (@Observable)
  Data/Settings.swift       UserDefaults-backed printer endpoint + order counter
  Printing/                 the only SDK-facing code (see table above)
  UI/                       Theme tokens, Shop / Cart / Settings screens + components
```

Stores are injected through `.environment` from one app-scoped container; views send
explicit events and render a single state. The persisted order counter is consumed only
after a successful print, so a failed receipt retries under the same order number.

## Run

Open `FreshCart.xcodeproj`, select your team under Signing & Capabilities (for device
runs), and press Run. The simulator can print to network printers on your Mac's LAN.
