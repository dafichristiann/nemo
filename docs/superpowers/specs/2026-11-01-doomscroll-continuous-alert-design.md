# Spesifikasi Teknis: Peringatan Doomscroll / Batas Sesi Medsos Kontinu

> **Status:** Disetujui (Approved Design) · **Tanggal:** 2026-11-01  
> **Target Rilis:** Anchor (Nemo) Core Update  
> **Referensi Arsitektur:** `docs/superpowers/design/02-architecture.md`, `03-database-schema.md`, `04-intervention-engine.md`, `08-permissions.md`, `2026-10-08-focus-strict-custom-blocking-design.md`  
> **Prinsip Utama:** *Awareness > Control* (PRD §4). Intervensi reflektif tanpa pemaksaan destruktif.

---

## 1. Ringkasan & Latar Belakang

### 1.1 Masalah (Problem Statement)
Penggunaan media sosial dan platform video berformat pendek (*infinite scroll content* seperti TikTok, Instagram Reels, X/Twitter, YouTube Shorts, Reddit) dirancang dengan mekanisme *variable reward schedule* yang memicu adiksi. Pengguna sering terjebak dalam kondisi *hypnotic trance* atau **doomscrolling** terus-menerus tanpa menyadari waktu yang telah dihabiskan. 

Pada sistem Anchor/Nemo yang ada saat ini:
1. `FocusSession` memblokir aplikasi hanya bila sesi fokus sengaja dimulai oleh pengguna (*pre-committed focus*).
2. `InterventionCheckWorker` melakukan evaluasi berbasis `UsageStatsManager` secara periodik (WorkManager polling per 15 menit), sehingga tidak mampu mendeteksi sesi kontinu secara tepat waktu (*real-time granularity*) saat pengguna sedang asyik scrolling.

### 1.2 Solusi (The Proposed Feature)
Fitur **Peringatan Doomscroll Kontinu** mendeteksi durasi scrolling aktif tanpa henti pada kategori aplikasi media sosial/hiburan secara *real-time* menggunakan `NemoAccessibilityService`. Ketika sesi kontinu mencapai ambang batas (default **20 menit**):
- Sistem memunculkan **Overlay Intervensi Non-Koersif** (`DoomscrollOverlayWindow`) di atas aplikasi yang sedang dibuka.
- Maskot Nemo hadir dengan persona empatik dan peduli (*caring & mindful reminder*).
- Pengguna diberikan dua jalur aksi sadar:
  1. **"Sudahi Scroll" (Aksi Utama):** Menutup overlay dan membawa pengguna kembali ke layar Home launcher (`Intent.CATEGORY_HOME`), memutus loop dopamin secara lembut tanpa mematikan paksa (*force stop*) aplikasi.
  2. **"Tambah 5 Menit" (Snooze):** Menutup overlay dan memberikan perpanjangan waktu 5 menit sebelum intervensi berikutnya dievaluasi kembali.

### 1.3 Nilai & Batasan Filosofis
- **Bukan Hard Blocking:** Selaras dengan prinsip *Awareness > Control*, intervensi ini tidak mengunci perangkat secara otoriter. Intervensi berfungsi sebagai "cermin kesadaran" agar pengguna mengambil keputusan secara sadar (*mindful friction*).
- **Zero Content Snooping (Privasi Absolut):** Aksesibilitas hanya membaca *package name* dari transisi window/event dan tidak pernah membaca teks, postingan, pesan, gambar, atau data personal layar (`canRetrieveWindowContent = false`).
- **Battery-Friendly:** Berjalan murni berbasis event transisi window dan timer in-memory tanpa background polling loop yang memboroskan baterai.

---

## 2. Arsitektur & Model Data

### 2.1 Perubahan Skema Basis Data (Room)
Pengaturan fitur disimpan secara persisten pada tabel tunggal `app_settings`. Database `NemoDatabase` dinaikkan dari versi **13 ke 14**.

#### Perubahan `AppSettingsEntity.kt`
```kotlin
package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val trackingEnabled: Boolean = false,
    val aiEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val usageRetentionDays: Int = DEFAULT_RETENTION_DAYS,
    val defaultFocusMode: String = "GENTLE",
    val dailyInterventionQuota: Int = 3,
    val cooldownHours: Int = 2,
    val onboardingCompleted: Boolean = false,
    val locale: String = "en",
    val blockedAppsJson: String? = null,
    val focusBlockingEnabled: Boolean = false,
    val nickname: String? = null,
    val focusArea: String = "COLLEGE",
    val defaultFocusDurationMin: Int = DEFAULT_FOCUS_DURATION_MIN,
    
    // Field baru: Fitur Peringatan Doomscroll
    val doomscrollAlertEnabled: Boolean = false,
    val doomscrollThresholdMinutes: Int = DEFAULT_DOOMSCROLL_THRESHOLD_MIN,
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_RETENTION_DAYS = 30
        const val DEFAULT_FOCUS_DURATION_MIN = 25
        const val DEFAULT_DOOMSCROLL_THRESHOLD_MIN = 20
    }
}
```

#### Migrasi Room `MIGRATION_13_14` pada `NemoDatabase.kt`
```kotlin
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE app_settings ADD COLUMN doomscrollAlertEnabled INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE app_settings ADD COLUMN doomscrollThresholdMinutes INTEGER NOT NULL DEFAULT 20"
        )
    }
}
```

#### Model Domain `AppSettings.kt`
```kotlin
package com.anchor.app.core.domain.model

@androidx.compose.runtime.Immutable
data class AppSettings(
    val trackingEnabled: Boolean = false,
    val aiEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val usageRetentionDays: Int = 30,
    val onboardingCompleted: Boolean = false,
    val locale: String = "en",
    val blockedApps: Set<String> = emptySet(),
    val focusBlockingEnabled: Boolean = false,
    val nickname: String = "",
    val focusArea: FocusArea = FocusArea.COLLEGE,
    val defaultFocusDurationMin: Int = DEFAULT_FOCUS_DURATION_MIN,
    
    // Domain properties baru
    val doomscrollAlertEnabled: Boolean = false,
    val doomscrollThresholdMinutes: Int = DEFAULT_DOOMSCROLL_THRESHOLD_MIN,
) {
    companion object {
        const val MIN_RETENTION_DAYS = 14
        const val MAX_RETENTION_DAYS = 30
        val FOCUS_DURATION_OPTIONS_MIN = listOf(25, 45, 60)
        const val DEFAULT_FOCUS_DURATION_MIN = 25
        const val MAX_NICKNAME_LENGTH = 24
        
        // Pilihan durasi doomscroll yang didukung
        val DOOMSCROLL_THRESHOLD_OPTIONS_MIN = listOf(10, 15, 20, 30, 45)
        const val DEFAULT_DOOMSCROLL_THRESHOLD_MIN = 20
    }
}
```

### 2.2 Status In-Memory: `DoomscrollState`
Sama halnya dengan `BlockingState` pada pemblokiran fokus, pemantauan sesi scrolling aktif bersifat *ephemeral in-memory*. Menyimpan hitungan detik berjalan ke Room akan memboroskan disk I/O dan daya baterai. State live dipelihara oleh singleton `DoomscrollState` yang diakses bersama oleh `NemoAccessibilityService` dan modul terkait.

```kotlin
package com.anchor.app.platform.doomscroll

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot kondisi pemantauan doomscroll aktif.
 */
data class DoomscrollSnapshot(
    val activePackage: String? = null,
    val sessionStartElapsedRealtimeMs: Long = 0L,
    val lastActiveElapsedRealtimeMs: Long = 0L,
    val isPaused: Boolean = false,
    val isOverlayShowing: Boolean = false,
    val accumulatedDurationMs: Long = 0L,
    val snoozeCount: Int = 0,
)

@Singleton
class DoomscrollState @Inject constructor() {

    private val _state = MutableStateFlow(DoomscrollSnapshot())
    val state: StateFlow<DoomscrollSnapshot> = _state.asStateFlow()

    fun snapshot(): DoomscrollSnapshot = _state.value

    fun startSession(packageName: String, nowMs: Long) {
        _state.value = DoomscrollSnapshot(
            activePackage = packageName,
            sessionStartElapsedRealtimeMs = nowMs,
            lastActiveElapsedRealtimeMs = nowMs,
            isPaused = false,
            isOverlayShowing = false,
            accumulatedDurationMs = 0L,
            snoozeCount = 0,
        )
    }

    fun updateActivity(nowMs: Long) {
        val current = _state.value
        if (current.activePackage != null && !current.isPaused) {
            _state.value = current.copy(lastActiveElapsedRealtimeMs = nowMs)
        }
    }

    fun pauseSession(nowMs: Long) {
        val current = _state.value
        if (current.activePackage != null && !current.isPaused) {
            val sessionDelta = nowMs - current.lastActiveElapsedRealtimeMs
            _state.value = current.copy(
                isPaused = true,
                accumulatedDurationMs = current.accumulatedDurationMs + maxOf(0L, sessionDelta)
            )
        }
    }

    fun resumeSession(nowMs: Long) {
        val current = _state.value
        if (current.activePackage != null && current.isPaused) {
            _state.value = current.copy(
                isPaused = false,
                lastActiveElapsedRealtimeMs = nowMs
            )
        }
    }

    fun setOverlayShowing(showing: Boolean) {
        _state.value = _state.value.copy(isOverlayShowing = showing)
    }

    fun applySnooze(snoozeDurationMs: Long, nowMs: Long) {
        val current = _state.value
        _state.value = current.copy(
            isOverlayShowing = false,
            lastActiveElapsedRealtimeMs = nowMs,
            // Menggeser waktu akumulasi mundur sebesar durasi snooze untuk memperpanjang ambang batas
            sessionStartElapsedRealtimeMs = current.sessionStartElapsedRealtimeMs + snoozeDurationMs,
            snoozeCount = current.snoozeCount + 1
        )
    }

    fun resetSession() {
        _state.value = DoomscrollSnapshot()
    }
}
```

---

## 3. Logika Pemantauan & Siklus Hidup (Lifecycle)

### 3.1 Daftar Paket Media Sosial Terpantau
Penentuan aplikasi yang termasuk kategori *doomscroll-prone* memanfaatkan `AppCategories` serta daftar paket populer yang dikonfigurasi secara eksplisit:

```kotlin
object DoomscrollTargets {
    val SOCIAL_PACKAGES = setOf(
        "com.zhiliaoapp.musically",       // TikTok
        "com.ss.android.ugc.trill",       // TikTok Lite / Global
        "com.instagram.android",          // Instagram
        "com.facebook.katana",            // Facebook
        "com.facebook.lite",              // Facebook Lite
        "com.twitter.android",            // X / Twitter
        "com.x.android",                  // X
        "com.reddit.frontpage",           // Reddit
        "com.google.android.youtube",     // YouTube (Shorts & Feed)
        "com.snapchat.android",           // Snapchat
        "com.pinterest",                  // Pinterest
        "tv.twitch.android.app"           // Twitch
    )

    fun isTarget(packageName: String): Boolean = packageName in SOCIAL_PACKAGES
}
```

### 3.2 Alur Pemantauan di `NemoAccessibilityService`

`NemoAccessibilityService` memperluas penanganan event untuk mencakup deteksi perpindahan window dan interaksi scroll:

```
[Accessibility Event Received]
           │
           ├─ Apakah pkg == anchor atau System Immune? ── Ya ──> Abaikan
           │
           ├─ Mode Pemblokiran Fokus Aktif? ── Ya ──> Tangani Focus Blocking (Prioritas 1)
           │
           └─ Fitur Doomscroll Alert Aktif di AppSettings?
                      │
                     Tidak ──> Reset Tracker Sesi
                      │
                      Ya (Prioritas 2)
                      v
           ┌────────────────────────────────────────┐
           │ Apakah pkg merupakan target Medsos?   │
           └──────────────────┬─────────────────────┘
                     Ya       │       Bukan Medsos / Home Screen
                     │        │
                     v        v
        ┌──────────────────┐ ┌──────────────────────────────────────┐
        │ Sama dgn paket   │ │ Mulai Grace Period Debounce (10 dtk) │
        │ sesi sekarang?   │ └──────────────────┬───────────────────┘
        └───┬──────────┬───┘                    │
           Ya          Tidak                    ├─ Kembali ke medsos < 10 dtk ──> Batalkan Reset
            │            │                      │
            │            v                      └─ Tetap di luar > 10 dtk ──> Reset Sesi Menjadi 0
            │    [Medsos Berbeda]
            │    Reset sesi lama,
            │    Mulai sesi paket baru
            v
     [Update Heartbeat]
     Jadwalkan/Periksa Elapsed Time
     terhadap doomscrollThresholdMinutes
            │
            ├─ Durasi Belum Tercapai ──> Lanjutkan pemantauan
            │
            └─ Durasi Tercapai (>= 20 Menit)
                      v
          Tampilkan DoomscrollOverlayWindow
```

### 3.3 Penanganan Debounce 10 Detik
Untuk mencegah trik perpindahan aplikasi sejenak (*switching loophole* — misal membuka aplikasi kalkulator atau membalas chat notifikasi selama 3 detik lalu kembali scrolling), sistem memberlakukan toleransi *grace period*:
1. Saat pengguna beralih dari aplikasi medsos ke aplikasi non-medsos atau launcher:
   - Sesi **tidak langsung dihapus**, melainkan status ditandai `isPaused = true`.
   - Sebuah Coroutine Job pending cancellation disiapkan selama **10.000 milidetik (10 detik)**.
2. Jika dalam rentang waktu < 10 detik pengguna kembali ke aplikasi medsos yang sama:
   - Pending cancellation dibatalkan.
   - Sesi di-resume (`resumeSession()`), penghitungan waktu aktif dilanjutkan tanpa kehilangan akumulasi menit sebelumnya.
3. Jika pengguna tetap berada di luar aplikasi target selama > 10 detik:
   - Job cancellation tereksekusi.
   - Sesi dibersihkan penuh (`resetSession()`).

### 3.4 Penanganan Siklus Hidup Layar Mati (Screen Off / Screen On)
Penggunaan `SystemClock.elapsedRealtime()` menjamin stabilitas terhadap manipulasi jam manual pengguna. Untuk menangani saat perangkat terkunci atau layar mati:
- `NemoAccessibilityService` mendaftarkan `BroadcastReceiver` internal secara dinamis untuk filter:
  - `Intent.ACTION_SCREEN_OFF`
  - `Intent.ACTION_USER_PRESENT` / `Intent.ACTION_SCREEN_ON`
- **Saat `ACTION_SCREEN_OFF` diterima:**
  - Timer sesi langsung di-pause.
  - Jika layar mati berlangsung lebih dari 10 detik, saat layar menyala kembali sesi otomatis di-reset dari awal (pengguna telah meletakkan ponsel dan menyudahi sesi secara natural).
- **Saat `ACTION_USER_PRESENT` diterima:**
  - Jika jendela foreground masih berada pada target medsos dan jeda layar mati < 10 detik, sesi dilanjutkan. Jika lebih lama, sesi dimulai segar.

---

## 4. Desain Intervensi Overlay (`DoomscrollOverlayWindow`)

### 4.1 Mekanisme Window Manager
Overlay diimplementasikan menggunakan arsitektur `WindowManager` berbasis `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`. 
- **Keunggulan:** Tidak memerlukan izin tambahan `SYSTEM_ALERT_WINDOW` (*Draw over other apps*) karena layanan aksesibilitas yang telah disetujui pengguna sudah memiliki hak istimewa menggambar overlay sistem di atas aplikasi foreground tanpa terpengaruh pembatasan *background activity start* pada Android 10+.
- **Tingkat Transparansi:** Scrim gelap dengan nilai alpha `#B3000000` (70% opacity black) memberikan fokus penuh pada pesan tanpa merusak rasa aman.

### 4.2 Tata Letak & Hirarki Visual (Card Specification)
Desain visual mengikuti standar token **Nemo Design Kit**:
- **Wadah Kartu (Card Container):**
  - Background: `nemo_surface` (#FFFFFF pada mode terang, #1A1A1A pada mode gelap).
  - Corner Radius: `24dp` (Pill/Rounded corner khas Nemo).
  - Padding: `24dp` horizontal dan vertical.
  - Lebar Maksimum: `320dp` (tampil proporsional di tengah layar ponsel).
- **Maskot Nemo:**
  - Drawable: `R.drawable.nemo_mascot_thinking` (ekspresi merenung, penuh perhatian dan hangat; bukan ekspresi marah/menghakimi).
  - Dimensi: `80dp x 80dp`, margin bawah `16dp`.
- **Tipografi & Konten:**
  - **Judul:** `"Sudah 20 Menit Berselancar..."`  
    Teks tebal (Bold, 20sp), warna `nemo_ink`.
  - **Isi Pesan (Body):**  
    `"Waktu bergulir cepat tanpa terasa. Istirahatkan matamu sejenak dan tarik napas dalam. Apakah kamu ingin menyudahi sesi ini atau melanjutkan?"`  
    Teks reguler (14sp), warna `nemo_ink_secondary`, perataan tengah (*center alignment*).
  - **Label Durasi:** Badge kecil bertuliskan `"Sesi Aktif: [X] Menit"` dengan latar belakang `nemo_fog` dan teks `nemo_mute` (12sp Mono).
- **Tombol Aksi:**
  1. **Tombol Utama: "Sudahi Scroll" (Primary CTA)**
     - Tampilan: Latar belakang solid `nemo_ink`, teks tebal warna `nemo_on_ink` (kontras tinggi), tinggi `48dp`, corner radius `24dp`.
     - Perilaku: 
       1. Menutup overlay window (`hide()`).
       2. Mereset sesi doomscroll (`resetSession()`).
       3. Mengirimkan intent Home launcher:
          ```kotlin
          val homeIntent = Intent(Intent.ACTION_MAIN).apply {
              addCategory(Intent.CATEGORY_HOME)
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(homeIntent)
          ```
       4. Hasil: Pengguna seketika terbebas dari aplikasi medsos dan mendarat aman di Home screen perangkat.
  2. **Tombol Sekunder: "Tambah 5 Menit" (Snooze CTA)**
     - Tampilan: Latar belakang `nemo_fog` (abu-abu tenang/subtle), teks warna `nemo_ink`, tinggi `44dp`, corner radius `22dp`.
     - Perilaku:
       1. Menutup overlay window (`hide()`).
       2. Memicu `DoomscrollState.applySnooze(5 * 60 * 1000L, nowMs)`.
       3. Menjadwalkan pengingat berikutnya tepat 5 menit ke depan jika pengguna masih scrolling tanpa jeda.

### 4.3 Navigasi Tombol Fisik (Back Key Dispatch)
Mengantisipasi saat pengguna menekan tombol *Back* perangkat ketika overlay muncul:
```kotlin
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
        // Menekan tombol kembali dianggap sebagai gestur ingin keluar dari distraksi
        hide()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(homeIntent) }
        return true
    }
    return super.dispatchKeyEvent(event)
}
```

---

## 5. Konfigurasi Pengaturan di `SettingsScreen` & `SettingsViewModel`

### 5.1 Penempatan Antarmuka (Information Architecture)
Opsi konfigurasi diletakkan pada grup **"Wellbeing & Mindful Limits"** di dalam `SettingsScreen.kt`, berdampingan dengan pengaturan *Focus Blocking* dan *App Categories*.

Komponen antarmuka terdiri dari:
1. **Master Toggle Switch:**
   - Label: `"Peringatan Doomscroll Kontinu"`
   - Deskripsi: `"Munculkan pengingat ramah dari Nemo saat scrolling media sosial tanpa henti."`
   - Syarat Aksesibilitas: Apabila `AccessibilityStatus.isEnabled(context) == false`, menampilkan banner peringatan kecil: *"Membutuhkan izin Aksesibilitas agar dapat mendeteksi durasi scrolling."* dengan tombol langsung membuka halaman Settings Aksesibilitas sistem.
2. **Selector Durasi Ambang Batas (Threshold Selector):**
   - Hanya tampil aktif jika Master Toggle bernilai `true`.
   - Pilihan interaktif berbentuk *Segmented Control* atau *Pill Chips*:
     - **10 Menit**
     - **15 Menit**
     - **20 Menit (Disarankan / Default)**
     - **30 Menit**
     - **45 Menit**

### 5.2 Implementasi di `SettingsViewModel.kt`
```kotlin
// Penambahan pada SettingsViewModel
fun setDoomscrollAlertEnabled(enabled: Boolean) {
    viewModelScope.launch(dispatchers.io) {
        settingsRepository.setDoomscrollAlertEnabled(enabled)
    }
}

fun setDoomscrollThresholdMinutes(minutes: Int) {
    require(minutes in AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN) {
        "Durasi tidak valid: $minutes menit"
    }
    viewModelScope.launch(dispatchers.io) {
        settingsRepository.setDoomscrollThresholdMinutes(minutes)
    }
}
```

### 5.3 Integrasi Repository & DAO
- **`AppSettingsDao.kt`:**
  ```kotlin
  @Query("UPDATE app_settings SET doomscrollAlertEnabled = :enabled WHERE id = 1")
  suspend fun updateDoomscrollAlertEnabled(enabled: Boolean)

  @Query("UPDATE app_settings SET doomscrollThresholdMinutes = :minutes WHERE id = 1")
  suspend fun updateDoomscrollThresholdMinutes(minutes: Int)
  ```
- **`SettingsRepository.kt` & `SettingsRepositoryImpl.kt`:**
  Menyediakan method abstrak dan implementasi pemanggilan DAO pada coroutine IO dispatcher.

---

## 6. Strategi Pengujian (Testing Strategy)

Untuk memastikan keandalan, akurasi penghitungan waktu, serta stabilitas migrasi basis data, pengujian dirancang ke dalam tiga lapisan:

### 6.1 Unit Tests

#### 1. `DoomscrollTrackerTest.kt`
Menguji logika status in-memory dan kalkulasi waktu aktif tanpa dependensi Android Framework:
- **Test Case 1 (`test_threshold_triggers_alert`):**
  - Simulasi awal sesi pada paket `"com.instagram.android"` pada `t = 0`.
  - Simulasi waktu maju hingga `t = 20 menit (1.200.000 ms)`.
  - Verifikasi bahwa tracker mengidentifikasi ambang batas telah terlampaui dan memicu status overlay visible.
- **Test Case 2 (`test_debounce_grace_period_success`):**
  - Sesi aktif di Instagram selama 15 menit.
  - Pengguna berpindah ke WhatsApp (non-target) pada `t = 15m 00s`. Status menjadi paused.
  - Pengguna kembali ke Instagram pada `t = 15m 08s` (8 detik kemudian, < 10 detik).
  - Verifikasi sesi tidak di-reset dan total waktu aktif tetap melanjutkan dari menit ke-15.
- **Test Case 3 (`test_exit_app_resets_session`):**
  - Sesi aktif di TikTok selama 12 menit.
  - Pengguna beralih ke Home screen selama 15 detik (> 10 detik grace period).
  - Pengguna membuka TikTok kembali.
  - Verifikasi sesi lama telah di-reset dan timer baru dimulai dari 0 menit.
- **Test Case 4 (`test_snooze_extends_by_5_minutes`):**
  - Ambang batas 20 menit tercapai. Pengguna menekan "Tambah 5 Menit".
  - Verifikasi overlay ditutup.
  - Simulasi waktu maju 4 menit -> tidak ada alert baru.
  - Simulasi waktu maju 5 menit -> alert intervensi kembali dipicu.
- **Test Case 5 (`test_screen_off_pauses_and_resets`):**
  - Layar mati selama 2 menit.
  - Verifikasi sesi ter-reset saat layar dihidupkan kembali.

#### 2. `SettingsRepositoryTest.kt`
- Menguji penyimpanan dan pembaruan `doomscrollAlertEnabled` dan `doomscrollThresholdMinutes`.
- Memastikan nilai default terpetakan dengan benar: `enabled = false`, `threshold = 20`.

### 6.2 Migration Test (`Migration13To14Test.kt`)
Menggunakan `androidx.room.testing.MigrationTestHelper` untuk memverifikasi bahwa perubahan skema dari versi 13 ke 14 berjalan aman tanpa data loss:
```kotlin
@RunWith(AndroidJUnit4::class)
class Migration13To14Test {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NemoDatabase::class.java
    )

    @Test
    fun migrate13To14_preservesExistingSettingsAndAddsDoomscrollColumns() {
        // 1. Buat database versi 13 dan isi data awal
        helper.createDatabase("nemo-test.db", 13).apply {
            execSQL(
                """
                INSERT INTO app_settings (id, trackingEnabled, aiEnabled, notificationsEnabled, 
                    usageRetentionDays, defaultFocusMode, dailyInterventionQuota, cooldownHours, 
                    onboardingCompleted, locale, focusBlockingEnabled, nickname, focusArea, 
                    defaultFocusDurationMin)
                VALUES (1, 1, 0, 1, 30, 'GENTLE', 3, 2, 1, 'en', 1, 'Budi', 'COLLEGE', 25)
                """.trimIndent()
            )
            close()
        }

        // 2. Jalankan migrasi ke versi 14
        val db = helper.runMigrationsAndValidate("nemo-test.db", 14, true, NemoDatabase.MIGRATION_13_14)

        // 3. Verifikasi kolom baru dan integritas data lama
        val cursor = db.query("SELECT * FROM app_settings WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        
        // Data lama terjaga
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("trackingEnabled")))
        assertEquals("Budi", cursor.getString(cursor.getColumnIndexOrThrow("nickname")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("focusBlockingEnabled")))
        
        // Kolom baru terisi nilai default yang benar
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("doomscrollAlertEnabled")))
        assertEquals(20, cursor.getInt(cursor.getColumnIndexOrThrow("doomscrollThresholdMinutes")))
        cursor.close()
    }
}
```

### 6.3 Pengujian Verifikasi Manual (End-to-End Checklist)

| No | Skenario Uji | Prosedur Pengujian | Hasil yang Diharapkan |
|:--:|:---|:---|:---|
| 1 | Aktivasi Pengaturan | Aktifkan toggle di Settings, pilih durasi 10 menit. | Pilihan tersimpan, Flow Room mengalirkan nilai baru ke ViewModel. |
| 2 | Skenario Normal Doomscroll | Buka Instagram/TikTok dan gunakan terus menerus selama batas waktu terpilih. | Tepat saat batas waktu tercapai, overlay Nemo muncul di layar secara lembut. |
| 3 | Eksekusi Tombol "Sudahi Scroll" | Ketuk tombol "Sudahi Scroll" pada overlay. | Overlay tertutup seketika dan pengguna dipindahkan ke Home launcher. Timer di-reset. |
| 4 | Eksekusi Tombol "Tambah 5 Menit" | Ketuk tombol "Tambah 5 Menit" pada overlay. | Overlay tertutup, aplikasi tetap terbuka. Pengingat tidak muncul selama 5 menit berikutnya. |
| 5 | Tes Debounce Aplikasi Lain (< 10 Detik) | Buka Instagram 15 menit, beralih ke Telegram selama 5 detik, lalu buka Instagram lagi. | Waktu akumulasi tidak hilang; timer melanjutkan dari 15 menit. |
| 6 | Tes Reset Keluar Aplikasi (> 10 Detik) | Buka TikTok 15 menit, tekan tombol Home dan diamkan 15 detik, lalu buka TikTok lagi. | Timer sesi ter-reset ke 0 menit. |
| 7 | Interaksi Kunci Layar (Screen Off) | Kunci layar saat sedang membuka Twitter, diamkan 1 menit, nyalakan kembali. | Sesi ter-reset dan overlay tidak muncul secara salah. |
| 8 | Interaksi Bersama Focus Session | Mulai Sesi Fokus Strict dengan Instagram terblokir. Buka Instagram. | Yang muncul adalah `BlockOverlayWindow` (Focus Block), bukan Doomscroll Alert. |

---

## 7. Rangkuman File yang Ditambahkan & Dimodifikasi

| Komponen / File | Peran & Perubahan |
|---|---|
| `docs/superpowers/specs/2026-11-01-doomscroll-continuous-alert-design.md` | Dokumen spesifikasi teknis lengkap fitur doomscroll alert ini. |
| `app/src/main/java/com/anchor/app/core/data/entity/AppSettingsEntity.kt` | Penambahan kolom `doomscrollAlertEnabled` dan `doomscrollThresholdMinutes`. |
| `app/src/main/java/com/anchor/app/core/domain/model/AppSettings.kt` | Penambahan field domain dan konstanta opsi durasi (10, 15, 20, 30, 45 menit). |
| `app/src/main/java/com/anchor/app/core/data/NemoDatabase.kt` | Peningkatan `version = 14`, implementasi `MIGRATION_13_14`, dan registrasi di `ALL_MIGRATIONS`. |
| `app/src/main/java/com/anchor/app/core/data/dao/AppSettingsDao.kt` | Penambahan query updater untuk kedua field baru. |
| `app/src/main/java/com/anchor/app/core/domain/repository/SettingsRepository.kt` | Penambahan kontrak `setDoomscrollAlertEnabled` dan `setDoomscrollThresholdMinutes`. |
| `app/src/main/java/com/anchor/app/core/data/repository/SettingsRepositoryImpl.kt` | Implementasi pembaruan data dan mapping entitas-ke-domain. |
| `app/src/main/java/com/anchor/app/platform/doomscroll/DoomscrollState.kt` | Manajemen in-memory live state untuk melacak sesi aktif dan snooze. |
| `app/src/main/java/com/anchor/app/platform/doomscroll/DoomscrollTargets.kt` | Daftar paket aplikasi media sosial & video pendek yang menjadi target pemantauan. |
| `app/src/main/java/com/anchor/app/platform/doomscroll/DoomscrollOverlayWindow.kt` | View overlay sistem bertipe `TYPE_ACCESSIBILITY_OVERLAY` menampilkan maskot Nemo dan tombol aksi. |
| `app/src/main/java/com/anchor/app/platform/accessibility/NemoAccessibilityService.kt` | Integrasi pemantauan event `TYPE_WINDOW_STATE_CHANGED`, debounce 10 detik, dan registrasi Screen Receiver. |
| `app/src/main/java/com/anchor/app/feature/settings/SettingsViewModel.kt` | Expose action setter untuk toggle switch dan durasi threshold. |
| `app/src/main/java/com/anchor/app/feature/settings/SettingsScreen.kt` | Penambahan UI card "Peringatan Doomscroll Kontinu" beserta selector durasi. |
| `app/src/test/java/com/anchor/app/platform/doomscroll/DoomscrollTrackerTest.kt` | Unit test untuk timer, debounce 10 detik, snooze, dan screen state. |
| `app/src/androidTest/java/com/anchor/app/core/data/migration/Migration13To14Test.kt` | Instrument test validasi migrasi Room dari versi 13 ke 14. |
