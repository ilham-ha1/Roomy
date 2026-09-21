package my.openlab.roomy.presentation.temperature

import my.openlab.roomy.domain.model.TemperatureReading

/** Everything the screen needs, and nothing else. One state object, one source of truth. */
data class TemperatureUiState(
    val isLoading: Boolean = true,
    val reading: TemperatureReading? = null,
    val history: List<Double> = emptyList(),
    val errorMessage: String? = null,
)
