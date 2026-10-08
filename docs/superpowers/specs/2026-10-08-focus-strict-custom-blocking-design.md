# Focus Strict/Custom Blocking via AccessibilityService — Design Spec

> **Status:** Approved design (brainstorming) · **Tanggal:** 2026-10-08
> **Terkait:** PRD `01-prd.md` §10.5 & §14; `08-permissions.md` §4/§5/§8; `13-spike-b-accessibility.md`;
> `14-hardening-report.md` §11; `04-intervention-engine.md` §2.5
> **Cakupan:** Menuntaskan FR-F4/FR-F8 yang selama ini masih `GentleBlockingStrategy` (no-op).

---

## 1. Tujuan & Kriteria Sukses

**Tujuan:** Wujudkan mode Focus **Strict** & **Custom** yang benar-benar menjeda app
distraksi terpilih, memakai `AccessibilityService` yang **opsional, feature-flagged,
opt-in**, dengan **fallback ke Gentle** yang andal — tanpa pernah gagal keras.

**Kriteria sukses (selaras PRD §10.5 Acceptance Criteria):**
1. User memilih Strict/Custom → app yang diblokir benar-benar dijeda selama sesi.
2. Emergency exit **selalu** tersedia dan seketika menghentikan pemblokiran.
3. Bila service tak tersedia / dimatikan OS / OEM memblokir → sesi lanjut sebagai
   **Gentle** + user diberi tahu jujur (FR-F8).
4. Tidak ada blocking saat fitur off (default). App tetap berguna penuh sebagai Smart Todo.
5. Tidak ada pembacaan konten layar (privasi, docs 09).

**Di luar cakupan:** deteksi drift real-time (Phase 3 sinyal) — tetap lewat WorkManager;
spesifikasi ini hanya soal **pemblokiran Strict/Custom**.

---

## 2. Prinsip (terkunci dari PRD & Spike B)

- **Awareness > Control** (PRD §4): pemblokiran **tidak memaksa**. Saat app terblokir dibuka,
  user melihat **layar perantara "kembali fokus"** — bukan paksa keluar tanpa penjelasan.
- **Accessibility bukan fondasi** (spike B §3.4, docs 08 §4): selalu ada jalur degradasi.
- **Feature-flagged & opt-in** (spike B §3.1): default nonaktif, butuh dua gerbang
  (build flag + toggle Settings) **dan** izin OS.
- **Graceful fallback wajib** (FR-F8): setiap kegagalan → Gentle, bukan crash/gagal keras.
- **Privasi** (docs 09): service **tidak** membaca konten; hanya nama package dari event
  perpindahan window.

---

## 3. Arsitektur & Alur

### 3.1 Gerbang tiga lapis
Pemblokiran hanya aktif bila **ketiganya** benar:
1. **Build flag** `BuildConfig.FOCUS_BLOCKING_ENABLED` (konstanta build).
2. **Toggle Settings** `AppSettings.focusBlockingEnabled` (opt-in runtime, default `false`).
3. **Izin OS** — `AnchorAccessibilityService` sedang aktif
   (`AccessibilityManager.getEnabledAccessibilityServiceList`).

Bila salah satu gagal → `AccessibilityBlockingStrategy.isAvailable() == false` →
`StartFocusUseCase` mencatat sesi sebagai **Gentle** + `fallbackReason` (sudah ada).

### 3.2 Alur end-to-end
```
Focus Setup (pilih Strict/Custom)
   │  jika service belum aktif → kartu penjelasan + tombol "Aktifkan di Settings"
   v
onBegin() → StartFocusUseCase.invoke(mode, blockedApps)
   │  mode Strict/Custom:
   v
AccessibilityBlockingStrategy.isAvailable()  ── false ──> engage() returns false → Gentle + notice
   │ true
   v
engage(blockedApps) → BlockingState.engage(pkgs)   // sessionActive=true, blockedApps=pkgs
   │
   v
AnchorAccessibilityService (onAccessibilityEvent: TYPE_WINDOW_STATE_CHANGED)
   │  event.packageName ∈ BlockingState.blockedApps && sessionActive
   v
FocusBlockOverlayActivity  // "Kembali ke fokus" + tombol kembali
   │
onFinish()/onEmergencyExit() → FinishFocusUseCase.release()
   v
AccessibilityBlockingStrategy.release() → BlockingState.release()  // kosongkan
```

### 3.3 Mengapa `BlockingState` in-memory
`AccessibilityService` dan app berjalan **in-process** (sama-sama `com.anchor.app`), sehingga
state live sesi dapat dibagi lewat singleton `@Singleton`. Ini **state sesi ephemeral**, bukan
data persisten: bila proses dibunuh OS, sesi juga tidak valid lagi — jadi tidak perlu Room.
`release()` idempoten (sesuai kontrak `BlockingStrategy`).

---

## 4. Komponen & Antarmuka

### 4.1 Komponen baru

| Komponen | Paket | Peran | Antarmuka kunci |
|---|---|---|---|
| `BlockingState` | `platform.blocking` | State live sesi; dibaca service, ditulis strategy. | `state: StateFlow<BlockingSnapshot>`; `engage(Set<String>)`; `release()`; `snapshot()` |
| `AccessibilityBlockingStrategy` | `platform.blocking` | Implementasi `BlockingStrategy` nyata. | `effectiveMode`, `isAvailable()`, `suspend engage(Set<String>)`, `suspend release()` |
| `AccessibilityStatus` | `platform.accessibility` | Cek status service & buka Settings izin. | `isEnabled(context): Boolean`; `open(context)` |
| `AnchorAccessibilityService` | `platform.accessibility` | Enforcement; deteksi window-change. | `AccessibilityService.onAccessibilityEvent` |
| `FocusBlockOverlayActivity` | `feature.focus` | Layar perantara "kembali fokus". | Activity; tombol → `finish()` / kembali |
| `anchor_accessibility_config.xml` | `res/xml` | Metadata service (event types, no content). | XML resource |

**`BlockingSnapshot`:**
```kotlin
data class BlockingSnapshot(
    val sessionActive: Boolean = false,
    val blockedApps: Set<String> = emptySet(),
)
```

### 4.2 Perubahan pada yang ada

| File | Perubahan |
|---|---|
| `core/di/BlockingModule.kt` | Ganti binding: `@Provides` pilih `AccessibilityBlockingStrategy` saat flag on, else `GentleBlockingStrategy`. |
| `core/domain/model/AppSettings.kt` | + `focusBlockingEnabled: Boolean = false` |
| `core/data/entity/AppSettingsEntity.kt` | + kolom `focusBlockingEnabled: Boolean = false` |
| `core/data/AnchorDatabase.kt` | + `MIGRATION_8_9`, daftar di `ALL_MIGRATIONS`, naikkan `version = 9` |
| `core/data/repository/SettingsRepositoryImpl.kt` | map kolom baru (toDomain/toEntity) + `setFocusBlockingEnabled` |
| `core/domain/repository/SettingsRepository.kt` | + `suspend fun setFocusBlockingEnabled(enabled: Boolean)` |
| `feature/settings/SettingsViewModel.kt` | + `setFocusBlockingEnabled`, + status izin Accessibility di `PermissionStatus`, refresh on resume |
| `feature/settings/SettingsScreen.kt` | + toggle "Focus blocking (experimental)" + `PermissionRow` Accessibility |
| `feature/focus/FocusViewModel.kt` | + `blockingAvailable`/status izin di state; refresh on resume |
| `feature/focus/FocusScreen.kt` | + kartu penjelasan + tombol "Aktifkan di Settings" saat Strict/Custom & belum siap |
| `AndroidManifest.xml` | + deklarasi `<service ...BIND_ACCESSIBILITY_SERVICE>` + `FocusBlockOverlayActivity` |
| `app/build.gradle.kts` | + `buildConfigField("boolean", "FOCUS_BLOCKING_ENABLED", ...)` |
| `feature/settings/SettingsScreen.kt` (nav) | route overlay tidak perlu; Activity overlay bukan route nav |

**Tidak berubah:** interface `BlockingStrategy` (kontraknya sudah memadai), `StartFocusUseCase`,
`FinishFocusUseCase`, `FocusService` (FR-F7), domain `FocusMode`/`FocusSession`.

---

## 5. Data, State & Penanganan Error

### 5.1 Migrasi Room v8 → v9 (aditif, nondestruktif)
```sql
ALTER TABLE `app_settings` ADD COLUMN `focusBlockingEnabled` INTEGER NOT NULL DEFAULT 0
```
Default `0` (OFF) — selaras prinsip privacy-first & spike B "default nonaktif".

### 5.2 Matriks degradasi (semua jalur → Gentle)

| Kondisi | Deteksi | Perilaku |
|---|---|---|
| Build flag OFF | konstanta build | `isAvailable()=false` → Gentle |
| Toggle Settings OFF | `AppSettings.focusBlockingEnabled` | `isAvailable()=false` → Gentle |
| Izin Accessibility belum ada | `AccessibilityStatus.isEnabled()` | kartu penjelasan + tombol; tetap tidak aktif → Gentle + notice |
| OS mematikan service saat sesi jalan | `onUnbind`/`onInterrupt` service | `BlockingState.release()`; saat app kembali, `onResume` cek ulang → bila Strict tapi service mati → notice fallback |
| OEM memblokir | `engage()` gagal / start Activity gagal | return false → `fallbackReason` tercatat (existing) |
| Overlay gagal start | `runCatching` di sekitar `startActivity` | diabaikan (non-fatal), lanjut |

### 5.3 Emergency exit (FR-F5)
Selalu tersedia dari: (1) Focus Active UI, (2) notifikasi FGS (`FocusService`),
(3) hint di overlay. `FinishFocusUseCase` → `release()` seketika mengosongkan `BlockingState`,
sehingga tidak ada app yang tertahan setelah exit.

### 5.4 Privasi
`anchor_accessibility_config.xml`: `android:canRetrieveWindowContent="false"`. Service hanya
membaca `event.packageName`; tidak ada konten, tidak ada egress (docs 09).

---

## 6. UI

- **Focus Setup** (modul Opsi 2 yang sudah ada + tambahan): saat mode Strict/Custom dipilih
  **dan** service belum aktif → kartu penjelasan ("Aktivitas app dibaca lokal untuk menjeda
  distraksi…") + tombol **"Aktifkan di Settings"** → `AccessibilityStatus.open()`.
  Ringkasan "n app(s) akan dijeda" tetap seperti Opsi 2.
- **Settings**: `PermissionRow` untuk Accessibility (pakai pola `UsageAccessSettings` yang ada,
  beri fallback intent defensif) + `SettingRow` toggle "Focus blocking (experimental)".
- **Overlay Activity**: layar sederhana, non-judgmental, tombol "Kembali" (menutup overlay) +
  pengingat emergency exit tersedia.
- Semua teks baru lewat `strings.xml` **en + id**, nada calm.

---

## 7. Testing (TDD)

**Unit (JVM, fake — pola `FocusUseCasesTest`):**
1. `isAvailable()==false` → `engage()` return false → `StartFocusUseCase` mencatat Gentle +
   `fallbackReason` (sudah ada; tambah kasus flag/toggle off).
2. `isAvailable()==true` → `engage(pkgs)` mengisi `BlockingState` (`sessionActive`, `blockedApps`).
3. `release()` mengosongkan state dan **idempoten** (dipanggil dua kali aman).
4. Keputusan gerbang: flag off & toggle off & service off → masing-masing tak tersedia.
5. `BlockingState` murni: `engage` → `release` transisi benar.

**Instrumented (androidTest):**
6. Migrasi **v8→v9** (pola `MigrationTest` yang ada) — kolom baru ada, default 0.
7. `BlockingState`/strategy bila butuh konteks.

**Manual (HP TECNO CN7c + emulator):**
8. Aktifkan izin → pilih Strict dengan app terblokir → buka app itu → overlay muncul.
9. Emergency exit saat overlay tampil → blocking langsung berhenti.
10. Cabut izin saat sesi jalan → kembali ke app → notice fallback.
11. Flag/toggle off → sesi Gentle, tanpa overlay.

---

## 8. Rencana Implementasi (ringkas — detail di implementation plan)

1. Room v8→v9 + `AppSettings`/entity/repo + test migrasi.
2. `BlockingState` + `AccessibilityBlockingStrategy` + `AccessibilityStatus` + DI binding (TDD).
3. `AnchorAccessibilityService` + config XML + manifest.
4. `FocusBlockOverlayActivity` + manifest.
5. Settings toggle + permission row + strings (en/id).
6. Focus Setup kartu izin + refresh on resume + strings.
7. `buildConfigField` flag.
8. Verifikasi: unit + instrumented + HP + emulator.

---

## 9. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| AccessibilityService dihentikan OEM | fallback Gentle + `onResume` re-check (FR-F8) |
| Review Play (kebijakan aksesibilitas) | eksperimental, opt-in, teks jelas, tunda aksesibilitas tak sesuai tujuan tunggal |
| Overlay mengganggu UX | hanya saat app terblokir & sesi aktif; tombol kembali instan |
| Migrasi gagal | aditif, default aman, diuji `MigrationTest` |
| Konsumsi baterai | service hanya memproses event window-change; tidak polling |

---

## 10. Referensi Silang

- Perilaku & AC → `01-prd.md` §10.5
- Model izin → `01-prd.md` §14, `08-permissions.md` §4/§5/§8
- Keputusan spike → `13-spike-b-accessibility.md`
- Status no-op saat ini → `14-hardening-report.md` §11
- Seam domain → `04-intervention-engine.md` §2.5, `core/domain/blocking/BlockingStrategy.kt`
