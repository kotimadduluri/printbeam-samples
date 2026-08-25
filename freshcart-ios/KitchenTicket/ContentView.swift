import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationStack {
            OrderView()
                .navigationTitle("New Order")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        NavigationLink {
                            SettingsView()
                                .navigationTitle("Settings")
                        } label: {
                            Image(systemName: "gearshape")
                        }
                    }
                }
        }
    }
}

#Preview {
    ContentView()
}
