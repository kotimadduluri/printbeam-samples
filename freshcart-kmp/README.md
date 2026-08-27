# FreshCart — PrintBeam in Kotlin Multiplatform

A quick-commerce grocery app built with Compose Multiplatform, targeting
Android and iOS from one codebase. When you place an order, it prints an
ESC/POS receipt. This is the reference integration of PrintBeam in a KMP
project: one dependency in `commonMain`, all printing logic shared, and
platform code reduced to a one-line initialization.

Product images are large emoji, a dependency-free placeholder for photography.

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
    api("dev.printbeam:printbeam:0.1.0-alpha03")
}
```

## Where PrintBeam is touched

| What | Where | The pattern |
|---|---|---|
| Android init | [`MainActivity.kt`](androidApp/src/main/kotlin/com/freshcart/MainActivity.kt) | A guarded, once-per-process `PrintBeam.initialize(…)`. Activity recreation must **not** re-initialize; that would tear down held connections. |
| iOS init | [`MainViewController.kt`](shared/src/iosMain/kotlin/com/freshcart/MainViewController.kt) | The app root runs once per process. `PrinterContext()` takes no arguments on iOS. |
| Print (shared!) | [`PrintBeamReceiptPrinter.kt`](shared/src/commonMain/kotlin/com/freshcart/printing/PrintBeamReceiptPrinter.kt) | `addManualPrinter`, then `PrintBeam.print(id) { … line(left, right) … divider("-") … cut() }`. Pure `commonMain`, no expect/actual. This is the sole implementation of the app's `ReceiptPrinter` seam, so ViewModels never see PrintBeam types. |
| Scan + disconnect (shared) | [`SettingsViewModel.kt`](shared/src/commonMain/kotlin/com/freshcart/ui/SettingsViewModel.kt) | A streaming `ScanListener` updates a `StateFlow` UiState directly; callbacks arrive on the main dispatcher. Call `ScanHandle.cancel()` on dismiss and in `onCleared`. On printer change, `PrintBeam.disconnect(addManualPrinter(endpoint))` closes the held session. |

After the two one-line platform initializations, every remaining PrintBeam
call site in this app lives in `commonMain`.

## The app

- **Shop** — a searchable, filterable 2-column grocery grid (8 products), with
  add-to-cart and a live badge count.
- **Cart** — quantity steppers, a savings line, and a **Place Order** button
  that prints the receipt, with printing / success / failure states. Order
  numbers come from a persisted counter (SharedPreferences on Android,
  NSUserDefaults on iOS) and are consumed only on a successful print.
- **Printer settings** — a streaming scan in a bottom sheet, with a persisted
  All / Network / Bluetooth scope control. Since alpha03 the SDK names network
  printers in the scan list itself, by sending `GS I` during discovery. A
  printer that still arrives nameless is asked for its identity via
  `PrintBeam.queryDeviceInfo` after picking (`resolveNameFromDevice` in the
  shared `SettingsViewModel`), and the resolved name is re-registered. Manual
  fallback: enter an IP, or on Android a BLE MAC. Paper width is selectable.
  Android gates the scan behind the SDK's `BluetoothPermissions` runtime set
  via an expect/actual seam. iOS declares `NSBluetoothAlwaysUsageDescription`
  and `NSBonjourServices` in `iosApp/iosApp/Info.plist`. The screen is always
  reachable from the printer icon in the Shop top bar, and placing an order
  without a configured printer lands here too.

All screens, ViewModels, theme, and printing logic are `commonMain` Compose
Multiplatform. The platform source sets contain only the entry points and the
`SettingsStore` actuals.

## Run

```sh
./gradlew :androidApp:installDebug          # Android
open iosApp/iosApp.xcodeproj                # iOS — run from Xcode
```
