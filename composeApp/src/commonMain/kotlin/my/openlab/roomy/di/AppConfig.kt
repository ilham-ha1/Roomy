package my.openlab.roomy.di

/** Runtime knobs, resolved once and injected like any other dependency. */
data class AppConfig(
    // TODO: point at the real sensor gateway.
    val baseUrl: String = "https://api.roomy.local",
    val defaultSensorId: String = "living-room",
    /** ponytail: fake sensor feed so the UI runs before the gateway exists. Flip to false. */
    val useDemoData: Boolean = true,
)
