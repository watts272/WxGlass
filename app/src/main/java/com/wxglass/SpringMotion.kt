package com.wxglass

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/** Telegram 风格的弹簧动效：短促、带轻微回弹、可被打断。 */
object SpringMotion {

    private fun spring(v: View, prop: DynamicAnimation.ViewProperty, to: Float,
                       stiffness: Float, damping: Float): SpringAnimation =
        SpringAnimation(v, prop, to).apply {
            this.spring.stiffness = stiffness
            this.spring.dampingRatio = damping
            start()
        }

    /** 按压：快速收缩，松手时弹回并有轻微过冲。 */
    fun press(v: View, down: Boolean) {
        if (down) {
            spring(v, DynamicAnimation.SCALE_X, 0.965f, 900f, SpringForce.DAMPING_RATIO_NO_BOUNCY)
            spring(v, DynamicAnimation.SCALE_Y, 0.965f, 900f, SpringForce.DAMPING_RATIO_NO_BOUNCY)
        } else {
            spring(v, DynamicAnimation.SCALE_X, 1f, 600f, 0.5f)
            spring(v, DynamicAnimation.SCALE_Y, 1f, 600f, 0.5f)
        }
    }

    /** 页面进入：从右侧 22% 宽度处带阻尼滑入并淡入。 */
    fun pageEnter(root: View) {
        val w = root.width.takeIf { it > 0 } ?: root.resources.displayMetrics.widthPixels
        root.translationX = w * 0.22f
        root.alpha = 0.6f
        spring(root, DynamicAnimation.TRANSLATION_X, 0f, 420f, 0.88f)
        spring(root, DynamicAnimation.ALPHA, 1f, 500f, SpringForce.DAMPING_RATIO_NO_BOUNCY)
    }

    /** 弹窗弹出：从 0.88 缩放弹到 1，略有过冲。 */
    fun popIn(v: View) {
        v.scaleX = 0.88f; v.scaleY = 0.88f; v.alpha = 0f
        spring(v, DynamicAnimation.SCALE_X, 1f, 520f, 0.68f)
        spring(v, DynamicAnimation.SCALE_Y, 1f, 520f, 0.68f)
        spring(v, DynamicAnimation.ALPHA, 1f, 800f, SpringForce.DAMPING_RATIO_NO_BOUNCY)
    }
}
