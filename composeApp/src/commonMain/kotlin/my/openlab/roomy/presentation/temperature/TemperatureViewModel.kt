package my.openlab.roomy.presentation.temperature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.openlab.roomy.domain.usecase.ObserveTemperature

class TemperatureViewModel(
    private val observeTemperature: ObserveTemperature,
    private val sensorId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(TemperatureUiState())
    val state = _state.asStateFlow()

    private var job: Job? = null

    init {
        watch()
    }

    fun retry() = watch()

    private companion object {
        const val HISTORY_SIZE = 40
    }

    private fun watch() {
        job?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        job = viewModelScope.launch {
            observeTemperature(sensorId)
                .catch { error ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Unknown error")
                    }
                }
                .collect { reading ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            reading = reading,
                            history = (current.history + reading.celsius).takeLast(HISTORY_SIZE),
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}
