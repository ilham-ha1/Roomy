package my.openlab.roomy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.openlab.roomy.di.appModule
import my.openlab.roomy.presentation.temperature.TemperatureScreen
import my.openlab.roomy.presentation.temperature.TemperatureViewModel
import my.openlab.roomy.presentation.theme.RoomyTheme
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    KoinApplication(application = { modules(appModule()) }) {
        RoomyTheme {
            val viewModel: TemperatureViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            TemperatureScreen(state = state, onRetry = viewModel::retry)
        }
    }
}
