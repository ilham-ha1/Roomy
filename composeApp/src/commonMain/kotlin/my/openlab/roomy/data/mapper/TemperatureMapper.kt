package my.openlab.roomy.data.mapper

import my.openlab.roomy.data.remote.dto.TemperatureDto
import my.openlab.roomy.domain.model.TemperatureReading
import kotlin.time.Instant

/** DTO -> domain. The only place that knows both shapes (DRY). */
fun TemperatureDto.toDomain(): TemperatureReading = TemperatureReading(
    sensorId = sensorId,
    celsius = celsius,
    humidityPercent = humidity,
    recordedAt = Instant.parse(recordedAt),
)
