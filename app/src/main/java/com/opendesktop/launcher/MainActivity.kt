package com.opendesktop.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.opendesktop.launcher.data.NotificationBadges
import com.opendesktop.launcher.data.SystemActions
import com.opendesktop.launcher.ui.LauncherScreen
import com.opendesktop.launcher.ui.LauncherViewModel
import com.opendesktop.launcher.ui.components.LocalBadgeCounts
import com.opendesktop.launcher.ui.theme.OpenDesktopTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    /** true = Home wciśnięty na widocznym launcherze, false = powrót z innej aplikacji. */
    private val homePressed = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    private var resumed = false

    private data class PendingWidget(
        val id: Int, val info: AppWidgetProviderInfo,
        val pageId: String, val x: Int, val y: Int, val w: Int, val h: Int,
    )

    private var pending: PendingWidget? = null

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val p = pending ?: return@registerForActivityResult
        if (r.resultCode == RESULT_OK) configureOrFinish(p) else cancelPending()
    }

    private val configureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val p = pending ?: return@registerForActivityResult
        if (r.resultCode == RESULT_OK) finishPending(p) else cancelPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Manifestowy motyw pokazuje znak podczas zimnego startu. Po utworzeniu
        // aktywności wracamy do przezroczystego motywu launchera z tapetą.
        setTheme(R.style.Theme_OpenDesktop)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            vm.messages.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
        }
        setContent {
            val cfg by vm.config.collectAsStateWithLifecycle()
            val badges by NotificationBadges.counts.collectAsStateWithLifecycle()
            OpenDesktopTheme(cfg) {
                // Plakietki na ikonach – puste, gdy wyłączone w ustawieniach albo bez dostępu
                CompositionLocalProvider(LocalBadgeCounts provides if (cfg.notificationBadges) badges else emptyMap()) {
                    LauncherScreen(vm = vm, homePressed = homePressed, onAddWidget = ::startAddWidget)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homePressed.tryEmit(resumed)
    }

    override fun onPause() {
        super.onPause()
        resumed = false
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        restoreHomeAfterUpdate()
    }

    /**
     * Część systemów (np. MIUI/HyperOS, One UI) przy aktualizacji odbiera launcherowi rolę
     * domyślnego ekranu głównego. Zapamiętujemy, że byliśmy domyślni, i po pierwszym starcie
     * nowej wersji od razu prosimy systemowym okienkiem o przywrócenie – raz na wersję.
     */
    private fun restoreHomeAfterUpdate() {
        val prefs = getSharedPreferences("home_role", MODE_PRIVATE)
        val isDefault = SystemActions.isDefaultLauncher(this)
        val version = BuildConfig.VERSION_CODE
        val lastVersion = prefs.getInt("version", version)
        val wasDefault = prefs.getBoolean("wasDefault", false)
        prefs.edit().putInt("version", version).putBoolean("wasDefault", isDefault || (wasDefault && lastVersion != version)).apply()
        if (!isDefault && wasDefault && lastVersion != version) {
            Toast.makeText(this, "Aktualizacja wyłączyła OpenDesktop jako ekran główny – przywróć go", Toast.LENGTH_LONG).show()
            SystemActions.requestDefaultLauncher(this)
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { vm.widgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { vm.widgetHost.stopListening() }
    }

    // ------------------------------------------------------------- dodawanie widżetu

    private fun startAddWidget(info: AppWidgetProviderInfo, pageIndex: Int) {
        val (w, h) = vm.widgetSpan(info)
        val spot = vm.reserveWidgetSpot(pageIndex, w, h)
        val id = vm.widgetHost.allocateAppWidgetId()
        val p = PendingWidget(id, info, spot[0] as String, spot[1] as Int, spot[2] as Int, w, h)
        pending = p
        val awm = AppWidgetManager.getInstance(this)
        val bound = runCatching { awm.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null) }.getOrDefault(false)
        if (bound) {
            configureOrFinish(p)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            runCatching { bindLauncher.launch(intent) }.onFailure { cancelPending() }
        }
    }

    private fun configureOrFinish(p: PendingWidget) {
        if (p.info.configure == null) {
            finishPending(p)
            return
        }
        // Najpierw przez host (obsługuje aktywności nieeksportowane), potem bezpośrednio
        val viaHost = runCatching {
            vm.widgetHost.startAppWidgetConfigureActivityForResult(this, p.id, 0, REQ_CONFIGURE, null)
        }.isSuccess
        if (!viaHost) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(p.info.configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, p.id)
            runCatching { configureLauncher.launch(intent) }.onFailure { finishPending(p) }
        }
    }

    @Deprecated("Wymagane przez AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONFIGURE) {
            val p = pending ?: return
            if (resultCode == RESULT_OK) finishPending(p) else cancelPending()
        }
    }

    private fun finishPending(p: PendingWidget) {
        vm.addWidgetItem(p.pageId, p.x, p.y, p.w, p.h, p.id, p.info.provider.flattenToString())
        pending = null
    }

    private fun cancelPending() {
        pending?.let { runCatching { vm.widgetHost.deleteAppWidgetId(it.id) } }
        pending = null
    }

    companion object {
        private const val REQ_CONFIGURE = 5101
    }
}
