# Intervention Engine — Anchor (Personal Focus Assistant)

> **Status:** Derived from PRD v0.2 (Locked) · **Version:** 0.1
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` (§10.3, §10.4), `03-database-schema.md`
> **Catatan:** Semua **aturan** di sini dikunci PRD. Dokumen ini hanya menjelaskan
> **algoritma & keputusan teknis** untuk mewujudkannya. Jika konflik → **PRD menang**.

---

## 0. Tujuan Mesin

Anchor harus menyadari ketika perilaku user **menyimpang dari baseline personal-nya**
(drift) dan menyampaikan **intervensi kontekstual yang lembut** pada momen yang tepat,
tanpa spam, tanpa memaksa, tanpa menghakimi.

Dua sub-sistem:
1. **Drift Detector** — menghasilkan `DriftSignal` (PRD §10.3).
2. **Intervention Engine** — mengubah sinyal menjadi (atau menahan) intervensi (PRD §10.4).

> **Pemisahan krusial:** *Detection Event ≠ Intervention Attempt.* Deteksi yang ditahan
> (cooldown/kuota habis) **tidak** mengurangi kuota (PRD §10.4 aturan kuota #2).

---

## 1. Pipeline Level Tinggi

```
[UsageStatsManager] ──► Artifact Usage (periodic)
        │
        ▼
  Baseline Store (personal, per-app)   ◄── DailyUsageSummary / WeeklyUsageSummary
        │
        ▼
  ┌─────────────────┐
  │  Drift Detector │  (hitung deviasi vs baseline personal)
  └────────┬────────┘
           │ DriftSignal? (hanya jika deviasi > threshold DAN ada task penting tertunda)
           ▼
  ┌──────────────────────┐
  │  Intervention Engine │
  │  • cek kuota harian  │
  │  • cek cooldown      │
  │  • pilih level       │
  │  • pilih channel     │
  └──────────┬───────────┘
             │ Decision (SHOW / SUPPRESS)
             ▼
     Channel (Notification / In-app / Fullscreen)
             │
             ▼
     InterventionLog (INTERACTED / DISMISSED / EXPIRED)
```

---

## 2. Drift Detector (PRD §10.3)

### 2.1 Definisi Baseline (personal)

Baseline **bukan** aturan universal. Ia statistik dari data nyata user.

Per app `p`, dari `DailyUsageSummary`/`UsageSession`:
- `avgSessionMs[p]` — rata-rata durasi sesi
- `stdDevMs[p]` — standar deviasi durasi sesi
- `sampleDays[p]` — jumlah hari data tersedia

Simpan di `weekly_usage_summary.baselineJson`.

### 2.2 Rumus Deviasi

```
deviation(p) = (currentSessionMs - avgSessionMs[p]) / max(stdDevMs[p], EPSILON)
```

Alternatif bila stdDev ~0: gunakan rasio `currentSessionMs / avgSessionMs[p]`.

### 2.3 Kondisi Drift (FR-D3)

Drift `true` **hanya jika ketiganya** benar:

```
1. sampleDays[p] >= MIN_SAMPLE_DAYS          (data cukup — FR-D6)
2. deviation(p)  >= DRIFT_DEVIATION_THRESHOLD
3. exists(task where status != DONE AND priority IN {HIGH, URGENT})
   AND task belum kedaluwarsa/arsip          (ada task penting tertunda)
```

**Parameter awal (tunable, bukan kebenaran absolut):**

| Parameter | Nilai awal | Catatan |
|---|---|---|
| `MIN_SAMPLE_DAYS` | 3 | Baseline belum berarti di bawah ini |
| `DRIFT_DEVIATION_THRESHOLD` | 2.0 σ | ≥ 2 std-dev dari kebiasaan |
| `EPSILON` | 60_000 ms | Cegah bagi nol |

> ❌ JANGAN pakai "> 30 menit = buruk". ✅ Pakai deviasi personal (PRD prinsip keras).

### 2.4 Output `DriftSignal`

```kotlin
data class DriftSignal(
    val packageName: String,
    val currentSessionMs: Long,
    val baselineAvgMs: Long,
    val deviationSigma: Double,
    val relatedTaskId: String?,       // task penting tertunda (jika ada)
    val detectedAt: Long
)
```

**Bahasa sinyal netral** (FR-D5): "You've been scrolling longer than usual" —
**bukan** "You are addicted".

### 2.5 Deteksi: Periodic vs Real-time (PRD §10.3)

| Phase | Sumber | Cara | Sifat |
|---|---|---|---|
| **2** | `UsageStatsManager` + `WorkManager` | Periodic (~15 m saat layar aktif), evaluasi akumulasi | Observasi historis |
| **3** | `AccessibilityService` | Event transisi app → sinyal instan | Eksperimental, feature-flagged |

Phase 3 **opsional**; jika tidak tersedia → tetap berjalan dengan Phase 2 saja.

---

## 3. Intervention Engine (PRD §10.4)

### 3.1 State yang Diperlukan

```
quotaUsedToday          = count(InterventionLog WHERE status ∈ {INTERACTED, DISMISSED}
                                AND shownAt ∈ hari-lokal-ini)
quotaRemaining          = MAX_DAILY(3) - quotaUsedToday
cooldownActive(context) = exists(InterventionLog WHERE contextPackage = context
                                AND lastAction = "Later" AND now - resolvedAt < 2j)
```

### 3.2 Keputusan (Pseudocode)

```
function evaluate(signal, now):
    if signal == null: return SUPPRESS("no-drift")

    if quotaRemaining(now) <= 0:
        return SUPPRESS("quota-exhausted")        # TIDAK mengurangi kuota

    if cooldownActive(signal.packageName, now):
        return SUPPRESS("cooldown")               # TIDAK mengurangi kuota

    if userIsInsideAnchor(signal.packageName):
        return SUPPRESS("user-in-app")             # jangan ganggu dgn notif yg sama

    level    = chooseLevel(signal, history)        # naik bertahap, TIDAK lompat ke 4
    channel  = chooseChannel(level, permissions)   # default Notification
    return SHOW(level, channel, signal)
```

### 3.3 Pemilihan Level (naik bertahap — FR-I2)

| Level | Nama | Contoh pesan | Prasyarat |
|---|---|---|---|
| 1 | **Gentle** | "You've been here for a while." | drift dasar |
| 2 | **Contextual** | "Your Web Assignment is still unfinished." | ada `relatedTaskId` |
| 3 | **Future View** | "Starting now gives you ~2 hours free tonight." | task punya estimasi/deadline |
| 4 | **Focus** | "Start Focus." | user pernah interaksi ≥ level 3 |

**Aturan eskalasi:** Naik **satu tingkat** per siklus; jangan langsung Level 4.
Turun/kembali ke Level 1 bila user belum pernah berinteraksi sebelumnya.

### 3.4 Pemilihan Channel (FR-I3)

```
1. Notification    (utama)   — jika POST_NOTIFICATIONS granted
2. In-app          (kedua)   — jika notification ditolak / user di dalam app
3. Fullscreen      (jarang)  — hanya kondisi benar-benar relevan
```

Fallback: **notification ditolak → in-app saja** (PRD Edge Cases).

### 3.5 Aksi Utama (FR-I7)

Setiap intervensi menawarkan: **[Start Focus]** / **[Later]** (+ opsi lihat Future View).

| Aksi user | Efek | Kuota |
|---|---|---|
| **Start Focus** | Buka Focus Setup; catat `INTERACTED` | −1 |
| **Later** | Set cooldown **2 jam** untuk konteks/app tersebut; catat `DISMISSED` | −1 |
| (swipe away / abaikan) | Catat `DISMISSED` | −1 |
| (tidak beraksi sampai kedaluwarsa) | Catat `EXPIRED` | **0** |

> Catatan penting dari PRD §10.4: **Interacted** & **Dismissed** menghabiskan kuota;
> **Expired** tidak. "Later" memicu **cooldown 2 jam** per konteks.

### 3.6 Kuota & Cooldown — Aturan Keras

```
MAX_DAILY_INTERVENTIONS = 3
RESET                   = tengah malam waktu lokal
COOLDOWN_AFTER_LATER    = 2 jam (per app/konteks)
```

- Deteksi yang **ditahan** tidak mengurangi kuota (pemisahan Detection vs Attempt).
- Kuota reset tiap hari lokal baru (FR-I6).

---

## 4. State Machine Intervensi

```
        (DriftSignal)
             │
             ▼
   ┌──────────────────┐   quota==0 / cooldown / in-app
   │   EVALUATING     │ ───────────────────────────► SUPPRESSED (no cost)
   └────────┬─────────┘
            │ boleh tampil
            ▼
   ┌──────────────────┐
   │     SHOWN        │  (tetapkan level + channel, catat log shownAt)
   └────────┬─────────┘
            │
   ┌────────┼───────────────┬──────────────────┐
   ▼        ▼               ▼                  ▼
INTERACTED DISMISSED      EXPIRED          (Later-action)
 (−1 kuota) (−1 kuota)     (0 kuota)        DISMISSED + cooldown 2j
   │           │
   ▼           ▼
Start Focus  set cooldown
```

---

## 5. Contoh Skenario (mengacu Acceptance Criteria PRD §10.4)

**Skenario A — intervensi kontekstual**
```
Given  user punya high-priority task belum selesai
When   user scrolling jauh di atas baseline personal
Then   Anchor DAPAT memberi Contextual Intervention (Level ≥ 2)
       — hanya jika kuota & cooldown mengizinkan
```

**Skenario B — cooldown "Later"**
```
Given  user menekan "Later"
When   dalam 2 jam user kembali scrolling app yang sama
Then   Anchor TIDAK langsung mengirim intervensi lagi  → SUPPRESS("cooldown")
```

**Skenario C — kuota harian**
```
Given  hari ini sudah ada 3 intervensi "bermakna" (INTERACTED/DISMISSED)
When   sinyal drift baru muncul
Then   Anchor menahan intervensi tambahan  → SUPPRESS("quota-exhausted")
       (deteksi ini TIDAK mengurangi kuota lebih lanjut)
```

**Skenario D — baseline kurang**
```
Given  sampleDays[app] < MIN_SAMPLE_DAYS
When   ada penggunaan panjang
Then   Anchor TIDAK mengklaim drift  (FR-D6)
```

---

## 6. Bahasa & Nada (WAJIB)

| ✅ Boleh | ❌ Dilarang |
|---|---|
| "You've been here longer than usual." | "You are addicted." |
| "Your Web Assignment is still unfinished." | "You are lazy." |
| "Based on your current schedule..." | "I know you'll fail." |
| "Starting now gives you ~2 hours free." | "Don't waste your life." |

Prinsip: **awareness, bukan tuduhan**. Netral, ringkas, mendukung (PRD §4).

---

## 7. Kontrak Antarmuka (Domain)

```kotlin
interface DriftDetector {
    suspend fun detect(now: Long): DriftSignal?
}

sealed interface InterventionDecision {
    data class Show(
        val level: Int,
        val channel: InterventionChannel,
        val signal: DriftSignal
    ) : InterventionDecision
    data class Suppress(val reason: SuppressReason) : InterventionDecision
}

enum class SuppressReason { NO_DRIFT, QUOTA_EXHAUSTED, COOLDOWN, USER_IN_APP, NO_PERMISSION }

interface InterventionEngine {
    suspend fun evaluate(signal: DriftSignal?, now: Long): InterventionDecision
    suspend fun recordAction(logId: String, action: InterventionAction, now: Long)
}

enum class InterventionAction { START_FOCUS, LATER, DISMISS, EXPIRE }
```

---

## 8. Integrasi dengan Channel

| Channel | Implementasi | Fallback |
|---|---|---|
| Notification | `NotificationManager` + channel "intervention" | → in-app |
| In-app | Compose overlay / banner di screen aktif | → suppress |
| Fullscreen | Activity transparan (jarang) | → notification/in-app |

**Aturan:** Jangan kirim notification untuk hal yang sama saat user **sedang di dalam Anchor**
(PRD Edge Cases). Hormati Emergency/kondisi khusus user — **tidak ada mekanisme paksa**.

---

## 9. Telemetri Internal (lokal, untuk evaluasi diri)

Semua metrik tinggal di device (tidak dikirim):
- jumlah intervensi/hari (aktual vs kuota)
- rasio INTERACTED vs DISMISSED vs EXPIRED
- pemulihan fokus setelah intervensi (kembali ke task dalam X menit)
- drift yang ditahan (untuk tuning threshold)

Dipakai untuk **tuning parameter** (threshold, sample days), bukan untuk menghakimi.

---

## 10. Referensi Silang

- Aturan perilaku & acceptance → `01-prd.md` §10.3, §10.4
- Tabel `intervention_log`, `usage_session`, baseline → `03-database-schema.md` §3, §5
- Worker & pipeline → `02-architecture.md` §4

---

> **CATATAN AKHIR:** Parameter numerik di dokumen ini (`MIN_SAMPLE_DAYS`,
> `DRIFT_DEVIATION_THRESHOLD`, dsb.) adalah **nilai awal yang tunable**, bukan aturan
> terkunci. Yang **terkunci** adalah: kuota max 3/hari, cooldown 2 jam, pemisahan
> Detection vs Attempt, dan larangan aturan universal/diagnosis.
