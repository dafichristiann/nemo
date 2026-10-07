# Technical Architecture — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Pilihan arsitektur:** Opsi B — Layered + Feature packages (satu Gradle module).
> **Dokumen terkait:** `01-prd.md`, `03-feature-specification.md`, `06-database-design.md`

---

## 1. Prinsip Arsitektur

1. **Local-first** — semua fitur inti bekerja tanpa internet.
2. **Single Gradle module** — hindari over-engineering; siap dipecah nanti.
3. **Feature-first + layered** — batas jelas `core/ domain/ feature/`.
4. **Dependency rule satu arah** — `feature → domain → core` (tidak ada siklus).
5. **Abstraksi di titik risiko** — `AiInsightSource`, `BlockingStrategy`, `UsageAggregator`
   (agar fitur eksperimental bisa diganti tanpa merusak core).
6. **Testable unit** — tiap layer punya peran & bisa diuji terpisah.
7. **Siap Phase 4–6 tanpa rewrite besar.**

---

## 2. Package Structure

```
app/
  src/
    main/
      java/.../anchor/
        AnchorApplication.kt              ← Application, DI container init
        MainActivity.kt                    ← host Compose
        di/                                ← dependency wiring (manual / Hilt opsional)

        core/                              ← infrastruktur & cross-cutting
          database/                        ← Room: entities, DAO, DB, migrations
          datastore/                       ← Preferences DataStore (settings)
          usage/                           ← UsageStatsManager wrapper, UsageAggregator
          notification/                    ← Notification channels & builders
          blocking/                        ← BlockingStrategy + implementasi
          ai/                              ← AiInsightSource + Gemini/RuleBased
          work/                            ← WorkManager workers
          alarm/                           ← AlarmManager scheduler
          service/                         ← Foreground services (Focus)
          designsystem/                    ← Theme, tokens, komponen bersama
          common/                          ← util, dispatcher, result types

        domain/                            ← bisnis murni, tanpa Android framework
          model/                           ← Task, Subtask, Usage*, Insight, ...
          repository/                      ← interface repository
          usecase/                         ← business use cases

        feature/                           ← per fitur, UI + ViewModel
          home/
          tasks/
          focus/
          wellbeing/
          future/
          insights/
          settings/
          intervention/                    ← UI intervensi in-app

      res/
        values/                            ← strings, colors, dimens, themes
        values-b+in/                       ← [future] lokalisasi Bahasa Indonesia
        drawable/
        mipmap/
```

---

## 3. Layer Boundaries & Dependency Rules

```
┌─────────────────────────────────────────────────────┐
│  feature/            (UI + ViewModel)                │
│  ── depends on ──►   domain/, core/designsystem      │
├─────────────────────────────────────────────────────┤
│  domain/             (model, usecase, repo interface)│
│  ── depends on ──►   nothing Android-framework heavy │
├─────────────────────────────────────────────────────┤
│  core/               (database, usage, ai, blocking, │
│                       work, notification, service)   │
│  ── implements ──►   domain/repository interfaces    │
└─────────────────────────────────────────────────────┘
```

**Aturan:**
- `feature` **tidak** mengakses `core/database` langsung → lewat `domain/repository`.
- `domain` **tidak** bergantung pada Compose/Android UI.
- `core` mengimplementasikan repository interface dari `domain`.
- Tidak ada dependency dua arah.

---

## 4. Repository Pattern

Setiap repository = interface di `domain/repository`, implementasi di `core`.

| Repository | Tanggung jawab |
|---|---|
| `TaskRepository` | CRUD task & subtask |
| `UsageRepository` | snapshot & session usage |
| `FocusRepository` | sesi fokus |
| `InsightRepository` | insight tersimpan |
| `InterventionRepository` | log intervensi |
| `CategoryRepository` | kategori & override app |
| `SettingsRepository` | DataStore-backed settings |
| `DependencyRepository` | task dependency (Phase 4) |

Interface mengembalikan `Flow` untuk data reaktif (Room), `suspend` untuk aksi.

---

## 5. Use Cases (domain)

Contoh (bukan daftar lengkap):
```
CreateTaskUseCase
UpdateTaskUseCase
CompleteTaskUseCase
RescheduleTaskUseCase
ComputeDriftSignalUseCase
EvaluateInterventionUseCase
StartFocusSessionUseCase
EndFocusSessionUseCase
ComputeFutureViewUseCase
ComputeDominoImpactUseCase
GenerateDailyInsightUseCase
AggregateUsageUseCase
```

Use case = satu tanggung jawab, murni, mudah dites.

---

## 6. ViewModel & State Management

- Satu `ViewModel` per screen (atau per feature), meng-expose satu `UiState`.
- `UiState` = immutable data class + `sealed` untuk event one-shot.
- Pola: `MutableStateFlow<UiState>` → `StateFlow` → `collectAsStateWithLifecycle()`.
- Aksi user → `viewModel.onAction(...)`.
- Tidak ada business logic di Composable.

```kotlin
data class TaskListUiState(
    val loading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.TODAY,
    val error: String? = null
)
```

---

## 7. Compose Architecture

- Navigation: `Navigation Compose` (single-activity).
- Screen = `Route` composable (ambil ViewModel) → `Content` composable (stateless) → testable.
- State hoisting untuk komponen reusable.
- Previews untuk komponen stateless.
- Stable keys di list; hindari lambda baru tiap recomposition.

---

## 8. Persistence

### Room
- Menyimpan: task, subtask, dependency, usage snapshot, usage session, focus session,
  insight, intervention, category, app category override.
- DAO mengembalikan `Flow` untuk observasi.
- Migrations versioned sejak awal.

### DataStore (Preferences)
- Settings: AI on/off, default focus mode, blocked packages, retention days,
  threshold drift, kategori default, dst.

> Detail skema → `06-database-design.md`.

---

## 9. Background Processing

| Kebutuhan | Mekanisme | Catatan |
|---|---|---|
| Agregasi usage periodik | **WorkManager Periodic** (min 15 menit) | Batching, hemat baterai |
| Refleksi harian (AI/rule) | WorkManager (window harian) | Opsional, sekali/hari |
| Reminder deadline kritis | **AlarmManager** `setExactAndAllowWhileIdle` | Butuh izin di API 31+ |
| Sesi fokus aktif | **Foreground Service** | Hanya selama fokus, notif persisten |
| Deteksi foreground app (opsional) | Accessibility Service (eksperimental) | Bukan fondasi |

**Prinsip:** tidak ada polling terus-menerus; semua lifecycle-aware; gunakan
`Result`/retry WorkManager untuk toleransi kegagalan.

---

## 10. UsageStats Architecture

```
UsageStatsManager (Android)
        │
        ▼
UsageStatsDataSource (core/usage)   ← queryUsageStats / queryEvents
        │
        ▼
UsageAggregator (core/usage)        ← normalize → snapshot/session
        │
        ▼
UsageRepository (domain interface / core impl)
        │
        ▼
Room (UsageSnapshot, UsageSession)
        │
        ▼
DriftDetector (domain/usecase)      ← baseline personal, deviasi
```

- Wrapper menangani izin & keterbatasan OEM.
- Normalisasi batas hari lokal.
- Tidak mengirim raw ke mana pun.

---

## 11. Accessibility Architecture (eksperimental)

```
AccessibilityService (core/service)
      │  onAccessibilityEvent(TYPE_WINDOW_STATE_CHANGED)
      ▼
ForegroundAppTracker → current package
      │
      ├──► Intervesi trigger (opsional)
      └──► BlockingStrategy(AccessibilityStrategy)
```

- Service minimal & hemat.
- Ada **emergency exit** (matikan dari app / notifikasi).
- Jika tidak stabil → tak dipakai; sistem jatuh ke notification-only.
- **Bukan** syarat berjalannya app.

---

## 12. Notification Architecture

- Channel terpisah: `intervention`, `focus`, `reminder`, `daily`.
- Prioritas rendah–sedang (tidak alarm berisik).
- Deduplikasi & rate-limit (3/hari untuk intervensi).
- Fallback in-app bila izin ditolak.

---

## 13. AI Abstraction

```kotlin
interface AiInsightSource {
    val id: String
    suspend fun isAvailable(): Boolean
    suspend fun generate(input: AggregatedInsightInput): InsightResult
}

class GeminiInsightSource(...) : AiInsightSource      // cloud, opt-in, aggregated only
class RuleBasedInsightSource(...) : AiInsightSource   // local fallback
```

- `InsightRepository` memilih source: Gemini bila aktif & tersedia, else rule-based.
- Payload **hanya agregat** (guard di layer ini).
- Cache + batching (maks 1x/hari + on-demand).

> Detail → `07-ai-architecture.md`.

---

## 14. Blocking Abstraction

```kotlin
interface BlockingStrategy {
    val id: String
    suspend fun isAvailable(): Boolean
    suspend fun start(blockedPackages: Set<String>)
    suspend fun stop()
}
// NoneStrategy | NotificationOnlyStrategy | AccessibilityStrategy
```

- `FocusManager` memilih strategy berdasarkan mode & ketersediaan.
- Gagal → fallback ke strategy lebih ringan (graceful).

---

## 15. Dependency Injection

- Mulai **manual DI** (container sederhana di `AnchorApplication`) untuk menghindari
  overhead & jumlah library.
- `[DECISION]` Hilt opsional belakangan bila struktur membesar.
  Rekomendasi: **manual DI dulu**, refactor bila perlu.

---

## 16. Concurrency

- Coroutines + Flow.
- Dispatcher: `Dispatchers.IO` untuk DB/network, `Default` untuk komputasi, `Main` untuk UI.
- Tidak ada blocking call di main thread.
- Semua operasi panjang cancellable.

---

## 17. Error Handling

- `Result`-like wrapper untuk operasi yang bisa gagal.
- Semua error user-facing bersifat manusiawi & actionable.
- Error teknis (izin, OEM, DB) → degrade gracefully, tidak crash.

---

## 18. Architecture Decision Records (ringkas)

| # | Keputusan | Alasan |
|---|---|---|
| AD-1 | Single module, feature-first | Keseimbangan kecepatan & kesiapan |
| AD-2 | `data` di dalam `core/` | Sesuai struktur §25, boundary tetap |
| AD-3 | Manual DI dulu | Hindari overhead library awal |
| AD-4 | Repository interface di domain | Testability & isolation |
| AD-5 | Abstraksi AI/Blocking/Usage | Isolasi risiko eksperimental |
| AD-6 | Model dependency & recurring dormant Phase 1 | Hindari migrasi besar |
| AD-7 | Foreground service hanya saat fokus | Hemat baterai |

---

## 19. Siap untuk Phase 4–6 (tanpa rewrite)

- Dependency: entity & repository sudah ada (dormant) sejak Phase 1.
- AI: interface siap ganti/menambah source.
- Blocking: strategy pattern siap ditambah implementasi baru.
- Sync (Phase 6): repository interface memungkinkan penambahan `RemoteDataSource`
  tanpa mengubah `domain`/`feature`.
