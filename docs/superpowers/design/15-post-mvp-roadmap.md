# Post-MVP Roadmap — Phase 6 (Backlog Ide, Bukan Komitmen)

> **Status:** Backlog perencanaan · **Versi:** 0.1 · **Tanggal:** 2026-10-08
> **Sumber:** `01-prd.md` §11/§18 (Phase 6 = Post-MVP), `05-mvp-scope.md` §3/§7,
> `12-sprint-backlog.md` §Post-MVP, `10-risk-register.md` (R34)
> **Dokumen terkait:** `09-privacy-and-security.md`, `08-permissions.md`

> **Update v0.3 (2026-10-08):** **P6-8 (Insight AI)** telah **dipromosikan** ke scope aktif
> sesuai PRD v0.3 §10.8/§13.1 — AI nyata via **Gemini REST opt-in** (agregat saja, fallback
> rule-based wajib). Item lain di dokumen ini tetap backlog.

> ⚠️ **PRD terkunci.** Phase 6 **secara sengaja di luar MVP** (`05-mvp-scope.md` §7,
> `01-prd.md` §11). Dokumen ini **hanya mencatat ide** agar tidak hilang, **bukan**
> komitmen atau izin untuk mulai mengerjakan. Untuk memindahkan item ke MVP, **ubah PRD
> terlebih dahulu** (hierarki otoritas `README.md` §2). Ini mitigasi risiko **R34 (scope
> creep)**.

---

## 1. Ringkasan

| # | Ide | Nilai | Kompleksitas | Risiko | Prasyarat |
|---|---|---|---|---|---|
| P6-1 | Advanced Blocking (Strict/Custom sungguhan) | Sedang | Tinggi | Tinggi | Spike B lanjutan, revisi kebijakan Play |
| P6-2 | Cloud Sync (multi-device) | Sedang | Tinggi | Tinggi | Desain E2E encryption, backend |
| P6-3 | Backup / Restore lokal (file) | Sedang | Rendah | Rendah | Skema ekspor stabil |
| P6-4 | Ekspor data (JSON/CSV) | Sedang | Rendah | Rendah | Definisi format publik |
| P6-5 | Akun opsional (login) | Rendah | Tinggi | Tinggi | Backend, kebijakan privasi baru |
| P6-6 | Home-screen widget / Quick Settings tile | Sedang | Sedang | Rendah | – |
| P6-7 | Wear OS companion | Rendah | Sedang | Sedang | – |
| P6-8 | Insight AI on-device (tanpa egress) | Sedang | Tinggi | Sedang | Model on-device, ukuran APK |
| P6-9 | Lokalisasi tambahan (mis. id penuh + lain) | Sedang | Rendah | Rendah | String resources (sudah siap) |

Urutan di atas **bukan** urutan pengerjaan; hanya inventaris.

---

## 2. Prinsip Penjaga (Guardrails)

Setiap ide Phase 6 **wajib** tetap menghormati prinsip yang sudah dikunci:

- **Local-first & privacy by default.** Tidak ada egress data user tanpa opt-in eksplisit.
- **Raw usage tidak pernah keluar device.** Sync (bila ada) mengirim agregat/enkripsi E2E.
- **Awareness > Control.** Blocking lanjutan tetap **tanpa paksaan**; emergency exit selalu ada.
- **Autonomy > Forced Behavior.** Tidak ada gamifikasi berat/tekanan.
- **No crash on degradation.** Semua fitur baru punya fallback graceful.

> Bila sebuah ide melanggar salah satu poin di atas, **ide itu ditolak**, bukan mengubah prinsip.

---

## 3. Detail Ide (Ringkas)

### P6-1 — Advanced Blocking
- **Deskripsi:** Strict/Custom memblokir app pengganggu sungguhan (bukan sekadar Gentle).
- **Catatan:** Spike B (`13-spike-b-accessibility.md`) sudah menyimpulkan blocking **tidak
  andal lintas OEM** dan berisiko ditolak review. Bila dikerjakan: tetap feature-flagged,
  opt-in, fallback Gentle, dan kejelasan ke user.
- **Risiko:** tinggi (kebijakan Play, reliabilitas, baterai).

### P6-2 — Cloud Sync
- **Deskripsi:** sinkronisasi task/settings antar perangkat.
- **Catatan:** memperkenalkan **backend** — bertentangan dengan "tanpa backend" MVP. Butuh
  keputusan PRD baru + desain E2E encryption (server tak boleh baca data).
- **Risiko:** tinggi (privasi, biaya, kompleksitas).

### P6-3 — Backup / Restore Lokal
- **Deskripsi:** ekspor-impor file backup (Room → file terenkripsi) tanpa cloud.
- **Catatan:** paling selaras dengan prinsip local-first; kandidat pertama yang wajar.
- **Risiko:** rendah.

### P6-4 — Ekspor Data
- **Deskripsi:** ekspor task/history ke JSON/CSV untuk user (portabilitas data, PRD §13).
- **Catatan:** sejalan dengan hak user atas data; perlu format stabil + versi.
- **Risiko:** rendah.

### P6-5 — Akun Opsional
- **Deskripsi:** login opsional untuk fitur sync/premium.
- **Catatan:** hanya bermakna jika P6-2 dikerjakan. Default tetap tanpa akun.
- **Risiko:** tinggi.

### P6-6 — Widget / Tile
- **Deskripsi:** widget home-screen (progres hari ini) & Quick Settings tile (mulai focus).
- **Catatan:** murni UX, tanpa data baru; risiko rendah.
- **Risiko:** rendah.

### P6-7 — Wear OS
- **Deskripsi:** companion jam untuk timer fokus.
- **Catatan:** pasar niche; kerja tambahan modul.
- **Risiko:** sedang.

### P6-8 — Insight AI On-Device
- **Deskripsi:** menjalankan model kecil on-device → nol egress.
- **Catatan:** menghapus kebutuhan jaringan untuk AI; namun menambah ukuran APK & RAM
  (bertabrakan dengan target < 150 MB). Perlu evaluasi.
- **Status (v0.3):** **sebagian dipromosikan** — AI nyata kini memakai **Gemini REST
  opt-in** (cloud, agregat saja), bukan on-device. Ide on-device ini tetap backlog bila
  ingin menghapus egress sepenuhnya.
- **Risiko:** sedang.

### P6-9 — Lokalisasi Tambahan
- **Deskripsi:** menambah locale (sudah ada English + Bahasa Indonesia).
- **Catatan:** seam sudah siap (`generateLocaleConfig`, `values-*/strings.xml`).
- **Risiko:** rendah.

---

## 4. Cara Promosi Ide ke MVP

1. **Ubah `01-prd.md` dulu** (locked) — tambahkan FR/AC untuk fitur.
2. Perbarui turunan: `05-mvp-scope.md`, `12-sprint-backlog.md`, `08-`/`09-` bila menyentuh
   izin/privasi, `11-` untuk strategi uji.
3. Perbarui `10-risk-register.md` (risiko baru) & `README.md` (traceability).
4. Baru masuk antrean sprint.

> Tanpa langkah 1, item **tidak** dikerjakan — sesuai hierarki otoritas dokumen.

---

## 5. Referensi Silang

- Ruang lingkup MVP & yang di luar scope → `05-mvp-scope.md` §3, §7
- Backlog MVP (Sprint 0–8) → `12-sprint-backlog.md`
- Kebijakan privasi & egress → `09-privacy-and-security.md`
- Izin & degradasi → `08-permissions.md`
- Risiko scope creep (R34) → `10-risk-register.md`
- Keputusan Spike B → `13-spike-b-accessibility.md`

---

> **CATATAN AKHIR:** Dokumen ini adalah **peta ide**, bukan janji fitur. MVP (Phase 1–5)
> tetap menjadi fokus sampai dinyatakan rilis. Jangan biarkan backlog ini menggeser
> prioritas MVP (R34).
