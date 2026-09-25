import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
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
