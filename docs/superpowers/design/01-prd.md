# PRD — Anchor (Personal Focus Assistant)

> **Status:** Draft untuk review · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `00-product-brief.md`
> **Catatan:** PRD ini fokus pada *apa* dan *mengapa*. Struktur teknis detail
> (DB schema, API spec, AI architecture, Android architecture) ada di dokumen
> terpisah yang mengalir dari PRD ini.

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
> Sebagai mahasiswa, aku ingin membuat task dengan deadline & prioritas, memecahnya
> jadi langkah kecil, dan melihat apa yang harus kukerjakan hari ini — supaya aku
> tahu harus mulai dari mana.

**Task Model (field awal — final di dokumen Database):**

| Field | Tipe | Nullable | Catatan |
|---|---|---|---|
| `id` | Long/UUID | no | PK |
| `title` | String | no | Judul task |
| `description` | String | yes | Detail opsional |
| `deadline` | Epoch millis | yes | Jika relevant |
| `priority` | Enum | no | `LOW / MEDIUM / HIGH / URGENT` (default `MEDIUM`) |
| `estimatedDurationMin` | Int | yes | Estimasi durasi (menit) |
| `category` | String/Enum | yes | Kategori user |
| `status` | Enum | no | `TODO / IN_PROGRESS / DONE / ARCHIVED` |
| `createdAt` | Epoch millis | no | Waktu dibuat |
| `completedAt` | Epoch millis | yes | Waktu selesai |
| `progressPercent` | Int | no | Dihitung dari subtask / manual (default 0) |
| `recurrenceRule` | String | yes | `[DECISION REQUIRED]` lihat §10.1.9 |
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
3. FR-T3: Set deadline (tanggal & jam opsional).
4. FR-T4: Set estimasi durasi dalam menit.
5. FR-T5: Set kategori (dari daftar kategori, bisa custom).
6. FR-T6: Tambah/hapus/check/uncheck subtask, atur urutan.
7. FR-T7: Tandai task selesai (status → DONE, catat `completedAt`).
8. FR-T8: Progress dihitung dari subtask jika ada, jika tidak = 0/100.
9. FR-T9: View: **All / Today / Upcoming / Completed**.
10. FR-T10: Overdue task ditandai (lihat 10.1.6).
11. FR-T11: Reschedule deadline (lihat 10.1.7).
12. FR-T12 (dormant): Model dependency disiapkan, UI di Phase 4.

**UX Behavior:**
- Task list padat & tenang; priority ditandai warna halus (bukan mencolok).
- Quick-add: satu field judul, sisa opsional.
- Empty state ramah, bukan menyalahkan ("No tasks yet — plan your day").

**Edge Cases:**
- Deadline di masa lalu saat dibuat → tetap boleh, langsung bertanda overdue.
- Estimated duration = 0/null → Future View memakai default asumsi internal.
- Subtask selesai semua → task boleh auto-suggest DONE (tidak paksa).
- Judul kosong → tidak bisa disimpan.
- Task dengan recurring `[OPEN]` → lihat 10.1.9.

**Overdue Behavior (FR-T10):** `[DECISION REQUIRED — OQ3]`
> Usulan default: **tenang, tidak menghakimi.** Tandai visual halus + sediakan
> aksi "Reschedule". **Tidak** mengirim notifikasi "kamu gagal".

**Reschedule (FR-T11):** User dapat memindah deadline; Anchor mencatat kejadian
reschedule (untuk insight pola penundaan).

**Task States:**
```
TODO ──► IN_PROGRESS ──► DONE ──► ARCHIVED
  └──────────► ARCHIVED
```
- `TODO`: dibuat, belum dikerjakan
- `IN_PROGRESS`: sedang dikerjakan / ada di focus session
- `DONE`: selesai (`completedAt` diisi)
- `ARCHIVED`: disembunyikan tanpa dihapus

**Recurring Task (FR-T12/T7):** `[DECISION REQUIRED — OQ1/A3]`
> Usulan: **model data `recurrenceRule` disiapkan di Phase 1 (dormant), mesin
> eksekusi (spawning task berulang) diimplementasi di phase lanjut.** Menghindari
> migrasi besar & bug dini. Alternatif: recurring sederhana (harian/mingguan) di
> Phase 1.

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

### 10.2 Digital Wellbeing

**Purpose:** Mengumpulkan & menampilkan pola penggunaan perangkat **secara lokal**
sebagai basis deteksi drift — tanpa mengirim data mentah ke mana pun.

**User Story:**
> Sebagai user, aku ingin tahu berapa lama aku memakai HP & app tertentu hari ini —
> supaya aku punya gambaran objektif perilakuku.

**Definisi:**
- **Session:** satu periode kontinu memakai sebuah app (start → end).
- **Screen time:** total durasi device aktif dalam periode.
- **App usage:** total durasi per app.
- **Daily summary:** agregat per hari.
- **Weekly trend:** agregat per 7 hari.
- **Category:** pengelompokan app (lihat 10.2.x).
- **Baseline:** statistik personal (mis. rata-rata durasi sesi per app).

**Sumber data:** `UsageStatsManager` (bukan Accessibility) untuk wellbeing murni.

**Keterbatasan Android (WAJIB dipahami & didokumentasikan di UI bila relevan):**
- `PACKAGE_USAGE_STATS` adalah **special permission** — user harus mengaktifkan manual di Settings.
- Sebagian OEM membatasi granularitas / tidak menyediakan data sesi detail.
- Data bisa ter-sampling; akurasi **tidak dijamin** → `[EXPERIMENT REQUIRED]` Spike A.
- Tidak dapat diandalkan untuk realtime; gunakan agregasi periodik.

**Functional Requirements:**
1. FR-W1: Cek status izin `PACKAGE_USAGE_STATS`; jika belum, tampilkan permission state yang jelas.
2. FR-W2: Query usage harian per app (durasi, jumlah sesi).
3. FR-W3: Query usage 7 hari terakhir untuk tren.
4. FR-W4: Agregasi lokal → simpan ke Room (`UsageSnapshot`, `UsageSession`).
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

Given  device/OEM tidak menyediakan data sesi detail
When   user membuka Wellbeing
Then   Anchor menampilkan apa yang tersedia & menjelaskan keterbatasannya
       dengan jujur, tanpa mengarang angka
```

---

### 10.3 Drift Detection

**Purpose:** Menyadari ketika perilaku user menyimpang dari **baseline personal-nya**,
sebagai langkah sebelum intervensi.

**Prinsip keras:**
- ❌ JANGAN pakai aturan universal ("lebih dari 30 menit = buruk").
- ✅ Pakai **baseline personal** (relatif terhadap kebiasaan user sendiri).
- ❌ JANGAN mendiagnosis ("You are addicted").
- ✅ Nyatakan secara netral ("You've been scrolling longer than usual").

**Contoh (persis dari brief):**
```
Baseline: rata-rata sesi TikTok user = 18 menit
Sesi saat ini: 47 menit
Deviasi: +161%
Output: "You've been scrolling longer than usual."
```

**Functional Requirements:**
1. FR-D1: Hitung baseline per app (rata-rata & deviasi sesi) dari data nyata.
2. FR-D2: Hitung deviasi sesi aktif terhadap baseline.
3. FR-D3: Tandai "drift" jika deviasi melewati threshold **DAN** memenuhi syarat lain
   (mis. ada task berprioritas belum selesai) — lihat `[ASSUMPTION] D4`.
4. FR-D4: Hasilkan sinyal drift (untuk dikonsumsi Intervention Engine).
5. FR-D5: Jangan diagnosis; hasilkan pesan netral.
6. FR-D6: Baseline butuh cukup data; jika belum → jangan klaim apa pun.

**Threshold:** `[DECISION REQUIRED]` — usulan awal `+100%` atau P95 historis user,
dikonfigurasi & dikalibrasi setelah Spike A + pemakaian nyata. **Jangan dikunci sekarang.**

**Edge Cases:**
- Baseline belum cukup data (< N hari) → tidak ada drift, tidak ada intervensi.
- App baru (belum pernah dipakai) → tidak ada baseline → jangan intervensi dulu.
- Sesi sangat singkat → bukan drift.
- User memang sedang sengaja santai → "Later" dihormati (lihat 10.4).

**Acceptance Criteria:**
```
Given  baseline sesi TikTok user = 18 menit (≥ window data terpenuhi)
When   user membuka TikTok selama 47 menit
Then   Anchor menghasilkan sinyal drift dengan deviasi positif signifikan,
       TANPA kata diagnostik seperti "addicted"

Given  data baseline belum cukup
When   user memakai app apa pun
Then   Anchor TIDAK menghasilkan sinyal drift (mencegah klaim tanpa dasar)
```

---

### 10.4 Contextual Intervention

**Purpose:** Menyampaikan kesadaran (awareness) secara kontekstual, pada momen yang
tepat, dengan pilihan — bukan perintah.

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

**Batas & aturan:**
- Max **~3 intervensi bermakna/hari**.
- **Cooldown** wajib setelah user memilih "Later".
- Harus **bisa dismiss** dengan mudah.
- **JANGAN spam.**
- Trigger **berbasis konteks**, bukan timer semata.

**Contoh baik:** *"You've been scrolling for 40 minutes, while your high-priority
task is still unfinished."*
**Contoh buruk:** *"STOP USING YOUR PHONE!"*

**Functional Requirements:**
1. FR-I1: Terima sinyal drift → evaluasi relevansi (ada task tertunda? sudah < 3 hari ini?).
2. FR-I2: Tentukan level intervensi yang sesuai (naik bertahap, tidak langsung Level 4).
3. FR-I3: Pilih channel paling sesuai (default notification).
4. FR-I4: Hormati "Later" → set cooldown, jangan langsung intervensi lagi.
5. FR-I5: Catat setiap intervensi di DB (`Intervention`) untuk metrik & insight.
6. FR-I6: Batasi total harian; reset tiap hari lokal baru.
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
- Tidak ada estimasi durasi → pakai default & tandai sebagai asumsi.
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
Then   Anchor memakai default internal dan MENANDAI asumsi dengan jelas
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

**Functional Requirements:**
1. FR-AI1: Susun payload agregat (bukan raw) dari data lokal.
2. FR-AI2: Kirim ke Gemini **hanya jika** diaktifkan & tersedia.
3. FR-AI3: Terapkan cache & batching (mis. maks 1x/hari + on-demand).
4. FR-AI4: Fallback otomatis ke `RuleBasedInsightSource` bila gagal/offline/quota habis.
5. FR-AI5: Simpan hasil ke `Insight` (dengan sumber: AI atau rule-based).
6. FR-AI6: Tampilkan hanya insight yang didukung data (sembunyikan jika data kurang).

**Edge Cases:**
- Offline / API key kosong / quota habis → rule-based hasil tetap ada.
- Respons AI tidak sesuai schema → tolak & fallback.
- Data sangat sedikit → nyatakan keterbatasan, jangan mengarang pola.

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
5. FR-S5: Atur retensi data & tombol hapus semua data.
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
| **1** | Smart Todo | Task CRUD, deadline, priority, estimasi, kategori, subtask, status, Today/Upcoming/Completed, model data dependency & recurring (dormant) | MVP |
| **2** | Digital Wellbeing | UsageStats, screen time, app usage, sesi, tren, baseline personal, kategori | MVP |
| **3** | Intervention + Focus | Deteksi drift, contextual notification, Focus Setup/Session, Gentle/Strict/Custom, eksperimen blocking, emergency exit | MVP |
| **4** | Future View + Domino | "If I start now" vs "If I delay", consequence, dependency UI, schedule impact | MVP |
| **5** | AI Personal Insight | Daily reflection, patterns, recommendations, rule-based fallback | MVP |
| **6** | Advanced Blocking + Sync | Blocking lanjutan, cloud sync, backup, akun (opsional) | Post-MVP |

**Definition of Done utama:** Phase 1–5 berjalan baik di real device.

---

## 12. Non-Functional Requirements

| Kategori | Requirement |
|---|---|
| **Performance** | Lancar di RAM 3–4 GB; hindari recomposition berlebih; animasi ringan |
| **Battery** | Tidak polling terus-menerus; agregasi batch; foreground service hanya saat Focus |
| **Offline** | Berfungsi penuh tanpa internet; AI opsional; rule-based selalu ada |
| **Localization** | Semua teks via string resources; default English; siap Bahasa Indonesia |
| **Privacy** | Raw usage tidak meninggalkan device; AI hanya dapat agregat; default opt-in |
| **Lifecycle** | Semua background work lifecycle-aware; izin selalu grace |
| **Reliability** | Tidak crash saat izin ditolak / OEM membatasi / migrasi DB gagal |

---

## 13. Data & Privacy Model (ringkas — detail di dokumen terpisah)

```
Raw Usage Data (device-only)
      ↓
Local Aggregation
      ↓
Daily / Session Statistics (local DB)
      ↓
[Aggregated, structured] ──► AI (opsional)
```

- **Local-only:** raw usage, session, snapshot.
- **Aggregated:** statistik harian/mingguan.
- **AI-shared:** hanya agregat terstruktur, opsional.
- Default: **privacy-preserving**.

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

## 16. Asumsi & Open Questions (ringkas)

**`[ASSUMPTION]`** (perlu koreksi):
D1 intervensi bermakna punya action · D2 Future View rule-based · D3 baseline rolling window ·
D4 drift = deviasi + ada task tertunda · D5 satu HP OEM menentukan Spike · D6 tanpa usage tetap berguna ·
D7 AI maks 1x/hari · D8 raw usage tak keluar device · D9 single-user, tanpa onboarding multi-user.

**`[OPEN QUESTION]`** (butuh keputusan — lihat PART C):
OQ1 dependency/recurring model di Phase 1 · OQ2 letak paket `data` · OQ3 overdue behavior ·
OQ4 reminder waktu vs konteks · OQ5 seed kategori default · OQ6 mockup image vs ASCII ·
OQ7 lokasi dokumen · OQ8 struktur file docs · OQ9 batching gate · OQ10 istilah English dalam dokumen ID.

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
> PRD ini adalah **draft**. Beberapa keputusan ditandai `[DECISION REQUIRED]`
> dan **belum final** sampai user menyetujui. Dokumentasi teknis detail
> (Architecture, Database, AI, Privacy) menyusul di batch berikutnya setelah
> PRD ini disetujui.
