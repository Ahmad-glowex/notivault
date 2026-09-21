package com.notivault.app.service.engine

import java.text.Normalizer
import java.util.regex.Pattern

object DeletedMessageDetector {

    // Anchored regex patterns for various languages & platforms.
    // Handles optional sender prefix (e.g. "Alice: This message was deleted") and optional trailing punctuation.
    private val DELETED_PATTERNS = listOf(
        // English (WhatsApp, Messenger, Instagram, Telegram)
        Pattern.compile("^(?:.+?:\\s+)?(?:This message was deleted|You deleted this message|This message was deleted by (?:the author|an admin|admin|you)|(?:.+?\\s+)?unsent a message|Message was unsent|Message was deleted|Message deleted|Message was removed|This media was deleted|This photo was deleted|This video was deleted|Deleted message|This message has been deleted)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Bengali (WhatsApp, Messenger, IMO) - handles both \u09DF and \u09AF\u09BC
        Pattern.compile("^(?:.+?:\\s+)?(?:(?:এই\\s+)?(?:বার্তাটি|মেসেজটি|মেসেজ|লেখাটি)\\s+(?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে দেওয়া হয়েছে|মুছে দেয়া হয়েছে|মুছে ফেলেছেন)|(?:.+?\\s+)?একটি বার্তা (?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে ফেলেছেন)|আপনি একটি বার্তা (?:মুছে ফেলেছেন|মুছে দিয়েছেন|মুছে দিয়েছেন)|বার্তা মুছে ফেলা হয়েছে|বার্তা মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Hindi
        Pattern.compile("^(?:.+?:\\s+)?(?:(?:यह\\s+)?संदेश हटा दिया गया(?: था)?|आपने एक संदेश हटा दिया|(?:.+?\\s+)?ने एक संदेश वापस ले लिया|एक संदेश वापस ले लिया|यह संदेश लेखक द्वारा हटा दिया गया(?: था)?)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Arabic
        Pattern.compile("^(?:.+?:\\s+)?(?:تم\\s+حذف\\s+(?:هذه\\s+الرسالة|الرسالة|هذا\\s+المحتوى|هذه\\s+الصورة|هذا\\s+الفيديو)|تم\\s+[إا]لغاء\\s+[إا]رسال\\s+الرسالة|(?:.+?\\s+)?[أا]لغ[ىيت]\\s+[إا]رسال\\s+رسالة|(?:.+?\\s+)?قام(?:ت)?\\s+ب[إا]لغاء\\s+[إا]رسال\\s+رسالة|حذف(?:ت)?\\s+هذه\\s+الرسالة|قمت\\s+بحذف\\s+هذه\\s+الرسالة|تم\\s+حذف\\s+هذه\\s+الرسالة\\s+بواسطة\\s+(?:المشرف|المؤلف)|تم\\s+حذف\\s+هذه\\s+الرسالة\\s+من\\s+قبلك)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Urdu & Persian
        Pattern.compile("^(?:.+?:\\s+)?(?:یہ\\s+پیغام\\s+حذف\\s+کر\\s+دیا\\s+گیا(?: تھا)?|این\\s+پیغام\\s+حذف\\s+شد|پیغام\\s+(?:حذف|ڈیلیٹ)\\s+(?:کر\\s+دیا\\s+گیا|کیا\\s+گیا|شد)|(?:.+?\\s+)?نے\\s+(?:ایک\\s+پیغام\\s+واپس\\s+لے\\s+لیا|پیغام\\s+حذف\\s+کر\\s+دیا)|(?:آپ\\s+نے\\s+)?(?:ایک\\s+پیغام\\s+واپس\\s+لے\\s+لیا|یہ\\s+پیغام\\s+حذف\\s+کر\\s+دیا)|یہ\\s+پیغام\\s+مصنف\\s+کی\\s+طرف\\s+سے\\s+حذف\\s+کر\\s+دیا\\s+گیا(?: تھا)?)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Russian
        Pattern.compile("^(?:.+?:\\s+)?(?:Сообщение удалено|Это сообщение было удалено|Данное сообщение удалено|Вы удалили это сообщение|(?:.+?\\s+)?отменил(?:а)?\\s+отправку сообщения|Сообщение было удалено|Это сообщение было удалено автором|Это сообщение удалено)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Turkish
        Pattern.compile("^(?:.+?:\\s+)?(?:Bu mesaj silindi|Bu mesaj silinmiştir|Bu mesajı sildiniz|(?:.+?\\s+)?bir mesajın gönderimini geri aldı|Mesaj silindi|Bu mesaj yönetici tarafından silindi|Bu mesaj yazar tarafından silindi)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Chinese (Simplified & Traditional)
        Pattern.compile("^(?:.+?[:：]\\s*)?(?:此消息已撤回|此消息已被删除|此消息已被撤回|(?:.+?\\s*)?撤回了一条消息|你撤回了一条消息|已撤回一条消息|该消息已被删除|此訊息已撤回|此訊息已被刪除|(?:.+?\\s*)?撤回了一則訊息|你撤回了一則訊息|已撤回一則訊息)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Japanese
        Pattern.compile("^(?:.+?[:：]\\s*)?(?:メッセージの送信を取り消しました|このメッセージは削除されました|メッセージが削除されました|送信を取り消しました|あなたがメッセージの送信を取り消しました|(?:.+?\\s*)?がメッセージの送信を取り消しました)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Spanish
        Pattern.compile("^(?:.+?:\\s+)?(?:Este mensaje fue eliminado|Se eliminó este mensaje|Eliminaste este mensaje|(?:.+?\\s+)?(?:eliminó|ha eliminado|anuló el envío de)\\s+un mensaje|Mensaje eliminado|Este mensaje fue eliminado por (?:el autor|un administrador))[.!]?$", Pattern.CASE_INSENSITIVE),

        // Portuguese
        Pattern.compile("^(?:.+?:\\s+)?(?:Esta mensagem foi apagada|Você apagou esta mensagem|(?:.+?\\s+)?(?:anulou o envio de|apagou)\\s+uma mensagem|apagou esta mensagem|Mensagem apagada|Esta mensagem foi apagada pelo autor)[.!]?$", Pattern.CASE_INSENSITIVE),

        // French
        Pattern.compile("^(?:.+?:\\s+)?(?:Ce message a été supprimé|Vous avez supprimé ce message|(?:.+?\\s+)?a annulé l'envoi d'un message|Message supprimé|Ce message a été supprimé par (?:l'auteur|un administrateur))[.!]?$", Pattern.CASE_INSENSITIVE),

        // German
        Pattern.compile("^(?:.+?:\\s+)?(?:Diese Nachricht wurde gelöscht|Du hast diese Nachricht gelöscht|(?:.+?\\s+)?hat eine Nachricht zurückgerufen|Diese Nachricht wurde vom Verfasser gelöscht|Diese Nachricht wurde von einem Admin gelöscht|Nachricht gelöscht)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Italian
        Pattern.compile("^(?:.+?:\\s+)?(?:Questo messaggio è stato eliminato|Hai eliminato questo messaggio|(?:.+?\\s+)?ha annullato l'invio di un messaggio|Messaggio eliminato|Questo messaggio è stato eliminato dall'autore)[.!]?$", Pattern.CASE_INSENSITIVE)
    )

    private val UNSENT_AUTHOR_PATTERNS = listOf(
        // English
        Pattern.compile("^(?:(.+?)\\s+unsent a message)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Bengali
        Pattern.compile("^(?:(.+?)\\s+একটি বার্তা মুছে ফেলেছেন)[.!]?$", Pattern.CASE_INSENSITIVE),
        // French
        Pattern.compile("^(?:(.+?)\\s+a annulé l'envoi d'un message)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Spanish
        Pattern.compile("^(?:(.+?)\\s+(?:eliminó|ha eliminado|anuló el envío de)\\s+un mensaje)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Portuguese
        Pattern.compile("^(?:(.+?)\\s+(?:anulou o envio de|apagou)\\s+uma mensagem)[.!]?$", Pattern.CASE_INSENSITIVE),
        // German
        Pattern.compile("^(?:(.+?)\\s+hat eine Nachricht zurückgerufen)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Arabic
        Pattern.compile("^(?:(.+?)\\s+[أا]لغ[ىيت]\\s+[إا]رسال\\s+رسالة)[.!]?$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:(.+?)\\s+قام(?:ت)?\\s+ب[إا]لغاء\\s+[إا]رسال\\s+رسالة)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Urdu
        Pattern.compile("^(?:(.+?)\\s+نے\\s+(?:ایک\\s+پیغام\\s+واپس\\s+لے\\s+لیا|پیغام\\s+حذف\\s+کر\\s+دیا))[.!]?$", Pattern.CASE_INSENSITIVE),
        // Russian
        Pattern.compile("^(?:(.+?)\\s+отменил(?:а)?\\s+отправку сообщения)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Turkish
        Pattern.compile("^(?:(.+?)\\s+bir mesajın gönderimini geri aldı)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Chinese
        Pattern.compile("^(?:(.+?)(?:已)?撤回了一[条則]消息)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Japanese
        Pattern.compile("^(?:(.+?)がメッセージの送信を取り消しました)[.!]?$", Pattern.CASE_INSENSITIVE),
        // Generic sender: <deleted text>
        Pattern.compile("^([^:：]+)[:：]\\s*(?:.+)$", Pattern.CASE_INSENSITIVE)
    )

    private val SELF_PRONOUNS = setOf(
        "you", "vous", "tú", "tu", "você", "voce", "du", "আপনি", "আপ", "आप",
        "انت", "أنت", "آپ", "вы", "ты", "sen", "你", "自分"
    )

    /**
     * Sanitizes strings by removing invisible bidirectional/zero-width Unicode control characters,
     * isolates, and normalizing composed/decomposed glyphs commonly inserted by OEM keyboards,
     * Android System UI, and RTL scripts.
     */
    fun sanitize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val withoutInvisibles = text.replace(
            Regex("[\\u200B-\\u200F\\uFEFF\\u202A-\\u202E\\u2060-\\u2069\\u061C\\u00A0\\u202F\\u180E]"),
            ""
        ).trim()
        // Normalize Unicode NFC and unify Bengali YYA (\u09DF) with JA+NUKTA (\u09AF\u09BC)
        val normalized = Normalizer.normalize(withoutInvisibles, Normalizer.Form.NFC)
        return normalized.replace("\u09AF\u09BC", "\u09DF")
    }

    /**
     * Checks whether the notification body indicates a message revocation/deletion event.
     * Guaranteed not to false-positive on regular conversation text containing words like 'deleted'.
     */
    fun isDeletedNotification(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val clean = sanitize(text)
        if (clean.isBlank()) return false
        return DELETED_PATTERNS.any { it.matcher(clean).matches() }
    }

    /**
     * Extracts sender from unsend strings like "Alice unsent a message" or "Bob: This message was deleted".
     * If not specifically formatted or if the author is self ("You"), returns fallback sender.
     */
    fun extractUnsentAuthor(text: String?, fallbackSender: String): String {
        if (text.isNullOrBlank()) return fallbackSender
        val clean = sanitize(text)

        for (pattern in UNSENT_AUTHOR_PATTERNS) {
            val matcher = pattern.matcher(clean)
            if (matcher.matches()) {
                val extracted = matcher.group(1)?.trim()
                if (!extracted.isNullOrEmpty() && extracted.lowercase() !in SELF_PRONOUNS) {
                    return extracted
                }
            }
        }
        return fallbackSender
    }
}
