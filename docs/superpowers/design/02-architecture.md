# Architecture — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 (Locked) · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md`, `03-database-schema.md`, `04-intervention-engine.md`
> **Catatan:** Dokumen ini **mengalir dari** PRD. Jika ada konflik, **PRD menang**.
> Tidak ada aturan perilaku baru di sini — hanya keputusan teknis untuk mewujudkannya.

---

## 0. Ringkasan Arsitektur

Anchor adalah aplikasi **Android native, local-first, single-user, tanpa backend**.

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer (Jetpack Compose)            │
│   Tasks · Wellbeing · Focus · Insights · Settings · Onboard │
└───────────────▲───────────────────────────────┬─────────────┘
                │ StateFlow (UI State)          │ Actions
┌───────────────┴───────────────────────────────▼─────────────┐
│                    Domain Layer (Use Cases)                 │
│  TaskManager · DriftDetector · InterventionEngine ·         │
│  FocusController · FutureViewCalculator · InsightGenerator  │
└───────────────▲───────────────────────────────┬─────────────┘
                │ Entities / Repositories       │
┌───────────────┴───────────────────────────────▼─────────────┐
│                      Data Layer (Room)                      │
│  TaskDefinition · TaskInstance · Subtask · UsageSession ·   │
│  DailyUsageSummary · WeeklyUsageSummary · InterventionLog · │
│  FocusSession · Insight · AppCategory · Settings            │
└───────────────▲───────────────────────────────┬─────────────┘
                │                               │
┌───────────────┴──────────┐   ┌────────────────▼─────────────┐
│   Android Platform APIs  │   │  Background (WorkManager)    │
│  UsageStatsManager       │   │  Usage Collect · Aggregation │
│  AccessibilityService*   │   │  Pruning · Drift Check ·     │
│  NotificationManager     │   │  Daily Reflection            │
│  ForegroundService       │   └──────────────────────────────┘
└──────────────────────────┘
   (* Experimental, Phase 3 saja)

              ── optional, opt-in ──►  Gemini API (aggregated data only)
```

**Prinsip arsitektur:**
1. **Local-first** — Room adalah single source of truth. App 100% berfungsi offline.
2. **Privacy by default** — tracking & AI **off** sampai user opt-in.
3. **Graceful degradation** — setiap fitur opsional punya fallback; tidak pernah crash.
4. **Lifecycle-aware** — semua background work via WorkManager, bukan polling manual.
5. **Data source of truth** — angka usage = Android API, bukan AI (PRD §10.2 FR-W7).

---

## 1. Stack Teknis

| Kategori | Pilihan | Alasan |
|---|---|---|
| Bahasa | **Kotlin** | Android modern, coroutines |
| UI | **Jetpack Compose** | Deklaratif, hemat kode, sesuai design (lihat `design/06-design-system.md`) |
| Persistensi | **Room** | Local-first, type-safe, migration terkontrol |
| Background | **WorkManager** | Battery-friendly, doze-proof, periodic |
| Async | **Coroutines + Flow** | Reaktif, lifecycle-aware |
| DI | **Hilt** (atau Koin) | Testable, modular |
| Preferensi | **DataStore (Proto/Preferences)** | Pengganti SharedPreferences |
| AI (opsional) | **Gemini API** | Hanya agregat, opt-in |
| Min SDK | **26 (Android 8.0)** | `UsageStatsManager` + WorkManager stabil |
| Target SDK | Terbaru stabil | Best practice Play (meski personal) |

> **Catatan real-device:** Karena single-user, target utama adalah device pribadi.
> Tetap uji di minimal 2 device (1 low-RAM 3–4 GB) untuk benchmark PRD §12.

---

## 2. Struktur Modul (Package Layout)

Struktur disarankan: **feature-based** dengan core layer bersama.

```
com.anchor.app
├── core
│   ├── data/            # Room: entities, DAO, database, migrations
│   ├── domain/          # Entities (pure), Repository interfaces, Result types
│   ├── common/          # TimeProvider, DispatcherProvider, constants
│   ├── design/          # Theme, tokens, komponen Compose reusable
│   └── di/              # Hilt modules
├── feature
│   ├── onboarding/
│   ├── tasks/           # Smart Todo (Phase 1)
│   ├── wellbeing/       # Digital Wellbeing (Phase 2)
│   ├── focus/           # Focus Setup + Active (Phase 3)
│   ├── intervention/    # In-app intervention & fullscreen (Phase 3)
│   ├── futureview/      # Future View + Domino (Phase 4)
│   ├── insights/        # Daily Reflection + AI (Phase 5)
│   └── settings/        # Privacy, permissions, data (Phase 1+)
├── platform
│   ├── usagestats/      # Wrapper UsageStatsManager
│   ├── accessibility/   # Experimental (Phase 3)
│   ├── notification/    # Notification channel + builder
│   └── service/         # Foreground focus service
└── worker
    ├── UsageCollectWorker
    ├── AggregationWorker
    ├── PruningWorker
    ├── DriftCheckWorker
    └── DailyReflectionWorker
```

**Aturan dependensi:** `feature` → `domain` ← `data`. `feature` tidak boleh mengakses
`data` secara langsung; selalu lewat repository interface di `domain`.

---

## 3. Layer Detail

### 3.1 Data Layer (Room)

Detail lengkap di `03-database-schema.md`. Ringkas:

| Entitas | Peran | Phase |
|---|---|---|
| `TaskDefinition` | Template + recurrence rule | 1 |
| `TaskInstance` | Task konkret harian (dueDate, status) | 1 |
| `Subtask` | Langkah kecil dalam task | 1 |
| `TaskDependency` | Relasi BLOCKS/RELATED (domino) | 4 |
| `UsageSession` | Satu sesi app kontinu (raw, di-prune) | 2 |
| `DailyUsageSummary` | Agregat harian | 2 |
| `WeeklyUsageSummary` | Agregat mingguan | 2 |
| `AppCategory` | Kategori app + override | 2 |
| `FocusSession` | Riwayat sesi fokus | 3 |
| `InterventionLog` | Audit intervensi + status | 3 |
| `Insight` | Hasil AI / rule-based | 5 |
| `RescheduleEvent` | Riwayat reschedule (insight) | 1 |

**Repository interface** (di domain) contoh: `TaskRepository`, `UsageRepository`,
`FocusRepository`, `InterventionRepository`, `InsightRepository`, `SettingsRepository`.

### 3.2 Domain Layer (Use Cases)

Use case adalah kelas kecil dengan satu tanggung jawab. Contoh:

- `CreateTaskUseCase`, `CompleteTaskUseCase`, `RescheduleTaskUseCase`, `ObserveTasksUseCase`
- `CollectUsageUseCase`, `AggregateUsageUseCase`, `ComputeBaselineUseCase`
- `DetectDriftUseCase` → output `DriftSignal` (lihat `04-intervention-engine.md`)
- `EvaluateInterventionUseCase` → output `InterventionDecision`
- `StartFocusUseCase`, `PauseFocusUseCase`, `FinishFocusUseCase`
- `ComputeFutureViewUseCase`, `ComputeDominoEffectUseCase`
- `GenerateInsightUseCase` (AI + fallback), `GenerateDailyReflectionUseCase`

### 3.3 UI Layer (Compose)

- **Unidirectional Data Flow:** Screen memiliki `ViewModel` → memancarkan `UiState`
  (sealed class / data class) → UI merender. Aksi user = event, bukan mutasi langsung.
- **Navigation:** Navigation-Compose. Top-level tabs: Tasks · Wellbeing · Focus ·
  Insights. Detail & modal sebagai route terpisah.
- **Tema:** monokrom + Inter + Material Symbols Rounded (lihat `design/06-design-system.md`).

---

## 4. Alur Data Utama

### 4.1 Mengumpulkan Usage (Phase 2)

```
WorkManager (Periodic, ~15 menit saat layar aktif)
    └─► UsageCollectWorker
          ├─ cek izin PACKAGE_USAGE_STATS (skip jika belum)
          ├─ query UsageStatsManager interval sejak koleksi terakhir
          ├─ map ke UsageSession (app, start, end, durasi)
          └─ simpan ke Room
```

> `UsageStatsManager` **tidak** punya event real-time (PRD §10.3). Jadi Phase 2 =
> observasi periodik. Phase 3 baru boleh pakai `AccessibilityService` (eksperimental)
> untuk sinyal instan.

### 4.2 Agregasi & Pruning

```
AggregationWorker (harian / saat data baru)
    ├─ UsageSession ──► DailyUsageSummary (screen time, top apps, kategori, distraksi)
    └─ DailyUsageSummary ──► WeeklyUsageSummary (7 hari, baseline)

PruningWorker (periodik)
    └─ hapus UsageSession usia > 30 hari (rentang kebijakan 14–30 hari, PRD §10.2)
```

### 4.3 Deteksi Drift → Intervensi (Phase 2–3)

```
DriftCheckWorker ─► DetectDriftUseCase ─► DriftSignal?
        │
        └─► InterventionEngine (lihat 04-intervention-engine.md)
                ├─ cek kuota harian (< 3) & cooldown (2 jam / konteks)
                ├─ pilih level (1–4) & channel
                └─ tayangkan + catat InterventionLog
```

### 4.4 Focus Session (Phase 3)

```
StartFocusUseCase ─► mulai ForegroundService + notification persisten
                   └─► catat FocusSession
BlockingStrategy (Gentle default) ─► bila gagal → fallback Gentle
FinishFocusUseCase ─► hentikan service, simpan durasi, tawarkan update progress task
```

---

## 5. Background Work (WorkManager)

| Worker | Trigger | Constraint | Idempotent | Retry |
|---|---|---|---|---|
| `UsageCollectWorker` | Periodic ~15m | Izin ada, layar aktif | Ya | Backoff |
| `AggregationWorker` | Setelah koleksi / harian | – | Ya | Backoff |
| `PruningWorker` | Harian | Device idle (opsional) | Ya | Backoff |
| `DriftCheckWorker` | Periodic | Izin ada + data cukup | Ya | Backoff |
| `DailyReflectionWorker` | Sekali sore/malam | – | Ya (1x/hari) | Backoff |

**Aturan:**
- Selalu `ExistingPeriodicWorkPolicy.KEEP` / `UPDATE` yang benar — jangan duplikat work.
- Jangan polling manual; andalkan WorkManager.
- Hormati Doze: jangan `setExpedited` kecuali benar-benar perlu.
- Setiap worker menangani kasus "izin hilang / data kosong" tanpa crash.

---

## 6. Permission & Degradasi

Mengacu PRD §14:

| Permission | Kapan diminta | Jika ditolak → perilaku |
|---|---|---|
| `PACKAGE_USAGE_STATS` | Saat user buka Wellbeing | App tetap jalan sebagai Smart Todo |
| `POST_NOTIFICATIONS` | Sebelum intervensi pertama | Fallback in-app saja |
| Accessibility Service | Saat user pilih Strict/Custom | Fallback ke Gentle |
| Foreground Service | Hanya saat Focus aktif | Focus terbatas (tanpa timer persisten) |
| Alarm permissions | Jika reminder kritis dipakai | Reminder fleksibel (WorkManager) |

**First launch tidak memaksa apa pun.** Smart Todo jalan tanpa izin apa pun.

**Pola degradasi:** setiap modul opsional mendefinisikan `Availability` (Available /
Degraded / Unavailable) dan UI menyesuaikan dengan pesan jujur, bukan crash.

---

## 7. Privacy & Keamanan

```
Raw Usage Data (device-only)
      ▼ Local Aggregation (Daily/Weekly Summary)
      ▼ [Pruned 14–30 hari] (raw dihapus)
      ▼ [Aggregated, structured] ──► AI (opsional, opt-in)
```

- Raw usage, sesi, snapshot → **hanya di device**.
- AI menerima **hanya agregat terstruktur**, dan **hanya jika diaktifkan**.
- Ada endpoint audit: sebelum kirim, `assert(payload is Aggregated)` — tolak raw
  (PRD §10.8 acceptance: "Given payload berisi raw usage → Anchor MENOLAK").
- Tombol "hapus semua data" menghapus seluruh tabel lokal terkait.
- Tidak ada analytics pihak ketiga, tidak ada crash reporter yang mengirim data user.

---

## 8. Testing & Kualitas

| Level | Fokus | Tool |
|---|---|---|
| Unit | Use cases, drift math, future view calc, aggregation | JUnit + Turbine |
| DAO | Query & migration | Room testing |
| Worker | Constraint & idempotency | WorkManager Test |
| UI | State rendering | Compose UI Test |
| Instrumented | Alur end-to-end di device | Espresso/UIAutomator (sparing) |

**Target non-fungsional (PRD §12):**
- RAM aktif < **150 MB**
- Baterai background ≤ **2–3% / 24 jam**
- Offline-first 100%
- Tidak crash saat izin ditolak / OEM membatasi / migrasi DB gagal

---

## 9. Risiko Arsitektur & Mitigasi

| Risiko | Level | Mitigasi arsitektural |
|---|---|---|
| Doze / Standby membatasi background | Tinggi | WorkManager + hindari polling |
| Granularitas UsageStats terbatas (OEM) | Tinggi | `Availability` state + degradasi bertahap; Spike A |
| Accessibility rapuh & boros | Tinggi | Eksperimental, Feature-flagged, fallback notifikasi |
| Exact alarm restrictions | Sedang | WorkManager / alarm fleksibel |
| OEM kill background | Sedang–Tinggi | Panduan battery optimization, uji device nyata |
| AI cost/rate-limit/offline | Sedang | Cache + batch + fallback rule-based |
| Compose performance | Sedang | Hindari animasi berat, stable keys, `key()` |

Detail penuh: PRD §15.

---

## 10. Referensi Silang

- Perilaku & aturan → `01-prd.md`
- Skema data → `03-database-schema.md`
- Deteksi drift & intervensi → `04-intervention-engine.md`
- Ruang lingkup fase → `05-mvp-scope.md`
- Visual → `design/06-design-system.md`, `design/07-screens.md`

---

> **CATATAN AKHIR:** Arsitektur ini diturunkan dari PRD v0.2 yang sudah **Locked**.
> Perubahan arsitektur yang mengubah perilaku **wajib** memperbarui PRD terlebih dahulu.
