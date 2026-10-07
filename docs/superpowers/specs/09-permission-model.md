# Permission Model — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Prinsip:** First launch **tidak** memaksa izin apa pun. Smart Todo harus jalan tanpa izin.
> **Dokumen terkait:** `01-prd.md` (§14), `08b-privacy-model.md`, `04-ux-ia-wireframes.md`

---

## 1. Ringkasan Izin

| Izin | Jenis | Untuk | Kapan diminta | Jika ditolak/dinonaktifkan |
|---|---|---|---|---|
| `PACKAGE_USAGE_STATS` | Special access (manual Settings) | Wellbeing, Drift | Saat user ingin fitur wellbeing | App tetap jalan sebagai Smart Todo |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Intervensi, reminder, fokus | Sebelum intervensi/notif pertama | Fallback in-app |
| Accessibility Service | System setting (manual) | Blocking eksperimental | Saat user pilih Strict/Custom | Fallback ke Gentle |
| Foreground Service | Manifest + runtime (API 34+ typed) | Sesi fokus aktif | Saat fokus dimulai | Fokus terbatas (tanpa persistensi) |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Special (API 31+) | Reminder deadline kritis | Saat user butuh reminder tepat waktu | Reminder fleksibel (WorkManager) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (opsional) | Special | Stabilitas background | Saat user mengalami delay | Tetap jalan, mungkin telat |

---

## 2. Detail per Izin

### 2.1 `PACKAGE_USAGE_STATS`
- **Why:** membaca statistik penggunaan app (screen time, sesi) — inti Wellbeing & Drift.
- **When requested:** saat user menuju/membuka Wellbeing, atau menekan "Enable usage access".
- **What happens if denied:** Wellbeing menampilkan panduan; Smart Todo 100% jalan;
  Drift tidak aktif (tanpa intervensi).
- **How to enable later:** pintasan dari Settings Anchor → Android Usage Access.
- **Graceful degradation:** semua fitur non-usage tetap normal.
- **Cek status:** `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS, ...)`.

### 2.2 `POST_NOTIFICATIONS` (API 33+)
- **Why:** menyampaikan intervensi & reminder.
- **When requested:** kontekstual (saat intervensi/notif pertama), bukan di first launch.
- **If denied:** intervensi in-app saja; reminder tampil di app.
- **Enable later:** Settings Anchor → Notifications.

### 2.3 Accessibility Service
- **Why:** mendeteksi foreground app & (opsional) blocking — **eksperimental**, bukan fondasi.
- **When requested:** hanya saat user memilih mode Strict/Custom.
- **If off:** mode jatuh ke Gentle; app lain tetap normal.
- **Catatan:** diminta via Settings sistem (tidak ada dialog langsung).
- **Privacy:** hanya dipakai untuk fitur fokus; tidak mengirim apa pun ke luar.

### 2.4 Foreground Service
- **Why:** menjaga sesi fokus tetap hidup saat layar mati / app di background.
- **When:** hanya saat fokus aktif; berhenti saat fokus selesai.
- **Notif persisten:** wajib (menunjukkan status fokus + emergency exit).

### 2.5 Exact Alarm (API 31+)
- **Why:** reminder deadline yang tepat waktu.
- **When:** hanya jika user mengaktifkan reminder waktu.
- **If denied:** gunakan WorkManager (fleksibel), akui mungkin telat.

### 2.6 Battery Optimization (opsional)
- **Why:** sebagian OEM membunuh background → deteksi/agregasi telat.
- **When:** saat user mengalami masalah atau di panduan Settings.
- **If not disabled:** app tetap jalan; mungkin ada delay.

---

## 3. Permission Request Timing (anti-overwhelm)

```
First launch  → TIDAK meminta apa pun (hanya onboarding + skip)
           │
           ├─ Buat task pertama        → tidak ada izin
           │
           ├─ Buka Wellbeing           → minta PACKAGE_USAGE_STATS
           │
           ├─ Intervensi/notif pertama → minta POST_NOTIFICATIONS
           │
           ├─ Pilih fokus Strict/Custom→ minta Accessibility
           │
           └─ Reminder waktu kritis    → minta exact alarm
```

**Prinsip:** just-in-time, kontekstual, dengan penjelasan singkat & alasan.

---

## 4. Degradation Matrix

| Fitur | Semua izin ada | Usage off | Notif off | Accessibility off | Fokus off |
|---|---|---|---|---|---|
| Smart Todo | ✅ | ✅ | ✅ | ✅ | ✅ |
| Wellbeing | ✅ | ⚠️ (panduan) | ✅ | ✅ | ✅ |
| Drift/Intervensi | ✅ | ❌ | ⚠️ (in-app) | ✅ | ✅ |
| Focus Gentle | ✅ | ✅ | ✅ | ✅ | ✅ |
| Blocking Strict | ✅ | ✅ | ✅ | ⚠️ (→Gentle) | ⚠️ |
| Reminder waktu | ✅ | ✅ | ⚠️ | ✅ | ✅ |
| AI Insight | ✅ (jika opt-in) | ✅ | ✅ | ✅ | ✅ |

**Kunci:** tidak ada kombinasi izin yang membuat app crash atau tak berguna.

---

## 5. Acceptance Criteria

```
Given  first launch
When   user membuka Anchor
Then   tidak ada permintaan izin memaksa; Smart Todo langsung bisa dipakai

Given  izin usage ditolak
When   user membuka Wellbeing
Then   panduan jelas + tombol ke Settings; app tidak crash

Given  user memilih Strict tanpa Accessibility
When   mulai fokus
Then   berjalan dalam mode Gentle + pemberitahuan jelas

Given  izin notifikasi ditolak
When   ada intervensi
Then   ditampilkan in-app, bukan gagal senyap
```

---

## 6. Manifest (ringkas)

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<service android:name=".core.service.FocusForegroundService"
    android:foregroundServiceType="specialUse" />
<service android:name=".core.service.AnchorAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

> `[TECHNICAL RISK]` `FOREGROUND_SERVICE_SPECIAL_USE` & tipe service dapat berubah
> antar versi Android. Verifikasi saat development (targetSdk terbaru).

---

## 7. Prinsip Tetap

- **Never assume permissions are granted.**
- Selalu cek status sebelum memakai fitur terkait.
- Selalu sediakan jalur degradasi & cara mengaktifkan kembali.
