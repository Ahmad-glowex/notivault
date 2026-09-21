package com.notivault.app.parser

import com.notivault.app.service.parser.NotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {

    @Test
    fun testDirectMessageTitleResolution() {
        val (chatTitle, senderName, text) = NotificationParser.resolveTitleAndSender(
            packageName = "com.whatsapp",
            rawTitle = "John Doe",
            text = "Hey there! Are you coming?",
            conversationTitle = null,
            isGroup = false
        )

        assertEquals("John Doe", chatTitle)
        assertEquals("John Doe", senderName)
        assertEquals("Hey there! Are you coming?", text)
    }

    @Test
    fun testGroupChatWithConversationTitleAndPrefix() {
        val (chatTitle, senderName, text) = NotificationParser.resolveTitleAndSender(
            packageName = "com.whatsapp",
            rawTitle = "Project Alpha",
            text = "Sarah: The PR is ready for review",
            conversationTitle = "Project Alpha",
            isGroup = true
        )

        assertEquals("Project Alpha", chatTitle)
        assertEquals("Sarah", senderName)
        assertEquals("The PR is ready for review", text)
    }

    @Test
    fun testGroupChatWithoutExplicitConversationTitle() {
        val (chatTitle, senderName, text) = NotificationParser.resolveTitleAndSender(
            packageName = "com.whatsapp",
            rawTitle = "Study Group",
            text = "Mark: Let's meet at 5 PM",
            conversationTitle = null,
            isGroup = true
        )

        assertEquals("Study Group", chatTitle)
        assertEquals("Mark", senderName)
        assertEquals("Let's meet at 5 PM", text)
    }
}
