# KitchenTicket — PrintBeam on native iOS (Swift)

A restaurant kitchen-ticket app (SwiftUI) printing to networked ESC/POS printers. This is
the reference integration of PrintBeam in a **pure-Swift iOS app via Swift Package Manager**
— including every Kotlin→Swift bridge detail you'd otherwise discover the hard way.

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
| One-time init | [`KitchenTicketApp.swift`](KitchenTicket/KitchenTicketApp.swift) → [`PrinterService.swift`](KitchenTicket/PrinterService.swift) | `PrintBeam.shared.initialize(config:)` from the `App` initializer |
| Print | [`PrinterService.swift`](KitchenTicket/PrinterService.swift) | `try PrintBeam.shared.addManualPrinter(…)` → `try await PrintBeam.shared.print(printerId:block:)` — Kotlin suspend functions arrive as `async`; the receipt DSL closure gets the builder |
| Streaming scan | [`PrinterService.swift`](KitchenTicket/PrinterService.swift) (`PrinterScanner`) + [`SettingsView.swift`](KitchenTicket/SettingsView.swift) | A Swift class conforming to the Kotlin `ScanListener` interface, streaming results into SwiftUI `@State` |

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

## Run

Open `KitchenTicket.xcodeproj`, select your team under Signing & Capabilities (for device
runs), and press Run. The simulator can print to network printers on your Mac's LAN.
