package my.openlab.roomy

import my.openlab.roomy.data.mapper.toDomain
import my.openlab.roomy.data.remote.dto.TemperatureDto
import my.openlab.roomy.domain.model.Comfort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TemperatureMappingTest {

    private fun dto(celsius: Double) = TemperatureDto(
        sensorId = "living-room",
        celsius = celsius,
        humidity = null,
        recordedAt = "2026-09-09T02:30:00Z",
    )

    @Test
    fun mapsWireShapeToDomain() {
        val reading = TemperatureDto("bedroom", 21.5, 55.0, "2026-09-09T02:30:00Z").toDomain()
        assertEquals("bedroom", reading.sensorId)
        assertEquals(21.5, reading.celsius)
        assertEquals(55.0, reading.humidityPercent)
        assertEquals(70.7, reading.fahrenheit, absoluteTolerance = 0.01)
    }

    @Test
    fun missingHumidityStaysNull() {
        assertNull(dto(22.0).toDomain().humidityPercent)
    }

    @Test
    fun comfortBandsUseInclusiveEdges() {
        assertEquals(Comfort.COLD, dto(19.9).toDomain().comfort)
        assertEquals(Comfort.COMFORTABLE, dto(20.0).toDomain().comfort)
        assertEquals(Comfort.COMFORTABLE, dto(28.0).toDomain().comfort)
        assertEquals(Comfort.HOT, dto(28.1).toDomain().comfort)
    }
}
