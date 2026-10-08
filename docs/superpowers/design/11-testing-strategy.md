# Testing Strategy — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 §10, §12 · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` §10 (acceptance), §12 (non-functional); `02-architecture.md` §8
> **Catatan:** Strategi ini memastikan setiap **Acceptance Criteria PRD** dan **target
> non-fungsional** benar-benar terverifikasi. Tidak menambah aturan perilaku baru.

---

## 0. Prinsip Pengujian

1. **Setiap Acceptance Criteria PRD → minimal satu test.**
2. **Setiap fallback (izin ditolak/OEM batasi) → test.** Tidak boleh crash.
3. **Domain logic (drift, future view, agregasi, recurring) wajib unit test.**
4. **Data uji deterministik** — pakai `TimeProvider`/clock injectable, bukan `now()` langsung.
5. **Uji di real device low-RAM (3–4 GB)** untuk target performa.

---

## 1. Piramida Test

```
        ▲  Instrumented / E2E (sedikit, alur kritis)
       ╱ ╲
      ╱   ╲   UI Compose test (state rendering)
     ╱     ╲
    ╱       ╲ DAO / Worker / Migration (Room + WorkManager Test)
   ╱─────────╲
  ╱  Unit (domain, use cases)  ╲   ← TERBANYAK
 └──────────────────────────────┘
```

| Level | Fokus | Tool | Target cakupan |
|---|---|---|---|
| Unit | Use cases, drift math, future view, aggregation, recurring | JUnit5 + Turbine | Domain utama: ~80% |
| DAO | Query, relasi, **migration** | Room Testing + `MigrationTestHelper` | Semua DAO |
| Worker | Constraint, idempotency, retry | WorkManager Test | Semua worker |
| UI | Rendering `UiState` | Compose UI Test | Layar utama |
| E2E | Alur end-to-end kritis | Espresso/UIAutomator (sparing) | 5–8 alur |

---

## 2. Mapping: Acceptance Criteria PRD → Test

### 2.1 Smart Todo (§10.1)

| AC | Skenario | Level |
|---|---|---|
| Buat task → tersimpan & di Today | Create task HIGH + deadline | Unit + UI |
| Overdue | Task deadline lewat muncul tanda halus + aksi Reschedule | Unit + UI |
| Complete | Status → DONE, `completedAt` diisi, muncul di Completed, progress 100% | Unit |
| Recurring DAILY/WEEKLY | Definition menerbitkan instance saat jatuh tempo; riwayat tetap | Unit |
| Idempotency recurring | Tidak dobel instance untuk hari yang sama | Unit |
| Overdue tidak auto-move | Tetap di asal; user yang pilih aksi | Unit |

### 2.2 Digital Wellbeing (§10.2)

| AC | Skenario | Level |
|---|---|---|
| Tanpa izin → panduan, tidak crash | Permission denied → empty state | UI + Instrumented |
| Data 7 hari → tampil screen time/top apps/tren | Aggregation dari data lokal | Unit |
| Pruning | Raw > retensi terhapus; agregat tetap | Worker |
| OEM membatasi (data kosong) | Tampilkan pesan jujur; task tetap jalan | Unit + UI |

### 2.3 Drift Detection (§10.3)

| AC | Skenario | Level |
|---|---|---|
| Deviasi > threshold + task penting → drift | Hitung deviasi vs baseline | Unit |
| Baseline kurang → tidak klaim | `sampleDays < MIN` → null | Unit |
| Bahasa netral | Pesan tanpa diagnosis | Unit (string assert) |

### 2.4 Intervention (§10.4)

| AC | Skenario | Level |
|---|---|---|
| Drift + task → boleh intervensi | Evaluate → Show | Unit |
| "Later" → cooldown 2 jam | Setelah Later, drift sama → Suppress("cooldown") | Unit |
| Kuota habis → tahan | 3 intervensi hari ini → Suppress("quota") | Unit |
| Detection ≠ Attempt | Suppress **tidak** mengurangi kuota | Unit |
| Notification ditolak → in-app | Fallback channel | Unit + UI |
| Reset harian | Hari lokal baru → kuota reset | Unit |

### 2.5 Focus Mode (§10.5)

| AC | Skenario | Level |
|---|---|---|
| Gentle mulai | Timer jalan, no blocking | Unit + UI |
| Strict blocking + emergency exit | App diblokir, exit tetap aksesibel | Instrumented |
| Finish → catat sesi + tawaran update | `FocusSession` tersimpan | Unit |
| Accessibility mati saat Strict | Fallback Gentle + beri tahu | Unit |

### 2.6 Future View (§10.6) & Domino (§10.7)

| AC | Skenario | Level |
|---|---|---|
| Dua skenario | Mulai sekarang vs menunda | Unit |
| Tanpa estimasi → heuristic + ditandai | Default 60m/23:59; asumsi tampil | Unit |
| Domino "may affect" | Bahasa estimatif; hitung count | Unit |
| Siklus dependency ditolak | A→B→A dicegah + dijelaskan | Unit |

### 2.7 AI Insight (§10.8) & Daily Reflection (§10.9)

| AC | Skenario | Level |
|---|---|---|
| AI off/offline → rule-based, no error | Fallback | Unit |
| AI aktif + data cukup → insight schema | Parse & simpan | Unit |
| **Payload raw ditolak** | Audit menolak raw session | Unit (WAJIB) |
| Respons di luar schema → tolak & fallback | Validasi | Unit |
| Hari tanpa data usage → sembunyikan bagian | UI hide | UI |

### 2.8 Settings & Privacy (§10.10)

| AC | Skenario | Level |
|---|---|---|
| Onboarding tak memaksa | Smart Todo jalan tanpa izin | Instrumented |
| Hapus semua data | Tabel terkait terhapus | Unit + DAO |
| Default privacy | tracking & AI = false | Unit |

---

## 3. Uji Non-Fungsional (PRD §12)

| Target | Cara uji | Alat |
|---|---|---|
| RAM < 150 MB | Profil memori saat skenario normal | Android Studio Profiler |
| Baterai ≤ 2–3% / 24 jam | Ukur dengan `Battery Historian` / uji 24 jam | Battery Historian |
| Offline 100% | Mode pesawat; semua fitur inti tetap jalan | Manual + test |
| Tak crash saat izin ditolak | Matikan tiap izin, jalankan alur | Manual + instrumented |
| Tak crash saat migrasi gagal | Test migrasi semua versi | `MigrationTestHelper` |
| Lokalisasi | Semua string via resource | Lint (HardcodedText) |

**Baseline device:** minimal 1 device **RAM 3–4 GB** + 1 device modern.

---

## 4. Uji Edge Case (wajib ada)

- Deadline di masa lalu saat dibuat → langsung overdue.
- Estimasi durasi 0/null → Future View pakai heuristic.
- Semua subtask selesai → task boleh auto-suggest DONE (tidak paksa).
- Judul kosong → tidak bisa disimpan.
- Timezone berubah / DST / lewat tengah malam.
- UsageStats kosong (OEM) → pesan jujur.
- Task tanpa dependensi → Domino empty state (bukan error).
- Hapus task yang jadi dependency → tawarkan pisahkan/ubah.
- Baterai lemah saat Focus → tidak memaksa + beri tahu.
- Layar mati lama saat Focus → sesi tetap tercatat.

---

## 5. Test Data & Determinisme

- `TimeProvider` **injectable** — jangan `System.currentTimeMillis()` langsung.
- Seed data uji: hari/sesi/task yang dapat direproduksi.
- Fake repositories untuk unit test (bukan Room).
- Untuk DAO/worker: in-memory Room / `TestListenableWorkerBuilder`.

---

## 6. Definition of Done (per fitur)

Suatu fitur dianggap **done** bila:
- [ ] Acceptance Criteria terkait punya test yang lulus.
- [ ] Fallback (izin ditolak/dibatasi) teruji — tidak crash.
- [ ] Tidak menambah egress jaringan (kecuali AI opt-in).
- [ ] Teks via string resources (idak hardcode).
- [ ] Lulus lint tanpa error.
- [ ] Diuji minimal di 1 device nyata (termasuk low-RAM untuk fitur berat).

---

## 7. Continuous Quality

- CI: jalankan unit + DAO test tiap commit (lokal/GitHub Actions opsional).
- Lint + detekt sebagai gate.
- `Espresso`/instrumented dijalankan manual sebelum menutup fase.
- Snapshot Compose untuk layar utama (opsional) → deteksi regresi visual.

---

## 8. Referensi Silang

- Acceptance Criteria → `01-prd.md` §10
- Target non-fungsional → `01-prd.md` §12
- Arsitektur & worker → `02-architecture.md` §5, §8
- Skema & migrasi → `03-database-schema.md` §9
- Risiko regresi → `10-risk-register.md` R36
