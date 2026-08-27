# FreshCart — PrintBeam on native Android

A quick-commerce grocery app (Kotlin + Jetpack Compose) that prints order receipts over
the network or Bluetooth LE when you place an order. This is the reference integration of
PrintBeam in a **single-platform Android app**.

## The dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven("https://kotimadduluri.github.io/printbeam-sdk/maven")
    }
}

// app/build.gradle.kts — Android-only projects use the -android artifact
dependencies {
    implementation("dev.printbeam:printbeam-android:0.1.0-alpha03")
}
```

## Where PrintBeam is touched

| What | Where | The pattern |
|---|---|---|
| One-time init | [`FreshCartApp.kt`](app/src/main/java/com/freshcart/FreshCartApp.kt) | `PrintBeam.initialize(PrintBeamConfig(context = PrinterContext(this)))` in `Application.onCreate` — once per process, before any screen |
| Print | [`PrintBeamReceiptPrinter.kt`](app/src/main/java/com/freshcart/printing/PrintBeamReceiptPrinter.kt) | `addManualPrinter(endpoint, name, paperWidth)` → stable id → `PrintBeam.print(id) { …receipt DSL… }`. Transport failures return `PrintResult.Failure`; only invalid receipt content throws. The connection is held between prints |
| Streaming scan | [`SettingsViewModel.kt`](app/src/main/java/com/freshcart/ui/settings/SettingsViewModel.kt) | `PrintBeam.scan(transports, listener)` — results stream into the scan sheet as printers respond; rows keyed by `printer.id` because enrichment re-emits; `ScanHandle.cancel()` on dismiss and `onCleared` |
| Auto-naming | [`SettingsViewModel.kt`](app/src/main/java/com/freshcart/ui/settings/SettingsViewModel.kt) (`resolveNameFromDevice`) | `PrintBeam.queryDeviceInfo(id)` — printers that don't advertise mDNS are picked nameless, then asked for their `GS I` identity (manufacturer + model) over the held session |
| Disconnect | same file | Re-derive the held session's id via `addManualPrinter(oldEndpoint)`, then `PrintBeam.disconnect(id)` |

The SDK is fenced behind a seam: ViewModels depend on the
[`ReceiptPrinter`](app/src/main/java/com/freshcart/printing/ReceiptPrinter.kt) interface,
and only [`PrintBeamReceiptPrinter.kt`](app/src/main/java/com/freshcart/printing/PrintBeamReceiptPrinter.kt)
and the settings/scan feature import `dev.printbeam.*` types.

## Platform notes

- **BLE needs runtime permissions** (`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` on API 31+): the
  SDK's `BluetoothPermissions` helper returns the right array per SDK level.
- Scan callbacks arrive **on the main dispatcher** — safe to touch UI state directly.
- Emulators sit on an isolated NAT network and can't see LAN printers; scan from a physical
  device, or use manual entry with your printer's IP.
- Product art is large emoji — a deliberate, dependency-free placeholder for photography,
  shared across all three FreshCart samples.
- Order numbers come from a persisted SharedPreferences counter and are consumed only when
  a print succeeds, so a failed print retries under the same order number.

## Run

```sh
./gradlew :app:installDebug
```

Add groceries on the **Shop** screen → open the **cart** → **Place Order**. With no printer
configured, that opens **Printer settings** (*Find Printer* scan or *Enter address
manually*); once connected, placing the order prints the receipt and shows the order
number. The printer icon in the Shop top bar opens the same settings screen any time, for
configuring or switching printers.
