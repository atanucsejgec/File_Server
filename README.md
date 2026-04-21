# Local Share 📱➡️💻

## A Professional Android Local File Server

---

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![NanoHTTPD](https://img.shields.io/badge/NanoHTTPD-Engine-FF6B6B?style=for-the-badge)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![API](https://img.shields.io/badge/Min%20API-26-orange?style=for-the-badge)

**Turn your Android device into a blazing-fast wireless file server.**
Access, manage, stream, and transfer your files from any browser
on your local network — no cables, no apps, no limits.

[![Download APK](https://img.shields.io/badge/⬇️%20Download-Latest%20APK-brightgreen?style=for-the-badge&logo=android)](https://drive.google.com/file/d/1Pcy3T98kIuYMelswqC-w-M6W7IuWy4-y/view?usp=sharing)

[✨ Features](#-features) • [🏗️ Architecture](#%EF%B8%8F-architecture) • [📡 API Reference](#-api-reference) • [⚡ Performance](#-performance-tuning) • [📲 Download](#-download--install)

</div>

---

## 📲 Download & Install

<div align="center">

### ⬇️ Get the Latest APK

[![Download APK](https://img.shields.io/badge/Download-APK%20Latest-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://drive.google.com/file/d/1Pcy3T98kIuYMelswqC-w-M6W7IuWy4-y/view?usp=sharing)

> 🔗 **Direct Link:** [https://drive.google.com/file/d/1Pcy3T98kIuYMelswqC-w-M6W7IuWy4-y/view?usp=sharing](https://drive.google.com/file/d/1Pcy3T98kIuYMelswqC-w-M6W7IuWy4-y/view?usp=sharing)

</div>

### 📋 Installation Steps

```
Step 1: Click the Download button above
│
▼
Step 2: Open Google Drive link on your Android device
│
▼
Step 3: Tap the Download icon (⬇️) in Google Drive
│
▼
Step 4: Go to Settings → Security
Enable "Install from Unknown Sources"
│
▼
Step 5: Open the downloaded .apk file
│
▼
Step 6: Tap "Install" and wait for completion
│
▼
Step 7: Open Local Share ✅
Connect PC browser to http://YOUR_PHONE_IP:8080
```

### 📱 Requirements

| Item | Requirement |
|---|---|
| 📱 **Android Version** | Android 8.0 (API 26) or higher |
| 💾 **Storage Permission** | Required (to browse files) |
| 📶 **Network** | Same Wi-Fi network as your PC |
| 🌐 **Browser** | Any modern browser (Chrome, Firefox, Edge) |

---

## ✨ Features

### 🗂️ File Management
| Feature | Description |
|---|---|
| 📂 **Browse** | Navigate your entire Android storage from any browser |
| 🔍 **Search** | Global file search across all directories (2+ char query) |
| 📁 **Create Folder** | Make new directories instantly |
| ✏️ **Rename** | Rename files and folders with conflict resolution |
| 🗑️ **Delete** | Remove files or entire folder trees |
| ⬆️ **Upload** | Drag-and-drop multi-file uploads with unique name handling |

### 📡 Streaming & Download
| Feature | Description |
|---|---|
| 🎬 **Media Streaming** | Stream videos and music directly in the browser |
| ⏩ **Range Requests** | Seek support for video/audio playback |
| 📦 **ZIP on-the-fly** | Compress and download multiple files without temp storage |
| 🗜️ **Folder ZIP** | Archive entire directories as a stream |
| 🖼️ **Thumbnails** | Live image and video preview generation |

### 🔐 Security
| Feature | Description |
|---|---|
| 🔑 **Password Protection** | Optional login page with session cookie management |
| 🎫 **UUID Session Tokens** | Secure, rotating session authentication |
| 🛡️ **Path Sanitization** | Prevents directory traversal attacks |
| 🌐 **CORS Support** | Configurable cross-origin request headers |

### 📊 Analytics
| Feature | Description |
|---|---|
| 💾 **Storage Stats** | Real-time disk usage for all storage volumes |
| 📈 **Usage Percent** | Visual progress indicators for storage consumption |
| 📋 **File Metadata** | Size, MIME type, last modified date, extension, and icon |

---

## 🏗️ Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Android Application                       │
│  ┌─────────────────┐         ┌──────────────────────────────┐   │
│  │  Jetpack Compose│◄────────│       Event Callbacks        │   │
│  │      UI Layer   │         │  • onTransferComplete()      │   │
│  │                 │         │  • onClientConnected()       │   │
│  │  • File Browser │         │  • onClientDisconnected()    │   │
│  │  • Upload Panel │         └──────────────┬───────────────┘   │
│  │  • Storage Stats│                        │                   │
│  └─────────────────┘                        │                   │
└─────────────────────────────────────────────────────────────────┘
```

### Core Components

```
com.apk.fileserver/
├── 📄 LocalFileServer.kt       ← NanoHTTPD core, router, all handlers
├── 📄 WebInterface.kt          ← HTML/CSS/JS for the browser UI
├── utils/
│   ├── 📄 FileUtils.kt         ← File ops, MIME, search, storage roots
│   ├── 📄 TransferRecord.kt    ← Transfer event data model
│   └── 📄 TransferType.kt      ← UPLOAD / DOWNLOAD enum
└── ui/
└── 📄 MainScreen.kt        ← Jetpack Compose UI
```

---

## 📡 API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/auth` | Password authentication |
| `GET` | `/api/list` | List directory contents |
| `GET` | `/api/search` | Search files globally |
| `GET` | `/api/storage` | Disk usage analytics |
| `POST` | `/api/mkdir` | Create new folder |
| `POST` | `/api/delete` | Delete file or folder |
| `POST` | `/api/rename` | Rename file or folder |
| `POST` | `/api/upload` | Upload files |
| `POST` | `/api/zip` | ZIP multiple files (streamed) |
| `GET` | `/api/zipfolder` | ZIP entire folder (streamed) |
| `GET` | `/files/{path}` | Stream or download file |
| `GET` | `/thumb/{path}` | Image thumbnail |

---

## ⚡ Performance Tuning

### Buffer Configuration

```kotlin
const val FILE_BUFFER = 1024 * 1024        // 1 MB  - File I/O
const val ZIP_BUFFER  = 512  * 1024        // 512 KB - ZIP compression
const val PIPE_BUFFER = 8    * 1024 * 1024 // 8 MB  - Stream pipe
```

### Smart Compression

Already-compressed formats (`mp4`, `mp3`, `jpg`, `zip`, `apk`, etc.)
use `STORED` mode — **skipping CPU-wasting re-compression**.

---

## 🔒 Security Notes

```
⚠️  Designed for LOCAL NETWORK USE ONLY.
    Do not expose port 8080 to the public internet.
```

| Protection | Implementation |
|---|---|
| 🛡️ **Path Traversal** | Validates all paths start with `/storage/` |
| 🔑 **Session Auth** | UUID tokens, cookie-based tracking |
| 📁 **Root Isolation** | Scoped to ExternalStorageDirectory |

---

## 🗺️ Roadmap

- [ ] 🎬 Video thumbnail generation
- [ ] 📱 QR code for easy connection
- [ ] 🔒 HTTPS with self-signed certificate
- [ ] 📊 Transfer speed monitoring
- [ ] 🌙 Dark / Light mode web UI toggle

---

## 📄 License

MIT License — Copyright (c) 2024 Local Share

---

<div align="center">

**Made with ❤️ and Kotlin**

[![Download APK](https://img.shields.io/badge/⬇️%20Download-Latest%20APK-brightgreen?style=for-the-badge&logo=android)](https://drive.google.com/file/d/1Pcy3T98kIuYMelswqC-w-M6W7IuWy4-y/view?usp=sharing)

⭐ Star this repo if Local Share saved you a USB cable ⭐

</div>
```
