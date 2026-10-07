# Design System — Anchor

> **Status:** Draft · **Tanggal:** 2026-10-07
> **Filosofi:** Minimal · Calm · Intelligent · Tactile · Fast
> **Prinsip:** Quiet, fast, intentional. Tidak boleh mengorbankan performa.
> **Dokumen terkait:** `04-ux-ia-wireframes.md`

---

## 1. Design Principles

1. **Calm by default** — UI tidak berteriak. Warna & gerak hemat.
2. **Content first** — tipografi kuat, whitespace lega, hierarki jelas.
3. **Tactile** — interaksi terasa nyata (haptic halus, ripple tepat).
4. **Fast** — animasi ringan, hindari recomposition, low-end friendly.
5. **Intentional** — setiap elemen punya alasan; tidak ada hiasan.

**Hindari:** card berlebihan, gradient berlebihan, animasi berlebihan,
gamification overload, dashboard penuh.

---

## 2. Color System (Material 3 roles)

> Nilai di bawah adalah *usulan awal*. Disimpan sebagai token; mudah disesuaikan.
> Semua mengikuti skema Material 3 (light & dark) agar konsisten & accessible.

### 2.1 Brand / Accent
| Token | Light | Dark | Pemakaian |
|---|---|---|---|
| `primary` | `#3D5A80` (calm blue) | `#9FB8D6` | Aksi utama, fokus |
| `onPrimary` | `#FFFFFF` | `#0A1C30` | Teks di atas primary |
| `primaryContainer` | `#D6E3F3` | `#29405C` | Kartu utama |
| `secondary` | `#6B7A8F` | `#B9C3D1` | Aksi sekunder |
| `tertiary` | `#6E8B6A` (calm green) | `#B5CBB2` | Sukses / selesai |

### 2.2 Surface / Neutral
| Token | Light | Dark |
|---|---|---|
| `background` | `#FAFAF8` | `#111214` |
| `surface` | `#FFFFFF` | `#191A1D` |
| `surfaceVariant` | `#F1F1EE` | `#26282C` |
| `outline` | `#DAD9D4` | `#3A3D42` |
| `onBackground` | `#1B1C1E` | `#E8E9EA` |
| `onSurfaceVariant` | `#5A5C60` | `#A9ACB1` |

### 2.3 Semantic (priority & status)
Sengaja **muted**, bukan menyala (tidak menghakimi).
| Token | Warna (light) | Pemakaian |
|---|---|---|
| `priorityUrgent` | `#B4453C` (muted red) | URGENT |
| `priorityHigh` | `#C0742E` (muted amber) | HIGH |
| `priorityMedium` | `#6B7A8F` (neutral) | MEDIUM |
| `priorityLow` | `#8A99A8` (muted) | LOW |
| `statusDone` | `#6E8B6A` (muted green) | DONE |
| `overdue` | `#8A6A5A` (muted clay) | Overdue — halus, bukan alarm |

> **Aturan:** warna status tidak boleh dominan; gunakan sebagai aksen kecil,
> bukan blok penuh. Tone harus tenang.

### 2.4 Accessibility
- Semua pasangan teks/latar memenuhi kontras **WCAG AA** (rasio ≥ 4.5:1).
- Jangan mengandalkan warna saja untuk status → tambahkan ikon/label.

---

## 3. Typography

**Font keluarga:** `Inter` (atau `Roboto` sebagai fallback sistem) — bersih, netral, hemat.

| Token | Size (sp) | Weight | Line height | Pemakaian |
|---|---|---|---|---|
| `displayL` | 32 | 600 | 40 | Timer fokus |
| `headlineM` | 24 | 600 | 32 | Judul screen |
| `titleL` | 20 | 600 | 28 | Judul kartu |
| `titleM` | 16 | 500 | 24 | Judul task |
| `bodyL` | 16 | 400 | 24 | Body |
| `bodyM` | 14 | 400 | 20 | Body sekunder |
| `labelL` | 14 | 500 | 20 | Chips, tombol |
| `labelM` | 12 | 500 | 16 | Metadata |
| `caption` | 12 | 400 | 16 | Disclaimer, timestamp |

- **Letter spacing:** 0 default; sedikit negatif (-0.2) untuk headline besar.
- **Numerik fokus:** gunakan tabular figures (`tabular-nums`) agar timer tidak goyang.

---

## 4. Spacing

Skala 4dp:
```
4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64
```
| Token | Value | Pemakaian umum |
|---|---|---|
| `spaceXs` | 4 | antar ikon-teks kecil |
| `spaceS` | 8 | padding internal kecil |
| `spaceM` | 16 | padding screen default |
| `spaceL` | 24 | antar section |
| `spaceXl` | 32 | antar blok besar |

**Padding screen default:** 16dp horizontal.
**Gutter list:** 16dp.

---

## 5. Shape / Radius

| Token | Value | Pemakaian |
|---|---|---|
| `radiusS` | 8 | chip, badge |
| `radiusM` | 12 | input, tombol |
| `radiusL` | 16 | card |
| `radiusFull` | 999 | FAB, avatar, pill |

Card utama task memakai `radiusL`. Konsisten di seluruh app.

---

## 6. Elevation

Minimal (tidak ada bayangan berlebihan):
| Level | Pemakaian |
|---|---|
| 0 | background, list item |
| 1 | card halus (shadow tipis atau border) |
| 2 | bottom sheet, FAB |
| 3 | dialog, menu |

**Preferensi:** gunakan **border/outline** ringan alih-alih shadow di list
(lebih murah untuk performa).

---

## 7. Iconography

- Sumber: **Material Symbols** (outlined default, rounded untuk keadaan aktif).
- Ukuran: 20dp (inline), 24dp (standar), 28dp (nav).
- Stroke konsisten (outlined = 1.5–2).
- Ikon selalu disertai label bila maknanya ambigu.

---

## 8. Motion

| Token | Durasi | Easing | Pemakaian |
|---|---|---|---|
| `motionShort` | 150ms | standard | ripple, toggle |
| `motionMedium` | 200–250ms | standard | transisi layar, sheet |
| `motionLong` | 300ms | emphasized | transisi kompleks (jarang) |

**Aturan:**
- Hormati **Reduce Motion** sistem.
- Tidak ada animasi loop/berat.
- Transisi daftar: fade ringan, bukan slide masif.
- Progress ring: animasi halus tapi murah (bukan full-blown physics).

---

## 9. Interaction Feedback

- **Ripple** Material standar pada tap target.
- **Haptic** ringan (light) untuk: selesai task, mulai fokus, tombol inti.
- **Snackbar** untuk konfirmasi cepat (mis. "Task added", "Rescheduled").
- **Undo** untuk aksi destruktif ringan (hapus/arsip) alih-alih dialog.

---

## 10. Themes

### Light
- Background: `#FAFAF8` (off-white hangat, tidak menyilaukan).
- Surface: putih bersih.

### Dark
- Background: `#111214` (near-black hangat, bukan hitam murni → hemat OLED & nyaman).
- Surface: `#191A1D`.
- Kontras dijaga (teks `#E8E9EA`).

**Default:** ikuti sistem. Sediakan toggle manual di Settings (opsional).

---

## 11. Component Rules

### 11.1 Buttons
| Jenis | Pemakaian |
|---|---|
| Filled | Aksi utama (Start Focus, Save) |
| Tonal | Aksi sekunder (Reschedule) |
| Text | Aksi tersier (Cancel, Later) |
- Tinggi tombol: 48dp. Radius `radiusM`. 
- Minimal satu filled per screen.

### 11.2 Cards
- Task card: radius `radiusL`, padding 16dp, outline tipis.
- Tidak menumpuk banyak kartu dalam kartu.

### 11.3 Lists
- Row height nyaman (≥ 56dp), divider tipis atau spacing.
- Swipe actions: complete/reschedule/archive.

### 11.4 Inputs
- Outlined, radius `radiusM`, label mengambang.
- Error state: teks + ikon (bukan merah menyala).

### 11.5 Chips (filter)
- Pill, `radiusFull`. Terpilih = filled tonal; tidak = outline.

### 11.6 Progress
- Linear tipis (4–6dp) atau ring tipis. Warna `primary`/`tertiary`.

### 11.7 Bottom sheet
- Radius atas `radiusL`. Untuk New Task & intervention ringan.

### 11.8 Bottom navigation
- 5 item; ikon + label; state aktif jelas (bukan warna menyala).

---

## 12. Performance Guardrails (mengikat desain)

- **Hindari animasi berat** & continuous animation.
- **Stable keys** pada `LazyColumn`/`LazyRow`.
- Gunakan **`remember`/`derivedStateOf`** untuk mengurangi recomposition.
- **Hindari nested scroll** berat.
- Ikon/asset: vektor (bukan PNG besar); jangan pakai gambar berat.
- Shadow hemat → pakai outline bila bisa.
- Target: **60fps** pada device RAM 3–4 GB; jank-free saat scroll list.

---

## 13. Token Delivery (implementasi)

Token disediakan sebagai:
- `Color.kt` — color roles
- `Type.kt` — typography scale
- `Dimens.kt` — spacing/radius
- `Motion.kt` — durasi/easing
- `Theme.kt` — Material 3 `ColorScheme` light/dark

Semua UI membaca token, **tidak ada hard-coded** warna/ukuran di composable.

> `[DECISION]` Font: Inter (bundel) vs Roboto (sistem). Rekomendasi: mulai
> **Roboto sistem** untuk menghemat ukuran APK & zero network; pertimbangkan Inter
> saat polish. Tidak dikunci sekarang.
