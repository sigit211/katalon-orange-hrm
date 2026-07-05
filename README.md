# Katalon Orange HRM

Proyek pengujian otomatisasi untuk aplikasi Orange HRM menggunakan Katalon Studio.

## Ringkasan
- Framework: Katalon Studio (tipe project: WEBUI)
- Versi Katalon yang tercatat di project: 10.3.2
- Tujuan: mengelola test case dan test suite untuk alur login dan regresi pada Orange HRM.

## Prasyarat
- Katalon Studio (disarankan versi >= 10.3.2)
- Java JDK 11+ (atau versi yang kompatibel dengan Katalon Anda)
- (Opsional) Gradle jika ingin menjalankan lewat Gradle (lihat `build.gradle`)

## Struktur penting
- `build.gradle` — konfigurasi Gradle / plugin Katalon
- `katalon-orange-hrm.prj` — file project Katalon
- `Include/` — skrip, fitur, dan konfigurasi proyek
- `Test Cases/` — folder test case (mis. `Test Cases/LOGIN/`)
- `Test Suites/` — daftar test suite (mis. `REGRESSION.ts`)

## Menjalankan pengujian
1. Cara cepat (GUI)
   - Buka Katalon Studio → Open Project → pilih folder proyek.
   - Jalankan Test Case atau Test Suite dari Katalon Studio.

2. Menggunakan Gradle (jika plugin terkonfigurasi)
   - Lihat task Gradle tersedia:

```bash
# Linux/macOS
./gradlew tasks

# Windows (wrapper)
gradlew.bat tasks

# Jika tidak ada wrapper
gradle tasks
```

3. Contoh menggunakan Katalon Console (sesuaikan path/parameter):

```bash
katalon -noSplash -runMode=console -projectPath="E:/dev/projects/qa/web/katalon-orange-hrm" -retry=0 -testSuitePath="Test Suites/REGRESSION" -executionProfile="default" -browserType="Chrome"
```

Pastikan executable Katalon tersedia di PATH atau gunakan path lengkap ke file executable.

## Konfigurasi lokal
- Periksa `Include/config/local.properties` dan `Include/config/local.properties.example` untuk konfigurasi environment.
- Jangan commit credentials sensitif; simpan di `local.properties` yang di-ignore.

## Kontribusi
- Ikuti struktur folder saat menambahkan test case atau keyword.
- Tambahkan dokumentasi singkat pada custom keyword di `Keywords/` untuk memudahkan penggunaan.

--
credits
Sigit Wahyudi
s.wahyudi21@gmail.com 
