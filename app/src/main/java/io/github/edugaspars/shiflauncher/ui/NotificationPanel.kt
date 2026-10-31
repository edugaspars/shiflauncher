package io.github.edugaspars.shiflauncher.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.edugaspars.shiflauncher.R
import io.github.edugaspars.shiflauncher.notifications.NotificationItem
import io.github.edugaspars.shiflauncher.notifications.NotificationStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheet(appLabel: String, items: List<NotificationItem>, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface.copy(alpha = 1f),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(appLabel, color = palette.fg, fontSize = 22.sp, modifier = Modifier.padding(bottom = 8.dp))
            NotificationList(items, onEmpty = onDismiss, onOpened = onDismiss, maxHeight = 520)
        }
    }
}

@Composable
fun InlineNotificationPanel(items: List<NotificationItem>, onClose: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(palette.fg.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        NotificationList(items, onEmpty = onClose, onOpened = onClose, maxHeight = 380)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationList(items: List<NotificationItem>, onEmpty: () -> Unit, onOpened: () -> Unit, maxHeight: Int) {
    val palette = LocalPalette.current
    if (items.isEmpty()) {
        Text(stringResource(R.string.no_notifications), color = palette.fg.copy(alpha = 0.6f))
        return
    }
    Column(Modifier.heightIn(max = maxHeight.dp).verticalScroll(rememberScrollState())) {
        for (item in items) {
            NotificationCard(item, onOpened)
        }
    }
    if (items.any { it.clearable }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { NotificationStore.dismissAll(items); onEmpty() }) {
                Text(stringResource(R.string.clear_all))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationCard(item: NotificationItem, onOpened: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    var replying by remember(item.key) { mutableStateOf(false) }
    var draft by remember(item.key) { mutableStateOf("") }
    val reply = item.replyAction

    fun send() {
        if (reply == null || draft.isBlank()) return
        if (NotificationStore.reply(context, reply, draft.trim())) {
            draft = ""; replying = false
            Toast.makeText(context, R.string.send, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable { if (NotificationStore.open(item)) onOpened() }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.title.orEmpty(),
                color = palette.fg, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                DateUtils.getRelativeTimeSpanString(item.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                color = palette.fg.copy(alpha = 0.5f), fontSize = 12.sp,
            )
        }
        if (item.lines.size > 1) {
            for (line in item.lines) {
                Text(
                    (line.sender?.let { "$it: " } ?: "") + line.text,
                    color = palette.fg.copy(alpha = 0.8f), fontSize = 14.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
        } else if (!item.text.isNullOrBlank()) {
            Text(item.text, color = palette.fg.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (b in item.buttons) {
                if (b.remoteInput != null) {
                    TextButton(onClick = { replying = !replying }) { Text(b.title.ifBlank { stringResource(R.string.reply) }) }
                } else {
                    TextButton(onClick = { NotificationStore.press(b) }) { Text(b.title) }
                }
            }
            if (item.clearable) {
                TextButton(onClick = { NotificationStore.dismiss(item) }) { Text(stringResource(R.string.dismiss)) }
            }
        }
        if (replying && reply != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(reply.remoteInput?.label?.toString() ?: stringResource(R.string.reply)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { send() }) { Text(stringResource(R.string.send)) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
