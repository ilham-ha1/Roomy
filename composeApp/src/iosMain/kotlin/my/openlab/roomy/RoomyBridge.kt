package my.openlab.roomy

import kotlinx.coroutines.flow.first
import my.openlab.roomy.di.AppConfig
import my.openlab.roomy.di.appModule
import my.openlab.roomy.domain.model.Comfort
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.domain.usecase.ObserveTemperature
import my.openlab.roomy.platform.SensorThreadRunner
import my.openlab.roomy.presentation.theme.label
import org.koin.dsl.koinApplication
import kotlin.concurrent.Volatile

/** What Swift sees of a reading. Plain types only: no enum, no Instant, no Double? (IOS_NOTES.md §3). */
class RoomSnapshot(
    val sensorId: String,
    val celsius: Double,
    val comfortLabel: String,
)

/** Kotlin -> Swift. Swift implements it (ActivityKit is Swift-only) and registers it at launch. */
interface LiveActivityController {
    fun show(snapshot: RoomSnapshot)
    fun hide()
}

/** Everything Swift is allowed to touch. Swift: `RoomyBridge.shared`. */
object RoomyBridge {

    var liveActivity: LiveActivityController? = null

    /** Swift-owned thread for sensor work. Register at launch, before the UI starts. */
    @Volatile
    var sensorThread: SensorThreadRunner? = null

    // Own Koin instance: an App Intent can run before (or without) the Compose UI starting its own.
    private val koin by lazy { koinApplication { modules(appModule()) }.koin }

    /** Swift -> Kotlin. One fresh reading for Siri/Shortcuts. Swift sees `try await currentRoom()`. */
    @Throws(Exception::class)
    suspend fun currentRoom(): RoomSnapshot {
        val sensorId = koin.get<AppConfig>().defaultSensorId
        return koin.get<ObserveTemperature>()(sensorId).first().toSnapshot()
    }

    /** Fed by the Compose UI on every reading. The "when to show" rule stays in Kotlin. */
    internal fun onReading(reading: TemperatureReading) {
        val controller = liveActivity ?: return
        if (reading.comfort == Comfort.HOT) controller.show(reading.toSnapshot()) else controller.hide()
    }

    private fun TemperatureReading.toSnapshot() = RoomSnapshot(sensorId, celsius, comfort.label())
}
