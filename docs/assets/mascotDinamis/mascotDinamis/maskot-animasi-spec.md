# Spesifikasi Animasi Maskot Nemo (untuk AI dan developer)

Dokumen ini dibaca oleh AI atau developer yang mengimplementasikan animasi maskot di app. Semua animasi dibuat dari **gambar diam** yang digerakkan lewat kode. **Tanpa video, GIF, atau Lottie.**

Pratinjau visual untuk manusia: https://claude.ai/artifact/NwFjmbHu6rnN3hDped3WgM (AI tidak perlu membukanya; semua yang dibutuhkan ada di berkas ini).

Berkas pendamping: `mascot-assets.zip` (16 PNG) dan `maskot-animasi.json` (data yang sama dengan bagian 3, untuk dibaca program).

## 0. Prompt siap tempel untuk AI

Tempel prompt ini bersama berkas ini dan zip aset. Ganti `[PLATFORM]`.

```
Implementasikan komponen animasi maskot untuk app [PLATFORM: mis. Android Jetpack Compose / Flutter / React Native / SwiftUI].
Baca spesifikasi (maskot-animasi-spec.md) sampai habis sebelum menulis kode.

1. Pakai PNG di folder mascot/ apa adanya. Jangan menggambar ulang atau mengubah warna.
2. Buat satu komponen MascotView(expression, size, paused). Mengganti expression memutar animasi wajah itu
   dari awal, dengan crossfade sekitar 120 ms dari wajah sebelumnya.
3. Terapkan tiap animasi persis seperti JSON di bagian 3: durasi, easing, keyframe, origin, face_swaps, dan track fx.
   Jika JSON dan CSS di bagian 4 berbeda, CSS yang benar.
4. Semua animasi berulang tanpa henti. Dukung paused, dan bila pengguna mengaktifkan hemat gerak/matikan animasi sistem,
   tampilkan gambar statis.
5. Hanya pakai transform, opacity, dan clip pada gambar. Jangan video, GIF, atau Lottie.
6. Buat satu layar demo yang menampilkan 12 ekspresi sekaligus agar saya bisa mengecek.

Keluarkan: kode komponen, daftar file yang dibuat, dan catatan bagian mana yang tidak bisa dibuat persis.
```

## 1. Aset

Semua PNG berukuran 760x760, latar transparan, kepala sudah sejajar di posisi yang sama pada semua wajah (supaya tidak melompat saat berganti wajah atau berganti layer).

| # | Wajah (`id`) | Layer kepala | Layer simbol (fx) |
|---|---|---|---|
| 1 | neutral | `mascot_neutral.png` | - |
| 2 | happy | `mascot_happy.png` | - |
| 3 | sad | `mascot_sad.png` | - |
| 4 | angry | `mascot_angry.png` | - |
| 5 | surprised | `mascot_surprised.png` | `mascot_surprised_fx.png` |
| 6 | sleepy | `mascot_sleepy.png` | `mascot_sleepy_fx.png` |
| 7 | confused | `mascot_confused.png` | `mascot_confused_fx.png` |
| 8 | laughing | `mascot_laughing.png` | - |
| 9 | shy | `mascot_shy.png` | - |
| 10 | thinking | `mascot_thinking.png` | `mascot_thinking_fx.png` |
| 11 | wink | `mascot_wink.png` | - |
| 12 | talking | `mascot_talking.png` | - |

Layer simbol hanya ada di surprised (garis kejut), sleepy (zzz), confused (tanda tanya), dan thinking (titik). Simbol lain, seperti tanda urat di Angry, air mata di Sad, dan garis pipi di Shy, menyatu di gambar kepala.

## 2. Konvensi (wajib dibaca)

- **Kotak maskot:** persegi tempat semua layer ditumpuk dengan ukuran penuh. Ukurannya mengikuti `size` komponen.
- **Satuan:** `x` dan `y` dalam **persen sisi kotak** (y positif = turun, x positif = ke kanan). `rot` dalam derajat, positif = searah jarum jam. `sx` dan `sy` adalah skala (1 = asli). `opacity` 0 sampai 1.
- **Urutan transform:** translate, lalu rotate, lalu scale, terhadap titik `origin` `[x%, y%]` dari kotak maskot (`[0,0]` = kiri-atas). Selisih akibat urutan pada rotasi kecil boleh diabaikan.
- **Keyframe:** `t` adalah posisi dalam satu siklus (0 sampai 1). Nilai di antara keyframe diinterpolasi linear per parameter.
- **Easing** berlaku di **setiap segmen** antar keyframe, bukan di seluruh siklus (sama seperti CSS). `ease-in-out` = `cubic-bezier(0.42,0,0.58,1)`, `ease-out` = `cubic-bezier(0,0,0.58,1)`. `steps(3,end)` = nilai melompat 3 kali di setiap segmen, lompatan terjadi di akhir tiap langkah.
- **Loop:** semua berulang tanpa henti. `direction: alternate` = satu siklus maju lalu mundur (total 2x `duration_ms`).
- **Urutan layer (bawah ke atas):** (1) kepala wajah dasar, (2) `face_swaps`, (3) `fx`.
- **face_swaps:** selama jendela `show_during` (pecahan siklus), tampilkan gambar kepala wajah lain **menimpa** wajah dasar. Loncat langsung, tanpa fade. Di luar jendela, wajah dasar yang terlihat.
- **body:** transform ini berlaku pada **semua layer sekaligus** (simbol ikut bergerak bersama kepala).
- **fx:** daftar track yang hanya menggerakkan layer simbol, di atas gerak body. Tiap track punya durasinya sendiri dan semuanya mulai bersamaan. `reveal` 0 = tersembunyi, 1 = terlihat penuh; `reveal_from: bottom` = muncul dari bawah ke atas, `left` = dari kiri ke kanan (buat dengan clip atau mask).

## 3. Spesifikasi 12 animasi

### 1. neutral

Bernapas pelan, sesekali mengedip sebelah.

```json
{
  "index": 1,
  "id": "neutral",
  "intent": "Bernapas pelan, sesekali mengedip sebelah.",
  "assets": {"head": "mascot_neutral.png", "fx": null},
  "body": {
    "duration_ms": 4000,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.5, "x": 0, "y": -4, "rot": 0, "sx": 1.015, "sy": 1.015},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": {
    "duration_ms": 4000,
    "swaps": [
      {
        "face": "wink",
        "show_during": [
          [0.72, 0.77]
        ]
      }
    ]
  },
  "fx": null
}
```

### 2. happy

Melompat kecil dengan efek gepeng dan melar (squash and stretch).

```json
{
  "index": 2,
  "id": "happy",
  "intent": "Melompat kecil dengan efek gepeng dan melar (squash and stretch).",
  "assets": {"head": "mascot_happy.png", "fx": null},
  "body": {
    "duration_ms": 1100,
    "direction": "normal",
    "easing": "cubic-bezier(0.3,0,0.4,1)",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1.07, "sy": 0.93},
      {"t": 0.15, "x": 0, "y": 0, "rot": 0, "sx": 0.96, "sy": 1.05},
      {"t": 0.45, "x": 0, "y": -16, "rot": 0, "sx": 0.98, "sy": 1.04},
      {"t": 0.75, "x": 0, "y": 0, "rot": 0, "sx": 1.07, "sy": 0.93},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1.07, "sy": 0.93}
    ]
  },
  "face_swaps": null,
  "fx": null
}
```

### 3. sad

Kepala merosot pelan sambil miring, seperti menghela napas.

```json
{
  "index": 3,
  "id": "sad",
  "intent": "Kepala merosot pelan sambil miring, seperti menghela napas.",
  "assets": {"head": "mascot_sad.png", "fx": null},
  "body": {
    "duration_ms": 5000,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.5, "x": 0, "y": 7, "rot": -5, "sx": 0.97, "sy": 0.97},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": null
}
```

### 4. angry

Diam menahan, lalu mengamuk: getar kiri-kanan cepat sambil sedikit membesar, lalu tenang.

```json
{
  "index": 4,
  "id": "angry",
  "intent": "Diam menahan, lalu mengamuk: getar kiri-kanan cepat sambil sedikit membesar, lalu tenang.",
  "assets": {"head": "mascot_angry.png", "fx": null},
  "body": {
    "duration_ms": 2400,
    "direction": "normal",
    "easing": "linear",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.54, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.57, "x": -3, "y": 0, "rot": -2, "sx": 1.04, "sy": 1.04},
      {"t": 0.61, "x": 3, "y": 0, "rot": 2, "sx": 1.04, "sy": 1.04},
      {"t": 0.65, "x": -3, "y": 0, "rot": -2, "sx": 1.04, "sy": 1.04},
      {"t": 0.69, "x": 3, "y": 0, "rot": 2, "sx": 1.04, "sy": 1.04},
      {"t": 0.73, "x": -3, "y": 0, "rot": -2, "sx": 1.04, "sy": 1.04},
      {"t": 0.77, "x": 3, "y": 0, "rot": 2, "sx": 1.04, "sy": 1.04},
      {"t": 0.82, "x": 0, "y": 0, "rot": 0, "sx": 1.04, "sy": 1.04},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": null
}
```

### 5. surprised

Terlonjak membesar lalu memantul; garis kejut muncul serentak dan menghilang.

```json
{
  "index": 5,
  "id": "surprised",
  "intent": "Terlonjak membesar lalu memantul; garis kejut muncul serentak dan menghilang.",
  "assets": {"head": "mascot_surprised.png", "fx": "mascot_surprised_fx.png"},
  "body": {
    "duration_ms": 2800,
    "direction": "normal",
    "easing": "ease-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.55, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.6, "x": 0, "y": -6, "rot": 0, "sx": 1.18, "sy": 1.18},
      {"t": 0.68, "x": 0, "y": 0, "rot": 0, "sx": 0.96, "sy": 0.96},
      {"t": 0.76, "x": 0, "y": 0, "rot": 0, "sx": 1.07, "sy": 1.07},
      {"t": 0.86, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": [
    {
      "name": "burst",
      "duration_ms": 2800,
      "direction": "normal",
      "easing": "ease-out",
      "origin": [50, 50],
      "keyframes": [
        {"t": 0, "opacity": 0, "sx": 0.8, "sy": 0.8},
        {"t": 0.55, "opacity": 0, "sx": 0.8, "sy": 0.8},
        {"t": 0.6, "opacity": 1, "sx": 1.12, "sy": 1.12},
        {"t": 0.86, "opacity": 1, "sx": 1, "sy": 1},
        {"t": 0.96, "opacity": 0, "sx": 1, "sy": 1},
        {"t": 1, "opacity": 0, "sx": 1, "sy": 1}
      ]
    }
  ]
}
```

### 6. sleepy

Terangguk pelan ke depan lalu tersentak bangun; huruf z muncul satu per satu dari bawah dan melayang naik.

```json
{
  "index": 6,
  "id": "sleepy",
  "intent": "Terangguk pelan ke depan lalu tersentak bangun; huruf z muncul satu per satu dari bawah dan melayang naik.",
  "assets": {"head": "mascot_sleepy.png", "fx": "mascot_sleepy_fx.png"},
  "body": {
    "duration_ms": 4500,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 96],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.48, "x": 0, "y": 4, "rot": 14, "sx": 1, "sy": 1},
      {"t": 0.57, "x": 0, "y": 4, "rot": 14, "sx": 1, "sy": 1},
      {"t": 0.63, "x": 0, "y": 0, "rot": -3, "sx": 1, "sy": 1},
      {"t": 0.72, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": [
    {
      "name": "reveal",
      "duration_ms": 3000,
      "direction": "normal",
      "easing": "steps(3,end)",
      "reveal_from": "bottom",
      "keyframes": [
        {"t": 0, "reveal": 0},
        {"t": 0.35, "reveal": 1},
        {"t": 1, "reveal": 1}
      ]
    },
    {
      "name": "float_move",
      "duration_ms": 3000,
      "direction": "normal",
      "easing": "ease-in-out",
      "origin": [50, 50],
      "keyframes": [
        {"t": 0, "x": 0, "y": 8},
        {"t": 1, "x": 7, "y": -12}
      ]
    },
    {
      "name": "float_fade",
      "duration_ms": 3000,
      "direction": "normal",
      "easing": "ease-in-out",
      "keyframes": [
        {"t": 0, "opacity": 0},
        {"t": 0.25, "opacity": 1},
        {"t": 0.7, "opacity": 1},
        {"t": 1, "opacity": 0}
      ]
    }
  ]
}
```

### 7. confused

Kepala miring kiri-kanan pelan; tanda tanya bergoyang sendiri.

```json
{
  "index": 7,
  "id": "confused",
  "intent": "Kepala miring kiri-kanan pelan; tanda tanya bergoyang sendiri.",
  "assets": {"head": "mascot_confused.png", "fx": "mascot_confused_fx.png"},
  "body": {
    "duration_ms": 3600,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": -7, "sx": 1, "sy": 1},
      {"t": 0.5, "x": 2, "y": 0, "rot": 7, "sx": 1, "sy": 1},
      {"t": 1, "x": 0, "y": 0, "rot": -7, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": [
    {
      "name": "question_mark",
      "duration_ms": 1800,
      "direction": "normal",
      "easing": "ease-in-out",
      "origin": [50, 50],
      "keyframes": [
        {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
        {"t": 0.5, "x": 0, "y": -8, "rot": 14, "sx": 1.1, "sy": 1.1},
        {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
      ]
    }
  ]
}
```

### 8. laughing

Tertawa terbahak: tiga lompatan cepat sambil mendongak, lalu jeda.

```json
{
  "index": 8,
  "id": "laughing",
  "intent": "Tertawa terbahak: tiga lompatan cepat sambil mendongak, lalu jeda.",
  "assets": {"head": "mascot_laughing.png", "fx": null},
  "body": {
    "duration_ms": 1800,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.06, "x": 0, "y": -7, "rot": -7, "sx": 1, "sy": 1},
      {"t": 0.14, "x": 0, "y": 1, "rot": -5, "sx": 1.03, "sy": 0.97},
      {"t": 0.22, "x": 0, "y": -7, "rot": -7, "sx": 1, "sy": 1},
      {"t": 0.3, "x": 0, "y": 1, "rot": -5, "sx": 1.03, "sy": 0.97},
      {"t": 0.38, "x": 0, "y": -7, "rot": -7, "sx": 1, "sy": 1},
      {"t": 0.46, "x": 0, "y": 1, "rot": -5, "sx": 1.03, "sy": 0.97},
      {"t": 0.56, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": null
}
```

### 9. shy

Menciut ke pojok kiri-bawah, bergoyang gugup, lalu mengintip kembali.

```json
{
  "index": 9,
  "id": "shy",
  "intent": "Menciut ke pojok kiri-bawah, bergoyang gugup, lalu mengintip kembali.",
  "assets": {"head": "mascot_shy.png", "fx": null},
  "body": {
    "duration_ms": 4200,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.25, "x": -7, "y": 5, "rot": 7, "sx": 0.92, "sy": 0.92},
      {"t": 0.32, "x": -9, "y": 5, "rot": 4, "sx": 0.92, "sy": 0.92},
      {"t": 0.39, "x": -6, "y": 5, "rot": 9, "sx": 0.92, "sy": 0.92},
      {"t": 0.6, "x": -7, "y": 5, "rot": 7, "sx": 0.92, "sy": 0.92},
      {"t": 0.76, "x": 1, "y": 0, "rot": -2, "sx": 1.03, "sy": 1.03},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": null
}
```

### 10. thinking

Kepala miring merenung; titik muncul satu per satu dari kiri ke kanan.

```json
{
  "index": 10,
  "id": "thinking",
  "intent": "Kepala miring merenung; titik muncul satu per satu dari kiri ke kanan.",
  "assets": {"head": "mascot_thinking.png", "fx": "mascot_thinking_fx.png"},
  "body": {
    "duration_ms": 5000,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.3, "x": -2, "y": 0, "rot": -6, "sx": 1, "sy": 1},
      {"t": 0.7, "x": -2, "y": 0, "rot": -6, "sx": 1, "sy": 1},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": null,
  "fx": [
    {
      "name": "dots",
      "duration_ms": 2400,
      "direction": "normal",
      "easing": "steps(3,end)",
      "reveal_from": "left",
      "keyframes": [
        {"t": 0, "reveal": 0},
        {"t": 0.8, "reveal": 1},
        {"t": 1, "reveal": 1}
      ]
    }
  ]
}
```

### 11. wink

Kedipan genit: melonjak dan miring tepat saat satu mata menutup, lalu kembali.

```json
{
  "index": 11,
  "id": "wink",
  "intent": "Kedipan genit: melonjak dan miring tepat saat satu mata menutup, lalu kembali.",
  "assets": {"head": "mascot_wink.png", "fx": null},
  "body": {
    "duration_ms": 2600,
    "direction": "normal",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.25, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 0.32, "x": 0, "y": -6, "rot": -5, "sx": 1.08, "sy": 1.08},
      {"t": 0.45, "x": 0, "y": 0, "rot": -4, "sx": 1.05, "sy": 1.05},
      {"t": 0.68, "x": 0, "y": 0, "rot": -4, "sx": 1.05, "sy": 1.05},
      {"t": 0.78, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 1, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": {
    "duration_ms": 2600,
    "swaps": [
      {
        "face": "neutral",
        "show_during": [
          [0, 0.28],
          [0.72, 1]
        ]
      }
    ]
  },
  "fx": null
}
```

### 12. talking

Mulut buka-tutup mengikuti ritme bicara; kepala mengangguk kecil.

```json
{
  "index": 12,
  "id": "talking",
  "intent": "Mulut buka-tutup mengikuti ritme bicara; kepala mengangguk kecil.",
  "assets": {"head": "mascot_talking.png", "fx": null},
  "body": {
    "duration_ms": 500,
    "direction": "alternate",
    "easing": "ease-in-out",
    "origin": [50, 88],
    "keyframes": [
      {"t": 0, "x": 0, "y": 0, "rot": 0, "sx": 1, "sy": 1},
      {"t": 1, "x": 0, "y": -2.5, "rot": 0, "sx": 1, "sy": 1}
    ]
  },
  "face_swaps": {
    "duration_ms": 2000,
    "swaps": [
      {
        "face": "neutral",
        "show_during": [
          [0.1, 0.18],
          [0.34, 0.42],
          [0.52, 0.6],
          [0.78, 1.0]
        ]
      }
    ]
  },
  "fx": null
}
```

## 4. Acuan CSS (kebenaran akhir)

Ini kode yang dipakai pada halaman pratinjau. Nama kelas `a-<id>` dipasang pada kotak maskot; `.s` adalah layer `face_swaps`; `.x` adalah layer `fx`. Jika ada selisih dengan JSON di atas, ikuti CSS ini.

```css
.a-neutral{animation:fl 4s ease-in-out infinite}
@keyframes fl{50%{transform:translateY(-4%) scale(1.015)}}
.a-neutral .s{animation:wk 4s step-end infinite}
@keyframes wk{0%,72%{opacity:0}72.1%,77%{opacity:1}77.1%,100%{opacity:0}}

.a-happy{animation:hop 1.1s cubic-bezier(.3,0,.4,1) infinite}
@keyframes hop{0%,100%{transform:scale(1.07,.93)}15%{transform:scale(.96,1.05)}45%{transform:translateY(-16%) scale(.98,1.04)}75%{transform:scale(1.07,.93)}}

.a-sad{animation:dr 5s ease-in-out infinite}
@keyframes dr{50%{transform:translateY(7%) rotate(-5deg) scale(.97)}}

.a-angry{animation:rg 2.4s linear infinite}
@keyframes rg{0%,54%,100%{transform:none}57%,65%,73%{transform:translateX(-3%) rotate(-2deg) scale(1.04)}61%,69%,77%{transform:translateX(3%) rotate(2deg) scale(1.04)}82%{transform:scale(1.04)}}

.a-surprised{animation:pp 2.8s ease-out infinite}
@keyframes pp{0%,55%,100%{transform:none}60%{transform:translateY(-6%) scale(1.18)}68%{transform:scale(.96)}76%{transform:scale(1.07)}86%{transform:none}}
.a-surprised .x{animation:bu 2.8s ease-out infinite}
@keyframes bu{0%,55%{opacity:0;transform:scale(.8)}60%{opacity:1;transform:scale(1.12)}86%{opacity:1;transform:none}96%,100%{opacity:0}}

.a-sleepy{transform-origin:50% 96%;animation:nd 4.5s ease-in-out infinite}
@keyframes nd{0%,100%{transform:none}48%,57%{transform:rotate(14deg) translateY(4%)}63%{transform:rotate(-3deg)}72%{transform:none}}
.a-sleepy .x{animation:rv 3s steps(3,end) infinite,zz 3s ease-in-out infinite}
@keyframes rv{0%{clip-path:inset(100% 0 0 0)}35%,100%{clip-path:inset(0)}}
@keyframes zz{0%{opacity:0;transform:translate(0,8%)}25%,70%{opacity:1}100%{opacity:0;transform:translate(7%,-12%)}}

.a-confused{animation:tl 3.6s ease-in-out infinite}
@keyframes tl{0%,100%{transform:rotate(-7deg)}50%{transform:rotate(7deg) translateX(2%)}}
.a-confused .x{animation:qm 1.8s ease-in-out infinite}
@keyframes qm{50%{transform:translateY(-8%) rotate(14deg) scale(1.1)}}

.a-laughing{animation:ha 1.8s ease-in-out infinite}
@keyframes ha{0%,56%,100%{transform:none}6%,22%,38%{transform:translateY(-7%) rotate(-7deg)}14%,30%,46%{transform:translateY(1%) rotate(-5deg) scale(1.03,.97)}}

.a-shy{animation:hd 4.2s ease-in-out infinite}
@keyframes hd{0%,100%{transform:none}25%,60%{transform:translate(-7%,5%) rotate(7deg) scale(.92)}32%{transform:translate(-9%,5%) rotate(4deg) scale(.92)}39%{transform:translate(-6%,5%) rotate(9deg) scale(.92)}76%{transform:translate(1%,0) rotate(-2deg) scale(1.03)}}

.a-thinking{animation:th 5s ease-in-out infinite}
@keyframes th{0%,100%{transform:none}30%,70%{transform:rotate(-6deg) translateX(-2%)}}
.a-thinking .x{animation:dt 2.4s steps(3,end) infinite}
@keyframes dt{0%{clip-path:inset(0 100% 0 0)}80%,100%{clip-path:inset(0)}}

.a-wink{animation:wp 2.6s ease-in-out infinite}
@keyframes wp{0%,25%,78%,100%{transform:none}32%{transform:translateY(-6%) rotate(-5deg) scale(1.08)}45%,68%{transform:rotate(-4deg) scale(1.05)}}
.a-wink .s{animation:nw 2.6s step-end infinite}
@keyframes nw{0%,28%{opacity:1}28.1%,72%{opacity:0}72.1%,100%{opacity:1}}

.a-talking{animation:bb .5s ease-in-out infinite alternate}
@keyframes bb{to{transform:translateY(-2.5%)}}
.a-talking .s{animation:mt 2s step-end infinite}
@keyframes mt{0%{opacity:0}10%{opacity:1}18%{opacity:0}34%{opacity:1}42%{opacity:0}52%{opacity:1}60%{opacity:0}78%,100%{opacity:1}}
```

## 5. Kriteria selesai

- Semua 12 wajah punya animasi yang berbeda dan terlihat sama dengan pratinjau.
- Kepala tidak melompat atau bergeser saat berganti wajah.
- `paused` dan hemat gerak sistem berfungsi (gambar statis, tanpa gerak).
- Tidak ada berkas video, GIF, atau Lottie.
- Animasi hanya memakai transform, opacity, dan clip supaya ringan (tanpa relayout tiap frame).
- Mengganti wajah memutar animasi wajah baru dari awal dengan crossfade sekitar 120 ms.
