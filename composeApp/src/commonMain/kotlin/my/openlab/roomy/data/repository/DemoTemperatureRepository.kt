package my.openlab.roomy.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.domain.repository.TemperatureRepository
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Fake sensor that sweeps across every comfort band so the UI can be reviewed without a
 * gateway. ponytail: delete this class once the real gateway is reachable.
 */
class DemoTemperatureRepository(
    private val sensorId: String = "living-room",
) : TemperatureRepository {

    override fun observeLatest(sensorId: String): Flow<TemperatureReading> = flow {
        var tick = 0
        while (true) {
            val wave = sin(tick / 6.0)
            emit(
                TemperatureReading(
                    sensorId = sensorId.ifBlank { this@DemoTemperatureRepository.sensorId },
                    celsius = 24.0 + wave * 7.0 + Random.nextDouble(-0.3, 0.3),
                    humidityPercent = 55.0 - wave * 18.0 + Random.nextDouble(-1.0, 1.0),
                    recordedAt = Clock.System.now(),
                ),
            )
            tick++
            delay(2.seconds)
        }
    }
}
