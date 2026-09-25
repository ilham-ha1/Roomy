package my.openlab.roomy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.openlab.roomy.di.appModule
import my.openlab.roomy.domain.model.TemperatureReading
import my.openlab.roomy.presentation.slice.RoomSliceScreen
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
            // ponytail: one boolean instead of a nav library; add navigation when there are 3+ screens.
            var showSlice by rememberSaveable { mutableStateOf(false) }
            if (showSlice) {
                RoomSliceScreen(reading = state.reading, onBack = { showSlice = false })
            } else {
                TemperatureScreen(state = state, onRetry = viewModel::retry, onOpenSlice = { showSlice = true })
            }
        }
    }
}
