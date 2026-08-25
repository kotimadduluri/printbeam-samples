package com.labelmate

import androidx.compose.ui.window.ComposeUIViewController
import com.labelmate.data.SettingsStore
import com.labelmate.ui.App
import dev.printbeam.discovery.PrinterContext
import dev.printbeam.sdk.PrintBeam
import dev.printbeam.sdk.PrintBeamConfig

@Suppress("FunctionName", "unused") // called from SwiftUI via Kotlin/Native interop
fun MainViewController() = run {
    // The app root is created once per process, so initializing here is the iOS twin of an
    // Android Application.onCreate. PrinterContext() needs no arguments on iOS.
    PrintBeam.initialize(PrintBeamConfig(context = PrinterContext()))
    ComposeUIViewController { App(SettingsStore()) }
}
