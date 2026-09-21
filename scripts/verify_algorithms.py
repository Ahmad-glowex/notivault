#!/usr/bin/env python3
"""
Algorithm & Heuristics Verification Test Suite for NotiVault
Tests DeletedMessageDetector regexes, NotificationParser sender separation, and CSV exporter logic across all languages.
"""

import re
import unicodedata
import unittest

DELETED_PATTERNS = [
    # English (WhatsApp, Messenger, Instagram, Telegram)
    re.compile(r"^(?:.+?:\s+)?(?:This message was deleted|You deleted this message|This message was deleted by (?:the author|an admin|admin|you)|(?:.+?\s+)?unsent a message|Message was unsent|Message was deleted|Message deleted|Message was removed|This media was deleted|This photo was deleted|This video was deleted|Deleted message|This message has been deleted)[.!]?$", re.IGNORECASE),

    # Bengali (WhatsApp, Messenger, IMO) - handles both \u09DF and \u09AF\u09BC
    re.compile(r"^(?:.+?:\s+)?(?:(?:এই\s+)?(?:বার্তাটি|মেসেজটি|মেসেজ|লেখাটি)\s+(?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে দেওয়া হয়েছে|মুছে দেয়া হয়েছে|মুছে ফেলেছেন)|(?:.+?\s+)?একটি বার্তা (?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে ফেলেছেন)|আপনি একটি বার্তা (?:মুছে ফেলেছেন|মুছে দিয়েছেন|মুছে দিয়েছেন)|বার্তা মুছে ফেলা হয়েছে|বার্তা মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে)[.!]?$", re.IGNORECASE),

    # Hindi
    re.compile(r"^(?:.+?:\s+)?(?:(?:यह\s+)?संदेश हटा दिया गया(?: था)?|आपने एक संदेश हटा दिया|(?:.+?\s+)?ने एक संदेश वापस ले लिया|एक संदेश वापस ले लिया|यह संदेश लेखक द्वारा हटा दिया गया(?: था)?)[.!]?$", re.IGNORECASE),

    # Arabic
    re.compile(r"^(?:.+?:\s+)?(?:تم\s+حذف\s+(?:هذه\s+الرسالة|الرسالة|هذا\s+المحتوى|هذه\s+الصورة|هذا\s+الفيديو)|تم\s+[إا]لغاء\s+[إا]رسال\s+الرسالة|(?:.+?\s+)?[أا]لغ[ىيت]\s+[إا]رسال\s+رسالة|(?:.+?\\s+)?قام(?:ت)?\s+ب[إا]لغاء\s+[إا]رسال\s+رسالة|حذف(?:ت)?\s+هذه\s+الرسالة|قمت\s+بحذف\s+هذه\s+الرسالة|تم\s+حذف\s+هذه\s+الرسالة\s+بواسطة\s+(?:المشرف|المؤلف)|تم\s+حذف\s+هذه\s+الرسالة\s+من\s+قبلك)[.!]?$", re.IGNORECASE),

    # Urdu & Persian
    re.compile(r"^(?:.+?:\s+)?(?:یہ\s+پیغام\s+حذف\s+کر\s+دیا\s+گیا(?: تھا)?|این\s+پیغام\s+حذف\s+شد|پیغام\s+(?:حذف|ڈیلیٹ)\s+(?:کر\s+دیا\s+گیا|کیا\s+گیا|شد)|(?:.+?\s+)?نے\s+(?:ایک\s+پیغام\s+واپس\s+لے\s+لیا|پیغام\s+حذف\s+کر\s+دیا)|(?:آپ\s+نے\s+)?(?:ایک\s+پیغام\s+واپس\s+لے\s+لیا|یہ\s+پیغام\s+حذف\s+کر\s+دیا)|یہ\s+پیغام\s+مصنف\s+کی\s+طرف\s+سے\s+حذف\s+کر\s+دیا\s+گیا(?: تھا)?)[.!]?$", re.IGNORECASE),

    # Russian
    re.compile(r"^(?:.+?:\s+)?(?:Сообщение удалено|Это сообщение было удалено|Данное сообщение удалено|Вы удалили это сообщение|(?:.+?\s+)?отменил(?:а)?\s+отправку сообщения|Сообщение было удалено|Это сообщение было удалено автором|Это сообщение удалено)[.!]?$", re.IGNORECASE),

    # Turkish
    re.compile(r"^(?:.+?:\s+)?(?:Bu mesaj silindi|Bu mesaj silinmiştir|Bu mesajı sildiniz|(?:.+?\s+)?bir mesajın gönderimini geri aldı|Mesaj silindi|Bu mesaj yönetici tarafından silindi|Bu mesaj yazar tarafından silindi)[.!]?$", re.IGNORECASE),

    # Chinese (Simplified & Traditional)
    re.compile(r"^(?:.+?[:：]\s*)?(?:此消息已撤回|此消息已被删除|此消息已被撤回|(?:.+?\s*)?撤回了一条消息|你撤回了一条消息|已撤回一条消息|该消息已被删除|此訊息已撤回|此訊息已被刪除|(?:.+?\s*)?撤回了一則訊息|你撤回了一則訊息|已撤回一則訊息)[.!]?$", re.IGNORECASE),

    # Japanese
    re.compile(r"^(?:.+?[:：]\s*)?(?:メッセージの送信を取り消しました|このメッセージは削除されました|メッセージが削除されました|送信を取り消しました|あなたがメッセージの送信を取り消しました|(?:.+?\s*)?がメッセージの送信を取り消しました)[.!]?$", re.IGNORECASE),

    # Spanish
    re.compile(r"^(?:.+?:\s+)?(?:Este mensaje fue eliminado|Se eliminó este mensaje|Eliminaste este mensaje|(?:.+?\s+)?(?:eliminó|ha eliminado|anuló el envío de)\s+un mensaje|Mensaje eliminado|Este mensaje fue eliminado por (?:el autor|un administrador))[.!]?$", re.IGNORECASE),

    # Portuguese
    re.compile(r"^(?:.+?:\s+)?(?:Esta mensagem foi apagada|Você apagou esta mensagem|(?:.+?\s+)?(?:anulou o envio de|apagou)\s+uma mensagem|apagou esta mensagem|Mensagem apagada|Esta mensagem foi apagada pelo autor)[.!]?$", re.IGNORECASE),

    # French
    re.compile(r"^(?:.+?:\s+)?(?:Ce message a été supprimé|Vous avez supprimé ce message|(?:.+?\s+)?a annulé l'envoi d'un message|Message supprimé|Ce message a été supprimé par (?:l'auteur|un administrateur))[.!]?$", re.IGNORECASE),

    # German
    re.compile(r"^(?:.+?:\s+)?(?:Diese Nachricht wurde gelöscht|Du hast diese Nachricht gelöscht|(?:.+?\s+)?hat eine Nachricht zurückgerufen|Diese Nachricht wurde vom Verfasser gelöscht|Diese Nachricht wurde von einem Admin gelöscht|Nachricht gelöscht)[.!]?$", re.IGNORECASE),

    # Italian
    re.compile(r"^(?:.+?:\s+)?(?:Questo messaggio è stato eliminato|Hai eliminato questo messaggio|(?:.+?\s+)?ha annullato l'invio di un messaggio|Messaggio eliminato|Questo messaggio è stato eliminato dall'autore)[.!]?$", re.IGNORECASE),
]

UNSENT_AUTHOR_PATTERNS = [
    re.compile(r"^(?:(.+?)\s+unsent a message)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+একটি বার্তা মুছে ফেলেছেন)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+a annulé l'envoi d'un message)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+(?:eliminó|ha eliminado|anuló el envío de)\s+un mensaje)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+(?:anulou o envio de|apagou)\s+uma mensagem)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+hat eine Nachricht zurückgerufen)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+[أا]لغ[ىيت]\s+[إا]رسال\s+رسالة)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+قام(?:ت)?\s+ب[إا]لغاء\s+[إا]رسال\s+رسالة)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+نے\s+(?:ایک\s+پیغام\s+واپس\s+لے\s+لیا|پیغام\s+حذف\s+کر\s+دیا))[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+отменил(?:а)?\s+отправку сообщения)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)\s+bir mesajın gönderimini geri aldı)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)(?:已)?撤回了一[条則]消息)[.!]?$", re.IGNORECASE),
    re.compile(r"^(?:(.+?)がメッセージの送信を取り消しました)[.!]?$", re.IGNORECASE),
    re.compile(r"^([^:：]+)[:：]\s*(?:.+)$", re.IGNORECASE)
]

SELF_PRONOUNS = {
    "you", "vous", "tú", "tu", "você", "voce", "du", "আপনি", "আপ", "आप",
    "انت", "أنت", "آپ", "вы", "ты", "sen", "你", "自分"
}

GROUP_TITLE_SENDER_PATTERN = re.compile(r"^(.+?)\s*\((.+?)\)$")

def sanitize(text: str | None) -> str:
    if not text:
        return ""
    without_invisibles = re.sub(r"[\u200B-\u200F\uFEFF\u202A-\u202E\u2060-\u2069\u061C\u00A0\u202F\u180E]", "", text).strip()
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
    clean_raw_title = sanitize(raw_title)
    clean_text = sanitize(text)
    clean_conv_title = sanitize(conv_title) if conv_title else None

    if clean_conv_title and clean_conv_title.strip():
        chat_title = clean_conv_title.strip()
        if clean_raw_title.strip() and clean_raw_title != clean_conv_title:
            sender_name = clean_raw_title.strip()
            msg_text = clean_text
        elif ": " in clean_text:
            parts = clean_text.split(": ", 1)
            sender_name = parts[0].strip()
            msg_text = parts[1].strip()
        elif "：" in clean_text:
            parts = clean_text.split("：", 1)
            sender_name = parts[0].strip()
            msg_text = parts[1].strip()
        else:
            sender_name = clean_raw_title.strip() or chat_title
            msg_text = clean_text
    else:
        group_match = GROUP_TITLE_SENDER_PATTERN.match(clean_raw_title.strip())
        if group_match:
            chat_title = group_match.group(1).strip()
            sender_name = group_match.group(2).strip()
            msg_text = clean_text
        elif is_group and ": " in clean_text:
            parts = clean_text.split(": ", 1)
            chat_title = clean_raw_title.strip() or "Group Chat"
            sender_name = parts[0].strip()
            msg_text = parts[1].strip()
        elif is_group and "：" in clean_text:
            parts = clean_text.split("：", 1)
            chat_title = clean_raw_title.strip() or "Group Chat"
            sender_name = parts[0].strip()
            msg_text = parts[1].strip()
        else:
            chat_title = clean_raw_title.strip() or "Direct Message"
            sender_name = chat_title
            msg_text = clean_text
    return chat_title, sender_name, msg_text

def escape_csv(val: str) -> str:
    escaped = val.replace('"', '""').replace('\r\n', ' ').replace('\n', ' ').replace('\r', ' ')
    return f'"{escaped}"'


def deduplicate_messages(messages: list[dict], tolerance_ms: int = 15000) -> list[dict]:
    from collections import defaultdict
    groups = defaultdict(list)
    for msg in messages:
        key = (msg["thread_id"], msg["sender_name"], msg["message_text"])
        groups[key].append(msg)

    kept_ids = set()
    for key, group in groups.items():
        sorted_group = sorted(group, key=lambda m: m["timestamp"])
        base = sorted_group[0]
        kept_ids.add(base["id"])
        for candidate in sorted_group[1:]:
            if abs(candidate["timestamp"] - base["timestamp"]) <= tolerance_ms:
                # duplicate dropped
                pass
            else:
                base = candidate
                kept_ids.add(base["id"])
    return [m for m in messages if m["id"] in kept_ids]


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
        # Arabic
        self.assertTrue(is_deleted_notification("تم حذف هذه الرسالة"))
        self.assertTrue(is_deleted_notification("تم إلغاء إرسال الرسالة"))
        self.assertTrue(is_deleted_notification("تم الغاء ارسال الرسالة"))
        self.assertTrue(is_deleted_notification("ألغى إرسال رسالة"))
        self.assertTrue(is_deleted_notification("أحمد: تم حذف هذه الرسالة"))
        self.assertTrue(is_deleted_notification("أحمد ألغى إرسال رسالة"))
        self.assertTrue(is_deleted_notification("تم حذف هذا المحتوى"))

        # Urdu
        self.assertTrue(is_deleted_notification("یہ پیغام حذف کر دیا گیا تھا"))
        self.assertTrue(is_deleted_notification("یہ پیغام حذف کر دیا گیا"))
        self.assertTrue(is_deleted_notification("پیغام حذف کیا گیا"))
        self.assertTrue(is_deleted_notification("احمد: یہ پیغام حذف کر دیا گیا تھا"))
        self.assertTrue(is_deleted_notification("احمد نے ایک پیغام واپس لے لیا"))

        # Russian
        self.assertTrue(is_deleted_notification("Сообщение удалено"))
        self.assertTrue(is_deleted_notification("Это сообщение было удалено"))
        self.assertTrue(is_deleted_notification("Вы удалили это сообщение"))
        self.assertTrue(is_deleted_notification("Иван: Сообщение удалено"))

        # Turkish
        self.assertTrue(is_deleted_notification("Bu mesaj silindi"))
        self.assertTrue(is_deleted_notification("Bu mesajı sildiniz"))
        self.assertTrue(is_deleted_notification("Ahmet: Bu mesaj silindi"))

        # Chinese
        self.assertTrue(is_deleted_notification("此消息已撤回"))
        self.assertTrue(is_deleted_notification("此消息已被删除"))
        self.assertTrue(is_deleted_notification("张三撤回了一条消息"))
        self.assertTrue(is_deleted_notification("张三: 此消息已撤回"))

        # Japanese
        self.assertTrue(is_deleted_notification("メッセージの送信を取り消しました"))
        self.assertTrue(is_deleted_notification("このメッセージは削除されました"))

        # European (Spanish, Portuguese, French, German, Italian)
        self.assertTrue(is_deleted_notification("Este mensaje fue eliminado"))
        self.assertTrue(is_deleted_notification("Esta mensagem foi apagada"))
        self.assertTrue(is_deleted_notification("Juan: Este mensaje fue eliminado"))
        self.assertTrue(is_deleted_notification("Ce message a été supprimé"))
        self.assertTrue(is_deleted_notification("Claire: Ce message a été supprimé."))
        self.assertTrue(is_deleted_notification("Diese Nachricht wurde gelöscht"))
        self.assertTrue(is_deleted_notification("Questo messaggio è stato eliminato"))

        # Bengali & Hindi
        self.assertTrue(is_deleted_notification("এই বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("এই বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("মেসেজটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("আহমদ একটি বার্তা মুছে ফেলেছেন"))
        self.assertTrue(is_deleted_notification("यह संदेश हटा दिया गया था"))
        self.assertTrue(is_deleted_notification("यह संदेश हटा दिया गया"))

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
        self.assertEqual(extract_unsent_author("أحمد ألغى إرسال رسالة", "Default"), "أحمد")
        self.assertEqual(extract_unsent_author("احمد نے ایک پیغام واپس لے لیا", "Default"), "احمد")
        self.assertEqual(extract_unsent_author("Иван отменил отправку сообщения", "Default"), "Иван")
        self.assertEqual(extract_unsent_author("Ahmet bir mesajın gönderimini geri aldı", "Default"), "Ahmet")
        self.assertEqual(extract_unsent_author("张三撤回了一条消息", "Default"), "张三")

    def test_non_deleted_messages(self):
        # Critical negative test cases across languages
        self.assertFalse(is_deleted_notification("Hey there, how are you doing?"))
        self.assertFalse(is_deleted_notification("Why was the message deleted?"))
        self.assertFalse(is_deleted_notification("Did you see that the message deleted by John?"))
        self.assertFalse(is_deleted_notification("Alice unsent a message yesterday when we were arguing"))
        self.assertFalse(is_deleted_notification("Can you check if that message was removed from the chat?"))
        self.assertFalse(is_deleted_notification("I deleted the old repo yesterday"))
        self.assertFalse(is_deleted_notification("Send me the message"))
        self.assertFalse(is_deleted_notification("مرحبا كيف حالك"))
        self.assertFalse(is_deleted_notification("هل حذفت الرسالة من هاتفك؟"))
        self.assertFalse(is_deleted_notification("کیا آپ نے پیغام دیکھا؟"))
        self.assertFalse(is_deleted_notification("Привет, как дела?"))
        self.assertFalse(is_deleted_notification("Merhaba nasılsın"))
        self.assertFalse(is_deleted_notification("今天天气真好"))
        self.assertFalse(is_deleted_notification("こんにちは、お元気ですか"))
        self.assertFalse(is_deleted_notification("কেমন আছেন ভাই?"))
        self.assertFalse(is_deleted_notification("आप कैसे हैं?"))
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

    def test_unicode_invisible_marks(self):
        self.assertTrue(is_deleted_notification("\u200EThis message was deleted"))
        self.assertTrue(is_deleted_notification("\u200FThis message was deleted."))
        self.assertTrue(is_deleted_notification("\uFEFFThis message was deleted"))
        self.assertTrue(is_deleted_notification("\u00A0This message was deleted"))
        self.assertTrue(is_deleted_notification("\u202FThis message was deleted"))
        self.assertTrue(is_deleted_notification("\u2068This message was deleted\u2069"))
        self.assertTrue(is_deleted_notification("\u200Eএই বার্তাটি মুছে ফেলা হয়েছে"))
        self.assertTrue(is_deleted_notification("Ahmad: \u200EThis message was deleted"))
        self.assertTrue(is_deleted_notification("\u2068أحمد\u2069: \u2068تم حذف هذه الرسالة\u2069"))
        self.assertTrue(is_deleted_notification("\u061Cتم حذف هذه الرسالة"))

    def test_csv_escaping(self):
        self.assertEqual(escape_csv("Hello, World!"), '"Hello, World!"')
        self.assertEqual(escape_csv('He said "hello".'), '"He said ""hello""."')
        self.assertEqual(escape_csv("Line 1\nLine 2\rLine 3"), '"Line 1 Line 2 Line 3"')
        self.assertEqual(escape_csv("Line 1\r\nLine 2"), '"Line 1 Line 2"')

    def test_message_deduplication_15s_tolerance(self):
        msgs = [
            {"id": 1, "thread_id": "com.whatsapp_Alice", "sender_name": "Alice", "message_text": "Hi", "timestamp": 1000},
            {"id": 2, "thread_id": "com.whatsapp_Alice", "sender_name": "Alice", "message_text": "Hi", "timestamp": 3000}, # duplicate (2s apart)
            {"id": 3, "thread_id": "com.whatsapp_Alice", "sender_name": "Alice", "message_text": "Hi", "timestamp": 14000}, # duplicate (13s from base)
            {"id": 4, "thread_id": "com.whatsapp_Alice", "sender_name": "Alice", "message_text": "Hi", "timestamp": 30000}, # NOT duplicate (29s from base)
            {"id": 5, "thread_id": "com.whatsapp_Alice", "sender_name": "Alice", "message_text": "Sent a photo", "timestamp": 35000},
            {"id": 6, "thread_id": "com.whatsapp_Alice", "sender_name": "Alice", "message_text": "Sent a photo", "timestamp": 36000}, # duplicate (1s apart)
        ]
        result = deduplicate_messages(msgs, tolerance_ms=15000)
        result_ids = [m["id"] for m in result]
        self.assertEqual(result_ids, [1, 4, 5])


if __name__ == '__main__':
    unittest.main()
