package com.wxglass

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

/**
 * 命中规则。微信的 resource id / 类名被混淆且每个版本都会变，
 * 所以用「几何 + 结构」启发式识别，而不是硬编码 id。识别不准时只改这个文件。
 */
object Targets {

    private fun dp(v: View, x: Float) = x * v.resources.displayMetrics.density
    private fun screenW(v: View) = v.resources.displayMetrics.widthPixels
    private fun screenH(v: View) = v.resources.displayMetrics.heightPixels
    private fun top(v: View): Int { val l = IntArray(2); v.getLocationOnScreen(l); return l[1] }
    private fun left(v: View): Int { val l = IntArray(2); v.getLocationOnScreen(l); return l[0] }

    /** 屏幕底部、通栏、高 44~84dp、4~5 个子项 → 主页底部 Tab。 */
    fun isBottomTabBar(v: View): Boolean {
        if (v !is ViewGroup || v.childCount !in 4..5) return false
        val h = v.height; val w = v.width
        if (w == 0 || h == 0 || w < screenW(v) * 0.9f) return false
        if (h < dp(v, 44f) || h > dp(v, 84f)) return false
        if (top(v) + h < screenH(v) * 0.85f) return false
        return (0 until v.childCount).all { v.getChildAt(it).let { c -> c.isClickable || c is ViewGroup } }
    }

    /** 顶部右侧：宽 60~140dp、高 30~52dp、2~3 个可点击子项 → 搜索/加号 这类胶囊按钮组。 */
    fun isTopCapsule(v: View): Boolean {
        if (v !is ViewGroup || v.childCount !in 2..3) return false
        val h = v.height; val w = v.width
        if (h < dp(v, 30f) || h > dp(v, 52f) || w < dp(v, 60f) || w > dp(v, 140f)) return false
        if (w <= h * 1.6f) return false
        if (top(v) > dp(v, 170f) || left(v) + w < screenW(v) * 0.6f) return false
        return (0 until v.childCount).all { v.getChildAt(it).isClickable }
    }

    /** 顶部单个图标按钮（返回 / 添加好友 / 相机）→ 圆形玻璃按钮。头像（位图）不算。 */
    fun isTopCircleButton(v: View): Boolean {
        if (v !is ImageView || !(v.isClickable || (v.parent as? View)?.isClickable == true)) return false
        if (v.drawable == null || v.drawable is BitmapDrawable) return false
        val w = v.width; val h = v.height
        if (w == 0 || w != h || w < dp(v, 28f) || w > dp(v, 48f)) return false
        return top(v) < dp(v, 170f)
    }

    /** 宽 ≥ 85% 屏、高 30~52dp、内含「搜索」文字 → 搜索栏。 */
    fun isSearchBar(v: View): Boolean {
        if (v !is ViewGroup) return false
        val h = v.height; val w = v.width
        if (h < dp(v, 30f) || h > dp(v, 52f) || w < screenW(v) * 0.85f) return false
        if (top(v) > dp(v, 320f)) return false
        return hasText(v, setOf("搜索", "Search"), 0)
    }

    private fun hasText(v: View, texts: Set<String>, depth: Int): Boolean {
        if (v is TextView) {
            val t = v.text?.toString()?.trim() ?: ""
            val hint = v.hint?.toString()?.trim() ?: ""
            if (t in texts || hint in texts) return true
        }
        if (depth < 3 && v is ViewGroup)
            for (i in 0 until v.childCount) if (hasText(v.getChildAt(i), texts, depth + 1)) return true
        return false
    }

    /** 方形、32~64dp 的位图 ImageView → 头像。 */
    fun isAvatar(v: View): Boolean {
        if (v !is ImageView) return false
        val w = v.width; val h = v.height
        if (w == 0 || w != h) return false
        return w >= dp(v, 32f) && w <= dp(v, 64f)
    }

    /** 全屏宽、占屏高 ≥30% 的可滚动列表（ListView / RecyclerView）。 */
    fun isMainList(v: View): Boolean {
        if (v !is ViewGroup) return false
        val scrollable = v is AbsListView || v.javaClass.name.contains("RecyclerView")
        if (!scrollable) return false
        return v.width >= screenW(v) * 0.9f && v.height >= screenH(v) * 0.3f
    }

    /** 列表里的「行」：高 40~140dp。更高的是横幅/日历/广告，更矮的是分组标题，都不套卡片。 */
    fun isRow(c: View): Boolean = c.height >= dp(c, 40f) && c.height <= dp(c, 140f)

    /** 聊天输入框：屏幕下方 40% 内、高 30~60dp 的 EditText。 */
    fun isChatInput(v: View): Boolean {
        if (v !is EditText) return false
        val h = v.height
        if (h < dp(v, 30f) || h > dp(v, 60f)) return false
        return top(v) > screenH(v) * 0.6f
    }
}
