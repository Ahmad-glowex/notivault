# NotiVault Permissions & Setup Guide

To function properly as a notification vault and deleted message logger, NotiVault requires explicit Android system authorizations.

---

## 1. Notification Listener Permission (Mandatory)

Android isolates notification access for user privacy. Unlike normal app permissions requested at runtime via a simple dialog, Notification Access requires opening Android's Special App Access settings.

### How to Grant:
1. Open NotiVault.
2. In the top banner, tap **"Grant Permission"** (or go to **Settings > Notification Interception > Grant Notification Access**).
3. Android will open the **Device & app notifications** screen.
4. Locate **NotiVault** in the list and toggle the switch to **Allowed**.
5. Read Android's security notice and confirm **Allow**.

> **Note**: NotiVault does not have internet access, so none of your intercepted notifications can ever leave your phone.

---

## 2. Media & Storage Access (For Media Observer)

To monitor and cache photos, videos, and voice notes before senders delete them:
- On **Android 13+ (API 33+)**: Grant **Photos and videos** and **Music and audio** permissions when prompted.
- On **Android 12 and below**: Grant **Storage** permission.

---

## 3. Battery Optimization Exemption (Recommended for Background Reliability)

Aggressive OEM battery managers (such as Xiaomi MIUI/HyperOS, Samsung One UI, Oppo/Vivo ColorOS) may terminate background services when the screen is locked.

### To ensure 24/7 background capture:
1. Go to your phone's **Settings > Apps > NotiVault > Battery**.
2. Select **Unrestricted** (or **Don't optimize**).
3. On devices with "Autostart" settings (Xiaomi, Vivo, Realme), ensure **Autostart** is enabled for NotiVault.

---

## 4. Troubleshooting

| Issue | Resolution |
| :--- | :--- |
| **Notifications stop logging after phone sleep** | Disable battery optimization for NotiVault and toggle the Notification Listener permission off and on once. |
| **Deleted message badge not showing up** | Ensure the notification was received while NotiVault was running. If the chat app was open in the foreground, Android may not generate a notification. |
| **WhatsApp view-once images not captured** | WhatsApp view-once media is protected by `FLAG_SECURE` and never touches public storage. See [EPHEMERAL_MEDIA_ARCHITECTURE.md](EPHEMERAL_MEDIA_ARCHITECTURE.md) for full technical details. |
