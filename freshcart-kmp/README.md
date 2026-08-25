# LabelMate — PrintBeam in Kotlin Multiplatform

A retail price-label app (Compose Multiplatform, Android + iOS from one codebase) printing
labels with EAN-13 barcodes. This is the reference integration of PrintBeam in a **KMP
project**: one dependency in `commonMain`, all printing logic shared, platform code reduced
to a one-line initialization.

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
| Android init | [`MainActivity.kt`](androidApp/src/main/kotlin/com/labelmate/MainActivity.kt) | Guarded once-per-process `PrintBeam.initialize(…)` — activity recreation must **not** re-initialize (it would tear down held connections) |
| iOS init | [`MainViewController.kt`](shared/src/iosMain/kotlin/com/labelmate/MainViewController.kt) | App root runs once per process; `PrinterContext()` takes no arguments on iOS |
| Print (shared!) | [`LabelPrinter.kt`](shared/src/commonMain/kotlin/com/labelmate/data/LabelPrinter.kt) | `addManualPrinter` → `PrintBeam.print(id) { … barcode(ean13, BarcodeType.EAN13) … }` — pure `commonMain`, no expect/actual |
| Scan + disconnect (shared) | [`AppState.kt`](shared/src/commonMain/kotlin/com/labelmate/ui/AppState.kt) | Streaming `ScanListener` updating Compose state directly (callbacks arrive on the main dispatcher); `ScanHandle.cancel()` on dismiss/`onCleared` |

The point of the facade for KMP consumers: after the two one-line platform initializations,
**every remaining PrintBeam call site in this app lives in `commonMain`**.

## Run

```sh
./gradlew :androidApp:installDebug          # Android
open iosApp/iosApp.xcodeproj                # iOS — run from Xcode
```
