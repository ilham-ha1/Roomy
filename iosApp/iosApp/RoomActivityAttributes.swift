import ActivityKit

/// Shared by the app (starts the activity) and the RoomyWidget extension (draws it).
/// Must be a member of both targets.
struct RoomActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var celsius: Double
        var comfortLabel: String
    }

    var sensorId: String
}
