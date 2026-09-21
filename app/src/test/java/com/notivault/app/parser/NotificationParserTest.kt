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

    @Test
    fun testGroupChatWithParenthesesInTitle() {
        val (chatTitle, senderName, text) = NotificationParser.resolveTitleAndSender(
            packageName = "com.whatsapp",
            rawTitle = "Family Chat (Mom)",
            text = "Dinner is ready",
            conversationTitle = null,
            isGroup = true
        )

        assertEquals("Family Chat", chatTitle)
        assertEquals("Mom", senderName)
        assertEquals("Dinner is ready", text)
    }

    @Test
    fun testMultipleColonsInMessage() {
        val (chatTitle, senderName, text) = NotificationParser.resolveTitleAndSender(
            packageName = "com.whatsapp",
            rawTitle = "Project Alpha",
            text = "Alice: Update: Meeting moved to 4:00 PM: urgent",
            conversationTitle = "Project Alpha",
            isGroup = true
        )

        assertEquals("Project Alpha", chatTitle)
        assertEquals("Alice", senderName)
        assertEquals("Update: Meeting moved to 4:00 PM: urgent", text)
    }

    @Test
    fun testMediaIndicatingTextResolution() {
        val (chatTitle, senderName, text) = NotificationParser.resolveTitleAndSender(
            packageName = "com.whatsapp",
            rawTitle = "Ahmad",
            text = "📷 Sent a photo",
            conversationTitle = null,
            isGroup = false
        )

        assertEquals("Ahmad", chatTitle)
        assertEquals("Ahmad", senderName)
        assertEquals("📷 Sent a photo", text)
    }

    @Test
    fun testBengaliSenderTitleResolution() {
        val (chatTitle, senderName, text) = NotificationParser.resolveTitleAndSender(
            packageName = "com.whatsapp",
            rawTitle = "আহমদ",
            text = "কেমন আছেন?",
            conversationTitle = null,
            isGroup = false
        )

        assertEquals("আহমদ", chatTitle)
        assertEquals("আহমদ", senderName)
        assertEquals("কেমন আছেন?", text)
    }

    @Test
    fun testPlainTextDoesNotTriggerMedia() {
        assertFalse(NotificationParser.isMediaIndicatingText("Hi"))
        assertFalse(NotificationParser.isMediaIndicatingText("Hello, how are you?"))
        assertFalse(NotificationParser.isMediaIndicatingText("কেমন আছেন?"))
        assertFalse(NotificationParser.isMediaIndicatingText("I will call you later"))
    }

    @Test
    fun testMediaIndicatingKeywords() {
        assertTrue(NotificationParser.isMediaIndicatingText("📷 Sent a photo"))
        assertTrue(NotificationParser.isMediaIndicatingText("ছবি পাঠিয়েছেন"))
        assertTrue(NotificationParser.isMediaIndicatingText("video"))
        assertTrue(NotificationParser.isMediaIndicatingText("View once"))
    }
}
