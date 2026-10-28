# Product Brief — Nemo (Personal Focus Assistant)

> **Status:** Approved / Baseline · **Version:** 0.2
> **Tanggal:** 2026-10-07 (v0.1) · 2026-10-27 (v0.2)
> **Dokumen terkait:** `01-prd.md`, `justmd/fitur-aplikasi-nemo-maskot.md`
> **Catatan:** Dokumen ini adalah *ringkasan satu halaman* yang menjelaskan **kenapa**
> Nemo ada. PRD (`01-prd.md`) menjelaskan **apa** dan **bagaimana** perilakunya.

---

## 1. Satu Kalimat

> **Nemo** (nama paket `com.anchor.app`) adalah asisten fokus personal untuk Android yang menyadari ketika kamu
> menjauh dari tugas penting, lalu dengan lembut membantumu kembali — tanpa memaksa,
> tanpa menghakimi, tanpa mengirim datamu ke mana pun.

## 2. Masalah

Orang tahu apa yang penting, tapi tetap menunda. Ketika menunda, distraksi terasa
nyaman dan **tidak terasa saat terjadi** — kesadaran baru datang *setelah* 40 menit
scrolling. Akibatnya penundaan terasa abstrak, sampai waktu bebas malam ini habis.

Tiga masalah inti (lihat PRD §1):
1. **Niat ≠ Tindakan**
2. **Distraksi tak terasa saat terjadi**
3. **Penundaan terasa abstrak**

## 3. Solusi

Nemo adalah **jembatan** antara dua hal yang biasanya terpisah:

| Yang ada | Yang kurang | Peran Nemo |
|---|---|---|
| Smart Todo | Kesadaran perilaku | Menghubungkan task dengan perilaku nyata |
| Digital Wellbeing | Konteks tugas | Memberi makna pada angka pemakaian HP |
| App Blocker | Kelembutan & otonomi | Membantu, bukan memaksa (blocking bukan fondasi) |

**Pembeda utama:** intervensi kontekstual berbasis **baseline personal**, bukan aturan
universal ("scrolling > 30 menit = buruk" ❌).

## 4. Untuk Siapa

**Single-user, personal.** Target MVP adalah pemilik aplikasi sendiri:

> **"Mahasiswa yang tahu tapi menunda."**
> Tahu apa yang harus dikerjakan, kadang menunda memulai, lalu terdistraksi
> scrolling/entertainment.

Bukan aplikasi rilis publik, bukan multi-user, bukan parental control.

## 5. Prinsip (dari PRD §4)

1. **Awareness > Control**
2. **Guidance > Punishment**
3. **Consistency > Perfection**
4. **Autonomy > Forced Behavior**
5. **Personal Baseline > Universal Rules**
6. **Wajah Aplikasi (Maskot Pendamping):** satu karakter maskot (anak laki-laki chibi bergaya tinta hitam, kepala saja, 12 ekspresi) yang menemani pengguna di seluruh alur 19 halaman, merefleksikan emosi secara tenang dan dekoratif tanpa menghakimi.

**Nada:** calm · supportive · concise · non-judgmental.

## 6. Ruang Lingkup MVP

MVP = **Phase 1–5** berjalan baik di perangkat nyata, **local-first**, **tanpa backend**.

| Phase | Nama | Inti |
|---|---|---|
| 1 | Smart Todo | Task CRUD, recurring (Daily/Weekly), subtask, priority, overdue banner, effort-based completion |
| 2 | Digital Wellbeing | UsageStats lokal, screen time, baseline personal, pruning |
| 3 | Intervention + Focus | Intervensi kontekstual (max 3/hari, cooldown 2 jam), Focus Mode (Gentle/Strict/Custom) |
| 4 | Future View + Domino | Konsekuensi waktu penundaan, dependency task |
| 5 | AI Personal Insight | Daily reflection & pola (AI opsional via Gemini + fallback rule-based) |
| 6 | Advanced Blocking + Sync | **Post-MVP** |

Detail lengkap: `01-prd.md` §10–§11, dan `05-mvp-scope.md`.

## 7. Bukan Apa

- ❌ Surveilans / parental control
- ❌ Diagnosis medis / klaim kesehatan mental
- ❌ Gamification berat
- ❌ AI chatbot
- ❌ Blocking sebagai fondasi
- ❌ Backend / cloud sync (di MVP)
- ❌ Kirim raw usage ke cloud

## 8. Definisi Sukses (personal, kualitatif)

Bukan vanity metrics. Yang penting (PRD §17):

- Task completion & delay berkurang
- Focus session tuntas
- Intervensi terasa **membantu** (bukan mengganggu)
- Pemulihan fokus setelah intervensi
- Konsistensi harian

⚠️ "Screen time turun" **bukan** otomatis "lebih produktif".

## 9. Batasan Teknis Kunci

- **Local-first**, Room sebagai single source of truth.
- Sumber kebenaran data usage = `UsageStatsManager` (Android), **bukan** AI.
- `PACKAGE_USAGE_STATS` = special permission (opt-in manual).
- Raw `UsageSession` di-pruning otomatis (14–30 hari).
- Splash intro memakai urutan gambar animasi maskot (tanpa video player).
- Target: RAM < 150 MB, baterai ≤ 2–3%/24 jam.

## 10. Peta Dokumen

| Dokumen | Isi |
|---|---|
| `00-product-brief.md` | Ringkasan ini — *why* |
| `01-prd.md` | Product requirements — *what & behavior* |
| `02-architecture.md` | Arsitektur teknis & modul |
| `03-database-schema.md` | Skema Room & model data |
| `04-intervention-engine.md` | Mesin deteksi drift & intervensi |
| `05-mvp-scope.md` | Ruang lingkup & fase MVP |
| `06-design-system.md` | Token visual & komponen |
| `07-screens.md` | Inventaris 19 layar & alur interaksi maskot |
| `README.md` | Index folder design |

---

> **CATATAN:** Brief ini tidak menambah aturan perilaku baru. Semua aturan mengikat
> tetap di `01-prd.md`. Brief hanya memberi konteks tingkat tinggi.
