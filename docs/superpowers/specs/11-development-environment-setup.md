# Development Environment Setup — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Tujuan:** Dari "Android Studio belum terinstall" → "Anchor menampilkan Hello World di HP."
> **Prinsip:** Hanya instal yang benar-benar dibutuhkan. Jangan over-install.

---

## 1. Yang Perlu Di-install (dan Kenapa)

| # | Komponen | Kenapa | Versi rekomendasi |
|---|---|---|---|
| 1 | **Android Studio** | IDE resmi + SDK + emulator | Versi stabil terbaru (Koala/Ladybug atau lebih baru) |
| 2 | **Android SDK Platform** | Kompilasi & target | API 35 (atau stabil terbaru) + API 28 (minSdk) |
| 3 | **Build Tools** | Build APK | Terbaru yang disarankan Studio |
| 4 | **Platform Tools (adb)** | Deploy ke device | Terbaru |
| 5 | **JDK** | Kompilasi Kotlin/Java | JDK 17 (bundel dengan Studio/Gradle) |
| 6 | **Git** | Version control | Terbaru |
| 7 | **GitHub** (opsional) | Remote backup | — |
| 8 | **Physical Android device** | Uji nyata (wajib untuk fitur Android) | Android 9+, RAM 3–4 GB |
| 9 | **Kabel USB / USB debugging** | Deploy & log | — |

> **Tidak perlu sekarang:** NDK, CMake, Docker, backend tools, database server eksternal.
> Anchor local-first tanpa backend.

---

## 2. Langkah Instalasi (Android Studio)

### 2.1 Download & Install
1. Unduh Android Studio dari situs resmi Android Developer.
2. Jalankan installer → Next sampai selesai (setuju license).
3. Buka Android Studio → **Standard installation** (bukan Custom) untuk pemula.

### 2.2 Setup Wizard
1. Pilih theme (bebas).
2. SDK Components → biarkan default (SDK, Platform-Tools, Emulator, Build Tools).
3. Selesaikan wizard → Studio mengunduh komponen.

### 2.3 Verifikasi JDK
- Studio membundel JDK 17 (via Gradle JDK).
- Cek: **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.
- Seharusnya menunjuk ke JDK 17 bawaan Studio (jbr).

### 2.4 SDK Manager
- **Tools → SDK Manager → SDK Platforms**
  - Centang: Android API **35** (atau stabil terbaru) + **API 28**.
- **SDK Tools**
  - Pastikan tercentang: Android SDK Build-Tools, Platform-Tools, Android Emulator,
    Android SDK Command-line Tools.

### 2.5 Environment Variables (opsional, untuk CLI)
- `ANDROID_HOME` → path SDK (mis. `C:\Users\<user>\AppData\Local\Android\Sdk`).
- Tambahkan `platform-tools` ke `PATH` agar `adb` bisa dipanggil dari mana saja.

**Cek:** buka PowerShell baru → `adb version` → harus muncul versi.

---

## 3. Device Setup (Physical)

### 3.1 Aktifkan Developer Options
1. Settings → About phone.
2. Tap **Build number** 7x → "You are now a developer".

### 3.2 Aktifkan USB Debugging
1. Settings → System → Developer options.
2. Aktifkan **USB debugging**.

### 3.3 Hubungkan
1. Colok kabel USB (kabel data, bukan hanya charging).
2. Di HP: izinkan "Allow USB debugging?" → **Always allow**.
3. Cek di PowerShell:
   ```
   adb devices
   ```
   → device muncul dengan status `device` (bukan `unauthorized`).

### 3.4 Jika `unauthorized`
- Cabut-colok ulang, terima dialog izin di HP.
- Atau: `adb kill-server; adb start-server`.

---

## 4. Membuat Project (setelah Android Studio terinstall)

> Belum dilakukan sekarang (masih pre-development). Langkah ini untuk nanti.

1. **New Project → Empty Activity** (Compose).
2. Name: `Anchor` · Package: `com.dafi.anchor` (atau sesuai).
3. Language: Kotlin · Minimum SDK: **API 28**.
4. Build configuration language: Kotlin DSL (`.kts`) direkomendasikan.
5. Finish → tunggu Gradle sync.

### 4.1 Gradle Sync
- Pastikan tidak ada error; jika ada, ikuti saran Studio (biasanya unduh Gradle/AGP).
- Cek `build.gradle.kts` (app): `compileSdk`, `minSdk = 28`, `targetSdk`.

### 4.2 Run ke Device
1. Pilih device di dropdown (nama HP-mu).
2. Tekan **Run ▶**.
3. App "Hello World" (Compose) harus muncul di HP.

### 4.3 Verifikasi Compose
- Pastikan `setContent { }` dengan `@Composable` tampil.
- Coba ubah teks "Hello Anchor" → Run ulang → terlihat berubah (hot reload via Apply Changes).

---

## 5. Git Setup

```
git init                     # sudah dilakukan di repo ini
```
Buat `.gitignore` (Android):
```
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
```
Commit awal:
```
git add .
git commit -m "chore: initial project scaffold"
```

(Opsional) Tambahkan remote GitHub untuk backup.

---

## 6. Verifikasi Cepat (Checklist)

```
[ ] Android Studio terbuka tanpa error
[ ] SDK Manager: API 35 + API 28 terinstall
[ ] adb version → menampilkan versi
[ ] adb devices → device muncul sebagai "device"
[ ] Project Compose baru bisa sync
[ ] Run ▶ → app muncul di HP
[ ] Git repo aktif + .gitignore ada
```

---

## 7. Tips Troubleshooting

| Masalah | Solusi |
|---|---|
| `adb` tidak dikenali | Tambahkan `platform-tools` ke PATH, buka terminal baru |
| Device `unauthorized` | Terima dialog di HP; restart adb |
| Gradle sync gagal | Cek koneksi; File → Invalidate Caches / Restart |
| Emulator lambat | Gunakan device fisik (lebih cepat & realistis) |
| Build lambat | Aktifkan Gradle daemon; tambah RAM di `gradle.properties` |
| HP tidak terdeteksi | Coba kabel lain; mode transfer file (MTP) |
| USB debugging mati sendiri | Beberapa OEM menonaktifkan otomatis → aktifkan ulang |

---

## 8. Setelah Setup

Setelah "Hello World" jalan di HP, langkah berikutnya adalah **Stage 1: Spike A (UsageStats)**
dan **Stage 2: Spike B (Accessibility)** sebelum membangun fondasi project —
lihat `11-development-roadmap.md`.
