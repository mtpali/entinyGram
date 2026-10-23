package desu.inugram.helpers.media

import java.io.File
import org.telegram.messenger.FileLoader
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLRPC

object OriginalFileNameHelper {
    private fun clean(name: String?): String? =
        FileLoader.fixFileName(name?.replace("\u0000", ""))?.takeIf { it.isNotBlank() && it != "." && it != ".." }

    @JvmStatic
    fun forDownload(document: TLRPC.Document?, parent: Any?): String? {
        if (parent !is MessageObject || parent.isRoundVideo || parent.isVoice || parent.isAnyKindOfSticker) return null
        return clean(FileLoader.getDocumentFileName(document))
    }

    @JvmStatic
    fun forSave(source: File, name: String?): String = clean(name) ?: source.name

    @JvmStatic
    fun forMessage(message: MessageObject?): String? =
        clean(FileLoader.getDocumentFileName(message?.qualityToSave ?: message?.document))

    @JvmStatic
    fun createDestination(directory: File, name: String): File {
        val filename = clean(name) ?: "file"
        val dot = filename.lastIndexOf('.').takeIf { it > 0 } ?: filename.length
        var candidate = File(directory, filename)
        var suffix = 0
        while (!candidate.createNewFile()) {
            suffix++
            candidate = File(directory, filename.substring(0, dot) + " ($suffix)" + filename.substring(dot))
        }
        return candidate
    }
}
