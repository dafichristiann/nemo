# Sprint Backlog — Anchor (Personal Focus Assistant)

> **Status:** Derived from `05-mvp-scope.md` · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `05-mvp-scope.md`, `01-prd.md` §10–§11, `11-testing-strategy.md`
> **Catatan:** Backlog ini adalah **pemecahan teknis** dari fase MVP menjadi item kerja.
> Tidak menambah fitur baru. Setiap item dapat dilacak ke FR/AC di PRD.

---

## 0. Konvensi

- **Format:** `[Phase]-[nn]` · Judul · FR/AC terkait · Est (S/M/L) · Dependensi.
- **Est:** S ≈ 0.5–1 hari, M ≈ 2–3 hari, L ≈ 4–5 hari (solo dev, perkiraan).
- **DoD:** mengikuti `11-testing-strategy.md` §6.
- Setiap item yang mengubah perilaku **wajib** merujuk FR/AC PRD.

---

## Sprint 0 — Fondasi

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 0-01 | Setup project (Gradle, Kotlin, Compose, Hilt, versi SDK) | – | S | – |
| 0-02 | Theme & design tokens (monokrom, Inter, Material Symbols) | `06-design-system.md` | M | 0-01 |
| 0-03 | Navigasi shell (4 tab: Tasks/Wellbeing/Focus/Insights) | `07-screens.md` | M | 0-02 |
| 0-04 | Room v1 (task tables) + `exportSchema` + DI | §10.1 | M | 0-01 |
| 0-05 | `TimeProvider`, `DispatcherProvider`, common util | – | S | 0-01 |
| 0-06 | Design system komponen reusable (lihat §07 §4) | – | M | 0-02 |

---

## Sprint 1 — Phase 1: Smart Todo

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 1-01 | Entity `TaskDefinition`/`TaskInstance`/`Subtask` + DAO | §10.1 | M | 0-04 |
| 1-02 | CreateTask (judul, deskripsi, deadline, priority, est, kategori) | FR-T1..T5 | M | 1-01 |
| 1-03 | Subtask CRUD + check + order + progress auto | FR-T6, FR-T9 | M | 1-01 |
| 1-04 | Complete task → DONE + `completedAt` | FR-T8 | S | 1-01 |
| 1-05 | View All/Today/Upcoming/Completed | FR-T10 | M | 1-01 |
| 1-06 | Quick-add + empty state ramah | UX §10.1 | S | 1-02 |
| 1-07 | Overdue banner (non-blocking, Reschedule/Complete/Keep/Dismiss) | FR-T11, AC §10.1 | M | 1-05 |
| 1-08 | Reschedule + `RescheduleEvent` | FR-T12 | M | 1-01 |
| 1-09 | Recurring DAILY/WEEKLY (generator idempotent) | FR-T7 | L | 1-01 |
| 1-10 | Layar 01 Home + 02 Add Task + 10 Task Detail | `07-screens.md` | L | 1-02..1-05 |
| 1-11 | Onboarding 00 + 00b (izin opsional) | §10.10, §14 | M | 0-03 |
| 1-12 | Settings dasar + string resources | FR-S1, FR-S6 | M | 0-03 |

**Demo Sprint 1:** buat task, checklist subtask, selesaikan; recurring harian; overdue banner.

---

## Sprint 2 — Phase 2: Data Pipeline Wellbeing

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 2-01 | **Spike A** UsageStats (granularitas di device target) | Risiko R2 | M | 0-01 |
| 2-02 | Wrapper `UsageStatsManager` + cek izin | FR-W1 | M | 2-01 |
| 2-03 | `UsageCollectWorker` → `UsageSession` | FR-W2, R1 | M | 2-02 |
| 2-04 | `AggregationWorker` → Daily/Weekly Summary | FR-W3 | M | 2-03 |
| 2-05 | `PruningWorker` (retensi 14–30 hari) | FR-W4 | M | 2-03 |
| 2-06 | `AppCategory` + deteksi distraksi | FR-S3 | M | 2-04 |
| 2-07 | Baseline personal per app | FR-W5 | M | 2-04 |
| 2-08 | Unit test agregasi + pruning + timezone | `11-testing` | M | 2-04 |

---

## Sprint 3 — Phase 2: UI Wellbeing

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 3-01 | Layar 03 Wellbeing (screen time, top apps, kategori, tren bar) | FR-W6, AC §10.2 | L | 2-04 |
| 3-02 | Permission state + panduan Settings (Wellbeing) | FR-W1, AC | M | 2-02 |
| 3-03 | Empty state (tanpa izin / tanpa data) | AC §10.2 | S | 3-01 |
| 3-04 | `DriftCheckWorker` (periodic) + `DetectDriftUseCase` | FR-D1..D6 | L | 2-07 |
| 3-05 | Unit test drift (deviasi, sample days, netral) | AC §10.3 | M | 3-04 |

**Demo Sprint 2–3:** Wellbeing menampilkan data nyata; tanpa izin → empty state.

---

## Sprint 4 — Phase 3: Intervention Engine

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 4-01 | `intervention_log` + DAO + query kuota/cooldown | §10.4, §5 schema | M | 2-04 |
| 4-02 | `InterventionEngine.evaluate` (kuota, cooldown, level, channel) | FR-I1..I3, I6 | L | 4-01 |
| 4-03 | Aksi Start Focus/Later (+cooldown 2 jam) | FR-I4, I7 | M | 4-02 |
| 4-04 | Channel Notification (utama) + fallback in-app | FR-I3 | M | 4-02 |
| 4-05 | Layar 04 Contextual Intervention | `07-screens.md` | M | 4-04 |
| 4-06 | Unit test kuota/cooldown/detection-vs-attempt | AC §10.4 | M | 4-02 |
| 4-07 | **Spike B** Accessibility (kelayakan & baterai) | Risiko R3 | M | 0-01 |

---

## Sprint 5 — Phase 3: Focus Mode

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 5-01 | `focus_session` + DAO | §10.5 | S | 0-04 |
| 5-02 | Layar 05 Focus Setup (task + mode + durasi) | FR-F1 | M | 4-02 |
| 5-03 | Foreground Service + notifikasi persisten | FR-F7 | M | 5-02 |
| 5-04 | Layar 06 Focus Active (timer, current step, pause/finish, emergency) | FR-F2..F6 | L | 5-03 |
| 5-05 | Gentle = kurangi notifikasi (tanpa block) | FR-F3 | M | 5-03 |
| 5-06 | Strict/Custom + `BlockingStrategy` + fallback Gentle | FR-F4, F8 | L | 4-07 |
| 5-07 | Finish → catat sesi + tawaran update progress | FR-F6, AC | M | 5-04 |

**Demo Sprint 4–5:** intervensi kontekstual (kuota/cooldown); sesi fokus lengkap + emergency.

---

## Sprint 6 — Phase 4: Future View + Domino

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 6-01 | `FutureViewCalculator` (dua skenario, heuristic) | FR-FV1, FV5 | L | 1-05 |
| 6-02 | Layar 07 Future View + disclaimer + tandai asumsi | FR-FV2..FV4 | L | 6-01 |
| 6-03 | `TaskDependency` + anti-siklus | FR-DE1, AC | M | 1-01 |
| 6-04 | Visualisasi Domino + bahasa "may affect" | FR-DE2..DE5 | M | 6-03 |
| 6-05 | Aksi Start Focus dari Future View | FR-FV4 | S | 5-02 |
| 6-06 | Unit test future view + siklus | AC §10.6–10.7 | M | 6-01 |

---

## Sprint 7 — Phase 5: Insights

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 7-01 | `insight` + DAO | §10.8 | S | 0-04 |
| 7-02 | `RuleBasedInsightSource` (fallback wajib) | FR-AI4 | M | 7-01 |
| 7-03 | `GenerateDailyReflectionUseCase` + `DailyReflectionWorker` | FR-DR1..DR5 | M | 7-02 |
| 7-04 | Payload agregat + audit tolak raw | FR-AI1, AC §10.8 | M | 7-01 |
| 7-05 | `GeminiInsightSource` (opt-in) + cache/batch | FR-AI2, AI3 | L | 7-04 |
| 7-06 | Layar 08 Insights (stats, distraction, insight, suggestion, pattern) | `07-screens.md` | L | 7-03 |
| 7-07 | Validasi schema respons + fallback | FR-AI4, AC | M | 7-05 |

**Demo Sprint 6–7:** Future View + Domino; Daily Reflection (rule-based, lalu AI opsional).

---

## Sprint 8 — Hardening

| ID | Item | FR/AC | Est | Dep |
|---|---|---|---|---|
| 8-01 | Uji performa (RAM < 150MB) di device low-RAM | §12 | M | all |
| 8-02 | Uji baterai (≤ 2–3%/24 jam) | §12 | M | all |
| 8-03 | Uji semua jalur izin ditolak/OEM batasi | §12, `08-` | M | all |
| 8-04 | Uji migrasi DB semua versi | `03-` §9 | M | all |
| 8-05 | Audit privasi (`allowBackup=false`, no logs, payload) | `09-` §7 | M | all |
| 8-06 | Rapikan copy (nada calm, lokalisasi) | §4 | S | all |
| 8-07 | Regresi manual alur kritis | `11-` §2 | M | all |

---

## Post-MVP (Phase 6 — JANGAN dikerjakan sekarang)

Backlog ide (bukan komitmen): advanced blocking, cloud sync, backup, akun, ekspor data.
Disimpan untuk mencegah **scope creep** (Risiko R34).

---

## Ringkasan

| Sprint | Fokus | Item |
|---|---|---|
| 0 | Fondasi | 6 |
| 1 | Smart Todo | 12 |
| 2 | Wellbeing data | 8 |
| 3 | Wellbeing UI + drift | 5 |
| 4 | Intervention | 7 |
| 5 | Focus | 7 |
| 6 | Future View + Domino | 6 |
| 7 | Insights | 7 |
| 8 | Hardening | 7 |
| **Total** | MVP Phase 1–5 | **~65 item** |

> Estimasi kasar solo dev: **~8–12 minggu** tergantung kecepatan & hasil spike.
> Sesuaikan tanpa mengubah ruang lingkup fase (`05-mvp-scope.md`).

---

## Referensi Silang

- Ruang lingkup fase → `05-mvp-scope.md`
- Acceptance Criteria → `01-prd.md` §10
- DoD & test → `11-testing-strategy.md`
- Risiko → `10-risk-register.md`
