package my.openlab.roomy.domain.model

import kotlin.time.Instant

/** One sample from an IoT temperature sensor. Pure domain type: no serialization, no framework. */
data class TemperatureReading(
    val sensorId: String,
    val celsius: Double,
    val humidityPercent: Double?,
    val recordedAt: Instant,
) {
    val fahrenheit: Double get() = celsius * 9 / 5 + 32

    val comfort: Comfort
        get() = when {
            celsius < COLD_BELOW -> Comfort.COLD
            celsius > HOT_ABOVE -> Comfort.HOT
            else -> Comfort.COMFORTABLE
        }

    companion object {
        // ponytail: fixed thresholds; move to user settings when rooms need per-room targets.
        const val COLD_BELOW = 20.0
        const val HOT_ABOVE = 28.0
    }
}

enum class Comfort { COLD, COMFORTABLE, HOT }
