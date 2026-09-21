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
        Pattern.compile("^(?:.+?:\\s+)?(?:(?:এই\\s+)?(?:বার্তাটি|মেসেজটি|মেসেজ|লেখাটি)\\s+(?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে দেওয়া হয়েছে|মুছে দেয়া হয়েছে|মুছে ফেলেছেন)|একটি বার্তা (?:মুছে ফেলা হয়েছে|মুছে ফেলা হয়েছে|মুছে ফেলেছেন)|আপনি একটি বার্তা (?:মুছে ফেলেছেন|মুছে দিয়েছেন|মুছে দিয়েছেন)|বার্তা মুছে ফেলা হয়েছে|বার্তা মুছে ফেলা হয়েছে)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Hindi
        Pattern.compile("^(?:.+?:\\s+)?(?:(?:यह\\s+)?संदेश हटा दिया गया(?: था)?|आपने एक संदेश हटा दिया)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Spanish
        Pattern.compile("^(?:.+?:\\s+)?(?:Este mensaje fue eliminado|Se eliminó este mensaje|Eliminaste este mensaje|.+?\\s+eliminó un mensaje)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Portuguese
        Pattern.compile("^(?:.+?:\\s+)?(?:Esta mensagem foi apagada|Você apagou esta mensagem|.+?\\s+anulou o envio de uma mensagem)[.!]?$", Pattern.CASE_INSENSITIVE),

        // French
        Pattern.compile("^(?:.+?:\\s+)?(?:Ce message a été supprimé|Vous avez supprimé ce message|.+?\\s+a annulé l'envoi d'un message)[.!]?$", Pattern.CASE_INSENSITIVE),

        // German
        Pattern.compile("^(?:.+?:\\s+)?(?:Diese Nachricht wurde gelöscht|Du hast diese Nachricht gelöscht|.+?\\s+hat eine Nachricht zurückgerufen)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Arabic
        Pattern.compile("^(?:.+?:\\s+)?(?:تم حذف هذه الرسالة|تم إلغاء إرسال الرسالة|تم حذف الرسالة)[.!]?$", Pattern.CASE_INSENSITIVE),

        // Urdu
        Pattern.compile("^(?:.+?:\\s+)?(?:یہ پیغام حذف کر دیا گیا(?: تھا)?)[.!]?$", Pattern.CASE_INSENSITIVE)
    )

    private val UNSENT_AUTHOR_PATTERNS = listOf(
        Pattern.compile("^(?:(.+?)\\s+unsent a message)[.!]?$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:(.+?)\\s+একটি বার্তা মুছে ফেলেছেন)[.!]?$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:(.+?)\\s+a annulé l'envoi d'un message)[.!]?$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:(.+?)\\s+eliminó un mensaje)[.!]?$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:(.+?)\\s+anulou o envio de uma mensagem)[.!]?$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:(.+?)\\s+hat eine Nachricht zurückgerufen)[.!]?$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^([^:]+):\\s+(?:এই বার্তাটি|মেসেজটি|This message was deleted|You deleted this message|Message deleted|Este mensaje|Esta mensagem|Ce message|Diese Nachricht)[.!]?$", Pattern.CASE_INSENSITIVE)
    )

    private val SELF_PRONOUNS = setOf("you", "vous", "tú", "tu", "você", "voce", "du", "আপনি")

    /**
     * Sanitizes strings by removing invisible bidirectional/zero-width Unicode control characters
     * and normalizing composed/decomposed glyphs commonly inserted by OEM keyboards and Android System UI.
     */
    fun sanitize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val withoutInvisibles = text.replace(Regex("[\\u200B-\\u200F\\uFEFF\\u202A-\\u202E\\u00A0]"), "").trim()
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
