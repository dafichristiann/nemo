# User Journeys — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Dokumen terkait:** `00-product-brief.md`, `01-prd.md`

---

## 1. Cara Membaca Dokumen Ini

Setiap journey ditulis dalam format:

```
Context → Trigger → Steps → Emotional state → Anchor's role → Success / Failure
```

Fokus bukan pada *fitur*, tetapi pada **pengalaman nyata user**. Semua journey
berkaitan dengan core loop:

```
PLAN → OBSERVE → DETECT DRIFT → INTERVENTION → FOCUS → COMPLETE → LEARN
```

---

## Journey 1 — "Merencanakan Hari" (PLAN)

**Persona:** Mahasiswa dengan banyak tugas.

**Context:**
Pagi, user baru bangun, sadar ada banyak tugas (Web Assignment, laporan praktikum,
review materi). Ia ingin tahu harus mulai dari mana.

**Trigger:**
Membuka Anchor setelah sarapan.

**Steps:**
1. User membuka **Home**. Melihat "Current priority" kosong → empty state ramah.
2. Tap **+ New Task** → isi "Kerjakan tugas Web".
3. Set deadline **hari ini 23:59**, priority **HIGH**, estimasi **120 menit**.
4. (Opsional) Tambah subtask: Setup project ✓, Create database ✓, Build login,
   Testing, Submit.
5. Perhatikan Task berikutnya, "Study calculus" → priority MEDIUM, deadline besok.
6. Save. Task muncul di **Today** dengan tanda HIGH.

**Emotional state:**
Dari cemas/tersebar → tenang, karena ada urutan jelas.

**Anchor's role:**
Menyediakan ruang yang tenang & cepat untuk mengubah kekacauan menjadi rencana.
Tidak menambah stress dengan notifikasi di tengah pembuatan.

**Success:**
≥2 task tersimpan, user tahu harus mulai dari mana (task HIGH muncul di atas).

**Failure yang harus dicegah:**
Formulir terlalu panjang → user malas. Solusi: **quick-add** (1 field dulu).

---

## Journey 2 — "Mengerjakan Sesi Fokus" (FOCUS → COMPLETE)

**Context:**
User memutuskan ingin mengerjakan "Kerjakan tugas Web" sekarang.

**Trigger:**
Dari Home, user tap task → tap **[Start Focus]**.

**Steps:**
1. **Focus Setup** muncul: task terpilih, pilih mode (default **Gentle**).
2. Tap **Start** → masuk **Active Focus**.
3. Layar: task name, current step (subtask aktif "Build login"), timer 2:00:00,
   progress, tombol [Pause] [Finish].
4. User fokus ~50 menit.
5. User check subtask ✓ Build login → progress naik.
6. Tap **Finish**.
7. Anchor tawarkan: "Mark task as complete?" atau "Keep in progress".

**Emotional state:**
Seesaw: awalnya ragu, lalu merasa produktif & menghargai tidak dihakimi.

**Anchor's role:**
Menghilangkan gesekan masuk fokus & membuat progres terlihat.

**Success:**
FocusSession tercatat ≥ X menit, progress task naik.

**Failure:**
Timer boros baterai → foreground service dioptimalkan. Blocking mati
tanpa alasan → fallback ke Gentle + notifikasi jelas.

---

## Journey 3 — "Terdistraksi & Intervensi" (OBSERVE → DETECT → INTERVENTION) ⭐ JOURNEY INTI

**Context:**
Malam, user istirahat, membuka TikTok. Awalnya niat "5 menit doang".

**Trigger:**
Sesi TikTok berjalan jauh melebihi baseline personal.

**Steps:**
1. **OBSERVE:** UsageAggregator mengumpulkan data sesi (lokal).
2. **DETECT:** Sesi 47 menit vs baseline TikTok user 18 menit → deviasi +161%.
   Anchor cek: ada task HIGH belum selesai? (Ya — "Kerjakan tugas Web") & apakah
   hari ini < 3 intervensi? (Ya).
3. **INTERVENTION Level 1→2:** Notifikasi lembut:
   > "You've been scrolling for 40 minutes, while your high-priority task is
   > still unfinished."
4. User tap notifikasi → masuk **In-app intervention**:
   > You've been scrolling for 47 minutes. Your Web Assignment is still waiting.
   > You planned around 2 hours for it.
   > [ Start Focus ]  [ Later ]
5. **CABANG A (Later):** Anchor set cooldown ~45–60 menit. Tidak mengganggu lagi
   dalam periode itu. **Menghormati keputusan user** — tidak spam.
6. **CABANG B (Start Focus):** User lanjut ke Journey 2.
7. (Bila deadline makin dekat & drift berulang) **Level 3 — Future View** muncul.

**Emotional state:**
Awalnya: "aduuh iya ya" — refleksi, bukan rasa bersalah.
Penghargaan: Anchor tidak menyalahkan & tidak mengganggu saat user pilih Later.

**Anchor's role:**
Menyadarkan **tanpa** menghakimi, memberi pilihan nyata.

**Success:**
User memilih Start Focus (recovery) ATAU memilih Later secara sadar (autonomy
dihormati). Keduanya valid — kriterianya adalah **keputusan sadar**, bukan paksaan.

**Failure yang harus dicegah:**
- Spam → user matikan notifikasi. → **Rate limit 3/hari + cooldown.**
- Bahasa menghakimi. → **Nada netral, tanpa diagnosis.**

---

## Journey 4 — "Melihat Konsekuensi Waktu" (INTERVENTION Level 3 → FOCUS)

**Context:**
Sore 15:10. User masih menunda. Anchor memicu Level 3.

**Trigger:**
Deadline makin dekat, task belum dikerjakan.

**Steps:**
1. Notifikasi: "Your evening is getting tighter. See what happens →"
2. User tap → **Future View**.
3. Layar menampilkan dua skenario berdampingan:
   ```
   IF I START NOW          IF I CONTINUE DELAYING
   15:10 → Start           20:00 → Start
   15:10–17:10 → Focus     20:00–22:00 → Work
   17:10 → Completed       22:00+ → Less free time
   Evening → Free          23:59 → Deadline
   ```
   Dengan "Based on your current schedule..."
4. User tap **[Start Focus]** → Journey 2.

**Emotional state:**
"Okay, aku masih bisa" — bukan panik. Ini kunci: **awareness, bukan fear**.

**Anchor's role:**
Mengubah "nanti aja" yang abstrak → konsekuensi waktu yang konkret.

**Success:**
User masuk Focus. Atau setidaknya melihat Future View (awareness tercapai).

---

## Journey 5 — "Fitur Tak Tersedia" (Graceful Degradation)

**Context:**
User baru install Anchor. Belum kasih izin UsageStats.

**Trigger:**
Membuka Wellbeing.

**Steps:**
1. **Empty/permission state:**
   > Wellbeing needs usage access to show your screen time.
   > [ Enable usage access ]
   > Your tasks work without it.
2. User boleh skip. Smart Todo tetap 100% jalan.
3. Kapan pun user mau → Settings → enable → Wellbeing aktif.

**Emotional state:**
Tidak dipaksa, merasa dihormati.

**Anchor's role:**
Menjelaskan **kenapa** & **apa kompensasinya** — bukan blokir.

**Success:**
App tetap berguna tanpa izin. User bisa enable kapan saja.

---

## Journey 6 — "Refleksi Harian" (LEARN)

**Context:**
Malam ~21:00. Hari selesai.

**Trigger:**
Anchor menghasilkan Daily Reflection (opsional notification).

**Steps:**
1. Notification halus / user buka Insights.
2. Tampil:
   ```
   YOUR DAY
   Tasks: 2/3 completed
   Focus: 1h 42m
   Screen time: 5h 18m
   Longest distraction: TikTok — 45m
   INSIGHT: "You completed difficult tasks earlier than usual today."
   SUGGESTION: "Tomorrow, start your hardest task within the first productive window."
   ```
3. User melihat, merasa didukung (bukan dinilai).

**Emotional state:**
Tenang, termotivasi untuk besok.

**Anchor's role:**
Menutup hari dengan refleksi lembut & satu langkah konkret.

**Success:**
User membaca. Insight didukung data nyata (bukan generik).

---

## Journey 7 — "Melacak Dampak Domino" (Phase 4)

**Context:**
User menunda "Kerjakan tugas Web" yang punya 3 task bergantung.

**Trigger:**
Membuka Domino Effect atau melihat badge di Task Detail.

**Steps:**
1. Layar menampilkan rantai:
   ```
   Kerjakan tugas Web → Review → Submit → Presentasi
   ```
2. Pesan: "1 task may affect 3 upcoming tasks."
3. Tap node → navigasi ke task terkait.

**Emotional state:**
"Ooh, ini nyambung ke presentasi" → awareness dampak.

**Anchor's role:**
Membuat ketergantungan antar-task terlihat.

**Success:**
User memahami dampak; bahasa estimatif (bukan deterministik).

---

## 8. Journey ↔ Feature ↔ Phase Mapping

| Journey | Fitur utama | Phase |
|---|---|---|
| J1 Plan | Smart Todo | 1 |
| J2 Focus | Focus Mode | 3 |
| J3 Distraction (inti) | Wellbeing + Drift + Intervention | 2–3 |
| J4 Consequence | Future View | 4 |
| J5 Degradation | Permission model | 1–3 |
| J6 Reflection | AI Insight + Daily Reflection | 5 |
| J7 Domino | Dependency + Domino Effect | 4 |
