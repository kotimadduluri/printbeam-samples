import SwiftUI

@main
struct KitchenTicketApp: App {
    init() {
        // One-time facade setup — everything else in the app just calls PrintBeam.shared.
        PrinterService.initializeLibrary()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
