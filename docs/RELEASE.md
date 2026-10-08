# Anchor — Build & Release Guide

> **Status:** Release tooling · **Terkait:** `docs/superpowers/design/05-mvp-scope.md`,
> `09-privacy-and-security.md`, `14-hardening-report.md`
> **Catatan:** Panduan praktis membangun & merilis APK/AAB Anchor. Default locale English,
> Bahasa Indonesia tersedia (`values-id`).

---

## 1. Prasyarat

- JDK 17 (AGP 8.7.3 / Kotlin 2.0.21).
- Android SDK `compileSdk = 35`, `minSdk = 26`.
- `local.properties` berisi `sdk.dir=...` (tidak di-commit).

---

## 2. Build Cepat

| Perintah | Hasil |
|---|---|
| `.\gradlew.bat :app:assembleDebug` | APK debug (`app/build/outputs/apk/debug/app-debug.apk`) |
| `.\gradlew.bat :app:assembleRelease` | APK release (R8 + resource shrink) |
| `.\gradlew.bat :app:bundleRelease` | AAB untuk Play Store |
| `.\gradlew.bat :app:testDebugUnitTest` | Unit test (JVM) |
| `.\gradlew.bat :app:lintDebug` | Lint (0 error) |
| `.\gradlew.bat :app:connectedDebugAndroidTest` | Instrumented test (butuh device/emulator) |

> **Ukuran aktual** (2026-10-08, R8 + shrink): APK ≈ 2.28 MB, AAB ≈ 4.16 MB.
> Ini hasil pengukuran, **bukan** requirement — ukuran dapat berubah dengan tuning
> R8/resource optimization. Ringan mendukung target RAM PRD §12.

---

## 3. Signing Rilis

Rilis membaca kredensial dari `keystore.properties` di root repo (git-ignored):

```properties
storeFile=anchor-release.jks
storePassword=CHANGE_ME
keyAlias=anchor
keyPassword=CHANGE_ME
```

Contoh tersedia di `keystore.properties.example`. Buat keystore sekali:

```powershell
keytool -genkeypair -v -keystore anchor-release.jks -alias anchor `
  -keyalg RSA -keysize 2048 -validity 10000
```

- **Jika `keystore.properties` ada** → release ditandatangani dengan kunci release-mu.
- **Jika tidak ada** (fresh clone/CI) → release ditandatangani dengan **debug key** agar
  `assembleRelease` tetap menghasilkan artefak yang bisa dipasang untuk pengujian lokal.
  **Jangan** pakai artefak ini untuk distribusi.

> ⚠️ Jangan pernah commit `keystore.properties`, `*.jks`, atau `*.keystore`.

---

## 4. R8 / ProGuard

`release` mengaktifkan `isMinifyEnabled = true` + `isShrinkResources = true`.

- Room / Hilt / WorkManager / Compose menyertakan *consumer rules* otomatis.
- Aturan spesifik aplikasi ada di `app/proguard-rules.pro` (entity Room, `@HiltWorker`,
  entry point manifest: `AnchorApp`, `MainActivity`, `InterventionActionReceiver`,
  `FocusService`).
- Setelah mengubah kode yang direfleksikan, uji `assembleRelease` pada device nyata.

---

## 5. Lokalisasi

- Default: **English** (`app/src/main/res/values/strings.xml`).
- **Bahasa Indonesia**: `app/src/main/res/values-id/strings.xml` (paritas 183 kunci).
- Per-app language (Android 13+) aktif via `generateLocaleConfig` +
  `app/src/main/res/resources.properties` (`unqualifiedResLocale=en-US`).
- Menambah string baru: tambahkan ke **kedua** berkas (EN & ID) agar paritas terjaga.

---

## 6. Checklist Rilis

1. `.\gradlew.bat lintDebug` hijau.
2. `.\gradlew.bat testDebugUnitTest` hijau.
3. `.\gradlew.bat :app:assembleRelease` (atau `bundleRelease`) sukses.
4. Isi `keystore.properties` dengan kunci rilis **sebelum** distribusi.
5. Jalankan item device-only dari `14-hardening-report.md` §7:
   ukur RAM (< 150 MB) & baterai (≤ 2–3%/24 jam), `connectedDebugAndroidTest`,
   regresi alur kritis, uji OEM agresif.
6. Verifikasi `allowBackup=false`, tanpa log data sensitif, payload AI agregat.
