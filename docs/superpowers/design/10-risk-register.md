# Risk Register — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 §15 (Locked) · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` §15; `02-architecture.md` §9; `08-permissions.md`
> **Catatan:** PRD §15 hanya tabel ringkas. Ini adalah **register lengkap** yang dijanjikan.
> Skala: **Level** (Tinggi/Sedang/Rendah) × **Likelihood** (T/S/R).

---

## 0. Cara Membaca

- **Level**: dampak bila terjadi (Tinggi = bisa menggagalkan fitur inti).
- **Likelihood**: peluang terjadi di device target.
- **Owner**: siapa yang menangani (Dev / Tester / Keduanya).
- **Status**: Open / Mitigated / Accepted / Watching.

**Prioritas kerja:** `Level × Likelihood` (Tinggi+Tinggi = tangani lebih dulu).

---

## 1. Risiko Platform (Android)

| # | Risk | Level | Likelihood | Mitigasi | Owner | Status |
|---|---|---|---|---|---|---|
| R1 | **Background execution limits (Doze/Standby)** menghambat koleksi & drift check | Tinggi | Tinggi | WorkManager; hindari polling; hormati Doze; constraint tepat | Dev | Open |
| R2 | **UsageStats granularitas terbatas** (OEM membatasi / tanpa sesi detail) | Tinggi | Tinggi | Spike A; `Availability` state; degradasi bertahap; tanpa klaim drift bila data kurang | Dev+Tester | Open |
| R3 | **Accessibility rapuh & boros baterai** | Tinggi | Sedang | Eksperimental, feature-flag, fallback ke notifikasi/Gentle | Dev | Open |
| R4 | **Notification permission ditolak** | Sedang | Tinggi | Fallback in-app only; jangan spam minta | Dev | Mitigated (by design) |
| R5 | **Exact alarm restrictions** (API 31+) | Sedang | Sedang | Default pakai WorkManager; exact alarm opsional | Dev | Mitigated |
| R6 | **OEM killing background** (Xiaomi/Huawei/Oppo) | Tinggi | Sedang–Tinggi | Panduan battery optimization; uji device nyata; jangan andalkan background untuk UX kritis | Dev | Open |
| R7 | **Foreground Service type & pembatasan** | Sedang | Sedang | Gunakan type tepat; notifikasi persisten jelas; hentikan saat tak perlu | Dev | Open |
| R8 | **API level differences** (13/31/33/34) | Sedang | Tinggi | Guard per SDK level; uji tiap versi target | Dev+Tester | Open |

---

## 2. Risiko Data & Performa

| # | Risk | Level | Likelihood | Mitigasi | Owner | Status |
|---|---|---|---|---|---|---|
| R9 | **Room migration gagal** → data hilang/crash | Tinggi | Sedang | Skema forward-compatible; `Migration` eksplisit; `MigrationTestHelper`; `exportSchema` | Dev | Open |
| R10 | **DB tumbuh besar** (raw usage) | Sedang | Sedang | Pruning 14–30 hari; agregasi berjenjang; indeks tepat | Dev | Mitigated |
| R11 | **Baterai > target (2–3%/24 jam)** | Sedang | Sedang | Batching; hindari polling; ukur & profiling | Dev+Tester | Open |
| R12 | **RAM > 150 MB** | Sedang | Sedang | Hindari animasi berat; stable keys; profiling di device low-RAM | Dev | Open |
| R13 | **Query usage lambat** saat data banyak | Sedang | Rendah | Indeks `startTime`/`packageName`; agregat untuk query berat | Dev | Mitigated |
| R14 | **Timezone / batas hari berubah** | Sedang | Rendah | Hitung ulang berdasar hari lokal; uji DST | Dev | Open |

---

## 3. Risiko Fitur & Logika

| # | Risk | Level | Likelihood | Mitigasi | Owner | Status |
|---|---|---|---|---|---|---|
| R15 | **Baseline kurang data** → klaim drift menyesatkan | Sedang | Sedang | `MIN_SAMPLE_DAYS`; jangan klaim drift bila data kurang (FR-D6) | Dev | Mitigated (by design) |
| R16 | **Spam intervensi** | Tinggi | Sedang | Kuota max 3/hari; cooldown 2 jam; pemisahan Detection vs Attempt | Dev | Mitigated (by design) |
| R17 | **Recurring task** menerbitkan instance ganda/salah | Sedang | Sedang | Pemisahan Definition/Instance; idempotent generator; uji DST/absen | Dev | Open |
| R18 | **Dependency siklus** → infinite loop / domino aneh | Sedang | Rendah | Deteksi siklus sebelum simpan; tolak + jelaskan | Dev | Mitigated (by design) |
| R19 | **Future View mengarang** (data kurang) | Sedang | Sedang | Heuristic default + tandai asumsi; jangan sajikan seolah pasti | Dev | Mitigated (by design) |
| R20 | **Overdue otomatis dipindah** (melanggar autonomy) | Tinggi | Rendah | Non-blocking banner; tidak auto-move; user yang memilih | Dev | Mitigated (by design) |

---

## 4. Risiko AI (Opsional)

| # | Risk | Level | Likelihood | Mitigasi | Owner | Status |
|---|---|---|---|---|---|---|
| R21 | **AI cost / rate-limit** | Sedang | Sedang | Cache + batch (maks 1×/hari + on-demand); opt-in | Dev | Open |
| R22 | **AI offline/gagal** | Sedang | Sedang | Fallback rule-based lokal; tidak error ke user | Dev | Mitigated (by design) |
| R23 | **AI respons di luar schema** | Sedang | Sedang | Validasi schema; tolak & fallback | Dev | Open |
| R24 | **Raw usage bocor ke AI** | Tinggi | Rendah | Audit payload agregat-only; unit test penolakan raw | Dev | Mitigated (by design) |
| R25 | **AI klaim di luar data** | Sedang | Sedang | Prompt terbatas; hanya insight didukung data; sembunyikan bila data kurang | Dev | Open |

---

## 5. Risiko Privasi & Keamanan

| # | Risk | Level | Likelihood | Mitigasi | Owner | Status |
|---|---|---|---|---|---|---|
| R26 | **Data usage bocor keluar** | Tinggi | Rendah | Local-only; `allowBackup=false`; no logs; AI opt-in agregat | Dev | Mitigated (by design) |
| R27 | **Auto-backup membawa data** | Sedang | Sedang | Matikan `allowBackup` / exclude DB | Dev | Mitigated |
| R28 | **Data sensitif di logcat** | Sedang | Sedang | Bersihkan log; jangan log usage/task di rilis | Dev | Open |
| R29 | **Izinkan pencabutan saat runtime** | Sedang | Sedang | Deteksi di `onResume`/service; fallback graceful | Dev | Open |

---

## 6. Risiko UX / Produk

| # | Risk | Level | Likelihood | Mitigasi | Owner | Status |
|---|---|---|---|---|---|---|
| R30 | **Terasa menghakimi** (melanggar nada) | Tinggi | Rendah | Audit copy; larangan merah menyala/kata menyalahkan | Dev+Review | Watching |
| R31 | **Onboarding memaksa izin** | Sedang | Rendah | First launch tanpa izin; opsional | Dev | Mitigated (by design) |
| R32 | **Fitur jadi tidak berguna tanpa izin** | Sedang | Sedang | Smart Todo selalu jalan; degradasi jujur | Dev | Mitigated (by design) |
| R33 | **Compose jank / animasi berat** | Sedang | Sedang | Motion minim; stable keys; hindari animasi berat | Dev | Open |

---

## 7. Risiko Proyek (Personal/Solo)

| # | Risk | Level | Likelihood | Mitigasi | Owner | Status |
|---|---|---|---|---|---|---|
| R34 | **Scope creep** (Phase 6 masuk MVP) | Sedang | Sedang | Ikuti `05-mvp-scope.md`; Phase 6 = post-MVP | Owner | Watching |
| R35 | **Tidak selesai** (proyek solo) | Sedang | Sedang | Sprint backlog kecil & vertikal; DoD per fase | Owner | Watching |
| R36 | **Regresi tanpa test** | Sedang | Sedang | `11-testing-strategy.md`; unit test domain wajib | Dev | Open |

---

## 8. Ringkasan Prioritas (Top 8)

Insiden yang paling perlu perhatian lebih dulu:

| Rank | Risk | Kenapa |
|---|---|---|
| 1 | R1 Doze/Standby | Menghambat inti Phase 2–3 |
| 2 | R2 UsageStats terbatas | Bisa mematikan Wellbeing/drift |
| 3 | R6 OEM killing | UX tidak andal di device tertentu |
| 4 | R3 Accessibility | Eksperimental tetapi inti blocking |
| 5 | R9 Migration | Bisa kehilangan data |
| 6 | R16 Spam intervensi | Melanggar prinsip produk |
| 7 | R24 Raw bocor ke AI | Melanggar privasi (keras) |
| 8 | R11/R12 Baterai & RAM | Target non-fungsional PRD §12 |

---

## 9. Risk → Dokumen Mitigasi

| Risk grup | Dokumen rujukan |
|---|---|
| Platform & background | `02-architecture.md` §5, §9 |
| Data & performa | `03-database-schema.md` §9, `11-testing-strategy.md` |
| Fitur & logika | `04-intervention-engine.md`, `01-prd.md` §10 |
| AI | `09-privacy-and-security.md` §6 |
| Privasi/keamanan | `09-privacy-and-security.md` |
| UX/produk | `06-design-system.md` §9, `07-screens.md` §7 |

---

## 10. Referensi Silang

- Register ringkas (sumber) → `01-prd.md` §15
- Arsitektur & mitigasi teknis → `02-architecture.md`
- Izin & degradasi → `08-permissions.md`
- Privasi → `09-privacy-and-security.md`
- QA → `11-testing-strategy.md`
