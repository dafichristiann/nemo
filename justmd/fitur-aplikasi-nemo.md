# Dokumentasi Fitur Aplikasi "Nemo"

> **Jenis dokumen:** Inventaris fitur yang dilihat & dialami pengguna (user-facing).
> **Sumber kebenaran:** kode aplikasi aktual (paket `com.anchor.app`), terutama berkas di
> `feature/**`, `navigation/AnchorNavHost.kt`, `platform/**`, dan `res/values/strings.xml`.
> **Cakupan:** hanya fitur yang tampil di layar dan bisa dipakai pengguna. Tidak membahas
> ViewModel, use case, tabel database, WorkManager, atau detail arsitektur.
> **Bahasa UI aplikasi:** default **Inggris** (`res/values/strings.xml`), tersedia terjemahan
> **Bahasa Indonesia** penuh (`res/values-id/strings.xml`). Karena itu, label asli di dokumen ini
> ditulis persis seperti yang muncul di aplikasi versi Inggris, disertai terjemahan Indonesia.

---

## 1. Ringkasan Aplikasi

**Nemo** (nama internal paket `com.anchor.app`) adalah aplikasi Android **asisten fokus personal**.
Ia menghubungkan apa yang kita **rencanakan** (daftar tugas) dengan apa yang kita **lakukan**
(pemakaian HP), lalu membantu kembali ke tugas penting dengan cara yang **tenang, tidak memaksa,
dan tidak menghakimi**. Seluruh data tersimpan **lokal di HP**; tidak ada server.

**Prinsip produk (tercermin di UI):**
- **Local-first / Privacy-first** — data tidak keluar dari perangkat; ada penegasan ini di
  beberapa layar ("Raw usage data never leaves your phone.").
- **Awareness > Control** — Nemo menyadarkan, bukan memaksa. Setiap intervensi selalu punya cara
  untuk diabaikan ("Not now", "Emergency exit").
- **Autonomy > Forced Behavior** — pengguna selalu punya jalan keluar.
- **Nada:** kalem, suportif, ringkas, tanpa menghakimi.

**Struktur navigasi utama:**
- **Splash** singkat → **Onboarding** (2 langkah) → **Shell utama**.
- Shell utama punya **4 tab bawah (bottom navigation):**
  1. **Tasks** (Checklist)
  2. **Wellbeing** (Bar chart)
  3. **Focus** (Timer)
  4. **Insights** (Insights/sparkle)
- Halaman **detail** dan **Settings** dibuka sebagai layar penuh (bottom bar disembunyikan,
  diganti header dengan tombol Back).

> **Catatan penamaan:** dokumen desain lama (`docs/superpowers/design/`) menyebut aplikasi ini
> **"Anchor"** dan memuat daftar layar yang berbeda (mis. "00 Splash & Onboarding",
> "00b Permission Onboarding"). **Kode yang benar-benar berjalan memakai nama "Nemo"** dan
> struktur halaman seperti dijelaskan dokumen ini. Bila ada perbedaan, dokumen ini mengikuti apa
> yang tampil di aplikasi.

---

## 2. Daftar Halaman

| # | Halaman | Cara Dibuka | Jenis |
|---|---|---|---|
| 1 | Splash | Otomatis saat aplikasi pertama dibuka (cold start) | Layar penuh |
| 2 | Onboarding — Profil | Otomatis setelah Splash (pengguna baru) | Layar penuh |
| 3 | Onboarding — Izin | Lanjut dari Onboarding Profil | Layar penuh |
| 4 | Tasks (Home) | Tab "Tasks" (tab default) | Tab bawah |
| 5 | Add Task (sheet) | Tombol `+` (FAB) di Tasks | Bottom sheet |
| 6 | Task Detail | Ketuk sebuah tugas di Tasks | Layar penuh |
| 7 | Reschedule (sheet) | Tombol "Reschedule" (dari Tasks atau Task Detail) | Bottom sheet |
| 8 | Wellbeing | Tab "Wellbeing" | Tab bawah |
| 9 | Focus — Setup | Tab "Focus" (saat tidak ada sesi aktif) | Tab bawah |
| 10 | Focus — Active | Setelah menekan "Begin Focus" | Tab bawah |
| 11 | Overlay "Back to focus?" | Otomatis saat membuka app yang diblokir ketika sesi fokus berjalan | Overlay sistem |
| 12 | Notifikasi Sesi Fokus | Otomatis saat sesi fokus berjalan | Notifikasi persisten |
| 13 | Future View + Domino | Callout "See what happens if you delay" di Task Detail | Layar penuh |
| 14 | Insights — Daily Reflection | Tab "Insights" | Tab bawah |
| 15 | Intervention (dalam app) | Muncul kontekstual saat Nemo mendeteksi drift | Overlay di dalam app |
| 16 | Notifikasi Intervention | Muncul kontekstual (channel notifikasi utama) | Notifikasi |
| 17 | Settings | Ikon "Tune" (kanan atas) di Tasks | Layar penuh |
| 18 | App Categories | Dari Settings → tombol "Manage" pada App categories | Layar penuh (sub-halaman) |
| 19 | Blocked Apps | Dari Settings → tombol "Manage" pada Blocked apps | Layar penuh (sub-halaman) |

---

## 3. Detail Setiap Halaman

### 3.1 Splash

- **Tujuan:** menampilkan intro bermerek singkat sebelum masuk ke aplikasi.
- **Cara membuka:** otomatis sekali per *cold start* (bukan setiap rotasi layar).
- **Yang dilihat pengguna:**
  - Klip video pendek (sekitar 3,5 detik) dengan suara/chime singkat, ditampilkan layar penuh.
  - Ada **dua varian:** versi **terang** dan versi **gelap**, dipilih otomatis mengikuti tema
    sistem HP. Latar belakang menyesuaikan (putih untuk terang, nyaris hitam `#0A0A0A` untuk gelap).
- **Perilaku penting:**
  - Setelah klip selesai, layar langsung berpindah ke langkah berikutnya (Onboarding bila belum
    selesai, atau tab **Tasks** bila onboarding sudah selesai).
  - **Tidak pernah macet:** jika klip gagal diputar (codec tidak didukung, error decode) atau
    tidak pernah melaporkan selesai, ada batas waktu maksimum (~4,5 detik) yang memaksa lanjut.
    Layar tidak akan tertahan kosong.

---

### 3.2 Onboarding — Profil (Setup 2 of 3)

- **Tujuan:** mengumpulkan tiga hal singkat agar Nemo bisa menyapa dan merencanakan sesuai
  prioritas pengguna. Semua **opsional**.
- **Cara membuka:** otomatis setelah Splash untuk pengguna baru.
- **Yang dilihat pengguna:**
  - Indikator langkah: **"Setup 2 of 3"**.
  - **Progress track** (bar kemajuan) yang terisi sebagian (2/3).
  - **Judul:** "Let's make Nemo yours."
  - **Subjudul:** "Three quick things so the app can greet you and plan around what matters most."
  - **Field 1 — Nickname**
    - Label bagian: **"What should we call you?"**
    - Kolom isian dengan placeholder **"Your nickname"** (maksimal 24 karakter).
    - Kolom teks biasa satu baris; keyboard punya tombol "Done" untuk menutup keyboard.
  - **Field 2 — Main focus**
    - Label bagian: **"Main focus"**
    - Chip pilihan (segmented): **College** • **Career** • **Personal Growth**.
  - **Field 3 — Focus session length**
    - Label bagian: **"Focus session length"**
    - Chip pilihan: **25 min** • **45 min** • **60 min** (default **25 min**).
  - **Tombol utama:** **"Continue"**.
  - **Tautan bawah:** **"Skip for now"** (teks di tengah bawah).
- **Perilaku penting:**
  - Setiap isian **langsung tersimpan** saat diubah, jadi pengguna bisa menekan Continue atau Skip
    kapan saja tanpa kehilangan yang sudah diketik.
  - Mengeklik **Continue**: bila izin opsional (Usage access & Notifications) sudah diberikan,
    langsung masuk ke shell utama; jika belum, lanjut ke layar **Onboarding — Izin**.
  - Mengeklik **Skip for now**: langsung menuju shell utama (melewati langkah izin).

---

### 3.3 Onboarding — Izin (Setup 3 of 3)

- **Tujuan:** menawarkan izin-izin opsional dengan penjelasan jelas, **tanpa memaksa**.
- **Cara membuka:** menekan "Continue" di Onboarding Profil (saat izin belum lengkap).
- **Yang dilihat pengguna:**
  - Indikator langkah: **"Setup 3 of 3"** dan progress track penuh.
  - **Judul:** "Smart Todo works right away."
  - **Subjudul:** "These permissions are optional - unlock Nemo's full features whenever you're
    ready."
  - **Tiga kartu izin**, masing-masing menampilkan judul, keterangan, dan status:
    1. **Usage access** — "For Wellbeing & drift detection".
    2. **Notifications** — "Gentle interventions & focus sessions".
    3. **App blocking** — "Pause chosen apps during Strict/Custom focus (needs Accessibility
       access)".
  - **Status tiap kartu:**
    - Belum aktif → tombol **"Enable"**.
    - Sudah aktif → label **"Enabled"** (pill).
  - **Kartu privasi:** ikon gembok + "All data is stored locally on your phone."
  - **Tombol utama:** **"Continue"**.
  - **Tautan bawah:** **"Skip for now"**.
- **Perilaku penting:**
  - Menekan **Enable** pada **Usage access** atau **App blocking** membuka **Pengaturan sistem HP**
    (izin ini hanya bisa diberikan dari sana, bukan lewat dialog dalam app).
  - Menekan **Enable** pada **Notifications** memunculkan **dialog izin notifikasi** sistem
    (Android 13+). Jika pengguna menolak, Nemo mengarahkan ke layar pengaturan notifikasi aplikasi.
  - **Status izin diperbarui otomatis** ketika pengguna kembali ke aplikasi (mis. setelah
    mengaktifkan Usage access di Settings lalu kembali) — kartu berubah menjadi "Enabled".
  - Menekan **Continue** atau **Skip for now** mengarahkan ke shell utama (tab Tasks). Onboarding
    ditandai selesai sehingga tidak muncul lagi.

---

### 3.4 Tasks (Home) — Smart Todo

- **Tujuan:** pusat daftar tugas harian — merencanakan, melihat progres, dan membuka detail tugas.
- **Cara membuka:** tab **"Tasks"** (tab default setelah onboarding).
- **Yang dilihat pengguna:**
  - **Header sapaan:** sapaan besar "Good morning" / "Good afternoon" / "Good evening" (mengikuti
    jam), dan bila nickname diisi akan jadi mis. **"Good morning, [nickname]"**. Subjudul:
    "Here's your day at a glance."
  - **Ikon Settings** (ikon "Tune"/penggeser) di kanan atas → membuka layar Settings.
  - **Week strip:** 7 hari (Senin–Minggu) minggu ini; hari ini ditandai pill berwarna gelap
    (tinta) agar menonjol.
  - **Banner overdue:** muncul **hanya bila ada tugas yang sudah lewat tenggat**. Berbunyi mis.
    **"1 task from yesterday is still waiting."** / **"N tasks from yesterday are still
    waiting."** (bentuk jamak mengikuti jumlah). Banner berisi tombol:
    - **"Review & Reschedule"** → menampilkan/menyembunyikan daftar tugas overdue.
    - **"Dismiss"** → menutup banner.
  - **Kartu progress ("Today's progress"):**
    - Teks: **"X of Y tasks completed"**.
    - Angka persen besar, mis. **"33%"**.
    - Garis track tipis dengan isian gelap sesuai persentase (beranimasi halus).
  - **Filter (segmented):** **Today** • **Upcoming** • **Overdue** • **Completed** (bisa digulir
    mendatar bila sempit). Filter aktif ditandai pill gelap.
  - **Daftar tugas (gaya timeline):** setiap baris memiliki:
    - **Node status** bulat di kiri (terisi centang bila tugas selesai), dengan konektor vertikal
      halus ke bawah.
    - **Judul tugas** (dicoret bila selesai).
    - **Baris meta:** durasi estimasi dan/atau label tenggat, dipisah titik.
    - **Bar kecil prioritas** di bawah judul (makin tinggi prioritas makin panjang; tetap lembut,
      tidak merah menyala).
    - **Pill prioritas** di kanan: **LOW** / **MEDIUM** / **HIGH** / **URGENT**.
  - **FAB (`+`)** bulat gelap di kanan bawah → membuka sheet **Add Task**.
  - **Empty state:** bila tidak ada tugas → ikon bulat + **"No tasks yet - plan your day."** +
    **"Tap + to add your first task."**
- **Daftar tugas overdue (muncul setelah menekan "Review & Reschedule"):**
  - Tiap tugas overdue menampilkan baris timeline yang sama, plus tiga tombol kecil:
    - **"Reschedule"** → membuka sheet Reschedule.
    - **"Keep for Today"** → memindahkan tenggat ke akhir hari ini (23:59) sehingga tidak lagi
      overdue.
    - **"Open"** → membuka Task Detail (menyelesaikan tugas kini melalui sesi fokus).
- **Perilaku penting:**
  - Tugas overdue **dipisahkan** ke daftar sendiri agar tidak muncul dua kali di daftar utama.
    (Pada filter **Completed**, semua tugas tetap ditampilkan.)
  - Progres tugas dihitung dari sub-tugas (step).
  - Saat membuka tab ini, Nemo memastikan tugas berulang (recurring) hari ini sudah dibuat
    (tidak menimbulkan duplikat).
  - Menekan sebuah tugas → membuka **Task Detail**.

---

### 3.5 Add Task (Bottom Sheet)

- **Tujuan:** membuat tugas baru dengan cepat. Hanya **judul** yang wajib; sisanya opsional.
- **Cara membuka:** FAB `+` di layar Tasks.
- **Yang dilihat pengguna (dalam sheet yang naik dari bawah, dengan latar gelap di belakang):**
  - **Header:** judul **"New Task"** + ikon **close (X)**.
  - **Field judul:** placeholder **"Task title."**
  - **Field deskripsi:** placeholder **"Description (optional)"**.
  - **Priority:** chip **LOW** • **MEDIUM** • **HIGH** • **URGENT** (default **MEDIUM**).
  - **Repeat:** chip **None** • **Daily** • **Weekly** (default **None**).
  - **Deadline:** kolom yang bisa ditekan. Menampilkan **"No deadline"** bila kosong, atau jam
    dalam format `HH:00`. Menekan kolom membuka **dialog roda (wheel)** berisi pilihan
    "No deadline" + jam 00–23, dengan tombol **"Set"** dan **"Cancel"**.
  - **Est. duration:** kolom yang bisa ditekan. Menampilkan **"Not set"** atau mis. **"120 min"**.
    Menekan kolom membuka **dialog roda** berisi "Not set" + opsi 5–240 menit (kelipatan 5),
    tombol **"Set"**/**"Cancel"**.
  - **Category (optional):** kolom isian dengan placeholder **"e.g. Study"**.
  - **Subtasks (0):** label menampilkan jumlah. Di dalamnya:
    - Daftar step yang sudah ditambahkan (bisa dihapus dengan ikon X di sebelahnya).
    - Kolom input placeholder **"Add a step."** + tombol **"Add step"** untuk menambah step.
  - **Tombol utama:** **"Save Task"** (nonaktif selama judul kosong).
- **Perilaku penting:**
  - **Judul kosong tidak bisa disimpan** — tombol Save nonaktif, dan bila dipaksa muncul pesan
    kesalahan.
  - Setelah berhasil disimpan, sheet tertutup dan tugas muncul di daftar (Today) sesuai
    filter/tenggat.
  - Baris priority, deadline, dan est. duration memakai pola "ketuk kolom → pilih di roda →
    konfirmasi Set".

---

### 3.6 Task Detail

- **Tujuan:** melihat dan mengelola satu tugas secara mendalam: langkah-langkah, progres, dan
  aksi.
- **Cara membuka:** menekan sebuah tugas di layar Tasks.
- **Yang dilihat pengguna:**
  - **Header:** tombol **Back** (panah + teks **"Tasks"**) untuk kembali.
  - **Priority bar:** garis tipis gelap di atas judul.
  - **Judul tugas** (besar, tebal).
  - **Badge:** pill prioritas (mis. **HIGH**) + pill **"IN PROGRESS"**.
  - **Meta:** baris-baris berisi label tenggat (mis. "Due Oct 7, 23:59"), **"Estimated: 2 hours"**,
    dan, bila ada step, **"Progress: X% (n of m steps)"**.
  - **Progress track:** bar dengan isian sesuai persentase.
  - **Section "Steps":**
    - Daftar step; tiap step punya **checkbox bulat** untuk dicentang/lepas-centang, dan teks
      step (dicoret bila selesai).
    - Bila belum ada step: teks **"No steps yet."**
    - **Quick add step:** kolom isian (placeholder **"Add a step."**) + tombol **"Add step"**.
  - **Callout Future View:** kotak yang bisa ditekan berisi **"See what happens if you delay"**
    → membuka halaman **Future View**.
  - **Aksi:**
    - Tombol utama: **"Start Focus to Complete"** → membuka sesi **Focus** untuk tugas ini
      (menyelesaikan tugas dilakukan lewat sesi fokus).
    - Tombol sekunder: **"Reschedule"** → membuka sheet Reschedule.
    - Tombol sekunder (merah/danger): **"Delete Task"** → menghapus tugas, lalu kembali ke Tasks.
- **Perilaku penting:**
  - Mencentang step memperbarui progres secara langsung.
  - Bila tugas tidak ditemukan, muncul pesan kosong sederhana.
  - **Menyelesaikan tugas bukan** lewat tombol "Mark as Done", melainkan lewat sesi fokus
    (lihat 3.10).

---

### 3.7 Reschedule (Bottom Sheet)

- **Tujuan:** memindahkan tenggat tugas dengan cara yang tenang dan tidak menghakimi.
- **Cara membuka:** tombol **"Reschedule"** di daftar Tasks (bagian overdue) atau di Task Detail.
- **Yang dilihat pengguna:**
  - **Header:** **"Reschedule"** + ikon close (X).
  - **Day:** chip **Today** • **Tomorrow** • **+3 Days** • **+7 Days** (default **Today**).
  - **Time:** chip jam, mis. **"23:00"**, **"09:00"**, dst. (default: jam tenggat saat ini bila
    ada, jika tidak **23:00**).
  - **Tombol utama:** **"Move deadline"** → menyimpan tenggat baru.
  - **Tombol teks (bila ada tenggat saat ini):** **"Clear deadline"** → menghapus tenggat.
- **Perilaku penting:**
  - Hari dihitung dari tanggal hari ini (deterministik).
  - Memindahkan tenggat tercatat sebagai event (dipakai untuk insight), tetapi **tidak** terlihat
    sebagai pesan ke pengguna.

---

### 3.8 Wellbeing — Digital Wellbeing

- **Tujuan:** menampilkan pemakaian layar lokal dan trennya (angka nyata dari perangkat, bukan
  karangan).
- **Cara membuka:** tab **"Wellbeing"**.
- **Yang dilihat pengguna (bila izin ada & ada data):**
  - **Judul:** "Wellbeing".
  - **Kartu Screen time:**
    - Label **"Screen time"** dan label hari (mis. **"Today"** atau **"Yesterday"**).
    - Angka total besar, mis. **"5h 18m"**.
    - Baris perbandingan: **"No data for the day before yet."** / **"About the same as
      yesterday."** / **"25m more than yesterday."** / **"1h 5m less than yesterday."**
  - **Kartu "TODAY AT A GLANCE":** baris-baris perbandingan (masing-masing dengan panah tren
    ▲/▼/–):
    - **"Screen time vs [yesterday/average]"** (nilai delta).
    - **"vs your N-day average"** (rata-rata hari yang ada datanya).
    - **"Distraction share vs [yesterday]"** (perubahan dalam poin persen, mis. "+4pp").
  - **Section "TOP APPS":** hingga 3 aplikasi teratas; tiap baris punya bulatan inisial, nama
    app, bar proporsional, dan durasi.
  - **Section "THIS WEEK":** grafik batang 7 hari (Label hari Sen–Min). Batang tumbuh dengan
    animasi halus; hari ini dan hari terpilih ditonjolkan. **Batang bisa ditekan** untuk melihat
    detail hari lain dalam minggu ini (kartu screen time, top apps, dan perbandingan ikut berubah).
  - **Catatan privasi:** **"All numbers come from your device. Nothing is sent anywhere."**
- **Empty state / kondisi khusus:**
  - **Izin belum diberikan** → tampil **"See your screen time"** + penjelasan bahwa Nemo butuh
    **Usage access** dan data tetap lokal, dengan tombol **"Open Settings"** (membuka pengaturan
    Usage access sistem) dan tautan **"App settings"** (membuka Settings dalam app).
  - **Belum ada data** → **"No usage data yet"** + **"Once you use your phone a little, your
    screen-time summary will appear here."**
- **Perilaku penting:**
  - Angka **hanya** dari data pemakaian Android; **tidak pernah** diisi angka palsu 0 yang
    menyesatkan (bila tak ada data, bagian disembunyikan/diganti empty state).
  - Saat kembali ke layar (mis. habis mengubah izin), data dimuat ulang otomatis.

---

### 3.9 Focus — Setup

- **Tujuan:** mengatur sesi fokus: pilih tugas, mode, dan durasi.
- **Cara membuka:** tab **"Focus"** ketika tidak ada sesi yang sedang berjalan.
- **Yang dilihat pengguna:**
  - **Judul:** "Focus session".
  - **Notice fallback** (bila sesi sebelumnya jatuh ke mode Gentle):
    **"Blocking isn't available, so this session runs in Gentle mode."**
  - **Section "Task":** daftar tugas yang belum selesai (TODO/IN PROGRESS) sebagai baris yang bisa
    dipilih (yang terpilih berlatar gelap). Bila tidak ada: **"No focus session yet. Set one up to
    get started."**
  - **Section "Mode":** kartu pilihan:
    - **Gentle** — "Just reduces interruptions - nothing is blocked."
    - **Strict** — "May pause distraction apps while you focus."
    - *(Mode **Custom** ada di sumber teks tetapi **tidak ditampilkan** di UI saat ini; berperilaku
      sama seperti Strict.)*
  - **Bagian bantuan bila mode Strict dipilih:**
    - Bila daftar app terblokir **ada**: notice **"N app(s) will be paused during this session."**
    - Bila **kosong**: notice **"No apps selected yet. Add some in Settings to pause them."** +
      tautan **"Choose apps in Settings"** (menuju sub-halaman Blocked Apps).
    - Bila layanan App blocking belum aktif: notice penjelasan + tautan
      **"Enable app blocking in Settings"** (membuka Accessibility di pengaturan sistem).
  - **Section "Duration":** kolom yang bisa ditekan (menampilkan mis. **"25:00"**). Menekan membuka
    **dialog roda** dengan opsi 5 detik–600 detik (kelipatan 5 detik), tombol **"Set"**/**"Cancel"**.
  - **Tombol utama:** **"Begin Focus"**.
- **Perilaku penting:**
  - Durasi awal mengikuti **default focus length** dari Profil/Settings (25/45/60 menit) sampai
    pengguna memilih sendiri.
  - Bila sebuah tugas belum dipilih, Nemo otomatis memilih tugas terbuka pertama.
  - Menekan **Begin Focus** memulai sesi (mencatat sesi + mengaktifkan layanan latar) dan berpindah
    ke **Focus — Active**.
  - **Strict/Custom** memerlukan izin **App blocking** (Accessibility). Bila tidak tersedia, sesi
    otomatis berjalan sebagai **Gentle**, disertai pesan pemberitahuan (fallback jujur).
  - Status App blocking dicek ulang setiap kembali ke layar (mis. setelah mengaktifkan di Settings).

---

### 3.10 Focus — Active

- **Tujuan:** menjalankan sesi fokus yang sedang berlangsung dengan kontrol yang jelas dan selalu
  ada jalan keluar.
- **Cara membuka:** setelah menekan **"Begin Focus"**, atau saat ada sesi aktif ketika membuka tab
  Focus.
- **Yang dilihat pengguna:**
  - **Pill "FOCUS"** di kiri atas.
  - **Nama tugas** (besar, tebal). Bila tak ada: **"No task selected"**.
  - **Timer Ring:** cincin progres monokrom tebal dengan **digit besar** di tengah (mis.
    **"19:22"**) dan sub-label **"remaining"** atau **"paused"**.
  - **Notice fallback** (bila berlaku): "Blocking isn't available, so this session runs in Gentle
    mode."
  - **Section "Current step":** menampilkan step yang sedang aktif; bila tak ada step, menampilkan
    teks **"No steps yet."** atau progres step.
  - **Tombol kontrol:**
    - **"Pause"** / **"Resume"** (berganti sesuai status) — tombol sekunder.
    - **"Finish"** — tombol utama.
  - **"Emergency exit"** — selalu tersedia, jelas, di bawah tombol kontrol.
  - **Dialog konfirmasi Finish** (muncul setelah menekan Finish):
    - Judul: **"Finish this session?"**
    - Isi: **"The session will end and the task will be marked as done."**
    - Tombol: **"Finish & complete"** dan **"Cancel"**.
  - **Feedback pasca-sesi** (setelah sesi berakhir), dengan nada tetap kalem:
    - Sesi penuh & tugas selesai: **"Task completed - you focused N min on \"[tugas]\"."**
    - Sesi berakhir lebih awal: **"Task completed. You ended early (N min); the full session is
      what makes it stick."**
    - Tombol **"Dismiss"** untuk menutup feedback.
- **Perilaku penting:**
  - **Pause** membekukan jam; **Resume** melanjutkan dari waktu terakumulasi (waktu saat pause
    tidak dihitung sebagai waktu fokus).
  - **Finish** mengakhiri sesi dan menandai tugas selesai, lalu mengarahkan pengguna ke tab
    **Tasks** agar langsung melihat item yang selesai.
  - **Emergency exit** selalu bisa ditekan, membatalkan sesi (tidak menandai tugas selesai).
  - Selama sesi aktif, ada **notifikasi persisten** (lihat 3.12) dan (untuk Strict/Custom) overlay
    pemblokiran app (lihat 3.11).

---

### 3.11 Overlay "Back to focus?" (Pemblokiran App)

- **Tujuan:** saat pengguna membuka app yang sengaja dipilih untuk dipause selama sesi
  Strict/Custom, Nemo menampilkan nudge lembut dengan cara kembali.
- **Cara membuka:** otomatis ketika sebuah app yang diblokir dibuka selama sesi fokus berjalan.
  Ditampilkan sebagai overlay di atas app tersebut.
- **Yang dilihat pengguna:**
  - **Kartu putih** di tengah layar dengan latar gelap transparan.
  - **Judul:** **"Back to focus?"**
  - **Isi:** **"You asked to pause this app during a focus session. Nemo is just reminding you -
    the choice is yours."**
  - **Hint:** **"You can always end the session from the focus screen or its notification."**
  - **Tombol:** **"Back to focus"** → menutup overlay, membawa Nemo ke depan pada tab **Focus**
    (sesi aktif).
- **Perilaku penting:**
  - **Tidak memaksa**: overlay menjelaskan dan menyediakan cara kembali, tapi pengguna selalu bisa
    mengabaikannya (Awareness > Control). Overlay bersifat fokusable agar tombol "Back to focus"
    benar-benar bisa ditekan.
  - Overlay **tidak membaca konten layar** app yang diblokir.

---

### 3.12 Notifikasi Sesi Fokus

- **Tujuan:** menjaga sesi fokus tetap berjalan dan memberi cara keluar kapan saja.
- **Kapan muncul:** otomatis selama sesi fokus aktif; hilang saat sesi selesai/dibatalkan.
- **Yang dilihat pengguna:**
  - **Judul:** **"Focus session in progress"**.
  - **Teks:** **"Tap to return. Emergency exit is always available."**
  - **Aksi:** **"Emergency exit"** (menghentikan sesi + membuka Nemo).
  - Notifikasi bersifat **menetap (ongoing)** dan prioritas rendah.
  - Mengetuk notifikasi membuka kembali aplikasi.
- **Perilaku penting:**
  - Notifikasi hanya hidup **selama sesi aktif**; tidak ada notifikasi "hantu" saat tak ada sesi.

---

### 3.13 Future View + Domino Effect

- **Tujuan:** menunjukkan konsekuensi bila tugas ditunda vs dikerjakan sekarang (berbasis aturan,
  jujur sebagai proyeksi).
- **Cara membuka:** callout **"See what happens if you delay"** di Task Detail.
- **Yang dilihat pengguna:**
  - **Header:** tombol **Back** (panah + "Back").
  - **Judul:** **"What happens next?"**
  - **Disclaimer:** **"Based on your current schedule - a projection, not a prediction."**
  - **Kartu Skenario A — "IF I START NOW":** rentang waktu (mis. "15:10 → 17:10") dan opsional
    label tenggat ("Deadline HH:mm").
  - **Kartu Skenario B — "IF I KEEP DELAYING":** rentang waktu (mis. "20:00 → 22:00").
  - **Notice asumsi** (bila ada nilai default yang dipakai): **"Some values use default
    assumptions (duration or deadline) - marked so you know."**
  - **Section "Domino Effect":**
    - Bila ada keterkaitan: teks **"1 task may affect N upcoming tasks."** dan rantai tugas
      (tugas saat ini ditandai **CURRENT**, lalu tugas-tugas terdampak di bawahnya, dengan panah
      penghubung). Tiap tugas bisa ditekan untuk dibuka (Task Detail).
    - Bila belum ada: **"No linked tasks yet."**
  - **Section "Link a task that this one affects":**
    - Hint: **"Tap a task to link it"**.
    - Daftar tugas kandidat; tiap baris punya tombol **"Link task"**.
    - Bila percobaan membuat lingkaran (siklus) ditolak: notice kalem **"That would create a
      circular link, so it wasn't added."**
    - Bila tak ada kandidat: **"No other tasks to link yet."**
  - **Tombol utama:** **"Start Focus Now"** → memulai sesi fokus untuk tugas ini.
- **Perilaku penting:**
  - Bahasa bersifat estimatif ("may affect"), bukan kepastian.
  - Bila tugas tidak punya estimasi durasi/tenggat, dipakai nilai asumsi default dan ditandai
    lewat notice asumsi.

---

### 3.14 Insights — Daily Reflection

- **Tujuan:** rangkuman reflektif harian yang kalem dan suportif.
- **Cara membuka:** tab **"Insights"**.
- **Yang dilihat pengguna (bila ada konten):**
  - **Judul:** "Insights" + baris tanggal (mis. "October 7 - Daily Reflection").
  - **Section "YOUR DAY":** baris-baris statistik:
    - **Tasks** → **"X / Y completed"**.
    - **Focus** → total waktu fokus (mis. "1h 42m").
    - **Screen Time** → total waktu layar (**hanya tampil bila ada data usage**).
    - **Longest distraction** → mis. "TikTok • 45m" (**hanya tampil bila ada data**).
  - **Pembatas (divider).**
  - **Blok "INSIGHT":** kutipan refleksi (mis. "You completed difficult tasks earlier than
    usual...").
  - **Section "Pattern":** daftar pola, mis. "• Most productive: 09:00-11:00".
  - **Blok "SUGGESTION":** satu saran realistis berikutnya.
  - **Atribusi sumber (jujur):** **"Generated locally on your device"** atau
    **"Generated by Nemo AI (aggregated data only)"**.
  - **Tautan "Refresh"** → membuat ulang refleksi.
- **Empty state:**
  - **"Nothing to reflect on yet"** + **"Once you plan or complete a task, your daily reflection
    will appear here."**
- **Perilaku penting:**
  - Bagian **Screen Time / Longest distraction disembunyikan** bila tidak ada data usage (tidak
    menampilkan 0 yang menyesatkan).
  - Refleksi bisa dihasilkan secara lokal (aturan) **atau** opsional via AI; sumbernya selalu
    disebut dengan jujur.

---

### 3.15 Intervention (Overlay di Dalam App)

- **Tujuan:** menyadarkan pengguna secara kontekstual saat Nemo mendeteksi "drift" (menjauh dari
  tugas penting) — sebagai kartu kalem di dalam app.
- **Kapan muncul:** ketika mesin intervensi mengirim lewat **channel dalam app** (mis. izin
  notifikasi ditolak), atau saat pengguna sedang berada di dalam Nemo. Muncul di atas layar saat
  ini.
- **Yang dilihat pengguna:**
  - **Latar gelap transparan** (scrim) di belakang; menekan scrim menutup tanpa cooldown.
  - **Kartu** yang naik dari bawah dengan animasi halus. Isinya:
    - **Judul** sesuai level:
      - Gentle: **"You've been here longer than usual."**
      - Contextual: **"Your task is still unfinished."**
      - Future: **"Starting now gives you free time later."**
      - Focus: **"Start a focus session?"**
    - **Isi:** **"Awareness only - it's your call."**
    - **Aksi utama:** **"Start Focus"**.
    - **Aksi sekunder:** **"Later"** dan **"Not now"**.
    - **Hint:** **"We'll stay quiet about this one for a while."**
- **Perilaku penting:**
  - **Start Focus** → masuk ke alur Focus (mencatat interaksi).
  - **Later** → mencatat penundaan dan memasang **cooldown 2 jam** untuk konteks ini (Nemo diam
    untuk sementara).
  - **Not now** → menutup begitu saja tanpa cooldown.
  - Nada selalu menawarkan pilihan, bukan perintah.

---

### 3.16 Notifikasi Intervention

- **Tujuan:** menyadarkan saat pengguna tidak sedang membuka Nemo; ini channel utama intervensi.
- **Kapan muncul:** kontekstual, saat mesin mendeteksi drift dan izin notifikasi tersedia.
- **Yang dilihat pengguna (judul & isi per level):**
  - **Gentle** — "A gentle check-in" / "You've been here a while. No pressure - just noticing."
  - **Contextual** — "Still on your list" / "A task that matters is still waiting for you."
  - **Future** — "A moment to plan" / "Starting now could give you some free time later tonight."
  - **Focus** — "Ready to focus?" / "Here whenever you're ready to start a focus session."
  - **Aksi:** **"Start Focus"** dan **"Later"**.
  - Nama channel: **"Gentle check-ins"** (dengan deskripsi kalem).
- **Perilaku penting:**
  - Bila notifikasi dinonaktifkan, mesin beralih ke channel dalam app (3.15).
  - Menekan **Start Focus** atau **Later** mencatat pilihan pengguna (mempengaruhi kuota &
    cooldown intervensi).

---

### 3.17 Settings

- **Tujuan:** mengatur profil, izin, privasi/AI, kategori app, app yang diblokir, dan kontrol data.
- **Cara membuka:** ikon **Tune** (kanan atas) di layar Tasks.
- **Yang dilihat pengguna:**
  - **Header:** tombol **Back** (panah) + judul **"Settings"**.
  - **Section "Profile":**
    - **Nickname** — kolom isian (placeholder "Your nickname", maks 24 karakter). Kosong =
      menyapa tanpa nama.
    - **Main focus** — chip **College** • **Career** • **Personal Growth**.
    - **Default focus length** — chip **25 min** • **45 min** • **60 min**.
  - **Section "Permissions":** tiga baris izin, masing-masing dengan judul + deskripsi + status:
    - **Usage access** — "Lets Nemo read screen-time locally for Wellbeing & drift detection."
    - **Notifications** — "Gentle check-ins and an ongoing focus notification."
    - **App blocking** — "Lets Nemo pause chosen apps during Strict/Custom focus. Read from
      Settings → Accessibility."
    - Status: **"On"** (pill gelap) bila sudah diberikan, atau tombol **"Enable"** bila belum
      (mengarah ke pengaturan sistem / atau dialog izin).
  - **Section "Privacy & AI":**
    - **AI Personal Insights** (toggle) — "Only aggregated summaries are sent, never raw usage."
    - **Usage tracking** (toggle) — "Collect local screen-time to power Wellbeing."
  - **Section "App categories":** ringkasan (mis. "N apps categorized" atau
    "No apps categorized yet. They appear once usage is collected.") + tombol **"Manage"** →
    sub-halaman **App Categories**.
  - **Section "Blocked apps":** ringkasan (mis. "N apps paused during focus" atau
    "No apps paused yet") + tombol **"Manage"** → sub-halaman **Blocked Apps**.
  - **Section "Focus blocking":** toggle **"Pause blocked apps (experimental)"** —
    "Requires App blocking access above. Off by default; sessions fall back to Gentle when
    unavailable."
  - **Section "Data controls":**
    - Teks retensi: **"Data retention - keep at most N days"**.
    - Tombol danger: **"Delete all data"** (menghapus seluruh data pengguna + mengatur ulang
      pengaturan).
  - **Penegasan:** **"Raw usage data never leaves your phone."**
- **Perilaku penting:**
  - Status izin diperbarui otomatis saat kembali ke layar (mis. setelah mengubah di Settings HP).
  - Toggle **AI Personal Insights** menjelaskan secara ringkas bahwa hanya data agregat yang
    dikirim (dan hanya jika diaktifkan).
  - **Default menjaga privasi:** tracking & AI mati sampai pengguna mengaktifkannya (opt-in).

---

### 3.18 App Categories (Sub-halaman)

- **Tujuan:** mengelola kategori app dan menandai mana yang dianggap "distraction".
- **Cara membuka:** Settings → **"Manage"** pada bagian **App categories**.
- **Yang dilihat pengguna:**
  - **Header:** tombol Back + judul **"App categories"**.
  - **Kolom pencarian:** placeholder **"Search apps."** (mencocokkan package atau label kategori).
  - **Daftar app terkategori:** tiap baris berisi:
    - **Ikon app** (atau inisial bila ikon tak tersedia).
    - **Label app** + **package name** di bawahnya.
    - **Kolom ubah label kategori** (placeholder "Category") + tombol **"Save"**.
    - **Toggle "Treat as distraction"**.
  - **Empty state:** **"No apps categorized yet. They appear once usage is collected."**
- **Perilaku penting:**
  - Menyimpan label kategori atau mengubah toggle "Treat as distraction" langsung menyimpan
    perubahan (override manual oleh pengguna).

---

### 3.19 Blocked Apps (Sub-halaman)

- **Tujuan:** memilih app mana yang akan dipause selama sesi fokus **Strict/Custom**.
- **Cara membuka:** Settings → **"Manage"** pada bagian **Blocked apps** (atau lewat tautan
  "Choose apps in Settings" pada Focus Setup).
- **Yang dilihat pengguna:**
  - **Header:** tombol Back + judul **"Blocked apps"**.
  - **Kolom pencarian:** placeholder **"Search apps."** (mencocokkan label/nama app).
  - **Daftar app terinstal:** tiap baris berisi ikon app, label, package name, dan **toggle**.
    - App yang **terblokir** ditampilkan **paling atas**, lalu app lainnya secara alfabetis.
  - **Empty state:** **"No installed apps found."**
- **Perilaku penting:**
  - Menyalakan toggle menambahkan app ke daftar blokir; mematikan menghapusnya.
  - Nemo hanya melihat app dengan entri launcher (bukan seluruh paket) demi privasi.
  - Daftar blokir hanya dipakai pada mode **Strict/Custom**; mode **Gentle** tidak memblokir apa
    pun.

---

## 4. Peta Alur Navigasi

```
[Splash]  (video intro, sekali per cold start)
    │
    ▼
[Onboarding — Profil]  "Setup 2 of 3"        (Continue / Skip)
    │  Continue (izin belum lengkap)
    ▼
[Onboarding — Izin]    "Setup 3 of 3"        (Continue / Skip)
    │
    ▼
╔════════════════ SHELL UTAMA (4 tab bawah) ════════════════╗
║  Tasks   │   Wellbeing   │   Focus   │   Insights          ║
╚═══════════════════════════════════════════════════════════╝
    │            │              │              │
    │            │              │              │
  Tasks        Wellbeing      Focus         Insights
    │                          ├─ (tidak aktif) Setup ──► [Begin Focus] ──►  Active
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

Kontekstual (dari mana saja):
  • [Intervention in-app]  (overlay)      ── "Start Focus" / "Later" / "Not now"
  • [Notifikasi intervention]            ── "Start Focus" / "Later"
  • [Overlay "Back to focus?"]           ── saat membuka app terblokir (Strict/Custom)
  • [Notifikasi Sesi Fokus]              ── persisten selama sesi aktif
```

### Tabel "Halaman → Cara Akses"

| Halaman | Cara mengakses |
|---|---|
| Splash | Otomatis saat cold start |
| Onboarding Profil | Otomatis setelah Splash (pengguna baru) |
| Onboarding Izin | "Continue" di Onboarding Profil |
| Tasks (Home) | Tab **Tasks** (default) |
| Add Task | FAB **+** di Tasks |
| Task Detail | Ketuk tugas di Tasks |
| Reschedule | Tombol **Reschedule** di Tasks (overdue) / Task Detail |
| Wellbeing | Tab **Wellbeing** |
| Focus Setup | Tab **Focus** (tanpa sesi aktif) |
| Focus Active | **Begin Focus** di Focus Setup |
| Overlay "Back to focus?" | Membuka app terblokir saat sesi fokus berjalan |
| Notifikasi Sesi Fokus | Selama sesi fokus aktif |
| Future View | Callout **"See what happens if you delay"** di Task Detail |
| Insights | Tab **Insights** |
| Intervention (in-app) | Kontekstual (dalam app) |
| Notifikasi Intervention | Kontekstual (channel notifikasi) |
| Settings | Ikon **Tune** di Tasks |
| App Categories | Settings → **Manage** (App categories) |
| Blocked Apps | Settings → **Manage** (Blocked apps) |

---

## 5. Catatan Ketidaksesuaian yang Perlu Diketahui

Hal-hal berikut adalah selisih nyata antara kode berjalan dan sumber teks, dicatat agar
dokumentasi tidak menyesatkan:

1. **Nama aplikasi.** Kode dan `strings.xml` memakai **"Nemo"**; dokumen desain lama menyebut
   **"Anchor"**.
2. **Mode "Custom"** pada Focus. Teks **"Custom"** ada di `strings.xml`, tetapi kartu mode di UI
   saat ini **hanya menampilkan Gentle dan Strict**. Kode menyebut Custom sengaja disembunyikan
   karena perilakunya identik dengan Strict.
3. **Onboarding.** Mockup lama menyebut "00 Splash & Onboarding" dan "00b Permission Onboarding"
   sebagai langkah 1 dari 2. Di aplikasi nyata, onboarding terdiri dari **dua langkah: Profil
   (2 of 3) lalu Izin (3 of 3)**, dengan Splash sebagai intro video terpisah.
4. **Tab dan layar.** Empat tab berjalan: **Tasks, Wellbeing, Focus, Insights**. Halaman
   `App Categories` dan `Blocked Apps` adalah **sub-halaman Settings** yang tidak selalu muncul di
   daftar layar dokumen lama.
5. **Bahasa.** Aplikasi default berbahasa **Inggris**, dengan terjemahan **Indonesia** tersedia
   penuh; label di dokumen ini mengikuti versi Inggris (seperti yang tampil di perangkat default).

---

> **Akhir dokumen.** Dokumen ini mendeskripsikan fitur yang dilihat & dialami pengguna untuk setiap
> halaman dan sub-halaman aplikasi Nemo, berdasarkan kode aplikasi aktual. Tidak ada kode yang
> diubah dalam penyusunan dokumen ini.
