package com.opendesktop.launcher.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Potwierdzenie tożsamości odciskiem palca, twarzą albo blokadą ekranu.
 * Zwraca funkcję (tytuł, akcja po sukcesie). Bez ustawionej blokady ekranu nie ma czym
 * potwierdzić – wtedy akcja wykonuje się od razu z podpowiedzią, żeby blokadę ustawić.
 */
@Composable
fun rememberDeviceUnlock(): (String, () -> Unit) -> Unit {
    val ctx = LocalContext.current
    val pendingState = remember { arrayOfNulls<() -> Unit>(1) }
    val credential = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) pendingState[0]?.invoke()
        pendingState[0] = null
    }
    val launchCredential by rememberUpdatedState { title: String, onSuccess: () -> Unit ->
        val km = ctx.getSystemService(KeyguardManager::class.java)
        @Suppress("DEPRECATION")
        val intent = km?.createConfirmDeviceCredentialIntent(title, null)
        if (intent == null) onSuccess() else {
            pendingState[0] = onSuccess
            credential.launch(intent)
        }
    }
    return remember(ctx) {
        { title: String, onSuccess: () -> Unit ->
            val km = ctx.getSystemService(KeyguardManager::class.java)
            when {
                km == null || !km.isDeviceSecure -> {
                    Toast.makeText(ctx, "Ustaw blokadę ekranu, żeby chronić ukryte aplikacje", Toast.LENGTH_LONG).show()
                    onSuccess()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> biometric(ctx, title, onSuccess) { launchCredential(title, onSuccess) }
                else -> launchCredential(title, onSuccess)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private fun biometric(ctx: Context, title: String, onSuccess: () -> Unit, fallback: () -> Unit) {
    val builder = BiometricPrompt.Builder(ctx).setTitle(title)
    val executor = ctx.mainExecutor
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        // Android 9: sama biometria, a w razie jej braku – blokada ekranu
        else -> builder.setNegativeButton("Użyj blokady ekranu", executor) { _, _ -> fallback() }
    }
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            when (errorCode) {
                BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT,
                BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS -> fallback()
            }
        }
    }
    runCatching { builder.build().authenticate(CancellationSignal(), executor, callback) }.onFailure { fallback() }
}
