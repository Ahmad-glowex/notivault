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
        // Bengali (both YYA \u09DF and JA+NUKTA \u09AF\u09BC)
        assertTrue(DeletedMessageDetector.isDeletedNotification("এই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("এই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মেসেজটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ahmad: এই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("আহমদ একটি বার্তা মুছে ফেলেছেন"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("আপনি একটি বার্তা মুছে ফেলেছেন"))

        // Hindi
        assertTrue(DeletedMessageDetector.isDeletedNotification("यह संदेश हटा दिया गया था"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("यह संदेश हटा दिया गया"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("आपने एक संदेश हटा दिया"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("राहुल: यह संदेश हटा दिया गया"))

        // Arabic
        assertTrue(DeletedMessageDetector.isDeletedNotification("تم حذف هذه الرسالة"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("تم إلغاء إرسال الرسالة"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("تم الغاء ارسال الرسالة"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("ألغى إرسال رسالة"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("أحمد: تم حذف هذه الرسالة"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("أحمد ألغى إرسال رسالة"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("تم حذف هذا المحتوى"))

        // Urdu
        assertTrue(DeletedMessageDetector.isDeletedNotification("یہ پیغام حذف کر دیا گیا تھا"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("یہ پیغام حذف کر دیا گیا"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("پیغام حذف کیا گیا"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("احمد: یہ پیغام حذف کر دیا گیا تھا"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("احمد نے ایک پیغام واپس لے لیا"))

        // Russian
        assertTrue(DeletedMessageDetector.isDeletedNotification("Сообщение удалено"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Это сообщение было удалено"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Вы удалили это сообщение"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Иван: Сообщение удалено"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Иван отменил отправку сообщения"))

        // Turkish
        assertTrue(DeletedMessageDetector.isDeletedNotification("Bu mesaj silindi"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Bu mesajı sildiniz"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ahmet: Bu mesaj silindi"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ahmet bir mesajın gönderimini geri aldı"))

        // Chinese
        assertTrue(DeletedMessageDetector.isDeletedNotification("此消息已撤回"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("此消息已被删除"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("张三撤回了一条消息"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("你撤回了一条消息"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("张三: 此消息已撤回"))

        // Japanese
        assertTrue(DeletedMessageDetector.isDeletedNotification("メッセージの送信を取り消しました"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("このメッセージは削除されました"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("田中がメッセージの送信を取り消しました"))

        // Spanish & Portuguese
        assertTrue(DeletedMessageDetector.isDeletedNotification("Este mensaje fue eliminado"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Esta mensagem foi apagada"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Juan: Este mensaje fue eliminado"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Carlos eliminó un mensaje"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Carlos anulou o envio de uma mensagem"))

        // French
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ce message a été supprimé"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Claire: Ce message a été supprimé."))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Pierre a annulé l'envoi d'un message"))

        // German
        assertTrue(DeletedMessageDetector.isDeletedNotification("Diese Nachricht wurde gelöscht"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Hans hat eine Nachricht zurückgerufen"))

        // Italian
        assertTrue(DeletedMessageDetector.isDeletedNotification("Questo messaggio è stato eliminato"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Messaggio eliminato"))
    }

    @Test
    fun testUnicodeAndInvisibleCharacterSanitization() {
        // LTR, RTL, BOM, NBSP, and Bidi isolate marks
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u200EThis message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u200FThis message was deleted."))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\uFEFFThis message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u00A0This message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u202FThis message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u2068This message was deleted\u2069"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u200Eএই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u200Eএই বার্তাটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ahmad 👍: \u200EThis message was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("آحمد: \u200Eاین پیغام حذف شد"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u2068أحمد\u2069: \u2068تم حذف هذه الرسالة\u2069"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("\u061Cتم حذف هذه الرسالة"))
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
        assertEquals("Alice", DeletedMessageDetector.extractUnsentAuthor("Alice unsent a message", "Default"))
        assertEquals("Default", DeletedMessageDetector.extractUnsentAuthor("You unsent a message.", "Default"))
        assertEquals("Bob", DeletedMessageDetector.extractUnsentAuthor("Bob: This message was deleted", "Default"))
        assertEquals("আহমদ", DeletedMessageDetector.extractUnsentAuthor("আহমদ একটি বার্তা মুছে ফেলেছেন", "Fallback"))
        assertEquals("Fallback", DeletedMessageDetector.extractUnsentAuthor("আপনি একটি বার্তা মুছে ফেলেছেন", "Fallback"))
        assertEquals("أحمد", DeletedMessageDetector.extractUnsentAuthor("أحمد ألغى إرسال رسالة", "Fallback"))
        assertEquals("Fallback", DeletedMessageDetector.extractUnsentAuthor("أنت ألغيت إرسال رسالة", "Fallback"))
        assertEquals("احمد", DeletedMessageDetector.extractUnsentAuthor("احمد نے ایک پیغام واپس لے لیا", "Fallback"))
        assertEquals("Fallback", DeletedMessageDetector.extractUnsentAuthor("آپ نے ایک پیغام واپس لے لیا", "Fallback"))
        assertEquals("Иван", DeletedMessageDetector.extractUnsentAuthor("Иван отменил отправку сообщения", "Fallback"))
        assertEquals("Ahmet", DeletedMessageDetector.extractUnsentAuthor("Ahmet bir mesajın gönderimini geri aldı", "Fallback"))
        assertEquals("张三", DeletedMessageDetector.extractUnsentAuthor("张三撤回了一条消息", "Fallback"))
        assertEquals("Fallback", DeletedMessageDetector.extractUnsentAuthor("你撤回了一条消息", "Fallback"))
        assertEquals("田中", DeletedMessageDetector.extractUnsentAuthor("田中がメッセージの送信を取り消しました", "Fallback"))
        assertEquals("Pierre", DeletedMessageDetector.extractUnsentAuthor("Pierre a annulé l'envoi d'un message", "Fallback"))
        assertEquals("Carlos", DeletedMessageDetector.extractUnsentAuthor("Carlos eliminó un mensaje", "Fallback"))
    }

    @Test
    fun testBengaliAndEnglishEdgeCases() {
        // Bengali variations
        assertTrue(DeletedMessageDetector.isDeletedNotification("এই মেসেজটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("এই মেসেজটি মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("একটি মেসেজ মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("আপনি একটি মেসেজ মুছে ফেলেছেন"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মেসেজ মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মেসেজ মুছে দেওয়া হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মেসেজ মুছে দেয়া হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মুছে ফেলা হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মুছে দেওয়া হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মুছে দেয়া হয়েছে"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মুছে ফেলেছেন"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("মুছে দিয়েছেন"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Ahmad: এই মেসেজটি মুছে ফেলা হয়েছে"))

        // English variations
        assertTrue(DeletedMessageDetector.isDeletedNotification("Deleted photo"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Deleted video"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("This media was deleted"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("1 deleted message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Alice unsent a message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("You unsent a message"))
        assertTrue(DeletedMessageDetector.isDeletedNotification("Message was unsent"))
    }

    @Test
    fun testNonDeletedMessagesDoNotTrigger() {
        // Critical: conversational mentions of deletion in any language MUST NOT trigger unsend logic
        assertFalse(DeletedMessageDetector.isDeletedNotification("Why was the message deleted?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Did you see that the message deleted by John?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Alice unsent a message yesterday when we were arguing"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Can you check if that message was removed from the chat?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("I deleted the file from my laptop"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Hey, how are you?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Where are we meeting today?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Send me the message"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("مرحبا كيف حالك"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("هل حذفت الرسالة من هاتفك؟"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("کیا آپ نے پیغام دیکھا؟"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Привет, как дела?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("Merhaba nasılsın"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("今天天气真好"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("こんにちは、お元気ですか"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("কেমন আছেন ভাই?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification("आप कैसे हैं?"))
        assertFalse(DeletedMessageDetector.isDeletedNotification(""))
        assertFalse(DeletedMessageDetector.isDeletedNotification("   "))
        assertFalse(DeletedMessageDetector.isDeletedNotification(null))
    }
}
