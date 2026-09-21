package com.notivault.app.service.engine

import java.util.regex.Pattern

object DeletedMessageDetector {

    // Regex patterns for various languages & platforms
    private val DELETED_PATTERNS = listOf(
        // English
        Pattern.compile(".*this message was deleted.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*you deleted this message.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*this message was deleted by the author.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*unsent a message.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*message was unsent.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*message deleted.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*message was removed.*", Pattern.CASE_INSENSITIVE),
        
        // Spanish
        Pattern.compile(".*este mensaje fue eliminado.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*eliminó un mensaje.*", Pattern.CASE_INSENSITIVE),
        
        // Portuguese
        Pattern.compile(".*esta mensagem foi apagada.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*anulou o envio de uma mensagem.*", Pattern.CASE_INSENSITIVE),
        
        // French
        Pattern.compile(".*ce message a été supprimé.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*a annulé l'envoi d'un message.*", Pattern.CASE_INSENSITIVE),
        
        // German
        Pattern.compile(".*diese nachricht wurde gelöscht.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*hat eine nachricht zurückgerufen.*", Pattern.CASE_INSENSITIVE),
        
        // Hindi & Bengali
        Pattern.compile(".*यह संदेश हटा दिया गया था.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*বার্তাটি মুছে ফেলা হয়েছে.*", Pattern.CASE_INSENSITIVE)
    )

    private val MESSENGER_UNSENT_PATTERN =
        Pattern.compile("^(.*)\\s+unsent a message$", Pattern.CASE_INSENSITIVE)

    /**
     * Checks whether the notification body indicates a message revocation/deletion event.
     */
    fun isDeletedNotification(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val clean = text.trim()
        return DELETED_PATTERNS.any { it.matcher(clean).matches() }
    }

    /**
     * Extracts sender from unsend strings like "Alice unsent a message".
     * If not specifically formatted, returns fallback sender or null.
     */
    fun extractUnsentAuthor(text: String?, fallbackSender: String): String {
        if (text == null) return fallbackSender
        val matcher = MESSENGER_UNSENT_PATTERN.matcher(text.trim())
        if (matcher.matches()) {
            val extracted = matcher.group(1)?.trim()
            if (!extracted.isNullOrEmpty() && !extracted.equals("You", ignoreCase = true)) {
                return extracted
            }
        }
        return fallbackSender
    }
}
