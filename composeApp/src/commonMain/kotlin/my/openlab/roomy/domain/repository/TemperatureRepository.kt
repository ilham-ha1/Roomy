package my.openlab.roomy.domain.repository

import kotlinx.coroutines.flow.Flow
import my.openlab.roomy.domain.model.TemperatureReading

/**
 * Domain-owned contract. The data layer implements it, so domain never depends on Ktor,
 * MQTT, or any transport (Dependency Inversion).
 */
interface TemperatureRepository {
    /** Emits the newest reading for [sensorId] until cancelled. Throws on transport failure. */
    fun observeLatest(sensorId: String): Flow<TemperatureReading>
}
