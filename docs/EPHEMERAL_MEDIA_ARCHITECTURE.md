# Ephemeral Media & View-Once Architecture Deep-Dive

## 1. Executive Summary

A core question in modern mobile forensics and privacy logging is: **Can an unprivileged Android application automatically capture and save "View-Once" or ephemeral media from messaging applications like WhatsApp, Instagram Direct, or Telegram?**

This document details the low-level Android OS security mechanisms that govern ephemeral media, compares the technical boundaries of unprivileged notification listeners vs. root-level hooks, and documents the exact architecture employed by **NotiVault**.

---

## 2. Android OS Sandboxing & Process Isolation

Android's security architecture enforces isolation at several fundamental operating system layers:

```
+-------------------------------------------------------------------+
|                        Applications Layer                         |
|  +---------------------------+     +---------------------------+  |
|  |   WhatsApp / Instagram    |     |         NotiVault         |  |
|  |   (UID: u0_a145)          |     |         (UID: u0_a289)    |  |
|  +-------------+-------------+     +-------------+-------------+  |
+----------------|---------------------------------|----------------+
                 |                                 |
+----------------v---------------------------------v----------------+
|                    Linux Kernel & SELinux Policy                  |
|  - Process UID isolation (/proc/<pid>/)                           |
|  - Enforced SELinux domains: untrusted_app                        |
|  - File permissions: /data/user/0/<pkg>/ restricted to own UID   |
+-------------------------------------------------------------------+
```

### 2.1. Linux Kernel UID Separation
Each Android application is assigned a unique Linux User ID (`UID`) upon installation (e.g. `u0_a145`). Standard POSIX file permissions (`0700` or `rwx------`) prevent any other application from reading the app’s private internal storage located at:
```
/data/user/0/<package_name>/
/data/data/<package_name>/
```
No process running under another UID (`u0_a289`) can traverse or read files within this path unless the device is rooted or the application runs with shared UIDs (which requires the same signing key).

### 2.2. SELinux Enforcing Mode
Android runs SELinux in `enforcing` mode under the `untrusted_app` domain. Even if a local file descriptor leak were to occur, SELinux policy rules restrict file access, binder calls, and memory mapping across disparate application contexts.

---

## 3. How "View-Once" / Ephemeral Media Functions Under the Hood

When a sender transmits a "View-Once" photo or video via WhatsApp (Signal Protocol) or Instagram Direct:

```
[Sender Device]
      │  (Encrypt with ephemeral symmetric key)
      ▼
[Server Relay (Encrypted Blob)]
      │  (Delivered via Push Notification / WebSockets)
      ▼
[Recipient WhatsApp Process]
      │
      ├─ 1. In-Memory Key Decryption
      │     Key is held in private process RAM (JVM heap / native memory).
      │
      ├─ 2. Storage Strategy
      │     Standard media: Written to public / shared storage (Android/media/...)
      │     View-Once media: Written to encrypted cache or /data/data/com.whatsapp/
      │     NEVER exposed to Android MediaStore or public external storage.
      │
      ├─ 3. Rendering via Display Pipeline
      │     Window is flagged with WindowManager.LayoutParams.FLAG_SECURE.
      │
      └─ 4. Zeroization upon Dismissal
            Once viewed, the ciphertext/decrypted cache file is unlinked (deleted)
            and the in-memory decryption key is zeroized.
```

### 3.1. Absence from Public `MediaStore`
Normal media attachments are downloaded into the shared external directory:
```
/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/
```
In contrast, **View-Once media is intentionally excluded from the public MediaStore provider and public storage directories**. The client writes the payload into its private app sandbox or streams directly into volatile RAM.

### 3.2. Window Security (`FLAG_SECURE`)
When rendering a View-Once image, the activity window sets:
```kotlin
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```
At the OS compositor layer (`SurfaceFlinger`), `FLAG_SECURE` instructs the display pipeline to:
1. Blank out window contents in screen capture APIs (`MediaProjection`, `AccessibilityService`, `DisplayManager`).
2. Blank out window snapshots in the Android "Recents" task switcher.
3. Blank out output transmitted over non-secure wireless displays (Miracast/Chromecast without HDCP).

---

## 4. How Community Tools & Exploit Frameworks Approach View-Once Media

| Approach | Root Required? | Safety / Security | Reliability | Mechanism |
| :--- | :---: | :---: | :---: | :--- |
| **NotiVault (Public Observer)** | **NO** | **100% Safe & Private** | Permanent | Intercepts standard public media & notification previews. Completely honors OS security boundaries. |
| **Notification Image Previews** | **NO** | **100% Safe** | High | Extracts inline thumbnail bitstreams from `NotificationCompat.MessagingStyle` when populated by the OS. |
| **LSPosed / Xposed Modules** | **YES** | **High Risk** (Ban risk) | Fragile | Runtime Dalvik/ART bytecode hooks on `is_view_once` boolean flags inside the WhatsApp APK. |
| **Frida Memory Injection** | **YES** | **High Risk** | Developer only | Attaches to target PID, hooks OpenSSL/BoringSSL crypto routines to dump decrypted buffers. |
| **Hardware Screen Capture** | **NO** | External hardware | High | External physical camera pointing at device screen. |

### 4.1. Why Modded Clients (WA Enhancer / GBWhatsApp) are Hazardous
Third-party modified clients and Xposed modules intercept View-Once media by modifying client-side bytecode before rendering. However:
- They require disabling Android verified boot (`dm-verity`) and unlocking the bootloader.
- They trigger Play Integrity API / SafetyNet failures.
- WhatsApp actively detects modified runtimes, resulting in permanent telephone number bans.
- Modded APKs often package closed-source trojans and spyware that exfiltrate chat databases.

---

## 5. NotiVault's Safe Caching Architecture

NotiVault strictly adheres to the principle of **Zero-Compromise Security**: no rooting, no closed-source binaries, and **zero internet permissions**.

### 5.1. Dual-Engine Media Detection
To safely preserve all allowable media before the sender can recall or delete it:

1. **`MediaStoreObserver` (ContentObserver)**:
   - Registers listeners on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`.
   - Fires asynchronously the moment a chat app commits an incoming media attachment to shared storage.

2. **`MediaFileObserver` (Inotify / FileObserver)**:
   - Targets messaging media directories:
     * `/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/`
     * `/storage/emulated/0/Telegram/`
     * `/storage/emulated/0/Pictures/Telegram/`
   - Monitors `CREATE` and `CLOSE_WRITE` events.

3. **Atomic Internal Ingestion (`MediaCacheManager`)**:
   - Immediately duplicates the incoming file into NotiVault's private storage (`context.filesDir/saved_media/`).
   - If the sender subsequently chooses "Delete for Everyone" in WhatsApp, the sender's command unlinks the file in WhatsApp's directory, but **NotiVault's internal copy remains intact and accessible**.

---

## 6. Summary of Architectural Limits

- **Standard Deleted Messages & Media**: Fully captured, tagged with deleted timestamps, and preserved offline.
- **Normal Photos, Videos & Voice Notes**: Fully backed up in real time before sender revocation.
- **View-Once / Ephemeral Media**: Guarded by OS-level `FLAG_SECURE` and private sandboxing. Unprivileged apps without root cannot and should not bypass Android's cryptographic boundaries.
