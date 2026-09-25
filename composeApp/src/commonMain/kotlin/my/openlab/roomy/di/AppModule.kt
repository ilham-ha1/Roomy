package my.openlab.roomy.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import my.openlab.roomy.data.remote.TemperatureApi
import my.openlab.roomy.data.repository.DemoTemperatureRepository
import my.openlab.roomy.data.repository.TemperatureRepositoryImpl
import my.openlab.roomy.domain.repository.TemperatureRepository
import my.openlab.roomy.domain.usecase.ObserveTemperature
import my.openlab.roomy.platform.sensorDispatcher
import my.openlab.roomy.presentation.temperature.TemperatureViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Single Koin module. Layers stay honest: the repository is bound to its domain interface,
 * so nothing above the data layer can see which implementation won.
 */
fun appModule(config: AppConfig = AppConfig()): Module = module {

    single { config }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single {
        HttpClient {
            install(ContentNegotiation) { json(get()) }
        }
    }

    single { TemperatureApi(client = get(), baseUrl = get<AppConfig>().baseUrl) }

    single<TemperatureRepository> {
        if (get<AppConfig>().useDemoData) {
            DemoTemperatureRepository()
        } else {
            TemperatureRepositoryImpl(api = get())
        }
    }

    factory { ObserveTemperature(repository = get(), dispatcher = sensorDispatcher) }

    viewModel {
        TemperatureViewModel(
            observeTemperature = get(),
            sensorId = get<AppConfig>().defaultSensorId,
        )
    }
}
