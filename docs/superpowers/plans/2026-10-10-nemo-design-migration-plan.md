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

- [ ] **1.1** Salin font: `assets/fonts/manrope_variable.ttf` dan
      `assets/fonts/fraunces_variable.ttf` → `app/src/main/res/font/`.
- [ ] **1.2** `core/design/theme/Color.kt`: ganti `AnchorColors` dengan `NemoColors`
      (data class) + `NemoDay` + `NemoNight` sesuai spec §4.1.
- [ ] **1.3** `core/design/theme/Type.kt`: `ManropeFamily` + `FrauncesFamily`
      (variable, `FontVariation.Settings`) + `NemoTypography` (spec §4.2).
      Hapus `InterFamily` dan `AnchorType`.
- [ ] **1.4** `core/design/theme/Shape.kt` (baru): `NemoShapes` + `SheetShape`
      (input 18, kartu 24, dialog 30, sheet atas 32, xl 32).
- [ ] **1.5** `core/design/theme/Spacing.kt`: nilai NEMO (4/8/12/16/24/32/48,
      `screenH`=24, `section`=28). Nama `Spacing` dipertahankan.
- [ ] **1.6** `core/design/theme/Radius.kt`: sesuaikan ke skala NEMO (pill penuh untuk
      button/chip; input 18; kartu 24; track 3). Nama `Radius` dipertahankan.
- [ ] **1.7** `core/design/theme/Motion.kt`: durasi 150/250/400/600, easing
      `CubicBezier(0.22f, 1f, 0.36f, 1f)`.
- [ ] **1.8** `core/design/theme/Theme.kt`: `NemoTheme(darkTheme = isSystemInDarkTheme())`,
      `Nemo.colors` / `Nemo.spacing`, `LocalNemoColors` / `LocalNemoSpacing`.
      Hapus `AnchorElevation` / `LocalAnchorElevation` / `AnchorTheme` / `AnchorTypography`.
- [ ] **1.9** Tambah `@Preview` contoh (adaptasi `NemoPreviewSample.kt`) untuk cek Night.

**Gate Fase 1:** Fase 1 dan Fase 2 dikerjakan sebagai satu cutover kompilasi (rename
besar membuat build sementara merah di tengah). Build pertama dijalankan setelah Fase 2
selesai: `assembleDebug` harus hijau dengan preview komponen, lalu lanjut memperbaiki
call-site layar di Fase 3+.

---

## Fase 2 — Komponen dasar (adaptasi di tempat)

**Tujuan:** semua komponen memakai token NEMO; nama publik dipertahankan agar call-site
layar tidak berubah (kecuali yang di-rename eksplisit).

- [ ] **2.1** `PrimaryButton.kt`: `PrimaryButton`/`SecondaryButton` → pill (999),
      height 54, token NEMO. Hapus pola soft-3D Base/Front + `LocalAnchorElevation`.
- [ ] **2.2** `SegmentedChips.kt`: pill; selected = ink.
- [ ] **2.3** `TextPill.kt` → `NemoPill.kt`: netral (`fog`) / filled (`ink`), label kapital.
- [ ] **2.4** `AnchorSwitch.kt` → `NemoSwitch.kt`: track ink saat on.
- [ ] **2.5** `ProgressTrack.kt`: track 3dp, animasi 600ms.
- [ ] **2.6** `TimerRing.kt`: angka Fraunces besar, label `remaining`/`paused`.
- [ ] **2.7** `TaskCard.kt` → `NemoTaskRow.kt`: node 20dp; selesai = ink + centang + coret.
- [ ] **2.8** `ListItemPicker.kt` + baris list → `NemoListRow`: judul + subtitle + trailing.
- [ ] **2.9** `SectionHeader.kt` → `NemoSectionLabel` (simpan `Section` wrapper).
- [ ] **2.10** `AnchorNavBar.kt` → `NemoNavBar.kt`: tinggi 76, aktif = ink + tebal,
      tanpa indikator pill.
- [ ] **2.11** `AddTaskFab.kt` → `NemoFab.kt`: bulat ink, 100dp dari bawah.
- [ ] **2.12** `AnchorBottomSheet.kt` → `NemoBottomSheet.kt`: `surface` + scrim,
      handle `line`.
- [ ] **2.13** `Divider.kt`, `Banner.kt`, `AppIcon.kt`, `Interactions.kt`,
      `WheelPicker.kt`, `WheelPickerDialog.kt`: token & radius NEMO.
- [ ] **2.14** Tambah `NemoTextField` (adaptasi dari input yang ada / dari kit).
- [ ] **2.15** Ikon: impor 9 SVG dari `assets/icons/` sebagai Vector Asset
      (`ic_add`, `ic_back`, `ic_close`, `ic_focus`, `ic_insights`, `ic_lock`,
      `ic_tasks`, `ic_tune`, `ic_wellbeing`).

**Gate Fase 2:** `assembleDebug` hijau (setelah call-site layar diperbaiki di Fase 3+;
lihat catatan Fase 1).

---

## Fase 3 — Shell + alur inti (Tasks, Add Task, Task Detail)

**Tujuan:** navigasi & alur inti tampil dengan NEMO.

- [ ] **3.1** `MainActivity.kt`: `AnchorTheme`→`NemoTheme`, `AnchorColors.surface`→
      `Nemo.colors.background`.
- [ ] **3.2** `navigation/AnchorNavHost.kt` → `NemoNavHost` (atau simpan nama, ganti isi):
      pakai `NemoNavBar`, `NemoFab`.
- [ ] **3.3** `core/design/navigation/TopLevelDestination.kt`: ikon dari Vector Asset NEMO.
- [ ] **3.4** `feature/tasks/TasksScreen.kt`: sapaan, week strip, progres, tab filter,
      `NemoTaskRow`, `NemoFab`; banner overdue pakai `Banner` NEMO.
- [ ] **3.5** `feature/tasks/AddTaskSheet.kt`: `NemoBottomSheet`, `NemoChip`,
      `NemoTextField`; Save nonaktif bila judul kosong.
- [ ] **3.6** `feature/tasks/TaskDetailScreen.kt`: pill, node, callout, delete = danger.
- [ ] **3.7** `feature/tasks/RescheduleSheet.kt`: `NemoChip`, `WheelPicker` NEMO.

**Gate Fase 3:** `assembleDebug` + `testDebugUnitTest` hijau. Cek visual Tasks/Add/Detail
pada sistem terang & gelap.

---

## Fase 4 — Focus

- [ ] **4.1** `feature/focus/FocusScreen.kt`: Focus Setup (pilih tugas, mode, durasi,
      Begin Focus) + Focus Active (`TimerRing`, Pause/Finish, Emergency exit).
- [ ] **4.2** `feature/intervention/InterventionOverlay.kt`: dialog/sheet NEMO.
- [ ] **4.3** `feature/focus` overlay "Back to focus?": dialog tengah di atas scrim.
- [ ] **4.4** `platform/service/FocusService.kt`: notifikasi sesi fokus pakai
      `ic_stat_nemo` + channel/warna NEMO.

**Gate Fase 4:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 5 — Wellbeing + Insights

- [ ] **5.1** `feature/wellbeing/WellbeingScreen.kt`: screen time besar, bar 7 hari,
      top 3 app, catatan privasi; empty state asli.
- [ ] **5.2** `feature/insights/InsightsScreen.kt`: tanggal, statistik, kutipan (Fraunces),
      pola, saran; sembunyikan blok tanpa data.

**Gate Fase 5:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 6 — Onboarding + Settings + sub-halaman

- [ ] **6.1** `feature/onboarding/OnboardingScreen.kt`: Profile Setup + Permission
      Onboarding (status "Enabled", catatan privasi).
- [ ] **6.2** `feature/settings/SettingsScreen.kt`: `NemoListRow`, `NemoSwitch`,
      `NemoPill`; opt-in default off; "Delete all data" danger di bawah.
- [ ] **6.3** `feature/settings/AppCategoriesScreen.kt`: search, list row, toggle.
- [ ] **6.4** `feature/settings/BlockedAppsScreen.kt`: search, terblokir di atas lalu alfabet.

**Gate Fase 6:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 7 — Intervention, Future View, notifikasi

- [ ] **7.1** `feature/intervention/*`: sheet per level, notifikasi intervention
      (channel "Gentle check-ins").
- [ ] **7.2** `feature/futureview/FutureViewScreen.kt`: dua skenario, domino, Start Focus Now.
- [ ] **7.3** `platform/notification/*`: warna & ikon notifikasi NEMO.

**Gate Fase 7:** `assembleDebug` + `testDebugUnitTest` hijau.

---

## Fase 8 — Verifikasi tema Day + aset

- [ ] **8.1** Uji semua 19 layar pada sistem terang (Day) & gelap (Night); perbaiki
      warna hardcode yang tersisa.
- [ ] **8.2** Aset launcher: salin `mipmap-*` dari `assets/android-launcher/` ke `res/`;
      `mipmap-anydpi-v26/*.xml` (adaptive + monochrome); pastikan manifest
      `@mipmap/ic_launcher` + `@mipmap/ic_launcher_round`.
- [ ] **8.3** `res/values/colors.xml` + `res/values-night/colors.xml`: token
      `nemo_*` + `ic_launcher_background`.
- [ ] **8.4** Ikon notifikasi `ic_stat_nemo.png` → `res/drawable`; pasang di notifikasi.
- [ ] **8.5** Logo NEMO di splash.

**Gate Fase 8:** `assembleDebug` hijau; ikon & splash sesuai.

---

## Fase 9 — Pembersihan (hapus Inter, rename penuh)

- [ ] **9.1** Hapus `res/font/inter_*.ttf` (4 file) setelah tidak direferensikan.
- [ ] **9.2** Rename sisa identifier `Anchor*` → `Nemo*` (grep `Anchor` menyeluruh
      di `core/design/` dan `feature/`).
- [ ] **9.3** Hapus token mati (mis. `depthBaseLight/Dark`, `hero*`, `AnchorType`).

**Gate Fase 9:** `grep` tidak menemukan `AnchorColors`/`InterFamily`/`AnchorTheme`/
`AnchorElevation`; `assembleDebug` + `testDebugUnitTest` hijau.

---

## Verifikasi Akhir

- [ ] `.\gradlew.bat :app:testDebugUnitTest` — semua tes lama lulus.
- [ ] `.\gradlew.bat :app:assembleDebug` — hijau.
- [ ] `grep -r "Anchor\|InterFamily" app/src/main/java/com/anchor/app/core/design` — kosong.
- [ ] Semua 19 layar dicek pada tema terang & gelap.
- [ ] `README`/`RELEASE`/dokumen desain lama tetap ada (tidak dihapus) kecuali diminta.
