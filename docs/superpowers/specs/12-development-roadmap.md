# Development Roadmap — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Prinsip:** Incremental. Satu stage selesai → test → lanjut. Tidak rewrite tanpa alasan.
> **Definisi DoD utama:** Phase 1–5 (produk) berjalan baik di device nyata.
> **Dokumen terkait:** `01-prd.md`, `10-testing-strategy.md`, `12-ready-for-implementation.md`

---

## Peta Besar

```
Stage 0  Environment Setup
Stage 1  Spike A — UsageStats
Stage 2  Spike B — Accessibility
Stage 3  Project Foundation
Stage 4  Smart Todo            (Product Phase 1)
Stage 5  Wellbeing            (Product Phase 2)
Stage 6  Intervention          (Product Phase 3a)
Stage 7  Focus                 (Product Phase 3b)
Stage 8  Future View           (Product Phase 4a)
Stage 9  Domino Effect         (Product Phase 4b)
Stage 10 AI Insight            (Product Phase 5)
Stage 11 Polish + Performance
```

> Spike (1–2) dijalankan **sebelum** mengunci desain Phase 2–3. Hasilnya menentukan
> seberapa ambisius deteksi & blocking.

---

## Stage 0 — Environment Setup

- **Objective:** Android Studio siap, device terhubung, Hello World jalan.
- **Deliverable:** lihat `11-development-environment-setup.md`.
- **Acceptance:**
  - [ ] `adb devices` menampilkan device
  - [ ] Project Compose sync tanpa error
  - [ ] Run ▶ tampil di HP
- **Dependencies:** tidak ada.
- **Risks:** OEM USB driver.
- **DoD:** Hello World tampil di device fisik.

---

## Stage 1 — Spike A: UsageStats

- **Objective:** Mengetahui akurasi & keterbatasan UsageStats di device target.
- **Deliverable:** skrip/app mini + laporan temuan (bukan kode produksi).
- **Steps:**
  1. App minimal dengan izin `PACKAGE_USAGE_STATS`.
  2. Query `queryUsageStats` + `queryEvents` (24 jam, 7 hari).
  3. Uji lintas: screen off, reboot, ganti hari.
  4. Bandingkan dengan penggunaan nyata terkontrol.
- **Acceptance:** laporan berisi akurasi durasi/sesi, delay, catatan per-OEM,
  kesimpulan: **layak / terbatas / tidak layak.**
- **Risks:** data ter-sampling; granularitas buruk.
- **Decision output:** menentukan desain Phase 2 (baseline penuh vs deteksi kasar).
- **DoD:** keputusan tertulis + direkam ke dokumen spike.

---

## Stage 2 — Spike B: Accessibility

- **Objective:** Menguji stabilitas & biaya baterai Accessibility Service.
- **Steps:**
  1. Service minimal log `TYPE_WINDOW_STATE_CHANGED`.
  2. Jalankan 24–48 jam; catat hidup/mati, drain, reboot, screen off/on.
  3. Uji emergency exit.
- **Acceptance:** laporan stability & battery + rekomendasi fallback.
- **Risks:** service sering mati; drain tinggi.
- **Decision output:** apakah blocking eksperimental layak dipakai di Phase 3,
  atau digeser ke Phase 6 (notification-only).
- **DoD:** keputusan tertulis.

---

## Stage 3 — Project Foundation

- **Objective:** Kerangka project sesuai arsitektur.
- **Scope:**
  - Struktur paket `core/ domain/ feature/`
  - Theme & Design System (tokens, Material 3)
  - Room DB versi 1 + entitas awal
  - DataStore settings
  - Navigation Compose (5 tab)
  - Manual DI container
  - `.gitignore`, CI-less build
- **Deliverable:** app kosong ber-navigasi dengan tema, tanpa fitur.
- **Acceptance:** navigasi jalan; tema light/dark; build bersih.
- **Tests:** build test, satu UI smoke test.
- **Risks:** over-engineering awal → jaga minimal.
- **DoD:** fondasi bisa di-run di device.

---

## Stage 4 — Smart Todo (Product Phase 1)

- **Objective:** Task management lengkap.
- **Files/Modules:** `feature/tasks`, `feature/home`, `domain/task`, `core/database` (task DAO).
- **Scope:** CRUD task, subtask, priority, deadline, estimasi, kategori, status,
  filter views, overdue (halus), reschedule, model dependency & recurring (dormant).
- **Acceptance:** semua acceptance criteria §10.1 (feature spec) lulus.
- **Tests:** unit (progress, mapping), Room DAO, ViewModel, Compose, integrasi.
- **Risks:** scope creep (recurring) → jaga dormant.
- **DoD:** user bisa merencanakan hari & menyelesaikan task dari device.

---

## Stage 5 — Wellbeing (Product Phase 2)

- **Objective:** Data usage lokal & tampilan ringkas.
- **Files/Modules:** `core/usage`, `feature/wellbeing`, Room (usage tables), Worker agregasi.
- **Scope:** izin, query, agregasi, snapshot/session, baseline, screen time, top apps, tren.
- **Acceptance:** §10.2 lulus; degradasi tanpa izin benar.
- **Tests:** aggregator unit, Room, permission, device.
- **Risks:** granularitas (hasil Spike A).
- **DoD:** Wellbeing menampilkan data nyata & jujur di device.

---

## Stage 6 — Intervention (Product Phase 3a)

- **Objective:** Deteksi drift + intervensi kontekstual.
- **Files/Modules:** `domain/drift`, `feature/intervention`, `core/notification`, Room (interventions).
- **Scope:** baseline & drift, evaluasi level, rate limit 3/hari, cooldown, channel, log.
- **Acceptance:** §10.3 & §10.4 lulus.
- **Tests:** drift unit, intervention rules, notification fallback.
- **Risks:** spam → rate limit & cooldown; trigger berisik.
- **DoD:** intervensi muncul pada konteks tepat & menghormati "Later".

---

## Stage 7 — Focus (Product Phase 3b)

- **Objective:** Sesi fokus + mode + blocking eksperimental.
- **Files/Modules:** `feature/focus`, `core/service` (foreground), `core/blocking`, Room (focus_sessions).
- **Scope:** setup, active focus, pause/finish, emergency exit, Gentle/Strict/Custom, fallback.
- **Acceptance:** §10.5 lulus.
- **Tests:** focus session, blocking fallback, device (foreground service).
- **Risks:** foreground service restrictions; accessibility rapuh (hasil Spike B).
- **DoD:** sesi fokus jalan & tercatat; emergency exit selalu tersedia.

---

## Stage 8 — Future View (Product Phase 4a)

- **Objective:** Konsekuensi waktu rule-based.
- **Files/Modules:** `domain/future`, `feature/future`.
- **Scope:** dua skenario, disclaimer, aksi Start Focus, penanganan data kurang.
- **Acceptance:** §10.6 lulus.
- **Tests:** calculator unit, Compose.
- **Risks:** mengesankan "prediksi" → disclaimer wajib.
- **DoD:** user melihat dua skenario dengan bahasa jujur.

---

## Stage 9 — Domino Effect (Product Phase 4b)

- **Objective:** Dampak dependency.
- **Files/Modules:** `domain/dependency`, `feature/future` (Domino).
- **Scope:** dependency CRUD, rantai, hitung dampak, bahasa estimatif.
- **Acceptance:** §10.7 lulus.
- **Tests:** impact calc, siklus dicegah, Room.
- **Risks:** siklus dependency.
- **DoD:** rantai tampil & benar.

---

## Stage 10 — AI Insight (Product Phase 5)

- **Objective:** Insight AI + rule-based fallback + daily reflection.
- **Files/Modules:** `core/ai`, `feature/insights`, Worker harian, Room (insights).
- **Scope:** payload agregat, Gemini opt-in, cache/batch, fallback, daily reflection.
- **Acceptance:** §10.8 & §10.9 lulus.
- **Tests:** rule-based unit, schema validation, fallback, offline.
- **Risks:** biaya/kuota; payload bocor raw → guard.
- **DoD:** insight tampil (AI atau local) dengan label sumber jujur.

---

## Stage 11 — Polish + Performance

- **Objective:** Menyelesaikan MVP (Phase 1–5) dengan kualitas.
- **Scope:** performance tuning (recomposition, list, animasi), battery check,
  empty/error/offline states, accessibility (kontras, touch target), copy review,
  dokumentasi akhir, cleanup.
- **Acceptance:** DoD utama tercapai — Phase 1–5 baik di device nyata.
- **Tests:** device matrix, battery, offline, jank check.
- **Risks:** regresi dari perubahan → jaga test.
- **DoD:** **READY** untuk pemakaian harian.

---

## Ketergantungan Antar-Stage

```
0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11
                    │         ▲
                    └─────────┘ (6&7 butuh hasil 1&2)
                               (8&9 butuh 4)
                               (10 butuh 4,5,6,7)
```

---

## Aturan Eksekusi (Agent Development Rules)

1. Jangan rewrite project tanpa alasan.
2. Jangan ubah arsitektur tanpa approval.
3. Jangan hapus fitur yang sudah jalan.
4. Implementasi incremental.
5. Satu fitur selesai → test.
6. Jangan tambah library hanya karena "lebih mudah".
7. Prioritas solusi Android-native.
8. Prioritas performa & baterai.
9. Background behavior lifecycle-aware.
10. Permission selalu graceful.
11. Jangan kirim raw usage ke cloud.
12. AI bukan dependency wajib.
13. Blocking bukan core dependency.
14. UI tidak ramai.
15. Gamification hanya dengan alasan produk.
16. Jangan pakai fake data bila API nyata tersedia.
17. Jangan anggap eksperimen teknis berhasil sebelum diuji di device nyata.

---

## Definition of Done per Product Phase (ringkas)

| Phase | DoD |
|---|---|
| 1 Smart Todo | Task lengkap usable di device |
| 2 Wellbeing | Data usage nyata tampil, degradasi benar |
| 3 Intervention+Focus | Intervensi tepat & sesi fokus jalan |
| 4 Future+Domino | Konsekuensi & rantai tampil jujur |
| 5 AI Insight | Insight (AI/local) dengan fallback |
| 6 (post-MVP) | Advanced blocking + sync (tidak sekarang) |
