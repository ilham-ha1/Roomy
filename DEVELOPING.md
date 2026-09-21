# Roomy: Panduan Developer

Semua perintah dijalankan dari root project (`Roomy/`).
Snippet di dokumen ini sudah dikompilasi terhadap project ini, bukan tempelan dari internet.

---

## 1. Build & Run

### Android

```bash
./gradlew :composeApp:assembleDebug     # APK -> composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug      # pasang ke device/emulator yang tersambung
./gradlew :composeApp:assembleRelease   # butuh signing config dulu
```

### iOS

```bash
open iosApp/iosApp.xcodeproj            # pilih scheme "iosApp", Run
```

Xcode memanggil Gradle sendiri lewat build phase `Compile Kotlin Framework`.
Tidak perlu build Gradle manual sebelum Run. Kalau mau cek framework saja:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

### Test

```bash
./gradlew :composeApp:iosSimulatorArm64Test   # test shared di Kotlin/Native
./gradlew :composeApp:test                    # test shared di JVM/Android
./gradlew allTests                            # semua target + laporan gabungan
```

Laporan HTML: `composeApp/build/reports/tests/`.

### Kalau build ngadat

```bash
./gradlew clean
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*   # khusus error aneh di Xcode
./gradlew --stop                                        # daemon nyangkut
```

Configuration cache aktif di `gradle.properties`. Kalau ada plugin yang tidak kompatibel,
matikan sementara dengan `--no-configuration-cache`, jangan langsung hapus barisnya.

---

## 2. Menambah Package (folder Kotlin)

Package = folder. Tidak ada registrasi, tidak ada file index. Buat folder, buat file,
tulis `package` di baris pertama. Gradle langsung lihat.

```bash
mkdir -p composeApp/src/commonMain/kotlin/my/openlab/roomy/presentation/history
```

```kotlin
package my.openlab.roomy.presentation.history
```

### Taruh di mana?

| Yang kamu tulis | Lokasi |
|---|---|
| Layar baru | `presentation/<namafitur>/` berisi UiState, ViewModel, Screen |
| Model bisnis, enum, aturan | `domain/model/` |
| Kontrak sumber data | `domain/repository/` |
| Aksi yang dipakai ViewModel | `domain/usecase/` |
| Bentuk JSON dari server | `data/remote/dto/` |
| Kelas yang manggil HTTP | `data/remote/` |
| Konversi DTO ke model | `data/mapper/` |
| Implementasi repository | `data/repository/` |
| Kode khusus Android | `composeApp/src/androidMain/kotlin/...` |
| Kode khusus iOS | `composeApp/src/iosMain/kotlin/...` |

### Aturan arah dependency

```
presentation  ->  domain  <-  data
```

`domain` tidak boleh `import` apa pun dari `data`, `presentation`, Ktor, atau Compose.
Kalau kamu terpaksa import ke arah sebaliknya, berarti abstraksinya kurang: pindahkan
interface-nya ke `domain/repository/`.

### Kapan pecah jadi Gradle module

Sekarang semua di satu module `composeApp` karena baru satu fitur. Pecah jadi module
(`:core:domain`, `:feature:temperature`) kalau salah satu ini kejadian:

- build incremental sudah lebih dari kira-kira satu menit
- dua tim rebutan file yang sama
- ada kode yang mau dipakai app lain

Sebelum itu, multi-module cuma nambah file build tanpa manfaat.

---

## 3. Menambah Dependency (library)

Semua versi terkunci di `gradle/libs.versions.toml`. Jangan pernah tulis versi
langsung di `build.gradle.kts`.

### Langkah

1. Tambah versi di blok `[versions]`:

```toml
[versions]
settings = "1.3.0"
```

2. Tambah artefak di blok `[libraries]`:

```toml
[libraries]
multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version.ref = "settings" }
```

3. Pakai di `composeApp/build.gradle.kts`, di source set yang tepat:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.multiplatform.settings) // dipakai Android + iOS
    }
    androidMain.dependencies {
        implementation(libs.androidx.work) // Android saja
    }
    iosMain.dependencies {
        implementation(libs.ktor.client.darwin) // iOS saja
    }
}
```

Nama di catalog pakai tanda hubung, di Kotlin jadi titik: `multiplatform-settings`
menjadi `libs.multiplatform.settings`.

4. Sync:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64
```

Kompilasi kedua target sekaligus. Library yang lolos di Android tapi tidak punya artefak
iOS baru ketahuan di target kedua.

### Cek dulu library-nya multiplatform atau tidak

Sebelum menaruh di `commonMain`, buka
`https://repo1.maven.org/maven2/<group-dengan-slash>/<artifact>/<versi>/`
dan cari file `.module`. Kalau di dalamnya ada varian `ios_arm64` dan `ios_simulator_arm64`,
aman untuk `commonMain`. Kalau tidak ada, library itu Android-only, taruh di `androidMain`
dan bikin abstraksinya di `commonMain` (lihat bagian expect/actual).

Gejala salah taruh:

```
Couldn't resolve dependency 'xxx' in 'commonMain' for all target platforms.
Unresolved platforms: [iosArm64]
```

### Batas versi di project ini

| Naik ke | Butuh |
|---|---|
| Compose Multiplatform 1.12+ | AGP 9.1+, compileSdk 37 |
| androidx lifecycle 2.11+ | AGP 9.1+ |
| AGP 9.x | Gradle 9.x, cmdline-tools terbaru untuk pasang SDK 37 |

Mesin ini masih JDK 17 dan cmdline-tools lama, jadi versi di atas ditahan. Naikkan bertiga
sekaligus, jangan satu-satu.

### Apakah package di Android dan iOS berbeda?

Jawaban pendeknya: **hampir semuanya sama, dan yang berbeda cuma tiga.**

Di project ini ada 17 dependency di `commonMain`, 3 khusus Android, dan 1 khusus iOS.

| Source set | Dependency |
|---|---|
| `commonMain` | Compose runtime, foundation, material3, material icons, resources; Koin core, compose, compose-viewmodel; lifecycle viewmodel-compose dan runtime-compose; coroutines; serialization-json; datetime; Ktor core, content-negotiation, serialization-json, logging |
| `androidMain` | `compose.preview`, `androidx.activity:activity-compose`, `ktor-client-okhttp` |
| `iosMain` | `ktor-client-darwin` |

Yang ditulis sekali di `commonMain` berlaku untuk kedua platform. Tidak perlu menulis
dua baris, tidak perlu menyamakan versi secara manual.

#### Tapi binernya memang beda

Satu baris di `commonMain` bukan berarti satu file yang sama dipakai dua platform. Gradle
menerjemahkan koordinat yang sama menjadi artefak berbeda untuk tiap target.

Buktinya bisa dilihat langsung. Untuk Android:

```bash
./gradlew :composeApp:dependencies --configuration debugCompileClasspath
```

```
org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0
  \--- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0
```

Untuk iOS:

```bash
./gradlew :composeApp:dependencies --configuration iosSimulatorArm64CompileKlibraries
```

```
org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0
  \--- org.jetbrains.kotlinx:kotlinx-coroutines-core-iossimulatorarm64:1.11.0
```

Koordinat yang kamu tulis, `kotlinx-coroutines-core`, sebenarnya cuma penunjuk. Di
dalamnya ada berkas metadata `.module` yang memberi tahu Gradle varian mana yang cocok
untuk target yang sedang dibangun. Android dapat `.jar`, iOS dapat `.klib`.

Terlihat juga di cache lokal:

```
ktor-client-core
ktor-client-core-iosarm64
ktor-client-core-iossimulatorarm64
ktor-client-core-jvm
```

Empat folder, satu dependency.

Efek sampingnya yang bagus: versi tidak mungkin melenceng antar platform, karena memang
cuma ada satu tempat yang menyebut nomornya.

#### Kenapa engine HTTP harus beda

Ktor dibelah dua dengan sengaja:

- **Core** berisi API, pipeline, serialisasi, dan logika. Sama di semua platform.
- **Engine** berisi pembungkus tumpukan jaringan milik sistem operasi.

Engine tidak mungkin sama, karena yang dibungkus memang berbeda. `ktor-client-okhttp`
membungkus OkHttp di JVM, `ktor-client-darwin` membungkus `NSURLSession` di Apple.

Kode kamu tidak pernah menyebut nama engine. Cukup tulis `HttpClient { }` di `commonMain`,
dan Ktor memilih engine yang ada di classpath target itu. Karena itu `TemperatureApi` tidak
punya satu pun baris kode khusus platform.

Pola yang sama berlaku di banyak library multiplatform: satu modul inti bersama, ditambah
modul tipis per platform.

#### Kenapa activity-compose hanya di Android

`androidx.activity:activity-compose` menyediakan `setContent`, yaitu cara menempelkan UI
Compose ke sebuah Activity. iOS tidak punya Activity, jadi tidak butuh dan tidak punya
padanannya. Perannya di iOS diambil `ComposeUIViewController` dari Compose Multiplatform,
yang sudah ikut di `commonMain`.

Jadi ini bukan fitur yang hilang di satu sisi, melainkan cara memasang UI yang memang
berbeda bentuknya.

#### Ringkasnya

| Pertanyaan | Jawaban |
|---|---|
| Apakah nama package-nya berbeda? | Tidak, untuk 17 dari 21 dependency |
| Apakah file binernya berbeda? | Ya, selalu. `.jar` untuk Android, `.klib` untuk iOS |
| Apakah versinya bisa melenceng? | Tidak, karena disebut sekali di catalog |
| Kapan benar-benar butuh dependency terpisah? | Saat library membungkus API sistem operasi, seperti engine HTTP |
| Kalau library tidak punya varian iOS? | Taruh di `androidMain`, lalu bikin abstraksinya di `commonMain` dengan `expect`/`actual` |

---

## 4. Dependency Injection dengan Koin

Dependency injection memakai Koin, satu modul di `di/AppModule.kt`. Tidak ada annotation
processor dan tidak ada kode yang digenerate, jadi tidak perlu build ulang untuk melihat
perubahan wiring.

### Menambah satu objek

```kotlin
fun appModule(config: AppConfig = AppConfig()): Module = module {
    single { HistoryStore(get()) }                  // satu instance seumur aplikasi
    factory { ExportReadings(repository = get()) }  // instance baru tiap kali diminta
    viewModel { HistoryViewModel(get()) }           // ikut daur hidup layar
}
```

Pilih yang mana:

| Kata kunci | Kapan |
|---|---|
| `single` | Mahal dibuat atau memang harus satu, misalnya `HttpClient` dan repository |
| `factory` | Murah, tanpa state, misalnya use case |
| `viewModel` | Semua ViewModel, supaya ikut `ViewModelStoreOwner` |

`get()` mengisi parameter dari graph. Tidak perlu menyebut tipe kalau parameternya sudah
jelas tipenya.

### Mengikat interface ke implementasi

Selalu daftarkan repository dengan tipe interface-nya, bukan tipe kelasnya:

```kotlin
single<TemperatureRepository> { TemperatureRepositoryImpl(api = get()) }
```

Kalau ditulis tanpa `<TemperatureRepository>`, Koin mendaftarkannya sebagai
`TemperatureRepositoryImpl`, dan `ObserveTemperature` yang meminta interface akan gagal
saat aplikasi jalan, bukan saat dikompilasi.

### Nilai konfigurasi

Semua knob ada di `di/AppConfig.kt` dan ikut disuntik seperti dependency biasa. Jangan
menulis konstanta baru langsung di dalam modul.

```kotlin
single { TemperatureApi(client = get(), baseUrl = get<AppConfig>().baseUrl) }
```

### Mengambil dari Compose

```kotlin
val viewModel: TemperatureViewModel = koinViewModel()
val config: AppConfig = koinInject()
```

Koin dimulai di `App.kt` lewat `KoinApplication { }`, jadi Android dan iOS memakai jalur
yang sama dan tidak ada kode start-up per platform.

### Wajib: uji graph-nya

Kesalahan wiring Koin baru meledak saat aplikasi jalan. Setiap kali menambah definisi,
tambahkan juga resolusinya di `commonTest/DiGraphTest.kt`:

```kotlin
@Test
fun everyDefinitionResolves() {
    val application = koinApplication { modules(appModule(AppConfig())) }
    application.koin.get<HistoryStore>()
    application.close()
}
```

```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

---

## 5. Lifecycle

Dipakai library `androidx.lifecycle` versi multiplatform, jadi kode lifecycle ditulis
sekali di `commonMain` dan jalan di Android maupun iOS.

### ViewModel

ViewModel bertahan melewati rotasi layar dan perubahan konfigurasi. Kerjaan async
dilempar ke `viewModelScope`, yang otomatis dibatalkan saat ViewModel dibuang.

```kotlin
class TemperatureViewModel(...) : ViewModel() {
    private val _state = MutableStateFlow(TemperatureUiState())
    val state = _state.asStateFlow()

    init { watch() }

    private fun watch() {
        viewModelScope.launch { /* ... */ }
    }
}
```

Bikin ViewModel di Composable pakai `viewModel { }`, bukan `remember { }`.
`remember` mati saat rotasi, `viewModel` tidak.

```kotlin
val viewModel = viewModel {
    TemperatureViewModel(observeTemperature, sensorId)
}
```

Kalau ViewModel perlu bersih-bersih sendiri:

```kotlin
override fun onCleared() {
    super.onCleared()
    job?.cancel()
}
```

### Membaca state tanpa boros baterai

Selalu `collectAsStateWithLifecycle`, jangan `collectAsState`. Yang pertama berhenti
mengumpulkan saat layar tidak terlihat.

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

### Menyalakan dan mematikan pekerjaan mengikuti layar

Polling sensor tidak perlu jalan saat app di background. Pakai `LifecycleStartEffect`:

```kotlin
import androidx.lifecycle.compose.LifecycleStartEffect

@Composable
fun PollWhileVisible(onStart: () -> Unit, onStop: () -> Unit) {
    LifecycleStartEffect(Unit) {
        onStart()
        onStopOrDispose { onStop() }
    }
}
```

Untuk satu event saja:

```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.retry() }
```

Mencegah klik ganda saat layar sedang berpindah:

```kotlin
import androidx.lifecycle.compose.dropUnlessResumed

Button(onClick = dropUnlessResumed { navigateToDetail() }) { Text("Detail") }
```

### Sisi Android

`MainActivity` sudah mendeklarasikan `configChanges` yang lengkap di manifest, jadi rotasi
tidak me-restart Activity dan Compose mengurus layoutnya sendiri. Jangan kurangi daftar itu
kecuali kamu memang mau Activity dibuat ulang.

### Sisi iOS

`MainViewController()` dipanggil sekali dari `ContentView.swift`. Compose Multiplatform
menyediakan `LifecycleOwner` sendiri yang mengikuti state UIKit, jadi semua kode
`LifecycleStartEffect` di atas ikut jalan tanpa tambahan apa pun.

---

## 6. Notifikasi

Tidak ada API notifikasi bersama di Kotlin Multiplatform. Pola yang dipakai: interface di
`commonMain`, implementasi per platform lewat `expect`/`actual`.

### 6.1 Kontrak bersama

`composeApp/src/commonMain/kotlin/my/openlab/roomy/platform/Notifier.kt`

```kotlin
package my.openlab.roomy.platform

interface Notifier {
    suspend fun ensurePermission(): Boolean
    fun show(id: Int, title: String, body: String)
}

expect fun createNotifier(): Notifier
```

### 6.2 Android

Tambah izin di `composeApp/src/androidMain/AndroidManifest.xml`, di luar tag `<application>`:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

`composeApp/src/androidMain/kotlin/my/openlab/roomy/platform/Notifier.android.kt`

```kotlin
package my.openlab.roomy.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AndroidAppContext {
    lateinit var value: Context
}

private const val CHANNEL_ID = "roomy_alerts"

class AndroidNotifier(private val context: Context) : Notifier {

    override suspend fun ensurePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun show(id: Int, title: String, body: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Roomy alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
        manager.notify(
            id,
            builder
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build(),
        )
    }
}

actual fun createNotifier(): Notifier = AndroidNotifier(AndroidAppContext.value)
```

Isi context-nya sekali di `MainActivity.onCreate`, sebelum `setContent`:

```kotlin
AndroidAppContext.value = applicationContext
```

Sejak Android 13, izin notifikasi harus diminta saat runtime. Mintanya dari Compose:

```kotlin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted -> /* simpan hasilnya ke state */ }

LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
```

Kode di atas Android-only, jadi letaknya di `androidMain`, bukan di `commonMain`.

Ganti `android.R.drawable.ic_dialog_info` dengan ikon monokrom sendiri sebelum rilis.
Ikon sistem itu terlihat jelek di status bar.

### 6.3 iOS

`composeApp/src/iosMain/kotlin/my/openlab/roomy/platform/Notifier.ios.kt`

```kotlin
package my.openlab.roomy.platform

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosNotifier : Notifier {

    override suspend fun ensurePermission(): Boolean = suspendCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, _ -> continuation.resume(granted) }
    }

    override fun show(id: Int, title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id.toString(),
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, null)
    }
}

actual fun createNotifier(): Notifier = IosNotifier()
```

Dua catatan iOS:

- `trigger = null` berarti tampil langsung. Isi dengan `UNTimeIntervalNotificationTrigger`
  kalau mau tertunda.
- Secara bawaan iOS menyembunyikan notifikasi saat app sedang dibuka. Supaya tetap muncul,
  pasang delegate di `iOSApp.swift` yang mengembalikan `.banner` dari
  `userNotificationCenter(_:willPresent:withCompletionHandler:)`.

### 6.4 Memakainya

```kotlin
val notifier = createNotifier()
if (notifier.ensurePermission()) {
    notifier.show(1, "Ruangan panas", "Suhu 30.2 °C")
}
```

Jangan panggil `createNotifier()` berkali-kali dari UI. Daftarkan sekali di
`di/AppModule.kt` sebagai `single { }` Koin, seperti dependency lain.

---

## 7. Pola expect/actual

Dipakai setiap kali ada API yang cuma ada di satu platform: notifikasi, storage, izin,
Bluetooth, GPS.

```kotlin
// commonMain
expect fun deviceName(): String

// androidMain
actual fun deviceName(): String = android.os.Build.MODEL

// iosMain
actual fun deviceName(): String = UIDevice.currentDevice.name
```

Aturan main:

- Nama file diberi akhiran platform: `Notifier.android.kt`, `Notifier.ios.kt`.
- Setiap `expect` wajib punya `actual` di semua target, kalau tidak build gagal.
- Lebih baik `expect fun` yang mengembalikan interface daripada `expect class`. Lebih mudah
  di-mock saat testing dan lebih longgar saat platform berbeda butuh konstruktor berbeda.

---

## 8. Menambah endpoint API

Empat file, urut dari luar ke dalam:

1. `data/remote/dto/XxxDto.kt`, tandai `@Serializable`, nama field JSON pakai `@SerialName`.
2. `data/remote/XxxApi.kt`, satu fungsi `suspend` per endpoint.
3. `data/mapper/XxxMapper.kt`, fungsi ekstensi `toDomain()`.
4. `data/repository/XxxRepositoryImpl.kt`, implementasi interface dari `domain/repository/`.

DTO tidak pernah bocor ke `domain` atau `presentation`. Kalau server ganti nama field,
yang berubah cuma DTO dan mapper.

Serialisasi memakai `ignoreUnknownKeys = true` di modul Koin, jadi field baru dari server
tidak bikin app crash.

---

## 9. Testing

Test bersama ada di `composeApp/src/commonTest/`, dijalankan di semua target.

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class SomethingTest {
    @Test
    fun namanyaJelasDanMenyebutPerilaku() {
        assertEquals(expected, actual)
    }
}
```

Yang wajib ada test: mapper, aturan domain, batas nilai. Yang tidak perlu: getter satu baris.

Untuk menguji repository tanpa jaringan, buat `FakeTemperatureRepository` di `commonTest`
yang mengimplementasikan interface domain. Ini alasan interface itu ada.

---

## 10. Hal penting lain

**HTTP polos.** Gateway di LAN yang belum HTTPS butuh
`android:usesCleartextTraffic="true"` di tag `<application>`. Nyalakan hanya untuk build
debug, lewat manifest terpisah di `src/debug/`. Jangan pernah ikut ke release.

**Rahasia dan kunci.** Jangan taruh token di `AppConfig.kt`. File itu ikut ke Git dan
ikut ke APK. Pakai `local.properties` yang dibaca lewat `buildConfigField`, atau minta
token dari backend saat runtime.

**Target iOS.** `iosX64` sengaja tidak ada karena Compose Multiplatform 1.11 tidak merilis
artefak untuk simulator Intel. Kalau nanti perlu, naikkan dulu versi Compose Multiplatform.
Simulator Intel juga dimatikan di Xcode lewat `EXCLUDED_ARCHS[sdk=iphonesimulator*]`.

**Nama framework iOS.** `baseName = "ComposeApp"` di `composeApp/build.gradle.kts` harus
sama dengan `import ComposeApp` di `ContentView.swift`. Ganti satu, ganti dua-duanya.

**Fungsi Kotlin yang dipanggil Swift.** Fungsi top-level di `MainViewController.kt` muncul
di Swift sebagai `MainViewControllerKt.MainViewController()`. Nama file plus akhiran `Kt`
menjadi nama kelasnya.

**Ambang suhu.** Nilainya ada di `TemperatureReading.Companion`, bukan di UI. Sensor asli
selalu meleset beberapa derajat, jadi biarkan angka itu bisa diatur dan jangan tanam ulang
di layar mana pun.

**Interval polling.** Bawaannya sepuluh detik di `TemperatureRepositoryImpl`. Naikkan kalau
baterai jadi masalah. Kalau gateway sudah bisa push, ganti kelas itu dengan implementasi
MQTT atau WebSocket. Tidak ada file di luar `data/` yang perlu disentuh.

**Komentar `ponytail:`.** Menandai penyederhanaan yang disengaja beserta batas dan jalur
naiknya. Cari dengan `grep -rn "ponytail:" composeApp/src` sebelum memutuskan arsitektur
sudah final.
