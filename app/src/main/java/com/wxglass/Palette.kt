package com.wxglass

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

internal fun View.dpf(v: Float) = v * resources.displayMetrics.density

/** 浅色 / 深色两套配色，跟随当前 Activity 的 uiMode（微信自己的深色开关也会改它）。 */
object Palette {
    const val ACCENT = 0xFF07C160.toInt()

    fun dark(v: View) =
        (v.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun uiKey(v: View) = if (dark(v)) 1 else 0

    /** 分组卡片底色 */
    fun card(v: View) = if (dark(v)) 0xFF1C1C1E.toInt() else Color.WHITE

    /** Tab 选中胶囊 */
    fun selectedPill(v: View) = if (dark(v)) 0xFF3A3A3C.toInt() else 0xFFE9E9EB.toInt()

    fun glassTop(v: View) = if (dark(v)) Color.argb(150, 58, 58, 62) else Color.argb(215, 255, 255, 255)
    fun glassBottom(v: View) = if (dark(v)) Color.argb(120, 34, 34, 38) else Color.argb(170, 246, 248, 252)
    fun stroke(v: View) = if (dark(v)) Color.argb(80, 255, 255, 255) else Color.argb(210, 255, 255, 255)

    /** 聊天气泡：发送方为半透明蓝，接收方为半透明白（深色下对应偏暗版本） */
    fun bubbleSent(v: View) = if (dark(v)) Color.argb(185, 16, 96, 170) else Color.argb(185, 120, 205, 255)
    fun bubbleRecv(v: View) = if (dark(v)) Color.argb(170, 58, 58, 62) else Color.argb(205, 255, 255, 255)

    fun text(v: View) = if (dark(v)) Color.WHITE else 0xFF111111.toInt()

    fun glass(v: View, radiusPx: Float): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(glassTop(v), glassBottom(v))).apply {
            cornerRadius = radiusPx
            setStroke(v.dpf(1f).toInt().coerceAtLeast(1), stroke(v))
        }

    fun solid(color: Int, radiusPx: Float) = GradientDrawable().apply {
        setColor(color); cornerRadius = radiusPx
    }

    /** 只圆上 / 下角，用于分组卡片中的首尾行。 */
    fun cardRow(v: View, top: Boolean, bottom: Boolean, r: Float) = GradientDrawable().apply {
        setColor(card(v))
        val t = if (top) r else 0f
        val b = if (bottom) r else 0f
        cornerRadii = floatArrayOf(t, t, t, t, b, b, b, b)
    }
}
