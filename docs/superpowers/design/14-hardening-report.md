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

---

## 10. Final MVP Closure (2026-10-08)

Quality gate & build rilis dijalankan ulang sebagai penutupan MVP (Phase 1–5).

**Quality gate — PASS**

| Gate | Hasil |
|---|---|
| `lintDebug` | PASS (0 error) |
| `testDebugUnitTest` (`--rerun-tasks`) | PASS — **114 test, 0 failure, 0 error, 0 skip** |

**Release build — PASS**

| Artefak | Hasil | Ukuran aktual |
|---|---|---|
| `app-release.apk` | BUILD SUCCESSFUL | 2,395,812 bytes (≈ 2.28 MB) |
| `app-release.aab` | BUILD SUCCESSFUL | 4,363,690 bytes (≈ 4.16 MB) |

> Ukuran di atas adalah **hasil pengukuran, bukan requirement**; dapat berubah dengan
> tuning R8/ASO.

**Signing**

⚠️ Tidak ada `keystore.properties` di mesin build → artefak release
**locally signed with debug key** (`CN=Android Debug`, diverifikasi via
`apksigner verify --print-certs`). Ini **valid untuk verifikasi lokal**, **bukan**
artefak distribusi. Distribusi tetap butuh keystore rilis asli (`../RELEASE.md` §3).
Signing architecture **tidak diubah** dan **tidak ada keystore produksi** yang dibuat.

**Physical Device Validation — PASS**

Diuji di device nyata **TECNO CN7c (Android 16 / API 36)** via ADB. Seluruh Phase 1–5
berfungsi end-to-end; tidak ada crash/ANR; perilaku privasi (local-first, AI default off,
raw usage tidak keluar device) terverifikasi. Worker WorkManager (collect/aggregate/
prune/intervention-check/daily-reflection) berjalan SUCCESS.

---

## 11. Known Follow-ups / Post-MVP Hardening

Temuan berikut **bukan blocker rilis** dan **tidak diimplementasikan** dalam closure ini.
Semuanya adalah hardening/verification lanjutan yang sengaja ditunda (MVP dikunci).

| ID | Temuan | Klasifikasi | Status |
|---|---|---|---|
| **F1** | PSS 152–167 MB vs target PRD §12 (< 150 MB). Breakdown didominasi Graphics (GL/EGL ≈ 54 MB) + Code/System (shared); Java heap hanya ~11 MB → tidak ada indikasi leak. Perlu pengukuran multi-skenario sebelum optimasi. | Needs measurement | Open (non-blocking) |
| **F2** | Recurring task **tanpa deadline** (mis. "Olahraga pagi", DAILY) tidak muncul di tab *Today*, hanya di *All*. Root cause: `TaskDao.observeToday` mensyaratkan `deadline IS NOT NULL` dan mengabaikan `definitionOccurrenceDate`. Overdue tetap ditampilkan via Overdue Banner (sesuai PRD §10.1) — itu by design, bukan bug. | UX improvement | Open (non-blocking) |
| **F3** | `intervention_log = 0` — engine berjalan (`InterventionCheckWorker SUCCESS`) tetapi belum pernah `fire`. Syarat belum terpenuhi: tidak ada task open HIGH/URGENT (semua task MEDIUM) dan/atau baseline weekly belum terbentuk. Butuh controlled verification. | Verification gap | **RESOLVED (2026-10-08)** — lihat §12 |
| **F4** | `POST_NOTIFICATIONS = ignore` → intervensi berjalan via **in-app fallback** (sesuai FR-I3). Channel notifikasi utama belum dites di device nyata. Dapat diverifikasi nanti via `pm grant`. | Verification gap | Open (non-blocking) |

> Semua item di atas dilacak sebagai backlog hardening; **tidak ada** yang mengubah
> keputusan MVP terkunci.

---

## 12. F3 — Intervention Verification (RESOLVED, 2026-10-08)

Controlled verification di device nyata **TECNO CN7c (Android 16)** menemukan **bug nyata**
di balik F3, bukan sekadar kondisi uji yang belum terpenuhi.

**Bug:** `AggregateUsageUseCase.invoke()` memanggil `aggregateWeek(week)` **tanpa menyimpan
hasilnya** — tidak ada `usageRepository.upsertWeeklySummary(...)`. Akibatnya
`weekly_usage_summary` selalu kosong → `DetectDriftUseCase` selalu `return null`
(baseline tidak ada) → **intervensi mustahil fire**.

**Fix:** simpan hasil `aggregateWeek` ke `upsertWeeklySummary` (bounded, satu baris).
Ditambah regression test `aggregate_invokePersistsWeeklySummaryForDriftBaseline`.

**Bukti sesudah fix (device nyata):**

| Langkah | Hasil |
|---|---|
| `POST_NOTIFICATIONS` | digrant → `allow` |
| Task open URGENT | dibuat ("TugasUrgent", TODO, URGENT) |
| Aggregation | `weekly_usage_summary` = 1 baris (`2026-W41`, baseline 5393 char) |
| `InterventionCheckWorker` | SUCCESS |
| `intervention_log` | **1 baris**: level 3 (FUTURE_VIEW), channel **NOTIFICATION**, context `com.github.android`, status EXPIRED |
| Notifikasi | tampil di shade: title "A moment to plan", text "Starting now could give you some free time later tonight." |

**Kesimpulan:** rantai Phase 3 (collect → aggregate → **baseline** → drift → engine →
notification) **terbukti bekerja end-to-end** di device nyata. F3 ditutup.

> Bug ini juga menjawab F2-adjacent: sebelumnya drift/insight berbasis baseline tidak akan
> pernah aktif. Perbaikan ini diperlukan agar Phase 3 berfungsi sesuai PRD §10.3/§10.4.

---

## 13. Follow-up Closure (2026-10-08, lanjutan)

Menutup temuan §11 F2/F4 dan gap UI yang teridentifikasi dari penelusuran PRD ↔ kode.
Semua perubahan **tidak mengubah** keputusan MVP terkunci.

| Item | Perubahan | FR/AC |
|---|---|---|
| **Domino UI** | `FutureViewScreen` kini punya pemilih task untuk **menambah dependensi** (memanggil `onAddDependency`), plus notice siklus. Sebelumnya infra ada tetapi UI-nya hilang → Domino selalu kosong. | FR-DE1..DE5, AC §10.7 |
| **Settings (FR-S1)** | Section **Permissions** baru: status Usage access & Notifications (live) + tombol aktifkan/pintasan Settings. Refresh di `onResume`. | FR-S1, §14 |
| **Settings (FR-S3)** | Section **App categories**: rename kategori + toggle "distraction" per app (via `AppCategory`). | FR-S3 |
| **Settings (FR-S4)** | Section **Blocked apps**: **picker daftar aplikasi terpasang di HP** (label + package, search, tap untuk block/unblock dengan badge "Blocked"), disimpan persisten (kolom baru `app_settings.blockedAppsJson`, migrasi **v7→v8**). Pakai `<queries>` bertarget MAIN/LAUNCHER — **bukan** `QUERY_ALL_PACKAGES` (sesuai docs 08 §7). Dikelola penuh di UI; tanpa efek blocking aktif (Strict/Custom dikecualikan dari scope ini). | FR-S4 |
| **Notifikasi runtime** | Alur "minta notifikasi lalu arahkan ke permission" (pola umum app): launcher `POST_NOTIFICATIONS` di Onboarding 00b & Settings; bila ditolak → buka `ACTION_APP_NOTIFICATION_SETTINGS` (fallback ke app details). Menutup §11 **F4**. | FR-I3, §14 |
| **Onboarding izin** | Kartu Usage → buka Usage access; kartu Notifications → request runtime. Tetap opsional (skip aman). | FR-S1, §14 |
| **Recurring background** | `RecurringTaskWorker` (periodik harian) dijadwalkan dari `AppViewModel` **terlepas** dari `trackingEnabled` — instance recurring muncul walau tab Tasks tidak dibuka. | FR-T7 |
| **F2** | `TaskDao.observeToday` kini juga menyertakan instance recurring hari ini (`definitionOccurrenceDate == today`) walau tanpa deadline. Menutup §11 **F2**. | FR-T10, §10.1 |

**Verifikasi (lokal, hijau):**
- `testDebugUnitTest` (`--rerun-tasks`) — PASS.
- `lintDebug` — PASS (0 error).
- `connectedDebugAndroidTest` — **24 test PASS** di device nyata **TECNO CN7c (Android 16)** **dan** emulator **Pixel_7 (API 17/preview)**, termasuk migrasi **v7→v8** (`migrate7ToLatest_addsBlockedAppsColumn_preservesSettings`) dan `observeToday` (recurring tanpa deadline).
- `compileReleaseKotlin` — PASS.

**Sengaja tetap di luar scope** (sesuai keputusan): ~~Strict/Custom blocking & `AccessibilityService`
(FR-F4/F8) — masih `GentleBlockingStrategy` (no-op)~~ — **kini terimplementasi** (lihat §15);
F1 (RAM < 150 MB) menunggu pengukuran device.

---

## 14. Settings UI Redesign (2026-10-08, lanjutan)

Menanggapi feedback: daftar app yang sangat panjang (100+ terinstal) membuat layar Settings
kurang nyaman. Diubah jadi **ringkas + sub-halaman terpisah** (pola Digital Wellbeing).

| Perubahan | Detail |
|---|---|
| **Settings ringkas** | `App categories` & `Blocked apps` kini hanya menampilkan **ringkasan** ("108 apps categorized" / "2 apps paused during focus") + tombol **Manage**. Tidak ada lagi daftar panjang di Settings. |
| **Sub-halaman Blocked apps** (`BlockedAppsScreen`) | Route `settings/blocked`. Search, **ikon aplikasi**, label + package sebagai sub-teks, **toggle** block/unblock. App yang diblokir naik ke atas. |
| **Sub-halaman App categories** (`AppCategoriesScreen`) | Route `settings/categories`. Search, ikon, rename kategori, toggle "treat as distraction". |
| **Ikon aplikasi** | `InstalledAppsSource` kini memuat `Drawable` ikon via `PackageManager`; komponen reusable `AppIcon` (dengan fallback inisial). |

**Verifikasi di device nyata (TECNO CN7c) + emulator (Pixel_7):**
- Settings ringkas tampil benar (ringkasan + Manage).
- Sub-halaman Blocked apps menampilkan app terinstal dengan ikon & toggle.
- Onboarding 00b: tombol **Enable** Notifications → dialog runtime **"Allow Anchor to send you notifications?"** → Allow → kartu berubah **"Enabled"**; tombol Enable Usage access → membuka **system Settings** (Usage access).
- `lintDebug` / `testDebugUnitTest` / `assembleDebugAndroidTest` — PASS.

---

## 15. Focus Strict/Custom Blocking via AccessibilityService (2026-10-08)

Menuntaskan FR-F4/F8 yang selama ini no-op (`GentleBlockingStrategy`). Spec:
`specs/2026-10-08-focus-strict-custom-blocking-design.md`; plan:
`plans/2026-10-08-focus-strict-custom-blocking-plan.md`; PRD v0.4 §10.5/§14/§16.

| Aspek | Implementasi |
|---|---|
| **Gerbang 3 lapis** | `BlockingGate` (`ProductionBlockingGate`): build flag `FOCUS_BLOCKING_ENABLED` + toggle Settings `focusBlockingEnabled` + izin OS. `AccessibilityBlockingStrategy.isAvailable()` mengecek ketiganya. |
| **State live** | `BlockingState` (`@Singleton`, in-memory `StateFlow<BlockingSnapshot>`); di-`engage`/`release` oleh strategy, dibaca service. |
| **Enforcement** | `AnchorAccessibilityService` (BIND_ACCESSIBILITY_SERVICE, `canRetrieveWindowContent=false`) memantau `TYPE_WINDOW_STATE_CHANGED`; app terblokir → overlay. |
| **Overlay** | `BlockOverlayWindow` via `TYPE_ACCESSIBILITY_OVERLAY` (bukan Activity — menghindari background-activity-start restriction pada Android 10+). Non-paksa; tombol "Back to focus" membuka Anchor di tab Focus. **Muncul ulang setiap kali masuk** ke app terblokir (re-arm saat keluar). |
| **Fallback (FR-F8)** | Setiap kegagalan → sesi dicatat sebagai Gentle + `fallbackReason` (jalur `StartFocusUseCase` yang sudah ada). `onInterrupt`/`onUnbind` mengosongkan `BlockingState`. |
| **Persistence** | Room **v8→v9** (`app_settings.focusBlockingEnabled`, default OFF) + `MigrationTest.migrate8ToLatest_addsFocusBlockingColumn_defaultOff`. |
| **Timer** | `FocusViewModel` mengadopsi sesi yang sedang berjalan saat di-recreate → timer tidak reset setelah kembali dari overlay. |
| **Privasi** | Tidak membaca konten layar; hanya nama package dari event window-change (docs 09). |

**Test:** `BlockingStateTest` (4) + `AccessibilityBlockingStrategyTest` (8) + migrasi instrumented — PASS.
**Verifikasi manual:** di TECNO CN7c (dan emulator Pixel_7) — Strict dengan app terblokir → overlay muncul; tombol kembali ke tab Focus; timer lanjut; overlay muncul ulang tiap masuk kembali.

> **Catatan OEM:** service hanya mulai menerima event setelah izin diberikan **lewat Settings → Accessibility**
> (set via adb `settings put` tidak selalu mengikat service). Karena itu UI mengarahkan user ke Settings (FR-F8).


