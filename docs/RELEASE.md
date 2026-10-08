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

## 5b. AI Insight (Gemini — opt-in)

AI Personal Insight memakai Gemini REST (lihat `superpowers/design/16-ai-insight-gemini.md`).
Konfigurasi key di `local.properties` (git-ignored, machine-specific):

```properties
GEMINI_API_KEY=<key dari Google AI Studio>
```

- Bila key **kosong** → AI no-op, insight memakai rule-based lokal, **tanpa egress**.
- Bila key **ada** & toggle AI di Settings **ON** → `generateContent` mengirim
  **hanya agregat**; gagal/offline → fallback rule-based otomatis.
- Default toggle AI = **OFF** (privacy).
- ⚠️ **Jangan commit** `local.properties` / key. Rotasi key bila pernah terekspos.

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

---

## 7. Status MVP Closure (2026-10-08)

MVP Phase 1–5 **ditutup**. Ringkasan verifikasi terakhir:

| Item | Hasil |
|---|---|
| `lintDebug` | PASS (0 error) |
| `testDebugUnitTest` | PASS — 114 test, 0 failure, 0 error, 0 skip |
| `assembleRelease` | PASS — APK 2,395,812 bytes (≈ 2.28 MB) |
| `bundleRelease` | PASS — AAB 4,363,690 bytes (≈ 4.16 MB) |
| Signing | **locally signed with debug key** (`CN=Android Debug`) — verifikasi lokal saja, **bukan** distribusi |
| Physical device | PASS — TECNO CN7c (Android 16 / API 36), tanpa crash/ANR, privasi terverifikasi |
| Remote push | NOT DONE (sesuai kebijakan) |

> Ukuran artefak = hasil pengukuran, **bukan** requirement.

**Known Follow-ups / Post-MVP Hardening** (non-blocking — detail di
`superpowers/design/14-hardening-report.md` §11):

- **F1 RAM** — PSS 152–167 MB vs target < 150 MB; butuh pengukuran multi-skenario
  sebelum optimasi (dominasi Graphics/Code shared, bukan leak).
- **F2 Recurring Today** — recurring tanpa deadline tidak muncul di *Today*
  (overdue via banner = by design).
- **F3 Intervention verification** — engine sehat, belum pernah fire (butuh task
  HIGH/URGENT + baseline); perlu controlled verification.
- **F4 Notification verification** — `POST_NOTIFICATIONS` ditolak → jalur in-app
  terbukti; channel notifikasi belum dites di device.

Untuk distribusi: isi `keystore.properties` (`§3`) lalu build ulang; itu akan
menghasilkan artefak release yang benar-benar ter-sign dan tidak lagi memakai debug key.
