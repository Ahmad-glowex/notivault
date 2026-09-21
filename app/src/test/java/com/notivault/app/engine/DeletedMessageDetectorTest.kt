package com.notivault.app.engine

import com.notivault.app.service.engine.DeletedMessageDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletedMessageDetectorTest {

    @Test
    fun testWhatsAppDeletedVariations() {
        assertTrue(DeletedMessageDetector.isDeletedNotification("This message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("this message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("This message was deleted."))
        assertTrue(DeletedMessageDetector.isDeletedNotification("You deleted this message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("This message was deleted by the author"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("This message was deleted by an admin"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Message deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Message was removed."))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Alice: This message was deleted"))
    }

    @Test
    fun testMultilingualDeletedVariations() {
        // Spanish & Portuguese
        assertTrue(DeletedMessageDetector.isDeletedNotification("Este mensaje fue eliminado"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Esta mensagem foi apagada"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Juan: Este mensaje fue eliminado"))
        // French
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ce message a été supprimé"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Claire: Ce message a été supprimé."))
        // German
        assertTrue(DeletedMessageDetector.isDeletedNotification("Diese Nachricht wurde gelöscht"))
        // Hindi & Bengali
        assertTrue(DeletedMessageDetector.isDeletedNotification("এই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মেসেজ মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ahmad: এই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("यह संदेश हटा दिया गया था"))
    }

    @Test
    fun testUnicodeAndInvisibleCharacterSanitization() {
        // WhatsApp commonly inserts LTR mark (\u200E) and non-breaking space
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u200EThis message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u200FThis message was deleted."))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\uFEFFThis message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u00A0This message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u200Eএই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ahmad 👍: \u200EThis message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("আহমদ: \u200Eএই বার্তাটি মুছে ফেলা হয়েছে"))
    }

    @Test
    fun testMessengerUnsentVariations() {
        assertTrue(DeletedMessageDetector.isDeletedNotification("You unsent a message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("You unsent a message."))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Alice unsent a message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Alice unsent a message."))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Bob unsent a message!"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Message was unsent"))
    }

    @Test
    fun testExtractUnsentAuthor() {
        val author1 = DeletedMessageDetector.extractUnsentAuthor("Alice unsent a message", "Default")
        assertEquals("Alice", author1)

        val author2 = DeletedMessageDetector.extractUnsentAuthor("Alice unsent a message.", "Default")
        assertEquals("Alice", author2)

        val author3 = DeletedMessageDetector.extractUnsentAuthor("Bob: This message was deleted", "Default")
        assertEquals("Bob", author3)

        val selfAuthor = DeletedMessageDetector.extractUnsentAuthor("You unsent a message.", "Default")
        assertEquals("Default", selfAuthor)
    }

    @Test
    fun testNonDeletedMessagesDoNotTrigger() {
        // Critical: conversational mentions of deletion MUST NOT trigger unsend logic
        assertFalse(DeletedMessageDetector.isDeletedNotification("Why was the message deleted?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Did you see that the message deleted by John?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Alice unsent a message yesterday when we were arguing"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Can you check if that message was removed from the chat?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("I deleted the file from my laptop"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Hey, how are you?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Where are we meeting today?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Send me the message"))
        assertFalse(DeletedMessageDetector.isDeletedNotification(""))
        assertFalse(DeletedMessageDetector.isDeletedNotification("   "))
        assertFalse(DeletedMessageDetector.isDeletedNotification(null))
    }
}
