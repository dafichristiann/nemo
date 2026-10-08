# MVP Scope & Delivery Plan — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 (Locked) · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` (§11, §18), `02-architecture.md`, `03-database-schema.md`
> **Catatan:** Ruang lingkup & fase dikunci PRD. Dokumen ini menambahkan rincian
> deliverable, Definition of Done, dan urutan kerja teknis.

---

## 0. Definisi MVP

> **MVP = Phase 1–5 berjalan baik di perangkat nyata.**
> Local-first · tanpa backend · privacy-preserving. (PRD §0, §11)

**Definition of Done utama:** Phase 1–5 berfungsi end-to-end di real device,
memenuhi target non-fungsional (PRD §12), tanpa crash pada kondisi izin ditolak.

Non-MVP (Phase 6) sengaja **tidak** dikerjakan: advanced blocking, cloud sync,
backup, akun.

---

## 1. Ringkasan Fase

| Phase | Nama | Inti | Status |
|---|---|---|---|
| **1** | Smart Todo | CRUD, recurring, subtask, priority, overdue banner | MVP (Locked) |
| **2** | Digital Wellbeing | UsageStats lokal, screen time, baseline, pruning | MVP (Locked) |
| **3** | Intervention + Focus | Intervensi (kuota/cooldown), Focus Mode | MVP (Locked) |
| **4** | Future View + Domino | Konsekuensi waktu, dependency | MVP (Locked) |
| **5** | AI Personal Insight | Daily reflection, pola, fallback rule-based | MVP (Locked) |
| **6** | Advanced Blocking + Sync | Blocking lanjutan, cloud, akun | **Post-MVP** |

---

## 2. Ketergantungan Antar-Fase (PRD §18)

```
Phase 1 (Smart Todo)
   └──► Phase 2 (Wellbeing) ── butuh task ada
           └──► Phase 3 (Intervention+Focus) ── butuh drift detection (Phase 2)
                   └──► Phase 4 (Future View+Domino) ── butuh task + dependency + schedule
                           └──► Phase 5 (AI Insight) ── butuh data dari semua phase
                                   └──► Phase 6 (post-MVP)
```

**Spike penentu** (dijalankan sebelum mengunci Phase 2–3):
- **Spike A — UsageStats:** uji granularitas `UsageStatsManager` di device target.
- **Spike B — Accessibility:** uji kelayakan & baterai `AccessibilityService`.

> Hasil spike dapat menurunkan ekspektasi fitur (degradasi bertahap), **bukan** mengubah
> prinsip perilaku.

---

## 3. Rincian per Fase

### Phase 1 — Smart Todo

**Deliverable:**
- Model `TaskDefinition` vs `TaskInstance` + `Subtask` + `RescheduleEvent`.
- Task CRUD lengkap (judul, deskripsi, deadline, priority, estimasi, kategori, subtask).
- Recurring **DAILY** & **WEEKLY** (definition menerbitkan instance baru).
- View: **All / Today / Upcoming / Completed**.
- **Overdue Banner** (non-blocking, `Reschedule / Complete / Keep for Today / Dismiss`).
- Progress dari subtask (auto-hitung) atau manual.
- Settings dasar + onboarding non-memaksa (layar 00 & 00b).

**DoD:** Acceptance Criteria PRD §10.1 lulus; overdue tidak auto-move; tidak crash pada
judul kosong; reschedule tercatat.

### Phase 2 — Digital Wellbeing

**Deliverable:**
- Cek & panduan izin `PACKAGE_USAGE_STATS`.
- `UsageCollectWorker` → `UsageSession` (raw).
- `AggregationWorker` → `DailyUsageSummary` & `WeeklyUsageSummary`.
- `PruningWorker` → hapus raw > 30 hari.
- Baseline personal per app (rolling window).
- UI Wellbeing: total screen time, top apps, kategori, tren (bar/sparkline sederhana).
- `DriftCheckWorker` (periodic) — evaluasi awal.

**DoD:** AC PRD §10.2 lulus; tanpa izin → empty state jelas, app tetap jadi Smart Todo;
angka semua dari Android (bukan AI/fake).

### Phase 3 — Intervention + Focus

**Deliverable:**
- `DriftDetector` + `InterventionEngine` (lihat `04-intervention-engine.md`).
- Kuota **max 3/hari**, cooldown **2 jam** ("Later"), pemisahan Detection vs Attempt.
- `InterventionLog` (INTERACTED/DISMISSED/EXPIRED).
- Channel: Notification (utama) → In-app → Fullscreen (jarang) + fallback.
- Focus Setup (task + mode Gentle/Strict/Custom + durasi).
- Focus Active (nama task, current step, timer, progress, Pause/Finish, **emergency exit**).
- Foreground Service hanya saat Focus aktif + notifikasi persisten.
- Blocking eksperimental (Accessibility) dengan **graceful fallback ke Gentle**.

**DoD:** AC PRD §10.4 & §10.5 lulus; emergency exit selalu tersedia; blocking gagal → Gentle.

### Phase 4 — Future View + Domino

**Deliverable:**
- `ComputeFutureViewUseCase`: dua skenario (mulai sekarang vs menunda), rule-based.
- Disclaimer jujur ("based on your current schedule") + tandai asumsi heuristic.
- Heuristic default: `DEFAULT_TASK_DURATION = 60m`, `DEFAULT_SAME_DAY_DEADLINE = 23:59`.
- `TaskDependency` (BLOCKS/RELATED) + visualisasi rantai Domino.
- Anti-siklus dependency.
- Aksi **[Start Focus]** dari Future View.

**DoD:** AC PRD §10.6 & §10.7 lulus; bahasa "may affect" (bukan pasti); task tanpa estimasi
tetap jalan dengan asumsi ditandai.

### Phase 5 — AI Personal Insight

**Deliverable:**
- `GenerateInsightUseCase` (AI opsional) + `RuleBasedInsightSource` (wajib fallback).
- Payload **hanya agregat** (tolak raw usage) — audit sebelum kirim.
- Cache + batch (maks 1×/hari + on-demand).
- Daily Reflection (tasks, focus, screen time, longest distraction, insight, suggestion).
- Simpan `Insight` dengan `source = AI | RULE_BASED`.

**DoD:** AC PRD §10.8 & §10.9 lulus; offline/nonaktif → rule-based tanpa error;
data usage kosong → sembunyikan bagian (bukan nol menyesatkan).

### Phase 6 — Post-MVP (Tidak dikerjakan sekarang)

Blocking lanjutan, cloud sync, backup, akun opsional. Ditulis sebagai backlog saja.

---

## 4. Urutan Kerja Teknis (saran)

```
Sprint 0: Setup project, DI, tema, Room v1 (task tables), onboarding shell
Sprint 1: Phase 1 — Smart Todo end-to-end + overdue banner + settings dasar
Sprint 2: Spike A (UsageStats) + Phase 2 data pipeline (collect/aggregate/prune)
Sprint 3: Phase 2 UI (Wellbeing) + baseline
Sprint 4: Spike B (Accessibility) + Phase 3 DriftDetector + InterventionEngine
Sprint 5: Phase 3 Focus Setup + Focus Active + Foreground Service
Sprint 6: Phase 4 Future View + Domino
Sprint 7: Phase 5 Insight (rule-based first, lalu AI opsional)
Sprint 8: Hardening — performa, baterai, edge cases, benchmark PRD §12
```

> Integrasi UI mengacu pada `design/07-screens.md` (inventaris layar) dan
> `design/06-design-system.md` (token & komponen).

---

## 5. Checklist DoD Global (per fase harus lulus)

- [ ] Acceptance Criteria fase (PRD) lulus.
- [ ] Tidak crash saat izin ditolak / OEM membatasi / migrasi DB gagal.
- [ ] Berfungsi offline (tanpa internet).
- [ ] Tidak ada pengiriman raw usage ke luar device.
- [ ] Teks lewat string resources (default English, siap Bahasa Indonesia).
- [ ] Nada UI: calm · supportive · concise · non-judgmental.
- [ ] Uji di minimal 1 device low-RAM (3–4 GB).

---

## 6. Target Non-Fungsional (PRD §12)

| Kategori | Target |
|---|---|
| RAM aktif | **< 150 MB** |
| Baterai background | **≤ 2–3% / 24 jam** |
| Offline | 100% berfungsi; AI opsional |
| Lokalisasi | via string resources; default English |
| Privasi | raw usage tak keluar device; pruning > 30 hari |
| Lifecycle | background work lifecycle-aware; izin selalu grace |
| Reliabilitas | tak crash saat izin ditolak / OEM batasi / migrasi gagal |

---

## 7. Yang Secara Sengaja Di Luar Scope MVP

- Backend / cloud apa pun
- Akun / login / multi-user
- Blocking sebagai fondasi (hanya opsional di Focus Strict)
- Kesehatan mental / klaim medis
- Gamification berat
- AI chatbot
- Kirim raw usage ke cloud

---

## 8. Referensi Silang

- Ruang lingkup & fase (sumber) → `01-prd.md` §11, §18
- Arsitektur & worker → `02-architecture.md`
- Data & retention → `03-database-schema.md` §10
- Drift & intervensi → `04-intervention-engine.md`

---

> **CATATAN AKHIR:** Urutan sprint di §4 bersifat **saran**; yang **terkunci** adalah isi
> fase (§3) dan ketergantungan (§2) dari PRD. Bila ada perubahan ruang lingkup MVP,
> ubah PRD terlebih dahulu.
