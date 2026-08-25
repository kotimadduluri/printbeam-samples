# BrewLog POS — PrintBeam on native Android

A café point-of-sale app (Kotlin + Jetpack Compose) that prints customer receipts over WiFi
or Bluetooth LE. This is the reference integration of PrintBeam in a **single-platform
Android app**.

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
    implementation("dev.printbeam:printbeam-android:0.1.0-alpha01")
}
```

## Where PrintBeam is touched

| What | Where | The pattern |
|---|---|---|
| One-time init | [`BrewLogApp.kt`](app/src/main/java/com/brewlog/pos/BrewLogApp.kt) | `PrintBeam.initialize(PrintBeamConfig(context = PrinterContext(this)))` in `Application.onCreate` — once per process, before any screen |
| Print | [`OrderViewModel.kt`](app/src/main/java/com/brewlog/pos/ui/OrderViewModel.kt) | `addManualPrinter(endpoint, name, paperWidth)` → stable id → `PrintBeam.print(id) { …receipt DSL… }`. Transport failures return `PrintResult.Failure`; only invalid receipt content throws. The connection is held between prints |
| Streaming scan | [`SettingsViewModel.kt`](app/src/main/java/com/brewlog/pos/ui/SettingsViewModel.kt) | `PrintBeam.scan(transports, listener)` — results stream into the dialog as printers respond; rows keyed by `printer.id` because enrichment re-emits; `ScanHandle.cancel()` on dismiss and `onCleared` |
| Disconnect | same file | Re-derive the held session's id via `addManualPrinter(oldEndpoint)`, then `PrintBeam.disconnect(id)` |

## Platform notes

- **BLE needs runtime permissions** (`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` on API 31+): the
  SDK's `BluetoothPermissions` helper returns the right array per SDK level.
- Scan callbacks arrive **on the main dispatcher** — safe to touch UI state directly.
- Emulators sit on an isolated NAT network and can't see LAN printers; scan from a physical
  device, or use manual entry with your printer's IP.

## Run

```sh
./gradlew :app:installDebug
```

Open **Settings** in the app → *Find printer* (scan) or *Enter manually* → back to the order
screen → add items → **Print receipt**.
