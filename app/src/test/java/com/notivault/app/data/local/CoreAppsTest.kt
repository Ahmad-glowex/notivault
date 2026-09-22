package com.notivault.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreAppsTest {

    @Test
    fun testCorePackagesDefinitions() {
        assertTrue(CoreApps.isCoreApp("com.whatsapp"))
        assertTrue(CoreApps.isCoreApp("com.whatsapp.w4b"))
        assertTrue(CoreApps.isCoreApp("com.gbwhatsapp"))
        assertTrue(CoreApps.isCoreApp("com.facebook.orca"))
        assertTrue(CoreApps.isCoreApp("com.facebook.mlite"))
        assertTrue(CoreApps.isCoreApp("org.telegram.messenger"))
        assertTrue(CoreApps.isCoreApp("org.telegram.messenger.web"))
        assertTrue(CoreApps.isCoreApp("org.thunderdog.challegram"))
        assertTrue(CoreApps.isCoreApp("org.telegram.plus"))
        assertTrue(CoreApps.isCoreApp("com.instagram.android"))
        assertTrue(CoreApps.isCoreApp("com.instagram.lite"))

        // Non-core apps must NOT be treated as core
        assertFalse(CoreApps.isCoreApp("com.openai.chatgpt"))
        assertFalse(CoreApps.isCoreApp("com.google.android.gm"))
        assertFalse(CoreApps.isCoreApp("com.muse.app"))
        assertFalse(CoreApps.isCoreApp("com.grove.app"))
        assertFalse(CoreApps.isCoreApp("com.discord"))
    }

    @Test
    fun testIsWhatsAppMatching() {
        assertTrue(CoreApps.isWhatsApp("com.whatsapp"))
        assertTrue(CoreApps.isWhatsApp("com.whatsapp.w4b"))
        assertFalse(CoreApps.isWhatsApp("com.facebook.orca"))
        assertFalse(CoreApps.isWhatsApp("org.telegram.messenger"))
    }

    @Test
    fun testDefaultAppsSeeding() {
        val defaults = CoreApps.DEFAULT_APPS
        assertEquals(5, defaults.size)

        val packageNames = defaults.map { it.packageName }.toSet()
        assertTrue(packageNames.contains("com.whatsapp"))
        assertTrue(packageNames.contains("com.whatsapp.w4b"))
        assertTrue(packageNames.contains("com.facebook.orca"))
        assertTrue(packageNames.contains("org.telegram.messenger"))
        assertTrue(packageNames.contains("com.instagram.android"))

        // All defaults must be enabled initially
        assertTrue(defaults.all { it.isEnabled })
    }

    @Test
    fun testDefaultColors() {
        assertEquals("#25D366", CoreApps.getDefaultColor("com.whatsapp"))
        assertEquals("#25D366", CoreApps.getDefaultColor("com.whatsapp.w4b"))
        assertEquals("#0084FF", CoreApps.getDefaultColor("com.facebook.orca"))
        assertEquals("#229ED9", CoreApps.getDefaultColor("org.telegram.messenger"))
        assertEquals("#E1306C", CoreApps.getDefaultColor("com.instagram.android"))
        assertEquals("#0D9488", CoreApps.getDefaultColor("com.custom.app"))
        assertEquals("#0D9488", CoreApps.getDefaultColor("org.thoughtcrime.securesms"))
    }

    @Test
    fun testWhatsAppBusinessClassification() {
        assertTrue(CoreApps.isWhatsApp(CoreApps.PACKAGE_WHATSAPP))
        assertTrue(CoreApps.isWhatsApp(CoreApps.PACKAGE_WHATSAPP_W4B))
        assertTrue(CoreApps.isCoreApp(CoreApps.PACKAGE_WHATSAPP_W4B))
    }
}
