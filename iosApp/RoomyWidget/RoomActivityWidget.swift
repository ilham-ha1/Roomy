import ActivityKit
import SwiftUI
import WidgetKit

/// Lock screen + Dynamic Island UI for the "room is hot" Live Activity.
/// Lives in the RoomyWidget extension target, not the app.
@main
struct RoomyWidgetBundle: WidgetBundle {
    var body: some Widget {
        RoomActivityWidget()
    }
}

struct RoomActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: RoomActivityAttributes.self) { context in
            HStack {
                Image(systemName: "thermometer.sun.fill").foregroundStyle(.orange)
                Text(context.attributes.sensorId)
                Spacer()
                Text(celsius(context.state)).font(.title2.bold())
            }
            .padding()
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text(context.state.comfortLabel)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(celsius(context.state)).font(.title2.bold())
                }
            } compactLeading: {
                Image(systemName: "thermometer.sun.fill").foregroundStyle(.orange)
            } compactTrailing: {
                Text(celsius(context.state))
            } minimal: {
                Image(systemName: "thermometer.sun.fill").foregroundStyle(.orange)
            }
        }
    }

    private func celsius(_ state: RoomActivityAttributes.ContentState) -> String {
        "\(Int(state.celsius.rounded()))°"
    }
}
