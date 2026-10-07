# UX / IA / Wireframes — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md`, `02-user-journeys.md`, `04-design-system.md`
> **Format wireframe:** ASCII (per keputusan OQ6). Fokus: struktur & hierarki,
> bukan pixel-perfect. High-fidelity menyusul bila diperlukan.

---

## 1. Information Architecture

```
Anchor
│
├── Onboarding (sekali, opsional/skippable)
│
├── Home                        [root]
│
├── Tasks
│   ├── All / Today / Upcoming / Completed  [filter tabs]
│   ├── Task Detail
│   │   ├── Subtasks
│   │   ├── Dependencies (Phase 4)
│   │   └── Focus entry
│   └── New/Edit Task
│
├── Focus
│   ├── Focus Setup
│   └── Active Focus
│
├── Wellbeing
│   ├── Overview (screen time, top apps)
│   ├── App Usage (detail per app)
│   └── Scrolling / Drift sessions
│
├── Future View
│   ├── Consequence Compare
│   └── Domino Effect
│
├── Insights
│   ├── Daily Reflection
│   ├── Patterns
│   └── Recommendations
│
└── Settings
    ├── Tracking Permissions
    ├── Notifications
    ├── Focus Rules
    ├── Blocked Apps
    ├── Categories
    ├── Privacy & AI
    └── Data & Retention
```

## 2. Sitemap

```
[Onboarding]
     │
     ▼
[Home] ──────────────────────────────────────────┐
   │  │  │  │                                     │
   │  │  │  └──────────► [Insights] ──► Patterns │
   │  │  └────────────► [Wellbeing] ──► App Usage│
   │  └───────────────► [Focus Setup] ──► [Active Focus] ──► Finish
   └──────────────────► [Tasks] ──► [Task Detail] ──► [Future View]
                                       │                  └─► [Domino Effect]
                                       └─► [New/Edit Task]
     │
     └──────────────► [Settings] ──► (sub-screens)
```

## 3. Navigation Model

**Bottom navigation bar (5 tab):**

```
┌───────────────────────────────────────────────┐
│                                               │
│                  CONTENT                      │
│                                               │
├───────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings    │
└───────────────────────────────────────────────┘
```

**Rationale:**
- **Home** — jawaban "apa yang harus kulakukan sekarang?"
- **Tasks** — kelola niat.
- **Focus** — aksi & sesi fokus (center, paling actionable).
- **Insights** — refleksi & pola (termasuk Wellbeing + Future View di dalamnya).

**Wellbeing & Future View** dijangkau **dari** Home/Insights (bukan tab terpisah)
untuk menghindari bar terlalu penuh & menjaga fokus. Wellbeing punya entri cepat di
Home (kartu kecil) & Insights. `[DECISION]`: bar 5 tab ini rekomendasi; alternatif
adalah 4 tab (Home, Tasks, Focus, Insights) + Wellbeing di Settings.

**Top app bar:** judul kontekstual + aksi sekunder (filter, dsb).

## 4. User Flows (ringkas)

### Flow: Create Task
```
Home/Tasks ──tap +──► [New Task sheet]
   ├─ isi title (wajib)
   ├─ (opsional) deadline, priority, estimasi, kategori, subtask
   └─ Save ──► Task list (view sesuai) ──► Snackbar "Task added"
```

### Flow: Distraction → Intervention (INTI)
```
User buka app distraksi
      │
      ▼
[background] UsageAggregator ──► Drift signal
      │
      ▼
Intervention Engine cek: task tertunda? < 3/hari? cooldown?
      │
      ▼
Level 1 Notification ──tap──► In-app intervention (Level 2)
                                   │
             ┌─────────────────────┴──────────────────┐
        [Start Focus]                             [Later]
             │                                        │
             ▼                                        ▼
        [Focus Setup]                          set cooldown
                                                      │
                                                      ▼
                                             (diam, hormati user)
```

### Flow: Focus Session
```
Task Detail ──[Start Focus]──► [Focus Setup] ──Start──► [Active Focus]
                                                             │
                                        ┌────────────────────┼──────────────┐
                                    [Pause]            [Finish]      [Emergency exit]
                                        │                  │               │
                                    (resume)         (tandai task)     (akhiri sesi)
```

### Flow: Permission Onboarding
```
First launch ──► [Onboarding screen] ──► Smart Todo usable immediately
                     │
                (opsional) "Enable usage access" ──► Android Settings
                     │
                (kapan pun) Settings ──► enable ──► Wellbeing aktif
```

## 5. Screen Inventory

| # | Screen | Purpose | Primary action |
|---|---|---|---|
| 1 | Home | "Apa yang harus kulakukan sekarang?" | Start Focus |
| 2 | Tasks | Kelola semua task | Add task |
| 3 | Task Detail | Lihat/ubah satu task | Start Focus |
| 4 | New/Edit Task | Buat/ubah task | Save |
| 5 | Focus Setup | Konfigurasi sesi | Start |
| 6 | Active Focus | Jalankan sesi | Finish |
| 7 | Wellbeing | Ringkasan screen time | (navigasi detail) |
| 8 | App Usage | Detail per app | (filter/back) |
| 9 | Future View | Konsekuensi waktu | Start Focus |
| 10 | Domino Effect | Rantai dependency | (navigasi task) |
| 11 | Insights | Refleksi & pola | (baca) |
| 12 | Settings | Kontrol & privasi | (toggle) |
| 13 | Permission Onboarding | Panduan izin | Enable / Skip |
| 14 | Contextual Intervention | In-app pesan drift | Start Focus / Later |

## 6. Screen Specifications (dengan wireframe ASCII)

Setiap screen memuat: **Purpose · Main info · Primary action · Secondary actions ·
Empty · Loading · Error · Permission · Offline**.

---

### 6.1 Home

**Purpose:** Menjawab "apa yang harus kulakukan sekarang?"

**Main info:** Current priority; today's tasks; progress; contextual recommendation.

**Primary action:** Start Focus · **Secondary:** open task detail, quick add.

```
┌─────────────────────────────────────────────┐
│  Good evening, Dafi            ⚙            │   ← top bar (greeting kontekstual)
├─────────────────────────────────────────────┤
│                                             │
│   RIGHT NOW                                 │   ← label kecil, muted
│   ┌───────────────────────────────────────┐ │
│   │ 🔴 Kerjakan tugas Web                 │ │   ← current priority card
│   │ Deadline: today 23:59 · Est: 2h       │ │
│   │ ▓▓▓▓▓▓░░░░░░░░  35%                    │ │   ← progress halus
│   │                                       │ │
│   │            [ Start Focus ]            │ │   ← primary action
│   └───────────────────────────────────────┘ │
│                                             │
│   TODAY                               2/3   │
│   ─────────────────────────────────────────  │
│   ☐ Kerjakan tugas Web            HIGH      │
│   ☐ Study calculus                MED      │
│   ☑ Baca slide chapter 3          done     │
│                                             │
│   ● You've been scrolling longer than       │   ← contextual tip (kalem)
│     usual today.                            │
│                                             │
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

**Empty:** "Nothing planned yet. What matters today?" + [Add task]
**Loading:** skeleton cards (shimmer halus)
**Error:** "Couldn't load your day. Retry?" (data lokal jarang error)
**Permission:** Home tetap tampil; tip usage disembunyikan
**Offline:** penuh fungsi (lokal)

---

### 6.2 Tasks

**Purpose:** Kelola semua task lintas waktu.

**Main info:** daftar task; **Primary:** add task; **Secondary:** filter, swipe actions.

```
┌─────────────────────────────────────────────┐
│  Tasks                            ⚙  +      │
├─────────────────────────────────────────────┤
│  [ All ]  Today   Upcoming   Completed       │   ← filter chips
├─────────────────────────────────────────────┤
│                                             │
│  🔴 Kerjakan tugas Web                      │
│     today 23:59 · 35% · 2h                   │
│  ───────────────────────────────────────────│
│  🟡 Study calculus                          │
│     tomorrow · 0%                            │
│  ───────────────────────────────────────────│
│  🟢 Baca slide chapter 3                   │
│     done                                     │
│                                             │
│                                      ( + )   │   ← FAB
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

**Empty:** "No tasks here yet." + [Create your first task]
**Loading:** list skeleton
**Error:** retry inline
**Permission:** N/A (selalu tersedia)
**Offline:** penuh

---

### 6.3 Task Detail

**Purpose:** Lihat/ubah satu task; pintu masuk Focus.

```
┌─────────────────────────────────────────────┐
│  ←                              ⋯           │
├─────────────────────────────────────────────┤
│  Kerjakan tugas Web                          │   ← title (edit di sini)
│  🔴 HIGH · Due today 23:59 · Est 2h          │
│  ────────────────────────────────────────────│
│  Description                                 │
│  Buat aplikasi web sederhana ...             │
│                                             │
│  PROGRESS  ▓▓▓▓▓▓░░░░░░░░  35%               │
│                                             │
│  SUBTASKS                                    │
│   ☑ Setup project                            │
│   ☑ Create database                          │
│   ☐ Build login                              │
│   ☐ Testing                                  │
│   ☐ Submit                                   │
│   + add subtask                              │
│                                             │
│  DEPENDENCIES (Phase 4)                      │
│   → Review → Submit → Presentation           │   ← dormant di Phase 1
│                                             │
│           [ Start Focus ]  [ Reschedule ]    │
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

**Empty:** N/A (selalu ada isi)
**Loading:** N/A
**Error:** "Task not found" bila dihapus
**Overdue:** badge "Overdue" muted + tombol Reschedule menonjol

---

### 6.4 Focus Setup

**Purpose:** Konfigurasi sesi fokus.

```
┌─────────────────────────────────────────────┐
│  ←  Focus                                   │
├─────────────────────────────────────────────┤
│  TASK                                        │
│   🔴 Kerjakan tugas Web                      │
│   Est 2h · current step: Build login         │
│                                             │
│  MODE                                        │
│   ( ) Gentle   — reduce interruptions only   │
│   (•) Strict   — block distracting apps      │
│   ( ) Custom   — choose apps to block        │
│                                             │
│  BLOCKED APPS (Strict/Custom)                │
│   TikTok 🔒   Instagram 🔒   YouTube 🔒       │
│   WhatsApp 🔓  Phone 🔓      Calculator 🔓    │
│                                             │
│  DURATION  [ 2h 00m ]                        │
│                                             │
│              [ Start Focus ]                 │
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

**Permission state:** bila Strict/Custom & Accessibility off →
"Blocking needs Accessibility. [Enable] or use Gentle." → fallback jelas.

---

### 6.5 Active Focus

**Purpose:** Menjalankan sesi; tetap bisa keluar.

```
┌─────────────────────────────────────────────┐
│                    FOCUS                     │
│                                             │
│             Kerjakan tugas Web               │
│                                             │
│                42:18                        │   ← timer besar, tenang
│               remaining                      │
│                                             │
│   ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░  60%                 │
│                                             │
│   Current step:                              │
│   Build login page                           │
│                                             │
│        [ Pause ]        [ Finish ]           │
│                                             │
│              Emergency exit                  │   ← selalu terlihat, kecil
│                                             │
└─────────────────────────────────────────────┘
```

**Blocking aktif:** indikator kecil "Blocking 4 apps".
**Fallback:** "Blocking unavailable — running in Gentle mode."
**Emergency exit:** mengakhiri sesi + (opsional) menandai interruption.

---

### 6.6 Wellbeing

**Purpose:** Ringkasan penggunaan perangkat (lokal).

```
┌─────────────────────────────────────────────┐
│  ←  Wellbeing                    ⚙           │
├─────────────────────────────────────────────┤
│  TODAY                                       │
│   Screen time       5h 32m                   │
│                                             │
│  TOP APPS                                    │
│   TikTok      ▓▓▓▓▓▓▓▓░░  2h 10m             │
│   YouTube     ▓▓▓▓░░░░░░  1h 12m             │
│   Instagram   ▓▓░░░░░░░░  48m                │
│                                             │
│  TREND (7 days)                              │
│   ▁ ▃ ▂ ▅ ▄ ▆ ▃                              │   ← sparkline sederhana
│                                             │
│  SESSIONS                                    │
│   Longest today: TikTok — 47m                │
│   (vs your usual 18m)                        │   ← baseline personal, netral
│                                             │
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

**Permission:** "Wellbeing needs usage access. [Enable] — your tasks work without it."
**Empty/limited:** "Your device limits usage details. Showing what's available."
**Offline:** penuh (lokal).

---

### 6.7 App Usage

**Purpose:** Detail per app.

```
┌─────────────────────────────────────────────┐
│  ←  TikTok                                   │
├─────────────────────────────────────────────┤
│  Today            2h 10m                     │
│  Sessions         6                          │
│  Avg session      22m                        │
│  Your baseline    18m                        │
│  Category         Social (edit)              │
│                                             │
│  SESSIONS TODAY                              │
│   09:12 – 09:30   18m                        │
│   12:40 – 12:55   15m                        │
│   21:05 – 21:52   47m  ← longer than usual   │   ← netral
│                                             │
└─────────────────────────────────────────────┘
```

---

### 6.8 Future View

**Purpose:** Konsekuensi waktu.

```
┌─────────────────────────────────────────────┐
│  ←  What happens?                            │
├─────────────────────────────────────────────┤
│  Based on your current schedule...           │   ← disclaimer
│                                             │
│   IF I START NOW      │  IF I CONTINUE DELAY │
│   ────────────────────┼──────────────────────│
│   15:10 → Start        │  20:00 → Start       │
│   15:10–17:10 Focus    │  20:00–22:00 Work    │
│   17:10 → Completed    │  22:00+ Less free    │
│   Evening → Free       │  23:59 → Deadline    │
│                                             │
│              [ Start Focus ]                 │
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

**Data kurang:** "Add an estimate to see a more accurate view." + pakai default bertanda.

---

### 6.9 Domino Effect

```
┌─────────────────────────────────────────────┐
│  ←  Impact                                   │
├─────────────────────────────────────────────┤
│   1 task may affect 3 upcoming tasks.        │   ← estimatif, bukan pasti
│                                             │
│   ┌───────────────┐                          │
│   │ Web Assignment│  (delayed)               │
│   └───────┬───────┘                          │
│           ▼                                  │
│   ┌───────────────┐                          │
│   │    Review     │                          │
│   └───────┬───────┘                          │
│           ▼                                  │
│   ┌───────────────┐                          │
│   │    Submit     │                          │
│   └───────┬───────┘                          │
│           ▼                                  │
│   ┌───────────────┐                          │
│   │ Presentation  │                          │
│   └───────────────┘                          │
│                                             │
└─────────────────────────────────────────────┘
```

---

### 6.10 Insights

```
┌─────────────────────────────────────────────┐
│  ←  Insights                                 │
├─────────────────────────────────────────────┤
│  YOUR DAY                                    │
│   Tasks        2 / 3 completed               │
│   Focus        1h 42m                        │
│   Screen time  5h 18m                        │
│   Longest distraction  TikTok — 45m          │
│                                             │
│  ─────────────────────────────────────────   │
│  INSIGHT                          (local/AI) │   ← sumber ditampilkan jujur
│   "You completed difficult tasks earlier     │
│    than usual today."                        │
│                                             │
│  SUGGESTION                                  │
│   "Tomorrow, start your hardest task within  │
│    the first productive window."             │
│                                             │
│  PATTERNS                                    │
│   • Long scrolling sessions often 9–11 PM    │
│                                             │
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

**Offline / AI off:** tampil rule-based, label "local". **Data minim:** tampil
keterbatasan, bukan pola karangan.

---

### 6.11 Settings

```
┌─────────────────────────────────────────────┐
│  ←  Settings                                 │
├─────────────────────────────────────────────┤
│  TRACKING                                    │
│   Usage access         [Enable / Granting]   │
│   Notifications        [Enabled]             │
│   Accessibility        [Off]                 │
│                                             │
│  FOCUS                                       │
│   Default mode         Gentle                │
│   Blocked apps         4 apps                │
│                                             │
│  CATEGORIES                                  │
│   Manage categories & app overrides          │
│                                             │
│  PRIVACY & AI                                │
│   AI insights          [ Off ]               │
│   Shared data          Aggregated only       │
│   Data retention       30 days               │
│   Delete all data      →                     │
│                                             │
├─────────────────────────────────────────────┤
│  Home   Tasks   Focus   Insights   Settings  │
└─────────────────────────────────────────────┘
```

---

### 6.12 Permission Onboarding

```
┌─────────────────────────────────────────────┐
│                                             │
│   Welcome to Anchor                          │
│                                             │
│   Anchor helps you notice when you drift     │
│   from what you said was important.          │
│                                             │
│   You can start with tasks right now.        │
│                                             │
│   To notice distractions, Anchor needs       │
│   usage access. It stays on your device.     │
│                                             │
│   [ Enable usage access ]                    │
│   [ Skip for now ]                           │   ← tidak memaksa
│                                             │
└─────────────────────────────────────────────┘
```

---

### 6.13 Contextual Intervention (in-app)

```
┌─────────────────────────────────────────────┐
│                                             │
│   You've been scrolling for 47 minutes.      │
│                                             │
│   Your Web Assignment is still waiting.      │
│   You planned around 2 hours for it.         │
│                                             │
│   [ Start Focus ]         [ Later ]          │
│                                             │
│   See what happens →                         │
│                                             │
└─────────────────────────────────────────────┘
```

**Tone:** singkat, netral, memberi pilihan. **Bukan** "STOP USING YOUR PHONE!"

## 7. Motion & Interaction Principles (ringkas — detail di Design System)

- Transisi halus, durasi pendek (150–250ms), easing standar.
- Tidak ada animasi berat; hormati "Reduce motion".
- Feedback taktil: haptic ringan pada aksi penting (selesai task, mulai fokus).
- Semua state (empty/loading/error) punya pesan manusiawi.
