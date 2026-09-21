# NotiVault System Architecture & Technical Specifications

NotiVault is an open-source, zero-network, on-device notification logger, deleted message recovery engine, and media backup vault for Android.

---

## 1. System Overview & Component Block Diagram

```
+───────────────────────────────────────────────────────────────────────────────+
|                               ANDROID SYSTEM                                  |
|                                                                               |
|  [WhatsApp / Messenger / Instagram / Telegram]                                |
|          │ (New message or "Message deleted" notification)                    |
|          ▼                                                                    |
|  [NotificationManagerService] ───(StatusBarNotification)───┐                  |
+────────────────────────────────────────────────────────────│──────────────────+
                                                             │
+────────────────────────────────────────────────────────────▼──────────────────+
|                           NOTIVAULT APPLICATION                               |
|                                                                               |
|  +─────────────────────────────────────────────────────────────────────────+  |
|  |                     NotiVaultListenerService                            |  |
|  |   - Package Filter (WhatsApp, Messenger, Instagram, Telegram)           |  |
|  |   - NotificationParser (MessagingStyle & Bundle Extraction)             |  |
|  |   - DeletedMessageDetector (Multilingual Unsend/Revoke Heuristics)      |  |
|  +────────────────────────────────────┬────────────────────────────────────+  |
|                                       │                                       |
|                                       ▼                                       |
|  +─────────────────────────────────────────────────────────────────────────+  |
|  |                      MessageRepository Layer                            |  |
|  |   - Coroutine / Flow Pipelines                                          |  |
|  |   - Atomic Thread & Message Upsert                                      |  |
|  |   - Deleted Message Correlation Engine                                  |  |
|  +────────────────────────────────────┬────────────────────────────────────+  |
|                                       │                                       |
|                                       ▼                                       |
|  +─────────────────────────────────────────────────────────────────────────+  |
|  |                       Jetpack Room Database                             |  |
|  |   - monitored_apps    (AppEntity)                                       |  |
|  |   - chat_threads      (ChatThreadEntity)                                |  |
|  |   - messages          (MessageEntity: text, timestamp, isDeleted, etc.) |  |
|  |   - saved_media       (MediaEntity: fileSizeBytes, mimeType, paths)     |  |
|  +─────────────────────────────────────────────────────────────────────────+  |
|                                                                               |
|  +───────────────────────────────+     +───────────────────────────────────+  |
|  |     MediaObserverService      |     |        DataExporter Engine        |  |
|  |   - MediaStoreObserver        |     |   - JSON serialization            |  |
|  |   - MediaFileObserver         |     |   - RFC 4180 CSV export           |  |
|  |   - MediaCacheManager         |     |   - FileProvider share intent     |  |
|  +───────────────────────────────+     +───────────────────────────────────+  |
|                                                                               |
|  +─────────────────────────────────────────────────────────────────────────+  |
|  |                     UI Presentation Layer (M3)                          |  |
|  |   - Jetpack Compose + Navigation Compose                                |  |
|  |   - HomeScreen (App Tabs, Search, Thread List, Live Stats)              |  |
|  |   - ChatDetailScreen (Message Bubbles, Deleted Badges, Export)          |  |
|  |   - MediaGalleryScreen (AsyncImage Grid, Fullscreen Viewer)             |  |
|  |   - SettingsScreen (Biometric Auth, Permissions, Storage Management)   |  |
|  +─────────────────────────────────────────────────────────────────────────+  |
+───────────────────────────────────────────────────────────────────────────────+
```

---

## 2. Notification Parsing & Deleted Message Heuristics

### 2.1. The Interception Pipeline
1. When a notification event arrives from Android's `NotificationListenerService`, `NotiVaultListenerService.onNotificationPosted(sbn)` is invoked.
2. `NotificationParser.parse(sbn)` inspects `notification.extras`:
   - Checks `NotificationCompat.EXTRA_MESSAGES` to unwrap individual message bundles (supporting multi-message conversation styling).
   - If absent, inspects `EXTRA_TITLE`, `EXTRA_BIG_TEXT`, and `EXTRA_TEXT`.
   - Resolves group conversation titles via `EXTRA_CONVERSATION_TITLE` and parses `Sender: Message` patterns.

### 2.2. Deleted Message Matching Sequence
When a user deletes a message ("Delete for Everyone" on WhatsApp or "Unsend" on Messenger):
1. The messaging application updates or posts a notification event stating:
   - *"This message was deleted"* (WhatsApp)
   - *"Alice unsent a message"* (Messenger)
   - *"Ce message a été supprimé"* (French)
   - *"Esta mensagem foi apagada"* (Portuguese/Spanish)
2. `DeletedMessageDetector` identifies the revocation signature.
3. The engine extracts the author (`extractUnsentAuthor`) and resolves the chat thread.
4. `MessageRepository.markDeletedBySender` queries the Room database for the most recent active (`isDeleted = 0`) message in that thread matching the author.
5. Upon match:
   - The original message text is **preserved unmodified**.
   - `isDeleted` is set to `true`.
   - `deletedTimestamp` is set to current epoch milliseconds.
   - The UI immediately renders a distinct `DeletedBadge` chip over the message bubble.

---

## 3. Database Schema

### 3.1. `monitored_apps` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `packageName` | `TEXT` (PK) | App package name (e.g. `com.whatsapp`) |
| `appName` | `TEXT` | Human-readable app name |
| `isEnabled` | `INTEGER` | 1 if monitored, 0 if ignored |
| `colorHex` | `TEXT` | Hex code for app branding in UI |
| `totalMessages` | `INTEGER` | Running count of intercepted messages |
| `lastActivityTimestamp`| `INTEGER` | Epoch timestamp of last notification |

### 3.2. `chat_threads` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `threadId` | `TEXT` (PK) | Composite identifier `${packageName}_${chatTitle}` |
| `packageName` | `TEXT` | Originating application package (Indexed) |
| `chatTitle` | `TEXT` | Name of contact or group |
| `isGroup` | `INTEGER` | Boolean flag for group conversation |
| `lastMessageText` | `TEXT` | Snippet for thread preview |
| `lastMessageTimestamp`| `INTEGER` | Epoch millis (Indexed for sort order) |
| `unreadCount` | `INTEGER` | Unread notifications count |
| `isPinned` | `INTEGER` | Boolean flag for pinned conversations |
| `isMuted` | `INTEGER` | Boolean flag for muted conversations |

### 3.3. `messages` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `INTEGER` (PK auto) | Primary autoincrement ID |
| `threadId` | `TEXT` (FK) | References `chat_threads.threadId` |
| `packageName` | `TEXT` | Indexed package identifier |
| `senderName` | `TEXT` | Sender name extracted from notification |
| `messageText` | `TEXT` | Raw message body |
| `timestamp` | `INTEGER` | Post timestamp (Indexed) |
| `isDeleted` | `INTEGER` | 1 if sender deleted this message, 0 otherwise |
| `deletedTimestamp` | `INTEGER` | Epoch millis when deletion notice occurred |
| `originalNotificationKey` | `TEXT` | Android SBN notification key |
| `hasMedia` | `INTEGER` | 1 if media attachment detected |
| `mediaUri` | `TEXT` | URI to media if applicable |

### 3.4. `saved_media` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `INTEGER` (PK auto) | Media autoincrement ID |
| `threadId` | `TEXT` | Associated thread identifier |
| `packageName` | `TEXT` | Originating app package |
| `originalPath` | `TEXT` | Public MediaStore or filesystem path |
| `internalSavedPath` | `TEXT` | Protected private storage destination path |
| `fileName` | `TEXT` | File name with extension |
| `mimeType` | `TEXT` | MIME type (e.g. `image/jpeg`, `video/mp4`) |
| `fileSizeBytes` | `INTEGER` | File size in bytes |
| `timestamp` | `INTEGER` | Ingestion timestamp |
| `mediaType` | `TEXT` | "IMAGE", "VIDEO", "AUDIO", or "DOCUMENT" |

---

## 4. Zero-Network Privacy Architecture

NotiVault is designed with zero-trust privacy principles:
- **No Internet Permission**: `android.permission.INTERNET` is completely absent from `AndroidManifest.xml`.
- **Compile-time Guarantee**: The OS sandbox prevents NotiVault from initiating any socket, HTTP, or DNS traffic.
- **Local Biometric Gating**: AndroidX `BiometricPrompt` protects access with device fingerprint, face, or PIN.
- **Window Masking**: Optional `FLAG_SECURE` prevents other background screen recorders or the Android Recents switcher from capturing log contents.
