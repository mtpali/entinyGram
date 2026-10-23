package desu.inugram.helpers.translate

import desu.inugram.InuConfig
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import org.telegram.messenger.MessageObject
import org.telegram.messenger.TranslateController
import org.telegram.messenger.Utilities
import org.telegram.tgnet.RequestDelegate
import org.telegram.tgnet.RequestDelegateTimestamp
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_iv

/**
 * Answers translation requests with the provider the user picked instead of Telegram. Requests
 * keep their [org.telegram.tgnet.ConnectionsManager] token, so stock cancels them as it does any other.
 */
object TranslationProviderHelper {

    private class RichTranslationRequest(
        val messages: List<TL_iv.RichMessage>,
        val languages: List<String?>,
        val toLanguage: String,
    ) : TLObject()

    @JvmStatic
    fun setSourceLanguage(text: TLRPC.TL_textWithEntities, language: String?) {
    }

    @JvmStatic
    fun createRichRequest(message: TL_iv.RichMessage, language: String?, toLanguage: String): TLObject =
        RichTranslationRequest(listOf(message), listOf(language), toLanguage)

    @JvmStatic
    fun createRichRequest(messages: List<MessageObject>, toLanguage: String): TLObject =
        RichTranslationRequest(
            messages.map { it.messageOwner.rich_message },
            messages.map { it.messageOwner.originalLanguage },
            toLanguage,
        )

    @JvmStatic
    fun isActive(): Boolean =
        false

    /** stageQueue, from `ConnectionsManager.sendRequestInternal` */
    @JvmStatic
    fun maybeTranslate(
        account: Int,
        token: Int,
        request: TLObject,
        onComplete: RequestDelegate?,
        onCompleteTimestamp: RequestDelegateTimestamp?,
    ): Boolean {
        return false
    }

    /** stageQueue, from `ConnectionsManager.cancelRequest`. False for a token this did not answer */
    @JvmStatic
    fun cancelRequest(account: Int, token: Int): Boolean {
        return false
    }

    private fun createError(text: String) = TLRPC.TL_error().apply {
        code = 400
        this.text = text
    }
}
