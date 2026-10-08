# Implementation Plan — Focus Strict/Custom Blocking via AccessibilityService

> **Spec:** `docs/superpowers/specs/2026-10-08-focus-strict-custom-blocking-design.md`
> **PRD:** v0.4 §10.5, §14, §16 (OQ-Focus-Blocking)
> **Tanggal:** 2026-10-08
> **Metode:** TDD (test-first untuk unit yang bisa diuji; instrumented untuk migrasi).
> **Gerbang:** Setiap fase berakhir hijau (`test` + `assembleDebug`) sebelum lanjut.

---

## Prasyarat & Konvensi

- Semua kelas baru → `com.anchor.app.platform.blocking` / `platform.accessibility`,
  Activity overlay → `com.anchor.app.feature.focus`.
- String baru → `res/values/strings.xml` **dan** `res/values-id/strings.xml`.
- Verifikasi cepat tiap fase: `.\gradlew.bat :app:testDebugUnitTest`.
- Verifikasi akhir tiap fase: `.\gradlew.bat :app:assembleDebug`.
- Tidak mengubah `BlockingStrategy` interface, `StartFocusUseCase`, `FinishFocusUseCase`,
  `FocusService` (kontrak sudah memadai).

---

## Fase 1 — Room v8 → v9 (flag opt-in)

**Tujuan:** kolom `focusBlockingEnabled` tersedia & termigrasi aman.

- [ ] **1.1 [TEST FIRST]** Tambah kasus di `MigrationTest` (androidTest): dari v8 → v9,
      tabel `app_settings` punya kolom `focusBlockingEnabled` default `0`.
- [ ] **1.2** `AppSettingsEntity`: `val focusBlockingEnabled: Boolean = false`.
- [ ] **1.3** `AnchorDatabase`:
  - `version = 9` dan `const val VERSION = 9`
  - `MIGRATION_8_9`:
    ```kotlin
    db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `focusBlockingEnabled` INTEGER NOT NULL DEFAULT 0")
    ```
  - tambahkan ke `ALL_MIGRATIONS`.
- [ ] **1.4** Ekspor skema (`app/schemas/.../9.json`) — dihasilkan otomatis saat build.
- [ ] **1.5** `AppSettings` (domain): `val focusBlockingEnabled: Boolean = false`.
- [ ] **1.6** `SettingsRepositoryImpl`: map `toDomain`/`toEntity`; tambah
      `setFocusBlockingEnabled(enabled)` (pola `setAiEnabled`).
- [ ] **1.7** `SettingsRepository` (interface): deklarasi `setFocusBlockingEnabled`.
- [ ] **1.8** `FakeSettingsRepository` (testutil): implementasi.

**Gate Fase 1:** `testDebugUnitTest` (termasuk `MigrationTest` bila dijalankan) hijau.

---

## Fase 2 — `BlockingState` + `AccessibilityBlockingStrategy` + `AccessibilityStatus` (TDD)

**Tujuan:** seam domain terisi perilaku nyata; test-first.

- [ ] **2.1 [TEST FIRST]** `BlockingStateTest`:
  - `engage(pkgs)` → `snapshot().sessionActive == true` & `blockedApps == pkgs`.
  - `release()` → kosong, `sessionActive == false`.
  - `release()` dua kali → tetap aman (idempoten).
- [ ] **2.2 [TEST FIRST]** `AccessibilityBlockingStrategyTest`:
  - flag off → `isAvailable() == false`, `engage()` return false.
  - flag on + toggle off → `isAvailable() == false`.
  - flag on + toggle on + service off → `isAvailable() == false`.
  - flag on + toggle on + service on → `engage(pkgs)` true & `BlockingState` terisi.
  - `release()` mengosongkan `BlockingState`.
  - Ganti binding Gentle↔Accessibility via flag.
- [ ] **2.3** `BlockingState` (`@Singleton`, `platform.blocking`):
  ```kotlin
  data class BlockingSnapshot(val sessionActive: Boolean = false, val blockedApps: Set<String> = emptySet())
  ```
  `state: StateFlow<BlockingSnapshot>`, `engage`, `release`, `snapshot()`.
- [ ] **2.4** `AccessibilityStatus` (`platform.accessibility`):
  `isEnabled(context): Boolean` via `AccessibilityManager.getEnabledAccessibilityServiceList`;
  `open(context)` mirror `UsageAccessSettings` (fallback intent defensif ke
  `Settings.ACTION_ACCESSIBILITY_SETTINGS`).
- [ ] **2.5** `AccessibilityBlockingStrategy` (`platform.blocking`, `@Inject`) — menerima
  `@ApplicationContext context`, `BlockingState`, dan pembaca toggle (mis. `SettingsRepository`
  atau lambda `isEnabled`). Implementasi `isAvailable()` = flag && toggle && `AccessibilityStatus.isEnabled`.
- [ ] **2.6** `BuildConfig` flag: `app/build.gradle.kts` →
  `buildConfigField("boolean", "FOCUS_BLOCKING_ENABLED", "true")` (default behavior; bisa `false` untuk matikan).
- [ ] **2.7** `BlockingModule`: ganti ke `@Provides` —
  ```kotlin
  @Provides @Singleton
  fun provideBlockingStrategy(
    gentle: GentleBlockingStrategy,
    strict: AccessibilityBlockingStrategy,
  ): BlockingStrategy = if (BuildConfig.FOCUS_BLOCKING_ENABLED) strict else gentle
  ```
  (atau delegasi runtime; pilih yang paling sederhana & testable).
- [ ] **2.8** Cek regresi: `StartFocusUseCase` test lama tetap hijau (Gentle default).

**Gate Fase 2:** `testDebugUnitTest` hijau.

---

## Fase 3 — `AnchorAccessibilityService` + config XML + manifest

**Tujuan:** enforcement nyata; overlay saat app terblokir dibuka.

- [ ] **3.1** `res/xml/anchor_accessibility_config.xml`:
  - `android:accessibilityEventTypes="typeWindowStateChanged"`
  - `android:accessibilityFeedbackType="feedbackGeneric"`
  - `android:canRetrieveWindowContent="false"`
  - `android:notificationTimeout="100"`
  - `android:accessibilityFlags="flagDefault"`
- [ ] **3.2** `AnchorAccessibilityService : AccessibilityService`:
  - `@AndroidEntryPoint` (butuh Hilt untuk `BlockingState`).
  - inject `BlockingState` + penyedia paket terblokir.
  - `onAccessibilityEvent`: bila `sessionActive` && `event.packageName ∈ blockedApps` && bukan
    Anchor sendiri → `startActivity(FocusBlockOverlayActivity)` dengan `FLAG_ACTIVITY_NEW_TASK`,
    dibungkus `runCatching` (non-fatal).
  - `onInterrupt`/`onUnbind`: `runCatching { blockingState.release() }` (deteksi OS matikan service).
- [ ] **3.3** `AndroidManifest.xml`:
  ```xml
  <service android:name=".platform.accessibility.AnchorAccessibilityService"
      android:exported="false"
      android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
      <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data android:name="android.accessibilityservice"
        android:resource="@xml/anchor_accessibility_config" />
  </service>
  ```

**Gate Fase 3:** `assembleDebug` hijau; service terdaftar di dump `dumpsys accessibility`.

---

## Fase 4 — `FocusBlockOverlayActivity`

**Tujuan:** layar perantara "kembali fokus" (non-paksa).

- [ ] **4.1** `FocusBlockOverlayActivity : ComponentActivity` — tema dialog/transparan;
  Compose content: pesan calm + tombol **Kembali** (→ `finish()`) + hint emergency exit.
- [ ] **4.2** Daftarkan di `AndroidManifest.xml`
  (`android:exported="false"`, `android:excludeFromRecents="true"`, tema overlay).
- [ ] **4.3** Guard: cegah double-launch (mis. `launchMode="singleTop"` atau cek di service).
- [ ] **4.4** Strings (en+id) untuk overlay.

**Gate Fase 4:** `assembleDebug`; manual: buka app terblokir saat sesi → overlay muncul; tombol kembali kerja.

---

## Fase 5 — Settings: toggle + izin Accessibility

- [ ] **5.1** `SettingsViewModel`:
  - `setFocusBlockingEnabled(enabled)` → `settingsRepository.set…`.
  - `PermissionStatus` + `focusBlockingGranted` (= `AccessibilityStatus.isEnabled`).
  - `refreshPermissions()` ikut memperbarui status Accessibility.
- [ ] **5.2** `SettingsScreen`:
  - `SettingRow` toggle "Focus blocking (experimental)" + subtitle jujur (opt-in, fallback).
  - `PermissionRow` Accessibility (`onEnable = { AccessibilityStatus.open(context) }`).
- [ ] **5.3** Strings en+id.

**Gate Fase 5:** `assembleDebug`; manual: toggle persist; baris izin menampilkan status & membuka Settings OS.

---

## Fase 6 — Focus Setup: kartu izin + refresh onResume

- [ ] **6.1** `FocusViewModel`:
  - expose `blockingReady: Boolean` (flag && toggle && service aktif) di `FocusUiState`.
  - `refreshBlockingStatus()` dipanggil dari `onResume`.
- [ ] **6.2** `FocusScreen`:
  - saat mode Strict/Custom && `!blockingReady` → kartu penjelasan + tombol "Aktifkan di Settings"
    (callback `onOpenAccessibilitySettings` → `AccessibilityStatus.open`).
  - ringkasan "n app(s) akan dijeda" tetap (Opsi 2).
  - tambah `LifecycleEventObserver(ON_RESUME)` → `viewModel.refreshBlockingStatus()`.
- [ ] **6.3** `AnchorNavHost`: teruskan callback buka Settings izin (atau langsung panggil util).
- [ ] **6.4** Strings en+id.

**Gate Fase 6:** `assembleDebug`; unit test ViewModel untuk `blockingReady` (bila feasible).

---

## Fase 7 — Verifikasi menyeluruh

- [ ] **7.1** `.\gradlew.bat :app:testDebugUnitTest` → hijau.
- [ ] **7.2** `.\gradlew.bat :app:connectedDebugAndroidTest` (emulator) → `MigrationTest` v8→v9 hijau.
- [ ] **7.3** `.\gradlew.bat :app:assembleDebug`.
- [ ] **7.4 HP TECNO CN7c:**
  - Aktifkan izin Accessibility → pilih Strict dengan app terblokir → buka app itu → overlay muncul.
  - Emergency exit saat overlay tampil → blocking langsung berhenti.
  - Cabut izin saat sesi jalan → kembali ke app → notice fallback (Gentle).
  - Flag/toggle off → sesi Gentle, tanpa overlay.
- [ ] **7.5 Emulator:** idem untuk regresi lintas-API.
- [ ] **7.6** Update `14-hardening-report.md` §11 (Strict/Custom tidak lagi no-op).

---

## Risiko Eksekusi & Mitigasi

| Risiko | Mitigasi |
|---|---|
| `@Provides` dua implementasi `BlockingStrategy` bentrok binding | Hapus `@Binds` lama saat menambah `@Provides` |
| Hilt di AccessibilityService | `@AndroidEntryPoint` + entry point; verifikasi `assembleDebug` |
| Overlay spam (event berturut-turut) | `singleTop` + debounce/flag di service |
| OEM beda perilaku | fallback `onResume` + `runCatching` di semua startActivity |
| `MigrationTest` butuh skema 9.json | build sekali untuk generate skema sebelum test |

---

## Definition of Done

1. Ketiga gerbang bekerja; salah satu mati → Gentle + notice (FR-F8).
2. Overlay muncul untuk app terblokir; bukan paksa; bisa ditutup.
3. Emergency exit selalu menghentikan blocking (FR-F5).
4. Semua test unit + migrasi hijau; build debug sukses.
5. Terverifikasi di HP TECNO CN7c + emulator.
6. PRD v0.4 & spec & hardening report konsisten.
