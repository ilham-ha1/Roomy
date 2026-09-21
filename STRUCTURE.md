# Struktur Project Roomy

Dokumen ini menjawab satu pertanyaan: file baru harus ditaruh di mana, dan kenapa.
Angka-angka di bawah dihitung dari isi repository ini, bukan contoh.

---

## Peta besar

```
Roomy/
├── composeApp/          <- semua kode aplikasi, Kotlin
│   └── src/
│       ├── commonMain/    1415 baris  dipakai Android dan iOS
│       ├── androidMain/     20 baris  khusus Android
│       ├── iosMain/          5 baris  khusus iOS
│       └── commonTest/      88 baris  test untuk kode bersama
├── iosApp/              <- cangkang Xcode, Swift
└── gradle/              <- versi library terkunci di sini
```

Rasionya yang penting. Dari 1440 baris kode aplikasi, 1415 dipakai bersama. Sisanya, 25
baris, adalah titik masuk per platform. Itulah janji Kotlin Multiplatform yang benar-benar
terwujud di project ini: yang berbeda hanya cara aplikasi dinyalakan, bukan isinya.

---

## composeApp

Satu Gradle module, tiga hasil build sekaligus:

- APK Android
- `ComposeApp.framework` untuk simulator Apple Silicon
- `ComposeApp.framework` untuk iPhone asli

Kotlin memanggil hasil-hasil itu **target**. Target yang aktif di project ini terlihat dari
nama task Gradle:

```
compileKotlinIosArm64            iPhone asli
compileKotlinIosSimulatorArm64   simulator
compileDebugKotlinAndroid        Android
```

Folder di bawah `composeApp/src/` disebut **source set**. Source set adalah kumpulan kode
yang dipakai oleh target tertentu. Satu source set bisa melayani banyak target.

---

## commonMain

Kode yang dikompilasi untuk semua target. Isinya 20 file dan seluruh logika aplikasi.

```
commonMain/kotlin/my/openlab/roomy/
├── App.kt                    titik masuk Compose bersama
├── di/                       AppConfig dan modul Koin
├── domain/                   model, kontrak repository, use case
├── data/                     Ktor, DTO, mapper, implementasi repository
└── presentation/             layar, ViewModel, komponen animasi, tema
```

Aturannya satu kalimat: **kalau bisa ditulis di sini, tulis di sini.**

Yang boleh masuk `commonMain` hanya API yang ada di semua platform, yaitu Kotlin standard
library, coroutines, serialization, Ktor, Compose Multiplatform, dan Koin. Menulis
`android.content.Context` di sini langsung gagal kompilasi, karena kelas itu tidak ada
di Kotlin/Native.

UI ikut di sini. Compose Multiplatform artinya `TemperatureScreen.kt` yang sama menggambar
di Android maupun iOS, bukan dua implementasi yang mirip.

---

## androidMain

Tiga file, 20 baris Kotlin. Isinya cuma yang tidak mungkin ditulis di `commonMain`:

```
androidMain/
├── AndroidManifest.xml                  izin, ikon, activity yang diluncurkan
├── kotlin/.../MainActivity.kt           Activity yang memanggil App()
└── res/values/strings.xml               nama aplikasi
```

`MainActivity.kt` hanya menyalakan edge-to-edge lalu memanggil `setContent { App() }`.
Tidak ada logika di dalamnya, dan memang tidak boleh ada.

Selain jadi rumah kode Android, source set ini juga tempat dependency yang cuma ada di
Android, misalnya engine HTTP OkHttp.

---

## iosMain

Satu file, lima baris:

```kotlin
package my.openlab.roomy

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

Fungsi ini membungkus UI Compose menjadi `UIViewController`, yaitu tipe layar yang dikenal
iOS. Swift memanggilnya, lalu dari titik itu seluruh layar adalah Compose.

Seperti `androidMain`, source set ini juga rumah bagi dependency khusus iOS, misalnya
engine HTTP Darwin.

---

## Hirarki source set

Source set tidak sejajar. Kotlin menyusunnya bertingkat, dan tingkat yang lebih bawah bisa
melihat semua yang di atasnya:

```
                commonMain
                /        \
        androidMain      appleMain
                             |
                          iosMain
                          /      \
              iosArm64Main    iosSimulatorArm64Main
```

`appleMain` dan `iosArm64Main` dibuat otomatis oleh Kotlin, tidak ada foldernya di
repository ini karena belum dipakai. Namanya tetap muncul di pesan error. Contoh nyata yang
pernah terjadi di project ini:

```
Source set 'appleMain' couldn't resolve dependencies for all target platforms
Unresolved platforms: [iosX64]
```

Arah pandangnya satu arah. `androidMain` bisa memakai apa pun dari `commonMain`.
`commonMain` tidak bisa memakai apa pun dari `androidMain`. Kalau `commonMain` butuh sesuatu
yang hanya ada di platform, jalannya lewat `expect`/`actual`, yang dijelaskan di
`DEVELOPING.md`.

---

## commonTest dan test per platform

`commonTest` berisi test untuk kode bersama, dan dijalankan berkali-kali, sekali untuk tiap
target:

```bash
./gradlew :composeApp:test                    # jalankan di JVM lewat Android
./gradlew :composeApp:iosSimulatorArm64Test   # jalankan di Kotlin/Native
```

Enam test yang sama lulus di kedua tempat. Ini bukan pemborosan. Kotlin/Native dan JVM punya
perilaku berbeda pada pembulatan, thread, dan urutan inisialisasi, jadi menjalankan test dua
kali memang menangkap masalah nyata.

Kalau butuh test khusus platform, buat `androidUnitTest` atau `iosTest` di sebelahnya.

---

## iosApp

Ini yang paling sering bikin bingung, karena namanya seolah sejajar dengan `androidMain`,
padahal bukan.

`iosApp` adalah **project Xcode**, ditulis dalam Swift, dan berada di luar `composeApp`.
Isinya enam file dan tidak ada satu pun logika aplikasi:

| File | Baris | Isi |
|---|---|---|
| `iosApp.xcodeproj/project.pbxproj` | 283 | Konfigurasi project Xcode |
| `iosApp/Info.plist` | 40 | Bundle id, versi, gaya status bar |
| `iosApp/ContentView.swift` | 17 | Membungkus `MainViewController()` jadi view SwiftUI |
| `iosApp/iOSApp.swift` | 10 | Titik masuk `@main` |
| `Assets.xcassets/...` | 2 file | Ikon aplikasi |

`ContentView.swift` melakukan satu hal:

```swift
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

Setelah baris itu, semua yang tampil di layar adalah Kotlin.

### Kenapa iOS butuh folder terpisah sedangkan Android tidak

Android Gradle Plugin bisa membangun APK langsung dari Gradle, jadi rumah kode Android cukup
menumpang di dalam `composeApp`. Xcode tidak bisa dikemudikan dari Gradle. Menandatangani
aplikasi, mendaftarkan izin, memasang ikon, dan mengirim ke App Store semuanya butuh project
Xcode yang asli. Karena itu iOS punya folder sendiri, dan asimetri ini memang disengaja.

### Alur build iOS

```
Xcode Run
  -> build phase "Compile Kotlin Framework"
       -> ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
            -> ComposeApp.framework
  -> Swift dikompilasi, framework ditautkan
  -> Roomy.app
```

Xcode yang memanggil Gradle, bukan sebaliknya. Jadi tidak perlu menjalankan Gradle secara
manual sebelum menekan Run.

Detail jebakan Kotlin ke Swift ada di `IOS_NOTES.md`.

---

## Menaruh file baru

| Yang mau ditulis | Lokasi |
|---|---|
| Layar, ViewModel, model, repository, use case | `commonMain` |
| Komponen UI | `commonMain`, karena Compose jalan di dua platform |
| Butuh `Context`, `NotificationManager`, izin Android | `androidMain` |
| Butuh `UNUserNotificationCenter`, `UIDevice`, API UIKit | `iosMain` |
| Kontrak untuk dua kode di atas | `commonMain`, pakai `expect`/`actual` |
| Test logika | `commonTest` |
| Izin iOS, ikon, signing, bundle id | `iosApp` |
| Izin Android, ikon, nama aplikasi | `androidMain/AndroidManifest.xml` |
| Versi library | `gradle/libs.versions.toml` |

Kalau ragu, mulai dari `commonMain`. Compiler yang akan memberitahu kalau kodenya tidak bisa
di sana, dan itu jauh lebih murah daripada menemukan belakangan bahwa logika yang sama
ditulis dua kali.
