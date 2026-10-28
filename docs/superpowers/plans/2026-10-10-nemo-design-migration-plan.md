# Implementation Plan — NEMO Design Migration

> **Spec:** `docs/superpowers/specs/2026-10-10-nemo-design-migration-design.md`
> **Sumber visual:** `docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md`
> **Tanggal:** 2026-10-10
> **Metode:** Migrasi visual bertahap; build hijau di tiap fase.
> **Gerbang:** Setiap fase berakhir hijau (`testDebugUnitTest` + `assembleDebug`) sebelum lanjut.
> **Prinsip:** Hanya lapisan visual. Data, ViewModel, navigasi, fitur, teks TIDAK berubah.

---

## Prasyarat & Konvensi

- Nama paket tetap `com.anchor.app`. **Tidak** mengubah `applicationId`, skema DB,
  accessibility service, atau string resources.
- Semua warna diambil dari `Nemo.colors`. Tidak hardcode warna baru di layar.
- Verifikasi tiap fase: `.\gradlew.bat :app:testDebugUnitTest` lalu `.\gradlew.bat :app:assembleDebug`.
- Sumber kit (referensi, jangan diubah):
  `compose/theme/{Color,Type,Shape,Theme}.kt`, `compose/components/NemoComponents.kt`,
  `assets/fonts/*.ttf`, `assets/icons/*.svg`, `assets/android-launcher/**`.
- Rename `Anchor*` → `Nemo*` dikerjakan penuh (keputusan brainstorming). Lakukan per-file
  agar build tetap bisa diperbaiki bertahap.

---

## Fase 1 — Fondasi tema (font + token + theme)

**Tujuan:** tema NEMO tersedia dan dikompilasi, tanpa mengubah layar dulu.

- [x] **1.1** Salin font: `assets/fonts/manrope_variable.ttf` dan
      `assets/fonts/fraunces_variable.ttf` → `app/src/main/res/font/`.
- [x] **1.2** `core/design/theme/Color.kt`: ganti `AnchorColors` dengan `NemoColors`
      (data class) + `NemoDay` + `NemoNight` sesuai spec §4.1.
- [x] **1.3** `core/design/theme/Type.kt`: `ManropeFamily` + `FrauncesFamily`
      (variable, `FontVariation.Settings`) + `NemoTypography` (spec §4.2).
      Hapus `InterFamily` dan `AnchorType`.
- [x] **1.4** `core/design/theme/Shape.kt` (baru): `NemoShapes` + `SheetShape`
      (input 18, kartu 24, dialog 30, sheet atas 32, xl 32).
- [x] **1.5** `core/design/theme/Spacing.kt`: nilai NEMO (4/8/12/16/24/32/48,
      `screenH`=24, `section`=28). Nama `Spacing` dipertahankan.
- [x] **1.6** `core/design/theme/Radius.kt`: sesuaikan ke skala NEMO (pill penuh untuk
      button/chip; input 18; kartu 24; track 3). Nama `Radius` dipertahankan.
- [x] **1.7** `core/design/theme/Motion.kt`: durasi 150/250/400/600, easing
      `CubicBezier(0.22f, 1f, 0.36f, 1f)`.
- [x] **1.8** `core/design/theme/Theme.kt`: `NemoTheme(darkTheme = isSystemInDarkTheme())`,
      `Nemo.colors` / `Nemo.spacing`, `LocalNemoColors` / `LocalNemoSpacing`.
      Hapus `AnchorElevation` / `LocalAnchorElevation` / `AnchorTheme` / `AnchorTypography`.
- [x] **1.9** Tambah `@Preview` contoh (adaptasi `NemoPreviewSample.kt`) untuk cek Night.

**Gate Fase 1:** Fase 1 dan Fase 2 dikerjakan sebagai satu cutover kompilasi (rename
besar membuat build sementara merah di tengah). Build pertama dijalankan setelah Fase 2
selesai: `assembleDebug` harus hijau dengan preview komponen, lalu lanjut memperbaiki
call-site layar di Fase 3+.

---

## Fase 2 — Komponen dasar (adaptasi di tempat)

**Tujuan:** semua komponen memakai token NEMO; nama publik dipertahankan agar call-site
layar tidak berubah (kecuali yang di-rename eksplisit).

- [x] **2.1** `PrimaryButton.kt`: `PrimaryButton`/`SecondaryButton` → pill (999),
      height 54, token NEMO. Hapus pola soft-3D Base/Front + `LocalAnchorElevation`.
- [x] **2.2** `SegmentedChips.kt`: pill; selected = ink.
- [x] **2.3** `TextPill.kt` → `NemoPill.kt`: netral (`fog`) / filled (`ink`), label kapital.
- [x] **2.4** `AnchorSwitch.kt` → `NemoSwitch.kt`: track ink saat on.
- [x] **2.5** `ProgressTrack.kt`: track 3dp, animasi 600ms.
- [x] **2.6** `TimerRing.kt`: angka Fraunces besar, label `remaining`/`paused`.
- [x] **2.7** `TaskCard.kt` → `NemoTaskRow.kt`: node 20dp; selesai = ink + centang + coret.
- [x] **2.8** `ListItemPicker.kt` + baris list → `NemoListRow`: judul + subtitle + trailing.
- [x] **2.9** `SectionHeader.kt` → `NemoSectionLabel` (simpan `Section` wrapper).
- [x] **2.10** `AnchorNavBar.kt` → `NemoNavBar.kt`: tinggi 76, aktif = ink + tebal,
      tanpa indikator pill.
- [x] **2.11** `AddTaskFab.kt` → `NemoFab.kt`: bulat ink, 100dp dari bawah.
- [x] **2.12** `AnchorBottomSheet.kt` → `NemoBottomSheet.kt`: `surface` + scrim,
      handle `line`.
- [x] **2.13** `Divider.kt`, `Banner.kt`, `AppIcon.kt`, `Interactions.kt`,
      `WheelPicker.kt`, `WheelPickerDialog.kt`: token & radius NEMO.
- [x] **2.14** Tambah `NemoTextField` (adaptasi dari input yang ada / dari kit).
- [x] **2.15** Ikon: impor 9 SVG dari `assets/icons/` sebagai Vector Asset
      (`ic_add`, `ic_back`, `ic_close`, `ic_focus`, `ic_insights`, `ic_lock`,
      `ic_tasks`, `ic_tune`, `ic_wellbeing`).

**Gate Fase 2:** `assembleDebug` hijau (setelah call-site layar diperbaiki di Fase 3+;
lihat catatan Fase 1).

---

## Fase 3 — Shell + alur inti (Tasks, Add Task, Task Detail)

**Tujuan:** navigasi & alur inti tampil dengan NEMO.

- [x] **3.1** `MainActivity.kt`: `AnchorTheme`→`NemoTheme`, `AnchorColors.surface`→
      `Nemo.colors.background`.
- [x] **3.2** `navigation/AnchorNavHost.kt` → `NemoNavHost` (atau simpan nama, ganti isi):
      pakai `NemoNavBar`, `NemoFab`.
- [x] **3.3** `core/design/navigation/TopLevelDestination.kt`: ikon dari Vector Asset NEMO.
- [x] **3.4** `feature/tasks/TasksScreen.kt`: sapaan, week strip, progres, tab filter,
      `NemoTaskRow`, `NemoFab`; banner overdue pakai `Banner` NEMO.
- [x] **3.5** `feature/tasks/AddTaskSheet.kt`: `NemoBottomSheet`, `NemoChip`,
      `NemoTextField`; Save nonaktif bila judul kosong.
- [x] **3.6** `feature/tasks/TaskDetailScreen.kt`: pill, node, callout, delete = danger.
- [x] **3.7** `feature/tasks/RescheduleSheet.kt`: `NemoChip`, `WheelPicker` NEMO.

**Gate Fase 3:** `assembleDebug` + `testDebugUnitTest` hijau. Cek visual Tasks/Add/Detail
pada sistem terang & gelap.

---

## Fase 4 — Focus

- [x] **4.1** `feature/focus/FocusScreen.kt`: Focus Setup (pilih tugas, mode, durasi,
      Begin Focus) + Focus Active (`TimerRing`, Pause/Finish, Emergency exit).
- [x] **4.2** `feature/intervention/InterventionOverlay.kt`: dialog/sheet NEMO.
- [x] **4.3** `feature/focus` overlay "Back to focus?": dialog tengah di atas scrim.
- [x] **4.4** `platform/service/FocusService.kt`: notifikasi sesi fokus pakai
      `ic_stat_nemo` + channel/warna NEMO.

**Gate Fase 4:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 5 — Wellbeing + Insights

- [x] **5.1** `feature/wellbeing/WellbeingScreen.kt`: screen time besar, bar 7 hari,
      top 3 app, catatan privasi; empty state asli.
- [x] **5.2** `feature/insights/InsightsScreen.kt`: tanggal, statistik, kutipan (Fraunces),
      pola, saran; sembunyikan blok tanpa data.

**Gate Fase 5:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 6 — Onboarding + Settings + sub-halaman

- [x] **6.1** `feature/onboarding/OnboardingScreen.kt`: Profile Setup + Permission
      Onboarding (status "Enabled", catatan privasi).
- [x] **6.2** `feature/settings/SettingsScreen.kt`: `NemoListRow`, `NemoSwitch`,
      `NemoPill`; opt-in default off; "Delete all data" danger di bawah.
- [x] **6.3** `feature/settings/AppCategoriesScreen.kt`: search, list row, toggle.
- [x] **6.4** `feature/settings/BlockedAppsScreen.kt`: search, terblokir di atas lalu alfabet.

**Gate Fase 6:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 7 — Intervention, Future View, notifikasi

- [x] **7.1** `feature/intervention/*`: sheet per level, notifikasi intervention
      (channel "Gentle check-ins").
- [x] **7.2** `feature/futureview/FutureViewScreen.kt`: dua skenario, domino, Start Focus Now.
- [x] **7.3** `platform/notification/*`: warna & ikon notifikasi NEMO.

**Gate Fase 7:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 8 — Verifikasi tema Day + aset

- [x] **8.1** Uji semua 19 layar pada sistem terang (Day) & gelap (Night); perbaiki
      warna hardcode yang tersisa.
- [x] **8.2** Aset launcher: salin `mipmap-*` dari `assets/android-launcher/` ke `res/`;
      `mipmap-anydpi-v26/*.xml` (adaptive + monochrome); pastikan manifest
      `@mipmap/ic_launcher` + `@mipmap/ic_launcher_round`.
- [x] **8.3** `res/values/colors.xml` + `res/values-night/colors.xml`: token
      `nemo_*` + `ic_launcher_background`.
- [x] **8.4** Ikon notifikasi `ic_stat_nemo.png` → `res/drawable`; pasang di notifikasi.
- [x] **8.5** Animasi Maskot Nemo (`Neutral` → `Happy`) di splash (tanpa video player).

**Gate Fase 8:** `assembleDebug` hijau; ikon & splash sesuai.

---

## Fase 9 — Pembersihan (hapus Inter, rename penuh)

- [x] **9.1** Hapus `res/font/inter_*.ttf` (4 file) setelah tidak direferensikan.
- [x] **9.2** Rename sisa identifier `Anchor*` → `Nemo*` (grep `Anchor` menyeluruh
      di `core/design/` dan `feature/`).
- [x] **9.3** Hapus token mati (mis. `depthBaseLight/Dark`, `hero*`, `AnchorType`).

**Gate Fase 9:** `grep` tidak menemukan `AnchorColors`/`InterFamily`/`AnchorTheme`/
`AnchorElevation`; `assembleDebug` + `testDebugUnitTest` hijau.

---

## Verifikasi Akhir

- [x] `.\gradlew.bat :app:testDebugUnitTest` — semua tes lama lulus.
- [x] `.\gradlew.bat :app:assembleDebug` — hijau.
- [x] `grep -r "Anchor\|InterFamily" app/src/main/java/com/anchor/app/core/design` — kosong.
- [x] Semua 19 layar dicek pada tema terang & gelap.
- [x] `README`/`RELEASE`/dokumen desain lama tetap ada (tidak dihapus) kecuali diminta.
