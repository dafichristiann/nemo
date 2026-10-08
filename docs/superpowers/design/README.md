# Anchor — Design & Product Documentation

> **Anchor (Personal Focus Assistant)** — asisten fokus personal untuk Android.
> Menghubungkan **niat** (to-do) dengan **perilaku nyata** (penggunaan perangkat):
> menyadari saat kamu menjauh dari tugas penting, lalu membantu kembali dengan lembut —
> **tanpa memaksa, tanpa menghakimi, tanpa mengirim datamu ke mana pun.**

Dokumentasi ini adalah *source of truth* untuk desain, arsitektur, dan MVP Anchor.

---

## 1. Mulai dari Mana?

| Jika kamu… | Baca |
|---|---|
| Baru pertama kali | `00-product-brief.md` |
| Mau tahu aturan perilaku produk | `01-prd.md` ⭐ *(dokumen terkunci / locked)* |
| Mau bangun arsitektur | `02-architecture.md` |
| Mau tahu skema database | `03-database-schema.md` |
| Mau paham mesin intervensi | `04-intervention-engine.md` |
| Mau tahu ruang lingkup MVP | `05-mvp-scope.md` |
| Mau membangun UI | `06-design-system.md` + `07-screens.md` |
| Mau implementasi izin | `08-permissions.md` |
| Mau implementasi privasi/keamanan | `09-privacy-and-security.md` |
| Mau tahu risiko & mitigasi | `10-risk-register.md` |
| Mau tahu cara menguji | `11-testing-strategy.md` |
| Mau mulai ngoding | `12-sprint-backlog.md` |
| Mau tahu hardening/rilis | `14-hardening-report.md` (dan `../RELEASE.md`) |
| Mau lihat rencana Phase 6 | `15-post-mvp-roadmap.md` |
| Mau lihat mockup | `gamabaran.pen` |

---

## 2. Daftar Dokumen

| # | Dokumen | Isi | Status |
|---|---|---|---|
| 00 | `00-product-brief.md` | Ringkasan *why*, masalah, solusi, ruang lingkup | Approved |
| 01 | `01-prd.md` | Product Requirements — aturan perilaku & acceptance criteria | **Locked v0.2** |
| 02 | `02-architecture.md` | Arsitektur teknis, modul, worker, degradasi | Derived |
| 03 | `03-database-schema.md` | Skema Room, relasi, migrasi, retention | Derived |
| 04 | `04-intervention-engine.md` | Drift detector + intervention engine | Derived |
| 05 | `05-mvp-scope.md` | Fase MVP, DoD, urutan kerja | Derived |
| 06 | `06-design-system.md` | Token visual & komponen | Derived |
| 07 | `07-screens.md` | Inventaris layar & alur | Derived |
| 08 | `08-permissions.md` | Detail izin, alur, fallback, degradasi | Derived (PRD §14) |
| 09 | `09-privacy-and-security.md` | Klasifikasi data, egress AI, retention, hapus data | Derived (PRD §13) |
| 10 | `10-risk-register.md` | Register risiko lengkap + mitigasi | Derived (PRD §15) |
| 11 | `11-testing-strategy.md` | Strategi QA + mapping AC→test + non-fungsional | Derived |
| 12 | `12-sprint-backlog.md` | Backlog sprint-ready Phase 1–5 | Derived |
| 13 | `13-spike-b-accessibility.md` | Keputusan Spike B (Accessibility) | Spike decision |
| 14 | `14-hardening-report.md` | Verifikasi Sprint 8 (hardening) | Report |
| 15 | `15-post-mvp-roadmap.md` | Roadmap Phase 6 (backlog ide, bukan komitmen) | Backlog |
| 16 | `16-ai-insight-gemini.md` | AI Insight aktif via Gemini (opt-in) | Spec |
| — | `gamabaran.pen` | Mockup desain (pen.dev) | Mockup |
| — | `build_pen.js` / `_cleanup.js` | Pembangun & pembersih file `.pen` | Tooling |

> **Hierarki otoritas:** `01-prd.md` adalah **dokumen terkunci**. Semua dokumen lain
> *mengalir dari* PRD. Jika terjadi konflik, **PRD menang**. Untuk mengubah perilaku
> produk, ubah PRD terlebih dahulu, baru turunannya.

> **Status kelengkapan:** PRD menjanjikan *"dokumen terpisah"* untuk Permission (§14),
> Privacy (§13), dan Risk Register (§15) — ketiganya kini tersedia (`08`, `09`, `10`).
> QA (`11`) dan backlog (`12`) melengkapi jalur dari dokumen ke eksekusi.

---

## 3. Sekilas Produk

```
    PLAN  ──►  OBSERVE  ──►  DETECT DRIFT  ──►  INTERVENE
  (Tasks)   (Wellbeing)      (baseline)        (kontekstual)
      │                                            │
      └────────►  FOCUS  ◄────────  FUTURE VIEW  ◄─┘
                   │              (konsekuensi)
                   ▼
              COMPLETE  ──►  LEARN  ──►  BESOK LEBIH BAIK
             (Tasks)      (Insights)      (Insights)
```

**Prinsip:** Awareness > Control · Guidance > Punishment · Autonomy > Forced Behavior ·
Personal Baseline > Universal Rules.

**Nada:** calm · supportive · concise · non-judgmental.

---

## 4. Ruang Lingkup MVP (Phase 1–5)

| Phase | Nama | Inti |
|---|---|---|
| 1 | Smart Todo | Task, recurring, subtask, overdue banner |
| 2 | Digital Wellbeing | Usage lokal, baseline, pruning |
| 3 | Intervention + Focus | Intervensi (max 3/hari), Focus Mode |
| 4 | Future View + Domino | Konsekuensi waktu, dependency |
| 5 | AI Personal Insight | Refleksi harian, pola (fallback lokal) |
| 6 | Advanced Blocking + Sync | **Post-MVP** |

Detail: `05-mvp-scope.md`.

---

## 5. Fakta Teknis Kunci

- **Android native** (Kotlin, Jetpack Compose), **local-first**, **tanpa backend**.
- **Room** = single source of truth; **WorkManager** untuk background.
- **Privacy by default** — tracking & AI *off* sampai opt-in; raw usage tak keluar device.
- Sumber kebenaran data usage = **Android API**, bukan AI.
- Target: **RAM < 150 MB**, **baterai ≤ 2–3%/24 jam**, **100% offline-capable**.

---

## 6. Struktur Folder

```
docs/superpowers/design/
├── 00-product-brief.md
├── 01-prd.md                  ⭐ (locked)
├── 02-architecture.md
├── 03-database-schema.md
├── 04-intervention-engine.md
├── 05-mvp-scope.md
├── 06-design-system.md
├── 07-screens.md
├── 08-permissions.md
├── 09-privacy-and-security.md
├── 10-risk-register.md
├── 11-testing-strategy.md
├── 12-sprint-backlog.md
├── 13-spike-b-accessibility.md
├── 14-hardening-report.md
├── 15-post-mvp-roadmap.md
├── README.md                  (ini)
├── gamabaran.pen              (mockup)
├── build_pen.js               (pembangun mockup)
└── _cleanup.js                (tooling mockup)
```

---

## 7. Traceability Matrix (PRD → Dokumen)

Setiap bagian penting PRD dipetakan ke dokumen turunan agar **tidak ada yang hilang**.

| PRD | Topik | Dokumen turunan |
|---|---|---|
| §4 | Product Principles | `06-design-system.md` §1, §9 |
| §9 | Core Loop | `README.md` §3, `02-architecture.md` §4 |
| §10.1 | Smart Todo | `03-` §2, `12-` Sprint 1 |
| §10.2 | Digital Wellbeing | `03-` §3, `08-`, `12-` Sprint 2–3 |
| §10.3 | Drift Detection | `04-` §2, `12-` Sprint 3 |
| §10.4 | Intervention Engine | `04-` §3–5, `12-` Sprint 4 |
| §10.5 | Focus Mode | `03-` §4, `08-` §5, `12-` Sprint 5 |
| §10.6 | Future View | `12-` Sprint 6 |
| §10.7 | Domino Effect | `03-` §2.5, `12-` Sprint 6 |
| §10.8 | AI Insight | `09-` §6, `12-` Sprint 7 |
| §10.9 | Daily Reflection | `07-` §2.12, `12-` Sprint 7 |
| §10.10 | Settings/Privacy | `03-` §7, `09-` §4 |
| §11 | MVP Scope per Phase | `05-mvp-scope.md`, `12-sprint-backlog.md` |
| §12 | Non-Functional | `11-testing-strategy.md` §3 |
| §13 | Data & Privacy Model | `09-privacy-and-security.md` |
| §14 | Permission Model | `08-permissions.md` |
| §15 | Risiko Teknis | `10-risk-register.md` |
| §16 | Locked Decisions | semua dokumen turunan |
| §18 | Roadmap | `05-mvp-scope.md` §2 |

---

## 8. Konvensi Dokumen

- Ditulis dalam **Bahasa Indonesia** dengan istilah teknis Inggris (konsisten).
- Setiap dokumen menyertakan **status**, **versi**, dan **referensi silang**.
- Aturan yang **dikunci** (locked) ditandai eksplisit; sisanya = *derived* (turunan).
- Tanpa emoji di dalam file (kecuali penanda status sederhana).
- ⚠️ Bila `gamabaran.pen` diedit, ulangi audit di `06-design-system.md` §12 dan
  `07-screens.md` §7.

---

> **CATATAN:** Seluruh dokumentasi ini diturunkan dari `01-prd.md` v0.2 yang telah
> Approved/Locked. Jangan ubah derivatif tanpa memastikan konsistensi dengan PRD.
