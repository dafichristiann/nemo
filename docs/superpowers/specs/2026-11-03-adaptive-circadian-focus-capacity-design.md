# Spesifikasi Teknis & Desain: Kurva Kapasitas Fokus Harian Adaptif (Adaptive Circadian Curve)

**Tanggal:** 3 November 2026  
**Status:** Spesifikasi Arsitektur Lengkap (Production-Ready Spec)  
**Dokumen Referensi:** `docs/superpowers/design/01-prd.md`, `docs/superpowers/design/03-database-schema.md`, `docs/superpowers/design/07-screens.md` §4.11 (`WellbeingScreen`), §4.10 (`FocusScreen`)

---

## 1. Ringkasan Eksekutif & Tujuan

### 1.1 Latar Belakang Masalah
Fitur *Kapasitas Fokus Harian* pada tab Waktu Layar (`WellbeingScreen`) saat ini menggunakan 7 batang statis berjam dinding kaku (07:00 bangun, 09:30 puncak fokus, 13:30 lelah, dst.).
* **Tidak Relevan bagi Variasi Jam Bangun & Nocturnal:** Pengguna yang bangun pukul 11:00 atau 13:00 mendapati grafik menampilkan "Jam Puncak" pada saat mereka baru bangun atau bahkan sedang tidur.
* **Tidak Ada Pembelajaran Adaptif:** Logika saat ini hanya menampilkan template konstan tanpa belajar dari riwayat fokus riil pengguna di perangkat.

### 1.2 Tujuan Solusi
1. **Relative Circadian Anchoring:** Menggeser seluruh kurva 24 jam berpatokan pada **jam bangun hari ini** ($T_{\text{wake}}$).
2. **Bayesian Shrinkage Model:** Menggabungkan kurva template biologis 5-fase (*prior*) dengan rata-rata rating sesi fokus riil pengguna (*evidence*) menggunakan konstanta kehati-hatian $k=5$.
3. **Continuous Monotone Cubic Spline:** Visualisasi kurva kontinu bebas *overshoot* yang memetakan zona *Peak Window*, titik lelah (*Circadian Dip*), dan indikator *NOW*.
4. **Koreksi Manual & Fallback:** Deteksi jeda layar-mati $\ge 4$ jam otomatis, dengan kemampuan koreksi manual via *TimePicker*, serta fallback median 3 hari saat begadang ekstrem.
5. **100% On-Device & Zero Network:** Semua perhitungan dieksekusi secara lokal di database SQLite/Room perangkat.

---

## 2. Arsitektur Data & Perubahan Skema Database

### 2.1 Migrasi Database Room (v14 $\rightarrow$ v15)
Tabel `focus_session` perlu menyimpan rating subjektif pengguna:

```sql
-- Migration 14 -> 15
ALTER TABLE focus_session ADD COLUMN rating INTEGER DEFAULT NULL;
```

* **Kolom Baru di `FocusSessionEntity`:**
  `val rating: Int? = null` (Nilai integer valid: 1, 2, 3, 4, 5).
* **Indeks:** Tidak membutuhkan indeks tambahan karena filter rating dilakukan saat agregasi sesi 30 hari terakhir.

### 2.2 Penyimpanan Override Jam Bangun (`AppSettingsEntity`)
Tambahkan field pada entitas `app_settings` (atau `DataStore` preferensi lokal):
* `manualWakeTimeEpochDay: Long? = null` (Tanggal override berlaku).
* `manualWakeTimeMinutes: Int? = null` (Menit dalam hari, misal $12 \times 60 + 30 = 750$ untuk 12:30).

---

## 3. Logika Domain & Algoritma Matematis

### 3.1 Deteksi Jam Bangun (`DetectWakeTimeUseCase`)
* **Input:** Waktu saat ini $T_{\text{now}}$, log sesi dari `UsageSessionDao.getBetween(T_now - 24h, T_now)`.
* **Langkah Evaluasi:**
  1. Periksa apakah ada manual override untuk `todayEpochDay`. Jika ada, gunakan nilai tersebut.
  2. Urutkan sesi pemakaian HP dalam 24 jam terakhir berdasarkan `startTime`.
  3. Cari interval jeda layar mati terpanjang: $\Delta t = \text{session}[i+1].\text{startTime} - \text{session}[i].\text{endTime}$.
  4. **Kondisi Normal:** Jika $\Delta t \ge 4\text{ jam}$, maka $T_{\text{wake}} = \text{session}[i+1].\text{startTime}$.
  5. **Kondisi Ekstrem / Begadang (Fallback 1B):** Jika $\Delta t < 4\text{ jam}$, ambil **median jam bangun dari 3 hari sebelumnya**. Jika data 3 hari belum tersedia, fallback ke default pukul `08:00`.
* **Output:** `WakeTimeResult(time: LocalTime, isManual: Boolean, confidence: WakeConfidence)`.

---

### 3.2 Penilaian Sesi Fokus & Normalisasi Rating (`RateFocusSessionUseCase`)
* **Trigger:** Muncul di `FocusScreen` (pada kartu `FocusFinishedBanner`) saat sesi fokus selesai:
  * Syarat: `actualDurationMs >= 15 * 60 * 1000L` (Sesi $\ge 15$ menit).
  * Non-blokir: Pengguna bisa langsung keluar tanpa memberi rating.
* **Skala Rating:** 1 (Sangat Terdistraksi) hingga 5 (Deep Flow Maksimal).
* **Normalisasi Rating Individu:**
  Untuk meniadakan bias subjektif antar-pengguna (misal pengguna tipe pelit nilai vs selalu memberi nilai 5):
  $$\mu = \frac{1}{M}\sum_{i=1}^M R_i, \quad \sigma = \sqrt{\frac{1}{M}\sum_{i=1}^M (R_i - \mu)^2}$$
  $$R_{\text{norm}} = \text{clamp}\left(0.5 + \frac{R - \mu}{2 \cdot (\sigma + 0.1)}, 0.0, 1.0\right)$$
  *(Jika total sampel riwayat pengguna $M < 5$, gunakan konversi linier standar: $R_{\text{norm}} = \frac{R - 1}{4}$.)*

---

### 3.3 Bayesian Hourly Bucket Model
Siklus 24 jam dibagi menjadi **12 bucket berdurasi 2 jam**, dihitung dari jam bangun ($T_{\text{wake}}$):

| Bucket ($b$) | Rentang Pasca-Bangun | Fase Sirkadian | Prior Biologis ($T_{\text{prior}}$) |
| :---: | :---: | :--- | :---: |
| **0** | $T+0\text{j} \dots T+2\text{j}$ | Inersia Bangun & Transisi | `0.35` |
| **1** | $T+2\text{j} \dots T+4\text{j}$ | **Peak Window #1 (Puncak Pagi/Awal)** | `0.95` |
| **2** | $T+4\text{j} \dots T+6\text{j}$ | Produktivitas Tinggi Berkelanjutan | `0.85` |
| **3** | $T+6\text{j} \dots T+8\text{j}$ | **Circadian Dip (Lembah Energi/Kantuk)** | `0.40` |
| **4** | $T+8\text{j} \dots T+10\text{j}$ | **Peak Window #2 (Second Wind / Sore-Malam)** | `0.75` |
| **5** | $T+10\text{j} \dots T+12\text{j}$ | Fokus Menengah / Transisi Santai | `0.60` |
| **6** | $T+12\text{j} \dots T+14\text{j}$ | Penurunan Energi Sore/Malam | `0.45` |
| **7** | $T+14\text{j} \dots T+16\text{j}$ | Fase Melatonin & Menjelang Istirahat | `0.25` |
| **8** | $T+16\text{j} \dots T+18\text{j}$ | Fase Tidur Awal | `0.15` |
| **9** | $T+18\text{j} \dots T+20\text{j}$ | Fase Tidur Dalam (Restoratif) | `0.10` |
| **10** | $T+20\text{j} \dots T+22\text{j}$ | Fase Tidur REM | `0.15` |
| **11** | $T+22\text{j} \dots T+24\text{j}$ | Persiapan Bangun | `0.25` |

#### Formula Pembaruan Bayes per Bucket:
$$\text{Kapasitas}(b) = \frac{n_b \cdot \bar{R}_{\text{norm}}(b) + k \cdot T_{\text{prior}}(b)}{n_b + k}$$
* $n_b$: Jumlah sesi fokus ter-rating pada bucket $b$ dalam rentang 30 hari terakhir.
* $\bar{R}_{\text{norm}}(b)$: Rata-rata rating normalisasi sesi di bucket $b$.
* $k = 5$: Bobot kehati-hatian (*pseudo-observations*), menjamin kurva tetap rasional saat data baru sedikit.

---

### 3.4 Interpolasi Kurva Kontinu (Monotone Cubic Hermite Spline)
Untuk mencegah *overshoot* (misalnya kurva melengkung ke atas $> 1.0$ atau ke bawah $< 0.0$ yang kerap terjadi pada Bezier standar):
* Titik simpul: 12 titik pusat bucket $(x_b, y_b)$ ditambah titik penutup di $T+24\text{j}$.
* Gradien tangen $d_b$ dihitung dengan rumus Fritsch-Carlson:
  $$\Delta_b = \frac{y_{b+1} - y_b}{x_{b+1} - x_b}$$
  Jika $\Delta_b = 0$, maka $d_b = d_{b+1} = 0$.
* Kurva digambar di Jetpack Compose `Canvas` melalui `Path.cubicTo()` menggunakan titik kontrol Hermite terhitung.

---

## 4. Rincian Antarmuka Pengguna (UI Specification)

### 4.1 Modifikasi di `WellbeingScreen`
Komponen `CircadianArcCard` digantikan oleh `AdaptiveCircadianCurveCard`:
1. **Header Row:**
   * Teks Judul: `"Kapasitas Fokus Harian"` (Geist Bold 12sp).
   * Badge Status: `"ADAPTIF"` berlatar belakang gelap dengan dot hijau.
2. **Sub-Header Interaktif (Wake Anchor):**
   * Teks: `"Bangun: 12:30 (Otomatis)"` atau `"Bangun: 10:00 (Manual)"`.
   * Tombol Aksi: Ikon pensil kecil berukuran tap target $32\times32\text{dp}$. Mengetuk ini memunculkan `TimePickerDialog` untuk koreksi jam bangun hari ini.
3. **Area Kanvas Kurva (Tinggi 110dp):**
   * **Garis Kurva:** Lebar 2.5dp, warna `Nemo.colors.ink`.
   * **Shading Area Peak:** Area di bawah kurva saat nilai $\ge 0.70$ diarsir warna `Nemo.colors.ink.copy(alpha = 0.10f)`.
   * **Garis Penunjuk "SEKARANG":** Garis vertikal putus-putus (`pathEffect = PathEffect.dashPathEffect`) tepat di menit saat ini, dengan lingkaran pin padat di atas kurva dan teks label `"SEKARANG"` di atasnya.
   * **Sumbu X:** 5 label jam dinding berformat GeistMono 9sp:
     $T_{\text{wake}}$, $T+4\text{j}$, $T+8\text{j}$, $T+12\text{j}$, $T+18\text{j}$.
4. **Touch Inspection (Tooltip):**
   * Pengguna dapat menggeser jari di atas kurva untuk melihat kapasitas dan rekomendasi pada jam tertentu (misal: *"15:30 • 88% — Puncak Kedua"*).

### 4.2 Modifikasi di `FocusScreen` (Rating Selesai Sesi)
Pada `FocusFinishedBanner` (setelah sesi $\ge 15$ menit):
* Tambahkan baris: `"Seberapa fokus tadi?"`
* 5 tombol lingkaran bernomor 1 s.d. 5 (atau ikon ekspresi Nemo: Dari mengantuk hingga sangat tajam).
* Mengetuk salah satu tombol langsung menyimpan rating ke database secara instan dan menampilkan teks terima kasih halus.

---

## 5. Rencana File yang Terkena Dampak (File Touchpoints)

1. **Database & Data:**
   * `app/.../core/data/entity/FocusSessionEntity.kt` (tambah kolom `rating`).
   * `app/.../core/data/NemoDatabase.kt` (upgrade version 14 $\rightarrow$ 15 + migration SQL).
   * `app/.../core/data/dao/FocusSessionDao.kt` (kueri update rating & get rated sessions 30 hari).
   * `app/.../core/data/dao/AppSettingsDao.kt` (tambah field manual wake override).
2. **Domain Layer:**
   * `app/.../core/domain/usecase/circadian/DetectWakeTimeUseCase.kt` (logika deteksi jeda 4 jam & fallback).
   * `app/.../core/domain/usecase/circadian/ComputeCircadianCurveUseCase.kt` (Bayesian bucket & spline points).
   * `app/.../core/domain/usecase/focus/RateFocusSessionUseCase.kt` (simpan rating & normalisasi).
3. **UI / Feature Layer:**
   * `app/.../feature/wellbeing/WellbeingScreen.kt` (penggantian `CircadianArcCard` dengan kanvas kurva spline).
   * `app/.../feature/wellbeing/WellbeingViewModel.kt` (ekspos state kurva, jam bangun, & handler manual edit).
   * `app/.../feature/focus/FocusScreen.kt` (komponen rating 1-5 di kartu selesai sesi).
   * `app/.../feature/focus/FocusViewModel.kt` (handler aksi rating sesi).

---

## 6. Skenario Pengujian & Kriteria Lolos

| ID | Kategori | Skenario Pengujian | Hasil yang Diharapkan |
| :---: | :--- | :--- | :--- |
| **TC-01** | Deteksi Bangun | Pengguna tidur 05:00, buka HP pertama kali 12:30 | Jam bangun terdeteksi `12:30`. Sumbu kurva mulai dari 12:30. |
| **TC-02** | Begadang Total | Pengguna tidak pernah mematikan layar $>2$ jam semalaman | Fallback otomatis ke median 3 hari sebelumnya tanpa error. |
| **TC-03** | Override Jam | Pengguna mengoreksi jam bangun dari 12:30 menjadi 10:00 via TimePicker | Kurva langsung ter-render ulang dengan titik awal 10:00. |
| **TC-04** | Filter Rating | Sesi fokus berlangsung 12 menit | Komponen rating tidak tampil di banner selesai fokus. |
| **TC-05** | Rating Valid | Sesi fokus 25 menit diberi rating 5 | Kolom `rating` di `focus_session` terisi 5, kurva di bucket terkait terangkat. |
| **TC-06** | Spline Boundary | Nilai Bayes berada di rentang 0.0 s.d. 1.0 | Interpolasi spline tidak pernah *overshoot* $<0$ atau $>1.0$ (Fritsch-Carlson). |
| **TC-07** | Indikator NOW | Waktu perangkat menunjukkan pukul 16:00 | Pin vertikal berada tepat di koordinat X yang setara dengan selisih waktu dari $T_{\text{wake}}$. |
