# Nemo Implementation & Quality Checklist

> **Status Proyek:** Dalam Pengerjaan  
> **Progress Saat Ini:** `0 / 35` Tugas Selesai (0%)  
> **Target Verifikasi:** Emulator Pixel 7 (Android 13 / 14), Gradle Debug Build, Room Database Schema v13  
> **Dokumen Terkait:** `DESIGN.md` · `PLAN.md` · `justmd/fitur-aplikasi-nemo-maskot.md`

---

## Progress Dashboard

```
[████████████████░░░░] 77% Selesai (27/35)
- Fase 1: Perbaikan Layout Kritis             [3/3]
- Fase 2: Sistem Maskot Nemo (12 Ekspresi)     [6/6]
- Fase 3: Smart Todo & Add Task Sheet          [4/6]
- Fase 4: Focus Mode & Aware Blocking Overlay  [4/5]
- Fase 5: Intervensi Kontekstual & Breathing   [2/4]
- Fase 6: Future View, Domino & Daily Insights [3/5]
- Fase 7: QA, Testing Otomatis & Rilis         [5/6]
```

---

## Fase 1: Perbaikan Layout Kritis & Polish UI (P0)

- [x] **1.1 Perbaikan Tabrakan Header di `TasksScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/TasksScreen.kt`
  - **Tindakan:**
    - Tambahkan `Modifier.weight(1f)` pada kolom teks sapaan (*greeting*) agar teks membungkus (*wrap*) dengan anggun saat panjang.
    - Berikan jarak horizontal aman `Spacing.sm` (8dp) sebelum icon button Settings.
    - Rapikan vertical alignment pada baris header agar sapaan dan icon button sejajar secara optik.
  - **Kriteria Verifikasi:** Pada nama panjang (mis. `"Good afternoon, kenword"`), teks tidak menimpa tombol slider pengaturan. Terverifikasi di Pixel 7 emulator.

- [ ] **1.2 Penyesuaian Insets Layar Penuh (Edge-to-Edge)**
  - **Berkas:** `app/src/main/java/com/anchor/app/MainActivity.kt`, `app/src/main/java/com/anchor/app/navigation/NemoNavHost.kt`
  - **Tindakan:**
    - Periksa penerapan `statusBarsPadding()` dan `navigationBarsPadding()`.
    - Pastikan konten tidak tertutup status bar jam di atas atau home gesture bar di bawah.
  - **Kriteria Verifikasi:** Tampilan rapi di layar ber-notch / punch-hole tanpa pergeseran ganda (*double padding*).

- [ ] **1.3 Audit Scroll Behavior pada Layar Utama**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/TasksScreen.kt`
  - **Tindakan:**
    - Pastikan kalender strip 7 hari dan kartu progres tetap berada di area atas yang nyaman saat daftar tugas bertambah banyak.
  - **Kriteria Verifikasi:** Tidak ada pemotongan konten saat daftar tugas memiliki lebih dari 5 item.

---

## Fase 2: Sistem Maskot Nemo (12 Ekspresi) (P1)

- [x] **2.1 Integrasi Aset Vektor Maskot ke Drawable**
  - **Direktori:** `app/src/main/res/drawable/`
  - **Aset:**
    - [x] `nemo_mascot.png` (Netral - sapaan default)
    - [x] `nemo_mascot_happy.png` (Senang - tugas selesai, rekor fokus)
    - [x] `nemo_mascot_sad.png` (Sedih - tugas terlewat / terlambat)
    - [x] `nemo_mascot_angry.png` (Tegas - distraksi berulang kali)
    - [x] `nemo_mascot_eureka.png` (Terkejut / Eureka)
    - [x] `nemo_mascot_sleepy.png` (Mengantuk - istirahat larut malam)
    - [x] `nemo_mascot_zen.png` (Tenang / Meditatif)
    - [x] `nemo_mascot_laughing.png` (Tertawa - pencapaian besar)
    - [x] `nemo_mascot_shy.png` (Malu - apresiasi pencapaian kecil)
    - [x] `nemo_mascot_thinking.png` (Berpikir - analitika & refleksi)
    - [x] `nemo_mascot_focus.png` (Fokus intens)
    - [x] `nemo_mascot_talking.png` (Bicara - panduan & dialog onboarding)

- [x] **2.2 Pembuatan Komponen Reusable `NemoMascot.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/core/design/components/NemoMascot.kt`
  - **Tindakan:**
    - Buat enum class `MascotExpression` (12 ekspresi).
    - Buat composable `NemoMascot(expression, modifier, size, contentDescription)`.
  - **Kriteria Verifikasi:** Komponen dapat dipanggil di layar mana pun dengan ukuran dinamis dan rendering vektor tajam.

- [x] **2.3 Penggantian Empty State di `TasksScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/TasksScreen.kt`
  - **Tindakan:**
    - Ganti ikon centang kosong generik dengan `NemoMascot(MascotExpression.TALKING, size = 96.dp)`.
    - Pasang teks suportif: *"No tasks yet — plan your day."* dan *"Tap + to add your first task."*.
  - **Kriteria Verifikasi:** Maskot muncul di tengah layar saat tidak ada tugas.

- [x] **2.4 Integrasi Maskot di `WellbeingScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/wellbeing/WellbeingScreen.kt`
  - **Tindakan:**
    - Tampilkan `NemoMascot` dengan ekspresi dinamis (HAPPY saat jam gacor aktif, SLEEPY saat sirkadian malam/dip, THINKING saat distraksi tinggi, LAUGHING saat ada kemajuan screen time).

- [x] **2.5 Integrasi Maskot di `InsightsScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/insights/InsightsScreen.kt`
  - **Tindakan:**
    - Tampilkan `NemoMascot(MascotExpression.TALKING)` dan `MascotExpression.THINKING` di samping header refleksi harian.

- [ ] **2.6 Integrasi Maskot di Layar Onboarding**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/onboarding/OnboardingScreen.kt`
  - **Tindakan:**
    - Pasang `NemoMascot(MascotExpression.TALKING)` pada layar pengenalan filosofi fokus dan setup izin.

---

## Fase 3: Smart Todo & Add Task Sheet (P1)

- [ ] **3.1 Desain Ulang Modal Form `AddTaskSheet.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/AddTaskSheet.kt`
  - **Tindakan:**
    - Terapkan gaya *inset surface* monokrom (`#141414` / `#F5F5F5`).
    - Input teks judul tugas yang responsif dengan autofocus keyboard.
  - **Kriteria Verifikasi:** Bottom sheet terbuka mulus dari bawah dan menutup saat di-drag atau tombol Close ditekan.

- [ ] **3.2 Selector Prioritas Berbentuk Pill Dinamis**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/AddTaskSheet.kt`
  - **Tindakan:**
    - Implementasikan 4 pilihan prioritas: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
    - Pill yang aktif memiliki latar solid kontras dan bayangan tegas sesuai `gamabaran.pen`.

- [ ] **3.3 Pemilih Durasi Estimasi & Tenggat Waktu**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/AddTaskSheet.kt`
  - **Tindakan:**
    - Integrasikan pemilih waktu estimasi (opsi cepat 25m, 45m, 60m, kustom).
    - Integrasikan pemilih tanggal & jam deadline menggunakan dialog picker native.

- [ ] **3.4 Subtask Input Dinamis pada Add Task**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/AddTaskSheet.kt`
  - **Tindakan:**
    - Sediakan input untuk menambahkan subtask satu per satu.
    - Tampilkan daftar subtask sementara dengan tombol hapus.

- [x] **3.5 Peningkatan Baris Tugas `NemoTaskRow.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/core/design/components/NemoTaskRow.kt`
  - **Tindakan:**
    - Tambahkan indikator garis vertikal prioritas (*priority bar* di sisi kiri).
    - Tambahkan badge rasio subtask numerik (`1/3`) dan progress dot indicator.
    - Sediakan animasi centang saat tugas diselesaikan: spring scale bounce, checkmark fade-in, dan transisi warna teks ink -> mute.

- [x] **3.6 Pemolesan Layar `TaskDetailScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/tasks/TaskDetailScreen.kt`
  - **Tindakan:**
    - Tampilkan checklist subtask interaktif (klik langsung memperbarui database).
    - Pasang tombol aksi *"Start Focus Session"* di bagian bawah.
    - Pasang kartu tautan menuju layar *Future View*.

---

## Fase 4: Focus Mode, Timer Ring & Aware Blocking (P1)

- [ ] **4.1 Sinkronisasi Setup Sesi Fokus (`FocusScreen.kt`)**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/focus/FocusScreen.kt`
  - **Tindakan:**
    - Sediakan pilihan tugas aktif yang akan difokuskan.
    - Sediakan pemilih mode: `Gentle` (notifikasi lembut), `Strict` (pemblokiran aplikasi), `Custom` (kustom).
    - Sediakan durasi fokus: `25m` (Pomodoro), `45m`, `60m`, `Custom`.

- [ ] **4.2 Komponen `TimerRing.kt` & Countdown Presisi**
  - **Berkas:** `app/src/main/java/com/anchor/app/core/design/components/TimerRing.kt`
  - **Tindakan:**
    - Visualisasikan sisa waktu menggunakan busur lingkaran (*circular sweep angle*).
    - Tampilkan angka sisa waktu besar (`MM:SS`) yang diperbarui setiap detik.
    - Berikan tombol kontrol: *Pause*, *Resume*, dan *Finish*.

- [ ] **4.3 Integrasi Persistent Notification `FocusService.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/platform/service/FocusService.kt`
  - **Tindakan:**
    - Pastikan foreground service berjalan dengan `START_NOT_STICKY`.
    - Tampilkan notifikasi persisten berisi sisa waktu hitung mundur dan tombol aksi cepat selesai.

- [x] **4.4 Jendela Pemblokiran Ramah (`BlockOverlayWindow`)**
  - **Berkas:** `app/src/main/java/com/anchor/app/platform/accessibility/BlockOverlayWindow.kt`
  - **Tindakan:**
    - Saat sesi *Strict Focus* aktif dan aplikasi terblokir dibuka di latar depan:
      - Munculkan jendela overlay yang tenang (tidak melempar paksa ke Home).
      - Maskot tegas `mascot_angry` ditampilkan sesuai PRD §10.0 & inventaris fitur.
      - Tampilkan pesan: *"Sesi fokus sedang berjalan untuk: [Nama Tugas]"*.
      - Tombol: *"Kembali ke Tugas"* (membuka kembali Nemo).
      - Tautan darurat: *"Emergency Unblock"* (dengan konfirmasi sadar).

- [x] **4.5 Penyimpanan Riwayat Sesi ke `focus_session`**
  - **Berkas:** `app/src/main/java/com/anchor/app/core/domain/usecase/focus/FinishFocusUseCase.kt`
  - **Tindakan:**
    - Tegakkan *Effort-based Completion* (PRD §10.1): Tugas hanya berstatus `DONE` jika sesi fokus berjalan penuh (`actualDurationMs >= plannedDurationSec * 1000`). Sesi selesai lebih awal tetap tercatat namun tugas tidak ditandai selesai.

---

## Fase 5: Intervensi Kontekstual & Animasi Pernapasan (P2)

- [x] **5.1 Komponen Animasi Napas `BreathingRing.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/core/design/components/BreathingRing.kt`
  - **Tindakan:**
    - Implementasikan 3 cincin konsentris berdenyut halus (*infinite scale & alpha transition*).
    - Pola napas terpandu 4-4-4 (Inhale 4 detik, Hold 4 detik, Exhale 4 detik).
    - Teks pemandu dinamis di bagian tengah cincin.

- [ ] **5.2 Dialog Intervensi In-App `InterventionOverlay.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/intervention/InterventionOverlay.kt`
  - **Tindakan:**
    - Dialog layar penuh redup (*scrim 55%*).
    - Tampilkan fakta penggunaan: durasi pemakaian aplikasi hiburan vs tugas prioritas yang tertunda.
    - Tombol aksi utama: *"Start Focus"* (langsung membuka sesi fokus) dan *"Nanti Saja"*.

- [ ] **5.3 Pemicu Deteksi Drift (`DetectDriftUseCase.kt`)**
  - **Berkas:** `app/src/main/java/com/anchor/app/core/domain/usecase/usage/DetectDriftUseCase.kt`
  - **Tindakan:**
    - Hitung rasio pemakaian aplikasi non-produktif terhadap baseline.
    - Picu event intervensi jika melampaui 150% baseline atau melebihi 30 menit scrolling tanpa henti.

- [ ] **5.4 Pencatatan Log ke `intervention_log`**
  - **Berkas:** `app/src/main/java/com/anchor/app/core/data/repository/InterventionRepositoryImpl.kt`
  - **Tindakan:**
    - Catat setiap intervensi yang muncul (`shownAt`, `contextPackage`, `relatedTaskId`, `status = SHOWN / RESOLVED / DISMISSED`).

---

## Fase 6: Future View, Domino Effect & Daily Insights (P2)

- [ ] **6.1 Tampilan Forked Timeline di `FutureViewScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/futureview/FutureViewScreen.kt`
  - **Tindakan:**
    - Kartu A (Start Now): Visualisasi jadwal selesai lebih cepat dan waktu santai di malam hari.
    - Kartu B (If Delay): Visualisasi penumpukan tugas hingga larut malam dan risiko terlewat deadline.
    - Disclaimer: *"Berdasarkan jadwalmu saat ini — proyeksi, bukan ramalan pasti."*.

- [ ] **6.2 Diagram Rantai Keterikatan Tugas (Domino Effect)**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/futureview/FutureViewScreen.kt`
  - **Tindakan:**
    - Tampilkan visualisasi rantai ketergantungan antar-tugas dari `ComputeDominoEffectUseCase`.
    - Berikan indikator panah alur yang menghubungkan tugas aktif dengan tugas berikutnya yang terdampak.

- [x] **6.3 Kurva Energi Sirkadian di `WellbeingScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/wellbeing/WellbeingScreen.kt`
  - **Tindakan:**
    - Visualisasikan grafik batang jam produktif harian (07:00, 09:30, 11:30, 13:30, 15:30, 18:00, 21:00).
    - Sorot *Peak Focus Window* (09:00 - 11:30) dengan kontras tinggi.

- [ ] **6.4 Kartu Refleksi Harian di `InsightsScreen.kt`**
  - **Berkas:** `app/src/main/java/com/anchor/app/feature/insights/InsightsScreen.kt`
  - **Tindakan:**
    - Tampilkan ringkasan metrik: Tasks Done, Focus Time, Screen Time.
    - Tampilkan kartu *Longest Distraction Session* (nama aplikasi & durasi).
    - Tampilkan kartu kutipan refleksi dan saran perbaikan hari esok.

- [ ] **6.5 Integrasi Fail-Soft Klien AI Gemini (`GeminiInsightSource.kt`)**
  - **Berkas:** `app/src/main/java/com/anchor/app/platform/ai/GeminiInsightSource.kt`
  - **Tindakan:**
    - Panggil endpoint Gemini Flash dengan format agregat tanpa PII.
    - Jika offline atau API key kosong, langsung alihkan (*fail-soft*) ke generator aturan lokal tanpa error dialog.

---

## Fase 7: QA, Testing Otomatis & Verifikasi Rilis (P0)

- [x] **7.1 Eksekusi Seluruh Suite Unit Testing**
  - **Perintah:** `.\gradlew.bat testDebugUnitTest`
  - **Target:** 100% tes hijau (23 test suites executed, 0 failure).

- [ ] **7.2 Verifikasi Uji Migrasi Database Room**
  - **Berkas:** `app/src/androidTest/java/.../AnchorDatabaseMigrationTest.kt`
  - **Target:** Migrasi dari skema v1 hingga v13 lulus validasi integritas struktur tabel.

- [ ] **7.3 Inspeksi Kode & Linting Android**
  - **Perintah:** `.\gradlew.bat lintDebug`
  - **Target:** 0 error kritis pada kode Kotlin dan file resource XML.

- [ ] **7.4 Pengujian Batas Resource (RAM & Baterai)**
  - **Target:**
    - RAM operasional di bawah 150 MB pada Android Studio Profiler.
    - Tidak ada WakeLock yang tertahan saat sesi fokus selesai atau dijeda.

- [x] **7.5 Verifikasi Audit Visual Lintas Tema (Day / Night)**
  - **Target:**
    - Uji tampilan tema sistem Terang (NemoDay) dan Gelap (NemoNight).
    - Pastikan seluruh 11 layar bebas cacat overlap atau kontras warna rendah. Terverifikasi via agent-device pada perangkat TECNO CN7c - 16.

- [x] **7.6 Pembuatan Paket Rilis Akhir**
  - **Perintah:** `.\gradlew.bat assembleDebug` (atau `assembleRelease` dengan keystore resmi).
  - **Target:** APK terkompilasi bersih (`app/build/outputs/apk/debug/app-debug.apk`) dan siap dijalankan.

---

*Ceklis ini diperbarui setiap kali satu tugas teknis selesai diimplementasikan dan diverifikasi di emulator.*
