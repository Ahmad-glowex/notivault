#!/usr/bin/env python3
"""
Algorithm & Heuristics Verification Test Suite for NotiVault
Tests DeletedMessageDetector regexes, NotificationParser sender separation, and CSV exporter logic.
"""

import re
import unicodedata
import unittest

DELETED_PATTERNS = [
    # English
    re.compile(r"^(?:.+?:\s+)?(?:This message was deleted|You deleted this message|This message was deleted by (?:the author|an admin|admin|you)|(?:.+?\s+)?unsent a message|Message was unsent|Message was deleted|Message deleted|Message was removed|This media was deleted|This photo was deleted|This video was deleted|Deleted message|This message has been deleted)[.!]?$", re.IGNORECASE),
    # Bengali (WhatsApp, Messenger, IMO) - handles both \u09DF and \u09AF\u09BC
    re.compile(r"^(?:.+?:\s+)?(?:(?:এই\s+)?(?:বার্তাটি|মেসেজটি|মেসেজ|লেখাটি)\s+(?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে দেওয়া হয়েছে|মুছে দেয়া হয়েছে|মুছে ফেলেছেন)|একটি বার্তা (?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে ফেলেছেন)|আপনি একটি বার্তা (?:মুছে ফেলেছেন|মুছে দিয়েছেন|মুছে দিয়েছেন)|বার্তা মুছে ফেলা হয়েছে|বার্তা মুছে ফেলা হয়েছে)[.!]?$", re.IGNORECASE),
    # Hindi
    re.compile(r"^(?:.+?:\s+)?(?:(?:यह\s+)?संदेश हटा दिया गया(?: था)?|आपने एक संदेश हटा दिया)[.!]?$", re.IGNORECASE),
    # Spanish
    re.compile(r"^(?:.+?:\s+)?(?:Este mensaje fue eliminado|Se eliminó este mensaje|Eliminaste este mensaje|.+?\s+eliminó un mensaje)[.!]?$", re.IGNORECASE),
    # Portuguese
    re.compile(r"^(?:.+?:\s+)?(?:Esta mensagem foi apagada|Você apagou esta mensagem|.+?\s+anulou o envio de uma mensagem)[.!]?$", re.IGNORECASE),
    # French
    re.compile(r"^(?:.+?:\s+)?(?:Ce message a été supprimé|Vous avez supprimé ce message|.+?\s+a annulé l'envoi d'un message)[.!]?$", re.IGNORECASE),
    # German
    re.compile(r"^(?:.+?:\s+)?(?:Diese Nachricht wurde gelöscht|Du hast diese Nachricht gelöscht|.+?\s+hat eine Nachricht zurückgerufen)[.!]?$", re.IGNORECASE),
    # Arabic
    re.compile(r"^(?:.+?:\s+)?(?:تم حذف هذه الرسالة|تم إلغاء إرسال الرسالة|تم حذف الرسالة)[.!]?$", re.IGNORECASE),
    # Urdu
    re.compile(r"^(?:.+?:\s+)?(?:یہ پیغام حذف کر دیا گیا(?: تھا)?)[.!]?$", re.IGNORECASE)
]

UNSENT_AUTHOR_PATTERNS = [
    re.compile(r"^(?:(.+?)\s+unsent a message)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+একটি বার্তা মুছে ফেলেছেন)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+a annulé l'envoi d'un message)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+eliminó un mensaje)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+anulou o envio de uma mensagem)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+hat eine Nachricht zurückgerufen)[.!]?$", re.IGNORECASE),
    re.compile(r"^([^:]+):\s+(?:এই বার্তাটি|মেসেজটি|This message was deleted|You deleted this message|Message deleted|Este mensaje|Esta mensagem|Ce message|Diese Nachricht)[.!]?$", re.IGNORECASE)
]

SELF_PRONOUNS = {"you", "vous", "tú", "tu", "você", "voce", "du", "আপনি"}

GROUP_TITLE_SENDER_PATTERN = re.compile(r"^(.+?)\s*\((.+?)\)$")

def sanitize(text: str | None) -> str:
    if not text:
        return ""
    without_invisibles = re.sub(r"[\u200B-\u200F\uFEFF\u202A-\u202E\u00A0]", "", text).strip()
    normalized = unicodedata.normalize("NFC", without_invisibles)
    return normalized.replace("\u09AF\u09BC", "\u09DF")

def is_deleted_notification(text: str | None) -> bool:
    clean = sanitize(text)
    if not clean:
        return False
    return any(p.match(clean) is not None for p in DELETED_PATTERNS)

def extract_unsent_author(text: str | None, fallback: str) -> str:
    clean = sanitize(text)
    if not clean:
        return fallback
    for pattern in UNSENT_AUTHOR_PATTERNS:
        m = pattern.match(clean)
        if m:
            extracted = m.group(1).strip()
            if extracted and extracted.lower() not in SELF_PRONOUNS:
                return extracted
    return fallback

def resolve_title_and_sender(pkg: str, raw_title: str, text: str, conv_title: str | None, is_group: bool):
    if conv_title and conv_title.strip():
        chat_title = conv_title.strip()
        if raw_title.strip() and raw_title != conv_title:
            sender_name = raw_title.strip()
            msg_text = text
        elif ": " in text:
            parts = text.split(": ", 1)
            sender_name = parts[0].strip()
            msg_text = parts[1].strip()
        else:
            sender_name = raw_title.strip() or chat_title
            msg_text = text
    else:
        group_match = GROUP_TITLE_SENDER_PATTERN.match(raw_title.strip())
        if group_match:
            chat_title = group_match.group(1).strip()
            sender_name = group_match.group(2).strip()
            msg_text = text
        elif is_group and ": " in text:
            parts = text.split(": ", 1)
            chat_title = raw_title.strip() or "Group Chat"
            sender_name = parts[0].strip()
            msg_text = parts[1].strip()
        else:
            chat_title = raw_title.strip() or "Direct Message"
            sender_name = chat_title
            msg_text = text
    return chat_title, sender_name, msg_text

def escape_csv(val: str) -> str:
    escaped = val.replace('"', '""').replace('\r\n', ' ').replace('\n', ' ').replace('\r', ' ')
    return f'"{escaped}"'


class TestNotiVaultAlgorithms(unittest.TestCase):

    def test_whatsapp_detection(self):
        self.assertTrue(is_deleted_notification("This message was deleted"))
        self.assertTrue(is_deleted_notification("this message was deleted"))
        self.assertTrue(is_deleted_notification("This message was deleted."))
        self.assertTrue(is_deleted_notification("You deleted this message"))
        self.assertTrue(is_deleted_notification("This message was deleted by the author"))
        self.assertTrue(is_deleted_notification("This message was deleted by an admin"))
        self.assertTrue(is_deleted_notification("Message deleted"))
        self.assertTrue(is_deleted_notification("Message was removed."))
        self.assertTrue(is_deleted_notification("Alice: This message was deleted"))

    def test_multilingual_detection(self):
        self.assertTrue(is_deleted_notification("Este mensaje fue eliminado"))
        self.assertTrue(is_deleted_notification("Esta mensagem foi apagada"))
        self.assertTrue(is_deleted_notification("Juan: Este mensaje fue eliminado"))
        self.assertTrue(is_deleted_notification("Ce message a été supprimé"))
        self.assertTrue(is_deleted_notification("Claire: Ce message a été supprimé."))
        self.assertTrue(is_deleted_notification("Diese Nachricht wurde gelöscht"))
        self.assertTrue(is_deleted_notification("বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("यह संदेश हटा दिया गया था"))

    def test_messenger_detection_and_author_extraction(self):
        self.assertTrue(is_deleted_notification("You unsent a message"))
        self.assertTrue(is_deleted_notification("You unsent a message."))
        self.assertTrue(is_deleted_notification("Alice unsent a message"))
        self.assertTrue(is_deleted_notification("Alice unsent a message."))
        self.assertEqual(extract_unsent_author("Alice unsent a message", "Default"), "Alice")
        self.assertEqual(extract_unsent_author("Alice unsent a message.", "Default"), "Alice")
        self.assertEqual(extract_unsent_author("Bob: This message was deleted", "Default"), "Bob")
        self.assertEqual(extract_unsent_author("You unsent a message", "Default"), "Default")
        self.assertEqual(extract_unsent_author("You unsent a message.", "Default"), "Default")

    def test_non_deleted_messages(self):
        # Critical negative test cases
        self.assertFalse(is_deleted_notification("Hey there, how are you doing?"))
        self.assertFalse(is_deleted_notification("Why was the message deleted?"))
        self.assertFalse(is_deleted_notification("Did you see that the message deleted by John?"))
        self.assertFalse(is_deleted_notification("Alice unsent a message yesterday when we were arguing"))
        self.assertFalse(is_deleted_notification("Can you check if that message was removed from the chat?"))
        self.assertFalse(is_deleted_notification("I deleted the old repo yesterday"))
        self.assertFalse(is_deleted_notification("Send me the message"))
        self.assertFalse(is_deleted_notification(""))
        self.assertFalse(is_deleted_notification("   "))
        self.assertFalse(is_deleted_notification(None))

    def test_notification_parser_direct_message(self):
        title, sender, text = resolve_title_and_sender("com.whatsapp", "John Doe", "Hello!", None, False)
        self.assertEqual(title, "John Doe")
        self.assertEqual(sender, "John Doe")
        self.assertEqual(text, "Hello!")

    def test_notification_parser_group_with_prefix(self):
        title, sender, text = resolve_title_and_sender("com.whatsapp", "Alpha Team", "Bob: Meeting at 3pm", "Alpha Team", True)
        self.assertEqual(title, "Alpha Team")
        self.assertEqual(sender, "Bob")
        self.assertEqual(text, "Meeting at 3pm")

    def test_notification_parser_multiple_colons(self):
        title, sender, text = resolve_title_and_sender("com.whatsapp", "Alpha Team", "Alice: Note: Meeting at 3pm: urgent", "Alpha Team", True)
        self.assertEqual(title, "Alpha Team")
        self.assertEqual(sender, "Alice")
        self.assertEqual(text, "Note: Meeting at 3pm: urgent")

    def test_notification_parser_group_parentheses(self):
        title, sender, text = resolve_title_and_sender("com.whatsapp", "Family Chat (Mom)", "Dinner is ready", None, True)
        self.assertEqual(title, "Family Chat")
        self.assertEqual(sender, "Mom")
        self.assertEqual(text, "Dinner is ready")

    def test_hindi_bengali_deleted(self):
        self.assertTrue(is_deleted_notification("এই বার্তাটি মুছে ফেলা হয়েছে")) # \u09af\u09bc
        self.assertTrue(is_deleted_notification("এই বার্তাটি মুছে ফেলা হয়েছে")) # \u09df
        self.assertTrue(is_deleted_notification("বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("মেসেজটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("Ahmad: এই বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("Ahmad 👍: This message was deleted"))
        self.assertTrue(is_deleted_notification("আপনি একটি বার্তা মুছে ফেলেছেন"))
        self.assertEqual(extract_unsent_author("Ahmad: এই বার্তাটি মুছে ফেলা হয়েছে", "Ahmad"), "Ahmad")
        self.assertEqual(extract_unsent_author("আহমদ একটি বার্তা মুছে ফেলেছেন", "Fallback"), "আহমদ")
        self.assertTrue(is_deleted_notification("यह संदेश हटा दिया गया था"))

    def test_unicode_invisible_marks(self):
        self.assertTrue(is_deleted_notification("\u200EThis message was deleted"))
        self.assertTrue(is_deleted_notification("\u200FThis message was deleted."))
        self.assertTrue(is_deleted_notification("\uFEFFThis message was deleted"))
        self.assertTrue(is_deleted_notification("\u00A0This message was deleted"))
        self.assertTrue(is_deleted_notification("\u200Eএই বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("Ahmad: \u200EThis message was deleted"))

    def test_csv_escaping(self):
        self.assertEqual(escape_csv("Hello, World!"), '"Hello, World!"')
        self.assertEqual(escape_csv('He said "hello".'), '"He said ""hello""."')
        self.assertEqual(escape_csv("Line 1\nLine 2\rLine 3"), '"Line 1 Line 2 Line 3"')
        self.assertEqual(escape_csv("Line 1\r\nLine 2"), '"Line 1 Line 2"')


if __name__ == '__main__':
    unittest.main()
