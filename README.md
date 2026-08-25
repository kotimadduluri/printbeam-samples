# PrintBeam Samples

Official integration samples for **[PrintBeam](https://github.com/kotimadduluri/printbeam-sdk)** —
the ESC/POS thermal printer SDK for Android, iOS, and Kotlin Multiplatform. Each app is a
complete, runnable integration of one consumption scenario, built against the published SDK
exactly the way your project would.

[![CI](https://github.com/kotimadduluri/printbeam-samples/actions/workflows/ci.yml/badge.svg)](https://github.com/kotimadduluri/printbeam-samples/actions/workflows/ci.yml)
[![SDK version](https://img.shields.io/github/v/release/kotimadduluri/printbeam-sdk?include_prereleases&label=PrintBeam)](https://github.com/kotimadduluri/printbeam-sdk/releases)

## Pick your scenario

| Your project is… | Sample | Gets PrintBeam via | Shows |
|---|---|---|---|
| **Native Android** (Kotlin, Compose) | [`brewlog-android/`](brewlog-android/) | Maven (`printbeam-android`) | Café POS: streaming scan, WiFi + BLE printing, held sessions, cash-drawer-free receipt flow |
| **Native iOS** (Swift, SwiftUI) | [`kitchenticket-ios/`](kitchenticket-ios/) | Swift Package Manager | Kitchen tickets: the Kotlin→Swift bridge done right (`try await`, `ScanListener` from Swift) |
| **Kotlin Multiplatform** (Compose MP) | [`labelmate-kmp/`](labelmate-kmp/) | Maven (`printbeam`) | Price labels with EAN-13 barcodes: all printing code in `commonMain`, one UI on two OSes |

Each sample's README lists the exact files where PrintBeam is touched — dependency,
initialization, scan, print, disconnect — so you can lift the pattern without reading the
whole app.

## Prerequisites

- An ESC/POS thermal printer: **networked** (WiFi/Ethernet, TCP port 9100) or **BLE**.
  No printer? Every app still runs — use manual entry to explore the API; prints fail
  gracefully with a real error message.
- Android samples: Android Studio (or just Gradle + an Android SDK), a device or emulator
  (note: emulators can't reach printers on your LAN — use a physical device to print).
- iOS sample: Xcode 16+, an iPhone or simulator (simulator can print to *network* printers
  on your Mac's LAN; BLE needs a physical device).

## Build everything

```sh
(cd brewlog-android   && ./gradlew :app:assembleDebug)
(cd labelmate-kmp     && ./gradlew :androidApp:assembleDebug)
(cd kitchenticket-ios && xcodebuild -project KitchenTicket.xcodeproj -scheme KitchenTicket \
    -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build)
```

No credentials are needed anywhere: the SDK resolves from
`https://kotimadduluri.github.io/printbeam-sdk/maven` (Gradle) and from the
`printbeam-sdk` repo's releases (SPM).

## License

The samples are [MIT licensed](LICENSE) — copy anything into your own app. The PrintBeam SDK
itself is free to use under the
[PrintBeam SDK License](https://github.com/kotimadduluri/printbeam-sdk/blob/main/LICENSE.md).
