package com.brewlog.pos

import android.app.Application
import dev.printbeam.discovery.PrinterContext
import dev.printbeam.sdk.PrintBeam
import dev.printbeam.sdk.PrintBeamConfig

/**
 * Initializes the PrintBeam facade once for the whole process. Every screen then talks to
 * [PrintBeam] directly — no printer objects or platform handles threaded through ViewModels.
 */
class BrewLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PrintBeam.initialize(PrintBeamConfig(context = PrinterContext(this)))
    }
}
