package desu.inugram.helpers

import org.telegram.messenger.LocaleController

object LanguageHelper {
    @JvmStatic
    fun isSupported(code: String?): Boolean = code == "en" || code == "fa"

    @JvmStatic
    fun addPersian(controller: LocaleController) {
        val info = LocaleController.LocaleInfo().apply {
            name = "فارسی"
            nameEnglish = "Persian"
            shortName = "fa"
            pluralLangCode = "fa"
            pathToFile = "remote"
            builtIn = true
            isRtl = true
        }
        controller.languages.add(info)
        controller.languagesDict[info.shortName] = info
    }
}
