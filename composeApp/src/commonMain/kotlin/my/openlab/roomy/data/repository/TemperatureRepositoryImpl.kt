package my.openlab.roomy.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import my.openlab.roomy.data.mapper.toDomain
import my.openlab.roomy.data.remote.TemperatureApi
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.domain.repository.TemperatureRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TemperatureRepositoryImpl(
    private val api: TemperatureApi,
    private val pollInterval: Duration = DEFAULT_POLL_INTERVAL,
) : TemperatureRepository {

    // ponytail: HTTP polling. Swap this class for an MQTT/WebSocket one when the
    // gateway pushes; nothing outside the data layer changes.
    override fun observeLatest(sensorId: String): Flow<TemperatureReading> = flow {
        while (true) {
            emit(api.latest(sensorId).toDomain())
            delay(pollInterval)
        }
    }

    companion object {
        val DEFAULT_POLL_INTERVAL = 10.seconds
    }
}
