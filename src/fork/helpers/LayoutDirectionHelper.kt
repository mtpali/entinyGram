package desu.inugram.helpers

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import org.telegram.messenger.ApplicationLoader
import java.util.Locale

object LayoutDirectionHelper {
    // entiny: LocaleController can initialize before InuConfig.load(), so read the persisted preference.
    @JvmStatic
    fun isForced(): Boolean = ApplicationLoader.applicationContext
        ?.getSharedPreferences("inugram", Context.MODE_PRIVATE)
        ?.getBoolean("force_ltr", false) == true

    @JvmStatic
    fun apply(configuration: Configuration) {
        if (isForced()) configuration.setLayoutDirection(Locale.ROOT)
    }

    @JvmStatic
    fun configureActivity(activity: Activity) {
        if (!isForced()) return
        val resources = activity.resources
        val configuration = Configuration(resources.configuration)
        apply(configuration)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        activity.window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
    }
}
