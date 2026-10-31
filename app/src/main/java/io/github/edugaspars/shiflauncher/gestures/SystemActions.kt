package io.github.edugaspars.shiflauncher.gestures

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object SystemActions {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.startsWith(context.packageName + "/") }
    }

    /** Bloqueia a tela. Retorna false se o serviço de acessibilidade não estiver ativo. */
    fun lockScreen(): Boolean {
        val service = ShifAccessibilityService.instance ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    /** Abre a aba de notificações (como deslizar para baixo na tela inicial do HyperOS). */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        val viaService = ShifAccessibilityService.instance
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true
        if (viaService) return
        runCatching {
            val sbm = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(sbm)
        }
    }

    fun openAccessibilitySettings(context: Context) = context.startSafely(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    )

    fun openNotificationAccessSettings(context: Context) = context.startSafely(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    )
}

fun Context.startSafely(intent: Intent): Boolean = runCatching {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
}.getOrDefault(false)
