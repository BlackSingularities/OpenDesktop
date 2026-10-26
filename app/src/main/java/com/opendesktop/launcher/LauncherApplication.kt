package com.opendesktop.launcher

import android.app.Application
import com.opendesktop.launcher.data.AppRepository
import com.opendesktop.launcher.data.LauncherStore
import com.opendesktop.launcher.data.NotificationBadges
import com.opendesktop.launcher.widget.LauncherWidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LauncherApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var store: LauncherStore
        private set
    lateinit var apps: AppRepository
        private set
    lateinit var widgetHost: LauncherWidgetHost
        private set

    override fun onCreate() {
        super.onCreate()
        store = LauncherStore(this, appScope)
        apps = AppRepository(this, appScope, store.config.value.iconShape)
        widgetHost = LauncherWidgetHost(this)
        NotificationBadges.ensureConnected(this)
    }
}
