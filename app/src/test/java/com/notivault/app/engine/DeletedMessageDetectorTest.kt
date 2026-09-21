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
        assertTrue(DeletedMessageDetector.isDeletedNotification("You deleted this message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("This message was deleted by the author"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Message deleted"))
    }

    @Test
    fun testMultilingualDeletedVariations() {
        // Spanish & Portuguese
        assertTrue(DeletedMessageDetector.isDeletedNotification("Este mensaje fue eliminado"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Esta mensagem foi apagada"))
        // French
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ce message a été supprimé"))
        // German
        assertTrue(DeletedMessageDetector.isDeletedNotification("Diese Nachricht wurde gelöscht"))
        // Hindi & Bengali
        assertTrue(DeletedMessageDetector.isDeletedNotification("এই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("यह संदेश हटा दिया गया था"))
    }

    @Test
    fun testMessengerUnsentVariations() {
        assertTrue(DeletedMessageDetector.isDeletedNotification("You unsent a message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Alice unsent a message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Bob unsent a message"))
    }

    @Test
    fun testExtractUnsentAuthor() {
        val author = DeletedMessageDetector.extractUnsentAuthor("Alice unsent a message", "Default")
        assertEquals("Alice", author)

        val selfAuthor = DeletedMessageDetector.extractUnsentAuthor("You unsent a message", "Default")
        assertEquals("Default", selfAuthor)
    }

    @Test
    fun testNonDeletedMessagesDoNotTrigger() {
        assertFalse(DeletedMessageDetector.isDeletedNotification("Hey, how are you?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("I deleted the file from my laptop"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Where are we meeting today?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification(""))
        assertFalse(DeletedMessageDetector.isDeletedNotification(null))
    }
}
