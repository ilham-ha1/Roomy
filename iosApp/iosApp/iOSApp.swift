import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Must be set before the UI starts polling; Kotlin falls back to Dispatchers.Default otherwise.
        RoomyBridge.shared.sensorThread = RoomySensorThread()

        // Hand Kotlin the Swift-only ActivityKit implementation.
        if #available(iOS 16.2, *) {
            RoomyBridge.shared.liveActivity = RoomLiveActivity()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
