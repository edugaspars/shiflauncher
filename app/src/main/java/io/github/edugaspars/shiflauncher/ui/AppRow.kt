package io.github.edugaspars.shiflauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import io.github.edugaspars.shiflauncher.data.NotificationPanelStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.edugaspars.shiflauncher.R
import io.github.edugaspars.shiflauncher.data.AppEntry
import io.github.edugaspars.shiflauncher.notifications.AppNotifications

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppEntry,
    notifications: AppNotifications?,
    showIcon: Boolean,
    large: Boolean,
    shadow: Boolean,
    menuOpen: Boolean,
    panelStyle: NotificationPanelStyle,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    menu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val palette = LocalPalette.current
    var downX by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    var panelOpen by remember(app.key) { mutableStateOf(false) }
    val items = notifications?.items.orEmpty()
    if (items.isEmpty()) panelOpen = false
    Column(modifier) {
      Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { width = it.width }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        downX = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position.x
                    }
                }
                .combinedClickable(
                    onClick = { if (panelOpen) panelOpen = false else onClick() },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Metade direita + notificações = resumo; senão, menu do app.
                        if (items.isNotEmpty() && downX > width / 2f) panelOpen = !panelOpen
                        else onLongClick()
                    },
                )
                .padding(vertical = if (large) 10.dp else 8.dp),
        ) {
            if (showIcon) {
                val iconSize = if (large) 40.dp else 34.dp
                Box {
                    if (app.icon != null) {
                        Image(app.icon, contentDescription = null, colorFilter = palette.iconFilter, modifier = Modifier.size(iconSize))
                    } else {
                        Spacer(Modifier.size(iconSize))
                    }
                    if (notifications != null) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        color = LocalPalette.current.fg,
                        fontSize = if (large) 22.sp else 18.sp,
                        fontWeight = if (large) FontWeight.Normal else FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.onWallpaper(shadow),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (notifications != null && !showIcon) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    }
                }
                if (notifications != null) {
                    val preview = listOfNotNull(notifications.title, notifications.text)
                        .joinToString(": ")
                        .ifBlank { stringResource(R.string.notifications_count, notifications.count) }
                    Text(
                        text = if (notifications.count > 1) "${notifications.count} · $preview" else preview,
                        color = LocalPalette.current.fg.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.onWallpaper(shadow),
                    )
                }
            }
        }
        if (menuOpen) menu()
      }
        if (panelOpen) {
            when (panelStyle) {
                NotificationPanelStyle.INLINE -> InlineNotificationPanel(items) { panelOpen = false }
                NotificationPanelStyle.SHEET -> NotificationSheet(app.label, items) { panelOpen = false }
            }
        }
    }
}

@Composable
fun AppMenu(
    app: AppEntry,
    isFavorite: Boolean,
    isHidden: Boolean,
    actions: AppActions,
    onRename: () -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        val items = listOf(
            (if (isFavorite) R.string.remove_favorite else R.string.add_favorite) to { actions.toggleFavorite(app) },
            R.string.rename to onRename,
            (if (isHidden) R.string.unhide else R.string.hide) to { actions.setHidden(app, !isHidden) },
            R.string.app_info to { actions.openInfo(app) },
            R.string.uninstall to { actions.uninstall(app) },
        )
        for ((label, action) in items) {
            DropdownMenuItem(
                text = { Text(stringResource(label)) },
                onClick = { onDismiss(); action() },
            )
        }
    }
}
