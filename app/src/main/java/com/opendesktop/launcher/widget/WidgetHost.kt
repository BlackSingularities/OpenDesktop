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
