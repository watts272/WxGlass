package com.wxglass

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 入口。LSPatch 内嵌模式下 lpparam.packageName 即微信本身。
 * 只使用系统 View/Activity/Dialog 的公开方法做 Hook，不依赖微信混淆类名，
 * 因此对大多数微信版本都能工作（样式命中率取决于 Targets.kt 的启发式规则）。
 */
class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.tencent.mm") return
        // 微信多进程：只处理主进程，避免 push/appbrand 等进程重复 Hook
        if (lpparam.processName != "com.tencent.mm") return

        try {
            hookActivity()
            hookDialog()
            hookPressFeedback()
            hookSelected()
            XposedBridge.log("[WxGlass] hooked, sdk=${Build.VERSION.SDK_INT}")
        } catch (t: Throwable) {
            XposedBridge.log("[WxGlass] init failed: $t")
        }
    }

    /** 每个 Activity 显示后：沉浸式状态栏 + 遍历 View 树套用玻璃样式 + 入场弹簧。 */
    private fun hookActivity() {
        XposedHelpers.findAndHookMethod(Activity::class.java, "onResume", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val a = param.thisObject as Activity
                runCatching {
                    GlassStyler.setupWindow(a.window)
                    val root = a.window.decorView as ViewGroup
                    root.post { GlassStyler.apply(root) }
                    GlassStyler.watchLayout(root)
                }.onFailure { XposedBridge.log("[WxGlass] onResume: $it") }
            }
        })

        // 页面转场：Telegram 风格——新页面从右侧带阻尼滑入，旧页面轻微视差
        XposedHelpers.findAndHookMethod(Activity::class.java, "onCreate",
            android.os.Bundle::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val a = param.thisObject as Activity
                    runCatching {
                        a.window.decorView.post { SpringMotion.pageEnter(a.window.decorView) }
                    }
                }
            })
    }

    /** 对话框/弹窗/底部菜单：圆角玻璃 + 背景模糊 + 弹性弹出。 */
    private fun hookDialog() {
        XposedHelpers.findAndHookMethod(Dialog::class.java, "show", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val d = param.thisObject as Dialog
                runCatching {
                    val w = d.window ?: return
                    GlassStyler.styleDialogWindow(w)
                    w.decorView.post { SpringMotion.popIn(w.decorView) }
                }.onFailure { XposedBridge.log("[WxGlass] dialog: $it") }
            }
        })
    }

    /** Tab 切换时刷新选中胶囊高亮。 */
    private fun hookSelected() {
        XposedHelpers.findAndHookMethod(View::class.java, "setSelected", Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    runCatching { GlassStyler.onSelectedChanged(param.thisObject as View) }
                }
            })
    }

    /** 通用按压反馈：任何可点击 View 被按下时做弹簧缩放（Telegram 的按压手感）。 */
    private fun hookPressFeedback() {
        XposedHelpers.findAndHookMethod(View::class.java, "setPressed", Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val v = param.thisObject as View
                    if (!v.isClickable || v is ViewGroup && v.childCount > 12) return
                    SpringMotion.press(v, param.args[0] as Boolean)
                }
            })
    }
}
