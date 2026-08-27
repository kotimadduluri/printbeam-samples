# FreshCart — PrintBeam on native Android

A quick-commerce grocery app built with Kotlin and Jetpack Compose. When you
place an order, it prints the receipt over the network or BLE. This is the
reference integration of PrintBeam in a single-platform Android app.

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
| One-time init | [`FreshCartApp.kt`](app/src/main/java/com/freshcart/FreshCartApp.kt) | `PrintBeam.initialize(PrintBeamConfig(context = PrinterContext(this)))` in `Application.onCreate`. Runs once per process, before any screen. |
| Print | [`PrintBeamReceiptPrinter.kt`](app/src/main/java/com/freshcart/printing/PrintBeamReceiptPrinter.kt) | `addManualPrinter(endpoint, name, paperWidth)` returns a stable id, then `PrintBeam.print(id) { …receipt DSL… }`. Transport failures return `PrintResult.Failure`; only invalid receipt content throws. The connection is held between prints. |
| Streaming scan | [`SettingsViewModel.kt`](app/src/main/java/com/freshcart/ui/settings/SettingsViewModel.kt) | `PrintBeam.scan(transports, listener)`. Results stream into the scan sheet as printers respond. Rows are keyed by `printer.id` because enrichment re-emits. Call `ScanHandle.cancel()` on dismiss and in `onCleared`. |
| Auto-naming | [`SettingsViewModel.kt`](app/src/main/java/com/freshcart/ui/settings/SettingsViewModel.kt) (`resolveNameFromDevice`) | `PrintBeam.queryDeviceInfo(id)`. Since alpha03 the SDK names network printers in the scan list itself, using `GS I` during discovery. This call is the fallback for a printer picked or entered without a name: query its identity over the held session, then re-register the resolved name. |
| Disconnect | same file | Re-derive the held session's id with `addManualPrinter(oldEndpoint)`, then call `PrintBeam.disconnect(id)`. |

The SDK sits behind a seam. ViewModels depend on the
[`ReceiptPrinter`](app/src/main/java/com/freshcart/printing/ReceiptPrinter.kt) interface.
Only [`PrintBeamReceiptPrinter.kt`](app/src/main/java/com/freshcart/printing/PrintBeamReceiptPrinter.kt)
and the settings/scan feature import `dev.printbeam.*` types.

## Platform notes

- BLE needs runtime permissions on API 31+ (`BLUETOOTH_SCAN` and
  `BLUETOOTH_CONNECT`). The SDK's `BluetoothPermissions` helper returns the
  right array for each SDK level.
- Scan callbacks arrive on the main dispatcher, so it is safe to touch UI state
  directly.
- Emulators sit on an isolated NAT network and can't see LAN printers. Scan
  from a physical device, or enter your printer's IP manually.
- Product art is large emoji. It is a dependency-free placeholder for
  photography, shared across all three FreshCart samples.
- Order numbers come from a persisted SharedPreferences counter. The counter is
  consumed only when a print succeeds, so a failed print retries under the same
  order number.

## Run

```sh
./gradlew :app:installDebug
```

Add groceries on the **Shop** screen, open the **cart**, and tap **Place
Order**. With no printer configured, that opens **Printer settings**, where you
can scan with *Find Printer* or use *Enter address manually*. Once connected,
placing the order prints the receipt and shows the order number. The printer
icon in the Shop top bar opens the same settings screen any time, so you can
configure or switch printers.
