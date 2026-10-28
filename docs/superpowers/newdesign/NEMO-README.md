# Nemo Design Kit

Paket handoff desain Nemo: tenang, kalem, seperti angin. Hitam–putih dengan abu tipis.
Tema yang direkomendasikan adalah **Night**, dan **Day** tersedia sebagai pasangannya.

## Isi paket

| Folder | Isi |
|---|---|
| `preview/` | Mockup HTML yang bisa dibuka di browser: `nemo-night-recommended.html` (utama), `nemo-day-airy.html`, `nemo-day-detailed.html` |
| `docs/design-spec.md` | Spesifikasi lengkap: prinsip, warna, tipografi, komponen, 19 layar, gerak, aksesibilitas |
| `tokens/` | `nemo-tokens.json` (untuk Figma Tokens/Style Dictionary) dan `nemo-tokens.css` |
| `compose/` | Tema Jetpack Compose (`theme/`) dan komponen siap pakai (`components/`) |
| `android/` | `colors.xml` day + night, adaptive icon XML |
| `assets/logo/` | Logo transparan hitam & putih (1024/512/256/128) + referensi asli |
| `assets/android-launcher/` | Ikon launcher semua densitas (adaptive foreground, monochrome, legacy, round), ikon notifikasi, Play Store 512 |
| `assets/icons/` | 9 ikon SVG gaya garis (1.6 stroke) |
| `assets/fonts/` | Manrope & Fraunces (variable TTF, lisensi OFL) |

## Cara memasang di project Android (`com.anchor.app`)

1. **Font:** salin `assets/fonts/*.ttf` ke `app/src/main/res/font/` (nama sudah sesuai: `manrope_variable.ttf`, `fraunces_variable.ttf`).
2. **Tema & komponen:** salin `compose/theme/` ke `com/anchor/app/ui/theme/` dan `compose/components/` ke `com/anchor/app/ui/components/`.
3. **Warna XML:** gabungkan `android/values/colors.xml` ke `res/values/` dan `android/values-night/colors.xml` ke `res/values-night/`.
4. **Ikon aplikasi:** salin folder `mipmap-*` dari `assets/android-launcher/` ke `res/`, lalu `android/mipmap-anydpi-v26/*.xml` ke `res/mipmap-anydpi-v26/`. Pastikan manifest memakai `@mipmap/ic_launcher` dan `@mipmap/ic_launcher_round`.
5. **Ikon notifikasi:** pakai `ic_stat_nemo.png` (putih, transparan) sebagai small icon notifikasi.
6. **Ikon UI:** di Android Studio pilih *New → Vector Asset → Local file* untuk mengimpor SVG di `assets/icons/`.
7. **Pasang tema:** bungkus aplikasi dengan `NemoTheme { ... }`, lalu ambil warna lewat `Nemo.colors` dan spasi lewat `Nemo.spacing`.

Kebutuhan: Compose UI 1.5+, Material3 1.2+ (untuk `HorizontalDivider`), minSdk 26 (variable font).
Kode ini belum saya compile di project Anda. Jalankan build dulu dan sesuaikan kalau ada selisih versi library.

## Yang perlu Anda putuskan/lengkapi
- **Layar yang belum dimockup:** Future View versi Day, App Categories, dan notifikasi Day. Spesifikasinya sudah ada di `docs/design-spec.md`.
- **Animasi Splash:** Splash memakai urutan gambar animasi maskot Compose (`Neutral` 1,5s → `Happy` 2s) tanpa memerlukan berkas video MP4 (bebas dari codec video). Aset maskot mencakup 12 ekspresi anak laki-laki chibi bergaya tinta hitam.
- **Lisensi font:** Manrope dan Fraunces berlisensi SIL OFL, jadi aman dibundel di aplikasi.
