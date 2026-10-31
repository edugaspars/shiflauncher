package io.github.edugaspars.shiflauncher.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.edugaspars.shiflauncher.BuildConfigInfo
import io.github.edugaspars.shiflauncher.R
import io.github.edugaspars.shiflauncher.data.AppEntry
import io.github.edugaspars.shiflauncher.data.LauncherSettings
import io.github.edugaspars.shiflauncher.data.Prefs
import io.github.edugaspars.shiflauncher.data.SwipeDownAction
import io.github.edugaspars.shiflauncher.gestures.SystemActions
import io.github.edugaspars.shiflauncher.gestures.startSafely
import io.github.edugaspars.shiflauncher.notifications.NotificationStore

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    prefs: Prefs,
    favorites: List<AppEntry>,
    hiddenApps: List<AppEntry>,
    resumeTick: Int,
    onRequestCalendar: () -> Unit,
    onSetDefault: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val notificationAccess = remember(resumeTick) { NotificationStore.hasAccess(context) }
    val accessibility = remember(resumeTick) { SystemActions.isAccessibilityEnabled(context) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.settings),
                fontSize = 30.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(stringResource(R.string.back)) }
        }

        Section(R.string.section_appearance)
        SwitchRow(R.string.black_background, settings.blackBackground, stringResource(R.string.black_background_desc)) {
            prefs.update { s -> s.copy(blackBackground = it) }
        }
        SwitchRow(R.string.show_icons, settings.showIcons) { prefs.update { s -> s.copy(showIcons = it) } }
        SwitchRow(R.string.show_notifications, settings.showNotifications) {
            prefs.update { s -> s.copy(showNotifications = it) }
            if (it && !notificationAccess) SystemActions.openNotificationAccessSettings(context)
        }
        SwitchRow(R.string.show_calendar, settings.showCalendar) {
            prefs.update { s -> s.copy(showCalendar = it) }
            if (it) onRequestCalendar()
        }

        Section(R.string.section_gestures)
        Hint(stringResource(R.string.gesture_desc))
        SwitchRow(
            R.string.double_tap_lock, settings.doubleTapLock,
            if (!accessibility) stringResource(R.string.needs_accessibility) else null,
        ) {
            prefs.update { s -> s.copy(doubleTapLock = it) }
            if (it && !accessibility) SystemActions.openAccessibilitySettings(context)
        }
        Text(
            stringResource(R.string.swipe_down_label),
            color = Color.White, fontSize = 16.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        for ((action, label) in listOf(
            SwipeDownAction.NOTIFICATIONS to R.string.swipe_down_shade,
            SwipeDownAction.SEARCH to R.string.swipe_down_search,
            SwipeDownAction.NONE to R.string.swipe_down_none,
        )) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.update { s -> s.copy(swipeDown = action) } },
            ) {
                RadioButton(
                    selected = settings.swipeDown == action,
                    onClick = { prefs.update { s -> s.copy(swipeDown = action) } },
                )
                Text(stringResource(label), color = Color.White.copy(alpha = 0.9f))
            }
        }
        Hint(stringResource(R.string.gestures_hyperos_tip))

        Section(R.string.section_favorites)
        if (favorites.isEmpty()) Hint(stringResource(R.string.no_favorites))
        favorites.forEachIndexed { i, app ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    app.label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { prefs.moveFavorite(app.key, -1) }, enabled = i > 0) {
                    Text(stringResource(R.string.move_up))
                }
                TextButton(onClick = { prefs.moveFavorite(app.key, 1) }, enabled = i < favorites.lastIndex) {
                    Text(stringResource(R.string.move_down))
                }
                TextButton(onClick = { prefs.toggleFavorite(app.key) }) { Text(stringResource(R.string.remove)) }
            }
        }

        Section(R.string.section_hidden)
        if (hiddenApps.isEmpty()) Hint(stringResource(R.string.no_hidden))
        for (app in hiddenApps) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    app.label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { prefs.setHidden(app.key, false) }) { Text(stringResource(R.string.unhide)) }
            }
        }

        Section(R.string.section_system)
        LinkRow(R.string.notification_access, if (notificationAccess) R.string.enabled else R.string.disabled) {
            SystemActions.openNotificationAccessSettings(context)
        }
        LinkRow(R.string.accessibility_service, if (accessibility) R.string.enabled else R.string.disabled) {
            SystemActions.openAccessibilitySettings(context)
        }
        LinkRow(R.string.set_default, null, onSetDefault)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LinkRow(R.string.language, null) {
                context.startSafely(
                    Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Hint("ShifLauncher ${BuildConfigInfo.versionName(context)}")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: Int) {
    Text(
        stringResource(title),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SwitchRow(title: Int, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), color = Color.White, fontSize = 16.sp)
            subtitle?.let { Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: Int, status: Int?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(stringResource(title), color = Color.White, fontSize = 16.sp)
        status?.let { Text(stringResource(it), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) }
    }
}
