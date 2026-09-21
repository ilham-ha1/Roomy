package my.openlab.roomy.domain.usecase

import kotlinx.coroutines.flow.Flow
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.domain.repository.TemperatureRepository

/** Single entry point the presentation layer is allowed to call. */
class ObserveTemperature(
    private val repository: TemperatureRepository,
) {
    operator fun invoke(sensorId: String): Flow<TemperatureReading> =
        repository.observeLatest(sensorId)
}
