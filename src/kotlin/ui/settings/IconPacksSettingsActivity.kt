package desu.inugram.ui.settings

import android.view.View
import desu.inugram.InuConfig
import desu.inugram.SearchRegistry
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter

class IconPacksSettingsActivity : SettingsPageActivity() {
    override fun getTitle(): CharSequence = LocaleController.getString(R.string.InuIconReplacement)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.InuIconReplacement)))
        items.add(UItem.asButton(PACK_DEFAULT, currentPackLabel(), "✓"))
        items.add(UItem.asShadow(null))
        items.add(UItem.asHeader(LocaleController.getString(R.string.InuNotificationIcon)))
        items.add(UItem.asButton(NOTIFICATION_ICON_ID, LocaleController.getString(R.string.InuNotificationIcon), notificationIconLabel()))
        items.add(UItem.asShadow(LocaleController.getString(R.string.InuNotificationIconInfo)))
    }

    private fun notificationIconLabel(): String = when (InuConfig.NOTIFICATION_ICON.value) {
        InuConfig.NotificationIconItem.NAGRAMXF -> LocaleController.getString(R.string.InuNotificationIconNagramXF)
        else -> LocaleController.getString(R.string.InuNotificationIconTelegram)
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id == NOTIFICATION_ICON_ID) {
            RadioItemOptions.show(
                this,
                view,
                listOf(
                    LocaleController.getString(R.string.InuNotificationIconTelegram),
                    LocaleController.getString(R.string.InuNotificationIconNagramXF),
                ),
                InuConfig.NOTIFICATION_ICON.value,
            ) { which ->
                InuConfig.NOTIFICATION_ICON.value = which
                showRestartBulletin()
                listView.adapter.update(false)
            }
            return
        }
    }

    companion object {
        fun currentPackLabel(): String = LocaleController.getString(R.string.InuIconReplacementOff)
        private const val PACK_DEFAULT = 28000
        private const val NOTIFICATION_ICON_ID = 28020

        @JvmField
        val PAGE = SearchRegistry.Page(
            slug = "icon-packs",
            titleRes = R.string.InuIconReplacement,
            iconRes = R.drawable.msg_theme,
            factory = ::IconPacksSettingsActivity,
            entries = listOf(
                SearchRegistry.Entry("icon-pack-default", R.string.InuIconReplacementOff, PACK_DEFAULT),
                SearchRegistry.Entry("notification-icon", R.string.InuNotificationIcon, NOTIFICATION_ICON_ID),
            ),
        )
    }
}
