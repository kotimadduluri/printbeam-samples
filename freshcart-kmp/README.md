# FreshCart — PrintBeam in Kotlin Multiplatform

A quick-commerce grocery app (Compose Multiplatform, Android + iOS from one codebase) that
prints an ESC/POS order receipt when you place an order. This is the reference integration of
PrintBeam in a **KMP project**: one dependency in `commonMain`, all printing logic shared,
platform code reduced to a one-line initialization.

Product images are large emoji — a deliberate, dependency-free placeholder for photography.

## The dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven("https://kotimadduluri.github.io/printbeam-sdk/maven")
    }
}

// shared/build.gradle.kts — commonMain; Gradle picks the right variant per target
commonMain.dependencies {
    api("dev.printbeam:printbeam:0.1.0-alpha01")
}
```

## Where PrintBeam is touched

| What | Where | The pattern |
|---|---|---|
| Android init | [`MainActivity.kt`](androidApp/src/main/kotlin/com/freshcart/MainActivity.kt) | Guarded once-per-process `PrintBeam.initialize(…)` — activity recreation must **not** re-initialize (it would tear down held connections) |
| iOS init | [`MainViewController.kt`](shared/src/iosMain/kotlin/com/freshcart/MainViewController.kt) | App root runs once per process; `PrinterContext()` takes no arguments on iOS |
| Print (shared!) | [`PrintBeamReceiptPrinter.kt`](shared/src/commonMain/kotlin/com/freshcart/printing/PrintBeamReceiptPrinter.kt) | `addManualPrinter` → `PrintBeam.print(id) { … line(left, right) … divider("-") … cut() }` — pure `commonMain`, no expect/actual. Sole implementation of the app's `ReceiptPrinter` seam, so ViewModels never see PrintBeam types |
| Scan + disconnect (shared) | [`SettingsViewModel.kt`](shared/src/commonMain/kotlin/com/freshcart/ui/SettingsViewModel.kt) | Streaming `ScanListener` updating a `StateFlow` UiState directly (callbacks arrive on the main dispatcher); `ScanHandle.cancel()` on dismiss/`onCleared`; `PrintBeam.disconnect(addManualPrinter(endpoint))` to close the held session on printer change |

The point of the facade for KMP consumers: after the two one-line platform initializations,
**every remaining PrintBeam call site in this app lives in `commonMain`**.

## The app

- **Shop** — searchable, filterable 2-column grocery grid (8 products); add-to-cart with a
  live badge count.
- **Cart** — quantity steppers, savings line, and a **Place Order** CTA that prints the
  receipt: printing / success / failure states, order numbers from a persisted counter
  (SharedPreferences / NSUserDefaults) consumed only on a successful print.
- **Printer settings** — streaming scan in a bottom sheet with an All / Network / Bluetooth
  scope control (persisted), manual IP (and, on Android,
  BLE MAC) fallback; paper width selection. Android gates the scan behind the SDK's
  `BluetoothPermissions` runtime set via an expect/actual seam; iOS declares
  `NSBluetoothAlwaysUsageDescription` + `NSBonjourServices` in `iosApp/iosApp/Info.plist`. Always reachable from the printer icon in the Shop top
  bar; placing an order without a configured printer lands here too.

All screens, ViewModels, theme, and printing logic are `commonMain` Compose Multiplatform;
the platform source sets contain only the entry points and the `SettingsStore` actuals.

## Run

```sh
./gradlew :androidApp:installDebug          # Android
open iosApp/iosApp.xcodeproj                # iOS — run from Xcode
```
