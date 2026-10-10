# Design System — Anchor (Personal Focus Assistant)

> **⚠️ SUPERSEDED (2026-10-10):** The app now uses the **NEMO** design system (Fraunces +
> Manrope, Day/Night following the system, flat/pill shapes). See
> `../newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md` (visual source of truth) and
> `../specs/2026-10-10-nemo-design-migration-design.md` (migration spec). This Anchor
> document is kept for historical reference only.

> **Status:** Derived from `gamabaran.pen` (aktual) + PRD v0.2 · **Version:** 0.2
> **Tanggal:** 2026-10-07
> **Dokumen terkait:** `01-prd.md` §4, `07-screens.md`, `gamabaran.pen`
> **Sumber token:** diekstrak **langsung dari `gamabaran.pen`** (bukan dari `build_pen.js`),
> agar dokumen ini mencerminkan mockup yang **sekarang** ada di kanvas.
>
> **Cara berpikir:** dokumen ini mendokumentasikan **bahasa visual** Anchor agar
> implementasi Compose konsisten dengan mockup. Nada desain harus mencerminkan
> prinsip PRD: **calm · supportive · concise · non-judgmental**.
>
> **Identitas visual (dari banner mockup):** *Black & White Minimalist · 3D Depth System*.
> Monokrom penuh + kedalaman lembut (layered base/front). Tidak ada warna aksen di
> mockup kecuali satu varian danger `#CC3333` untuk keadaan kritis.

---

## 1. Prinsip Desain

1. **Tenang, bukan ramai.** Banyak ruang putih, sedikit warna, tanpa gradien mencolok.
2. **Monokrom + satu aksen.** Hampir seluruh UI hitam-putih; warna hanya untuk sinyal halus.
3. **Kedalaman lembut (soft 3D).** Kartu & tombol "terangkat" via layered base + shadow tipis.
4. **Hindari menghakimi.** Tidak ada merah menyala, tidak ada label "buruk".
5. **Konsisten dengan kepadatan.** List padat namun lapang; ikon 20–28 px.
6. **Motion minim.** Transisi halus; tidak ada animasi berat (hemat performa, PRD §12).

---

## 2. Palet Warna (Monokrom)

Diekstrak dari mockup. Format `#RRGGBBAA`.

Frekuensi pemakaian di mockup ditulis dalam tanda kurung `(n×)`.

### 2.1 Inti (permukaan)

| Token | Hex | Peran |
|---|---|---|
| `ink` (primary) | `#0A0A0A` (112×) | Teks utama, tombol utama, ikon aktif |
| `surface` | `#FFFFFF` (68×) | Latar layar/kartu putih |
| `surface-sunken` | `#F8F8F8` (25×) | Latar kartu, input |
| `surface-card` | `#F9F9F9` (2×) | Front kartu permission |
| `surface-muted` | `#F5F5F5` (11×) | Avatar/track/latar lembut |
| `track` | `#EEEEEE` (3×) / `#EAEAEA` (3×) | Progress track, divider halus |
| `border` | `#E0E0E0` (23×) | Garis, divider |
| `border-soft` | `#F0F0F0` (3×) / `#E5E5E5` (3×) | Divider sangat halus, base 3D |
| `ink-alt` | `#262626` (17×) | "Depth base" tombol/latar gelap 3D |
| `ink-deep` | `#222222` (6×) | "Depth base" CTA, hero |

### 2.2 Teks (hierarki abu)

| Token | Hex | Peran |
|---|---|---|
| `text-primary` | `#0A0A0A` (112×) | Judul, nilai penting |
| `text-strong-alt` | `#333333` (3×) / `#444444` | Teks gelap sekunder |
| `text-secondary` | `#555555` (20×) | Subjudul |
| `text-secondary-alt` | `#666666` (6×) | Deskripsi paragraf |
| `text-muted` | `#777777` (28×) | Meta, caption utama |
| `text-muted-alt` | `#888888` (25×) | Meta, non-aktif, label step |
| `text-faint-alt` | `#999999` (4×) | Tag kecil |
| `text-faint` | `#AAAAAA` (27×) | Placeholder, label kecil |
| `text-faint-soft` | `#BBBBBB` (1×) / `#CCCCCC` (8×) | Placeholder paling redup |

### 2.3 Aksen (dipakai sangat hemat)

| Token | Hex | Peran | Catatan |
|---|---|---|---|
| `accent-danger` | `#CC3333` | **Sangat jarang** — hanya indikator kritis (mis. delete) | Bukan untuk menghakimi tugas overdue |
| `accent-danger-soft` | `#CC333315` | Latar lembut untuk area danger | |
| `danger-surface` | `#FFF5F5` | Latar kartu "Delete" | |

> ⚠️ **Aturan penting:** tugas overdue & drift **BUKAN** merah menyala. Gunakan tanda
> halus (label abu, ikon netral) — PRD §10.1 "non-blocking UI Card/Banner".

### 2.4 Lapisan Kedalaman (soft 3D)

Kartu & tombol dibuat dari **dua lapisan**: `Base` (lebih gelap, offset +y) di belakang,
`Front` (warna utama) di depan. Ini menciptakan efek terangkat tanpa gambar.

| Lapisan | Fill | Offset dari Front | Efek |
|---|---|---|---|
| `X Base` | `#E5E5E5` / `#222222` | `+0, +6..8` | tanpa/terbatas shadow |
| `X Front` | `#F9F9F9` / `#0A0A0A` | – | `shadow(0, 6–10, 16–24, #0000000f..40)` |

---

## 3. Tipografi

- **Font:** **Inter** (semua teks).
- **Ikon:** **Material Symbols Rounded**.
- **Line height:** `1.4–1.5` untuk paragraf (`lineHeight` rasio).

### 3.1 Skala (aktual dari `gamabaran.pen`)

Ukuran font yang **benar-benar ada** di file mockup (n = jumlah pemakaian).
Skala ini kontinu (bukan ramp tetap) — pantau ukuran yang sering dipakai.

| Rentang | Ukuran (weight) yang dipakai di mockup | Peran khas |
|---|---|---|
| **Dekor/banner** | `68/700` (hero judul), `50/700` (hero slogan), `36/700` (4×) | Judul hero, angka besar |
| **Display** | `32/700` (2×), `28/700` (7×), `26/700` (3×) | Headline onboarding/layar utama |
| **Title** | `24/700`, `22/700` (banner, hero), `20/700` | Judul card utama |
| **Heading** | `18/700` (3×), `16/700` (7×) / `16/600` (5×) | Judul card, judul modal |
| **Body** | `15/600` (7×), `14/600` (9×) / `14/500` (10×) / `14/400` (8×) | Nama task, judul list, paragraf |
| **Label** | `13/600` (20×) / `13/500` (11×) / `13/400` (13×) | Meta, label field, hint |
| **Caption** | `12/700` / `12/600` / `12/500` / `12/400` (27×) | Meta kecil, teks badge |
| **Micro** | `11/700`, `11/600`, `11/500` (8×), `11/400` | Tag, indikator, mini-phone |
| **Tiny (mini-phone hero)** | `10/700`, `10/500`, `9/700`, `9/500`, `8/700`, `8/600` | Detail mockup HP di hero saja |

> Ukuran `8`–`10` **hanya** muncul pada mockup HP mini di dalam hero (bukan UI app nyata).

### 3.2 Aturan

- Judul: `fontWeight 700`, `fill ink (#0A0A0A)`.
- Meta/desc: `fontWeight 400`, `fill text-muted (#777)`.
- Tag kecil: `fontWeight 500`, `fill text-faint (#999)`.
- Jangan campur >3 ukuran dalam satu card.
- Banner (papan judul) memakai `22/700` + `12/400`; hero judul `68/700`, slogan `50/700`.

---

## 4. Bentuk & Radius

Nilai radius yang **benar-benar ada** di `gamabaran.pen` (n = jumlah pemakaian):
`1×1, 2×10, 3×2, 5×1, 6×20, 8×10, 9×5, 10×19, 11×1, 12×22, 14×19, 16×28, 18×18, 20×14, 22×2, 24×5, 28×2, 32×14, 38×1, 44×2, 46×2`.

| Token | Nilai (mockup) | Dipakai untuk |
|---|---|---|
| `radius-track` | 2 | Progress track/bar (tipis) |
| `radius-hairline` | 6 | Divider tebal, bar mini |
| `radius-chip` | 8–10 | Badge kecil, bar prioritas, dot |
| `radius-field` | 12–14 | Input field, kartu kecil |
| `radius-button` | 16 | Tombol utama (28× — paling sering) |
| `radius-card` | 18 | Kartu, panel (18×) |
| `radius-card-alt` | 20 | Kartu besar (14×) |
| `radius-panel` | 22–24 | Panel/sheet |
| `radius-sheet` | 28 | Bottom sheet atas |
| `radius-screen` | 32 | Sudut frame layar device (14×) |
| `radius-lg` | 38–46 | Elemen dekoratif besar (mis. CTA hero) |

**Aturan praktis:** track=2 · chip=10 · field=12 · button=16 · card=18 · screen=32.

Sudut membulat, lembut. Hindari sudut tajam kecuali divider (`radius 1–2`).

---

## 5. Spacing & Tata Letak

- Lebar device mockup: **375 px**. Padding horizontal utama: **32 px**.
- Lebar konten kartu: **311 px** (375 − 2×32).
- Grid antar-frame pada canvas: seragam **60 px** (lihat `07-screens.md`).

| Token | Nilai |
|---|---|
| `space-1` | 4 |
| `space-2` | 8 |
| `space-3` | 12 |
| `space-4` | 16 |
| `space-5` | 20 |
| `space-6` | 24 |
| `space-8` | 32 |

- Gap antar kartu dalam list: **12–16 px**.
- Padding dalam kartu: **20–22 px** horizontal.

---

## 6. Elevasi & Shadow

Monokrom + kedalaman lembut. Shadow sangat halus (alpha rendah).

| Token | Nilai (offset, blur, warna) | Dipakai |
|---|---|---|
| `elev-card` | `(0, 6, 16, #0000000f)` | Kartu standar |
| `elev-card-raised` | `(0, 8, 20, #00000015)` | Kartu menonjol |
| `elev-button` | `(0, 10, 24, #00000040)` | Tombol utama (efek 3D) |
| `elev-screen` | `(0, 20, 50, #00000018)` | Frame layar |
| `elev-avatar` | `(0, 16, 24, #0000001f)` | Elemen dekoratif |

**Aturan:** jangan menumpuk banyak shadow. Maksimum satu shadow per elemen.

### 6.1 Stroke (garis tepi)

Stroke yang dipakai di mockup: `1` (divider/outline), `1.5` (outline aksen), `2`
(outline avatar/border 3D), `10` (cincin Timer Ring di Focus Active).

| Token | Nilai | Dipakai |
|---|---|---|
| `stroke-hairline` | 1 | Divider, outline tipis |
| `stroke-outline` | 1.5–2 | Border avatar, outline kartu terpilih |
| `stroke-ring` | 10 | Ring timer Focus (elemen fokus) |

---

## 7. Komponen

Setiap komponen di mockup dibangun dengan pola `Base`/`Front` (3D) atau kartu datar.

### 7.1 Tombol Utama (Primary / CTA)

```
┌──────────────────────────────┐
│        Get started           │   Front: #0A0A0A, radius 16, tinggi 56
└──────────────────────────────┘   Base:  #222222, offset +y 6
   Label: 16/600, fill #FFFFFF    shadow elev-button
```

- Lebar penuh konten (311 px), tinggi **56 px**.
- Ada `Base` di belakang untuk efek terangkat.
- Nada label: imperatif lembut ("Get started", "Save Task", "Continue", "Begin Focus").

### 7.2 Kartu (Card)

```
Base (#E5E5E5, offset +6) ─┐
Front (#F9F9F9, radius 18) ─┘  shadow elev-card
```

- Dipakai untuk: task item, permission item, app usage, stats, insight block.
- Varian: `selected` (border/aksen tipis `#0A0A0A`).

### 7.3 Task Item

- Priority ditandai **bar tipis kiri** (warna halus), bukan latar merah.
- Checkbox bulat `#0A0A0A` saat selesai.
- Meta abu kecil; badge prioritas di kanan (radius kecil).

### 7.4 Tab

- Baris teks; tab aktif diindikasikan garis bawah **2 px** (`Tab Indicator`), bukan pill.
- Jarak antar tab ~40 px.

### 7.5 Progress Bar / Track

- Track: `#EEEEEE`, radius 2, tinggi **3–4 px**.
- Fill: `#0A0A0A`.
- Angka persentase di kanan, `label`.

### 7.6 Badge / Tag

- Radius kecil, tinggi ~22 px, font 11.
- Nada netral (abu) — bukan warna menyala.
- Contoh: `High`, `Optional`, `Opsional`.

### 7.7 Chip Pilihan (Priority / Duration)

- Baris chip; yang terpilih = `#0A0A0A` + teks putih; lain = abu muda.

### 7.8 Field / Input

- `#F8F8F8` (atau putih dengan border), radius 12–14, tinggi ~44–50 px.
- Placeholder abu (`text-faint`).

### 7.9 Bottom Sheet (Add Task)

- Scrim gelap lembut; sheet putih radius atas 24–28.
- Handle kecil (`48×4`, `#E0E0E0`) di tengah atas.

### 7.10 Navigation Bar

- Tinggi 56, latar putih + divider tipis.
- 4 tab: **Tasks · Wellbeing · Focus · Insights**.
- Item aktif `#0A0A0A`, non-aktif `#888`.

### 7.11 FAB (Tambah Task)

- Bulat 56 px, `#0A0A0A`, ikon `+` putih.
- Posisi kanan bawah (di atas nav bar).

### 7.12 Banner Overdue (Non-blocking)

```
┌──────────────────────────────┐
│  3 tasks from yesterday      │
│  are still waiting.          │
│  [Review & Reschedule]       │
└──────────────────────────────┘
```

- Nada tenang; **tidak** merah; ada tombol sekunder.
- Muncul saat app dibuka bila ada overdue (PRD §10.1).

### 7.13 Ring Timer (Focus Active)

- Cincin `strokeWidth` tebal, latar track abu, progres `#0A0A0A`.
- Glow sangat halus di luar.
- Angka besar di tengah (mis. `19:22`), sub-label di bawah.

### 7.14 Bar Chart (Wellbeing)

- Batang monokrom (`#0A0A0A`), base shadow tipis, label hari abu.
- Sederhana; bukan dashboard ramai.

### 7.15 Icon

- Geometri: **Material Symbols Rounded**, `fill = #0A0A0A` (atau `#888` non-aktif).
- Ukuran umum: 16 (dalam chip), 20–22 (list), 24–28 (header/card), 40+ (hero/modal).

---

## 8. Ikonografi (glyph yang dipakai mockup)

| Fungsi | Glyph (Material Symbols Rounded) |
|---|---|
| Logo/brand | `anchor` |
| Wellbeing | `bar_chart` |
| Selesai | `check` |
| Tambah | `add` |
| Navigasi balik | `chevron_left` / `arrow_back` |
| Dropdown | `expand_more` / `chevron_right` |
| Panah domino | `arrow_downward` |
| Tutup | `close` |
| Fokus | `timer` / `center_focus_strong` |
| Privasi | `lock` / `shield` |

---

## 9. Nada Visual & Anti-Pola

**Lakukan:**
- Ruang putih lapang, satu aksen per layar.
- Kedalaman lembut, radius membulat, tipografi rapi.
- Pesan netral & ringkas.

**Hindari:**
- ❌ Merah menyala untuk overdue/penundaan.
- ❌ Gradien berlebihan, shadow bertumpuk.
- ❌ Emoji berlebihan / gamification.
- ❌ Dashboard penuh angka tanpa konteks.
- ❌ Fear-based copy di UI.

---

## 10. Pemetaan ke Compose (saran)

| Token desain | Compose |
|---|---|
| `ink` `#0A0A0A` | `MaterialTheme.colorScheme.onSurface` (light) |
| `surface` `#FFFFFF` | `colorScheme.background` / `surface` |
| `surface-sunken` `#F8F8F8` | `surfaceVariant` |
| `radius-card 18` | `RoundedCornerShape(18.dp)` |
| `elev-card` | `Modifier.shadow(elevation, shape, ambientColor, spotColor)` |
| Font Inter | `Typography` dengan `FontFamily(Font(R.font.inter))` |
| Ikon | `Icons.Rounded.*` / `Icon(painterResource(...))` |
| Spacing 32/16/8 | `object Spacing { val x8 = 32.dp; ... }` |

> Semua teks lewat `string.xml` (default English, siap `id`) — PRD §12 Lokalisasi.

---

## 11. Referensi Silang

- Sumber token (aktual) → `gamabaran.pen`
- Pembangun mockup → `build_pen.js` (perhatikan: `.pen` mungkin sudah diedit setelah build)
- Inventaris layar → `07-screens.md`
- Prinsip nada → `01-prd.md` §4
- Target performa (motion minim) → `01-prd.md` §12

---

## 12. Kesesuaian dengan `gamabaran.pen` (audit)

Dokumen ini **diaudit ulang langsung terhadap isi `gamabaran.pen`** yang sekarang,
bukan hanya dari `build_pen.js`. Hasil:

| Aspek | Status | Catatan |
|---|---|---|
| Font | ✅ Sesuai | 100% teks = **Inter** |
| Palet inti | ✅ Sesuai | `#0A0A0A`, putih, skala Abu `#555/#666/#777/#888/#999/#AAA/#CCC` |
| Aksen | ✅ Sesuai | Hanya `#CC3333` (+ `#CC333315`, `#FFF5F5`) untuk danger |
| Kedalaman 3D | ✅ Sesuai | Pola `Base`/`Front` + shadow alpha rendah |
| Radius | 🔧 Diperbarui | Skala aktual 2→46 (lihat §4) |
| Tipografi | 🔧 Diperbarui | Skala aktual 8→68 (lihat §3.1); dokumen lama hanya mencatat s/d 32 |
| Stroke | 🔧 Ditambah | 1 / 1.5 / 2 / 10 (ring timer) |
| Ikon | ✅ Sesuai | Material Symbols Rounded; `anchor`, `bar_chart`, `check`, `add`, dll. |
| Ukuran layar | ✅ Sesuai | 11 layar HP 375×812, radius sudut **32** |
| Grid kanvas | ✅ Sesuai | Banner & hero lebar **1680**; grid rata (gap 60) |

**Kesimpulan:** dokumen design **sudah sesuai** dengan `gamabaran.pen` setelah tiga
koreksi (radius, tipografi, stroke). Identitas "Black & White Minimalist · 3D Depth
System" dari banner terverifikasi konsisten di seluruh mockup.

Cara mengaudit ulang (tooling `execute` pada `.pen`):
```
Get(n => n.type === "text" && Print(n.fontSize, n.fontWeight));
Get(n => n.cornerRadius !== undefined && Print(n.cornerRadius));
Get(n => typeof n.fill === "string" && n.fill.startsWith("#") && Print(n.fill));
```

---

> **CATATAN AKHIR:** Token di sini diekstrak dari `gamabaran.pen` dan boleh dirapikan saat
> implementasi selama **nada & prinsip** (monokrom, tenang, non-judgmental) dipertahankan.
> Jika `.pen` diedit lagi, ulangi audit §12.
