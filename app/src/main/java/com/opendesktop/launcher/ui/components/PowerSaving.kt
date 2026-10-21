package com.opendesktop.launcher.ui.components

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.opendesktop.launcher.data.PowerSaver

/** Czy telefon jest „słaby”: system sam to zgłasza albo ma mniej niż 3 GB RAM. */
fun isLowEndDevice(ctx: Context): Boolean {
    val am = ctx.getSystemService(ActivityManager::class.java) ?: return false
    if (am.isLowRamDevice) return true
    val info = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
    return info.totalMem in 1 until 3L * 1024 * 1024 * 1024
}

/** Czy tryb oszczędny działa teraz – śledzi systemowe oszczędzanie baterii. */
@Composable
fun rememberPowerSaving(mode: PowerSaver): Boolean {
    val ctx = LocalContext.current
    val pm = remember(ctx) { ctx.getSystemService(PowerManager::class.java) }
    val lowEnd = remember(ctx) { isLowEndDevice(ctx) }
    var batterySaver by remember { mutableStateOf(pm?.isPowerSaveMode == true) }
    DisposableEffect(ctx, mode) {
        if (mode != PowerSaver.Auto) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                batterySaver = pm?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            ctx, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        batterySaver = pm?.isPowerSaveMode == true
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }
    return when (mode) {
        PowerSaver.Off -> false
        PowerSaver.On -> true
        PowerSaver.Auto -> batterySaver || lowEnd
    }
}
