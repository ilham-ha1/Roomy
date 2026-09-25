package my.openlab.roomy.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.domain.repository.TemperatureRepository

/** Single entry point the presentation layer is allowed to call. */
class ObserveTemperature(
    private val repository: TemperatureRepository,
    /** Where polling runs. Collectors still receive on their own dispatcher. */
    private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(sensorId: String): Flow<TemperatureReading> =
        repository.observeLatest(sensorId).flowOn(dispatcher)
}
