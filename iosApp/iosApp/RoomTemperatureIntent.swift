import AppIntents
import ComposeApp

/// "Hey Siri, room temperature in Roomy". Data and wording come from Kotlin.
@available(iOS 16.0, *)
struct RoomTemperatureIntent: AppIntent {
    static var title: LocalizedStringResource = "Room temperature"
    static var description = IntentDescription("Reads the current room temperature.")

    @MainActor // Kotlin suspend functions must be called from the main thread (IOS_NOTES.md §4).
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let room = try await RoomyBridge.shared.currentRoom()
        let celsius = room.celsius.formatted(.number.precision(.fractionLength(1)))
        return .result(dialog: "\(celsius)°C, \(room.comfortLabel)")
    }
}

/// Makes the intent show up in Siri and Shortcuts without any setup by the user.
@available(iOS 16.0, *)
struct RoomyShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: RoomTemperatureIntent(),
            phrases: [
                "Room temperature in \(.applicationName)",
                "How warm is it in \(.applicationName)",
            ],
            shortTitle: "Room temperature",
            systemImageName: "thermometer.medium"
        )
    }
}
