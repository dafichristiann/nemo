# Database Schema — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 (Locked) · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` (§10 model), `02-architecture.md`
> **Catatan:** Skema ini mewujudkan model data di PRD. Tipe & relasi boleh diperkaya
> secara teknis, tetapi **tidak boleh mengubah** field wajib yang sudah dikunci PRD.

---

## 0. Prinsip Skema

1. **Local-first** — Room adalah single source of truth. Tidak ada remote DB.
2. **Forward-compatible** — desain tabel tahan terhadap penambahan kolom (lihat §9).
3. **Task Definition ≠ Task Instance** — recurring task tanpa merusak riwayat (PRD §10.1).
4. **Raw data di-prune** — `UsageSession` mentah dibatasi usia; agregat disimpan permanen.
5. **Waktu konsisten** — simpan sebagai `Long` epoch millis (UTC). Batas "hari" dihitung
   dari timezone lokal device.
6. **Enum disimpan sebagai `String` + konverter**, agar migrasi aman.

---

## 1. Peta Tabel

| Tabel | Phase | Kategori | Retention |
|---|---|---|---|
| `task_definition` | 1 | Core | Sampai dihapus user |
| `task_instance` | 1 | Core | Sampai dihapus user |
| `subtask` | 1 | Core | Mengikuti task_instance |
| `reschedule_event` | 1 | Riwayat | Sampai dihapus user |
| `task_dependency` | 4 | Core | Mengikuti task |
| `usage_session` | 2 | Raw | **Di-prune (14–30 hari)** |
| `daily_usage_summary` | 2 | Agregat | Permanen |
| `weekly_usage_summary` | 2 | Agregat | Permanen |
| `app_category` | 2 | Konfig | Sampai dihapus user |
| `focus_session` | 3 | Riwayat | Permanen |
| `intervention_log` | 3 | Riwayat | Permanen (kecil) |
| `insight` | 5 | Riwayat | Permanen / dibatasi |
| `app_settings` | 1+ | Konfig | Single row |

---

## 2. Modul: Task (Phase 1)

### 2.1 `task_definition`

Template + aturan keberulangan. Task non-recurring pun boleh memiliki satu definition
(1:1) agar model seragam.

| Kolom | Tipe (Kotlin/Room) | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | `String` (UUID) | no | – | PK |
| `title` | `String` | no | – | Judul dasar |
| `description` | `String?` | yes | null | Detail opsional |
| `defaultPriority` | `TaskPriority` | no | `MEDIUM` | Enum |
| `defaultDurationMin` | `Int?` | yes | null | Estimasi default (menit) |
| `category` | `String?` | yes | null | Kategori user |
| `recurrenceRule` | `RecurrenceRule` | no | `NONE` | `NONE / DAILY / WEEKLY` |
| `recurrenceMetaJson` | `String?` | yes | null | Detail (mis. hari dalam seminggu) |
| `createdAt` | `Long` | no | now | Epoch millis |
| `archivedAt` | `Long?` | yes | null | Diarsipkan |

```kotlin
enum class RecurrenceRule { NONE, DAILY, WEEKLY }
```

> **Catatan recurring:** Definition menerbitkan Instance baru saat jatuh tempo.
> Menyimpan riwayat tiap instance **tanpa menimpa** template (PRD §10.1).

### 2.2 `task_instance`

Subjek konkret yang dikerjakan user. Ini adalah tabel utama untuk list & status.

| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | `String` (UUID) | no | – | PK |
| `definitionId` | `String?` | yes | null | FK → `task_definition.id` (jika recurring) |
| `title` | `String` | no | – | Judul task |
| `description` | `String?` | yes | null | Detail opsional |
| `deadline` | `Long?` | yes | null | Epoch millis |
| `priority` | `TaskPriority` | no | `MEDIUM` | `LOW/MEDIUM/HIGH/URGENT` |
| `estimatedDurationMin` | `Int?` | yes | null | Menit |
| `category` | `String?` | yes | null | Kategori |
| `status` | `TaskStatus` | no | `TODO` | `TODO/IN_PROGRESS/DONE/ARCHIVED` |
| `createdAt` | `Long` | no | now | |
| `completedAt` | `Long?` | yes | null | Diisi saat DONE |
| `progressPercent` | `Int` | no | 0 | Dihitung dari subtask / manual |
| `parentTaskId` | `String?` | yes | null | Hierarki/dependency dasar |
| `definitionOccurrenceDate` | `Long?` | yes | null | Tanggal instance recurring |

```kotlin
enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }
enum class TaskStatus { TODO, IN_PROGRESS, DONE, ARCHIVED }
```

**Indeks:** `index(definitionId)`, `index(status)`, `index(deadline)`.

### 2.3 `subtask`

| Kolom | Tipe | Null | Default |
|---|---|---|---|
| `id` | `String` (UUID) | no | – |
| `taskId` | `String` | no | FK → `task_instance.id` (ON DELETE CASCADE) |
| `title` | `String` | no | – |
| `isDone` | `Boolean` | no | false |
| `orderIndex` | `Int` | no | 0 |

### 2.4 `reschedule_event`

Untuk insight pola penundaan (PRD FR-T12).

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `id` | `String` (UUID) | no | PK |
| `taskId` | `String` | no | FK → task_instance |
| `fromDeadline` | `Long?` | yes | Deadline lama |
| `toDeadline` | `Long?` | yes | Deadline baru |
| `reason` | `String?` | yes | Opsional |
| `createdAt` | `Long` | no | Kapan di-reschedule |

### 2.5 `task_dependency` (Phase 4)

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `id` | `String` (UUID) | no | PK |
| `fromTaskId` | `String` | no | FK → task_instance (yang menunda) |
| `toTaskId` | `String` | no | FK → task_instance (yang terdampak) |
| `type` | `DependencyType` | no | `BLOCKS / RELATED` |

```kotlin
enum class DependencyType { BLOCKS, RELATED }
```

> **Anti-siklus:** sebelum menyimpan, jalankan deteksi siklus (mis. DFS `toTaskId → …`).
> Jika menemukan `fromTaskId` kembali → tolak & jelaskan (PRD §10.7).

---

## 3. Modul: Usage / Wellbeing (Phase 2)

### 3.1 `usage_session` (RAW — di-prune)

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `id` | `String` (UUID) | no | PK |
| `packageName` | `String` | no | App |
| `startTime` | `Long` | no | Epoch millis |
| `endTime` | `Long` | no | Epoch millis |
| `durationMs` | `Long` | no | endTime − startTime |
| `collectedAt` | `Long` | no | Kapan dikoleksi |

**Retention:** dihapus otomatis saat `startTime` usia > **30 hari** (kebijakan 14–30 hari).
**Indeks:** `index(startTime)`, `index(packageName)`.

### 3.2 `daily_usage_summary` (AGREGAT)

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `dateEpochDay` | `Long` | no | PK (hari lokal, mis. LocalDate.toEpochDay) |
| `totalScreenTimeMs` | `Long` | no | Total screen time |
| `distractionRatio` | `Float?` | yes | Rasio waktu di app distraksi |
| `topAppsJson` | `String` | no | JSON: `[{pkg, ms, sessions}]` |
| `categoryBreakdownJson` | `String` | no | JSON: `{kategori: ms}` |
| `sessionCount` | `Int` | no | Jumlah sesi |
| `computedAt` | `Long` | no | Kapan dihitung |

### 3.3 `weekly_usage_summary` (AGREGAT)

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `isoWeek` | `String` | no | PK (mis. `2026-W41`) |
| `totalScreenTimeMs` | `Long` | no | Total 7 hari |
| `dailyAvgMs` | `Long` | no | Rata-rata harian |
| `baselineJson` | `String` | no | Baseline per app (avg sesi, deviasi) |
| `trendJson` | `String` | no | Data tren untuk sparkline/bar |
| `computedAt` | `Long` | no | |

> **Baseline personal** (PRD §10.3) dihitung dari `daily_usage_summary` / `usage_session`,
> disimpan di `baselineJson` per app: `{pkg: {avgSessionMs, stdDev, sampleDays}}`.
> Jika `sampleDays` kurang → **jangan** klaim drift (FR-D6).

### 3.4 `app_category`

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `packageName` | `String` | no | PK |
| `category` | `String` | no | mis. `PRODUCTIVE/SOCIAL/ENTERTAINMENT/OTHER` |
| `isDistraction` | `Boolean` | no | Default false |
| `overrideSource` | `String` | no | `AUTO / USER` (user bisa override, PRD FR-S3) |

---

## 4. Modul: Focus (Phase 3)

### 4.1 `focus_session`

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `id` | `String` (UUID) | no | PK |
| `taskId` | `String?` | yes | FK → task_instance |
| `mode` | `FocusMode` | no | `GENTLE / STRICT / CUSTOM` |
| `plannedDurationMin` | `Int?` | yes | Durasi direncanakan |
| `actualDurationMs` | `Long` | no | Durasi aktual (default 0) |
| `startedAt` | `Long` | no | |
| `endedAt` | `Long?` | yes | |
| `status` | `FocusStatus` | no | `ACTIVE / PAUSED / FINISHED / ABORTED` |
| `blockedAppsJson` | `String?` | yes | Untuk Strict/Custom |
| `fallbackReason` | `String?` | yes | Jika blocking gagal → Gentle |

```kotlin
enum class FocusMode { GENTLE, STRICT, CUSTOM }
enum class FocusStatus { ACTIVE, PAUSED, FINISHED, ABORTED }
```

---

## 5. Modul: Intervention (Phase 3)

### 5.1 `intervention_log`

Audit tiap intervensi (PRD FR-I5). Menjadi sumber kuota harian & cooldown.

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `id` | `String` (UUID) | no | PK |
| `level` | `Int` | no | 1–4 |
| `channel` | `InterventionChannel` | no | `NOTIFICATION / IN_APP / FULLSCREEN` |
| `contextPackage` | `String?` | yes | App konteks distraksi |
| `relatedTaskId` | `String?` | yes | Task yang dihubungkan |
| `status` | `InterventionStatus` | no | `INTERACTED / DISMISSED / EXPIRED` |
| `shownAt` | `Long` | no | Kapan ditampilkan |
| `resolvedAt` | `Long?` | yes | Kapan user bereaksi |

```kotlin
enum class InterventionChannel { NOTIFICATION, IN_APP, FULLSCREEN }
enum class InterventionStatus { INTERACTED, DISMISSED, EXPIRED }
```

**Query kuota harian:** `COUNT(*) WHERE status IN (INTERACTED, DISMISSED) AND shownAt ∈ hari-ini`.
`EXPIRED` (tidak berinteraksi) **tidak** menghabiskan kuota (PRD §10.4 aturan kuota).

**Query cooldown:** cari `INTERVENTION` terakhir dengan `contextPackage` sama dan
`resolvedAt` (aksi "Later") dalam **2 jam** terakhir.

---

## 6. Modul: Insight (Phase 5)

### 6.1 `insight`

| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `id` | `String` (UUID) | no | PK |
| `type` | `InsightType` | no | `DAILY_REFLECTION / PATTERN / SUGGESTION` |
| `dateEpochDay` | `Long` | no | Hari terkait |
| `source` | `InsightSource` | no | `AI / RULE_BASED` |
| `summary` | `String` | no | Teks ringkas |
| `patternsJson` | `String` | no | Array pola |
| `riskLevel` | `String?` | yes | `low/medium/high` |
| `suggestionsJson` | `String` | no | Array saran |
| `recommendedAction` | `String?` | yes | mis. `start_focus` |
| `createdAt` | `Long` | no | |

```kotlin
enum class InsightType { DAILY_REFLECTION, PATTERN, SUGGESTION }
enum class InsightSource { AI, RULE_BASED }
```

> Output AI **wajib** sesuai schema PRD §10.8. Jika respons tidak valid → tolak & simpan
> versi `RULE_BASED`.

---

## 7. Modul: Settings

### 7.1 `app_settings` (single row, id = 1)

| Kolom | Tipe | Default | Catatan |
|---|---|---|---|
| `id` | `Int` | 1 | PK konstanta |
| `trackingEnabled` | `Boolean` | **false** | Opt-in (privacy default, PRD FR-S6) |
| `aiEnabled` | `Boolean` | **false** | Opt-in |
| `notificationsEnabled` | `Boolean` | true | |
| `usageRetentionDays` | `Int` | 30 | Rentang 14–30 |
| `defaultFocusMode` | `FocusMode` | `GENTLE` | |
| `dailyInterventionQuota` | `Int` | 3 | |
| `cooldownHours` | `Int` | 2 | |
| `onboardingCompleted` | `Boolean` | false | |
| `locale` | `String` | `en` | Siap `id` |

> Preferensi non-rahasia boleh juga di DataStore. Namun status yang mempengaruhi query
> domain (retention, quota) sebaiknya di Room agar transaksional.

---

## 8. Relasi & Foreign Key

```
task_definition 1 ──< task_instance 1 ──< subtask
                          │ 1
                          ├──< reschedule_event
                          ├──< task_dependency (from/to)
                          └──< focus_session (taskId, optional)

usage_session   (raw, standalone)
daily_usage_summary / weekly_usage_summary (agregat, standalone)
app_category    (standalone, key = packageName)

intervention_log (standalone, optional FK → task_instance)
insight          (standalone, key = dateEpochDay)
app_settings     (single row)
```

- Cascade: `subtask` & `reschedule_event` ikut terhapus saat `task_instance` dihapus.
- `task_dependency`: menolak baris yang membentuk siklus (dicek di domain, bukan DB trigger).
- `usage_session` **tidak** punya FK (raw, cepat, di-prune).

---

## 9. Migrasi & Versioning

**Aturan aman migrasi (PRD §15):**
1. Versi DB di-*bump* setiap perubahan skema. Sediakan `Migration` eksplisit.
2. **Tambah** kolom → `ALTER TABLE ADD COLUMN` dengan default.
3. **Jangan** drop kolom/rename langsung; tambah kolom baru, migrasi data, baru bersihkan
   di versi berikutnya.
4. Untuk enum, selalu simpan sebagai `String`; tambah nilai baru tidak butuh migrasi.
5. Sediakan `exportSchema = true` dan simpan JSON skema di repo.
6. Uji migrasi dengan `MigrationTestHelper` (Room) untuk setiap versi sebelumnya.

**Contoh rentang migrasi:**

```
v1 (Phase 1): task_definition, task_instance, subtask, reschedule_event, app_settings
v2 (Phase 2): + usage_session, daily_usage_summary, weekly_usage_summary, app_category
v3 (Phase 3): + focus_session, intervention_log
v4 (Phase 4): + task_dependency
v5 (Phase 5): + insight
```

> Satu versi Room bisa mencakup beberapa tabel; pemisahan di atas hanya menandai *kapan*
> tabel masuk, bukan jumlah migrasi final.

---

## 10. Retention & Pruning

| Data | Kebijakan |
|---|---|
| `usage_session` (raw) | Hapus otomatis jika usia > **30 hari** (rentang 14–30); via `PruningWorker` |
| `daily_usage_summary` | Permanen (kecil, untuk tren) |
| `weekly_usage_summary` | Permanen |
| `intervention_log` | Permanen (baris kecil); boleh dibatasi 1 tahun |
| `insight` | Permanen / dibatasi sesuai kebutuhan |
| "Hapus semua data" | Menghapus seluruh tabel user (kecuali yang dipilih dipertahankan) — PRD §10.10 |

---

## 11. Referensi Silang

- Field wajib & aturan → `01-prd.md` §10.1, §10.2, §10.4, §10.8
- Arsitektur & worker → `02-architecture.md`
- Deteksi drift & kuota intervensi → `04-intervention-engine.md`

---

> **CATATAN AKHIR:** Nama kolom boleh disesuaikan saat implementasi selama **field wajib
> yang dikunci PRD tetap ada** dan semantiknya tidak berubah. Setiap perubahan model yang
> mengubah perilaku harus dimulai dari PRD.
