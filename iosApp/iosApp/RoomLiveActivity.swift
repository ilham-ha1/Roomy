import ActivityKit
import ComposeApp

/// Swift side of Kotlin's `LiveActivityController`. Kotlin decides *when* (room is HOT);
/// this class only knows *how* to talk to ActivityKit.
@available(iOS 16.2, *)
final class RoomLiveActivity: NSObject, LiveActivityController {

    func show(snapshot: RoomSnapshot) {
        let state = RoomActivityAttributes.ContentState(
            celsius: snapshot.celsius,
            comfortLabel: snapshot.comfortLabel
        )
        let content = ActivityContent(state: state, staleDate: nil)

        if let activity = Activity<RoomActivityAttributes>.activities.first {
            Task { await activity.update(content) }
        } else if ActivityAuthorizationInfo().areActivitiesEnabled {
            _ = try? Activity.request(
                attributes: RoomActivityAttributes(sensorId: snapshot.sensorId),
                content: content
            )
        }
    }

    func hide() {
        for activity in Activity<RoomActivityAttributes>.activities {
            Task { await activity.end(nil, dismissalPolicy: .immediate) }
        }
    }
}
