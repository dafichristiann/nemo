# Feature Specification — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` (§10), `02-user-journeys.md`
> **Catatan:** PRD memuat ringkasan fitur. Dokumen ini **memperluas** tiap fitur
> menjadi spesifikasi yang siap diimplementasi, dengan format seragam:
> Purpose · Model · Functional Requirements · UX · Edge Cases · Acceptance Criteria.

---

## Daftar Isi
1. [Module: Tasks (Smart Todo)](#1-module-tasks-smart-todo)
2. [Module: Wellbeing](#2-module-wellbeing)
3. [Module: Drift Detection](#3-module-drift-detection)
4. [Module: Intervention](#4-module-intervention)
5. [Module: Focus](#5-module-focus)
6. [Module: Future View](#6-module-future-view)
7. [Module: Domino Effect](#7-module-domino-effect)
8. [Module: Insights](#8-module-insights)
9. [Module: Settings](#9-module-settings)

---

## 1. Module: Tasks (Smart Todo)

### 1.1 Purpose
Mengekspresikan & mengelola niat user (apa, kapan, seberapa penting, berapa lama).

### 1.2 Domain Model

```kotlin
enum class Priority { LOW, MEDIUM, HIGH, URGENT }   // default MEDIUM
enum class TaskStatus { TODO, IN_PROGRESS, DONE, ARCHIVED }

data class Task(
    val id: Long,
    val title: String,                     // wajib, non-blank
    val description: String?,              // opsional
    val deadline: Long?,                   // epoch millis, opsional
    val priority: Priority = Priority.MEDIUM,
    val estimatedDurationMin: Int?,        // menit, opsional
    val categoryId: Long?,                 // FK Category, opsional
    val status: TaskStatus = TaskStatus.TODO,
    val createdAt: Long,
    val completedAt: Long?,
    val progressPercent: Int = 0,          // 0..100
    val recurrenceRule: String? = null,    // [DORMANT Phase 1]
    val parentTaskId: Long? = null         // hierarki dasar / dependency [DORMANT]
)

data class Subtask(
    val id: Long,
    val taskId: Long,                      // FK Task (onDelete CASCADE)
    val title: String,
    val isDone: Boolean = false,
    val orderIndex: Int
)
```

### 1.3 Functional Requirements

| ID | Requirement |
|---|---|
| T-1 | Buat task dengan title (wajib) + field opsional lain |
| T-2 | Edit task |
| T-3 | Hapus task (soft: ARCHIVED; hard: delete + konfirmasi) |
| T-4 | Set priority (4 level) |
| T-5 | Set deadline (tanggal + jam opsional) |
| T-6 | Set estimasi durasi (menit) |
| T-7 | Set kategori (dari daftar; bisa buat kategori baru) |
| T-8 | Subtask CRUD + reorder + check/uncheck |
| T-9 | Progress dihitung dari subtask (jika ada); else manual |
| T-10 | Tandai selesai → status DONE + `completedAt` |
| T-11 | Filter view: All / Today / Upcoming / Completed |
| T-12 | Tandai overdue (visual halus) |
| T-13 | Reschedule deadline (catat kejadian untuk insight) |
| T-14 | `[DORMANT]` Model dependency disiapkan, UI di Phase 4 |
| T-15 | `[DORMANT]` Model recurring disiapkan, mesin eksekusi di phase lanjut |

### 1.4 UX Behavior
- **Quick-add:** satu input title; tekan save langsung; detail menyusul.
- Priority ditandai warna halus (bukan blok warna mencolok).
- Task list: urut default = priority desc, lalu deadline asc.
- Swipe actions: complete / reschedule / archive (tenang, bukan meriah).
- Chip filter di atas: **All · Today · Upcoming · Completed**.

### 1.5 Overdue Behavior (`[DECISION]` OQ3 — resolved)
- Tandai task dengan indikator halus (mis. teks "Overdue" muted).
- **Tidak ada** notifikasi menghakimi.
- Sediakan aksi **Reschedule** yang mudah dijangkau.
- Catat reschedule ke tabel activity untuk pola penundaan (Phase 5).

### 1.6 Recurring (`[DECISION]` OQ1/A3 — resolved)
- Phase 1: hanya **field** `recurrenceRule` (format disiapkan, dormant).
- Mesin spawn task berulang: **phase lanjut** (bukan Phase 1).
- Alasan: hindari kompleksitas & migrasi besar.

### 1.7 Edge Cases
| Kasus | Perilaku |
|---|---|
| Title kosong | Tombol Save disabled |
| Deadline masa lalu | Boleh, langsung bertanda overdue |
| Estimasi 0/null | Future View pakai default internal (ditandai) |
| Semua subtask done | Suggest DONE (tidak auto-paksa) |
| Hapus task dengan subtask | Cascade delete subtask |
| Hapus task ber-dependency (Phase 4) | Tawarkan lepas/ubah |

### 1.8 Acceptance Criteria
```
Given  user membuat task lengkap dengan deadline+priority+estimasi
When   disimpan
Then   task muncul di view yang sesuai (Today/Upcoming) & tersimpan di Room

Given  task punya 3 subtask, 1 sudah done
When   user check subtask ke-2
Then   progressPercent naik proporsional & tersimpan

Given  task melewati deadline
When   user membuka list
Then   task diberi indikator overdue halus + aksi Reschedule tersedia,
       tanpa notifikasi menghakimi

Given  user menekan save dengan title kosong
When   tombol ditekan
Then   tidak tersimpan & ada indikasi field wajib
```

---

## 2. Module: Wellbeing

### 2.1 Purpose
Mengumpulkan & menampilkan pola penggunaan perangkat **secara lokal** sebagai basis
deteksi drift.

### 2.2 Data Sources
- `UsageStatsManager.queryUsageStats()` — agregat per app per interval.
- `UsageStatsManager.queryEvents()` — event foreground/session (jika tersedia).
- `AppOpsManager` / `UsageStatsManager` — cek status `PACKAGE_USAGE_STATS`.

### 2.3 Domain Model
```kotlin
data class UsageSnapshot(       // agregat per app per hari
    val id: Long,
    val dateEpochDay: Long,
    val packageName: String,
    val categoryId: Long?,
    val totalTimeMs: Long,
    val sessionCount: Int,
    val firstUseMs: Long?,
    val lastUseMs: Long?
)

data class UsageSession(        // sesi individual (jika tersedia dari events)
    val id: Long,
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long
)
```

### 2.4 Functional Requirements
| ID | Requirement |
|---|---|
| W-1 | Cek & laporkan status izin `PACKAGE_USAGE_STATS` |
| W-2 | Query usage harian per app |
| W-3 | Query usage 7 hari (tren) |
| W-4 | Agregasi lokal → simpan `UsageSnapshot` |
| W-5 | Bangun `UsageSession` dari events (jika tersedia) |
| W-6 | Hitung baseline personal (rolling window) |
| W-7 | Tampilkan: total screen time, top apps, kategori, tren |
| W-8 | Semua angka dari Android API (bukan AI, bukan fake) |

### 2.5 Keterbatasan Android (WAJIB ditampilkan jujur)
- `PACKAGE_USAGE_STATS` = special access → user aktifkan manual.
- Beberapa OEM membatasi granularitas sesi.
- Data bisa ter-sampling → akurasi tidak dijamin (Spike A).
- Tidak realtime → gunakan agregasi periodik (WorkManager).

### 2.6 UX Behavior
- Angka netral, tanpa label baik/buruk.
- Permission belum ada → empty state + tombol Settings.
- Tren sederhana (bar/sparkline), bukan dashboard ramai.

### 2.7 Edge Cases
| Kasus | Perilaku |
|---|---|
| UsageStats kosong (OEM) | Pesan jujur; task tetap jalan |
| Ganti hari/timezone | Hitung ulang by batas hari lokal |
| Data < N hari | Jangan tampilkan baseline menyesatkan |
| App di-uninstall | Abaikan paket tanpa label; pakai snapshot lama |

### 2.8 Acceptance Criteria
```
Given  izin usage belum diberikan
When   buka Wellbeing
Then   tampil panduan izin, tidak crash, app tetap berguna

Given  data 7 hari tersedia
When   buka Wellbeing
Then   tampil total screen time, top apps, tren dari data lokal

Given  OEM tidak menyediakan sesi detail
When   buka Wellbeing
Then   tampil apa yang ada + penjelasan jujur, tanpa angka karangan
```

---

## 3. Module: Drift Detection

### 3.1 Purpose
Menyadari penyimpangan perilaku dari **baseline personal**.

### 3.2 Rules (NO universal thresholds)
```
baseline = mean(session_duration[app], window = last N days)
deviation = (current_session - baseline) / baseline

drift = deviation >= threshold            // threshold configurable
        AND app_has_enough_baseline_data
        AND NOT in_cooldown
        AND daily_intervention_count < 3
```

> `[DECISION]` Threshold awal usulan: `+100%` (deviasi ≥ 2x baseline) atau
> `P95` historis user. **Dikalibrasi setelah Spike A + pemakaian nyata. Jangan dikunci.**

### 3.3 Functional Requirements
| ID | Requirement |
|---|---|
| D-1 | Hitung baseline per app dari data nyata |
| D-2 | Hitung deviasi sesi aktif |
| D-3 | Tandai drift bila threshold + syarat terpenuhi |
| D-4 | Hasilkan sinyal drift → Intervention Engine |
| D-5 | Pesan netral (tanpa diagnosis) |
| D-6 | Tidak mengklaim apa pun bila baseline belum cukup |

### 3.4 Output
```
"Drift detected: TikTok, current 47m vs baseline 18m (+161%)"
→ user-facing: "You've been scrolling longer than usual."
```

### 3.5 Edge Cases
| Kasus | Perilaku |
|---|---|
| Baseline < N hari | Tidak ada drift |
| App baru | Tidak ada baseline → tidak intervensi |
| Sesi sangat singkat | Bukan drift |
| User sengaja santai ("Later") | Hormati cooldown |

### 3.6 Acceptance Criteria
```
Given  baseline TikTok = 18m (data cukup)
When   sesi = 47m
Then   sinyal drift dihasilkan TANPA kata "addicted"

Given  data baseline belum cukup
When   pakai app apa pun
Then   TIDAK ada sinyal drift
```

---

## 4. Module: Intervention

### 4.1 Purpose
Menyampaikan awareness kontekstual pada momen tepat, dengan pilihan.

### 4.2 Levels
| Level | Pesan | Trigger |
|---|---|---|
| 1 Gentle | "You've been here for a while." | Drift terdeteksi |
| 2 Contextual | "Your Web Assignment is still unfinished." | Ada task HIGH tertunda |
| 3 Future View | "Starting now gives you ~2h free time tonight." | Deadline dekat / drift berulang |
| 4 Focus | "Start Focus." | CTA di semua level |

### 4.3 Domain Model
```kotlin
enum class InterventionLevel { GENTLE, CONTEXTUAL, FUTURE_VIEW, FOCUS_CTA }
enum class InterventionChannel { NOTIFICATION, IN_APP, FULLSCREEN }
enum class InterventionOutcome { SHOWN, DISMISSED, LATER, STARTED_FOCUS, IGNORED }

data class Intervention(
    val id: Long,
    val timestamp: Long,
    val level: InterventionLevel,
    val channel: InterventionChannel,
    val triggerPackage: String?,
    val relatedTaskId: Long?,
    val outcome: InterventionOutcome,
    val dayEpochDay: Long
)
```

### 4.4 Rules
- Max **3 intervensi bermakna/hari**.
- Cooldown setelah **Later** (mis. 45–60 menit).
- Bisa dismiss mudah.
- Level naik **bertahap** (tidak langsung Level 4).
- Channel default: Notification.

### 4.5 Functional Requirements
| ID | Requirement |
|---|---|
| I-1 | Terima sinyal drift → evaluasi relevansi |
| I-2 | Tentukan level yang sesuai |
| I-3 | Pilih channel (default notification) |
| I-4 | Hormati "Later" → set cooldown |
| I-5 | Catat tiap intervensi ke DB |
| I-6 | Batasi total harian, reset tiap hari lokal |
| I-7 | Aksi: [Start Focus] / [Later] (+ opsi Future View) |

### 4.6 Edge Cases
| Kasus | Perilaku |
|---|---|
| Notification permission ditolak | Fallback in-app |
| User di dalam Anchor | Jangan kirim notif hal sama |
| Fullscreen berisiko mengganggu | Hanya kondisi benar-benar relevan |
| Cooldown aktif | Tahan intervensi |

### 4.7 Acceptance Criteria
```
Given  high-priority task belum selesai
When   scrolling jauh di atas baseline
Then   DAPAT memberi contextual intervention

Given  user menekan "Later"
When   kembali scrolling app sama dalam cooldown
Then   TIDAK langsung mengirim intervensi lagi

Given  sudah 3 intervensi hari ini
When   sinyal drift baru muncul
Then   intervensi tambahan ditahan
```

---

## 5. Module: Focus

### 5.1 Purpose
Membantu user masuk & menyelesaikan sesi fokus.

### 5.2 Modes
| Mode | Perilaku |
|---|---|
| Gentle | Awareness + notif reduction, tanpa blokir |
| Strict | Blokir app distraksi terpilih |
| Custom | User tentukan app yang diblokir |

### 5.3 Domain Model
```kotlin
enum class FocusMode { GENTLE, STRICT, CUSTOM }
enum class FocusState { SETUP, RUNNING, PAUSED, FINISHED, ABORTED }

data class FocusSession(
    val id: Long,
    val taskId: Long,
    val mode: FocusMode,
    val startMs: Long,
    val endMs: Long?,
    val plannedMs: Long?,
    val actualMs: Long?,
    val state: FocusState,
    val interruptionCount: Int = 0
)
```

### 5.4 Blocking Abstraction
```kotlin
interface BlockingStrategy {
    val id: String
    suspend fun isAvailable(): Boolean
    suspend fun start(blockedPackages: Set<String>)
    suspend fun stop()
}
// Implementasi: NoneStrategy, NotificationOnlyStrategy, AccessibilityStrategy
```

### 5.5 Functional Requirements
| ID | Requirement |
|---|---|
| F-1 | Focus Setup: pilih task + mode |
| F-2 | Mulai sesi, timer berjalan, catat `FocusSession` |
| F-3 | Gentle: kurangi notifikasi tanpa blokir |
| F-4 | Strict/Custom: pakai `BlockingStrategy` terpilih |
| F-5 | **Emergency exit selalu tersedia** |
| F-6 | Pause & Finish; Finish tawarkan update task |
| F-7 | Foreground Service hanya saat fokus aktif |
| F-8 | Blocking gagal → fallback Gentle |

### 5.6 Edge Cases
| Kasus | Perilaku |
|---|---|
| Accessibility mati saat Strict | Jatuh ke Gentle + beri tahu |
| OS matikan service | Deteksi & fallback |
| Layar mati lama | Sesi tetap tercatat |
| Baterai lemah | Beri tahu, jangan paksa |
| User force-close app | Sesi dihentikan rapi |

### 5.7 Acceptance Criteria
```
Given  user pilih task + mode Gentle
When   mulai
Then   timer jalan, task tampil, Pause/Finish ada, tidak ada blokir

Given  mode Strict dengan TikTok
When   user buka TikTok
Then   TikTok dibatasi & emergency exit tetap bisa diakses

Given  sesi selesai
When   user Finish
Then   FocusSession tercatat & ditawari update progres
```

---

## 6. Module: Future View

### 6.1 Purpose
Menunjukkan konsekuensi waktu "mulai sekarang" vs "menunda" — awareness, bukan fear.

### 6.2 Computation (rule-based)
```
Input: now, estimatedDuration, deadline, otherScheduledTasks, dependencies
startNow   = now + estimatedDuration  → free time after
keepDelay  = heuristik dari pola user (atau default +X jam) → finish closer to deadline
```
> `[ASSUMPTION]` Delay scenario memakai pola user bila ada; jika tidak, pakai
> asumsi default bertanda. **Bukan** prediksi AI.

### 6.3 Functional Requirements
| ID | Requirement |
|---|---|
| FV-1 | Hitung dua skenario dari input |
| FV-2 | Tampilkan berdampingan |
| FV-3 | Disclaimer "based on your current schedule..." |
| FV-4 | Aksi [Start Focus] |
| FV-5 | Data kurang → jelaskan, jangan mengarang |

### 6.4 Edge Cases
| Kasus | Perilaku |
|---|---|
| Tanpa estimasi | Pakai default + tandai asumsi |
| Waktu lewat deadline | Skenario realistis, tanpa panik |
| Tanpa dependency | Domino kosong (bukan error) |

### 6.5 Acceptance Criteria
```
Given  deadline 23:59, estimasi 2h, now 15:10
When   buka Future View
Then   dua skenario tampil + disclaimer

Given  tanpa estimasi
When   buka Future View
Then   pakai default & tandai asumsi dengan jelas
```

---

## 7. Module: Domino Effect

### 7.1 Purpose
Menunjukkan dampak penundaan ke task yang bergantung.

### 7.2 Domain Model
```kotlin
enum class DependencyType { BLOCKS, RELATED }
data class TaskDependency(
    val id: Long,
    val fromTaskId: Long,
    val toTaskId: Long,
    val type: DependencyType
)
```

### 7.3 Functional Requirements
| ID | Requirement |
|---|---|
| DE-1 | Simpan dependency antar task |
| DE-2 | Visualisasi rantai |
| DE-3 | Hitung jumlah task berpotensi terdampak |
| DE-4 | Bahasa estimatif ("may affect") |
| DE-5 | Tap node → navigasi ke task |

### 7.4 Edge Cases
| Kasus | Perilaku |
|---|---|
| Siklus (A→B→A) | Cegah saat menyimpan |
| Tanpa dependency | Empty state jelas |
| Hapus task ber-dependency | Tawarkan lepas/ubah |

### 7.5 Acceptance Criteria
```
Given  3 task bergantung pada "Web Assignment"
When   buka Domino Effect
Then   rantai + "1 task may affect 3 upcoming tasks" (estimatif)

Given  membuat dependency siklus
When   simpan
Then   dicegah + penjelasan
```

---

## 8. Module: Insights

### 8.1 Purpose
Menginterpretasi data agregat → insight personal, dengan fallback.

### 8.2 Output Schema
```json
{
  "summary": "...",
  "patterns": [],
  "risk_level": "low",
  "suggestions": [],
  "recommended_action": "start_focus"
}
```

### 8.3 Domain Model
```kotlin
enum class InsightSource { AI, RULE_BASED }
data class Insight(
    val id: Long,
    val dateEpochDay: Long,
    val source: InsightSource,
    val summary: String,
    val patternsJson: String,
    val riskLevel: String,
    val suggestionsJson: String,
    val recommendedAction: String?,
    val createdAt: Long
)
```

### 8.4 Functional Requirements
| ID | Requirement |
|---|---|
| AI-1 | Susun payload agregat (bukan raw) |
| AI-2 | Kirim ke Gemini hanya jika aktif & tersedia |
| AI-3 | Cache & batch (maks 1x/hari + on-demand) |
| AI-4 | Fallback ke `RuleBasedInsightSource` |
| AI-5 | Simpan hasil ke `Insight` (catat sumber) |
| AI-6 | Hanya tampilkan insight didukung data |

### 8.5 Edge Cases
| Kasus | Perilaku |
|---|---|
| Offline / key kosong / quota | Rule-based tetap ada |
| Respons di luar schema | Tolak & fallback |
| Data minim | Nyatakan keterbatasan |

### 8.6 Acceptance Criteria
```
Given  AI dinonaktifkan/offline
When   buka Insights
Then   rule-based insight tampil tanpa error

Given  payload berisi raw usage
When   hendak dikirim
Then   DITOLAK, hanya agregat
```

---

## 9. Module: Settings

### 9.1 Sections
- Tracking permissions (status + pintasan)
- Notifications
- Focus rules
- Blocked apps
- Categories (+ override per app)
- Privacy & AI
- Data retention + hapus data

### 9.2 Functional Requirements
| ID | Requirement |
|---|---|
| S-1 | Tampilkan status izin + cara enable |
| S-2 | Toggle AI; jelaskan sederhana apa yang dikirim |
| S-3 | Kelola kategori & override |
| S-4 | Kelola blocked apps |
| S-5 | Atur retensi + hapus semua data |
| S-6 | Default privacy-preserving |

### 9.3 Acceptance Criteria
```
Given  first launch
When   onboarding
Then   TIDAK memaksa izin; Smart Todo langsung bisa dipakai

Given  user hapus semua data
When   dikonfirmasi
Then   data lokal terhapus sesuai pilihan
```
