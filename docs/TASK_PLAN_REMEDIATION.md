# Rencana Terstruktur Remediasi Aplikasi Nemo (Nemo Remediation Plan)

Dokumen ini memuat rencana kerja terstruktur untuk perbaikan dan stabilisasi aplikasi Nemo (Anchor), diklasifikasikan berdasarkan tingkat urgensi dan dampaknya terhadap pengguna: **Kritis (P1)**, **Moderat (P2)**, dan **Minor (P3)**.

---

## 1. Klasifikasi Prioritas

- **P1 — Kritis (Critical)**: Bug yang menyebabkan kegagalan fitur inti, data loss permanen tanpa konfirmasi pengguna, atau inkonsistensi data kritis yang menghalangi fungsionalitas utama.
- **P2 — Moderat (Moderate)**: Bug yang berpotensi memicu runtime crash pada layout/device tertentu, miskomunikasi pesan kesalahan/validasi UI, atau degradasi pengalaman pengguna pada alur utama.
- **P3 — Minor (Low/Polish)**: Polish visual, konsistensi token/spacing, standardisasi string resource multi-bahasa, serta kelengkapan accessibility label (WCAG).

---

## 2. Rencana Remediasi Terinci

### Kategori Kritis (P1)

| ID | Komponen / File | Masalah | Rencana Remediasi | Status |
|---|---|---|---|---|
| **P1-01** | `TasksViewModel.kt` | Filter `TaskFilter.OVERDUE` pada `visibleTasks` memfilter keluar item yang overdue (`filterNot { it.id in overdueIds }`), menyebabkan tab OVERDUE/Domino selalu kosong. | Ubah evaluasi `visibleTasks` agar saat `TaskFilter.OVERDUE` (dan `COMPLETED`), daftar task tidak dieksklusi oleh `overdueIds`. Gunakan `when (currentFilter) { TaskFilter.COMPLETED, TaskFilter.OVERDUE -> tasks; else -> tasks.filterNot { it.id in overdueIds } }`. | **REMEDIATED** |
| **P1-02** | `SettingsScreen.kt` | Tombol "Hapus Semua Data" (`onDeleteAllData`) langsung mengeksekusi penghapusan seluruh data lokal tanpa konfirmasi, berisiko tinggi menyebabkan *unintended destructive data loss*. | Tambahkan state `showDeleteConfirmDialog` dan `AlertDialog` konfirmasi sebelum memanggil `viewModel.onDeleteAllData()`. Sediakan tombol Batal (`dialog_cancel`) dan tombol Hapus bertema danger. | **REMEDIATED** |
| **P1-03** | Room & Data Layer | Verifikasi transactional integrity saat reset database atau batch update recurring instances. | Pastikan eksekusi operasi cascading delete dan update berjalan dalam database transaction yang aman. | Planned |

---

### Kategori Moderat (P2)

| ID | Komponen / File | Masalah | Rencana Remediasi | Status |
|---|---|---|---|---|
| **P2-01** | `OnboardingScreen.kt` | Penggunaan `Modifier.weight(1f)` di dalam scrollable Column (`verticalScroll`) pada `PermissionOnboardingScreen` dan `ProfileSetupScreen` memicu crash `IllegalArgumentException` (infinite height constraint violation). | Hapus `Modifier.weight(1f)` dari `Spacer` di dalam kedua screen tersebut. Ganti dengan fixed spacing (`Spacer(modifier = Modifier.height(Spacing.x4))`) agar layout dapat discroll secara aman di semua resolusi layar. | **REMEDIATED** |
| **P2-02** | `AddTaskSheet.kt` | Pesan error validasi saat judul kosong memakai string `home_empty_title` ("Belum ada to-do nih..."), yang merupakan copy untuk empty state list Home, bukan pesan validasi input. | Tambahkan string resource khusus `add_task_error_empty_title` ("Judul to-do tidak boleh kosong" / "Task title cannot be empty") di `values`, `values-id`, dan `values-in`. Arahkan UI validasi untuk menampilkan string tersebut. | **REMEDIATED** |
| **P2-03** | Permission & Settings | Penanganan fallback izin OS (UsageStats, Notification, Accessibility) saat ditolak atau dicabut di latar belakang. | Verifikasi sinkronisasi state izin saat `ON_RESUME` dan sajikan banner informatif non-intrusif jika izin esensial nonaktif. | Planned |
| **P2-04** | Focus Session State | Edge case saat service focus dihentikan oleh OS (low memory killer) sebelum sesi selesai. | Simpan checkpoint durasi ke DataStore/Room secara periodik untuk recovery sesi. | Planned |

---

### Kategori Minor (P3)

| ID | Komponen / File | Masalah | Rencana Remediasi | Status |
|---|---|---|---|---|
| **P3-01** | `TaskDetailScreen.kt` | Status pill hardcoded ke "IN PROGRESS" mengabaikan `task.isDone`; tombol "Edit" berupa no-op click tanpa aksi. | Cek `task.isDone` untuk menampilkan `detail_status_completed` ("SELESAI") atau `detail_status_in_progress` ("LAGI DIKERJAIN"). Parameterkan `onEditTask` dan sembunyikan tombol jika edit belum didukung agar tidak menjadi dead no-op button. | **REMEDIATED** |
| **P3-02** | `BlockedAppsScreen.kt` | Item berpindah seketika (*list jumping*) saat switch dibalik karena list di-re-sort langsung terhadap state `blocked` terkini. | Stabilkan urutan list berbasis snapshot awal saat data aplikasi dimuat (`initialBlockedSnapshot`), sementara state switch tetap reaktif terhadap data real-time. | **REMEDIATED** |
| **P3-03** | `WellbeingScreen.kt` & `InsightsScreen.kt` | Menampilkan angka/sample rekayasa hardcoded saat data null atau kosong ("3h 42m", "-35m", "82%", "2.4 hrs deep work", "5h 18m", dummy 45m distraction, dummy pattern). | Tampilkan tanda strip ("—") atau teks "Belum ada data" / `wellbeing_no_comparison` / `insights_no_patterns` secara jujur tanpa data buatan saat data tidak tersedia atau izin belum aktif. | **REMEDIATED** |
| **P3-04** | Strings Localization | Inkonsistensi teks antar `values/strings.xml`, `values-id/strings.xml`, dan `values-in/strings.xml`. | Lakukan audit komprehensif dan sinkronisasi seluruh ID string resource antar locale bahasa. | In Progress |
| **P3-05** | Design Tokens / UI Spacing | Beberapa container menggunakan hardcoded padding dp alih-alih `Spacing.x*`. | Refactor padding dan margin manual ke semantic design token `Spacing` untuk konsistensi tema. | Planned |
| **P3-06** | Semantics & Accessibility | Maskot Nemo dan ikon custom belum memiliki semantic role atau contentDescription yang seragam untuk screen reader. | Tambahkan `contentDescription` deskriptif atau `clearAndSetSemantics` untuk elemen dekoratif murni. | Planned |

---

## 3. Matriks Verifikasi & Validasi

1. **Static Analysis & Compilation**:
   - Menjalankan `.\gradlew.bat :app:compileDebugKotlin` untuk memastikan tidak ada kesalahan kompilasi atau type mismatch.
2. **Unit & Logic Testing**:
   - Memastikan `TasksViewModel` mengembalikan task overdue saat filter `TaskFilter.OVERDUE` dipilih.
   - Memastikan dialog konfirmasi muncul sebelum aksi pembersihan data dipanggil.
3. **Layout & Compose Integrity**:
   - Memastikan hierarki scrollable layout pada layar onboarding bebas dari infinity constraint violations.
