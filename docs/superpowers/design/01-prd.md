# PRD — Anchor (Personal Focus Assistant)

> **Status:** Approved / Locked for Architecture · **Version:** 0.3
> **Tanggal:** 2026-10-07 (v0.2) · 2026-10-08 (v0.3)
> **Dokumen terkait:** `00-product-brief.md`
> **Catatan:** PRD ini mendefinisikan aturan perilaku (*behavioral rules*) yang telah dikunci.
> **v0.3:** mengaktifkan AI Personal Insight via **Gemini opt-in** (§10.8, §13.1); Phase 6
> item "Insight AI" dipromosikan. Perubahan ini melonggarkan prinsip "no egress" **hanya**
> untuk AI yang di-opt-in; default tetap OFF.

---

## 0. Executive Summary

Anchor adalah aplikasi Android personal (single-user) yang menghubungkan **niat**
(to-do list) dengan **perilaku nyata** (penggunaan perangkat). Ia menyadari ketika
user menjauh dari tugas yang sudah ia anggap penting, lalu memberi intervensi
kontekstual yang lembut, menunjukkan konsekuensi waktu dari penundaan, dan membantu
user masuk ke sesi fokus — semuanya tanpa menghakimi dan tanpa memaksa.

Anchor **bukan** to-do list biasa, **bukan** Digital Wellbeing biasa, dan **bukan**
app-blocker. Ia adalah *jembatan* antara keduanya.

Target MVP: **Phase 1–5 berjalan baik di perangkat nyata**. Backend tidak diperlukan.
Arsitektur local-first dan privacy-preserving.

---

## 1. Problem

Tiga masalah saling terkait:

1. **Niat ≠ Tindakan.** User tahu harus mengerjakan tugas, tapi tetap menunda.
2. **Distraksi tak terasa saat terjadi.** User baru sadar sudah scrolling 40 menit
   *setelah* kejadian, bukan saat mulai melenceng.
3. **Penundaan terasa abstrak.** Tidak ada gambaran konkret "kalau aku terus menunda,
   waktu bebas malam ini habis".

Setiap masalah punya *sudut pandang* berbeda (niat, perilaku, konsekuensi), dan
tidak ada satu alat yang menangani ketiganya sekaligus.

## 2. Opportunity

- **Smart Todo** tanpa kesadaran perilaku = mudah diabaikan.
- **Digital Wellbeing** tanpa konteks tugas = angka tanpa makna.
- **Menggabungkan keduanya** memungkinkan intervensi yang *benar-benar relevan*:
  bukan "kamu scroll lama", tapi "kamu scroll 40 menit, sementara Web Assignment
  yang High-priority belum selesai, dan kamu sudah rencanakan 2 jam untuk itu."

Pembeda utama: **intervensi kontekstual berbasis baseline personal**, bukan aturan universal.

## 3. Product Vision

> **Anchor** = asisten tenang yang hidup di dalam HP-mu. Ia mengerti apa yang kamu
> rencanakan, sadar ketika perilakumu menjauh darinya, menunjukkan ke mana pilihan
> itu membawa, dan dengan lembut membantumu kembali.

**Prinsip naratif:** Anchor adalah *"teman yang sadar ketika gue mulai menjauh dari
apa yang gue bilang penting"* — **bukan** *"aplikasi yang memaksa gue berhenti
menggunakan HP."*

## 4. Product Principles

1. **Awareness > Control**
2. **Guidance > Punishment**
3. **Consistency > Perfection**
4. **Autonomy > Forced Behavior**
5. **Personal Baseline > Universal Rules**

**Nada intervensi:** calm · supportive · concise · non-judgmental.

## 5. Goals

| # | Goal | Terukur lewat |
|---|---|---|
| G1 | User dapat membuat & mengelola task dengan lengkap | Task CRUD, subtask, deadline, priority |
| G2 | User dapat melihat pola penggunaan perangkat secara lokal | Wellbeing screen, screen time, app usage |
| G3 | Anchor mendeteksi drift berbasis baseline personal | Deviasi sesi > threshold |
| G4 | Anchor memberi intervensi kontekstual yang tidak spam | Max ~3/hari, ada cooldown, bisa dismiss |
| G5 | User dapat masuk & menyelesaikan sesi fokus | Focus session completeness |
| G6 | User dapat melihat konsekuensi waktu dari penundaan | Future View, Domino Effect |
| G7 | Anchor memberi insight personal dari data nyata | Daily Reflection + pattern |
| G8 | Aplikasi tetap berguna tanpa internet & tanpa izin khusus | Graceful degradation |
| G9 | Aplikasi hemat baterai & lancar di RAM 3–4 GB | Profiling, performa |

## 6. Non-Goals

- ❌ Bukan surveilans / parental control
- ❌ Bukan diagnosis medis / klaim kesehatan mental
- ❌ Bukan gamification berat
- ❌ Bukan AI chatbot
- ❌ Blocking **bukan** fondasi aplikasi
- ❌ Tidak ada backend di MVP
- ❌ Tidak mengirim raw usage ke cloud
- ❌ Bukan aplikasi untuk rilis publik (single-user, personal)

## 7. Target User

Pengguna tunggal: **pemilik aplikasi sendiri**. Ini bukan aplikasi multi-user,
tidak butuh onboarding kompleks, tidak terikat kebijakan Play Store rilis publik —
namun tetap menjaga kebersihan privasi & performa.

## 8. Persona

**Persona utama (satu-satunya untuk MVP):**

> **"Mahasiswa yang tahu tapi menunda."**
>
> Seorang mahasiswa dengan banyak tugas/proyek, sering berpindah antara coding,
> belajar, dan aktivitas lain. Ia **mengetahui** apa yang harus dikerjakan, tetapi
> kadang **menunda memulai**, lalu **terdistraksi** oleh scrolling/entertainment.

**Yang sengaja TIDAK dikarang** (akan dipelajari dari data nyata):
- Jam produktif spesifik
- App distraksi spesifik
- Durasi sesi tipikal
- Pola mingguan

Semua ini **configurable** dan **dipelajari dari data aktual**, bukan diasumsikan.

## 9. Core Loop (detail tahap)

| Tahap | Deskripsi | Di mana di UI | Phase |
|---|---|---|---|
| **PLAN** | User membuat task: judul, deadline, priority, estimasi durasi, kategori, subtask | Tasks | 1 |
| **OBSERVE** | Anchor mengumpulkan data penggunaan perangkat secara lokal | Wellbeing | 2 |
| **DETECT DRIFT** | Bandingkan perilaku dengan baseline personal per-app/per-sesi | (background) | 2–3 |
| **INTERVENTION** | Sampaikan secara kontekstual, hubungkan dengan task tertunda | Notification / In-app | 3 |
| **FOCUS** | Bantu user masuk sesi fokus pada task terpilih | Focus | 3 |
| **COMPLETE** | Task/sesi selesai, dicatat | Tasks / Focus | 1–3 |
| **LEARN** | Pola dikumpulkan dari riwayat nyata | Insights | 5 |
| **BESOK LEBIH BAIK** | Rekomendasi & refleksi makin relevan | Insights | 5 |

---

## 10. FEATURE SPECIFICATION

> Format tiap fitur: **Purpose · User Story · Functional Requirement · UX Behavior ·
> Edge Cases · Acceptance Criteria (Given/When/Then)**.
> Acceptance Criteria mengikuti filosofi §28 master prompt.

---

### 10.1 Smart Todo

**Purpose:** Tempat user mengekspresikan niat — apa yang penting, kapan, seberapa
penting, berapa lama.

**User Story:**
> Sebagai mahasiswa, aku ingin membuat task (termasuk task berulang harian/mingguan seperti jadwal kuliah)
> dengan deadline & prioritas, memecahnya jadi langkah kecil, dan melihat apa yang harus kukerjakan hari ini
> — supaya aku tahu harus mulai dari mana.

**Task Architecture (Definition vs Instance) [LOCKED v0.2]:**
Untuk mendukung recurring task tanpa mengacaukan riwayat & progress, model dipisahkan menjadi:
1. **Task Definition / Template:** Menyimpan konfigurasi dasar dan aturan keberulangan (`recurrenceRule`: `NONE / DAILY / WEEKLY`).
2. **Task Instance:** Subjek task konkret harian/mingguan yang dikerjakan user (`dueDate`, `status`, `completedAt`, `definitionId`).

**Task Instance Model (field awal — final di dokumen Database):**

| Field | Tipe | Nullable | Catatan |
|---|---|---|---|
| `id` | Long/UUID | no | PK Instance |
| `definitionId` | Long/UUID | yes | FK ke Task Definition (jika dari recurring) |
| `title` | String | no | Judul task |
| `description` | String | yes | Detail opsional |
| `deadline` | Epoch millis | yes | Tanggal/jam jatuh tempo |
| `priority` | Enum | no | `LOW / MEDIUM / HIGH / URGENT` (default `MEDIUM`) |
| `estimatedDurationMin` | Int | yes | Estimasi durasi (menit) |
| `category` | String/Enum | yes | Kategori user |
| `status` | Enum | no | `TODO / IN_PROGRESS / DONE / ARCHIVED` |
| `createdAt` | Epoch millis | no | Waktu dibuat |
| `completedAt` | Epoch millis | yes | Waktu selesai |
| `progressPercent` | Int | no | Dihitung dari subtask / manual (default 0) |
| `parentTaskId` | Long | yes | Untuk dependency/hierarki dasar |

**Subtask model:**

| Field | Tipe | Nullable |
|---|---|---|
| `id` | Long | no |
| `taskId` | Long (FK) | no |
| `title` | String | no |
| `isDone` | Boolean | no (default false) |
| `orderIndex` | Int | no |

**Functional Requirements:**
1. FR-T1: Buat/edit/hapus task dengan semua field di atas.
2. FR-T2: Set priority dari 4 level.
3. FR-T3: Set deadline/dueDate (tanggal & jam opsional).
4. FR-T4: Set estimasi durasi dalam menit.
5. FR-T5: Set kategori (dari daftar kategori, bisa custom).
6. FR-T6: Tambah/hapus/check/uncheck subtask, atur urutan.
7. FR-T7: Support Recurring Task: **Daily** dan **Weekly** aktif sejak Phase 1 via pemisahan Task Definition & Task Instance.
8. FR-T8: Tandai task selesai (status → DONE, catat `completedAt`).
9. FR-T9: Progress dihitung dari subtask jika ada, jika tidak = 0/100.
10. FR-T10: View: **All / Today / Upcoming / Completed**.
11. FR-T11: Overdue task handling via non-blocking UI (lihat Overdue Behavior).
12. FR-T12: Reschedule deadline (mencatat riwayat reschedule untuk insight).

**UX Behavior:**
- Task list padat & tenang; priority ditandai warna halus (bukan mencolok).
- Quick-add: satu field judul, sisa opsional.
- Empty state ramah, bukan menyalahkan ("No tasks yet — plan your day").

**Edge Cases:**
- Deadline di masa lalu saat dibuat → tetap boleh, langsung bertanda overdue.
- Estimated duration = 0/null → Future View memakai default domain heuristic.
- Subtask selesai semua → task boleh auto-suggest DONE (tidak paksa).
- Judul kosong → tidak bisa disimpan.

**Overdue Behavior (FR-T11) [LOCKED v0.2]:**
- **Non-blocking Card/Banner di UI:** Saat app dibuka, tampilkan card halus yang tenang:
  ```
  ┌──────────────────────────────┐
  │  3 tasks from yesterday      │
  │  are still waiting.          │
  │                              │
  │  [Review & Reschedule]       │
  └──────────────────────────────┘
  ```
- **Prinsip Autonomy:** Task overdue **TIDAK** otomatis dipindahkan menjadi Today. User memiliki kontrol penuh untuk memilih aksi: *Reschedule*, *Complete*, *Keep for Today*, atau *Dismiss*.
- **Tanpa Notifikasi Menghakimi:** Tidak ada modal pop-up mengganggu atau notifikasi yang menghujat penundaan.

**Reschedule (FR-T12):** User dapat memindah deadline; Anchor mencatat kejadian reschedule (untuk insight pola penundaan).

**Task States:**
```
TODO ──► IN_PROGRESS ──► DONE ──► ARCHIVED
  └──────────► ARCHIVED
```
- `TODO`: dibuat, belum dikerjakan
- `IN_PROGRESS`: sedang dikerjakan / ada di focus session
- `DONE`: selesai (`completedAt` diisi)
- `ARCHIVED`: disembunyikan tanpa dihapus

**Recurring Task Architecture [LOCKED v0.2]:**
- Supported rules: **DAILY** dan **WEEKLY**.
- Task Definition menerbitkan Task Instance baru saat hari/minggu jatuh tempo.
- Menyimpan riwayat penyelesaian tiap instance tanpa menimpa template utama.

**Acceptance Criteria:**
```
Given  user berada di Home tanpa task apa pun
When   user menekan tombol tambah & menyimpan task "Kerjakan tugas Web"
       dengan deadline hari ini 23:59, priority HIGH, estimasi 120 menit
Then   task tersimpan, tampil di "Today", dan log activity mencatat pembuatan

Given  user memiliki task dengan deadline yang sudah lewat
When   user membuka Today
Then   task tampil dengan tanda overdue yang halus (bukan merah menyala),
       dan tersedia aksi "Reschedule", tanpa notifikasi menghakimi

Given  user menandai task selesai
When   status berubah menjadi DONE
Then   completedAt dicatat, task muncul di "Completed", progress = 100%
```

---

### 10.2 Digital Wellbeing & Retention Policy

**Purpose:** Mengumpulkan & menampilkan pola penggunaan perangkat **secara lokal**
sebagai basis deteksi drift — tanpa mengirim data mentah ke mana pun.

**User Story:**
> Sebagai user, aku ingin tahu berapa lama aku memakai HP & app tertentu hari ini —
> supaya aku punya gambaran objektif perilakuku.

**Definisi:**
- **Session (`UsageSession`):** satu periode kontinu memakai sebuah app (start → end).
- **Daily Summary (`DailyUsageSummary`):** agregat harian (screen time, top apps, category breakdown, distraction ratio).
- **Weekly Summary (`WeeklyUsageSummary`):** agregat 7 hari untuk tren & baseline.

**Retention & Aggregation Policy [LOCKED v0.2]:**
```
Raw UsageSession (mentah)
       │
       │ Pruning otomatis (14–30 hari)
       ▼
DailyUsageSummary (agregat harian)
       │
       ▼
WeeklyUsageSummary (agregat mingguan)
```
- Raw `UsageSession` disimpan selama **14–30 hari** untuk analisis baseline mendalam, lalu **dihapus otomatis (pruning)**.
- Data dikonsolidasi menjadi `DailyUsageSummary` dan `WeeklyUsageSummary`.
- Keuntungan: Menghemat kapasitas DB, menjaga performa query, melindungi privasi, hemat baterai, dan mereduksi token AI.

**Sumber data:** `UsageStatsManager` (bukan Accessibility) untuk wellbeing murni.

**Keterbatasan Android (WAJIB dipahami & didokumentasikan di UI bila relevan):**
- `PACKAGE_USAGE_STATS` adalah **special permission** — user harus mengaktifkan manual di Settings.
- Sebagian OEM membatasi granularitas / tidak menyediakan data sesi detail.
- Data ter-agregasi periodik; `UsageStatsManager` tidak menyediakan event real-time.

**Functional Requirements:**
1. FR-W1: Cek status izin `PACKAGE_USAGE_STATS`; jika belum, tampilkan permission state yang jelas.
2. FR-W2: Query usage harian per app (durasi, jumlah sesi).
3. FR-W3: Agregasi lokal berjenjang → simpan ke Room (`UsageSession` → `DailyUsageSummary` → `WeeklyUsageSummary`).
4. FR-W4: Jalankan pruning otomatis data `UsageSession` yang berusia > 30 hari via WorkManager.
5. FR-W5: Hitung baseline personal per app (rata-rata durasi sesi, rolling window).
6. FR-W6: Tampilkan: total screen time, top apps, kategori, tren.
7. FR-W7: Semua angka bersumber dari Android API — **tidak** dari AI.

**UX Behavior:**
- Angka ditampilkan jujur, tanpa label baik/buruk.
- Jika permission belum ada → empty state jelas + tombol menuju Settings.
- Tren ditampilkan sederhana (sparkline/bar), bukan dashboard ramai.

**Edge Cases:**
- UsageStats kosong (OEM membatasi) → tampilkan pesan jujur, fitur task tetap jalan.
- Hari baru & timezone berubah → hitung ulang berdasarkan batas hari lokal.
- Data tidak tersedia > X hari → jangan tampilkan baseline yang menyesatkan.

**Acceptance Criteria:**
```
Given  user belum memberikan izin PACKAGE_USAGE_STATS
When   user membuka Wellbeing
Then   Anchor menampilkan panduan izin yang jelas, TIDAK crash,
       dan aplikasi tetap berfungsi sebagai Smart Todo

Given  izin usage diberikan & data tersedia 7 hari
When   user membuka Wellbeing
Then   Anchor menampilkan total screen time, top apps, dan tren mingguan
       yang dihitung dari data lokal (bukan AI, bukan fake data)
```

---

### 10.3 Drift Detection Architecture

**Purpose:** Menyadari ketika perilaku user menyimpang dari **baseline personal-nya**,
sebagai langkah sebelum intervensi.

**Pemisahan Arsitektur Deteksi Drift [LOCKED v0.2]:**
```
Phase 2: Periodic Drift Detection
UsageStatsManager ──► WorkManager (Periodic Task) ──► Historical / Baseline Analysis

Phase 3: Real-Time Contextual Detection
AccessibilityService (Eksperimental) ──► Instant Signal ──► Intervention Engine
```

1. **Phase 2 (Periodic Observation):** `UsageStatsManager` **tidak** memiliki Callback/Event real-time. Oleh karena itu, Phase 2 menggunakan `WorkManager` yang berjalan secara periodik (misal tiap 15 menit saat layar aktif) untuk mengevaluasi akumulasi durasi penggunaan.
2. **Phase 3 (Real-time Contextual Signal):** Menggunakan `AccessibilityService` (opsional/eksperimental) untuk mendeteksi momen transisi perpindahan aplikasi secara instan saat user mulai terdistraksi.

**Prinsip keras:**
- ❌ JANGAN pakai aturan universal ("lebih dari 30 menit = buruk").
- ✅ Pakai **baseline personal** (relatif terhadap kebiasaan user sendiri).
- ❌ JANGAN mendiagnosis ("You are addicted").
- ✅ Nyatakan secara netral ("You've been scrolling longer than usual").

**Functional Requirements:**
1. FR-D1: Hitung baseline per app (rata-rata & deviasi sesi) dari data `DailyUsageSummary` / `UsageSession`.
2. FR-D2: Hitung deviasi sesi aktif terhadap baseline personal.
3. FR-D3: Tandai "drift" jika deviasi melewati threshold **DAN** ada task berprioritas yang belum selesai.
4. FR-D4: Hasilkan sinyal drift (untuk dikonsumsi Intervention Engine).
5. FR-D5: Jangan diagnosis; hasilkan pesan netral.
6. FR-D6: Baseline butuh cukup data; jika belum → jangan klaim/eksekusi drift.

---

### 10.4 Contextual Intervention Engine

**Purpose:** Menyampaikan kesadaran (awareness) secara kontekstual, pada momen yang
tepat, dengan pilihan — bukan perintah.

**Aturan Kuota & Cooldown [LOCKED v0.2]:**
1. **Aturan Quota:** Maksimal **3 intervensi bermakna per hari**.
2. **Pemisahan Konsep:** *Detection Event ≠ Intervention Attempt*. Deteksi internal yang menahan notifikasi (karena cooldown/kuota habis) **TIDAK** mengurangi kuota harian.
3. **Penghitungan Kuota:**
   - User **Interacted** (misal menekan *Start Focus*) ➔ Mengurangi 1 kuota harian.
   - User **Dismissed** (misal swipe away notifikasi) ➔ Mengurangi 1 kuota harian.
4. **Aturan Cooldown:** Aksi **"Later"** memicu **cooldown selama 2 jam** untuk aplikasi/konteks distraksi yang sama.

**Levels:**

| Level | Nama | Contoh pesan | Sifat |
|---|---|---|---|
| 1 | **Gentle** | "You've been here for a while." | Netral, tanpa tugas |
| 2 | **Contextual** | "Your Web Assignment is still unfinished." | Hubung ke task |
| 3 | **Future View** | "Starting now gives you around 2 hours of free time tonight." | Konsekuensi waktu |
| 4 | **Focus** | "Start Focus." | Ajakan masuk fokus |

**Channels (prioritas):**
1. **Notification** (utama)
2. **In-app intervention** (kedua)
3. **Fullscreen** (hanya kondisi benar-benar relevan — jarang)

**Functional Requirements:**
1. FR-I1: Evaluasi sinyal drift terhadap sisa kuota harian (< 3) dan status cooldown.
2. FR-I2: Tentukan level intervensi yang sesuai (naik bertahap, tidak langsung Level 4).
3. FR-I3: Pilih channel paling sesuai (default notification).
4. FR-I4: Hormati "Later" → set cooldown 2 jam untuk konteks/app tersebut, jangan langsung intervensi lagi.
5. FR-I5: Catat setiap intervensi di DB (`InterventionLog`) dengan status `INTERACTED`, `DISMISSED`, atau `EXPIRED`.
6. FR-I6: Batasi total harian max 3 intervensi; reset tiap hari lokal baru.
7. FR-I7: Aksi utama: **[Start Focus]** / **[Later]** (+ opsi lihat Future View).

**Edge Cases:**
- Notification permission ditolak → fallback in-app saja.
- User sedang di dalam Anchor → jangan kirim notification untuk hal yang sama.
- Emergency / kondisi khusus user → hormati (tidak ada mekanisme paksa).

**Acceptance Criteria:**
```
Given  user punya high-priority task belum selesai
When   user scrolling jauh di atas personal baseline
Then   Anchor DAPAT memberi contextual intervention

Given  user menekan "Later"
When   dalam periode cooldown user kembali scrolling app yang sama
Then   Anchor TIDAK langsung mengirim intervensi lagi

Given  hari ini sudah ada 3 intervensi bermakna
When   sinyal drift baru muncul
Then   Anchor menahan intervensi tambahan (maksimum harian dihormati)
```

---

### 10.5 Focus Mode

**Purpose:** Membantu user benar-benar masuk & menyelesaikan sesi fokus pada satu task.

**Modes:**

| Mode | Perilaku |
|---|---|
| **Gentle** | Awareness + pengurangan notifikasi. **Tidak** block apa pun. |
| **Strict** | App distraksi terpilih dibatasi sementara. |
| **Custom** | User menentukan app mana yang dibatasi. |

**Active Focus — wajib menampilkan:**
- Nama task
- Current step (subtask aktif)
- Timer (sisa waktu)
- Progress
- Tombol **Pause** & **Finish**
- **Emergency exit WAJIB**

**Functional Requirements:**
1. FR-F1: Focus Setup → pilih task + mode (Gentle/Strict/Custom).
2. FR-F2: Mulai sesi fokus; timer berjalan (`FocusSession` dicatat).
3. FR-F3: Gentle: kurangi notifikasi (mode DND/priority-only) tanpa blocking.
4. FR-F4: Strict/Custom: gunakan `BlockingStrategy` terpilih (default gentle).
5. FR-F5: **Emergency exit** selalu tersedia & jelas.
6. FR-F6: Pause & Finish; Finish → tawarkan tandai task selesai / progress.
7. FR-F7: Foreground Service hanya saat Focus aktif (dengan notifikasi persisten).
8. FR-F8: Jika blocking gagal/tak tersedia → lanjut sebagai Gentle (graceful fallback).

**Edge Cases:**
- Accessibility mati saat Strict dipilih → jatuh ke Gentle + beri tahu.
- Telah kuberi izin tapi OS mematikan service → deteksi & fallback.
- Layar mati lama saat fokus → sesi tetap tercatat.
- Baterai lemah → tidak memaksa, beri tahu.

**Acceptance Criteria:**
```
Given  user memilih task "Web Assignment" & mode Gentle
When   sesi dimulai
Then   timer berjalan, task tampil, Pause/Finish tersedia,
       dan tidak ada app yang diblokir

Given  user memilih mode Strict dengan TikTok & Instagram
When   user mencoba membuka TikTok
Then   TikTok dibatasi, DAN emergency exit tetap bisa diakses

Given  sesi fokus selesai
When   user menekan Finish
Then   FocusSession dicatat dengan durasi, & user ditawari update progres task
```

---

### 10.6 Future View

**Purpose:** Menunjukkan konsekuensi waktu dari keputusan "mulai sekarang" vs "menunda"
— untuk meningkatkan awareness, **bukan** menakut-nakuti.

**Input (rule-based):**
- current time
- task duration (estimasi)
- deadline
- other scheduled tasks
- dependencies
- schedule

**Output dua skenario:**

```
IF I START NOW
15:10 → Start
15:10–17:10 → Focus
17:10 → Task completed
Evening → Free
```

```
IF I CONTINUE DELAYING
20:00 → Start
20:00–22:00 → Work
22:00+ → Less free time
23:59 → Deadline
```

**Aturan bahasa (WAJIB):**
- ✅ "Based on your current schedule..."
- ✅ "If you continue at your current pace..."
- ❌ JANGAN klaim AI tahu masa depan.
- ❌ JANGAN fear-based UX.

**Functional Requirements:**
1. FR-FV1: Hitung proyeksi berbasis aturan dari input di atas.
2. FR-FV2: Tampilkan dua skenario berdampingan.
3. FR-FV3: Gunakan disclaimer bahasa yang jujur (proyeksi, bukan ramalan).
4. FR-FV4: Sediakan aksi **[Start Focus]** dari layar ini.
5. FR-FV5: Jika data kurang (tak ada estimasi/deadline) → jelaskan, jangan mengarang.

**Edge Cases:**
- Tidak ada estimasi durasi / deadline → Gunakan domain heuristics (`DEFAULT_TASK_DURATION = 60m`, `DEFAULT_SAME_DAY_DEADLINE = 23:59`) dan MENANDAI asumsi dengan jelas di UI (*"Based on current schedule assumptions"*).
- Deadline malam & waktu sudah lewat → tunjukkan skenario realistis tanpa panik.
- Task tidak ada dependensi → Domino Effect kosong (bukan error).

**Acceptance Criteria:**
```
Given  task punya deadline 23:59, estimasi 2 jam, waktu sekarang 15:10
When   user membuka "See what happens"
Then   Anchor menampilkan dua skenario (mulai sekarang vs menunda)
       dengan disclaimer "based on your current schedule"

Given  task tidak punya estimasi durasi
When   user membuka Future View
Then   Anchor memakai default internal (heuristics) dan MENANDAI asumsi dengan jelas
       (bukan menyajikan angka seolah pasti)
```

---

### 10.7 Domino Effect

**Purpose:** Menunjukkan bagaimana satu task tertunda dapat berdampak pada task lain
yang bergantung padanya.

```
Web Assignment
      ↓
   Review
      ↓
   Submit
      ↓
 Presentation
```
> "1 task may affect 3 upcoming tasks."

**Aturan bahasa:** Jangan deterministik. Gunakan "may affect" / "could impact".

**Functional Requirements:**
1. FR-DE1: Model dependency antar-task (`TaskDependency`).
2. FR-DE2: Visualisasikan rantai dependency.
3. FR-DE3: Hitung & tampilkan jumlah task yang berpotensi terdampak.
4. FR-DE4: Bahasa estimatif ("may affect"), bukan kepastian.
5. FR-DE5: Ketuk node → navigasi ke task terkait.

**Dependency Model (dormant sejak Phase 1 — lihat §10.1):**
| Field | Tipe |
|---|---|
| `id` | Long |
| `fromTaskId` | Long (FK) |
| `toTaskId` | Long (FK) |
| `type` | Enum (BLOCKS / RELATED) |

**Edge Cases:**
- Siklus dependency (A→B→A) → deteksi & cegah.
- Task tanpa dependensi → tampilkan empty state jelas.
- Menghapus task yang jadi dependency → tawarkan pisahkan atau ubah.

**Acceptance Criteria:**
```
Given  task "Web Assignment" punya 3 task bergantung padanya
When   user membuka Domino Effect
Then   Anchor menampilkan rantai & pesan "1 task may affect 3 upcoming tasks"
       dengan bahasa estimatif (bukan pasti)

Given  user mencoba membuat dependency yang membentuk siklus
When   menyimpan
Then   Anchor mencegah siklus & menjelaskan kenapa
```

---

### 10.8 AI Personal Insight

**Purpose:** Menginterpretasikan data **agregat** untuk menghasilkan insight personal
yang didukung data — dengan fallback lokal.

**AI menganalisis:**
- completed tasks
- delayed tasks
- focus sessions
- screen time
- distraction sessions
- productive periods
- personal patterns

**Output terstruktur (schema):**
```json
{
  "summary": "...",
  "patterns": [],
  "risk_level": "low",
  "suggestions": [],
  "recommended_action": "start_focus"
}
```

**Aturan keras:**
- AI **tidak boleh** membuat klaim di luar data.
- AI **bukan** sumber kebenaran data penggunaan (sumber = Android).
- AI **optional** — wajib ada `RuleBasedInsightSource` sebagai fallback.
- Hanya **aggregated data** yang dikirim; **raw usage tidak**.
- AI **opt-in**: tidak ada permintaan (izin jaringan / kirim data) sebelum user
  menyalakan toggle AI. Default tetap OFF.
- API key **tidak** di-commit; dibaca dari `local.properties` (machine-specific).

**Functional Requirements:**
1. FR-AI1: Susun payload agregat (`DailyUsageSummary`, bukan raw) dari data lokal.
2. FR-AI2: Kirim ke Gemini **hanya jika** diaktifkan & tersedia.
3. FR-AI3: Terapkan cache & batching (mis. maks 1x/hari + on-demand).
4. FR-AI4: Fallback otomatis ke `RuleBasedInsightSource` bila gagal/offline/quota habis.
5. FR-AI5: Simpan hasil ke `Insight` (dengan sumber: AI atau rule-based).
6. FR-AI6: Tampilkan hanya insight yang didukung data (sembunyikan jika data kurang).
7. FR-AI7: Implementasi AI nyata menggunakan **Gemini REST API** (`generateContent`)
   dengan `GEMINI_API_KEY` dari `local.properties`→`BuildConfig`. Bila key kosong →
   binding otomatis jatuh ke rule-based (tidak ada request jaringan, tidak crash).
8. FR-AI8: Timeout/kegagalan jaringan tidak pernah memblokir UI — selalu `null` →
   fallback rule-based (tidak ada dialog error).

**Edge Cases:**
- Offline / API key kosong / quota habis → rule-based hasil tetap ada.
- Respons AI tidak sesuai schema → tolak & fallback.
- Data sangat sedikit → nyatakan keterbatasan, jangan mengarang pola.
- Rate limit (HTTP 429) → fallback rule-based tanpa retry agresif.
- Key ada tapi invalid/expired → fallback rule-based (log lokal, tanpa crash).

**Acceptance Criteria:**
```
Given  AI dinonaktifkan / offline
When   user membuka Insights
Then   Anchor menampilkan rule-based insight lokal, TANPA error

Given  AI aktif & data cukup
When   analisis harian dijalankan
Then   Anchor menyimpan insight berstruktur dari Gemini
       & menampilkan sumbernya dengan jujur

Given  payload berisi raw usage
When   hendak dikirim
Then   Anchor MENOLAK & hanya mengirim agregat
```

---

### 10.9 Daily Reflection

**Purpose:** Ringkasan harian yang menenangkan & mendukung, menutup satu hari.

**Contoh tampilan:**
```
YOUR DAY

Tasks
2 / 3 completed

Focus
1h 42m

Screen Time
5h 18m

Longest distraction
TikTok — 45m

────────────────

INSIGHT
"You completed difficult tasks earlier than usual today."

SUGGESTION
"Tomorrow, start your hardest task within the first productive window."
```

**Tone:** calm · supportive · concise · non-judgmental.

**Functional Requirements:**
1. FR-DR1: Hasilkan ringkasan sekali per hari (mis. sore/malam).
2. FR-DR2: Kumpulkan: tasks completed, focus time, screen time, longest distraction.
3. FR-DR3: Sertakan insight (AI jika ada, rule-based jika tidak).
4. FR-DR4: Sertakan satu suggestion realistis (jangan generik).
5. FR-DR5: Jangan menghakimi; fokus pada pola & langkah berikutnya.

**Edge Cases:**
- Hari tanpa task → tetap tampilkan ringkasan tanpa menyalahkan.
- Tidak ada data usage → sembunyikan bagian itu, jangan tampilkan nol menyesatkan.
- Insight tidak tersedia → tampilkan ringkasan saja.

**Acceptance Criteria:**
```
Given  user menyelesaikan 2 dari 3 task hari ini
When   Daily Reflection dihasilkan
Then   Anchor menampilkan ringkasan yang tenang & mendukung,
       dengan insight yang didukung data

Given  tidak ada data usage hari itu
When   Daily Reflection ditampilkan
Then   Anchor menyembunyikan bagian screen time (bukan menampilkan 0 menyesatkan)
```

---

### 10.10 Settings, Privacy & Permission Controls

**Purpose:** Memberi user kendali penuh atas tracking, AI, kategori, blocking, & data.

**Settings minimal:**
- Tracking permissions (status + pintasan Settings)
- Notification settings
- Focus rules
- Blocked apps
- Categories (+ override manual per app)
- Privacy & AI (on/off, apa yang dibagikan)
- Data retention (berapa lama disimpan + hapus data)

**Functional Requirements:**
1. FR-S1: Tampilkan status tiap izin + cara mengaktifkan.
2. FR-S2: Toggle AI on/off; jelaskan sederhana apa yang dikirim.
3. FR-S3: Kelola kategori & override per app.
4. FR-S4: Kelola daftar app yang diblokir.
5. FR-S5: Atur retensi data (pruning 14–30 hari) & tombol hapus semua data.
6. FR-S6: Semua default = privacy-preserving (tracking & AI off sampai di-opt-in).

**Acceptance Criteria:**
```
Given  user pertama kali membuka Anchor
When   onboarding muncul
Then   Anchor TIDAK memaksa semua izin; Smart Todo bisa dipakai langsung

Given  user menghapus semua data
When   dikonfirmasi
Then   semua data lokal terhapus (kecuali yang user pilih untuk dipertahankan)
```

---

## 11. MVP Scope per Phase

| Phase | Nama | Isi utama | Status |
|---|---|---|---|
| **1** | Smart Todo | Task Definition vs Instance, Daily/Weekly Recurring, Overdue Banner, Subtask, Priority, Today/Upcoming/Completed | MVP (Locked v0.2) |
| **2** | Digital Wellbeing | UsageStats, screen time, app usage, sesi, tren, baseline personal, pruning 14–30 hari, WorkManager Periodic Drift Check | MVP (Locked v0.2) |
| **3** | Intervention + Focus | Quota (max 3/day), 2h Cooldown, Dismiss tracking, Focus Setup/Session, Gentle/Strict/Custom, Accessibility Experiment | MVP (Locked v0.2) |
| **4** | Future View + Domino | "If I start now" vs "If I delay", consequence, dependency UI, Heuristic defaults (60m/23:59) | MVP (Locked v0.2) |
| **5** | AI Personal Insight | Daily reflection, patterns, recommendations, rule-based fallback, aggregated Gemini insights | MVP (Locked v0.2) |
| **6** | Advanced Blocking + Sync | Blocking lanjutan, cloud sync, backup, akun (opsional) | Post-MVP |

**Definition of Done utama:** Phase 1–5 berjalan baik di real device.

---

## 12. Non-Functional Requirements & Benchmark Targets

| Kategori | Target Requirement [LOCKED v0.2] |
|---|---|
| **Performance (RAM)** | Target penggunaan RAM aktif **< 150 MB** pada skenario penggunaan normal |
| **Battery Consumption** | Target penggunaan baterai background **≤ 2–3% tambahan per 24 jam** pada kondisi uji terstandar |
| **Offline First** | Berfungsi penuh 100% tanpa jaringan internet; AI bersifat opsional |
| **Localization** | Semua teks via string resources; default English; siap Bahasa Indonesia |
| **Privacy & Storage** | Raw usage tidak meninggalkan device; Pruning otomatis `UsageSession` berusia > 30 hari |
| **Lifecycle** | Semua background work lifecycle-aware; izin selalu grace |
| **Reliability** | Tidak crash saat izin ditolak / OEM membatasi / migrasi DB gagal |

---

## 13. Data & Privacy Model (ringkas — detail di dokumen terpisah)

```
Raw Usage Data (device-only)
      ↓
Local Aggregation (DailySummary & WeeklySummary)
      ↓
[Pruned 14-30 days] (Raw deleted)
      ↓
[Aggregated, structured] ──► AI (opsional)
```

- **Local-only:** raw usage, session, snapshot.
- **Aggregated:** statistik harian/mingguan.
- **AI-shared:** hanya agregat terstruktur, opsional.
- Default: **privacy-preserving**.

### 13.1 AI Egress (aktif via Gemini — keputusan v0.3)

Saat user **menyalakan** toggle AI, Anchor mengizinkan satu jalur egress:

| Aspek | Ketentuan |
|---|---|
| Tujuan | Google Gemini API (`generativelanguage.googleapis.com`) |
| Data terkirim | **Hanya** `InsightPayload` agregat (tasks count, focus ms, screen ms, kategori distraksi). **Tidak ada** raw session, judul task, timestamp per-sesi, atau identitas device. |
| Gerbang | Toggle `aiEnabled` (default **OFF**) + key tersedia. Keduanya harus true. |
| Izin | `INTERNET` (+ `ACCESS_NETWORK_STATE`) hanya terpakai saat AI ON. Tanpa AI ON, tidak ada request. |
| Kegagalan | Timeout/offline/429/invalid key/respons tidak sesuai schema → fallback rule-based, tanpa crash & tanpa dialog. |
| Key | Disimpan di `local.properties` (git-ignored) → `BuildConfig.GEMINI_API_KEY`. **Tidak** di-commit. |
| Transparansi | UI menandai sumber: "Generated by Anchor AI…" vs "Generated locally on your device". |

> Ini melonggarkan prinsip "tanpa backend / tanpa egress" **hanya untuk AI opt-in**.
> Semua fase lain tetap 100% offline. Bila AI OFF (default), egress = nol.

---

## 14. Permission Model (ringkas — detail di dokumen terpisah)

| Permission | Kapan diminta | Jika ditolak |
|---|---|---|
| `PACKAGE_USAGE_STATS` | Saat user ingin Wellbeing | App tetap jalan sebagai Smart Todo |
| `POST_NOTIFICATIONS` | Sebelum intervensi pertama | Fallback in-app |
| Accessibility Service | Saat user memilih Strict/Custom | Fallback ke Gentle |
| Foreground Service | Hanya saat Focus aktif | Focus mode terbatas |
| Alarm permissions | Jika reminder kritis dipakai | Reminder fleksibel |

**First launch tidak memaksa apa pun.** Smart Todo jalan tanpa izin apa pun.

---

## 15. Risiko Teknis & Mitigasi (ringkas — register lengkap di dokumen terpisah)

| Risk | Level | Mitigasi |
|---|---|---|
| Background execution limits (Doze/Standby) | Tinggi | WorkManager, hindari polling |
| UsageStats granularitas terbatas | Tinggi | Spike A; degradasi bertahap |
| Accessibility rapuh & boros | Tinggi | Eksperimental; fallback ke notifikasi |
| Notification permission ditolak | Sedang | Fallback in-app |
| Exact alarm restrictions | Sedang | Alarm fleksibel / WorkManager |
| OEM killing background | Sedang–Tinggi | Panduan battery optimization; uji device nyata |
| AI cost/rate-limit/offline | Sedang | Cache, batch, fallback rule-based |
| Compose performance | Sedang | Hindari animasi berat, stable keys |
| Room migration | Sedang | Skema forward-compatible, versioning |

---

## 16. Asumsi & Open Questions (LOCKED v0.3)

**Keputusan yang telah dikunci (Locked Decisions):**
- **OQ1 (Recurring Task):** Phase 1 mendukung Daily & Weekly dengan pemisahan `TaskDefinition` vs `TaskInstance`.
- **OQ3 (Overdue Behavior):** Non-blocking UI Card/Banner, menjaga otonomi user (*Awareness > Control*), tanpa auto-move ke Today.
- **Drift Architecture:** Phase 2 menggunakan `WorkManager` (periodic), Phase 3 menggunakan `AccessibilityService` (real-time/eksperimental).
- **Data Retention:** `UsageSession` mentah di-pruning setelah 14–30 hari; diagregasi ke `DailyUsageSummary`.
- **Intervention Rules:** Max 3 intervensi/hari, action *Interacted* & *Dismissed* menghabiskan kuota, *Later* memicu cooldown 2 jam per konteks.
- **Future View:** Default duration 60m & deadline 23:59 sebagai heuristic domain logic.
- **Performance Targets:** Baterai ≤ 2–3%/24h dan RAM < 150MB ditetapkan sebagai *benchmark targets*.
- **OQ-AI (v0.3):** AI Personal Insight **aktif** memakai **Gemini REST** (`generateContent`),
  opt-in (default OFF), key dari `local.properties`→`BuildConfig.GEMINI_API_KEY`,
  wajib fallback rule-based, hanya agregat yang dikirim (§13.1). Ini **promosi** Phase 6
  "Insight AI" ke dalam scope aktif.

---

## 17. Metrik Keberhasilan (personal, kualitatif)

Prioritaskan (bukan vanity metrics):
- Task completion & delay
- Focus session completion
- Intervention usefulness (apakah di-anggap membantu)
- Intervention dismissal
- Distraction recovery (kembali fokus setelah intervensi)
- Consistency
- User-reported usefulness (dirimu sendiri)

⚠️ **Jangan simpulkan** "screen time turun = lebih produktif" tanpa konteks.

---

## 18. Roadmap Ringkas & Ketergantungan

```
Phase 1 (Smart Todo)
   └──► Phase 2 (Wellbeing) ── butuh Phase 1 (task ada)
           └──► Phase 3 (Intervention+Focus) ── butuh drift detection (Phase 2)
                   └──► Phase 4 (Future View+Domino) ── butuh task+dependency+schedule
                           └──► Phase 5 (AI Insight) ── butuh data dari semua phase
                                   └──► Phase 6 (post-MVP: advanced blocking + sync)
```

**Eksperimen penentu:** Spike A (UsageStats) & Spike B (Accessibility) dijalankan
sebelum mengunci desain Phase 2–3.

---

## 19. Lampiran: Glosarium

| Istilah | Arti |
|---|---|
| **Drift** | Penyimpangan perilaku dari baseline personal |
| **Baseline** | Statistik kebiasaan personal user (dari data nyata) |
| **Intervensi** | Pesan kontekstual lembut saat drift terdeteksi |
| **Future View** | Proyeksi dua skenario waktu (mulai vs menunda) |
| **Domino Effect** | Dampak penundaan satu task ke task yang bergantung |
| **Aggregated data** | Statistik terolah — bukan data mentah |
| **Spike** | Eksperimen teknis kecil untuk menjawab pertanyaan kelayakan |

---

> **CATATAN AKHIR DOKUMEN:**
> PRD v0.2 ini telah **disetujui (Approved / Locked for Architecture)**.
> Aturan perilaku (*behavioral rules*) di atas digunakan sebagai fondasi tetap untuk menyusun
> dokumentasi teknis lanjutan (`02-architecture.md`, `03-database-schema.md`, `04-intervention-engine.md`).

