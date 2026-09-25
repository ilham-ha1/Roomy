package my.openlab.roomy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.openlab.roomy.di.appModule
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.presentation.temperature.TemperatureScreen
import my.openlab.roomy.presentation.temperature.TemperatureViewModel
import my.openlab.roomy.presentation.theme.RoomyTheme
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

/** [onReading] lets a platform react to new readings (iOS: Live Activity). */
@Composable
fun App(onReading: (TemperatureReading) -> Unit = {}) {
    KoinApplication(application = { modules(appModule()) }) {
        RoomyTheme {
            val viewModel: TemperatureViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.reading) { state.reading?.let(onReading) }
            TemperatureScreen(state = state, onRetry = viewModel::retry)
        }
    }
}
