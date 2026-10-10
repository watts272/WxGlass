package com.wxglass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import java.util.WeakHashMap

/**
 * 苹果「液态玻璃」风格：分组圆角卡片、悬浮 Tab 胶囊、顶部玻璃胶囊/圆按钮、
 * 半透明聊天气泡、玻璃输入栏、弹窗背景模糊。全部随浅/深色切换。
 */
object GlassStyler {

    private val done = WeakHashMap<View, Int>()         // View -> 已按哪种主题处理过
    private val tabBars = WeakHashMap<View, Boolean>()
    private val capsules = WeakHashMap<View, Boolean>()
    private val lists = WeakHashMap<View, Boolean>()
    private val rowKey = WeakHashMap<View, Int>()
    private val marginDone = WeakHashMap<View, Boolean>()
    private val watched = WeakHashMap<View, Boolean>()

    // ---------- 基础 ----------

    private fun roundOutline(radiusPx: Float) = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
        }
    }

    private fun isChatActivity(v: View): Boolean {
        var c: Context? = v.context
        while (c is ContextWrapper) {
            if (c is Activity) return c.javaClass.name.contains("Chatting")
            c = c.baseContext
        }
        return false
    }

    fun setupWindow(w: Window) {
        w.statusBarColor = Color.TRANSPARENT
        w.navigationBarColor = Color.TRANSPARENT
    }

    /** 对话框 / 底部面板：圆角玻璃 + 背后内容模糊（Android 12+）。 */
    fun styleDialogWindow(w: Window) {
        val dv = w.decorView
        w.setBackgroundDrawable(Palette.glass(dv, dv.dpf(30f)))
        dv.clipToOutline = true
        dv.outlineProvider = roundOutline(dv.dpf(30f))
        if (Build.VERSION.SDK_INT >= 31) {
            w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            w.attributes = w.attributes.apply { blurBehindRadius = 48 }
            runCatching { w.setBackgroundBlurRadius(60) }
        }
        w.setDimAmount(if (Palette.dark(dv)) 0.35f else 0.18f)
    }

    // ---------- 遍历 ----------

    fun watchLayout(root: ViewGroup) {
        if (watched.put(root, true) != null) return
        var last = 0L
        val run = Runnable { apply(root) }
        val throttled = {
            val now = System.currentTimeMillis()
            if (now - last >= 100) { last = now; run.run() }   // 滚动时新出现的行要尽快套样式
        }
        root.viewTreeObserver.addOnGlobalLayoutListener { throttled() }
        root.viewTreeObserver.addOnScrollChangedListener(ViewTreeObserver.OnScrollChangedListener { throttled() })
    }

    fun apply(root: View) {
        walk(root, 0, Palette.uiKey(root), isChatActivity(root))
    }

    private fun walk(v: View, depth: Int, ui: Int, chat: Boolean) {
        if (depth > 16) return

        if (done[v] != ui) {
            when {
                Targets.isBottomTabBar(v) -> floatingTabBar(v as ViewGroup, ui)
                Targets.isTopCapsule(v) -> { topPill(v, v.height / 2f); capsules[v] = true; done[v] = ui }
                Targets.isTopCircleButton(v) && (v.parent as? View)?.let { capsules[it] } == null -> {
                    topPill(v, v.width / 2f); done[v] = ui
                }
                Targets.isSearchBar(v) -> { searchPill(v); done[v] = ui }
                chat && Targets.isChatInput(v) -> { chatInput(v as EditText); done[v] = ui }
                Targets.isAvatar(v) -> { squircle(v as ImageView); done[v] = ui }
                !chat && Targets.isMainList(v) -> { setupList(v as ViewGroup); done[v] = ui }
                else -> Unit
            }
        }

        // 气泡会被微信在复用时重置，每轮都检查（已替换的就不再是 9-patch）
        if (v is TextView && v !is EditText && v.background is NinePatchDrawable) bubble(v)

        if (v is ViewGroup) {
            if (lists[v] != null) restyleRows(v, ui)
            for (i in 0 until v.childCount) walk(v.getChildAt(i), depth + 1, ui, chat)
        }
    }

    // ---------- 各组件 ----------

    /** 底部 Tab → 悬浮玻璃胶囊，选中项带一个灰色胶囊高亮。 */
    private fun floatingTabBar(bar: ViewGroup, ui: Int) {
        val r = bar.dpf(32f)
        bar.background = Palette.glass(bar, r)
        bar.clipToOutline = true
        bar.outlineProvider = roundOutline(r)
        bar.elevation = bar.dpf(12f)
        (bar.parent as? ViewGroup)?.clipChildren = false
        if (marginDone.put(bar, true) == null) {
            (bar.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                val m = bar.dpf(14f).toInt()
                it.setMargins(m, it.topMargin, m, m)
                bar.layoutParams = it
            }
        }
        tabBars[bar] = true
        done[bar] = ui
        refreshTab(bar)
    }

    /** 选中项高亮。由 View.setSelected 的 Hook 触发，也在每次套样式时调用。 */
    fun refreshTab(bar: ViewGroup) {
        val inset = bar.dpf(5f).toInt()
        for (i in 0 until bar.childCount) {
            val c = bar.getChildAt(i)
            val sel = c.isSelected || (c is ViewGroup && (0 until c.childCount).any { c.getChildAt(it).isSelected })
            c.background = if (sel)
                InsetDrawable(Palette.solid(Palette.selectedPill(bar), bar.dpf(26f)), inset, inset, inset, inset)
            else null
        }
    }

    fun onSelectedChanged(v: View) {
        val p = v.parent as? ViewGroup ?: return
        if (tabBars[p] != null) { refreshTab(p); return }
        val pp = p.parent as? ViewGroup ?: return
        if (tabBars[pp] != null) refreshTab(pp)
    }

    private fun topPill(v: View, radius: Float) {
        v.background = Palette.glass(v, radius)
        v.clipToOutline = true
        v.outlineProvider = roundOutline(radius)
        v.elevation = v.dpf(6f)
        (v.parent as? ViewGroup)?.clipChildren = false
    }

    private fun searchPill(v: View) {
        val r = v.height / 2f
        v.background = Palette.solid(Palette.card(v), r)
        v.clipToOutline = true
        v.outlineProvider = roundOutline(r)
    }

    private fun chatInput(e: EditText) {
        val r = e.dpf(22f)
        e.background = Palette.glass(e, r)
    }

    /** 方形头像 → iOS 连续圆角（近似 squircle）。 */
    private fun squircle(img: ImageView) {
        img.clipToOutline = true
        img.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.29f)
            }
        }
    }

    /** 聊天气泡：9-patch → 半透明玻璃胶囊；按水平位置区分发送/接收。 */
    private fun bubble(tv: TextView) {
        if (tv.width == 0 || tv.height == 0) return
        val l = IntArray(2); tv.getLocationOnScreen(l)
        val sent = l[0] + tv.width / 2 > tv.resources.displayMetrics.widthPixels / 2
        val r = minOf(tv.height / 2f, tv.dpf(20f))
        tv.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(if (sent) Palette.bubbleSent(tv) else Palette.bubbleRecv(tv))
            cornerRadius = r
            setStroke(tv.dpf(1f).toInt().coerceAtLeast(1), Palette.stroke(tv))
        }
        tv.setTextColor(Palette.text(tv))
    }

    // ---------- 分组卡片列表 ----------

    private fun setupList(list: ViewGroup) {
        if (lists.put(list, true) != null) return
        val m = list.dpf(14f).toInt()
        list.setPadding(list.paddingLeft + m, list.paddingTop, list.paddingRight + m, list.paddingBottom)
        list.clipToPadding = false
    }

    /** 连续的「行」合成一张卡片：首行圆上角，末行圆下角，遇到标题/横幅断开成新卡片。 */
    private fun restyleRows(list: ViewGroup, ui: Int) {
        val r = list.dpf(20f)
        val n = list.childCount
        for (i in 0 until n) {
            val c = list.getChildAt(i)
            if (!Targets.isRow(c)) continue
            val top = i == 0 || !Targets.isRow(list.getChildAt(i - 1))
            val bottom = i == n - 1 || !Targets.isRow(list.getChildAt(i + 1))
            val key = (if (top) 1 else 0) or (if (bottom) 2 else 0) or (ui shl 2)
            if (rowKey[c] == key && c.background is android.graphics.drawable.GradientDrawable) continue
            c.background = Palette.cardRow(c, top, bottom, r)
            rowKey[c] = key
        }
    }
}
