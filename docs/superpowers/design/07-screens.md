# Screens & Flows — Nemo (Personal Focus Assistant)

> **Status:** Authoritative Screen Inventory & Flow Spec · **Version:** 0.6
> **Tanggal:** 2026-10-07 (v0.2) · 2026-10-27 (v0.6)
> **Sumber kebenaran:** `justmd/fitur-aplikasi-nemo-maskot.md` & kode aplikasi aktual (`com.anchor.app`).
> **Dokumen terkait:** `01-prd.md`, `06-design-system.md`, `../specs/2026-10-10-nemo-design-migration-design.md`
> **Bahasa UI:** Default English (`res/values/strings.xml`), paritas penuh Bahasa Indonesia (`res/values-id/strings.xml`).

---

## 1. Daftar 19 Halaman

| # | Halaman | Cara Dibuka | Jenis Tampilan | Ukuran & Ekspresi Maskot |
|---|---|---|---|---|
| 1 | Splash | Otomatis saat cold start | Layar penuh | Besar (120dp): `Neutral` (1.5s) → `Happy` (2s) |
| 2 | Onboarding — Profil (2 of 3) | Otomatis setelah Splash bagi user baru | Layar penuh | Besar (120dp): `Talking` (menyapa) → `Happy` saat nickname terisi |
| 3 | Onboarding — Izin (3 of 3) | "Continue" di Onboarding Profil | Layar penuh | Besar (120dp): `Shy` (minta izin) → `Happy` saat semua izin aktif |
| 4 | Tasks (Home) | Tab bawah "Tasks" (default) | Tab shell utama | Header: `Happy`/`Neutral` · Overdue: `Sad` · 100%: `Laughing` · Empty: `Talking` |
| 5 | Add Task | FAB `+` di Tasks | Bottom sheet | Tidak ada maskot (fokus pada formulir) |
| 6 | Task Detail | Ketuk sebuah task di Tasks | Layar penuh | Callout Future View: Kecil `Thinking` |
| 7 | Reschedule | Tombol "Reschedule" di Overdue / Task Detail | Bottom sheet | Tidak ada maskot |
| 8 | Wellbeing | Tab bawah "Wellbeing" | Tab shell utama | Permission prompt: Besar `Shy` · No data: Besar `Sleepy` |
| 9 | Focus — Setup | Tab bawah "Focus" (tanpa sesi aktif) | Tab shell utama | Title: Kecil `Neutral` · Fallback notice: `Confused` · Task empty: `Thinking` |
| 10 | Focus — Active | "Begin Focus" di Focus Setup | Tab shell utama | Timer running: Kecil `Neutral` · Pause: `Sleepy` · Finish verified: `Laughing` · Early end: `Sad` |
| 11 | Overlay "Back to focus?" | Membuka app terblokir saat sesi Strict/Custom aktif | System window overlay | Sedang (80dp): `Angry` (menegur secara ramah) |
| 12 | Notifikasi Sesi Fokus | Otomatis saat sesi fokus berjalan | Ongoing notification | Status bar: `mascot_silhouette` · Large icon: `Neutral` |
| 13 | Future View + Domino | Callout "See what happens if you delay" di Task Detail | Layar penuh | Skenario A: Kecil `Happy` · Skenario B: Kecil `Angry` · Circular link: `Confused` |
| 14 | Insights — Daily Reflection | Tab bawah "Insights" | Tab shell utama | INSIGHT card: Kecil `Talking` · Empty: Besar `Sleepy` |
| 15 | Intervention (In-App) | Drift terdeteksi (saat dalam app / notif ditolak) | Bottom sheet / overlay | Sedang (80dp): Level 1 `Thinking` · Level 2 `Sad` · Level 3 `Happy` · Level 4 `Talking` |
| 16 | Notifikasi Intervention | Drift terdeteksi (channel "Gentle check-ins") | System notification | Status bar: `mascot_silhouette` · Large icon: sesuai level (Thinking/Sad/Happy/Talking) |
| 17 | Settings | Ikon "Tune" (kanan atas) di Tasks | Layar penuh | Penegasan privasi: Kecil `Happy` · Delete all data: Kecil `Sad` |
| 18 | App Categories | Settings → "Manage" pada App categories | Layar penuh (sub-halaman) | Empty state: Besar `Sleepy` |
| 19 | Blocked Apps | Settings → "Manage" pada Blocked apps | Layar penuh (sub-halaman) | Empty state: Besar `Confused` |

---

## 2. Peta Alur Navigasi

```
[Splash]  (animasi maskot 3.5 detik, tanpa video player)
    │
    ▼
[Onboarding — Profil]  "Setup 2 of 3"        (Continue / Skip for now)
    │  Continue (izin belum lengkap)
    ▼
[Onboarding — Izin]    "Setup 3 of 3"        (Continue / Skip for now)
    │
    ▼
╔════════════════ SHELL UTAMA (4 tab bawah) ════════════════╗
║  Tasks   │   Wellbeing   │   Focus   │   Insights          ║
╚═══════════════════════════════════════════════════════════╝
    │            │              │              │
    │            │              │              │
  Tasks        Wellbeing      Focus         Insights
    │                          ├─ (tidak aktif) Setup ──► [Begin Focus] ──► Active
    │                          │                                              │
    │                          │                                    Pause/Finish/Emergency exit
    │                          └─ (akses) Settings → Blocked Apps
    │
    ├─ (FAB +) ──► [Add Task sheet]
    ├─ (ketuk tugas) ──► [Task Detail]
    │                        ├─ "See what happens if you delay" ──► [Future View + Domino]
    │                        ├─ "Reschedule" ──► [Reschedule sheet]
    │                        ├─ "Start Focus to Complete" ──► Focus (untuk tugas ini)
    │                        └─ "Delete Task"
    ├─ (ikon Tune) ──► [Settings]
    │                      ├─ Manage ──► [App Categories]
    │                      └─ Manage ──► [Blocked Apps]
    └─ (banner overdue) ──► Reschedule / Keep for Today / Open

Kontekstual (lintas layar):
  • [Intervention in-app]  (overlay sheet) ── "Start Focus" / "Later" (cooldown 2h) / "Not now"
  • [Notifikasi intervention]             ── "Start Focus" / "Later"
  • [Overlay "Back to focus?"]            ── saat membuka app terblokir (Strict/Custom)
  • [Notifikasi Sesi Fokus]               ── ongoing selama timer berjalan
```

---

## 3. Peta Ekspresi Maskot Nemo

1. **Neutral (`mascot_neutral`):** Mata melirik ke samping, mulut datar.
   - Splash (0–1.5s), Header Tasks siang & malam, Focus Active saat berjalan, Notifikasi Fokus.
2. **Happy (`mascot_happy`):** Mata melengkung, senyum lebar.
   - Splash (1.5–3.5s memantul pelan), Onboarding Profil (nickname terisi), Onboarding Izin (semua aktif), Skenario A Future View ("IF I START NOW"), Penegasan privasi di Settings.
3. **Sad (`mascot_sad`):** Alis turun, air mata, mulut melengkung bawah.
   - Banner Overdue di Tasks, Sesi fokus diakhiri lebih awal, Intervensi Level 2 (Contextual), Tombol "Delete all data" di Settings.
4. **Angry (`mascot_angry`):** Alis menukik tajam, urat dahi, gigi rapat.
   - Overlay "Back to focus?" (menegur halus membuka app paused), Skenario B Future View ("IF I KEEP DELAYING").
5. **Surprised (`mascot_surprised`):** Mata bulat, mulut "O", garis kejut. Cadangan.
6. **Sleepy (`mascot_sleepy`):** Mata setengah terpejam, simbol "zzz".
   - Wellbeing no data, Focus Active saat tombol Pause ditekan, Insights empty state, App Categories empty state.
7. **Confused (`mascot_confused`):** Satu alis terangkat, mulut gelombang, tanda "?".
   - Focus Setup fallback notice (blocking unavailable), Notice circular dependency di Domino Effect, Blocked Apps empty state.
8. **Laughing (`mascot_laughing`):** Mata terpejam menyipit, mulut tertawa terbuka.
   - Kartu progress Tasks mencapai 100%, Selesai sesi fokus penuh (effort verified).
9. **Shy (`mascot_shy`):** Mata menghindar, pipi merona.
   - Onboarding Izin (meminta akses dengan sopan), Wellbeing permission prompt.
10. **Thinking (`mascot_thinking`):** Mata melirik ke atas, tanda "...".
    - Callout Future View di Task Detail, Intervensi Level 1 (Gentle), Focus Setup saat daftar tugas kosong.
11. **Wink (`mascot_wink`):** Satu mata berkedip, senyum kecil. Alternatif idle.
12. **Talking (`mascot_talking`):** Mulut terbuka oval berbicara.
    - Tasks empty state ("No tasks yet"), Onboarding Profil greeting, INSIGHT quote card, Intervensi Level 4 (Focus).

**Aset Khusus:**
- `mascot_blink`: Animasi idle berkedip (150ms tiap 3–5 detik acak).
- `mascot_silhouette`: Ikon siluet monokrom status bar notifikasi & themed icon Android 13+.
- `app_icon`: Neutral di tengah kanvas berlatar putih dengan safe zone 66%.

---

## 4. Rincian 19 Halaman

### 4.1 Splash Screen
- **Mekanisme:** Rangkaian gambar maskot Compose (tanpa pemutar video).
  - 0.0s – 1.5s: `mascot_neutral`, berkedip sekali.
  - 1.5s – 3.5s: `mascot_happy` sambil memantul lembut vertikal via modifier Compose.
  - Teks "Nemo" di bawah maskot.
- **Tema:** Light (maskot di latar putih) dan Night (maskot dalam lingkaran badge putih di atas latar `#0A0A0A`).
- **Timeout:** Batas maksimal 4.5 detik memaksa navigasi jika ada gangguan rendering.

### 4.2 Onboarding — Profil (Setup 2 of 3)
- **Tujuan:** Data preferensi awal non-wajib.
- **Komponen:**
  - Maskot besar `Talking` → `Happy` saat input nickname terisi.
  - Header "Setup 2 of 3" + ProgressTrack 66%.
  - Judul "Let's make Nemo yours."
  - TextField Nickname (maksimal 24 karakter, auto-save).
  - Chip Main Focus: `College` • `Career` • `Personal Growth`.
  - Chip Focus Length: `25 min` • `45 min` • `60 min` (default 25).
  - Tombol "Continue" & Teks tombol "Skip for now".

### 4.3 Onboarding — Izin (Setup 3 of 3)
- **Tujuan:** Penjelasan izin sistem tanpa paksaan.
- **Komponen:**
  - Maskot besar `Shy` → `Happy` saat semua izin aktif.
  - Header "Setup 3 of 3" + ProgressTrack 100%.
  - Tiga kartu izin: `Usage access`, `Notifications`, `App blocking` (dengan tombol "Enable" atau pill "Enabled").
  - Kartu penegasan privasi lokal ("All data is stored locally on your phone.").
  - Tombol "Continue" & Teks tombol "Skip for now".

### 4.4 Tasks (Home)
- **Tujuan:** Pusat manajemen task harian.
- **Komponen:**
  - Header greeting ("Good morning, [Nickname]") dengan maskot kecil `Happy`/`Neutral`.
  - Ikon Tune menuju Settings.
  - Week strip 7 hari (pill ink pada hari ini).
  - Overdue banner (jika ada task tertunda) dengan maskot kecil `Sad` + tombol "Review & Reschedule" & "Dismiss".
  - Kartu "Today's progress" (angka besar, track progress, maskot `Laughing` saat 100%).
  - Segmented filters: `Today` • `Upcoming` • `Overdue` • `Completed`.
  - Task row timeline: status node centang, judul dicoret jika selesai, priority pill, durasi/deadline.
  - FAB `+` bulat ink di kanan bawah (100dp dari dasar).
  - Empty state: Maskot besar `Talking` + "No tasks yet - plan your day."

### 4.5 Add Task (Bottom Sheet)
- **Tujuan:** Pembuatan tugas baru dengan judul wajib.
- **Komponen:** Header "New Task" + X close, input title, input description, priority chips, recurrence chips (`None`/`Daily`/`Weekly`), deadline picker (wheel dialog), estimated duration picker (wheel dialog), category input, subtask checklist input, tombol "Save Task".

### 4.6 Task Detail
- **Tujuan:** Tinjauan mendalam satu tugas & trigger focus session.
- **Komponen:** Tombol Back, judul tugas, badge priority & IN PROGRESS, baris meta deadline & duration, progress track subtask, daftar checklist steps (quick add step), callout "See what happens if you delay" (maskot kecil `Thinking`), tombol "Start Focus to Complete", tombol "Reschedule", tombol danger "Delete Task".

### 4.7 Reschedule (Bottom Sheet)
- **Tujuan:** Penjadwalan ulang tenggat secara tenang.
- **Komponen:** Header "Reschedule" + X, Day chips (`Today` • `Tomorrow` • `+3 Days` • `+7 Days`), Time chips (`23:00`, `09:00`, dll.), tombol "Move deadline", teks tombol "Clear deadline".

### 4.8 Wellbeing
- **Tujuan:** Visualisasi objektif screen-time lokal.
- **Komponen:** Judul "Wellbeing", kartu Screen time harian (angka besar Fraunces + delta kemarin), kartu TODAY AT A GLANCE (panah tren vs average & distraction share), list TOP APPS (sampai 3 app teratas), barchart THIS WEEK (interaktif per hari), disclaimer privasi lokal.
- **Empty / Error:** Belum ada izin: Maskot besar `Shy` + "See your screen time" + tombol "Open Settings". Belum ada data: Maskot besar `Sleepy` + "No usage data yet".

### 4.9 Focus — Setup
- **Tujuan:** Konfigurasi parameter sesi fokus sebelum mulai.
- **Komponen:** Judul "Focus session" + maskot kecil `Neutral`, notice fallback jika blocking mati (maskot kecil `Confused`), pemilihan tugas yang belum selesai, selector mode (`Gentle` vs `Strict`), selector durasi (wheel dialog), tombol "Begin Focus". Empty task: Maskot besar `Thinking`.

### 4.10 Focus — Active
- **Tujuan:** Menjalankan timer fokus berintegritas tinggi.
- **Komponen:** Pill "FOCUS" + maskot kecil `Neutral`, judul tugas, TimerRing Fraunces tebal + label remaining/paused, notice fallback mode, display current step, tombol "Pause"/"Resume", tombol "Finish" (membuka konfirmasi), tautan "Emergency exit".
- **Maskot:** Berjalan: `Neutral` berkedip · Pause: `Sleepy` · Finish verified penuh: `Laughing` · End early: `Sad`.

### 4.11 Overlay "Back to focus?"
- **Tujuan:** Nudge sistem saat app terblokir dibuka pada mode Strict.
- **Komponen:** Dialog melayang di atas app terblokir berlatar scrim, maskot sedang `Angry`, judul "Back to focus?", teks penjelasan pilihan otonom, tombol "Back to focus", teks link "Emergency exit".

### 4.12 Notifikasi Sesi Fokus
- **Tujuan:** Status foreground ongoing session.
- **Komponen:** Small icon `mascot_silhouette`, large icon `Neutral`, title "Focus session in progress", timer countdown, aksi "Emergency exit".

### 4.13 Future View + Domino Effect
- **Tujuan:** Menampilkan proyeksi waktu jika menunda vs mulai sekarang.
- **Komponen:** Header Back, judul "What happens next?", disclaimer jadwal, Skenario A "IF I START NOW" (maskot kecil `Happy`), Skenario B "IF I KEEP DELAYING" (maskot kecil `Angry`), notice circular link (maskot kecil `Confused`), diagram rantai Domino Effect, kandidat penautan task, tombol "Start Focus Now".

### 4.14 Insights — Daily Reflection
- **Tujuan:** Refleksi harian penutup hari yang menenangkan.
- **Komponen:** Header tanggal, statistik YOUR DAY (Tasks, Focus, Screen Time, Longest distraction), blok INSIGHT (maskot kecil `Talking`), pola terdeteksi, blok SUGGESTION, atribusi sumber data lokal/AI, tombol Refresh. Empty state: Maskot besar `Sleepy`.

### 4.15 Intervention (In-App Overlay)
- **Tujuan:** Kartu intervensi halus saat mendeteksi drift di dalam aplikasi.
- **Komponen:** Sheet melayang dari bawah dengan maskot sedang menempel di tepi atas kartu:
  - Level 1 Gentle: `Thinking`
  - Level 2 Contextual: `Sad`
  - Level 3 Future: `Happy`
  - Level 4 Focus: `Talking`
  - Aksi: "Start Focus", "Later" (cooldown 2 jam), "Not now" (tutup tanpa cooldown).

### 4.16 Notifikasi Intervention
- **Tujuan:** Intervensi saat user di luar aplikasi.
- **Komponen:** Channel "Gentle check-ins", Small icon `mascot_silhouette`, Large icon sesuai level, aksi "Start Focus" dan "Later".

### 4.17 Settings
- **Tujuan:** Kontrol privasi, profil, AI, dan data pengguna.
- **Komponen:** Header Settings, seksi Profile (nickname, focus, length), seksi Permissions (status & pintasan), seksi Privacy & AI (toggle AI & Usage), seksi App categories ("Manage"), seksi Blocked apps ("Manage"), toggle Focus blocking, seksi Data controls (retensi & "Delete all data" dengan maskot kecil `Sad`), penegasan privasi lokal dengan maskot kecil `Happy`.

### 4.18 App Categories (Sub-halaman)
- **Tujuan:** Pengelompokan aplikasi & penanda distraksi manual.
- **Komponen:** Search bar, daftar aplikasi terpasang, editor kategori, toggle "Treat as distraction". Empty state: Maskot besar `Sleepy`.

### 4.19 Blocked Apps (Sub-halaman)
- **Tujuan:** Memilih aplikasi yang dibatasi selama mode Strict.
- **Komponen:** Search bar, daftar aplikasi dengan switch on/off (aplikasi terblokir diposisikan di atas). Empty state: Maskot besar `Confused`.
