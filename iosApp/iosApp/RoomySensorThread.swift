import Foundation
import ComposeApp

/// A real, manually configured Foundation `Thread` that runs Kotlin's sensor work.
/// Kotlin wraps it as a CoroutineDispatcher (`sensorDispatcher`); jobs run one at a time, in order.
final class RoomySensorThread: NSObject, SensorThreadRunner {
    private let condition = NSCondition()
    private var jobs: [() -> Void] = []

    override init() {
        super.init()
        let thread = Thread { [unowned self] in self.runLoop() }
        thread.name = "roomy-sensor"
        thread.qualityOfService = .utility
        thread.stackSize = 1 << 20 // 1 MiB
        thread.start()
    }

    func execute(block: @escaping () -> Void) {
        condition.lock()
        jobs.append(block)
        condition.signal()
        condition.unlock()
    }

    // ponytail: thread lives for the whole app; add a stop flag if it ever needs shutting down.
    private func runLoop() {
        while true {
            condition.lock()
            while jobs.isEmpty { condition.wait() }
            let job = jobs.removeFirst()
            condition.unlock()
            job()
        }
    }
}
