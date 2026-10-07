# Testing Strategy — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Prinsip:** Test setiap fitur utama. Prioritas pada unit & integrasi (murah, cepat),
> ditambah uji device nyata untuk perilaku Android yang tak bisa disimulasikan.
> **Dokumen terkait:** `03-feature-specification.md`, `10-development-roadmap.md`

---

## 1. Piramida Pengujian

```
        ▲  Real Device / Manual (behavior Android)
       ╱ ╲
      ╱   ╲   Instrumentation (Compose UI, Room, WorkManager)
     ╱─────╲
    ╱       ╲ Unit (usecase, drift, rule-based, aggregator)
   ╱─────────╲
```

Fokus: **banyak unit test**, sedang instrumentasi, secukupnya device nyata.

---

## 2. Jenis Pengujian

### 2.1 Unit Tests (paling banyak)
- **Target:** use case, domain logic, rule-based insight, drift calculation,
  future-view computation, domino impact, mapper.
- **Tools:** JUnit4/5, Kotlin test, `kotlinx-coroutines-test`, Turbine (Flow).
- **Contoh:**
  - `DriftDetectorTest` — baseline 18m, current 47m → drift signal true, tanpa kata diagnostik.
  - `DriftDetectorTest` — baseline kurang → no drift.
  - `RuleBasedInsightSourceTest` — output sesuai schema.
  - `FutureViewCalculatorTest` — dua skenario benar dari input.
  - `TaskProgressTest` — progress dari subtask benar.

### 2.2 Repository Tests
- **Target:** implementasi `TaskRepository`, `UsageRepository`, `InsightRepository`.
- Fake DAO / in-memory.
- Uji mapping entity ↔ domain, alur Flow.

### 2.3 Room Tests (instrumented / Robolectric)
- **Target:** DAO queries & migrations.
- `MigrationTestHelper` untuk tiap versi skema.
- Uji query: Tasks Today, baseline average, longest distraction, intervention count.

### 2.4 ViewModel Tests
- **Target:** `TaskListViewModel`, `FocusViewModel`, `InsightsViewModel`.
- Uji `UiState` hasil dari aksi & flow.
- Dispatcher rule (`Main` → test dispatcher).

### 2.5 Compose UI Tests
- **Target:** screen utama & komponen stateless.
- `createComposeRule`, tag untuk elemen penting.
- Uji: empty state, permission state, error state, aksi utama.

### 2.6 Integration Tests
- **Target:** alur end-to-end dalam app (tanpa device API eksternal).
- Contoh: buat task → muncul di Today → tandai selesai → pindah ke Completed.

### 2.7 Permission Tests
- Simulasi izin granted/denied.
- Verifikasi degradasi (Wellbeing tanpa izin → tampil panduan, tidak crash).

### 2.8 UsageStats Tests
- **Target:** `UsageStatsDataSource` & `UsageAggregator`.
- Pakai data sampel untuk normalisasi & batas hari.
- `[EXPERIMENT REQUIRED]` Akurasi nyata diuji manual (Spike A).

### 2.9 Accessibility Tests
- **Manual + instrumented terbatas.**
- Verifikasi service lifecycle, emergency exit.
- `[EXPERIMENT REQUIRED]` Stabilitas & baterai diuji di device (Spike B).

### 2.10 Background Tests
- **Target:** WorkManager workers (agregasi, cleanup, daily insight).
- `WorkManagerTestInitHelper` untuk trigger manual.
- Uji constraint (no network untuk lokal).

### 2.11 Battery Tests
- **Manual.** Profil `Battery Historian` atau `dumpsys batterystats`.
- Ukur: idle drain, drain saat fokus aktif, drain saat service accessibility jalan.

### 2.12 Offline Tests
- Matikan jaringan → verifikasi AI fallback, semua fitur lokal jalan.

### 2.13 Real Device Tests
- **Wajib** sebelum menandai fase selesai.
- Device target: RAM 3–4 GB, Android 9+ (dan versi targetSdk).

---

## 3. Test Matrix

| Area | Unit | Repo | Room | VM | Compose | Integrasi | Device |
|---|---|---|---|---|---|---|---|
| Tasks CRUD | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| Subtask/progress | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Filter views | ✅ | ✅ | ✅ | ✅ | — | — | — |
| Overdue/reschedule | ✅ | ✅ | — | ✅ | ✅ | — | ⚠️ |
| Usage aggregation | ✅ | — | ✅ | — | — | — | ✅ |
| Baseline | ✅ | ✅ | ✅ | — | — | — | ✅ |
| Drift detection | ✅ | — | — | — | — | ✅ | ✅ |
| Intervention rules | ✅ | ✅ | — | ✅ | ✅ | — | ✅ |
| Focus session | ✅ | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| Blocking fallback | ✅ | — | — | ✅ | — | — | ✅ |
| Future View calc | ✅ | — | — | ✅ | ✅ | — | — |
| Domino impact | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| AI/rule insight | ✅ | ✅ | — | ✅ | — | — | ⚠️ |
| Permissions/degradasi | — | — | — | ✅ | ✅ | ✅ | ✅ |
| Offline | — | — | — | ✅ | — | ✅ | ✅ |

Legend: ✅ otomatis/manual sesuai baris · ⚠️ manual device jika relevan · — tidak perlu.

---

## 4. Aturan Kualitas

- Use case & perhitungan (drift, future view, insight) **wajib** punya unit test.
- Setiap acceptance criteria di feature spec → minimal satu test terkait.
- Migration **wajib** diuji tiap menaikkan versi DB.
- Tidak ada test yang bergantung pada `Thread.sleep` (pakai test scheduler).
- Test harus deterministik (waktu & random di-inject).

---

## 5. Manual Test Checklist (per fase)

```
[ ] App launch tanpa crash (fresh install)
[ ] Buat task → muncul di Today
[ ] Task tanpa izin usage → app tetap normal
[ ] Wellbeing tanpa izin → panduan, no crash
[ ] Enable usage → data muncul (device nyata)
[ ] Sesi fokus Gentle lengkap
[ ] Sesi fokus Strict + emergency exit
[ ] Accessibility off → fallback Gentle
[ ] Offline → insight rule-based
[ ] Reboot device → background tetap jalan
[ ] Battery idle check
```

---

## 6. Definition of Done (testing)

Sebuah fitur dianggap selesai (dari sisi test) bila:
1. Unit test untuk logika inti lulus.
2. Test integrasi (bila relevan) lulus.
3. Manual check di device nyata (bila menyentuh API Android) lulus.
4. Tidak ada regresi pada fitur yang sudah ada.
