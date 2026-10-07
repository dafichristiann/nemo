# Privacy Model — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Prinsip inti:** Privacy-preserving by default. Raw data tidak meninggalkan device.
> **Dokumen terkait:** `06-technical-architecture.md`, `08-ai-architecture.md`, `09-permission-model.md`

---

## 1. Prinsip

1. **Local-first** — data inti hidup & diproses di device.
2. **Raw data never leaves the device** — kecuali user secara eksplisit memilih.
3. **Aggregated only to AI** — dan hanya jika AI di-opt-in.
4. **User in control** — bisa lihat, atur, dan hapus data.
5. **Default = paling aman** — tracking & AI **off** sampai user mengaktifkan.
6. **Transparency** — jelaskan dengan bahasa sederhana apa yang dipakai & kenapa.

---

## 2. Data Classification

| Kategori | Contoh | Lokasi | Boleh ke AI? |
|---|---|---|---|
| **A. Local-only (raw)** | usage events, session detail, task mentah | Device (Room) | ❌ Tidak |
| **B. Aggregated** | statistik harian, total menit, hitungan sesi | Device (Room) | ✅ Ya (jika opt-in) |
| **C. Settings** | preferensi, API key, blocked apps | DataStore | ❌ (key tidak dikirim) |
| **D. Derived insight** | summary, patterns, suggestion | Device (Room) | N/A (hasil) |

**Raw usage** (A) **tidak pernah** dikirim ke cloud. Hanya agregat (B) yang mungkin.

---

## 3. Alur Privasi

```
Raw Usage (A)  ──►  Local Processing  ──►  Aggregated (B)
   [device only]                              │
                                              ▼
                                     [hanya jika AI opt-in]
                                              ▼
                                        AI (cloud)
                                              ▼
                                     Insight (D) di device
```

---

## 4. Retention Policy

| Data | Retensi default | Dapat diubah? |
|---|---|---|
| Usage snapshots/sessions | 30 hari | ✅ di Settings |
| Interventions log | 30 hari | ✅ |
| Insights | 90 hari | ✅ |
| Tasks | Tanpa batas (milik user) | Hapus manual |
| Activity log | 30 hari | ✅ |

Worker pembersih menghapus data melewati retensi secara periodik.

---

## 5. Delete Data Behavior

- **Per-kategori:** hapus usage history / insights / dll.
- **Hapus semua:** konfirmasi → hapus semua tabel + reset DataStore (kecuali yang
  user pilih pertahankan).
- **Hapus tanpa backup:** tidak ada salinan tersembunyi; ini personal & lokal.

---

## 6. Transparency UI

Di **Settings → Privacy & AI**, tampilkan sederhana:

> **What Anchor stores (on your device)**
> • Your tasks
> • App usage statistics
> • Focus sessions
> • Daily insights
>
> **What leaves your device**
> • Nothing, unless you turn on AI insights
> • When on: only aggregated stats (no raw usage, no task titles)
>
> **AI insights:** [ Off ]
> **Data retention:** 30 days
> **Delete all data**

Tidak ada jargon. Tidak ada klaim berlebihan.

---

## 7. AI Opt-In Detail

- AI default **off**.
- Saat diaktifkan, tampilkan **apa yang akan dibagikan** (contoh payload agregat).
- User dapat mematikan kapan pun → source berpindah ke rule-based.
- API key disimpan lokal; tidak di-log; tidak dikirim ke pihak lain.

---

## 8. Tracking Controls

- Tracking (wellbeing) default **off** sampai izin diberikan.
- User dapat mematikan tracking kapan pun; data lama mengikuti kebijakan retensi.
- Tidak ada tracking latar belakang yang tidak terlihat.

---

## 9. App Categories & Override

- Kategori app dikelola lokal.
- Override manual (mis. YouTube → productivity) hanya di device.
- Jika AI aktif, yang dikirim adalah **kategori + durasi**, bukan nama package mentah
  (`[DECISION]` — lihat AI architecture §4).

---

## 10. Security

- Data lokal disimpan dalam sandbox app (Android).
- API key: DataStore (MVP). `[DECISION]` bisa ditingkatkan ke EncryptedSharedPreferences/Keystore.
- HTTPS only untuk AI.
- Tidak ada SDK analytics/tracking pihak ketiga.

---

## 11. Threat Considerations (ringkas)

| Ancaman | Mitigasi |
|---|---|
| Data usage bocor ke cloud | Guard hanya agregat; AI opt-in; raw tak dikirim |
| API key bocor | Tidak di-log; simpan lokal; (opsi) enkripsi |
| Akses fisik ke device | Sandbox Android; (opsi) app lock ke depan |
| Diagnosa/klaim berlebih | Larangan diagnosis di prompt & rule |

---

## 12. Non-Negotiable

> **Raw device activity TIDAK boleh dikirim ke AI** tanpa alasan jelas & desain
> privasi eksplisit. Ini prinsip tetap produk.
