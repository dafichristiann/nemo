# Database Design — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Teknologi:** Room (SQLite) untuk data terstruktur; Preferences DataStore untuk settings.
> **Prinsip:** forward-compatible, versioned, migration-safe sejak awal.
> **Dokumen terkait:** `03-feature-specification.md`, `06-technical-architecture.md`

---

## 1. Ringkasan

- **Skema versi awal:** `version = 1`
- **Konvensi:** nama tabel `snake_case` plural; kolom `snake_case`.
- **Tipe waktu:** epoch millis (`Long`).
- **Tipe hari:** `date_epoch_day` (`Long`) untuk agregasi harian (stable lintas timezone).
- **Foreign keys:** aktif dengan `onDelete` sesuai kebutuhan.
- **Indeks:** pada kolom yang sering difilter (status, deadline, date).

---

## 2. Entity Relationship (ringkas)

```
Category 1────* Task
Task     1────* Subtask
Task     1────* TaskDependency (from/to)
Task     1────* FocusSession
Task     0..1──* Intervention (related_task)

UsageSnapshot *────1 Category (nullable)
UsageSession  (berdiri sendiri, by package)

Insight       (berdiri sendiri, harian)
```

---

## 3. Tables

### 3.1 `tasks`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK, autoinc) | no | — | |
| `title` | TEXT | no | — | wajib |
| `description` | TEXT | yes | NULL | |
| `deadline` | INTEGER | yes | NULL | epoch millis |
| `priority` | TEXT | no | 'MEDIUM' | LOW/MEDIUM/HIGH/URGENT |
| `estimated_duration_min` | INTEGER | yes | NULL | menit |
| `category_id` | INTEGER (FK) | yes | NULL | → categories.id |
| `status` | TEXT | no | 'TODO' | TODO/IN_PROGRESS/DONE/ARCHIVED |
| `created_at` | INTEGER | no | — | |
| `completed_at` | INTEGER | yes | NULL | |
| `progress_percent` | INTEGER | no | 0 | 0..100 |
| `recurrence_rule` | TEXT | yes | NULL | [dormant Phase 1] |
| `parent_task_id` | INTEGER (FK self) | yes | NULL | [dormant] |

**Indeks:** `status`, `deadline`, `category_id`.

### 3.2 `subtasks`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `task_id` | INTEGER (FK) | no | — | → tasks.id, CASCADE |
| `title` | TEXT | no | — | |
| `is_done` | INTEGER (bool) | no | 0 | |
| `order_index` | INTEGER | no | 0 | |

**Indeks:** `task_id`.

### 3.3 `task_dependencies`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `from_task_id` | INTEGER (FK) | no | — | task yang mendahului |
| `to_task_id` | INTEGER (FK) | no | — | task yang bergantung |
| `type` | TEXT | no | 'BLOCKS' | BLOCKS/RELATED |

**Indeks:** `from_task_id`, `to_task_id`.
**Constraint:** cegah siklus di level aplikasi (validasi sebelum insert).

### 3.4 `categories`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `name` | TEXT | no | — | mis. Productivity, Social |
| `is_system` | INTEGER (bool) | no | 0 | seed default |
| `color_hex` | TEXT | yes | NULL | aksen opsional |

**Seed default:** Productivity, Communication, Entertainment, Social Media,
Games, Utilities, Other.

### 3.5 `app_category_overrides`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `package_name` | TEXT | no | — | unik |
| `category_id` | INTEGER (FK) | no | — | → categories.id |

**Indeks:** unik `package_name`.

### 3.6 `usage_snapshots`
Agregat per app per hari.
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `date_epoch_day` | INTEGER | no | — | hari lokal |
| `package_name` | TEXT | no | — | |
| `category_id` | INTEGER (FK) | yes | NULL | resolusi kategori |
| `total_time_ms` | INTEGER | no | 0 | |
| `session_count` | INTEGER | no | 0 | |
| `first_use_ms` | INTEGER | yes | NULL | |
| `last_use_ms` | INTEGER | yes | NULL | |

**Indeks:** unik(`date_epoch_day`,`package_name`), `date_epoch_day`.

### 3.7 `usage_sessions`
Sesi individual (jika tersedia dari events).
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `package_name` | TEXT | no | — | |
| `start_ms` | INTEGER | no | — | |
| `end_ms` | INTEGER | no | — | |
| `duration_ms` | INTEGER | no | — | |
| `date_epoch_day` | INTEGER | no | — | untuk filter cepat |

**Indeks:** `package_name`, `date_epoch_day`.

### 3.8 `focus_sessions`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `task_id` | INTEGER (FK) | no | — | → tasks.id |
| `mode` | TEXT | no | 'GENTLE' | GENTLE/STRICT/CUSTOM |
| `start_ms` | INTEGER | no | — | |
| `end_ms` | INTEGER | yes | NULL | |
| `planned_ms` | INTEGER | yes | NULL | |
| `actual_ms` | INTEGER | yes | NULL | |
| `state` | TEXT | no | 'RUNNING' | SETUP/RUNNING/PAUSED/FINISHED/ABORTED |
| `interruption_count` | INTEGER | no | 0 | |

**Indeks:** `task_id`, `start_ms`.

### 3.9 `insights`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `date_epoch_day` | INTEGER | no | — | |
| `source` | TEXT | no | 'RULE_BASED' | AI/RULE_BASED |
| `summary` | TEXT | no | — | |
| `patterns_json` | TEXT | yes | NULL | |
| `risk_level` | TEXT | yes | NULL | low/medium/high |
| `suggestions_json` | TEXT | yes | NULL | |
| `recommended_action` | TEXT | yes | NULL | |
| `created_at` | INTEGER | no | — | |

**Indeks:** unik(`date_epoch_day`,`source`).

### 3.10 `interventions`
| Kolom | Tipe | Null | Default | Catatan |
|---|---|---|---|---|
| `id` | INTEGER (PK) | no | — | |
| `timestamp` | INTEGER | no | — | |
| `level` | TEXT | no | — | GENTLE/CONTEXTUAL/FUTURE_VIEW/FOCUS_CTA |
| `channel` | TEXT | no | — | NOTIFICATION/IN_APP/FULLSCREEN |
| `trigger_package` | TEXT | yes | NULL | |
| `related_task_id` | INTEGER (FK) | yes | NULL | |
| `outcome` | TEXT | no | 'SHOWN' | SHOWN/DISMISSED/LATER/STARTED_FOCUS/IGNORED |
| `day_epoch_day` | INTEGER | no | — | untuk hitung harian |

**Indeks:** `day_epoch_day`, `outcome`.

### 3.11 `activity_log` (pendukung insight pola)
Mencatat kejadian penting untuk analisis Phase 5.
| Kolom | Tipe | Null | Catatan |
|---|---|---|---|
| `id` | INTEGER (PK) | no | |
| `timestamp` | INTEGER | no | |
| `type` | TEXT | no | TASK_CREATED/TASK_COMPLETED/TASK_RESCHEDULED/… |
| `task_id` | INTEGER | yes | |
| `metadata_json` | TEXT | yes | detail ringan |

**Indeks:** `type`, `timestamp`.

---

## 4. DataStore (Preferences)

Kunci utama di `settings` DataStore:

| Key | Tipe | Default | Catatan |
|---|---|---|---|
| `ai_enabled` | Boolean | false | opt-in |
| `ai_api_key` | String | "" | dikonfigurasi user |
| `tracking_enabled` | Boolean | false | opt-in |
| `default_focus_mode` | String | "GENTLE" | |
| `blocked_packages` | Set<String> | {} | |
| `drift_threshold_percent` | Int | 100 | kalibrasi |
| `daily_intervention_limit` | Int | 3 | |
| `intervention_cooldown_min` | Int | 45 | |
| `data_retention_days` | Int | 30 | |
| `theme_mode` | String | "SYSTEM" | |
| `onboarding_done` | Boolean | false | |

> API key disimpan di DataStore untuk MVP. `[DECISION]` Untuk keamanan lebih baik,
> gunakan EncryptedSharedPreferences/Keystore bila dirasa perlu. Tidak dikunci.

---

## 5. Migration Concerns

- **Mulai `version = 1`** dengan migration scaffolding.
- Setiap perubahan skema → naikkan versi + `Migration` eksplisit (bukan fallbackToDestructive).
- Kolom baru → nullable atau beri default.
- Tabel dependency & recurring sudah ada sejak Phase 1 → menghindari migrasi besar Phase 4.
- Uji migration dengan `MigrationTestHelper`.

---

## 6. Retention & Cleanup

- `usage_snapshots`/`usage_sessions` lebih tua dari `data_retention_days` → dihapus oleh worker.
- `interventions` > retensi → dihapus (kecuali agregat yang dibutuhkan).
- "Delete all data" menghapus semua tabel + reset DataStore (kecuali yang user pilih).

---

## 7. Query Patterns (contoh)

```
Tasks Today:
  SELECT * FROM tasks
  WHERE status != 'ARCHIVED'
    AND (deadline IS NULL OR deadline < :endOfToday)
  ORDER BY priority DESC, deadline ASC

Baseline TikTok (14 hari):
  SELECT AVG(duration_ms) FROM usage_sessions
  WHERE package_name = :pkg AND date_epoch_day >= :from

Longest distraction hari ini:
  SELECT package_name, MAX(total_time_ms) FROM usage_snapshots
  WHERE date_epoch_day = :today GROUP BY package_name ORDER BY 2 DESC LIMIT 1

Intervensi hari ini:
  SELECT COUNT(*) FROM interventions
  WHERE day_epoch_day = :today AND outcome != 'IGNORED'
```
