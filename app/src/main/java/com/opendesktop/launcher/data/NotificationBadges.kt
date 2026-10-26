package com.opendesktop.launcher.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Liczba aktywnych powiadomień na pakiet – źródło plakietek na ikonach. */
object NotificationBadges {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts

    fun hasAccess(ctx: Context): Boolean =
        ctx.packageName in NotificationManagerCompat.getEnabledListenerPackages(ctx)

    /**
     * Po starcie procesu system nie zawsze od razu podłącza nasłuch – prośba o ponowne
     * podłączenie sprawia, że plakietki pojawiają się bez czekania na kolejne powiadomienie.
     */
    fun ensureConnected(ctx: Context) {
        if (!hasAccess(ctx)) return
        runCatching {
            NotificationListenerService.requestRebind(ComponentName(ctx, BadgeListenerService::class.java))
        }
    }

    fun openAccessSettings(ctx: Context) {
        val component = ComponentName(ctx, BadgeListenerService::class.java).flattenToString()
        val direct = Intent("android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS")
            .putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", component)
        val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        for (i in listOf(direct, list)) {
            if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    internal fun publish(active: Array<StatusBarNotification>?) {
        _counts.value = countByPackage(active.orEmpty().filter(::counts).map { it.packageName })
    }

    internal fun clear() {
        _counts.value = emptyMap()
    }

    /** Stałe (muzyka, nawigacja) i podsumowania grup nie są nowymi wiadomościami. */
    private fun counts(sbn: StatusBarNotification): Boolean {
        val flags = sbn.notification.flags
        return sbn.isClearable &&
            flags and Notification.FLAG_ONGOING_EVENT == 0 &&
            flags and Notification.FLAG_GROUP_SUMMARY == 0
    }

    fun countByPackage(packages: List<String>): Map<String, Int> = packages.groupingBy { it }.eachCount()
}

class BadgeListenerService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onListenerDisconnected() = NotificationBadges.clear()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        NotificationBadges.publish(runCatching { activeNotifications }.getOrNull())
    }
}
