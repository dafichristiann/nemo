# Hardening Report — Sprint 8

> **Status:** Sprint 8 (Hardening) · **Tanggal:** 2026-10-08
> **Terkait:** `10-risk-register.md`, `11-testing-strategy.md` §2-§3, `09-privacy-and-security.md` §7
> **Catatan:** Ringkasan apa yang diverifikasi di kode/CI dan mana yang tetap butuh device nyata.

---

## 1. Ringkasan Status

| Item | Target | Status |
|---|---|---|
| 8-01 Performa RAM < 150 MB | Profil di device low-RAM | Manual (device) — belum diukur di CI |
| 8-02 Baterai ≤ 2–3% / 24 jam | Battery Historian | Manual (device) — belum diukur di CI |
| 8-03 Jalur izin ditolak/OEM batasi | Tak crash | Teruji (unit) |
| 8-04 Migrasi DB semua versi | `MigrationTestHelper` | Teruji (instrumented) |
| 8-05 Audit privasi | allowBackup=false, no logs, payload agregat | Teruji + diperiksa |
| 8-06 Copy & lokalisasi | String resources, nada calm | Diverifikasi lint + `values-id` |
| 8-07 Lint | 0 error | Teruji (lintDebug hijau) |

> **Update 2026-10-08:** wiring schema `androidTest.assets` diperbaiki sehingga seluruh
> 5 test migrasi **benar-benar berjalan** di emulator (`connectedDebugAndroidTest` hijau,
> 22 test termasuk DAO). Ditambah 20 unit test baru (112 total). Lihat §8.

> **Manual/device-only** item (8-01, 8-02, regresi alur kritis di device) tidak dapat
> dieksekusi tanpa perangkat fisik/emulator; dicatat di sini sebagai langkah rilis yang tersisa.

---

## 2. 8-01 / 8-02 — Perf & baterai (panduan, device-only)

Arsitektur sudah menopang target sejak awal (docs 02 §0):
- Semua kerja latar via **WorkManager** (doze-friendly, no polling manual).
- `UsageCollectWorker` interval ~15 m; agregasi chained; pruning 1×/hari; drift 15 m.
- Tidak ada network kecuali AI opt-in (yang default-nya `NoOpAiInsightSource` → nol egress).
- `FocusService` hanya hidup saat sesi aktif + notifikasi IMPORTANCE_LOW.

**Sisa (butuh device):** ukur RAM saat skenario normal (< 150 MB) dan baterai 24 jam
(≤ 2–3%) pada device 3–4 GB, sesuai docs 11 §3.

---

## 3. 8-03 — Jalur degradasi (teruji)

Semua fallback punya test, tidak ada yang crash:
- **Usage Access ditolak** → Wellbeing empty state; drift tidak mengklaim (FR-D6). (`DriftAndCategoryTest`)
- **OEM batasi / data kosong** → agregasi tanpa throw; UI pesan jujur. (`EdgeCasesAndPrivacyTest`)
- **Notification ditolak** → channel turun ke in-app (FR-I3). (`InterventionEngineTest`)
- **Blocking/Accessibility tak tersedia saat Strict** → fallback Gentle + notice. (`FocusUseCasesTest`)
- **AI offline/invalid/throw** → rule-based. (`InsightPipelineTest`)

---

## 4. 8-04 — Migrasi DB (instrumented)

`app/src/androidTest/.../MigrationTest.kt` membuka DB di versi lama (1, 2, 3, 5) dan
menjalankan **seluruh** rantai migrasi (v1→v7) via `MigrationTestHelper`, memvalidasi
skema target + memastikan baris lama **tidak hilang**. `AnchorDatabase.ALL_MIGRATIONS`
adalah satu sumber kebenaran untuk produksi & test.

`InterventionLogDaoTest` memverifikasi query kuota (kecualikan EXPIRED) & cooldown.

> Dijalankan dengan `connectedDebugAndroidTest` di device/emulator; sumbernya dikompilasi
> di build default (`assembleDebugAndroidTest`).

---

## 5. 8-05 — Audit privasi

Checklist docs 09 §7:
- [x] `android:allowBackup="false"` (+ `dataExtractionRules`, `fullBackupContent=false`).
- [x] Tidak ada endpoint jaringan selain AI; AI default off + `NoOpAiInsightSource` (nol egress).
- [x] Test menolak payload berisi data raw — `InsightPayload` **tidak punya field** untuk
      raw session/timestamp/identitas; `passesPrivacyAudit` diuji (`EdgeCasesAndPrivacyTest`).
- [x] `PruningWorker` menghapus raw > retensi; agregat tetap (docs 03 §10).
- [x] "Hapus semua data" membersihkan **semua** tabel user + reset settings
      (`SettingsViewModel.onDeleteAllData`).
- [x] Default `trackingEnabled=false`, `aiEnabled=false`.

---

## 6. 8-06 / 8-07 — Copy & lint

- Semua teks via `strings.xml` (default English, siap `values-id`); placeholder Sprint-0
  yang tak terpakai dihapus.
- `lintDebug` **hijau (0 error)**. Ditemukan & diperbaiki: `MissingPermission` pada
  `InterventionNotifier.notify` → guard `POST_NOTIFICATIONS` eksplisit di call-site.
- `app/lint.xml` menonaktifkan **false positive** `ObsoleteSdkInt` pada `mipmap-anydpi-v26`
  (adaptive icon wajib kualifikasi `-v26`) dan menurunkan hint versi dependensi ke informational.

---

## 7. Sisa untuk Rilis (butuh device nyata)

1. Ukur RAM & baterai (8-01/8-02) di device low-RAM.
2. ~~Jalankan `connectedDebugAndroidTest` (migrasi + DAO) di emulator/device.~~ ✅
   Diverifikasi di emulator Pixel_7 (API 17/preview) — 22 test hijau.
3. Regresi manual 5–8 alur kritis (docs 11 §2) termasuk tiap izin dinonaktifkan.
4. Uji OEM yang agresif mematikan service/WorkManager.

---

## 8. Update — Test & Rilis (2026-10-08)

- **Fix migrasi:** `app/build.gradle.kts` kini mengekspos `schemas/` ke
  `androidTest.assets`, sehingga `MigrationTestHelper` benar-benar menemukan schema
  historis. Sebelumnya 5 test migrasi **gagal** (`FileNotFoundException`); sekarang hijau.
- **Unit test:** 92 → **112** (+20): `DateTimeFormatterTest` (menangkap bug format
  hari/bulan `WED OCT` → `Wed Oct`), `ObserveTasksUseCaseTest` (filter + batas hari),
  `TasksViewModelTest` (state Home).
- **Instrumented test:** 11 → **22** (+11): `TaskDaoTest`, `UsageSessionDaoTest`.
- **Rilis:** `release` memakai R8 + `shrinkResources` (APK ≈ **2.3 MB**), signing dari
  `keystore.properties` (fallback debug bila tak ada), keep-rules di
  `app/proguard-rules.pro`. Panduan: `docs/RELEASE.md`.
- **Lokalisasi:** `values-id/strings.xml` penuh (183 kunci, paritas 100%) +
  per-app language (`generateLocaleConfig`, default `en-US`).
- **Housekeeping:** hapus `SprintPlaceholder.kt` (dead code); mockup retensi
  "90 hari" → "30 hari" agar konsisten PRD.

---

## 9. Update — Verifikasi Build Lokal (2026-10-08)

Paket Sprint 8 (di atas) dikomit; setelah itu quality gate & build rilis
dijalankan ulang secara lokal untuk konfirmasi.

**Quality gate (lokal, hijau):**
- `lintDebug` — BUILD SUCCESSFUL, **0 error**.
- `testDebugUnitTest` (`--rerun-tasks`) — **114 test, 0 failure, 0 error, 0 skip**.

**Build rilis (lokal, hijau):**
- `assembleRelease` — BUILD SUCCESSFUL (R8 + `shrinkResources` + `lintVital` hijau).
- `bundleRelease` — BUILD SUCCESSFUL.

**Ukuran artefak (aktual; bukan target, dapat berubah dengan tuning R8/ASO):**

| Artefak | Ukuran |
|---|---|
| `app-release.apk` | 2,395,812 bytes (≈ 2.28 MB) |
| `app-release.aab` | 4,363,686 bytes (≈ 4.16 MB) |

> ⚠️ **Signing:** tidak ada `keystore.properties` di mesin build, sehingga artefak
> release ini **locally signed with debug key** (`CN=Android Debug`,
> diverifikasi via `apksigner verify --print-certs`). Ini **hanya untuk verifikasi
> lokal** — **bukan** artefak distribusi. Untuk distribusi, isi `keystore.properties`
> dengan kunci rilis asli (lihat `../RELEASE.md` §3) dan build ulang.

**Sisa yang tetap butuh device nyata** (lihat §7): pengukuran RAM/baterai, regresi
manual alur kritis, uji OEM agresif. `connectedDebugAndroidTest` belum dijalankan ulang
pada verifikasi ini (butuh emulator/device).

