# Permissions Model — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 §14 (Locked) · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` §10.2, §10.4, §10.5, §14; `02-architecture.md` §6
> **Catatan:** PRD §14 hanya memberi tabel ringkas. Dokumen ini adalah **detail teknis**
> yang dijanjikan ("detail di dokumen terpisah"). Tidak menambah aturan perilaku baru.

---

## 0. Prinsip Izin

1. **First launch tidak memaksa apa pun.** Smart Todo jalan tanpa izin apa pun.
2. **Semua izin opsional**, diminta **on-demand** saat user membuka fitur terkait.
3. **Setiap penolakan punya fallback** — app tidak pernah crash karena izin.
4. **Grace setiap saat** — izin bisa dicabut kapan saja; app mendeteksi & berdegradasi.
5. **Jelaskan kenapa** sebelum meminta (bukan pop-up sistem langsung).

---

## 1. Ringkasan Izin

| Izin | Tipe | Kapan diminta | Jika ditolak |
|---|---|---|---|
| `PACKAGE_USAGE_STATS` | Special (Settings manual) | Saat user buka Wellbeing | App tetap jadi Smart Todo |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Sebelum intervensi pertama / saat onboarding izin | Fallback in-app |
| Accessibility Service | Special (Settings manual) | Saat user pilih mode Strict/Custom | Fallback ke Gentle |
| Foreground Service | Manifest + runtime (API 34+ tipe) | Hanya saat Focus aktif | Focus terbatas |
| Alarm permissions (`SCHEDULE_EXACT_ALARM`) | Special (API 31+) | Jika reminder kritis dipakai | Reminder fleksibel (WorkManager) |
| `QUERY_ALL_PACKAGES` (opsional) | Manifest | **Hindari**; pakai query bertarget | Kategori app manual |

> Detail ini **tidak** mengubah PRD §14 — hanya memperjelas tipe & fallback per API level.

---

## 2. `PACKAGE_USAGE_STATS` (Usage Access)

**Peran:** sumber data Wellbeing & basis deteksi drift (PRD §10.2, §10.3).

**Sifat:** **Special permission** — tidak bisa via dialog runtime; user harus mengaktifkan
manual di *Settings → Special app access → Usage access*.

**Cara mendeteksi status:**
```kotlin
fun hasUsageAccess(ctx: Context): Boolean {
    val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= 29)
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
    else
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}
```

**Alur permintaan (UX):**
1. User buka Wellbeing (atau onboarding 00b) → tampilkan kartu penjelasan + tombol.
2. Tombol → `Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)`.
3. Saat kembali (`onResume`) → cek ulang status → update UI.

**Fallback bila ditolak:**
- Wellbeing menampilkan **empty state jelas** + tombol ke Settings, **tidak crash**.
- Drift detector **tidak berjalan** (tanpa data) — tidak mengklaim apa pun (FR-D6).
- Task tetap berfungsi penuh.

**Keterbatasan OEM (WAJIB didokumentasikan di UI bila relevan):**
- Beberapa OEM membatasi granularitas atau tidak menyediakan sesi detail.
- Data ter-agregasi periodik; **tidak ada event real-time**.
- Bila data kosong > X hari → jangan tampilkan baseline menyesatkan.

---

## 3. `POST_NOTIFICATIONS`

**Peran:** channel utama intervensi (PRD §10.4) + notifikasi Focus persisten.

**Sifat:** runtime permission (Android 13+/API 33). API < 33 granted by default.

**Kapan diminta:** **sebelum intervensi pertama**, atau saat onboarding izin (00b).
Jangan spam — satu kali minta, hormati keputusan.

**Fallback bila ditolak:**
- Intervensi tampil **in-app saja** (banner/overlay) — PRD §10.4 Edge Cases.
- Focus tetap bisa berjalan, tapi tanpa notifikasi persisten (batasi fitur secara jujur).
- **Jangan** minta berulang setiap kali app dibuka.

---

## 4. Accessibility Service (Eksperimental)

**Peran:** sinyal drift **real-time** (Phase 3, PRD §10.3) & blocking mode Strict/Custom
(PRD §10.5). **Eksperimental & feature-flagged.**

**Sifat:** Special access (Settings) + deklarasi service di Manifest.

**Kapan diminta:** hanya saat user memilih mode **Strict/Custom** (atau mengaktifkan
deteksi real-time secara eksplisit). Jangan diminta di onboarding awal.

**Fallback bila tidak tersedia/dimatikan OS:**
- Mode Strict/Custom → **turun ke Gentle** + beri tahu user (FR-F8).
- Deteksi drift tetap jalan via Phase 2 (WorkManager).
- **Deteksi OS mematikan service** saat runtime → tangani & fallback.

> **Prinsip:** Accessibility **bukan fondasi**. Aplikasi tetap berguna tanpanya.

---

## 5. Foreground Service (Focus Aktif)

**Peran:** menjaga timer Focus & (opsional) blocking selama sesi (PRD §10.5).

**Sifat:** Foreground service **hanya saat Focus aktif**, dengan **notifikasi persisten**
yang jelas. Wajib `foregroundServiceType` sesuai (mis. `specialUse`/`shortService` bila
relevan).

**Aturan:**
- Mulai saat `StartFocusUseCase`, hentikan saat Finish/Pause sesuai kebutuhan.
- Selalu ada notifikasi persisten (transparan) + **emergency exit**.
- Battery-aware: jangan paksa saat baterai lemah (PRD §10.5 Edge Cases).

**Fallback bila gagal/dibatasi:**
- Focus tetap tercatat, tapi tanpa persistence (batas waktu terbatas).
- Beri tahu user secara jujur.

---

## 6. Alarm / Exact Reminder (Opsional)

**Peran:** reminder kritis (jika dipakai). **Bukan** fondasi.

**Sifat:** `SCHEDULE_EXACT_ALARM` (API 31+) bersifat special; akses bisa dicabut.

**Aturan:**
- **Default: jangan** pakai exact alarm. Gunakan **WorkManager** (fleksibel).
- Exact alarm hanya bila user menyalakan reminder waktu-presisi.
- Bila tidak tersedia → reminder fleksibel (bisa meleset beberapa menit) + beri tahu.

---

## 7. `QUERY_ALL_PACKAGES` — Hindari

Framework membatasi visibilitas paket (Android 11+). **Jangan** meminta
`QUERY_ALL_PACKAGES` untuk app personal ini. Gunakan query bertarget:

```xml
<queries>
  <intent><action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" /></intent>
</queries>
```

Jika daftar app untuk kategori tidak lengkap → sediakan **input manual** dari user.

---

## 8. Matriks Degradasi Fitur

| Fitur | Butuh | Tanpa izin → |
|---|---|---|
| Smart Todo | – | Berfungsi penuh |
| Wellbeing | Usage Access | Empty state + CTA Settings |
| Drift (Phase 2) | Usage Access | Tidak aktif (tidak klaim) |
| Drift real-time (Phase 3) | Accessibility | Fallback periodic |
| Intervensi | Notifications | In-app saja |
| Focus Gentle | Foreground Service | Focus terbatas |
| Focus Strict/Custom | Accessibility + FGS | Turun ke Gentle |
| Reminder presisi | Exact Alarm | Reminder fleksibel |
| AI Insight | Internet + opt-in | Rule-based lokal |

---

## 9. Checklist Implementasi

- [ ] Deteksi status tiap izin **tanpa crash** (semua API level target).
- [ ] Cek ulang status di `onResume` setelah user kembali dari Settings.
- [ ] Setiap fitur opsional punya `Availability` state (Available/Degraded/Unavailable).
- [ ] Tidak ada permintaan izin di first launch untuk fitur non-inti.
- [ ] Teks penjelasan izin via string resources + nada calm.
- [ ] Uji: izin ditolak, izin dicabut saat jalan, OEM membatasi.

---

## 10. Referensi Silang

- Model izin ringkas → `01-prd.md` §14
- Degradasi arsitektural → `02-architecture.md` §6
- Data & privasi → `09-privacy-and-security.md`
- Risiko terkait → `10-risk-register.md`
