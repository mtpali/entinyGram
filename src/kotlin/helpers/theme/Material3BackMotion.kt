package desu.inugram.helpers.theme

import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ProfileActivity
import org.telegram.ui.ViewPagerActivity

internal inline fun ViewGroup.eachChild(action: (View) -> Unit) {
    for (i in 0 until childCount) action(getChildAt(i))
}

object Material3BackMotion {
    const val ENTER_OFFSET_DP = 96f
    const val SCRIM_ALPHA_BYTE = 77
    const val SCRIM_FADE = 0.5f

    val EMPHASIZED: Interpolator = PathInterpolator(
        Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        }
    )

    fun getFragmentBackground(fragment: BaseFragment?): Drawable? {
        var f = fragment
        while (f is ViewPagerActivity) f = f.currentVisibleFragment
        // entiny: ProfileActivity fragmentView is transparent so resolve window background gray directly
        if (f is ProfileActivity) return ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray))
        val bg = f?.fragmentView?.background
        if (bg is ColorDrawable && Color.alpha(bg.color) == 0) return null
        return bg
    }
}
