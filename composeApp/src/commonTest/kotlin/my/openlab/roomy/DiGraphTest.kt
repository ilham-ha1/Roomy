package my.openlab.roomy

import io.ktor.client.HttpClient
import my.openlab.roomy.data.remote.TemperatureApi
import my.openlab.roomy.data.repository.DemoTemperatureRepository
import my.openlab.roomy.data.repository.TemperatureRepositoryImpl
import my.openlab.roomy.di.AppConfig
import my.openlab.roomy.di.appModule
import my.openlab.roomy.domain.repository.TemperatureRepository
import my.openlab.roomy.domain.usecase.ObserveTemperature
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Koin resolves at runtime, so the graph needs a test or a broken wiring ships. */
class DiGraphTest {

    private fun app(config: AppConfig) = koinApplication { modules(appModule(config)) }

    @Test
    fun everyDefinitionResolves() {
        val application = app(AppConfig())
        val koin = application.koin

        assertEquals("living-room", koin.get<AppConfig>().defaultSensorId)
        koin.get<HttpClient>().close()
        koin.get<TemperatureApi>()
        koin.get<TemperatureRepository>()
        koin.get<ObserveTemperature>()

        application.close()
    }

    @Test
    fun demoFlagPicksTheFakeFeed() {
        val application = app(AppConfig(useDemoData = true))
        assertTrue(application.koin.get<TemperatureRepository>() is DemoTemperatureRepository)
        application.close()
    }

    @Test
    fun productionFlagPicksTheHttpFeed() {
        val application = app(AppConfig(useDemoData = false))
        assertTrue(application.koin.get<TemperatureRepository>() is TemperatureRepositoryImpl)
        application.close()
    }
}
