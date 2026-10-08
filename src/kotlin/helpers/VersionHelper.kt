package desu.inugram.helpers

import desu.inugram.InuConfig
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.BuildVars
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R

object VersionHelper {
    private val pInfo by lazy {
        ApplicationLoader.applicationContext.packageManager.getPackageInfo(
            ApplicationLoader.applicationContext.packageName, 0
        )
    }

    @JvmStatic
    val stockVersionName by lazy {
        pInfo.versionName?.replace(Regex("-[0-9a-f]{7}$"), "") ?: ""
    }

    @JvmStatic
    val commitSha: String? by lazy {
        Regex("-([0-9a-f]{7})$").find(pInfo.versionName ?: "")?.groupValues?.get(1)
    }

    fun getVersionInfoString(): String {
        // entiny: STOCK_VERSION_CODE is upstream Telegram's build number, not ours -- releases are tagged by pInfo.versionCode
        val base = LocaleController.formatString(R.string.InuVersion, stockVersionName, pInfo.versionCode)
        val withBeta = if (BuildVars.isBetaApp()) "$base ${LocaleController.getString(R.string.InuVersionBetaSuffix)}" else base
        val commitSuffix = commitSha?.let { " @$it" } ?: ""
        return "$withBeta$commitSuffix [${BuildConfig.INU_BUILD_TYPE}]"
    }

    @JvmStatic
    fun getFullVersionInfo(): String {
        return "${getVersionInfoString()}\nBuilt on: ${BuildVars.BUILD_DATE}"
    }

    @JvmStatic
    fun getSessionAppName(serverName: String): String {
        if (!InuConfig.MASK_SERVER_APP_NAME.value) return serverName
        if (serverName.contains("inugram", ignoreCase = true) || serverName.contains("entinygram", ignoreCase = true)) {
            return runCatching { LocaleController.getString(R.string.AppName) }.getOrElse { serverName }
        }
        return serverName
    }

    private val GIT_SHA_SUFFIX = Regex("-[0-9a-fA-F]{6,40}(?=[ (]|$)")

    @JvmStatic
    fun getSessionAppVersion(rawVersion: String): String {
        if (!InuConfig.MASK_SERVER_APP_NAME.value) return rawVersion
        return GIT_SHA_SUFFIX.replace(rawVersion, "")
    }
}
