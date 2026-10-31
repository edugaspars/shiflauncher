package io.github.edugaspars.shiflauncher.gestures

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import io.github.edugaspars.shiflauncher.ShifApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Serviço usado para ações globais (bloquear a tela, abrir notificações) e para
 * os gestos de navegação nas bordas. Não lê o conteúdo de nenhuma janela.
 */
class ShifAccessibilityService : AccessibilityService() {
    private var scope: CoroutineScope? = null
    private var gestures: NavigationGestures? = null
    private var gestureConfig: Pair<Boolean, Boolean> = false to true

    override fun onServiceConnected() {
        instance = this
        gestures = NavigationGestures(this)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { s ->
            s.launch {
                (application as ShifApp).prefs.settings
                    .map { it.navGestures to it.navLine }
                    .distinctUntilChanged()
                    .collect { config -> gestureConfig = config; applyGestures() }
            }
        }
    }

    private fun applyGestures() {
        val (enabled, line) = gestureConfig
        if (enabled) gestures?.attach(line) else gestures?.detach()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyGestures() // recalcula as áreas ao girar a tela
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        gestures?.detach()
        gestures = null
        scope?.cancel()
        scope = null
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        @Volatile var instance: ShifAccessibilityService? = null
            private set
    }
}
