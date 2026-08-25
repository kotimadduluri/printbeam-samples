package com.freshcart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.freshcart.data.SettingsStore
import com.freshcart.ui.App
import dev.printbeam.discovery.PrinterContext
import dev.printbeam.sdk.PrintBeam
import dev.printbeam.sdk.PrintBeamConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FreshCartPrinting.ensureInitialized(applicationContext)
        setContent { App(SettingsStore(applicationContext), supportsBleManual = true) }
    }
}

/**
 * Process-wide PrintBeam bootstrap. Guarded because activity recreation (rotation, theme
 * change) re-runs onCreate, and PrintBeam.initialize replaces the facade wholesale — the guard
 * keeps held printer connections alive across configuration changes.
 */
private object FreshCartPrinting {
    private var initialized = false

    fun ensureInitialized(appContext: android.content.Context) {
        if (initialized) return
        PrintBeam.initialize(PrintBeamConfig(context = PrinterContext(appContext)))
        initialized = true
    }
}
