# Mulai dari Sini

Dokumen untuk orang yang baru pertama kali membuka repository ini. Tidak mengasumsikan kamu
pernah menyentuh Kotlin Multiplatform, Compose, atau iOS.

Baca berurutan. Sekitar satu jam sampai bisa mengubah sesuatu dengan yakin.

---

## 1. Aplikasi ini sebenarnya apa

Roomy membaca suhu dari sensor IoT dan menampilkannya. Satu layar saja.

Yang membuatnya menarik bukan fiturnya, tapi bentuknya: **satu kode, dua aplikasi.**
Android dan iOS memakai logika yang sama dan tampilan yang sama. Bukan dua tim menulis hal
serupa, melainkan satu berkas `TemperatureScreen.kt` yang menggambar di dua sistem operasi.

Angkanya di project ini:

| Kode | Baris |
|---|---|
| Dipakai bersama | 1415 |
| Khusus Android | 20 |
| Khusus iOS | 5 |

Dua puluh lima baris itu hanya cara aplikasi dinyalakan. Sisanya sama persis.

---

## 2. Yang harus terpasang

| Alat | Untuk apa | Cek |
|---|---|---|
| JDK 17 | Menjalankan Gradle | `java -version` |
| Android Studio | Menulis kode, emulator | Buka aplikasinya |
| Xcode | Build iOS | `xcodebuild -version` |

Xcode hanya perlu kalau kamu mengerjakan sisi iOS. Sisi Android jalan tanpa itu, termasuk
di Windows dan Linux.

Tidak perlu memasang Gradle. Repository ini membawa `gradlew`, yang mengunduh versi Gradle
yang benar sendiri.

---

## 3. Jalankan dulu, mengerti belakangan

Urutannya penting. Jangan lompat.

**Satu.** Pastikan bisa build:

```bash
./gradlew :composeApp:assembleDebug
```

Yang benar berakhir `BUILD SUCCESSFUL`. Kali pertama bisa beberapa menit karena mengunduh
banyak hal. Kali berikutnya beberapa detik.

**Dua.** Nyalakan emulator Android dari Android Studio, lalu:

```bash
./gradlew :composeApp:installDebug
```

Buka aplikasi Roomy di emulator. Angka suhunya bergerak. Datanya palsu, dan itu disengaja,
dijelaskan di bagian 6.

**Tiga.** Jalankan test:

```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

Enam test lulus. Kalau merah padahal kamu belum mengubah apa pun, berhenti dan tanya.
Jangan lanjut menumpuk perubahan di atas project yang sudah rusak.

**Empat.** Kalau punya Mac, buka iOS:

```bash
open iosApp/iosApp.xcodeproj
```

Pilih scheme `iosApp`, tekan Run. Aplikasi yang sama muncul di simulator iPhone.

---

## 4. Kosakata

Kata-kata ini muncul di mana-mana. Hafal artinya dulu, sisanya menyusul.

**Gradle** alat build. Semua perintah `./gradlew ...` adalah Gradle.

**Target** satu hasil build. Project ini punya tiga: Android, simulator iOS, dan iPhone asli.

**Source set** folder kode untuk target tertentu. Yang ada di sini: `commonMain`,
`androidMain`, `iosMain`, `commonTest`.

**expect / actual** cara `commonMain` meminta sesuatu yang hanya ada di platform.
`commonMain` menulis janjinya, tiap platform menulis isinya.

**Composable** fungsi yang menggambar bagian UI. Ditandai `@Composable`. Bukan objek,
melainkan fungsi yang dipanggil ulang setiap kali datanya berubah.

**Recomposition** pemanggilan ulang itu. Compose menjalankan lagi fungsi yang datanya
berubah, dan hanya itu.

**State** data yang kalau berubah memicu recomposition.

**suspend** fungsi yang boleh menunggu tanpa memblokir. Hanya bisa dipanggil dari fungsi
suspend lain atau dari coroutine.

**Flow** aliran nilai yang datang berkali-kali seiring waktu. Cocok untuk sensor.

**StateFlow** Flow yang selalu punya nilai terakhir. Dipakai ViewModel untuk menyimpan
kondisi layar.

**ViewModel** pemegang state layar. Bertahan saat layar diputar.

**Koin** yang merakit objek dan menyerahkannya ke yang butuh. Semua perakitan ada di satu
berkas.

**Repository** satu-satunya pintu ke data. UI tidak pernah menyentuh jaringan langsung.

---

## 5. Ikuti satu angka dari sensor sampai layar

Ini latihan terbaik untuk mengerti sebuah codebase. Buka lima berkas berikut berurutan dan
ikuti satu angka suhu.

**1. Data lahir** di `data/repository/DemoTemperatureRepository.kt:21`

```kotlin
override fun observeLatest(sensorId: String): Flow<TemperatureReading> = flow {
    while (true) {
        emit(TemperatureReading(...))
        delay(2.seconds)
    }
}
```

`emit` melempar satu nilai ke dalam aliran. Loopnya tidak pernah berhenti sendiri.

**2. Use case meneruskan** di `domain/usecase/ObserveTemperature.kt:11`

```kotlin
operator fun invoke(sensorId: String): Flow<TemperatureReading> =
    repository.observeLatest(sensorId)
```

Kelihatan tidak berguna karena isinya cuma meneruskan. Gunanya ada di bagian 8.

**3. ViewModel menampung** di `presentation/temperature/TemperatureViewModel.kt:43`

```kotlin
.collect { reading ->
    _state.update { current ->
        current.copy(isLoading = false, reading = reading, ...)
    }
}
```

`collect` mengambil tiap nilai yang datang. `copy` membuat state baru, bukan mengubah yang
lama. Compose hanya menggambar ulang kalau objeknya berbeda, jadi mengubah isi objek lama
tidak akan terlihat apa-apa.

**4. UI berlangganan** di `App.kt:18`

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

Satu baris ini menyambungkan Flow ke Compose. Setiap kali state berganti, layar digambar
ulang.

**5. Layar menggambar** di `presentation/temperature/TemperatureScreen.kt:87`

```kotlin
val reading = state.reading
```

Dari sini semuanya cuma soal tampilan.

Arahnya selalu sama: **data naik, kejadian turun.** Suhu naik dari repository sampai layar.
Tombol muat ulang turun dari layar sampai ViewModel. Tidak ada jalan pintas.

---

## 6. Kenapa datanya palsu

`di/AppConfig.kt` berisi `useDemoData = true`. Dengan itu aplikasi memakai sensor tiruan
yang menyapu seluruh rentang suhu.

Alasannya sederhana. Gateway aslinya belum ada. Tanpa data palsu, layar hanya menampilkan
pesan error, dan tidak ada yang bisa dikerjakan.

Ubah ke `false` dan isi `baseUrl` kalau gateway sudah siap.

---

## 7. Tugas pertama yang aman

Kerjakan berurutan. Setiap satu, jalankan aplikasinya dan lihat hasilnya.

**Ubah kata di layar.** `presentation/theme/RoomyTheme.kt:54`. Ganti `"Dingin"` jadi apa
saja. Jalankan, lihat berubah.

**Ubah ambang suhu.** `domain/model/TemperatureReading.kt:23`. `COLD_BELOW` dari `20.0` jadi
`24.0`. Warna seluruh layar ikut berubah lebih cepat. Perhatikan bahwa kamu mengubah satu
angka dan tampilan, gauge, serta latar belakang menyesuaikan sendiri. Itu tanda ambangnya
memang tinggal di satu tempat.

**Ubah kecepatan data.** `data/repository/DemoTemperatureRepository.kt`, ganti
`delay(2.seconds)` jadi `delay(500)`, karena `delay` tanpa satuan berarti milidetik. Grafik trennya bergerak jauh lebih cepat.

**Ubah warna.** `presentation/theme/RoomyTheme.kt:19`. Palet dingin, nyaman, dan panas ada
berurutan di berkas yang sama.

**Tambah satu test.** Buka `commonTest/TemperatureMappingTest.kt`, tiru salah satu test yang
ada, lalu jalankan. Rasakan bedanya test hijau dan merah sebelum menulis test sungguhan.

---

## 8. Lima aturan yang kelihatan sepele tapi bukan

### Arah ketergantungan hanya satu

```
presentation  ->  domain  <-  data
```

`domain` tidak boleh mengimpor apa pun dari `data`, `presentation`, Ktor, atau Compose.

Inilah gunanya use case yang tadi kelihatan tidak berguna. Karena `domain` tidak tahu
apa-apa soal jaringan, mengganti HTTP dengan MQTT nanti tidak menyentuh satu pun berkas di
`domain` maupun `presentation`. Aturan yang terasa birokratis di hari pertama adalah yang
menyelamatkan di bulan keenam.

### Versi library hanya boleh di satu berkas

Semua versi ada di `gradle/libs.versions.toml`. Jangan pernah menulis nomor versi di
`build.gradle.kts`. Kalau dua tempat menyebut versi berbeda, yang menang sulit ditebak.

### Mulai dari commonMain

Kalau bingung menaruh berkas baru, taruh di `commonMain`. Compiler akan menolak kalau
memang tidak bisa di sana. Menulis di `androidMain` lebih dulu berarti nanti harus ditulis
ulang untuk iOS.

### Jangan menyentuh state dari luar ViewModel

UI membaca state, tidak mengubahnya. Perubahan selalu lewat fungsi di ViewModel, seperti
`retry()`. Kalau UI ikut menulis state, tidak akan ada satu pun tempat yang bisa dipercaya
soal kondisi layar.

### Bug diperbaiki di akarnya

Kalau sebuah fungsi salah, perbaiki fungsinya, bukan tempat yang kebetulan dilaporkan.
Cari semua pemanggilnya dulu. Satu perbaikan di tempat yang benar selalu lebih kecil
daripada tambalan di lima pemanggil.

---

## 9. Hal Compose yang paling sering menjebak pemula

### `remember` bukan pengganti ViewModel

```kotlin
var count by remember { mutableStateOf(0) }   // hilang saat layar diputar
```

Untuk data yang harus bertahan, pakai ViewModel.

### Membuat objek di dalam composable itu mahal

Fungsi composable dipanggil ulang berkali-kali per detik. Apa pun yang dibuat di dalamnya
ikut dibuat ulang. Bungkus dengan `remember` kalau pembuatannya tidak murah.

### Mengubah isi objek tidak memicu gambar ulang

Compose membandingkan objek, bukan isinya. Karena itu state selalu diganti dengan `copy`,
tidak pernah diubah di tempat.

### Efek samping punya tempatnya sendiri

Memulai sesuatu saat layar muncul bukan ditulis begitu saja di badan composable, melainkan
di `LaunchedEffect`. Kalau ditulis langsung, dia jalan setiap kali gambar ulang, yaitu
puluhan kali per detik.

### State diangkat ke atas

`TemperatureScreen` tidak menyimpan data apa pun. Semua diterima sebagai parameter dan
kejadian dikirim ke atas lewat `onRetry`. Karena itu layar bisa diuji dan dipakai ulang.

---

## 10. Pesan error yang akan kamu temui

| Pesan | Artinya | Perbaikan |
|---|---|---|
| `Unresolved platforms: [iosArm64]` | Library taruhanmu tidak punya versi iOS | Pindah ke `androidMain`, atau cari library multiplatform |
| `expect ... has no actual declaration` | Kamu menulis `expect` tapi lupa isinya di salah satu platform | Lengkapi keduanya |
| `Xcode Requested Architecture Not Configured in Gradle` | Xcode minta arsitektur yang tidak dibangun Gradle | Sudah diatur di project ini, jangan hapus `EXCLUDED_ARCHS` |
| `NoDefinitionFoundException` dari Koin | Ada objek yang belum didaftarkan | Tambahkan di `di/AppModule.kt` dan di `DiGraphTest` |
| `cannot find 'HttpClient' in scope` di Swift | Objek Kotlin tidak bisa dibuat dari Swift | Sediakan fungsi factory di Kotlin |
| Build hijau tapi aplikasi mati saat jalan | Sering karena Koin, karena Koin dicek saat jalan | Jalankan `DiGraphTest` |

Poin terakhir itu penting. **Kompilasi berhasil tidak berarti aplikasi jalan.** Selalu
jalankan aplikasinya, jangan berhenti di `BUILD SUCCESSFUL`.

---

## 11. Ritme kerja harian

```bash
./gradlew :composeApp:installDebug          # ubah kode, pasang, lihat
./gradlew :composeApp:iosSimulatorArm64Test # sebelum menganggap selesai
./gradlew clean                             # hanya kalau build berlaku aneh
```

Sebelum meminta review, pastikan tiga hal: Android jalan, test hijau, dan iOS masih
terkompilasi. Yang ketiga sering terlupa, dan paling mahal kalau ketahuan belakangan.

---

## 12. Selanjutnya ke mana

| Dokumen | Isi |
|---|---|
| `STRUCTURE.md` | Arti `composeApp`, `commonMain`, `androidMain`, `iosMain`, `iosApp` |
| `DEVELOPING.md` | Menambah package, dependency, Koin, lifecycle, notifikasi |
| `IOS_NOTES.md` | Jebakan Kotlin ketika dipanggil dari Swift |
| `PLATFORM_TRICKS.md` | Hal aneh yang bisa dilakukan di source set platform |
| `README.md` | Ringkasan arsitektur dan alasan di baliknya |

Baca `STRUCTURE.md` berikutnya. Sisanya baca saat butuh, bukan sekarang.

---

## 13. Satu nasihat terakhir

Kode yang paling murah dirawat adalah kode yang tidak pernah ditulis. Sebelum menambah
kelas, abstraksi, atau library baru, tanyakan apakah yang sudah ada di project ini bisa
dipakai. Biasanya bisa.

Kalau bingung, tanya lebih awal. Satu pertanyaan lima menit lebih murah daripada dua hari
menulis ke arah yang salah.
