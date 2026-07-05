# Git Branching Strategy untuk Project Katalon Automation

Dokumen ini menjelaskan strategi branch yang sederhana namun cukup kuat untuk project automation testing yang mulai berkembang. Pola yang digunakan adalah kombinasi dari branch utama dan branch pendukung dengan 4 jenis branch utama:

- master
- feature/*
- fix/*
- release/*

## Aturan Utama

- master = source of truth
- feature/* = pengembangan fitur baru
- fix/* = perbaikan bug
- release/* = tahap stabilisasi sebelum rilis
- gunakan tag untuk penanda versi resmi
- hapus branch setelah merge agar repository tetap rapi

---

## Peran Masing-masing Branch

### 1. master

Branch ini adalah sumber kode yang stabil.

Isi dari master biasanya:

- kode yang stabil
- sudah lolos testing
- siap digunakan tim

Jangan melakukan development langsung di branch ini.

---

### 2. feature/*

Branch ini digunakan untuk pengembangan fitur baru.

Contoh:

- feature/framework-core
- feature/login
- feature/employee

Biasanya dibuat dari master:

```bash
git checkout master
git checkout -b feature/login
```

---

### 3. fix/*

Branch ini digunakan untuk memperbaiki bug yang sudah masuk ke master atau release.

Contoh:

- fix/login-timeout
- fix/testrail-sync

Biasanya dibuat dari master atau release:

```bash
git checkout master
git checkout -b fix/login-timeout
```

---

### 4. release/*

Branch ini digunakan untuk persiapan rilis.

Contoh:

- release/v1.0
- release/v1.1

Di branch ini biasanya dilakukan:

- regression test
- UAT
- final bug fixing
- smoke test

---

# Contoh Alur Lengkap

Misalnya akan membuat release v1.0.

## Step 1 - Buat Feature

```text
master
├── feature/framework-core
├── feature/login
└── feature/employee
```

## Step 2 - Selesaikan Feature

Feature yang selesai kemudian di-merge ke master satu per satu.

```text
feature/framework-core  -> master
feature/login          -> master
feature/employee       -> master
```

Setelah merge, branch feature dapat dihapus.

```bash
git branch -d feature/login
```

## Step 3 - Buat Release

Ketika seluruh scope v1.0 selesai:

```bash
git checkout master
git checkout -b release/v1.0
```

```text
master
└── release/v1.0
```

## Step 4 - Testing Release

Saat QA melakukan testing, jika ditemukan bug, buat fix dari release:

```bash
git checkout release/v1.0
git checkout -b fix/login-timeout
```

```text
master
└── release/v1.0
      └── fix/login-timeout
```

Setelah selesai, fix di-merge kembali ke release.

```bash
git branch -d fix/login-timeout
```

## Step 5 - Release Resmi

Setelah release stabil:

```bash
git checkout master
git merge release/v1.0
```

Tambahkan tag versi:

```bash
git tag -a v1.0 -m "First stable release"
```

Lalu hapus branch release.

```bash
git branch -d release/v1.0
```

---

# Jika Setelah v1.0 Ada Bug

Jika bug ditemukan setelah release resmi, cukup buat branch fix dari master.

```text
master
└── fix/testrail-api
```

Setelah selesai:

```bash
git checkout master
git merge fix/testrail-api
```

Kemudian bisa dibuat tag versi perbaikan seperti:

```bash
git tag -a v1.0.1 -m "Bugfix release"
```

---

# Visualisasi Sederhana

```text
master
├── feature/framework-core
├── feature/login
├── feature/employee
└── release/v1.0
      └── fix/login-timeout
```

---

## Saran untuk Project Katalon Anda

Pola ini sangat cocok untuk project automation testing karena:

- branch tetap rapi dan mudah dipahami
- fitur baru dipisah dari bugfix
- release punya area khusus sebelum versi resmi
- proses testing dan stabilisasi lebih teratur

Workflow sederhananya adalah:

```text
feature/* -> master -> release/* -> master -> tag v1.x
```

Dengan pola ini, alur kerja menjadi lebih terstruktur, bugfix lebih jelas, dan release lebih aman sebelum ditandai sebagai versi resmi.
