package desu.inugram.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.telegram.ui.LauncherIconController

class LauncherRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            LauncherIconController.tryFixLauncherIconIfNeeded()
        }
    }
}
