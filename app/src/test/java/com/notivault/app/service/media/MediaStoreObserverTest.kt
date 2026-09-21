package com.notivault.app.service.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreObserverTest {

    @Test
    fun testScreenshotsAreBlacklisted() {
        val path = "/storage/emulated/0/DCIM/Screenshots/Screenshot_20260921.png"
        val relPath = "DCIM/Screenshots/"
        val name = "Screenshot_20260921.png"

        assertTrue(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertNull(MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testCameraPhotosAreBlacklisted() {
        val path = "/storage/emulated/0/DCIM/Camera/IMG_20260921_123456.jpg"
        val relPath = "DCIM/Camera/"
        val name = "IMG_20260921_123456.jpg"

        assertTrue(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertNull(MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testDownloadsAreBlacklisted() {
        val path = "/storage/emulated/0/Download/homework.pdf"
        val relPath = "Download/"
        val name = "homework.pdf"

        assertTrue(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertNull(MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testWhatsAppMediaIsWhitelisted() {
        val path = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/IMG-20260921-WA0001.jpg"
        val relPath = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/"
        val name = "IMG-20260921-WA0001.jpg"

        assertFalse(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertEquals("com.whatsapp", MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testWhatsAppBusinessMediaIsWhitelisted() {
        val path = "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images/IMG.jpg"
        val relPath = "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images/"
        val name = "IMG.jpg"

        assertFalse(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertEquals("com.whatsapp.w4b", MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testTelegramMediaIsWhitelisted() {
        val path = "/storage/emulated/0/Pictures/Telegram/telegram_photo.jpg"
        val relPath = "Pictures/Telegram/"
        val name = "telegram_photo.jpg"

        assertFalse(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertEquals("org.telegram.messenger", MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testMessengerMediaIsWhitelisted() {
        val path = "/storage/emulated/0/Pictures/Messenger/messenger_photo.jpg"
        val relPath = "Pictures/Messenger/"
        val name = "messenger_photo.jpg"

        assertFalse(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertEquals("com.facebook.orca", MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testUnknownRandomPathIgnored() {
        val path = "/storage/emulated/0/Documents/notes.txt"
        val relPath = "Documents/"
        val name = "notes.txt"

        assertNull(MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testWhatsAppSentMediaIsBlacklisted() {
        val path = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent/IMG-20260921-WA0001.jpg"
        val relPath = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent/"
        val name = "IMG-20260921-WA0001.jpg"

        assertTrue(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertNull(MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testTelegramRootFolderWhitelisted() {
        val path = "/storage/emulated/0/Telegram/Telegram Images/photo_20260921.jpg"
        val relPath = "Telegram/Telegram Images/"
        val name = "photo_20260921.jpg"

        assertFalse(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertEquals("org.telegram.messenger", MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }

    @Test
    fun testQrCodeScreenshotBlacklisted() {
        val path = "/storage/emulated/0/DCIM/Screenshots/Screenshot_WhatsApp_QR_2026.png"
        val relPath = "DCIM/Screenshots/"
        val name = "Screenshot_WhatsApp_QR_2026.png"

        assertTrue(MediaStoreObserver.isBlacklisted(path, relPath, name))
        assertNull(MediaStoreObserver.resolveMessagingPackage(path, relPath, name))
    }
}
