# Hal Aneh yang Bisa Dilakukan di androidMain dan iosMain

`commonMain` hidup di ruang sempit: hanya API yang ada di semua platform. Begitu turun ke
`androidMain` atau `iosMain`, batas itu hilang total. Di sana Kotlin bukan lagi bahasa yang
netral, melainkan warga penuh JVM di satu sisi dan warga penuh Objective-C di sisi lain.

Semua kode di dokumen ini sudah dikompilasi terhadap project ini, lalu dihapus lagi.
Yang gagal kompilasi ikut dicatat, karena justru itu yang paling mahal kalau tidak tahu.

---

## Kenapa ini mungkin

Dua hal sekaligus berlaku di source set platform:

1. Semua isi `commonMain` terlihat dari sana.
2. Seluruh API platform terbuka, tanpa pembungkus dan tanpa perantara.

Arah pandangnya tetap satu arah. `androidMain` melihat `commonMain`, tidak sebaliknya.

---

## androidMain

### 1. `actual` yang isinya kelas milik orang lain

`expect class` tidak harus dijawab dengan kelas yang kamu tulis. Boleh dijawab dengan
`typealias` ke kelas yang sudah ada:

```kotlin
// commonMain
expect class PlatformFile

// androidMain
actual typealias PlatformFile = java.io.File
```

`java.io.File` mendadak menjadi tipe yang dikenal `commonMain`, tanpa satu pun wrapper.
Ini cara paling murah membawa tipe platform ke kode bersama.

### 2. File Java ikut dikompilasi, tapi salah folder berarti bom waktu

Android target masih JVM, jadi `.java` boleh hidup berdampingan dengan Kotlin:

```java
package my.openlab.roomy.legacy;

public final class Legacy {
    public static int checksum(String value) { ... }
}
```

Jebakannya nyata dan sudah saya buktikan di project ini. Taruh file itu di
`src/androidMain/kotlin/`, dan Kotlin **berhasil kompilasi**, karena Kotlin ikut membaca
sumber Java untuk keperluan resolusi. Tapi javac tidak pernah menyentuhnya, tidak ada
`Legacy.class` yang dihasilkan, dan aplikasi mati saat dijalankan.

Folder yang benar terlihat dari Gradle sendiri:

```bash
./gradlew :composeApp:sourceSets
```

```
main
----
Java sources: [composeApp/src/main/java]
```

Jadi taruh di `composeApp/src/main/java/`. Setelah dipindah ke sana, kelasnya benar-benar
muncul:

```
composeApp/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/.../Legacy.class
```

Bukti bahwa build hijau tidak sama dengan aplikasi jalan.

### 3. Menyisipkan View Android asli ke dalam UI Compose bersama

Layar utama Roomy ditulis sekali di `commonMain`. Tapi satu bagian di dalamnya boleh diisi
View Android sungguhan, lewat pasangan `expect`/`actual` composable:

```kotlin
// commonMain
@Composable
expect fun PlatformWebView(url: String, modifier: Modifier)

// androidMain
@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    AndroidView(
        factory = { context -> WebView(context).apply { loadUrl(url) } },
        modifier = modifier,
    )
}
```

Composable bisa dijadikan `expect`. Artinya sebagian kecil UI boleh turun ke widget asli
tanpa merobek arsitektur bersama.

### 4. Menangkap Context tanpa membuat kelas Application

Trik lama Android yang masih bekerja. `ContentProvider` dijalankan sistem sebelum
`Application.onCreate`, jadi bisa dipakai hanya untuk mencuri Context:

```kotlin
object AppContextHolder {
    lateinit var value: Context
}

class ContextGrabber : ContentProvider() {
    override fun onCreate(): Boolean {
        AppContextHolder.value = requireNotNull(context).applicationContext
        return true
    }
    // sisanya dikembalikan null atau 0
}
```

Daftarkan di manifest dengan `authorities` yang unik. Library seperti WorkManager dan
Firebase memakai cara ini untuk menginisialisasi diri sendiri tanpa merepotkan pemakainya.
Berguna di KMP karena `commonMain` tidak punya konsep Context sama sekali.

### 5. Refleksi

Kotlin/Native tidak punya refleksi penuh. JVM punya, jadi di `androidMain` boleh:

```kotlin
fun fieldNamesOf(instance: Any): List<String> =
    instance::class.java.declaredFields.map { it.name }
```

Konsekuensinya, library yang bergantung pada refleksi seperti Gson dan sebagian besar
framework mocking hanya bisa dipakai di sisi Android.

### 6. Menyadap semua crash aplikasi

```kotlin
fun installCrashHandler(onCrash: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        onCrash(error)
        previous?.uncaughtException(thread, error)
    }
}
```

Tidak ada padanannya di iOS, karena crash Kotlin/Native mematikan proses tanpa bisa
ditahan.

### 7. Anotasi yang tidak berarti apa-apa di tempat lain

`@JvmStatic`, `@JvmName`, `@JvmOverloads`, `@Synchronized`, `@Volatile`. Semua legal di
`androidMain` dan tidak ada artinya di `commonMain`.

### 8. Source set khusus debug

Selain `androidMain`, AGP mengenal `src/debug/` dan `src/release/`. Manifest di `src/debug/`
digabung hanya untuk build debug. Ini cara aman menyalakan `usesCleartextTraffic` untuk
gateway HTTP di LAN tanpa ikut terbawa ke rilis.

---

## iosMain

### 1. Memanggil UIKit langsung, tanpa jembatan apa pun

Tidak ada wrapper, tidak ada plugin, tidak ada file Swift perantara. UIKit langsung tersedia
sebagai package Kotlin:

```kotlin
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication

fun showNativeAlert(title: String, message: String) {
    val alert = UIAlertController.alertControllerWithTitle(
        title = title,
        message = message,
        preferredStyle = UIAlertControllerStyleAlert,
    )
    alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(alert, animated = true, completion = null)
}
```

Hasilnya dialog iOS asli yang muncul di atas UI Compose.

### 2. Kelas Kotlin yang menjadi delegate Objective-C

Kotlin boleh mewarisi `NSObject` dan memenuhi protokol Objective-C. UIKit tidak bisa
membedakannya dari kelas Swift:

```kotlin
class ForegroundNotificationDelegate :
    NSObject(),
    UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(UNNotificationPresentationOptionBanner)
    }
}
```

Ini yang membuat notifikasi tetap tampil saat aplikasi sedang dibuka, dan seluruhnya
ditulis di Kotlin.

Perhatikan namanya. Protokol Objective-C `UNUserNotificationCenterDelegate` menjadi
`UNUserNotificationCenterDelegateProtocol` di Kotlin. Akhiran `Protocol` selalu ditambahkan.

### 3. Menyisipkan WKWebView asli ke UI Compose bersama

Pasangan dari trik `AndroidView` di atas:

```kotlin
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    UIKitView(
        factory = {
            WKWebView().apply {
                NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
        modifier = modifier,
    )
}
```

`UIKitView` menuntut `@OptIn(ExperimentalForeignApi::class)`. Tanpa itu build gagal dengan
`This declaration needs opt-in`.

### 4. Pointer C mentah, di dalam Kotlin

Yang paling jauh dari kesan "Kotlin". Alokasi memori manual, dengan aritmetika pointer:

```kotlin
@OptIn(ExperimentalForeignApi::class)
fun scratchPointer(): UInt = memScoped {
    val slot = alloc<UIntVar>()
    slot.value = 42u
    slot.ptr.pointed.value
}
```

`memScoped` membebaskan semuanya saat blok selesai. Ini pintu masuk ke library C mana pun
lewat cinterop, termasuk library C milik sendiri.

### 5. Loncat ke main queue tanpa coroutine

```kotlin
@OptIn(ExperimentalForeignApi::class)
fun onMainQueue(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) { block() }
}
```

Grand Central Dispatch dipanggil langsung dari Kotlin. Berguna saat harus menyentuh UIKit
dari kode yang tidak dijalankan coroutine.

### 6. Mendengarkan siaran sistem iOS

```kotlin
fun observeKeyboard(onShown: () -> Unit) {
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIKeyboardWillShowNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> onShown() }
}
```

Perhatikan `` `object` `` diapit backtick, karena itu kata kunci Kotlin. Nama parameter
Objective-C yang bentrok dengan kata kunci Kotlin selalu ditulis begitu.

### 7. Membaca Info.plist dari Kotlin

Info.plist milik folder `iosApp`, tapi isinya bisa dibaca dari sisi Kotlin:

```kotlin
fun appVersion(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?"
```

Artinya nomor versi cukup ditulis sekali di Xcode, lalu dibaca oleh kode bersama lewat
`expect`/`actual`.

### 8. Info perangkat

```kotlin
fun deviceSummary(): String {
    val device = UIDevice.currentDevice
    return "${device.name} ${device.systemName} ${device.systemVersion}"
}
```

### 9. `actual` berupa tipe Foundation

Sama seperti `java.io.File` di Android, tipe Apple bisa langsung dijadikan jawaban:

```kotlin
actual typealias PlatformFile = NSURL
```

---

## Satu expect, dua gaya actual yang sama sekali berbeda

Ini yang paling sering luput. Kedua `actual` tidak perlu mirip, bahkan tidak perlu sama
bentuknya:

```kotlin
// commonMain
expect class PlatformFile
expect fun platformFileName(file: PlatformFile): String

// androidMain
actual typealias PlatformFile = java.io.File
actual fun platformFileName(file: PlatformFile): String = file.name

// iosMain
actual typealias PlatformFile = NSURL
actual fun platformFileName(file: PlatformFile): String = file.lastPathComponent ?: ""
```

Satu memakai `java.io.File`, satu memakai `NSURL`. `commonMain` tidak pernah tahu, dan tidak
perlu tahu.

---

## Yang tetap tidak bisa

| Keinginan | Kenyataan |
|---|---|
| `commonMain` memanggil fungsi dari `androidMain` | Tidak bisa. Jalannya cuma `expect`/`actual` |
| `iosMain` memakai kode `androidMain` | Tidak bisa. Keduanya bersaudara, bukan bertingkat |
| Refleksi penuh di `iosMain` | Tidak ada. Library berbasis refleksi mati di sini |
| File `.java` di `iosMain` | Tidak ada JVM di sana |
| Menangkap crash Kotlin di iOS | Proses langsung mati, kecuali fungsinya ditandai `@Throws` |
| `expect` tanpa `actual` di salah satu target | Build gagal, bukan peringatan |

---

## Jebakan yang sudah terbukti di project ini

| Gejala | Sebab | Perbaikan |
|---|---|---|
| Kompilasi hijau, aplikasi mati saat jalan | `.java` ditaruh di `androidMain/kotlin` | Pindahkan ke `src/main/java` |
| `This declaration needs opt-in` | `UIKitView` dan seluruh cinterop | Tambah `@OptIn(ExperimentalForeignApi::class)` |
| `Unresolved reference 'pointed'` | Ekstensi pointer harus diimpor satu per satu | `import kotlinx.cinterop.pointed` |
| `Unresolved platforms: [iosX64]` | Library tidak punya artefak untuk target itu | Buang target, atau turunkan versi library |

---

## Aturan pakai

Semua trik di atas menarik, dan hampir semuanya salah tempat kalau dipakai sembarangan.

Pindahkan kode ke source set platform hanya ketika `commonMain` benar-benar menolaknya, dan
sisakan di sana sesedikit mungkin. Ukurannya sudah ada di project ini: 1415 baris di
`commonMain`, 20 baris di `androidMain`, 5 baris di `iosMain`. Setiap baris yang pindah ke
platform adalah baris yang harus ditulis dan diuji dua kali.
