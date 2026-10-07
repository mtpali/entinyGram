package desu.inugram.helpers

import android.os.Build
import desu.inugram.helpers.security.ParanoiaHelper
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
        if (ParanoiaHelper.isDisguised()) {
            val abis = Build.SUPPORTED_ABIS
            return "Telegram for Android v${stockVersionName} (${BuildConfig.STOCK_VERSION_CODE})\ndirect ${abis.getOrNull(0)} ${abis.getOrNull(1)}"
        }
        return "${getVersionInfoString()}\nBuilt on: ${BuildVars.BUILD_DATE}"
    }

}
