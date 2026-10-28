# Nemo — Spesifikasi Desain

## 1. Prinsip
- **Tenang, bukan memaksa.** Setiap intervensi punya jalan keluar ("Not now", "Emergency exit").
- **Satu ide per layar.** Ruang kosong adalah bagian dari desain, jadi jangan menjejalkan elemen.
- **Netral murni.** Hitam, putih, abu. Prioritas dan bahaya dibedakan lewat panjang bar, bobot huruf, dan label, bukan warna menyala.
- **Privasi terlihat.** Penegasan "data tetap di HP" muncul di Onboarding Izin, Wellbeing, dan Settings.
- **Nada tulisan:** pendek, hangat, tanpa menghakimi. Pakai "may", "could", "no pressure"; hindari "must", "failed", "overdue!".

## 2. Maskot & Identitas Visual (Nemo)
- **Karakter:** Anak laki-laki chibi, kepala saja (tanpa leher dan badan), digambar dengan gaya tinta spidol hitam-putih: rambut runcing berantakan dengan goresan putih tipis, kacamata bulat tebal, telinga kecil, hidung titik, mulut kecil. Tanpa warna, bayangan, atau gradasi.
- **Peran:** Menggantikan seluruh logo/ilustrasi lama, ikon bulat empty state, dan intro video Splash. Bertindak sebagai identitas, ilustrasi, dan pendamping kontekstual di seluruh 19 layar.
- **12 Ekspresi Karakter:**
  1. `mascot_neutral` (Splash 1.5s, header Tasks siang/malam, Focus Active, notifikasi fokus)
  2. `mascot_happy` (Splash 2s memantul, onboarding nickname terisi, izin aktif semua, Skenario A Future View, penegasan privasi)
  3. `mascot_sad` (Overdue banner, sesi fokus diakhiri awal, intervensi Contextual, Delete all data)
  4. `mascot_angry` (Overlay "Back to focus?", Skenario B Future View "IF I KEEP DELAYING")
  5. `mascot_surprised` (Cadangan)
  6. `mascot_sleepy` (Wellbeing belum ada data, Focus Active saat pause, Insights empty, App Categories empty)
  7. `mascot_confused` (Focus Setup fallback notice, circular link warning Domino, Blocked Apps empty)
  8. `mascot_laughing` (Progress 100% Tasks, Focus selesai penuh verified)
  9. `mascot_shy` (Onboarding Izin meminta akses, Wellbeing permission prompt)
  10. `mascot_thinking` (Task Detail callout Future View, Intervensi Level 1 Gentle, Focus Setup tugas kosong)
  11. `mascot_wink` (Alternatif idle)
  12. `mascot_talking` (Tasks empty state, Onboarding Profil sapaan, INSIGHT quote card, Intervensi Focus)
- **Aset Tambahan:** `mascot_blink` (siklus kedip 150ms tiap 3-5 detik), `mascot_silhouette` (ikon monokrom status bar notifikasi & themed icon Android 13+), `app_icon` (Neutral di latar putih, safe zone tengah).
- **Skala Ukuran:** Besar (~120dp untuk Splash, Onboarding, Empty State), Sedang (~80dp untuk Overlay pemblokiran & Intervensi), Kecil (~48dp untuk Header & notice).
- **Mode Tampilan:** Garis hitam transparan langsung di latar Day; dibungkus dalam badge lingkaran putih pada latar Night (`#0A0A0A`) agar siluet rambut tidak hilang.
- **Animasi:** Crossfade transisi 120ms, kedip idle 150ms, pantulan pelan via kode Compose; nonaktif jika sistem mengaktifkan "Remove animations".

## 3. Warna
| Peran | Day | Night | Dipakai untuk |
|---|---|---|---|
| background | #FFFFFF | #0A0A0A | Latar layar |
| surface | #FAFAFA | #141414 | Sheet, dialog, kartu halus |
| fog | #F5F5F5 | #1B1B1B | Input, track, tombol sekunder, pill netral |
| line | #E9E9E9 | #262626 | Garis pemisah, border chip |
| mute | #767676 | #8A8A8A | Teks sekunder, meta, ikon tidak aktif |
| placeholder | #8C8C8C | #6B6B6B | Placeholder input |
| inkSecondary | #444444 | #CFCFCF | Teks isi sekunder |
| ink | #111111 | #F2F2F2 | Teks utama, tombol utama, node selesai, FAB |
| onInk | #FFFFFF | #0A0A0A | Teks/ikon di atas ink |
| danger | #8A3B3B | #D9A0A0 | Hanya "Delete Task", "Delete all data" |
| scrim | #000 @ 42% | #000 @ 67% | Latar di belakang sheet/overlay |

Catatan aksesibilitas: mockup awal memakai abu #8C8C8C untuk teks kecil. Di paket ini `mute` Day dinaikkan ke #767676 agar lolos kontras 4,5:1 pada putih. #8C8C8C hanya untuk placeholder dan ikon.

## 4. Tipografi
Fraunces Light/Regular untuk judul dan angka besar. Manrope untuk semua teks lain.

| Gaya | Font | Berat | Ukuran/baris | Dipakai untuk |
|---|---|---|---|---|
| displayLarge | Fraunces | 300 | 46/52 | Timer fokus |
| displayMedium | Fraunces | 300 | 34/40 | Persen progres, screen time |
| headlineLarge | Fraunces | 300 | 30/36 | Judul layar ("Good morning, Rina") |
| headlineMedium | Fraunces | 400 | 22/28 | Judul sheet, dialog |
| titleLarge | Fraunces | 400 | 19/26 | Kutipan Insight |
| titleMedium | Manrope | 600 | 15/22 | Judul tugas, judul baris |
| bodyLarge / Medium / Small | Manrope | 400 | 14/22 · 13/20 · 12/18 | Isi, meta, catatan |
| labelLarge | Manrope | 600 | 14/20 | Tombol |
| labelMedium | Manrope | 600 | 12/16 | Tombol kecil |
| labelSmall | Manrope | 600, kapital | 10.5/14, tracking +0.12em | Label bagian (YOUR DAY, STEPS) |

## 5. Spasi, bentuk, ukuran
- **Spasi (dp):** 4 · 8 · 12 · 16 · 24 · 32 · 48. Margin horizontal layar 24. Jarak antar bagian 28.
- **Radius:** tombol/chip/pill penuh (999) · input 18 · kartu 24 · dialog 30 · sheet atas 32.
- **Ukuran:** tombol 54 (kecil 34) · input 54 · FAB 56 · bottom bar 76 · node tugas 20 · toggle 40×24 · track progres 3 · stroke ring 6 · ring 220.
- **Area sentuh minimum 48dp.** Tombol kecil (34dp) harus punya area sentuh yang diperluas.
- **Elevasi:** hampir datar. Bayangan hanya pada FAB. Sheet dan dialog dibedakan oleh warna `surface` dan scrim.

## 6. Komponen (file: `compose/components/NemoComponents.kt`)
| Komponen | Varian/status | Catatan |
|---|---|---|
| NemoButton | Primary, Secondary, Outline, Danger, Text · enabled/disabled · small | Disabled = 35% opasitas. Satu tombol Primary per layar. |
| NemoChip | selected / unselected | Pilihan tunggal: Priority, Repeat, Day, Time, Main focus, Durasi sesi |
| NemoPill | netral / filled | LOW MEDIUM HIGH URGENT · IN PROGRESS · On · Enabled · FOCUS · CURRENT |
| NemoToggle | on / off | AI Insights, Usage tracking, Blocked apps |
| NemoTextField | default / fokus | Latar fog, tanpa garis bawah |
| NemoProgressTrack | 0–1 | Animasi 600ms, easing lembut |
| NemoTimerRing | 0–1 | Angka besar Fraunces di tengah, label "remaining"/"paused" |
| NemoNode, NemoTaskRow | belum/selesai | Selesai = isian ink + centang, judul dicoret dan memudar |
| NemoListRow | dengan subtitle/trailing | Dipakai di Settings, Izin, Blocked apps |
| NemoSectionLabel | – | Label kapital kecil |
| NemoBottomBar | 4 tab | Tasks, Wellbeing, Focus, Insights. Tab aktif = ink + tebal, tanpa indikator pil |
| NemoFab | – | Bulat ink, kanan bawah, 100dp dari bawah layar |
| NemoBottomSheet | – | Add Task, Reschedule, Intervention |

Ikon (garis 1,6, ujung bulat): back, close, add, tune, tasks, wellbeing, focus, insights, lock.

## 7. Layar (19)
Rujukan visual: `preview/nemo-night-recommended.html`.

| # | Layar | Komposisi | Komponen utama | Catatan UX |
|---|---|---|---|---|
| 1 | Splash | Maskot di tengah layar + teks "Nemo" di bawah | Maskot animasi, teks | Urutan animasi maskot ±3,5 dtk (`Neutral` 1,5s → `Happy` 2s memantul pelan) tanpa pemutar video, batas waktu 4,5 dtk. Ikuti tema sistem (Night: badge lingkaran putih). |
| 2 | Onboarding Profil | "Setup 2 of 3", track, judul, 3 isian | TextField, Chip, Button | Semua opsional. Tersimpan otomatis. "Skip for now" selalu ada. |
| 3 | Onboarding Izin | Judul + 3 baris izin + catatan privasi | ListRow, Button kecil, Pill | Status "Enabled" menggantikan tombol. Diperbarui saat kembali ke app. |
| 4 | Tasks | Sapaan, week strip, progres, tab filter, daftar tugas, FAB | TaskRow, ProgressTrack, Fab | Hari ini = pil ink. Tab filter berupa teks bergaris bawah. Banner overdue hanya bila ada. |
| 5 | Add Task | Sheet: judul, deskripsi, prioritas, tenggat/durasi, Save | BottomSheet, Chip, TextField | Save nonaktif bila judul kosong. Repeat, kategori, subtask di bawah "More". Tenggat/durasi memakai roda pilihan. |
| 6 | Task Detail | Back, judul, pill, meta, steps, callout, aksi | Pill, Node, Button | Tidak ada "Mark as Done", selesai lewat sesi fokus. Delete = teks danger. |
| 7 | Reschedule | Sheet: Day, Time, Move deadline | Chip, Button | "Clear deadline" hanya bila ada tenggat. |
| 8 | Wellbeing | Screen time besar, bar 7 hari, top 3 app, catatan privasi | ProgressTrack, bar | Bar bisa diketuk. Jangan tampilkan angka 0 palsu, pakai empty state. |
| 9 | Focus Setup | Pilih tugas, Mode (Gentle/Strict), durasi, Begin Focus | Row, Card, Button | Custom disembunyikan. Strict: tampilkan notice jumlah app / tautan ke Settings. |
| 10 | Focus Active | Pill FOCUS, nama tugas, ring, step aktif, Pause/Finish | TimerRing, Button | "Emergency exit" selalu terlihat. Finish memunculkan dialog konfirmasi. |
| 11 | Overlay Back to focus? | Dialog tengah di atas scrim | Dialog, Button | Satu tombol. Nada mengingatkan, bukan melarang. |
| 12 | Notifikasi sesi fokus | Ongoing, prioritas rendah | – | Aksi "Emergency exit". |
| 13 | Future View | Dua skenario (angka besar), domino, Start Focus Now | Teks display, Pill | Bahasa estimatif ("may affect"). Tandai nilai asumsi. |
| 14 | Insights | Tanggal, statistik, kutipan, pola, saran | ListRow, teks display | Sembunyikan Screen Time/Distraction bila tak ada data. Selalu sebut sumber ("Generated locally…"). |
| 15 | Intervention in-app | Sheet: judul sesuai level, "Awareness only", Start Focus, Later, Not now | BottomSheet, Button | "Later" = cooldown 2 jam. "Not now" = tutup tanpa cooldown. |
| 16 | Notifikasi intervention | Judul + isi per level, aksi Start Focus/Later | – | Channel "Gentle check-ins". |
| 17 | Settings | Profile, Permissions, Privacy & AI, Manage apps, Data | ListRow, Toggle, Pill | Opt-in default mati. "Delete all data" paling bawah, gaya danger. |
| 18 | App Categories | Search, daftar app + kategori + Treat as distraction | ListRow, TextField, Toggle | Perubahan tersimpan langsung. |
| 19 | Blocked Apps | Search, app terblokir di atas lalu alfabet | ListRow, Toggle | Hanya berlaku di mode Strict. |

Keadaan kosong (teks asli): Tasks "No tasks yet - plan your day." · Wellbeing "No usage data yet" / "See your screen time" · Insights "Nothing to reflect on yet" · Blocked apps "No installed apps found."

## 8. Gerak
- Durasi: tekan 150ms · transisi 250ms · sheet 400ms · progres 600ms.
- Easing `cubic-bezier(0.22, 1, 0.36, 1)`. Tanpa bounce, tanpa goyang, tanpa kedip.
- Sheet naik dari bawah. Overlay muncul dengan fade. Bar tumbuh pelan.
- Hormati "Remove animations" di pengaturan aksesibilitas: ganti gerak dengan fade singkat.

## 9. Aksesibilitas
- Kontras teks ≥ 4,5:1. Ikon dan border ≥ 3:1.
- Semua ikon-saja punya `contentDescription`. Node tugas bisa dibaca sebagai "selesai/belum selesai".
- Prioritas tidak boleh hanya mengandalkan warna. Pill selalu memuat teks.
- Mendukung font scaling hingga 200%: gunakan `sp` untuk teks, dan hindari tinggi tetap pada baris yang berisi teks.
- Fokus keyboard dan TalkBack: urutan atas ke bawah, tombol utama terakhir.

## 10. Urutan implementasi yang disarankan
1. Pasang font, `NemoTheme`, dan cek dengan `NemoPreviewSample.kt` (mode Night).
2. Bangun komponen dasar: Button, Chip, Pill, TextField, ListRow.
3. Bangun Tasks + Add Task + Task Detail (alur inti).
4. Focus Setup + Focus Active + overlay.
5. Wellbeing + Insights.
6. Onboarding + Settings + sub-halaman.
7. Intervention, notifikasi, lalu tema Day.
