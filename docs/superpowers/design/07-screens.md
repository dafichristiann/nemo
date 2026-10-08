# Screens & Flows — Anchor (Personal Focus Assistant)

> **Status:** Derived from `gamabaran.pen` (aktual) + PRD v0.2 · **Version:** 0.2
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md`, `06-design-system.md`, `gamabaran.pen`
> **Catatan:** Inventaris ini mengekstrak **isi teks sebenarnya** dari file desain
> `gamabaran.pen`. Nama frame = nama layar. Gunakan ini saat membangun navigasi Compose.
>
> **Bahasa mockup:** banyak label memakai Bahasa Indonesia (mis. "Lanjutkan", "Nanti
> saja", "Lihat dampak jika menunda", "Hapus Semua Data") bercampur dengan English
> (mis. "Get started", "Save Task"). Lihat §7 untuk catatan lokalisasi.

---

## 0. Ringkasan Kanvas

File `gamabaran.pen` berisi:
- **1 banner** (Anchor - Top Banner) — papan judul.
- **1 hero** (3D Hero Showcase) — showcase dengan mockup HP.
- **11 layar HP** (375 × 812) mewakili alur produk dari onboarding sampai detail task.

### Peta Kanvas (grid)

```
Kolom x  : 0, 435, 870, 1305      (gap 60)
Baris y  : banner 0 · hero 140 · grid 780, 1652, 2524  (gap 60)
Lebar grid total = 1680 = lebar banner/hero (rata).
```

| Baris | Kolom 1 (x0) | Kolom 2 (x435) | Kolom 3 (x870) | Kolom 4 (x1305) |
|---|---|---|---|---|
| 1 (y780) | 00 Splash & Onboarding | 00b Permission Onboarding | 01 Home - Smart Todo | 02 Add Task Sheet |
| 2 (y1652) | 03 Digital Wellbeing | 04 Contextual Intervention | 05 Focus Setup | 06 Focus Mode - Active |
| 3 (y2524) | 07 Future View | 08 Insights - Daily Reflection | 09 Settings & Privacy | 10 Task Detail |

---

## 1. Alur Tingkat Tinggi

```
   [00 Splash/Onboarding]
            │  Get started
            ▼
   [00b Permission Onboarding]  (opsional, tidak memaksa)
            │  Continue / Skip
            ▼
   ┌─────────────────────────── Home / Tabs ───────────────────────────┐
   │  Tasks (01)   ·   Wellbeing (03)   ·   Focus (05/06)   ·  Insights (08) │
   └───┬───────────────┬──────────────────┬──────────────────┬─────────┘
       │               │                  │                  │
       ▼               ▼                  ▼                  ▼
  [10 Task Detail]  [03 Wellbeing]   [05 Focus Setup]    [08 Insights]
       │                                 │                  │
       ├──► [02 Add Task Sheet]          ▼                  └─(data dari semua)
       │                          [06 Focus Mode - Active]
       └──► [07 Future View] ◄──(lihat konsekuensi)   [09 Settings & Privacy]
                    │
                    └──► Domino Effect (bagian dari 07)

  [04 Contextual Intervention]  ← muncul kontekstual (dari mana saja, via notif/in-app)
```

---

## 2. Inventaris Layar (detail)

### 2.1 `Anchor - Top Banner` — Papan Judul
- **Peran:** header kanvas (bukan layar app).
- **Isi:** `Anchor - Personal Focus Assistant` (22/700) + `Black & White Minimalist · 3D Depth System` (12/400).
- **Latar:** `#0A0A0A`, teks putih. Ukuran 1680 × 80.

### 2.2 `3D Hero Showcase` — Hero
- **Peran:** showcase promo (bukan alur app). Ukuran 1680 × 580.
- **Isi:**
  - Judul `Anchor` (68/700), slogan `Stay anchored to what matters.` (50/700),
    sub `Personal Focus Assistant · Android · Local-first` (28).
  - 3 pill: **Local-first · Privacy-first · AI Optional**.
  - 3 stat card: **5** Phases in MVP · **10** Screens Designed · **3D** Depth System.
  - Mockup HP di kanan menampilkan **Home mini** (Good morning, "Today's progress
    1 of 3 completed 33%", task Web Assignment HIGH, Read Chapter 3, Review lecture
    notes, nav 4 tab).
- **Dipakai untuk:** konteks brand + referensi visual Home yang diperkecil.

---

### 2.3 `00 - Splash & Onboarding` (Phase 1)

| Elemen | Isi |
|---|---|
| Skip | Teks kanan atas `#888` |
| Avatar Stack | Lingkaran 251 px + ikon `anchor` (logo) |
| Title Line 1 / 2 | "Stay anchored" / "to what matters." — 32/700 |
| Subtitle | Nada lembut: "Anchor menyadari ketika kamu menjauh dari tugas yang penting — dan membantu kamu kembali, tanpa menghakimi." |
| CTA | Tombol 3D "Get started" (56 px) |

**Behavior:** tidak memaksa izin. Tekan "Get started" → 00b.

---

### 2.4 `00b - Permission Onboarding` (Phase 1)

| Elemen | Isi |
|---|---|
| Step Indicator | "Setup 1 of 2" |
| Progress Track/Bar | Track `#EEE`, bar `#0A0A0A` (155/311) |
| Headline | "Smart Todo works / right away." |
| Sub | "Izin ini opsional — buka fitur lengkap Anchor kapan pun kamu siap." |
| Perm 1 Card | Usage Access — "Untuk Wellbeing & deteksi drift" — tag `Opsional` |
| Perm 2 Card | Notifications — "Intervensi lembut & sesi fokus" — tag `Opsional` |
| Privacy Card | "Semua data tersimpan lokal di HP." (ikon `lock`) |
| Continue | Tombol utama — "Lanjutkan" |
| Skip Link | "Lewati dulu" (teks tengah bawah) |

**Behavior (PRD §14):** izin opsional; ditolak → app tetap jalan sebagai Smart Todo.
**Untuk implementasi, tambahkan cara menampilkan status izin + pintasan ke Settings** (FR-S1).

---

### 2.5 `01 - Home - Smart Todo` (Phase 1)

| Elemen | Isi |
|---|---|
| Greeting / Date | "Good morning" + "Wednesday, Oct 7" |
| Progress Card | "Today's progress" / "1 of 3 tasks completed" / `33%` |
| Tabs | **All · Today · Upcoming · Completed** (indikator garis di bawah) |
| Task Item ×3 | Bar prioritas kiri, judul, meta, badge prioritas, sub-progress |
| FAB | `+` (kanan bawah) → Add Task Sheet |
| Nav Bar | Tasks · Wellbeing · Focus · Insights |

Contoh isi task: **Web Assignment** (Due today 23:59 · 2h est · 3 subtasks, badge HIGH,
sub 1/3) · **Read Chapter 3** (Completed 10:32) · **Review lecture notes** (Due tomorrow ·
45 min est, badge MEDIUM).

**Behavior:**
- Overdue → tampilkan **banner non-blocking** di atas list (lihat `06-design-system.md` §7.12).
- Progress task = dari subtask; jika tidak ada → 0/100 (FR-T9).
- FAB → `02 Add Task Sheet`.

---

### 2.6 `02 - Add Task Sheet` (Phase 1)

Bottom sheet (radius atas), scrim gelap di belakang.

| Elemen | Isi |
|---|---|
| Sheet Handle + Title | "New Task" + close |
| Title Field | Placeholder "Task title..." |
| Priority | Chip: **Low · Medium · High · Urgent** (High terpilih) |
| Deadline | Field `Oct 7, 23:59` |
| Est. Duration | Field `120 min` |
| Subtasks (1/3) | Baris subtask yang bisa ditambah/centang |
| Save Task | Tombol utama |

**Behavior (FR-T1..T6):** simpan → task muncul di Today; judul kosong tidak bisa disimpan.
Heuristic default bila durasi/deadline kosong (lihat Future View).

---

### 2.7 `03 - Digital Wellbeing` (Phase 2)

| Elemen | Isi |
|---|---|
| Screen Title / Date | Judul + tanggal |
| Screen-Time Card | Label, nilai besar, perbandingan ("vs kemarin") |
| Top Apps | 2 app: ikon, nama, meta, durasi |
| Weekly Chart | Bar 7 hari (Mon–Sun), label hari |
| Nav Bar | |

**Behavior (PRD §10.2):**
- **Tanpa izin** → empty state jelas + tombol ke Settings (bukan crash).
- Angka **dari Android**, bukan AI/fake.
- Tren tampil sederhana (bar), bukan dashboard ramai.
- Kategori bisa di-override manual per app (FR-S3).

---

### 2.8 `04 - Contextual Intervention` (Phase 3)

Overlay kartu di tengah (scrim), muncul kontekstual.

| Elemen | Isi |
|---|---|
| Card | Ikon fokus, headline 2 baris: "You've been scrolling longer than usual." |
| Body | "TikTok · 47 min (+161% baseline) · Web Assignment…" |
| Primary | **[Start Focus]** — "Start Focus" |
| Secondary | "Lihat dampak jika menunda" (link ke Future View) |
| Tertiary | "Nanti saja" (dismiss / Later) |

**Behavior (PRD §10.4):**
- Konten menyebut konteks & menghubungkan ke task tertunda.
- **[Start Focus]** → Focus Setup & catat `INTERACTED` (−1 kuota).
- **"Nanti saja"** → cooldown 2 jam per konteks & catat `DISMISSED` (−1 kuota).
- Kuota max 3/hari; jika habis → ditahan (tidak tampil).

---

### 2.9 `05 - Focus Setup` (Phase 3)

| Elemen | Isi |
|---|---|
| Back / Title / Sub | Navigasi + judul |
| Task Selector | Nama + meta task + chevron |
| Mode | **Gentle · Strict · Custom** (3 kartu) |
| Duration | Chip: **25 · 45 · 60 · Custom** (25 terpilih) |
| Begin | Tombol utama |

**Behavior (FR-F1..F8):**
- Gentle = kurangi notifikasi, **tanpa** block apa pun.
- Strict/Custom = butuh Accessibility; jika tidak tersedia → fallback Gentle + beri tahu.
- Begin → Foreground Service + `06 Focus Mode - Active`.

---

### 2.10 `06 - Focus Mode - Active` (Phase 3)

| Elemen | Isi |
|---|---|
| Focus Tag / Task Name / Current Step | Konteks sesi |
| Timer Ring | Cincin progres + digit besar (mis. `19:22`) + sub |
| Subtasks | Sub 1 (checked), Sub 2 (aktif), Sub 3 |
| Pause / Finish | Dua tombol |
| Emergency Link | **Wajib tersedia & jelas** |

**Behavior (PRD §10.5):**
- Harus menampilkan: nama task, current step, timer, progress, Pause/Finish, **emergency exit**.
- Finish → catat `FocusSession` + tawarkan update progres task.
- Foreground Service hanya saat aktif (notifikasi persisten).

---

### 2.11 `07 - Future View` (Phase 4)

| Elemen | Isi |
|---|---|
| Back / Title | "Back" / **What happens next?** |
| Disclaimer | "Berdasarkan jadwalmu saat ini — proyeksi, bukan ramalan." |
| Scen A Card | **IF I START NOW** — `15:10 Start right now · 15:10–17:10 Focus` ... |
| Scen B Card | **IF I DELAY** — `20:00 Start late · 20:00–22:00 Work at night` ... |
| Domino Header/Sub | "Domino Effect" / "1 task may affect 3 upcoming tasks." |
| Node 1→2→3 | Web Assignment (CURRENT) → Peer review submission → Final presentation |
| Start Focus | Aksi utama — "Start Focus Now" |

**Behavior (PRD §10.6–10.7):**
- Dua skenario berdampingan, rule-based.
- Bahasa estimatif ("may affect"), **bukan** kepastian.
- Tanpa estimasi/deadline → pakai heuristic (60m / 23:59) & tandai asumsi.
- Anti-siklus dependency.

---

### 2.12 `08 - Insights - Daily Reflection` (Phase 5)

| Elemen | Isi |
|---|---|
| Title / Date | "Insights" / "October 7 — Daily Reflection" |
| Stats | `2/3` Tasks done · `1h 42m` Focus time · `5h 18m` Screen time (dengan divider) |
| Distraction | "Longest distraction session — TikTok · 45 min" |
| Insight Block | Tag `INSIGHT` + kutipan ("You completed difficult tasks earlier than usual...") |
| Suggestion Block | Tag `SUGGESTION` + saran realistis |
| AI Attribution | "Generated by Anchor Local AI" (tandai sumber) |
| Pattern | "Most productive: 09:00–11:00", "Peak distraction: 14:00–16:00" |
| Nav Bar | Tasks · Wellbeing · Focus · Insights |

**Behavior (PRD §10.8–10.9):**
- AI opsional; offline/nonaktif → **rule-based** tanpa error.
- Tidak ada data usage → sembunyikan bagian itu (bukan menampilkan 0 menyesatkan).
- Tampilkan sumber insight dengan jujur.

---

### 2.13 `09 - Settings & Privacy` (Phase 1+)

| Seksi | Isi |
|---|---|
| Title | "Settings" |
| `PERMISSIONS` | Usage Access — "Granted" · Notifications — "Belum aktif — ketuk untuk izinkan" |
| `PRIVACY & AI` | AI Personal Insights — "Hanya kirim ringkasan agregat" + **toggle** on/off |
| `DATA CONTROLS` | Data Retention — "Simpan maksimal 30 hari" · **Hapus Semua Data** (danger lembut) |
| Assurance | "Raw usage data tidak pernah meninggalkan HP." |

**Behavior (PRD §10.10):**
- Toggle AI menjelaskan sederhana apa yang dikirim (hanya agregat).
- Atur retensi + tombol hapus semua data.
- **Default privacy-preserving** (tracking & AI off sampai opt-in).

> ✅ **Mockup diperbarui:** teks retensi kini **"maksimal 30 hari"**, konsisten dengan
> PRD §10.2 yang mengunci **14–30 hari** (raw `UsageSession`, contoh 30 hari). Koreksi
> diterapkan di `build_pen.js` dan `gamabaran.pen`. Lihat §7.

---

### 2.14 `10 - Task Detail` (Phase 1)

| Elemen | Isi |
|---|---|
| Back / Edit | "Tasks" / "Edit" |
| Priority Bar Top | Garis atas (prioritas) |
| Task Title | "Web Assignment" |
| Badges | `HIGH` + `IN PROGRESS` |
| Meta | "Due Oct 7, 23:59" · "Estimated: 2 hours" · "Progress: 33% (1 of 3 steps)" |
| Progress Track | Track + fill (33%) |
| Subtasks | "Research references" (done) · "Write the report body (Active)" · "Add conclusion" |
| Future View Callout | "Lihat skenario jika ditunda" |
| Actions | "Start Focus Session" (primary) + "Reschedule" + "Mark as Done" |

**Behavior (FR-T8, FR-T12):**
- Complete → `completedAt` diisi, progress 100%, pindah ke Completed.
- Reschedule → catat `RescheduleEvent` (untuk insight).
- Start Focus → Focus Setup untuk task ini.

---

## 3. Peta Layar → Fase → FR

| Layar | Phase | FR terkait (PRD) |
|---|---|---|
| 00 Splash | 1 | §10.10 onboarding |
| 00b Permission | 1 | FR-S1, §14 |
| 01 Home | 1 | FR-T1..T12 |
| 02 Add Task | 1 | FR-T1..T6 |
| 03 Wellbeing | 2 | FR-W1..W7 |
| 04 Intervention | 3 | FR-I1..I7 |
| 05 Focus Setup | 3 | FR-F1..F8 |
| 06 Focus Active | 3 | FR-F1..F8 |
| 07 Future View | 4 | FR-FV1..FV5, FR-DE1..DE5 |
| 08 Insights | 5 | FR-AI1..AI6, FR-DR1..DR5 |
| 09 Settings | 1+ | FR-S1..S6 |
| 10 Task Detail | 1 | FR-T8, FR-T11, FR-T12 |

---

## 4. Komponen yang Perlu Jadi Reusable Component (Compose)

- `TaskCard` (varian: with/without subtask, priority bar).
- `PrimaryButton` (3D), `SecondaryButton`, `TextPill`.
- `PermissionCard`, `SettingsRow`, `ToggleRow`.
- `ProgressTrack` (+ fill).
- `SegmentedChips` (priority, duration).
- `BottomSheet` (Add Task).
- `NavBar` (4 tab).
- `Banner` (overdue / callout).
- `TimerRing` (Focus).
- `InsightBlock` (tag + quote + source).
- `Divider`, `SectionHeader`.

---

## 5. Yang Belum Ada di Mockup (perlu ditambah saat implementasi)

Berdasarkan PRD, layar/komponen berikut **belum** tampak di `gamabaran.pen` dan sebaiknya
ditambahkan saat implementasi:

1. **Empty states** (tanpa task, tanpa izin Wellbeing, hari tanpa data) — FR & §10.
2. **Overdue banner** (mockup hanya punya task biasa) — §10.1.
3. **Izin ditolak → panduan Settings** eksplisit untuk Wellbeing — §10.2.
4. **Notifikasi intervensi** (channel utama) — §10.4.
5. **Fullscreen intervention** (jarang) — §10.4.
6. **Reschedule picker** (tanggal/waktu) — FR-T12.
7. **Kategori picker + override per app** — FR-S3, FR-S4.
8. **Focus selesai → dialog update progres** — FR-F6.
9. **Error/degradasi** (Accessibility mati, OEM batasi UsageStats).

> Menambah layar ini mengikuti token di `06-design-system.md`.

---

## 6. Catatan Implementasi Layout

- Frame mockup 375×812 = referensi device; gunakan padding horizontal **32** & lebar
  konten **311**.
- Nav bar 4 tab konsisten di Home / Wellbeing / Insights.
- Modal & sheet: scrim + radius atas membulat.
- Header layar berulang pakai pola: "Back" (koin kiri) + Title; layar tertentu punya
  aksi kanan ("Edit", "Skip").
- Semua teks via string resources (default English, siap `id`).

---

## 7. Catatan Lokalisasi & Ketidaksesuaian Mockup ↔ PRD

### 7.1 Bahasa campuran

Mockup memakai campuran **English** dan **Bahasa Indonesia** pada label:
- ID: "Lanjutkan", "Lewati dulu", "Nanti saja", "Lihat dampak jika menunda",
  "Simpan maksimal 30 hari", "Hapus Semua Data", "Pilih task dan mode fokus kamu."
- EN: "Get started", "Save Task", "Begin Focus", "Start Focus Now", "Mark as Done",
  "Tasks/Wellbeing/Focus/Insights".

**Rekomendasi:** pilih **satu** default bahasa untuk rilis (PRD §12: default **English**,
siap Indonesia). Seluruh teks harus lewat `string.xml` — jangan hardcode.

> ✅ **Status implementasi:** default rilis = **English** (`res/values/strings.xml`), dengan
> terjemahan **Bahasa Indonesia** penuh (`res/values-id/strings.xml`, 183 kunci, paritas
> 100%). Pemilihan bahasa per-app diaktifkan via `generateLocaleConfig` + `res/resources.properties`
> (default `en-US`), sehingga pengguna Android 13+ dapat memilih bahasa dari pengaturan sistem.

### 7.2 Selisih yang perlu diperbaiki di mockup

| # | Di mockup | PRD (Locked) | Tindakan |
|---|---|---|---|
| 1 | Settings: retensi "maksimal 90 hari" | §10.2: raw `UsageSession` di-prune **14–30 hari** (contoh 30) | ✅ Diperbarui ke "maksimal 30 hari" (mockup + docs) |
| 2 | Overdue **banner** tidak tampak di mockup Home | §10.1: banner non-blocking wajib | Tambah ke mockup/implementasi |
| 3 | Notifikasi intervensi & fullscreen tidak digambar | §10.4: notification channel utama | Tambah (mockup belum ada) |
| 4 | Empty states (tanpa task/izin/data) tidak digambar | §10.2, §10.9, §10.10 | Tambah |
| 5 | Dialog "update progres" setelah Finish | FR-F6 | Tambah |

> Semua selisih di atas **tidak** mengubah desain yang sudah ada — hanya menambah/merapikan
> agar konsisten dengan PRD.

### 7.3 Yang sudah sesuai (terverifikasi dari `.pen`)

- 11 layar + banner + hero ada dan lengkap; narasi Home → Detail → Focus → Future → Insight
  konsisten.
- Angka Bahasa di mockup (5h 18m, 1h 42m, 33%, dst.) konsisten antar layar.
- Task contoh ("Web Assignment") konsisten dari Home sampai Task Detail & Future View.

---

## 8. Referensi Silang

- Token visual → `06-design-system.md`
- Perilaku detail → `01-prd.md` §10
- Alur fase → `05-mvp-scope.md`
- Mockup → `gamabaran.pen`

---

> **CATATAN AKHIR:** Inventaris ini adalah **peta navigasi**, bukan pengganti mockup.
> Saat menambah layar baru, patuhi prinsip visual & nada PRD. Jika `gamabaran.pen` diedit,
> perbarui §7 (audit) agar tetap sinkron.
