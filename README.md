# PrintBeam Samples — FreshCart, three ways

Official integration samples for **[PrintBeam](https://github.com/kotimadduluri/printbeam-sdk)**,
the ESC/POS thermal printer SDK for Android, iOS, and Kotlin Multiplatform.

All three apps are the same product: *FreshCart*, a quick-commerce grocery shop.
You browse the catalog, add to cart, and place an order. The receipt prints on a
real thermal printer through PrintBeam. Same design, same catalog, same receipt,
three stacks. Compare any screen across the three apps to see the same
integration in each ecosystem.

[![CI](https://github.com/kotimadduluri/printbeam-samples/actions/workflows/ci.yml/badge.svg)](https://github.com/kotimadduluri/printbeam-samples/actions/workflows/ci.yml)
[![SDK version](https://img.shields.io/github/v/release/kotimadduluri/printbeam-sdk?include_prereleases&label=PrintBeam)](https://github.com/kotimadduluri/printbeam-sdk/releases)

## Pick your stack

| Your project is… | Sample | Gets PrintBeam via |
|---|---|---|
| **Native Android** (Kotlin, Jetpack Compose, Material 3) | [`freshcart-android/`](freshcart-android/) | Maven (`printbeam-android`) |
| **Native iOS** (Swift, SwiftUI, `@Observable`) | [`freshcart-ios/`](freshcart-ios/) | Swift Package Manager |
| **Kotlin Multiplatform** (Compose Multiplatform) | [`freshcart-kmp/`](freshcart-kmp/) | Maven (`printbeam`) |

Each sample's README lists the exact files where PrintBeam is used: dependency,
initialization, scan, and print. You can lift the pattern without reading the
whole app. The shared design system, catalog, receipt format, and architecture
rules live in [`DESIGN.md`](DESIGN.md).

## Prerequisites

- An ESC/POS thermal printer, either **network** (TCP port 9100) or **BLE**.
  No printer? The apps still run. Browse, cart, and checkout all work, and the
  print step fails gracefully with a real error message.
- Android samples: Android Studio, or Gradle plus the Android SDK. Print from a
  physical device; emulators can't reach LAN printers.
- iOS sample: Xcode 16+. The simulator can print to network printers on your
  Mac's LAN.

## Build everything

```sh
(cd freshcart-android && ./gradlew :app:assembleDebug)
(cd freshcart-kmp     && ./gradlew :androidApp:assembleDebug)
(cd freshcart-ios     && xcodebuild -project FreshCart.xcodeproj -scheme FreshCart \
    -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build)
```

No credentials are needed. Gradle resolves the SDK from
`https://kotimadduluri.github.io/printbeam-sdk/maven`, and SPM resolves it from
the `printbeam-sdk` repo's releases.

## License

The samples are [MIT licensed](LICENSE), so copy anything into your own app.
The PrintBeam SDK itself is free to use under the
[PrintBeam SDK License](https://github.com/kotimadduluri/printbeam-sdk/blob/main/LICENSE.md).
