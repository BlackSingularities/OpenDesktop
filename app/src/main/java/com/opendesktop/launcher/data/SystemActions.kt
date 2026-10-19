package com.opendesktop.launcher.data

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings

object SystemActions {
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    @SuppressLint("WrongConstant")
    fun openQuickSettings(context: Context) {
        runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandSettingsPanel").invoke(service)
        }
    }

    fun openWallpaperPicker(context: Context) {
        runCatching {
            context.startActivity(
                Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wybierz tapetę")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openClock(context: Context) {
        runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun webSearch(context: Context, query: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://duckduckgo.com/?q=" + android.net.Uri.encode(query)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun openUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Uruchamia launcher ponownie: nowy start aktywności i zakończenie bieżącego procesu. */
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return
        context.startActivity(Intent.makeRestartActivityTask(intent.component))
        Runtime.getRuntime().exit(0)
    }

    fun isDefaultLauncher(context: Context): Boolean {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == context.packageName
    }

    @Suppress("DEPRECATION")
    fun requestDefaultLauncher(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = activity.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { activity.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 7001) }
                    .onSuccess { return }
            }
        }
        runCatching { activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
    }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
