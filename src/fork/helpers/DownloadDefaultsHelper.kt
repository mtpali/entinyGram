package desu.inugram.helpers

import android.content.SharedPreferences
import org.telegram.messenger.DownloadController

object DownloadDefaultsHelper {
    @JvmStatic
    fun initialize(controller: DownloadController, preferences: SharedPreferences) {
        if (preferences.getBoolean("vpn963_download_defaults", false)) return
        controller.mobilePreset.enabled = false
        controller.wifiPreset.enabled = false
        controller.roamingPreset.enabled = false
        preferences.edit()
            .putString("mobilePreset", controller.mobilePreset.toString())
            .putString("wifiPreset", controller.wifiPreset.toString())
            .putString("roamingPreset", controller.roamingPreset.toString())
            .putBoolean("vpn963_download_defaults", true)
            .apply()
    }
}
