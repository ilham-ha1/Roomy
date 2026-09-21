# Roomy

Kotlin Multiplatform + Compose Multiplatform app that reads temperature from IoT sensors.
Android and iOS share one codebase, including the UI.

## Run

```bash
./gradlew :composeApp:assembleDebug          # Android APK
./gradlew :composeApp:iosSimulatorArm64Test  # shared unit tests
open iosApp/iosApp.xcodeproj                 # iOS, run the "iosApp" scheme
```

The app ships with `useDemoData = true` in `di/AppConfig.kt`, a fake sensor that sweeps
every comfort band so the UI runs with no backend. Flip it to `false` and set `baseUrl`
to talk to a real gateway.
The gateway must serve `GET {baseUrl}/sensors/{id}/latest` returning:

```json
{ "sensor_id": "living-room", "celsius": 24.3, "humidity": 58.0, "recorded_at": "2026-09-09T02:30:00Z" }
```

Plain-HTTP gateways on the LAN need `android:usesCleartextTraffic="true"` in the Android
manifest. Leave it off for anything shipped.

## Architecture pattern

Clean Architecture in three layers, MVVM at the top. Layers are Kotlin packages, not Gradle
modules: one screen and one data source do not justify four build files. Promote a package to
its own module when two features start fighting over the same code.

Dependencies point one way only, inward:

```
presentation  ->  domain  <-  data
```

`domain` knows nothing about Ktor, Compose, Android, or iOS. It defines
`TemperatureRepository`; `data` implements it. That inversion is what lets the transport
change from HTTP polling to MQTT without touching a single domain or UI file.

## Folder structure

```
composeApp/src/
├── commonMain/kotlin/my/openlab/roomy/
│   ├── App.kt                    # shared Compose entry point
│   ├── di/                       # Koin module and AppConfig
│   ├── domain/                   # pure Kotlin, no dependencies
│   │   ├── model/                # TemperatureReading, Comfort
│   │   ├── repository/           # interfaces the data layer must satisfy
│   │   └── usecase/              # ObserveTemperature
│   ├── data/                     # everything that talks to the outside world
│   │   ├── remote/               # TemperatureApi (Ktor)
│   │   ├── remote/dto/           # wire shapes, @Serializable
│   │   ├── mapper/               # DTO -> domain
│   │   └── repository/           # TemperatureRepositoryImpl
│   └── presentation/
│       └── temperature/          # UiState, ViewModel, Screen
├── commonTest/kotlin/            # shared tests, run on every target
├── androidMain/                  # MainActivity, manifest, resources
└── iosMain/                      # MainViewController
iosApp/                           # Xcode shell that hosts the shared UI
```

Where new code goes: a new screen is a new folder under `presentation/`, a new sensor type is
a new model plus repository interface under `domain/`, a new backend is a new class under
`data/`. Nothing else moves.

## Coding principles

**SOLID.** Each class has one job: `TemperatureApi` speaks HTTP, the mapper converts,
the repository orchestrates, the ViewModel holds state. `TemperatureRepository` carries one
method, so nobody implements what they do not need. Both the use case and the repository
implementation depend on abstractions, never on concrete transports.

**DRY.** DTO-to-domain conversion lives in exactly one file. Temperature thresholds live on
the model, not scattered across the UI. Versions live in `gradle/libs.versions.toml`.

**KISS.** Dependency injection is one object with lazy properties, not a framework. State is
one immutable data class, not five booleans. Dependency injection is one Koin module, with
no annotation processor and no generated code to wait on.

## Deliberate simplifications

Marked in code with `ponytail:` comments.

| What | Why | Upgrade when |
|---|---|---|
| HTTP polling every 10s | No push infrastructure yet | Gateway supports MQTT or WebSocket |
| Hard-coded comfort thresholds | One room, one preference | Per-room targets are a feature |
| No iOS x86_64 target | Compose Multiplatform 1.11 dropped it | Never; Intel Macs are gone |

## Dokumentasi

Panduan developer lengkap ada di [DEVELOPING.md](DEVELOPING.md): build, menambah package,
menambah dependency, lifecycle, notifikasi, dan jebakan yang sudah diketahui.

- [IOS_NOTES.md](IOS_NOTES.md): jebakan khusus iOS karena kode intinya Kotlin.

- [STRUCTURE.md](STRUCTURE.md): arti composeApp, commonMain, androidMain, iosMain, dan iosApp.

- [PLATFORM_TRICKS.md](PLATFORM_TRICKS.md): hal aneh yang bisa dilakukan di androidMain dan iosMain.

- [ONBOARDING.md](ONBOARDING.md): mulai dari sini kalau baru pertama kali membuka repository ini.
