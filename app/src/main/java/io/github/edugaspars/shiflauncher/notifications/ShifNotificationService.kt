package io.github.edugaspars.shiflauncher.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Uma mensagem dentro de uma conversa (MessagingStyle). */
data class ChatLine(val sender: String?, val text: String, val time: Long)

/** Um botão da notificação. [remoteInput] != null = ação de resposta. */
data class NotificationButton(val title: String, val intent: PendingIntent, val remoteInput: RemoteInput?)

data class NotificationItem(
    val key: String,
    val title: String?,
    val text: String?,
    val time: Long,
    val clearable: Boolean,
    val contentIntent: PendingIntent?,
    val lines: List<ChatLine>,
    val buttons: List<NotificationButton>,
) {
    val replyAction: NotificationButton? get() = buttons.firstOrNull { it.remoteInput != null }
}

/** Resumo das notificações de um app: quantas existem, o texto da mais recente e todas elas. */
data class AppNotifications(
    val count: Int,
    val title: String?,
    val text: String?,
    val items: List<NotificationItem> = emptyList(),
)

object NotificationStore {
    /** Chave: "pacote#userId" -> resumo. */
    internal val state = MutableStateFlow<Map<String, AppNotifications>>(emptyMap())
    val notifications: StateFlow<Map<String, AppNotifications>> = state
    internal var service: ShifNotificationService? = null

    fun key(packageName: String, userHash: Int) = "$packageName#$userHash"

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun dismiss(item: NotificationItem) {
        runCatching { service?.cancelNotification(item.key) }
    }

    fun dismissAll(items: List<NotificationItem>) {
        val keys = items.filter { it.clearable }.map { it.key }.toTypedArray()
        if (keys.isNotEmpty()) runCatching { service?.cancelNotifications(keys) }
    }

    fun open(item: NotificationItem): Boolean = runCatching {
        item.contentIntent?.send(); item.contentIntent != null
    }.getOrDefault(false)

    fun press(button: NotificationButton) {
        runCatching { button.intent.send() }
    }

    fun reply(context: Context, button: NotificationButton, text: String): Boolean {
        val input = button.remoteInput ?: return false
        return runCatching {
            val intent = Intent()
            val results = Bundle().apply { putCharSequence(input.resultKey, text) }
            RemoteInput.addResultsToIntent(arrayOf(input), intent, results)
            button.intent.send(context, 0, intent)
            true
        }.getOrDefault(false)
    }
}

class ShifNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationStore.service = this
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationStore.service = null
        NotificationStore.state.value = emptyMap()
        requestRebind(ComponentName(this, ShifNotificationService::class.java))
    }

    override fun onDestroy() {
        if (NotificationStore.service === this) NotificationStore.service = null
        super.onDestroy()
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
                val items = list.sortedByDescending { it.postTime }.map(::toItem)
                val latest = items.first()
                AppNotifications(count = items.size, title = latest.title, text = latest.text, items = items)
            }
    }

    private fun toItem(sbn: StatusBarNotification): NotificationItem {
        val n = sbn.notification
        val extras = n.extras
        val lines = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
                ?.messages
                ?.mapNotNull { m ->
                    val t = m.text?.toString() ?: return@mapNotNull null
                    ChatLine(m.person?.name?.toString(), t, m.timestamp)
                }
                ?.takeLast(6)
        }.getOrNull().orEmpty()
        val buttons = n.actions.orEmpty().mapNotNull { a ->
            val intent = a.actionIntent ?: return@mapNotNull null
            NotificationButton(
                title = a.title?.toString().orEmpty(),
                intent = intent,
                remoteInput = a.remoteInputs?.firstOrNull { it.allowFreeFormInput },
            )
        }
        return NotificationItem(
            key = sbn.key,
            title = (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
                ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString(),
            text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString(),
            time = sbn.postTime,
            clearable = sbn.isClearable,
            contentIntent = n.contentIntent,
            lines = lines,
            buttons = buttons,
        )
    }
}
