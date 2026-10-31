package io.github.edugaspars.shiflauncher.gestures

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Serviço usado apenas para ações globais (bloquear a tela, abrir notificações).
 * Não lê o conteúdo de nenhuma janela.
 */
class ShifAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        @Volatile var instance: ShifAccessibilityService? = null
            private set
    }
}
