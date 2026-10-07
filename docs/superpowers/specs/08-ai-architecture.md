# AI Architecture — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Prinsip:** AI **optional**, hanya **aggregated data**, wajib **rule-based fallback**.
> **Dokumen terkait:** `03-feature-specification.md` (§8), `08-privacy-model.md`

---

## 1. Prinsip AI

1. AI **bukan** sumber kebenaran data penggunaan — Android adalah sumbernya.
2. AI **menginterpretasikan** data agregat, bukan mengumpulkannya.
3. AI **tidak wajib** — aplikasi penuh fungsi tanpa AI.
4. AI **opt-in** — default **off**.
5. AI **tidak** membuat klaim di luar data.
6. AI **dibatasi** (cache, batch, 1x/hari default).
7. Output **terstruktur** (JSON schema).

---

## 2. Alur Data AI

```
Android (UsageStats, task logs, focus sessions)
        │
        ▼
Local Aggregation  ←─ hanya statistik, bukan raw
        │
        ▼
AggregatedInsightInput (structured, minimal)
        │
        ▼  [hanya jika AI enabled & online]
AiInsightSource (Gemini)
        │
        ▼
StructuredInsight (JSON validated)
        │
        ▼
Local DB (Insight) ──► UI (label sumber: AI / local)
```

**Guard:** sebelum keluar device, payload diverifikasi **hanya berisi agregat**.

---

## 3. Abstraksi

```kotlin
interface AiInsightSource {
    val id: String
    suspend fun isAvailable(): Boolean
    suspend fun generate(input: AggregatedInsightInput): InsightResult
}

class GeminiInsightSource(
    private val apiKeyProvider: () -> String?,
    private val httpClient: HttpClient
) : AiInsightSource { ... }

class RuleBasedInsightSource(
    private val rules: List<InsightRule>
) : AiInsightSource { ... }
```

`InsightRepository` memilih source:
```
ai_enabled && apiKey != null && online && !overQuota
    → GeminiInsightSource
else
    → RuleBasedInsightSource
```

---

## 4. Input Schema (yang dikirim — AGGREGATED ONLY)

```json
{
  "date": "2026-10-07",
  "tasks": {
    "total": 3, "completed": 2, "delayed": 1, "rescheduled": 1,
    "high_priority_pending": 1
  },
  "focus": {
    "sessions": 2, "total_minutes": 102, "avg_minutes": 51, "completed_sessions": 1
  },
  "usage": {
    "screen_time_minutes": 318,
    "top_apps": [
      { "label": "TikTok", "category": "social", "minutes": 130, "sessions": 6 },
      { "label": "YouTube", "category": "entertainment", "minutes": 72, "sessions": 3 }
    ],
    "longest_session_minutes": 47,
    "baseline_deviation_percent": 161
  },
  "patterns_hint": {
    "productive_window": "morning",
    "longest_distraction_window": "evening"
  }
}
```

**Yang TIDAK pernah dikirim:**
- Judul/deskripsi task mentah (hanya angka/label kategori)
- Raw usage event
- Nama package mentah (gunakan label ramah/kategori bila memungkinkan)
- Identitas perangkat/user

> `[DECISION]` Apakah label app (mis. "TikTok") boleh dikirim? Rekomendasi: kirim
> **label kategori + durasi**, hindari nama package mentah bila memungkinkan. Bisa
> dikonfigurasi. **Tidak dikunci.**

---

## 5. Output Schema

```json
{
  "summary": "You completed your most important task earlier than usual today.",
  "patterns": [
    "Long scrolling sessions often happen between 9 PM and 11 PM."
  ],
  "risk_level": "low",
  "suggestions": [
    "Tomorrow, start your hardest task within the first productive window."
  ],
  "recommended_action": "start_focus"
}
```

**Validasi wajib:**
- Semua field sesuai tipe.
- `risk_level` ∈ {low, medium, high}.
- `recommended_action` ∈ {start_focus, plan_task, none}.
- Jika validasi gagal → **tolak & fallback** rule-based.

---

## 6. Prompt Design (Gemini)

System instruction (ringkas):
> Kamu adalah asisten fokus yang tenang. Interpretasikan HANYA data agregat yang
> diberikan. Jangan mengarang data. Jangan mendiagnosis kondisi medis. Gunakan
> bahasa netral, mendukung, non-menghakimi. Kembalikan JSON sesuai schema. Jika
> data tidak cukup, nyatakan keterbatasannya.

Guardrails:
- Temperature rendah (deterministik-ish).
- Output JSON mode bila tersedia.
- Tolak respons di luar schema.

---

## 7. Rule-Based Fallback (selalu tersedia)

Aturan lokal (contoh):
```
IF completed_high_priority_before_noon THEN
  summary = "You completed an important task early today."
IF has_long_evening_distraction THEN
  suggestion = "Your evenings are your weakest window — try a short focus block before dinner."
IF task_rescheduled_more_than_once THEN
  pattern  = "You postponed this task; similar tasks often take longer after repeated delays."
IF focus_completion_rate_low THEN
  suggestion = "Try shorter focus blocks (25m) to build momentum."
```

Fallback menghasilkan **struktur yang sama** → UI tidak perlu tahu sumbernya
(hanya menampilkan label "local").

---

## 8. Batching, Cache, Rate Limits

| Aspek | Aturan |
|---|---|
| Frekuensi default | 1x/hari (Daily Reflection) |
| On-demand | Saat user buka Insights & cache kadaluarsa |
| Cache | Simpan `Insight` per hari (`date_epoch_day` + source) → hindari panggilan ulang |
| Rate limit | Batasi jumlah request/hari (mis. ≤5) |
| Retry | Backoff eksponensial untuk error sementara |
| Offline | Langsung rule-based; tidak ada retry agresif |

**Jangan** memanggil AI setiap app dibuka. **Jangan** memanggil saat offline.

---

## 9. Biaya & Kuota

- Karena single-user & 1x/hari, biaya Gemini sangat kecil.
- Tetap ada **guard**: hitung request harian; bila melewati batas → rule-based.
- Tampilkan status kuota/error di Settings (jujur, tidak silent fail).

> `[EXPERIMENT REQUIRED]` Validasi biaya & rate limit nyata setelah API key diisi.

---

## 10. Error Handling

| Kondisi | Perilaku |
|---|---|
| API key kosong | Rule-based, tanpa error |
| Offline | Rule-based |
| Quota habis (429) | Rule-based + info di Settings |
| Respons invalid | Tolak, fallback, log internal |
| Server error (5xx) | Retry backoff, lalu fallback |
| Timeout | Fallback |

Semua kondisi → **user tetap melihat insight** (dari rule-based).

---

## 11. Keamanan & Privasi AI

- API key tidak di-log.
- HTTPS only.
- Tidak ada telemetry pihak ketiga.
- Payload agregat minimal; user dapat melihat **apa yang dibagikan** di Settings
  (transparency UI).
- AI default off.

> Detail → `08-privacy-model.md`.

---

## 12. Acceptance (ringkas)

```
Given  AI off/offline
When   buka Insights
Then   rule-based insight tampil, tanpa error

Given  AI on & data cukup
When   analisis harian dijalankan
Then   insight AI tersimpan & sumber ditampilkan jujur

Given  payload mengandung raw usage
When   hendak dikirim
Then   DITOLAK (guard), hanya agregat
```
