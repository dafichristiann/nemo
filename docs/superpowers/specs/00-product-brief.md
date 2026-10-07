# Product Brief — Anchor (Personal Focus Assistant)

> **Status:** Draft untuk review. Belum final sampai disetujui.
> **Bahasa dokumen:** Bahasa Indonesia · **Bahasa UI aplikasi:** English (localization-ready)
> **Tanggal:** 2026-10-07

---

## 1. Ringkasan Satu Paragraf

**Anchor** adalah aplikasi Android personal yang mengubah niat menjadi tindakan dengan cara *menyadari* ketika perilaku menjauh dari apa yang sudah direncanakan, *menunjukkan* konsekuensinya secara lembut, dan *membantu* kembali fokus — tanpa menghakimi, tanpa memaksa. Anchor menghubungkan dua dunia yang biasanya terpisah: **niat** (to-do list) dan **perilaku** (penggunaan perangkat).

**Tagline kerja:** *"Help me do what I said was important."*

---

## 2. Masalah Inti

| Alat yang ada | Yang ia tahu | Yang ia tidak tahu |
|---|---|---|
| To-Do List | **Niat** — apa yang kamu rencanakan | Kamu sedang menjauh darinya |
| Digital Wellbeing | **Perilaku** — berapa lama kamu pakai HP | Apa yang *seharusnya* kamu kerjakan |

**Titik kosong:** tidak ada yang menghubungkan **niat** dengan **perilaku**. Akibatnya:
- User tahu harus mengerjakan tugas, tapi tetap menunda.
- User baru sadar sudah scrolling 40 menit **setelah** kejadian.
- Tidak ada gambaran konkret tentang **konsekuensi waktu** dari penundaan.

Anchor mengisi kekosongan ini.

---

## 3. Core Loop (dijaga di semua fase)

```
PLAN
  → buat task (apa, kapan, seberapa penting, berapa lama)
OBSERVE
  → kumpulkan data penggunaan perangkat secara lokal
DETECT DRIFT
  → bandingkan perilaku dengan baseline personal
CONTEXTUAL INTERVENTION
  → hubungkan drift dengan task yang tertunda, beri pilihan
FOCUS
  → bantu user masuk ke sesi fokus
COMPLETE
  → task selesai
LEARN
  → pola dipelajari dari data nyata
BESOK LEBIH BAIK
  → rekomendasi lebih relevan
```

---

## 4. Prinsip Produk (Filter Setiap Fitur)

1. **Awareness > Control** — bantu sadar, bukan larang.
2. **Guidance > Punishment** — tunjukkan jalan, bukan salahkan.
3. **Consistency > Perfection** — dukung kebiasaan, bukan kesempurnaan.
4. **Autonomy > Forced Behavior** — user tetap pegang kendali.
5. **Personal Baseline > Universal Rules** — ukur relatif terhadap kebiasaan user sendiri.

**Pertanyaan filter wajib:**
> *"Apakah fitur ini membantu user mengambil tindakan bermakna?"*
> Jika tidak → buang atau turunkan prioritas.

---

## 5. Untuk Siapa

**Persona utama (satu-satunya untuk MVP):**
Mahasiswa dengan banyak tugas/proyek. Sering berpindah antara **coding**, **belajar**, dan **aktivitas lain**. Mengetahui apa yang harus dikerjakan, tetapi kadang menunda memulai, lalu terdistraksi oleh scrolling/entertainment.

> Perilaku detail (jam produktif, app pemicu distraksi) **tidak diasumsikan** — akan dipelajari dari data nyata dan bisa dikonfigurasi.

---

## 6. Definition of Done

**Target akhir (MVP):** Phase 1–5 berjalan baik di perangkat nyata.

- ✅ Smart Todo
- ✅ Screen Time / Usage Tracking
- ✅ Contextual Intervention
- ✅ Focus Mode
- ✅ Future View
- ✅ Domino Effect
- ✅ AI Personal Insight

**Post-MVP (Phase 6):** Advanced blocking + Cloud sync — arsitektur harus siap, tapi **tidak** diimplementasi sekarang.

---

## 7. Constraint Kunci (mengikat desain)

| Aspek | Keputusan |
|---|---|
| Platform | Android native · Kotlin · Jetpack Compose · Material 3 |
| minSdk | API 28 (Android 9) |
| targetSdk | Latest stable saat development |
| Device target | Physical Android · RAM 3–4 GB |
| Arsitektur | Local-first · single Gradle module · feature-first + layered |
| Persistence | Room + DataStore |
| Background | WorkManager · AlarmManager (kritis) · Foreground Service (Focus aktif) |
| Wellbeing | UsageStatsManager |
| AI | Gemini Cloud **optional** · hanya aggregated data · wajib rule-based fallback |
| Blocking | Accessibility Service eksperimental · gentle by default · wajib emergency exit |
| Intervensi | Max ~3 bermakna/hari · konteks-driven · ada cooldown |
| Distraksi | Hybrid auto-categorize + manual override · personal baseline > kategori |
| Lokalisasi | UI English default · string resources sejak awal |

---

## 8. Tanda & Kategori Status

Selama fase discovery, setiap keputusan ditandai:

- `[OPEN QUESTION]` — belum diputuskan
- `[ASSUMPTION]` — diasumsikan sementara, perlu koreksi
- `[TECHNICAL RISK]` — risiko teknis
- `[EXPERIMENT REQUIRED]` — perlu diuji di device nyata
- `[DECISION REQUIRED]` — butuh approval user

---

## 9. Non-Goals (yang secara sadar TIDAK dibangun)

- ❌ Bukan surveilans / parental control
- ❌ Bukan diagnosis medis atau klaim kesehatan mental
- ❌ Bukan gamification berat (poin/leaderboard/streak yang menekan)
- ❌ Bukan AI chatbot
- ❌ Bukan app-blocker sebagai fondasi utama
- ❌ Bukan cloud-first (backend hanya untuk Phase 6, itupun opsional)
- ❌ Tidak mengirim raw usage data ke cloud tanpa alasan & desain privasi

---

## 10. Pertanyaan Kritis Penjaga Arah

Setiap fitur harus lolos:

1. Apakah ini membantu user **sadar**, **memutuskan**, **fokus**, atau **menyelesaikan**?
2. Apakah ini menambah **kendali** atau justru memberi **kelegaan**?
3. Apakah ini bekerja tanpa internet dan tanpa izin khusus?
4. Apakah klaimnya didukung data nyata (bukan tebakan)?

Jika ada yang gagal → kembali ke desain.
