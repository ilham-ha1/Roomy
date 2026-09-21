package my.openlab.roomy.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape. Kept separate from the domain model so an API change never leaks inward. */
@Serializable
data class TemperatureDto(
    @SerialName("sensor_id") val sensorId: String,
    @SerialName("celsius") val celsius: Double,
    @SerialName("humidity") val humidity: Double? = null,
    @SerialName("recorded_at") val recordedAt: String,
)
