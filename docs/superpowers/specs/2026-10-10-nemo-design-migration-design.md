# NEMO Design Migration — Design Spec

> **Status:** Approved in brainstorming · **Version:** 1.0
> **Tanggal:** 2026-10-10
> **Dokumen terkait:** `docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md`
> (spesifikasi visual sumber), `docs/superpowers/design/06-design-system.md` (sistem lama "Anchor"),
> `docs/superpowers/design/07-screens.md` (inventaris layar)
> **Menggantikan:** Design System "Anchor" (Inter + soft-3D, light-first)

Dokumen ini mendefinisikan **migrasi lapisan visual** aplikasi dari sistem desain
**Anchor** ke sistem desain **NEMO**. Semua detail token, komponen, dan aturan visual
mengacu pada `newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md` sebagai sumber
kebenaran. Dokumen ini fokus pada **bagaimana** menerapkannya di aplikasi
`com.anchor.app` tanpa mengubah perilaku.

---

## 1. Tujuan & Kriteria Sukses

**Tujuan:** Aplikasi tampil persis sesuai bahasa visual NEMO (tenang, kalem, monokrom,
datar, tipografi Fraunces + Manrope) menggantikan Anchor (Inter, soft-3D, light-first).

**Yang TIDAK berubah (di luar konteks):**
- Struktur data, entitas Room, DAO, migrasi database.
- Repository, use case, ViewModel, alur state.
- Navigasi (`AnchorNavHost`, rute, deep-link `EXTRA_OPEN_ROUTE`).
- Fitur: task/focus/wellbeing/insights/intervention/blocking/notifikasi/worker.
- Teks/konten (string resources English + `values-id`), nama fitur, perilaku interaksi.
- Nama paket `com.anchor.app`, `applicationId`, skema DB, accessibility config service.

**Kriteria sukses:**
1. `./gradlew assembleDebug` hijau.
2. `./gradlew testDebugUnitTest` tetap lulus (semua tes lama).
3. Aplikasi mengikuti tema sistem: sistem terang → NemoDay, gelap → NemoNight.
4. Semua 19 layar memakai token NEMO (tidak ada sisa referensi `AnchorColors`/`InterFamily`).
5. Tidak ada identifier `Anchor*` yang tersisa di `core/design/` (rename penuh ke Nemo*).
6. Ikon launcher, ikon notifikasi, dan logo NEMO terpasang.

---

## 2. Ruang Lingkup

Migrasi ini adalah perubahan **arsitektural** karena menyentuh fondasi tema yang
dikonsumsi seluruh ~19 layar dan ~18 komponen.

**Termasuk:**
- Token warna, tipografi, bentuk, spacing, motion.
- Tema root (`NemoTheme`) dengan Day/Night mengikuti sistem.
- Semua komponen di `core/design/components/` (adaptasi di tempat + rename).
- Penerapan ke seluruh 19 layar, overlay, sheet, dan notifikasi.
- Aset: font (Manrope + Fraunces), ikon UI SVG, ikon launcher, ikon notifikasi, logo.
- Rename identifier `Anchor*` → `Nemo*`, hapus `InterFamily` + font Inter.

**Tidak termasuk:**
- Perubahan perilaku, logika, data, atau navigasi.
- Fitur baru atau layar baru.
- Penghapusan `TaskCard`/`ListItemPicker` (dipertahankan meski belum dipakai layar).

---

## 3. Arsitektur & Strategi

**Prinsip:** Ganti *lapisan visual* saja; kode di luar `core/design/` dan `feature/`
(file UI) tidak disentuh kecuali karena rename import/komponen.

### 3.1 Struktur file tema

```
core/design/theme/
  Color.kt    → NemoColors (data class) + NemoDay + NemoNight              [GANTI]
  Type.kt     → ManropeFamily, FrauncesFamily, NemoTypography             [GANTI]
  Shape.kt    → NemoShapes, SheetShape                                    [BARU]
  Theme.kt    → NemoTheme, Nemo (colors/spacing), LocalNemo*              [GANTI]
  Spacing.kt  → Spacing (nilai NEMO)                                      [EDIT]
  Radius.kt   → Radius (dipertahankan, disesuaikan ke skala NEMO)         [EDIT]
  Motion.kt   → Motion (durasi/easing NEMO)                               [EDIT]
  Theme.kt dihapus: AnchorElevation, LocalAnchorElevation                 [HAPUS]
```

### 3.2 Strategi rename

| Lama (Anchor) | Baru (NEMO) |
|---|---|
| `AnchorColors` (object) | `NemoColors` (data class) + `NemoDay` / `NemoNight` |
| `AnchorColors.*` | `Nemo.colors.*` |
| `AnchorTheme` | `NemoTheme` |
| `AnchorTypography` | `NemoTypography` |
| `AnchorType` | dihapus; pakai `MaterialTheme.typography.*` |
| `InterFamily` | dihapus; pakai `Manrope` / `Fraunces` |
| `AnchorElevation` / `LocalAnchorElevation` | dihapus |
| `AnchorNavBar` | `NemoNavBar` |
| `AnchorSwitch` | `NemoSwitch` |
| `AnchorBottomSheet` | `NemoBottomSheet` |
| `TaskCard` | `NemoTaskRow` |
| `TextPill` | `NemoPill` |
| `PrimaryButton` / `SecondaryButton` | dipertahankan nama (API tetap) |
| `SegmentedChips` | dipertahankan nama; tampilan pill NEMO |
| `ProgressTrack` / `TimerRing` / `WheelPicker` / `Divider` / `Banner` / `AppIcon` / `Interactions` / `SectionHeader` / `AddTaskFab` / `ListItemPicker` | dipertahankan nama; tampilan disesuaikan |
| `Spacing` / `Radius` / `Motion` | nama dipertahankan; nilai disesuaikan |

`Spacing`, `Radius`, `Motion` sengaja tidak di-rename agar jumlah file yang tersentuh
tidak membengkak; hanya nilai di dalamnya yang berubah.

### 3.3 Pendekatan komponen

Komponen lama **diadaptasi di tempat** (edit tampilan + token) sambil mempertahankan
nama publik dan signature agar call-site di layar tidak error. Ini memungkinkan migrasi
layar bertahap dengan build tetap hijau di tiap langkah.

### 3.4 Tema Day/Night

`NemoTheme(darkTheme: Boolean = isSystemInDarkTheme())` memilih `NemoDay` atau
`NemoNight`, lalu membangun `ColorScheme` (light/dark) memakai token NEMO dan
menyediakan `LocalNemoColors` + `LocalNemoSpacing`. `MainActivity` membungkus
`NemoTheme { Surface(color = Nemo.colors.background) { ... } }`.

**Catatan implementasi:** karena `NemoTheme` sekarang *mengikuti sistem* (bukan lagi
memaksa terang), seluruh layar otomatis merender night saat sistem gelap. Semua warna
harus diambil dari `Nemo.colors`, tidak boleh hardcode (kecuali fallback splash yang
sudah menyesuaikan tema).

---

## 4. Sistem Visual

Seluruh nilai di bawah diambil dari `design-spec.md` §3–§5; salinan ringkas agar
implementasi mandiri.

### 4.1 Warna (`NemoColors`)

| Peran | Day | Night | Dipakai untuk |
|---|---|---|---|
| background | #FFFFFF | #0A0A0A | latar layar |
| surface | #FAFAFA | #141414 | sheet, dialog, kartu halus |
| fog | #F5F5F5 | #1B1B1B | input, track, tombol sekunder, pill netral |
| line | #E9E9E9 | #262626 | garis pemisah, border chip |
| mute | #767676 | #8A8A8A | teks sekunder, meta, ikon tidak aktif |
| placeholder | #8C8C8C | #6B6B6B | placeholder input |
| inkSecondary | #444444 | #CFCFCF | teks isi sekunder |
| ink | #111111 | #F2F2F2 | teks utama, tombol utama, node selesai, FAB |
| onInk | #FFFFFF | #0A0A0A | teks/ikon di atas ink |
| danger | #8A3B3B | #D9A0A0 | hanya "Delete Task", "Delete all data" |
| dangerLine | #EAD9D9 | #3A2A2A | border tombol danger |
| scrim | #000 @ 42% | #000 @ 67% | belakang sheet/overlay |

Aturan: prioritas & bahaya dibedakan lewat panjang bar, bobot huruf, dan label — bukan
warna menyala. Overdue **bukan** merah.

### 4.2 Tipografi

- **Fraunces** (Light 300 / Regular 400) — display & judul.
- **Manrope** (400/500/600/700) — semua teks lain.
- Variable TTF; butuh minSdk 26 (terpenuhi: minSdk = 26).

| Gaya M3 | Font | Berat | Ukuran/baris |
|---|---|---|---|
| displayLarge | Fraunces | 300 | 46/52 |
| displayMedium | Fraunces | 300 | 34/40 |
| headlineLarge | Fraunces | 300 | 30/36 |
| headlineMedium | Fraunces | 400 | 22/28 |
| titleLarge | Fraunces | 400 | 19/26 |
| titleMedium | Manrope | 600 | 15/22 |
| bodyLarge | Manrope | 400 | 14/22 |
| bodyMedium | Manrope | 400 | 13/20 |
| bodySmall | Manrope | 400 | 12/18 |
| labelLarge | Manrope | 600 | 14/20 |
| labelMedium | Manrope | 600 | 12/16 |
| labelSmall | Manrope | 600, kapital | 10.5/14, tracking +1.3sp |

### 4.3 Bentuk (radius)

Tombol/chip/pill = penuh (999) · input 18 · kartu 24 · dialog 30 · sheet atas 32 ·
track progres 3 (tinggi) · ring 220 (diameter) / 6 (stroke).

### 4.4 Spacing

4 · 8 · 12 · 16 · 24 · 32 · 48. Margin horizontal layar **24** (`screenH`). Jarak antar
bagian **28** (`section`).

### 4.5 Ukuran

Tombol 54 (kecil 34) · input 54 · FAB 56 · bottom bar 76 · node tugas 20 · toggle
40×24. **Area sentuh minimum 48dp**; tombol kecil 34 pakai perluasan sentuh
(`Interactions.minTouchTarget`).

### 4.6 Gerak

Tekan 150 · transisi 250 · sheet 400 · progres 600. Easing
`CubicBezier(0.22, 1, 0.36, 1)`. Tanpa bounce/goyang/kedip. Sheet naik dari bawah,
overlay fade.

### 4.7 Elevasi

Praktis datar. Shadow hanya pada FAB. Sheet/dialog dibedakan oleh `surface` + scrim.
`AnchorElevation`/`LocalAnchorElevation` dihapus.

---

## 5. Komponen

Semua berkas di `core/design/components/`. Nama publik dipertahankan kecuali yang
di-rename eksplisit (§3.2).

| Komponen | Perubahan NEMO | Dipakai di |
|---|---|---|
| `NemoButton` (PrimaryButton) | Pill (999), 54dp (kecil 34); Primary/Secondary/Outline/Danger/Text; disabled 35% | semua layar |
| `SecondaryButton` | fog/outline pill | Focus pause |
| `NemoChip` (SegmentedChips) | pill; selected = ink | Add Task, Reschedule, Focus Setup |
| `NemoPill` (TextPill) | netral (fog) / filled (ink), label kapital | Task Detail, Settings, status |
| `NemoSwitch` (AnchorSwitch) | track ink saat on | Settings, App Categories, Blocked Apps |
| `NemoTextField` | latar fog, tanpa garis bawah | Onboarding, App Categories, Add Task |
| `ProgressTrack` | track 3dp, animasi 600ms | Tasks, Wellbeing |
| `TimerRing` | angka Fraunces besar di tengah, label "remaining"/"paused" | Focus Active |
| `NemoTaskRow` (TaskCard) | node 20dp; selesai = isian ink + centang + coret | Tasks, Task Detail |
| `NemoListRow` (ListItemPicker + row) | judul + subtitle + trailing | Settings, Izin, Blocked Apps |
| `NemoSectionLabel` (SectionHeader) | label kapital kecil | semua layar |
| `NemoBottomBar` (AnchorNavBar) | 4 tab, aktif = ink + tebal, tanpa indikator pill | shell navigasi |
| `NemoFab` (AddTaskFab) | bulat ink, 100dp dari bawah | Tasks |
| `NemoBottomSheet` (AnchorBottomSheet) | surface + scrim, handle `line` | Add Task, Reschedule, Intervention |
| `WheelPicker` | dipertahankan; warna disesuaikan | Add Task, Reschedule, Focus Setup |
| `Divider` / `Banner` / `AppIcon` / `Interactions` | warna & radius disesuaikan | lintas layar |

`TaskCard` dan `ListItemPicker` dipertahankan (dipakai sebagian layar / disiapkan) dan
di-rename sesuai tabel.

---

## 6. Layar (19)

Hanya lapisan visual yang berubah, perilaku & navigasi tetap. Rujukan komposisi:
`design-spec.md` §7.

1. **Splash** — animasi maskot Compose (`Neutral` 1,5s → `Happy` 2s), ikuti tema sistem (±3,5 dtk, maks 4,5 dtk, tanpa video player).
2. **Onboarding Profil** — "Setup 2 of 3", track, 3 isian (semua opsional).
3. **Onboarding Izin** — judul + 3 baris izin + catatan privasi; status "Enabled".
4. **Tasks** — sapaan, week strip, progres, tab filter, daftar tugas, FAB.
5. **Add Task** — sheet: judul, deskripsi, prioritas, tenggat/durasi, Save.
6. **Task Detail** — back, judul, pill, meta, steps, callout, aksi; delete = teks danger.
7. **Reschedule** — sheet: Day, Time, Move deadline.
8. **Wellbeing** — screen time besar, bar 7 hari, top 3 app, catatan privasi.
9. **Focus Setup** — pilih tugas, Mode (Gentle/Strict), durasi, Begin Focus.
10. **Focus Active** — pill FOCUS, nama tugas, ring, step aktif, Pause/Finish.
11. **Overlay "Back to focus?"** — dialog tengah di atas scrim.
12. **Notifikasi sesi fokus** — ongoing, prioritas rendah, aksi "Emergency exit".
13. **Future View** — dua skenario, domino, Start Focus Now.
14. **Insights** — tanggal, statistik, kutipan, pola, saran.
15. **Intervention in-app** — sheet per level, "Awareness only", Start Focus, Later, Not now.
16. **Notifikasi intervention** — judul + isi per level; channel "Gentle check-ins".
17. **Settings** — Profile, Permissions, Privacy & AI, Manage apps, Data.
18. **App Categories** — search, daftar app + kategori + Treat as distraction.
19. **Blocked Apps** — search, app terblokir di atas lalu alfabet.

State kosong dipertahankan apa adanya (teks asli dari `design-spec.md` §7).

---

## 7. Aset

1. **Font:** salin `assets/fonts/manrope_variable.ttf` dan `fraunces_variable.ttf` ke
   `app/src/main/res/font/`. (SIL OFL — aman dibundel.)
2. **Ikon UI:** impor 9 SVG di `assets/icons/` (garis 1.6) sebagai Vector Asset:
   `ic_add`, `ic_back`, `ic_close`, `ic_focus`, `ic_insights`, `ic_lock`, `ic_tasks`,
   `ic_tune`, `ic_wellbeing`; menggantikan `Icons.Rounded.*` yang setara.
3. **Ikon launcher:** salin `mipmap-*` dari `assets/android-launcher/` ke `res/`, dan
   `mipmap-anydpi-v26/*.xml` (adaptive + monochrome) ke `res/mipmap-anydpi-v26/`.
   Pastikan manifest memakai `@mipmap/ic_launcher` dan `@mipmap/ic_launcher_round`.
   `ic_launcher_background` = putih (day) / hitam (night) via `values`/`values-night`.
4. **Ikon notifikasi:** pakai `ic_stat_nemo.png` sebagai small icon notifikasi.
5. **Maskot & Logo:** Aset ekspresi Maskot Nemo (12 ekspresi) dan launcher/notifikasi di layar splash dan alur 19 halaman.

---

## 8. Urutan Implementasi

Mengikuti `design-spec.md` §10, disesuaikan dengan dependensi proyek:

1. **Fondasi:** salin font; tulis `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`,
   `Radius.kt`, `Motion.kt`, `Theme.kt` (NEMO). Verifikasi via preview Night.
2. **Komponen dasar:** NemoButton, NemoChip, NemoPill, NemoTextField, NemoListRow,
   NemoSwitch, NemoSectionLabel, ProgressTrack, Divider, Banner, Interactions.
3. **Alur inti:** Tasks (TaskRow, FAB, BottomBar), Add Task, Task Detail.
4. **Focus:** Focus Setup, Focus Active (TimerRing), overlay, notifikasi sesi.
5. **Wellbeing + Insights.**
6. **Onboarding + Settings + sub-halaman** (App Categories, Blocked Apps).
7. **Intervention in-app + notifikasi intervention.**
8. **Future View.**
9. **Verifikasi tema Day** (semua layar pada sistem terang).
10. **Aset:** launcher, notifikasi, logo.
11. **Pembersihan:** hapus Inter TTF + `InterFamily`; rename penuh `Anchor*` → `Nemo*`;
    hapus `AnchorElevation`.

Build `assembleDebug` dijalankan minimal setelah langkah 1, 3, 7, dan 11.

---

## 9. Aksesibilitas

- Kontras teks ≥ 4,5:1; ikon/border ≥ 3:1.
- Semua ikon-saja punya `contentDescription`.
- Prioritas tidak hanya lewat warna — pill selalu memuat teks.
- Dukung font scaling s/d 200%: pakai `sp`, hindari tinggi tetap pada baris berteks.
- Urutan fokus keyboard/TalkBack atas→bawah, tombol utama terakhir.

---

## 10. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| Volume rename besar (375 `AnchorColors`, 202 `InterFamily`) menyebabkan build pecah | Kerjakan per-file bertahap; build hijau tiap langkah |
| Ganti light-first → ikuti sistem mengubah tampilan default | Uji pada terang & gelap; semua warna dari `Nemo.colors` |
| Variable font tidak didukung | minSdk 26 terpenuhi; gunakan `FontVariation.Settings` seperti kit |
| API Material3 (`HorizontalDivider`) belum ada | Cek versi Material3 dari BOM; sesuaikan bila perlu |
| Komponen di kit belum dikompilasi di proyek | Mulai dari preview, sesuaikan versi library |

---

## 11. Referensi Silang

- Spesifikasi visual sumber → `newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md`
- Token → `newdesign/nemo-design-kit/nemo-kit/tokens/nemo-tokens.{json,css}`
- Tema & komponen kit → `newdesign/nemo-design-kit/nemo-kit/compose/`
- Mockup → `newdesign/nemo-design-kit/nemo-kit/preview/nemo-night-recommended.html`
- Sistem lama → `docs/superpowers/design/06-design-system.md`
- Inventaris layar → `docs/superpowers/design/07-screens.md`
