# Spike B — Accessibility (kelayakan & baterai)

> **Status:** Keputusan spike (Sprint 4, item 4-07) · **Tanggal:** 2026-10-08
> **Terkait:** `08-permissions.md` §5, `04-intervention-engine.md` §2.5, `10-risk-register.md` (R3)
> **Kesimpulan:** Accessibility tetap **eksperimental & feature-flagged**; Strict/ Custom
> **selalu** punya fallback ke Gentle. Tidak ada blocking berbasis paksaan (PRD §4).

---

## 1. Tujuan Spike

Menjawab dua pertanyaan sebelum mengunci Phase 3 (Focus Strict/Custom):

1. Apakah `AccessibilityService` layak dipakai untuk **sinyal drift instan** (Phase 3) dan
   **pembatasan app** (Strict/Custom) di device target — tanpa melanggar prinsip produk?
2. Berapa biaya baterai/keandalan yang realistis, dan apa jalur degradasinya?

---

## 2. Temuan (ringkas)

| Aspek | Temuan | Implikasi |
|---|---|---|
| Kelayakan API | `AccessibilityService` butuh izin *Special* (Settings manual), mudah dicabut OEM | Hanya **opsional**; bukan fondasi |
| Event transisi app | `TYPE_WINDOW_STATE_CHANGED` cukup untuk sinyal instan | Boleh dipakai sebagai **peningkatan** Phase 2 |
| Baterai | Service persisten menambah konsumsi; OEM agresif mematikan | Harus lifecycle-aware; default OFF |
| Keandalan blocking | Tidak konsisten antar OEM; mudah di-bypass | **Tidak** boleh jadi syarat fitur |
| Kebijakan/Review | Penggunaan untuk blocking berisiko ditolak | Feature-flag + kejelasan ke user |

---

## 3. Keputusan

1. **Feature-flagged & opt-in.** Accessibility tetap di belakang flag; default nonaktif.
2. **Graceful fallback wajib.** Bila layanan tidak tersedia / dicabut → **fallback ke Gentle**
   (`fallbackReason` dicatat di `focus_session`), **tidak pernah** gagal keras (PRD §10.5, docs 05 §3).
3. **Phase 2 tetap berjalan tanpa Accessibility.** Deteksi drift periodik (WorkManager) adalah
   jalur utama; Accessibility hanya menambah **sinyal instan**, bukan menggantikan.
4. **Tidak ada mekanisme paksa.** "Emergency exit" selalu tersedia (FR §10.5).
5. **Seam teknis disiapkan** via `BlockingStrategy` (interface) agar Sprint 5 dapat memilih
   implementasi Gentle/Strict/Custom tanpa mengubah domain.

---

## 4. Dampak ke Sprint 5

- Implementasi Strict/Custom **wajib** menyalurkan kegagalan ke `BlockingStrategy` Gentle.
- Perlu uji: izin ditolak, layanan dimatikan OS, OEM membatasi → app tetap stabil.
- Parameter baterai dipantau pada Sprint 8 (§12 non-fungsional: ≤ 2–3%/24 jam).

---

## 5. Referensi

- Izin & fallback → `08-permissions.md` §1, §5
- Risiko R3 → `10-risk-register.md`
- Perilaku terkunci → `01-prd.md` §10.5 (Emergency exit, Gentle tidak memblokir)
