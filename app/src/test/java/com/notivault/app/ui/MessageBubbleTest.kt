package com.notivault.app.ui

import com.notivault.app.ui.screens.chat.components.isMediaPlaceholderText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageBubbleTest {

    @Test
    fun testIsMediaPlaceholderText() {
        // View Once variations
        assertTrue(isMediaPlaceholderText("📷 ① Sent a photo"))
        assertTrue(isMediaPlaceholderText("① Sent a photo"))
        assertTrue(isMediaPlaceholderText("① Photo"))
        assertTrue(isMediaPlaceholderText("📷 ① Photo"))
        assertTrue(isMediaPlaceholderText("①"))
        assertTrue(isMediaPlaceholderText("📷 ①"))
        assertTrue(isMediaPlaceholderText("📷 ① ছবি"))
        assertTrue(isMediaPlaceholderText("① ছবি"))
        assertTrue(isMediaPlaceholderText("একবার দেখার ছবি"))
        assertTrue(isMediaPlaceholderText("একবার দেখার ভিডিও"))
        assertTrue(isMediaPlaceholderText("View Once"))
        assertTrue(isMediaPlaceholderText("View once photo"))
        assertTrue(isMediaPlaceholderText("View once video"))
        assertTrue(isMediaPlaceholderText("🎥 ① Sent a video"))
        assertTrue(isMediaPlaceholderText("① Sent a video"))
        assertTrue(isMediaPlaceholderText("① Video"))

        // Standard media placeholders
        assertTrue(isMediaPlaceholderText("📷 Sent a photo"))
        assertTrue(isMediaPlaceholderText("Sent a photo"))
        assertTrue(isMediaPlaceholderText("📷 Photo"))
        assertTrue(isMediaPlaceholderText("Photo"))
        assertTrue(isMediaPlaceholderText("ছবি"))
        assertTrue(isMediaPlaceholderText("🎥 Sent a video"))
        assertTrue(isMediaPlaceholderText("Video"))
        assertTrue(isMediaPlaceholderText("ভিডিও"))
        assertTrue(isMediaPlaceholderText(""))
        assertTrue(isMediaPlaceholderText("   "))

        // Real user messages should NOT be considered placeholders
        assertFalse(isMediaPlaceholderText("Hello, how are you?"))
        assertFalse(isMediaPlaceholderText("Check this photo out!"))
        assertFalse(isMediaPlaceholderText("Here is the meeting notes video"))
        assertFalse(isMediaPlaceholderText("আজকে মিটিং আছে"))
    }
}
