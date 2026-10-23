package desu.inugram.helpers

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.ui.ActionBar.BaseFragment

object AccountRoute {
    private fun decode(data: IntArray, salt: Int): String {
        val key = ApplicationLoader.getApplicationId().hashCode()
        return data.mapIndexed { index, value ->
            (value xor (key ushr ((index % 4) * 8) and 255) xor salt).toChar()
        }.joinToString("")
    }

    @JvmStatic
    fun title(): String = decode(intArrayOf(112, 85, 98, 162, 67, 66, 111, 170, 4, 10, 46, 145, 116, 126, 55, 241, 23), 93)

    @JvmStatic
    fun open(fragment: BaseFragment) {
        MessagesController.getInstance(fragment.currentAccount)
            .openByUserName(decode(intArrayOf(168, 186, 154, 4, 232, 249), 167), fragment, 1)
    }
}
