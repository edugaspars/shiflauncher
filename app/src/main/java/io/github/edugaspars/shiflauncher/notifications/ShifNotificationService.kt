package io.github.edugaspars.shiflauncher.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Resumo das notificações de um app: quantas existem e o texto da mais recente. */
data class AppNotifications(val count: Int, val title: String?, val text: String?)

object NotificationStore {
    /** Chave: "pacote#userId" -> resumo. */
    internal val state = MutableStateFlow<Map<String, AppNotifications>>(emptyMap())
    val notifications: StateFlow<Map<String, AppNotifications>> = state

    fun key(packageName: String, userHash: Int) = "$packageName#$userHash"

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

class ShifNotificationService : NotificationListenerService() {

    override fun onListenerConnected() = refresh()
    override fun onListenerDisconnected() {
        NotificationStore.state.value = emptyMap()
        requestRebind(ComponentName(this, ShifNotificationService::class.java))
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        NotificationStore.state.value = active
            .filter { sbn ->
                val n = sbn.notification
                !sbn.isOngoing && n.flags and Notification.FLAG_GROUP_SUMMARY == 0
            }
            .groupBy { NotificationStore.key(it.packageName, it.user.hashCode()) }
            .mapValues { (_, list) ->
                val latest = list.maxBy { it.postTime }
                val extras = latest.notification.extras
                AppNotifications(
                    count = list.size,
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                )
            }
    }
}
