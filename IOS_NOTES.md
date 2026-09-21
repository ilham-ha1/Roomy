# Catatan iOS untuk Project Kotlin

Kode iOS di sini bukan Swift. Yang jalan adalah Kotlin/Native yang dibungkus jadi framework
Objective-C, lalu dipanggil dari Swift. Semua kejanggalan di dokumen ini berasal dari
lapisan Objective-C di tengah.

Semua contoh sudah dikompilasi terhadap project ini, dan potongan header yang dikutip
diambil dari `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Headers/ComposeApp.h`
yang benar-benar dihasilkan.

---

## 1. Jalur Kotlin sampai ke Swift

```
Kotlin (commonMain + iosMain)
    -> Kotlin/Native compiler
    -> ComposeApp.framework (header Objective-C)
    -> Swift import ComposeApp
```

Konsekuensinya: apa pun yang tidak ada di Objective-C, tidak sampai ke Swift.
Tidak ada struct, tidak ada enum Swift, tidak ada generic yang ketat, tidak ada
`async` asli, tidak ada `Result`, tidak ada default argument.

---

## 2. Nama berubah saat menyeberang

| Kotlin | Swift |
|---|---|
| `fun MainViewController()` di `MainViewController.kt` | `MainViewControllerKt.MainViewController()` |
| `class TemperatureApi` | `TemperatureApi` |
| package `my.openlab.roomy.data.remote` | hilang, semua rata di satu namespace |
| `data class` method `copy()` | `doCopy()` |
| `companion object` | `.companion`, atau `.shared` untuk `object` |
| `enum entry COLD` | `Comfort.cold` |

Tiga aturan yang paling sering bikin bingung:

**Fungsi top-level butuh akhiran `Kt`.** Nama file plus `Kt` menjadi nama kelas pembungkus.
`MainViewController.kt` menghasilkan `MainViewControllerKt`. Ganti nama file, kode Swift ikut
rusak, dan compiler Kotlin tidak akan memperingatkan.

**Package dibuang total.** Dua kelas bernama sama di package berbeda akan bentrok di Swift.
Kotlin tetap kompilasi, Swift yang gagal. Beri nama unik lintas package.

**Nama yang bentrok dengan Objective-C dimangling.** `copy()` menjadi `doCopy()` karena
`NSObject` sudah punya `copy`. Ini yang benar-benar keluar di header:

```objc
- (ComposeAppTemperatureReading *)doCopySensorId:(NSString *)sensorId ...
    __attribute__((swift_name("doCopy(sensorId:celsius:humidityPercent:recordedAt:)")));
```

Hindari nama method `copy`, `description`, `hash`, `init`, `alloc`, `new`, `class`, `retain`,
`release` di kelas yang dipakai dari Swift.

Mau nama Swift yang berbeda? Pakai `@ObjCName`:

```kotlin
@OptIn(kotlin.experimental.ExperimentalObjCName::class)
@ObjCName("RoomySensor")
class TemperatureApi(...)
```

---

## 3. Pemetaan tipe dan jebakannya

| Kotlin | Swift |
|---|---|
| `String` | `String` |
| `Int`, `Long`, `Double` non-null | `Int32`, `Int64`, `Double` |
| `Int?`, `Double?` nullable | `KotlinInt?`, `KotlinDouble?` (objek kotak, bukan angka) |
| `List<T>` | `[T]` |
| `Map<K, V>` | `[K: V]` |
| `enum class` | kelas turunan `KotlinEnum`, bukan `enum` Swift |
| `sealed class` | kelas biasa, tidak ada exhaustiveness |
| `data class` | kelas, tapi `isEqual` dan `hash` ikut terbawa |
| `Duration` | `Int64` mentah |
| `Unit` | `Void` |
| `Any` | `Any` |
| `Throwable` | `KotlinThrowable` |

### Nullable primitive jadi objek

Perhatikan dua baris ini dari header project kita:

```objc
@property (readonly) double celsius;
@property (readonly) ComposeAppDouble * _Nullable humidityPercent;
```

`celsius: Double` tetap primitif. `humidityPercent: Double?` menjadi objek kotak.
Di Swift kamu harus `reading.humidityPercent?.doubleValue`, bukan langsung dipakai
sebagai angka. Kalau field boleh kosong dan sering dipakai berhitung, pertimbangkan
memberi nilai default di Kotlin supaya tetap non-null.

### Enum bukan enum

```swift
switch reading.comfort {
case Comfort.cold: return "cold"
case Comfort.hot: return "hot"
default: return "ok"       // wajib ada, Swift tidak tahu daftarnya lengkap
}
```

`default` tidak bisa dihilangkan. Artinya: menambah entry enum baru di Kotlin tidak akan
menghasilkan error kompilasi di Swift. Cabang baru itu diam-diam jatuh ke `default`.
Kalau logikanya penting, kerjakan pemetaan enum di Kotlin, jangan di Swift.

### Duration bocor sebagai angka mentah

```objc
- (instancetype)initWithApi:(ComposeAppTemperatureApi *)api pollInterval:(int64_t)pollInterval;
```

`kotlin.time.Duration` sampai ke Swift sebagai `Int64` dengan representasi internal, bukan
detik dan bukan milidetik. Jangan pernah membuat objek Kotlin dari Swift kalau ada parameter
`Duration`. Sediakan factory di Kotlin.

Aturan umum: **jangan konstruksi objek Kotlin dari Swift.** Buat fungsi factory di Kotlin
dan panggil itu. Contoh nyata dari project ini, `HttpClient()` tidak bisa dipanggil dari
Swift sama sekali:

```
error: cannot find 'HttpClient' in scope
```

---

## 4. Fungsi suspend

Fungsi `suspend` diekspor sebagai method dengan completion handler, dan Swift otomatis
membacanya sebagai `async throws`.

Kotlin:

```kotlin
suspend fun latestOnce(sensorId: String): TemperatureReading
```

Header:

```objc
- (void)latestSensorId:(NSString *)sensorId
    completionHandler:(void (^)(ComposeAppTemperatureDto * _Nullable, NSError * _Nullable))completionHandler;
```

Swift:

```swift
func loadOnce() async {
    do {
        let reading = try await FlowBridgeKt.latestOnce(sensorId: "living-room")
        print(reading.celsius)
    } catch {
        print(error.localizedDescription)
    }
}
```

Batasnya: fungsi `suspend` hanya boleh dipanggil dari main thread iOS. Kalau dipanggil dari
thread lain, aplikasi berhenti dengan `IllegalStateException`.

---

## 5. Flow tidak bisa dipakai langsung

`Flow` diekspor sebagai protokol buram:

```objc
@protocol ComposeAppKotlinx_coroutines_coreFlow
- (void)collectCollector:(id<ComposeAppKotlinx_coroutines_coreFlowCollector>)collector
    completionHandler:(void (^)(NSError * _Nullable))completionHandler;
@end
```

Tidak ada pembatalan, tidak ada `AsyncSequence`, tidak ada tipe elemen. Praktis tidak bisa
dipakai. Solusinya bungkus di sisi Kotlin. Kelas berikut sudah dikompilasi dan dipanggil
dari Swift di project ini:

```kotlin
// composeApp/src/iosMain/kotlin/my/openlab/roomy/platform/FlowBridge.kt
package my.openlab.roomy.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowBridge<T : Any>(private val flow: Flow<T>) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    fun start(onEach: (T) -> Unit, onError: (Throwable) -> Unit) {
        job = scope.launch {
            try {
                flow.collect { onEach(it) }
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

fun temperatureBridge(sensorId: String): FlowBridge<TemperatureReading> =
    FlowBridge(AppModule.observeTemperature(sensorId))
```

Generic tetap terbaca di Swift:

```objc
+ (ComposeAppFlowBridge<ComposeAppTemperatureReading *> *)temperatureBridgeSensorId:(NSString *)sensorId;
```

Pemakaian di Swift:

```swift
@MainActor
final class TemperatureModel: ObservableObject {
    @Published var celsius: Double = 0
    @Published var error: String?

    private var bridge: FlowBridge<TemperatureReading>?

    func start() {
        let b = FlowBridgeKt.temperatureBridge(sensorId: "living-room")
        bridge = b
        b.start(
            onEach: { reading in
                Task { @MainActor in self.celsius = reading.celsius }
            },
            onError: { throwable in
                Task { @MainActor in self.error = throwable.message }
            }
        )
    }

    func stop() { bridge?.cancel() }
}
```

Wajib panggil `cancel()` di `onDisappear` atau `deinit`. Kalau lupa, coroutine polling terus
jalan sampai app mati.

**Untuk project ini kamu belum butuh semua itu.** UI-nya Compose dan hidup di Kotlin, jadi
Flow tidak pernah menyeberang. Bridge di atas baru perlu kalau ada layar SwiftUI asli yang
mau ikut membaca data sensor.

Alternatif kalau nanti banyak layar SwiftUI: pasang plugin SKIE, yang mengubah `Flow` jadi
`AsyncSequence` dan `sealed class` jadi enum Swift yang exhaustive.

---

## 6. Exception Kotlin bisa mematikan aplikasi

Header project ini menuliskannya terang-terangan:

```
@note This method converts instances of CancellationException to errors.
      Other uncaught Kotlin exceptions are fatal.
```

Exception Kotlin yang lolos ke Objective-C tidak jadi `NSError`. Aplikasi langsung mati,
tanpa `catch` yang bisa menahannya. Yang menyeberang jadi `NSError` hanya yang ditandai:

```kotlin
@Throws(Exception::class)
suspend fun latestOnce(sensorId: String): TemperatureReading
```

Aturan praktis: setiap fungsi Kotlin yang dipanggil Swift harus salah satu dari dua ini,
menangkap sendiri semua error dan mengembalikan tipe hasil, atau ditandai `@Throws`.
Jangan biarkan error jaringan menyeberang tanpa penanda.

---

## 7. Semua yang public ikut terekspor

Framework project ini mengekspor 108 kelas Objective-C, termasuk `TemperatureApi`,
`TemperatureDto`, dan seluruh isi Ktor serta coroutines. Semuanya cuma karena `public`
adalah default di Kotlin.

Akibatnya header jadi besar, waktu linking naik, dan detail internal terlihat dari Swift.

Perbaikannya murah. Kelas yang tidak dipakai Swift beri `internal`:

```kotlin
internal class TemperatureApi(...)
internal data class TemperatureDto(...)
```

Compose UI tetap bisa memakainya karena masih satu module. Yang hilang cuma dari header iOS.

Kalau perlu tetap `public` tapi disembunyikan dari Swift:

```kotlin
@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@HiddenFromObjC
class InternalDetail
```

Ukuran hasil build debug saat ini:

| Artefak | Ukuran |
|---|---|
| ComposeApp.framework debug simulator | 345 MB |
| Roomy.app debug simulator | 51 MB |

Angka debug memang besar karena penuh simbol debug. Build release jauh lebih kecil, tapi
tetap cek ukurannya sebelum rilis.

---

## 8. Konfigurasi build yang tidak boleh diutak-atik sembarangan

Empat pengaturan di project ini saling terkait. Ubah satu tanpa yang lain, build iOS mati.

**`baseName = "ComposeApp"`** di `composeApp/build.gradle.kts` harus sama persis dengan
`import ComposeApp` di `ContentView.swift`.

**`isStatic = true`** artinya framework statis, langsung ditautkan ke binary. Kalau diubah ke
`false`, kamu harus menambahkan build phase Embed Frameworks di Xcode.

**`FRAMEWORK_SEARCH_PATHS`** di target Xcode menunjuk ke
`$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
Folder itu diisi oleh task Gradle, bukan oleh Xcode.

**`ENABLE_USER_SCRIPT_SANDBOXING = NO`** wajib. Build phase kita menjalankan Gradle, dan
Gradle menulis ke luar folder build Xcode. Dengan sandboxing menyala, Gradle ditolak.

**`EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64`** ada karena Compose Multiplatform 1.11
tidak merilis artefak untuk simulator Intel, jadi target `iosX64` dibuang dari Gradle.
Hapus baris ini dan kamu dapat:

```
error: Xcode Requested Architecture Not Configured in Gradle
```

Kalau nanti benar-benar butuh simulator Intel, tambahkan `iosX64()` di Gradle lebih dulu.

### Build phase yang memanggil Gradle

```sh
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Xcode-lah yang memanggil Gradle, bukan sebaliknya. Jadi tidak perlu build Gradle manual
sebelum menekan Run.

Kalau Xcode mengeluh soal Java, artinya Gradle tidak menemukan JDK dari lingkungan Xcode.
Tambahkan di `gradle.properties`:

```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home
```

---

## 9. Compose di iOS

**Safe area diurus sendiri.** `ContentView.swift` memakai `.ignoresSafeArea(.all)`, jadi
Compose menggambar sampai ke notch dan home indicator. Padding harus dari Compose:

```kotlin
Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) { ... }
```

**Tidak ada tombol back sistem.** iOS mengandalkan gesture geser dari tepi, dan gesture itu
milik UINavigationController yang tidak dipakai di sini. Kalau menambah navigasi, gesture
back harus dibuat sendiri, atau host Compose di dalam UINavigationController.

**Xcode Preview tidak bisa merender Compose.** Preview hanya jalan untuk view SwiftUI asli.
Untuk melihat UI Compose, jalankan di simulator, atau pakai preview Compose dari
Android Studio pada target Android.

**Keyboard, scroll, dan seleksi teks** memakai perilaku iOS bawaan Compose Multiplatform,
tapi tidak identik dengan UIKit. Uji form panjang langsung di device sebelum menyimpulkan.

**Font dan resource** ikut ke dalam framework lewat `compose.components.resources`, bukan
lewat Assets.xcassets. Yang masuk Assets.xcassets cuma ikon aplikasi dan launch screen.

---

## 10. Thread dan memori

Kotlin/Native versi sekarang tidak lagi membekukan objek. Objek boleh dipakai lintas thread,
dan `Dispatchers.Main` di iOS adalah main queue.

Dua hal yang tetap perlu dijaga:

**UI dan fungsi suspend harus di main thread.** Semua panggilan Compose dan semua fungsi
`suspend` yang dipanggil dari Swift wajib dari main thread.

**Objek Kotlin dipegang ARC.** Kalau objek Swift menyimpan objek Kotlin yang balik menyimpan
closure Swift, siklusnya tidak akan pernah lepas. Pakai `[weak self]` di closure yang
diserahkan ke Kotlin, dan panggil `cancel()` secara eksplisit seperti pada `FlowBridge`.

---

## 11. Debug dan log

`println` dari Kotlin muncul di console Xcode. Untuk membedakannya dari log sistem yang
ramai, beri awalan:

```kotlin
println("[Roomy] reading=$reading")
```

Crash dari Kotlin mencetak stack trace Kotlin lengkap dengan nama fungsi di console Xcode.
Baca dari situ dulu sebelum menebak.

Menyetel breakpoint di dalam kode Kotlin dari Xcode berjalan setengah jalan saja, dan
inspeksi variabel sering kosong. Lebih cepat menaruh log, atau memindahkan logika yang
bermasalah ke `commonTest` dan menjalankannya lewat:

```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

Itu alasan lain kenapa logika dijaga tetap di `domain`. Yang ada di `domain` bisa diuji
tanpa simulator sama sekali.

---

## 12. Hal yang Kotlin tidak bisa urus

Semua ini tinggal di sisi Xcode dan tidak akan pernah muncul dari Kotlin.

- Izin dan keterangannya di `iosApp/iosApp/Info.plist`, misalnya
  `NSLocalNetworkUsageDescription` kalau app menembak gateway di LAN.
- Bundle identifier, tim signing, provisioning profile.
- Ikon aplikasi di `Assets.xcassets/AppIcon.appiconset`.
- Launch screen.
- Kemampuan seperti push notification dan background mode.
- App Transport Security. Gateway HTTP polos butuh pengecualian `NSAppTransportSecurity`
  di Info.plist, dan App Store akan menanyakan alasannya.

---

## 13. Checklist sebelum menyentuh sisi iOS

- [ ] Ganti nama file Kotlin yang berisi fungsi top-level? Perbaiki juga nama `...Kt` di Swift.
- [ ] Menambah fungsi `suspend` yang dipanggil Swift? Tandai `@Throws`.
- [ ] Menambah dependency di `commonMain`? Pastikan ada varian `ios_arm64` di Maven.
- [ ] Menambah kelas yang tidak dipakai Swift? Beri `internal`.
- [ ] Menambah entry enum? Cek semua `switch` di Swift, karena tidak akan ada error kompilasi.
- [ ] Mengubah `baseName`? Ubah juga `import` di `ContentView.swift`.
- [ ] Build gagal setelah tarik perubahan? `./gradlew clean` lalu hapus DerivedData iosApp.
