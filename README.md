# 🍢 POS UMKM Tahu Bulat — Panduan Setup di VS Code

## 📋 Deskripsi Aplikasi

Aplikasi Point of Sale (POS) untuk UMKM Tahu Bulat berbasis **JavaFX** dengan fitur lengkap:

| Fitur | Implementasi |
|-------|-------------|
| 🔊 Pengelolaan Suara | `javax.sound.sampled` (Java Sound API) — beep klik & melodi transaksi |
| 🖼️ Gambar | JavaFX Canvas — ilustrasi tahu bulat bergradien |
| 📅 Calendar & Date | `LocalDateTime` + `DateTimeFormatter` (ID Locale) — real-time clock |
| 🔍 Pencarian | Live search filter pada daftar menu |
| 🧮 Perhitungan | `jumlah × harga` → total otomatis per item & keseluruhan |

---

## 🛠️ Prasyarat

### 1. Instal Java 17+
```
https://adoptium.net/  ← Download Eclipse Temurin JDK 17
```
Cek: `java -version`

### 2. Instal JavaFX SDK
```
https://gluonhq.com/products/javafx/  ← Download JavaFX 21 LTS
```
Ekstrak ke folder, misal: `C:\javafx-sdk-21` (Windows) atau `/opt/javafx-sdk-21` (Linux/Mac)

### 3. Instal Maven
```
https://maven.apache.org/download.cgi
```
Cek: `mvn -version`

### 4. VS Code Extensions yang dibutuhkan:
- **Extension Pack for Java** (Microsoft) — wajib
- **Maven for Java** (Microsoft) — wajib

---

## 🚀 Cara Menjalankan

### Metode 1: Maven (Disarankan)

```bash
# Masuk ke folder project
cd TahuBulatPOS

# Jalankan aplikasi
mvn javafx:run
```

### Metode 2: VS Code (via launch.json)

1. Buka folder `TahuBulatPOS` di VS Code
2. Edit `.vscode/launch.json` → isi `PATH_TO_FX` dengan path JavaFX SDK kamu:
   ```json
   "env": {
       "PATH_TO_FX": "C:/javafx-sdk-21/lib"
   }
   ```
3. Tekan `F5` atau klik **Run → Start Debugging**

### Metode 3: Terminal langsung

**Windows:**
```bash
set PATH_TO_FX=C:\javafx-sdk-21\lib
mvn javafx:run
```

**Mac/Linux:**
```bash
export PATH_TO_FX=/opt/javafx-sdk-21/lib
mvn javafx:run
```

---

## 📂 Struktur Project

```
TahuBulatPOS/
├── pom.xml                          ← Konfigurasi Maven + dependensi JavaFX
├── src/
│   └── main/
│       └── java/
│           ├── module-info.java     ← Deklarasi modul JavaFX
│           └── pos/
│               └── MainApp.java    ← SELURUH KODE APLIKASI
├── .vscode/
│   ├── launch.json                  ← Konfigurasi run di VS Code
│   └── settings.json                ← Pengaturan project Java
└── README.md
```

---

## 🎯 Alur Program (Algoritma)

```
START
  │
  ▼
[Inisialisasi Menu] ← 10 varian tahu bulat dengan harga
  │
  ▼
[Tampilkan UI] ← Header (tanggal/waktu), Panel Menu, Keranjang
  │
  ├──► [Pencarian Menu]
  │        │ User ketik keyword
  │        ▼
  │    Filter list menu secara real-time
  │
  ├──► [Pilih Menu dari List]
  │        │ User klik salah satu menu
  │        ▼
  │    menuDipilih = menu yang dipilih
  │
  ├──► [Input Jumlah]
  │        │ Klik +/- atau ketik langsung
  │        ▼
  │    tfJumlah.getText() → validasi angka
  │
  ├──► [Tambah ke Keranjang]
  │        │ Klik "Tambah ke Keranjang"
  │        ▼
  │    KeranjangItem = menuDipilih × jumlah
  │    total += harga × jumlah
  │    bunyikanKlik() ← Java Sound API
  │
  ├──► [Bayar Sekarang]
  │        │ Klik "BAYAR SEKARANG"
  │        ▼
  │    Hitung total semua item
  │    Generate struk (teks berformat)
  │    Tampilkan di TextArea
  │    bunyikanTerimaKasih() ← melodi Do-Mi-Sol-Do
  │    Tampilkan dialog konfirmasi
  │
  └──► [Transaksi Baru]
           │ Reset semua state
           ▼
         Kembali ke awal
```

---

## 🔊 Implementasi Java Sound API

```java
// Membuat dan memutar gelombang sinus (beep)
AudioFormat format = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false
);
SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
line.open(format);
line.start();
line.write(data, 0, data.length);  // data = array byte sinusoidal
line.drain();
line.close();
```

- **Saat tambah item**: beep nada 880Hz (80ms)
- **Saat transaksi berhasil**: melodi C5→E5→G5→C6 (akord mayor)

---

## 🎨 Screenshot Fitur

```
┌─────────────────────────────────────────────────────────────┐
│  🍢 TAHU BULAT MANG UDIN                                    │
│  "Crispy di Luar, Lembut di Dalam — Goreng Mendadak!"       │
│  📅 Rabu, 21 Mei 2025  |  14:35:22                          │
├──────────────┬──────────────────────────┬───────────────────┤
│ 🖼️ Produk   │ 🛒 Pilih Menu            │ 🧾 Keranjang      │
│              │ 🔍 [cari menu...]        │                   │
│  [gambar     │                          │ Menu  Qty Subtotal│
│   tahu bulat │ 🟡 Original  Rp 2.000   │ ──────────────────│
│   bergradien]│ 🔴 Pedas     Rp 2.500   │                   │
│              │ 🧀 Keju      Rp 3.000   │ TOTAL: Rp 0       │
│ ℹ️ Info Toko │ ...                      │                   │
│              │ Jumlah: [−][1][+]        │ [📄 Struk...]     │
│ 📊 Statistik │ [🛒 Tambah ke Keranjang] │ [💰 BAYAR]        │
└──────────────┴──────────────────────────┴───────────────────┘
│ ✅ Sistem siap — Selamat berjualan!         v1.0 POS Tahu   │
└─────────────────────────────────────────────────────────────┘
```

---

## ❓ Troubleshooting

| Masalah | Solusi |
|---------|--------|
| `Error: JavaFX runtime components are missing` | Pastikan `PATH_TO_FX` diset dengan benar |
| `mvn: command not found` | Install Maven dan tambahkan ke PATH |
| Tidak ada suara | Pastikan speaker/headphone terhubung; sound API butuh audio output |
| Build gagal dengan Java 8 | Gunakan Java 17 atau lebih baru |

---

*Dibuat untuk tugas UMKM POS — Tahu Bulat Mang Udin © 2025*
