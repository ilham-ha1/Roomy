package my.openlab.roomy.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import my.openlab.roomy.data.remote.dto.TemperatureDto

/** Thin HTTP boundary. One job: speak to the sensor gateway. */
class TemperatureApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun latest(sensorId: String): TemperatureDto =
        client.get("$baseUrl/sensors/$sensorId/latest").body()
}
