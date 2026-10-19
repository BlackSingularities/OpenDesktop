package com.opendesktop.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import kotlin.math.abs

class LauncherWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
        LauncherWidgetHostView(context)

    companion object {
        const val HOST_ID = 0x0D35
    }
}

/**
 * Widok widżetu, który rozpoznaje długie przytrzymanie (menu / edycja) bez blokowania
 * zwykłych dotknięć widżetu.
 */
class LauncherWidgetHostView(context: Context) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null
    private var longPressed = false
    private var downX = 0f
    private var downY = 0f
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var lastW = -1f
    private var lastH = -1f

    private val trigger = Runnable {
        longPressed = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressed = false
                downX = ev.x
                downY = ev.y
                removeCallbacks(trigger)
                postDelayed(trigger, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) removeCallbacks(trigger)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(trigger)
        }
        return longPressed
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (longPressed) {
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) longPressed = false
            return true
        }
        return super.onTouchEvent(ev)
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        removeCallbacks(trigger)
    }

    /**
     * Rozciąganie: wiele widżetów rysuje treść o stałej wysokości mimo podanego rozmiaru.
     * Wtedy skalujemy faktyczną treść (sumę obszarów jej elementów) na cały kafelek.
     * Transformacja widoku przelicza też dotyk, więc przyciski widżetu dalej działają.
     */
    var stretch = false
        set(v) {
            if (field != v) {
                field = v
                requestLayout()
            }
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val root = getChildAt(0) ?: return
        if (!stretch || width <= 0 || height <= 0) {
            root.scaleX = 1f; root.scaleY = 1f; root.translationX = 0f; root.translationY = 0f
            return
        }
        var l = Int.MAX_VALUE; var t = Int.MAX_VALUE; var r = Int.MIN_VALUE; var b = Int.MIN_VALUE
        if (root is ViewGroup && root.childCount > 0) {
            for (i in 0 until root.childCount) {
                val c = root.getChildAt(i)
                if (c.visibility != VISIBLE || c.width == 0 || c.height == 0) continue
                l = minOf(l, c.left); t = minOf(t, c.top); r = maxOf(r, c.right); b = maxOf(b, c.bottom)
            }
        }
        if (l == Int.MAX_VALUE) { l = 0; t = 0; r = root.width; b = root.height }
        val sx = (width.toFloat() / (r - l).coerceAtLeast(1)).coerceIn(0.5f, 3f)
        val sy = (height.toFloat() / (b - t).coerceAtLeast(1)).coerceIn(0.5f, 3f)
        root.pivotX = 0f; root.pivotY = 0f
        root.scaleX = sx; root.scaleY = sy
        root.translationX = -root.left - l * sx
        root.translationY = -root.top - t * sy
    }

    fun updateSize(wDp: Float, hDp: Float) {
        if (wDp <= 0f || hDp <= 0f) return
        if (abs(wDp - lastW) < 1f && abs(hDp - lastH) < 1f) return
        lastW = wDp
        lastH = hDp
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                updateAppWidgetSize(Bundle(), listOf(SizeF(wDp, hDp)))
            } else {
                @Suppress("DEPRECATION")
                updateAppWidgetSize(null, wDp.toInt(), hDp.toInt(), wDp.toInt(), hDp.toInt())
            }
        }
    }
}
