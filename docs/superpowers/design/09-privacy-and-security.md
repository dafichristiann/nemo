# Privacy & Security — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 §10.2, §13 (Locked) · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` §10.2, §10.8, §13, §14; `03-database-schema.md`; `08-permissions.md`
> **Catatan:** PRD §13 hanya memberi diagram ringkas. Dokumen ini adalah **detail** yang
> dijanjikan ("detail di dokumen terpisah"). Tidak menambah aturan perilaku baru.

---

## 0. Prinsip Privasi (non-negotiable)

1. **Local-first.** Semua data tinggal di device. Tidak ada backend di MVP.
2. **Raw usage tidak pernah meninggalkan device.**
3. **AI hanya menerima agregat**, dan **hanya jika di-opt-in**.
4. **Privacy by default.** Tracking & AI **OFF** sampai user menyalakan (PRD FR-S6).
5. **Data source of truth = Android API**, bukan AI (FR-W7).
6. **User punya kontrol penuh** — bisa lihat, atur retensi, dan hapus semua data.

---

## 1. Klasifikasi Data

| Kategori | Contoh | Lokasi | Boleh keluar device? |
|---|---|---|---|
| **Raw usage** | `UsageSession` (app, start, end, durasi) | Room (device) | ❌ Tidak pernah |
| **Agregat** | `DailyUsageSummary`, `WeeklyUsageSummary` | Room (device) | ✅ Hanya jika AI opt-in |
| **Task** | Task, subtask, deadline | Room (device) | ❌ (kecuali user ekspor) |
| **Intervensi** | `InterventionLog` | Room (device) | ❌ |
| **Insight** | Hasil AI/rule | Room (device) | ❌ |
| **Settings** | Preferensi, opt-in | Room/DataStore | ❌ |

**Alur data (PRD §13):**
```
Raw Usage Data (device-only)
      ▼ Local Aggregation
Daily/Weekly Summary
      ▼ [Pruned 14–30 hari] (raw dihapus)
      ▼ [Aggregated, structured] ──► AI (opsional, opt-in)
```

---

## 2. Prinsip "No Raw Leaves the Device"

**Aturan keras:** sebelum mengirim ke AI, lakukan audit payload:

```kotlin
fun assertAggregatedOnly(payload: InsightPayload) {
    require(payload is AggregatedPayload) { "Raw usage must never be sent" }
    require(payload.sessions.isEmpty())   { "Session-level data is not allowed" }
    require(payload.perAppMs.size <= MAX_AGG_APPS) { "Only top-N aggregated apps" }
}
```

- Payload **hanya** berisi: total screen time, top-N app **durasi agregat**, kategori,
  jumlah sesi — **bukan** timestamp per sesi.
- Ada unit test yang **menolak** payload berisi raw (PRD §10.8 acceptance).
- Jika AI dinonaktifkan/offline → **rule-based** lokal, tanpa kirim apa pun.

---

## 3. Retention & Pruning

| Data | Kebijakan | Mekanisme |
|---|---|---|
| `UsageSession` (raw) | Hapus jika usia > **retensi** (default 30 hari, rentang 14–30) | `PruningWorker` |
| `DailyUsageSummary` | Permanen (kecil) | – |
| `WeeklyUsageSummary` | Permanen | – |
| `intervention_log` | Permanen (boleh dibatasi ~1 tahun) | Opsional |
| `insight` | Permanen / dibatasi | – |

- Retensi **configurable** di Settings (14–30 hari) — PRD FR-S5.
- ⚠️ **Mockup menulis "90 hari"** — ini **tidak sesuai** PRD. Ikuti PRD (14–30 hari),
  perbarui teks mockup (lihat `07-screens.md` §7).
- Hapus raw **setelah** agregasi berhasil (jangan hapus sebelum diagregasi).

---

## 4. Kontrol Data User (Settings → DATA CONTROLS)

| Kontrol | Perilaku |
|---|---|
| **Tracking on/off** | Matikan → hentikan koleksi & deteksi; data lama tetap sampai dihapus |
| **AI on/off** | Matikan → tidak ada permintaan jaringan; insight rule-based lokal |
| **Retention (14–30 hari)** | Mengatur `usageRetentionDays` → memengaruhi `PruningWorker` |
| **Hapus Semua Data** | Hapus seluruh tabel user (kecuali yang user pilih dipertahankan) |

**Hapus semua data (PRD §10.10):**
```
Given user menghapus semua data
When  dikonfirmasi
Then  semua data lokal terhapus (kecuali yang user pilih dipertahankan)
```
- Konfirmasi jelas + **tidak** destruktif tanpa konfirmasi.
- Setelah hapus → reset state Wellbeing/Focus/Insight, tampilkan empty state.

---

## 5. Keamanan Data Lokal

| Aspek | Keputusan MVP | Catatan |
|---|---|---|
| Enkripsi at-rest | Room + **(opsional)** SQLCipher | Untuk app personal, OS sandbox + full-disk encryption umumnya cukup; SQLCipher opsional bila ingin ekstra |
| Backup | **Matikan** `android:allowBackup="false"` | Cegah data keluar via auto-backup |
| Screenshots | Biarkan default; opsional `FLAG_SECURE` di layar sensitif | Personal app → opsional |
| Log | **Jangan** log data usage/task ke logcat di rilis | Bersihkan log debug |
| Clipboard | Jangan salin data sensitif | – |
| File eksternal | Tidak menulis data user ke storage eksternal | – |

**Prinsip:** karena single-user & local, fokus pada **tidak membocorkan** (no network,
no backup, no logs), bukan enkripsi berat.

---

## 6. Permintaan Jaringan (AI) — Satu-satunya Egress

Egress **hanya** terjadi jika:
1. User menyalakan **AI Privacy & AI** (opt-in), DAN
2. Device **online**, DAN
3. Ada **payload agregat** yang valid.

**Kontrol:**
- Cache + batch (maks 1×/hari + on-demand) — FR-AI3.
- API key: **tidak** di-commit; simpan lokal (jika user memasukkan sendiri).
- Respons AI divalidasi terhadap schema; jika tidak valid → **tolak & fallback**.
- Timeout/rate-limit/offline → fallback rule-based, tanpa error ke user.

**Yang tidak boleh dikirim:**
- ❌ Timestamp per sesi / per app
- ❌ Judul/isi task pribadi (kecuali user eksplisit mau)
- ❌ Raw `UsageSession`
- ❌ Identitas device/user

---

## 7. Audit & Verifikasi Privasi (checklist)

- [ ] `android:allowBackup="false"` diset.
- [ ] Tidak ada endpoint jaringan selain AI (dan hanya saat opt-in).
- [ ] Unit test menolak payload berisi raw session.
- [ ] `PruningWorker` teruji: raw > retensi terhapus; agregat tetap.
- [ ] "Hapus Semua Data" menghapus seluruh tabel user + reset UI.
- [ ] Tidak ada log data usage/task di build rilis.
- [ ] Default: `trackingEnabled=false`, `aiEnabled=false`.
- [ ] UI Settings menampilkan status & penjelasan jujur.

---

## 8. Kepatuhan & Nada

- Tidak ada klaim medis/kesehatan mental (PRD §6 Non-Goals).
- Tidak ada klaim AI tahu masa depan (Future View = proyeksi, §10.6).
- Tidak menyimpan data lebih lama dari yang diperlukan.
- Menghormati autonomy: tidak ada blocking yang tidak bisa di-exit.

> **Catatan:** karena aplikasi ini **personal & single-user** (bukan rilis publik),
> kewajiban Play Store Data Safety bersifat opsional — namun kebersihan privasi tetap
> dijaga sebagai prinsip, bukan karena kewajiban.

---

## 9. Referensi Silang

- Model data & retention → `03-database-schema.md` §10
- Izin & degradasi → `08-permissions.md`
- Aturan AI → `01-prd.md` §10.8
- Data & Privacy Model (ringkas) → `01-prd.md` §13
