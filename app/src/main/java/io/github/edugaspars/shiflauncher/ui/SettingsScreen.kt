package io.github.edugaspars.shiflauncher.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.edugaspars.shiflauncher.BuildConfigInfo
import io.github.edugaspars.shiflauncher.R
import io.github.edugaspars.shiflauncher.data.AppEntry
import io.github.edugaspars.shiflauncher.data.LauncherSettings
import io.github.edugaspars.shiflauncher.data.MAX_FAVORITES
import io.github.edugaspars.shiflauncher.data.NotificationPanelStyle
import io.github.edugaspars.shiflauncher.data.Prefs
import io.github.edugaspars.shiflauncher.data.SwipeDownAction
import io.github.edugaspars.shiflauncher.data.ThemeMode
import io.github.edugaspars.shiflauncher.gestures.SystemActions
import io.github.edugaspars.shiflauncher.gestures.startSafely
import io.github.edugaspars.shiflauncher.notifications.NotificationStore
import kotlin.math.roundToInt

enum class SettingsPage { ROOT, APPEARANCE, NOTIFICATIONS, GESTURES, NAVIGATION, FAVORITES, HIDDEN, SYSTEM, SETUP }

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    prefs: Prefs,
    favorites: List<AppEntry>,
    allApps: List<AppEntry>,
    hiddenApps: List<AppEntry>,
    resumeTick: Int,
    isDefaultLauncher: Boolean,
    startPage: SettingsPage,
    onRequestCalendar: () -> Unit,
    onSetDefault: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var page by remember(startPage) { mutableStateOf(startPage) }
    val status = SetupStatus(
        isDefault = isDefaultLauncher,
        notifications = remember(resumeTick) { NotificationStore.hasAccess(context) },
        accessibility = remember(resumeTick) { SystemActions.isAccessibilityEnabled(context) },
    )
    val finishSetup = { prefs.update { it.copy(setupDone = true) } }

    BackHandler {
        when {
            page == SettingsPage.SETUP && startPage == SettingsPage.SETUP -> { finishSetup(); onClose() }
            page != SettingsPage.ROOT -> page = SettingsPage.ROOT
            else -> onClose()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(LocalPalette.current.surface.copy(alpha = 1f))
            .systemBarsPadding()
    ) {
        AnimatedContent(page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { p ->
            val back = { if (p == SettingsPage.ROOT) onClose() else page = SettingsPage.ROOT }
            when (p) {
                SettingsPage.ROOT -> RootPage(settings, favorites.size, hiddenApps.size, status, onClose) { page = it }
                SettingsPage.APPEARANCE -> Page(R.string.section_appearance, back) {
                    AppearancePage(settings, prefs, onRequestCalendar)
                }
                SettingsPage.NOTIFICATIONS -> Page(R.string.section_notifications, back) {
                    NotificationsPage(settings, prefs, status)
                }
                SettingsPage.GESTURES -> Page(R.string.section_gestures, back) { GesturesPage(settings, prefs, status) }
                SettingsPage.NAVIGATION -> Page(R.string.section_navigation, back) { NavigationPage(settings, prefs, status) }
                SettingsPage.FAVORITES -> Page(R.string.section_favorites, back) { FavoritesPage(favorites, allApps, prefs) }
                SettingsPage.HIDDEN -> Page(R.string.section_hidden, back) { HiddenPage(hiddenApps, prefs) }
                SettingsPage.SYSTEM -> Page(R.string.section_system, back) {
                    SystemPage(status, onSetDefault) { page = SettingsPage.SETUP }
                }
                SettingsPage.SETUP -> SetupPage(status, onSetDefault) {
                    finishSetup()
                    if (startPage == SettingsPage.SETUP) onClose() else page = SettingsPage.ROOT
                }
            }
        }
    }
}

data class SetupStatus(val isDefault: Boolean, val notifications: Boolean, val accessibility: Boolean) {
    val complete get() = isDefault && notifications && accessibility
}

/* ---------- Raiz ---------- */

@Composable
private fun RootPage(
    settings: LauncherSettings,
    favoriteCount: Int,
    hiddenCount: Int,
    status: SetupStatus,
    onClose: () -> Unit,
    open: (SettingsPage) -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings), fontSize = 32.sp, color = LocalPalette.current.fg, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = LocalPalette.current.fg) }
        }
        Spacer(Modifier.height(12.dp))
        if (!status.complete) {
            Card(onClick = { open(SettingsPage.SETUP) }, highlight = true) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Title(stringResource(R.string.setup_banner))
                    Sub(stringResource(R.string.setup_banner_desc))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Category(Icons.Default.Edit, R.string.section_appearance, themeName(settings.theme)) { open(SettingsPage.APPEARANCE) }
        Category(Icons.Default.Star, R.string.section_favorites, "$favoriteCount / $MAX_FAVORITES") { open(SettingsPage.FAVORITES) }
        Category(
            Icons.Default.Notifications, R.string.section_notifications,
            stringResource(if (settings.showNotifications) R.string.on else R.string.off),
        ) { open(SettingsPage.NOTIFICATIONS) }
        Category(Icons.Default.Home, R.string.section_gestures, stringResource(R.string.gestures_summary)) { open(SettingsPage.GESTURES) }
        Category(
            Icons.Default.Menu, R.string.section_navigation,
            stringResource(if (settings.navGestures) R.string.on else R.string.off),
        ) { open(SettingsPage.NAVIGATION) }
        Category(Icons.Default.Lock, R.string.section_hidden, hiddenCount.toString()) { open(SettingsPage.HIDDEN) }
        Category(
            Icons.Default.Settings, R.string.section_system,
            stringResource(if (status.complete) R.string.all_ok else R.string.needs_attention),
        ) { open(SettingsPage.SYSTEM) }
        Spacer(Modifier.height(24.dp))
        Sub("ShifLauncher ${BuildConfigInfo.versionName(context)}", Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun themeName(mode: ThemeMode) = stringResource(themeLabel(mode))

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.WALLPAPER -> R.string.theme_wallpaper
    ThemeMode.BLACK -> R.string.theme_black
    ThemeMode.WHITE -> R.string.theme_white
    ThemeMode.MONO_BLACK -> R.string.theme_mono_black
    ThemeMode.KINDLE -> R.string.theme_kindle
    ThemeMode.KINDLE_DARK -> R.string.theme_kindle_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

@Composable
private fun Category(icon: ImageVector, title: Int, summary: String, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Icon(icon, null, tint = LocalPalette.current.fg.copy(alpha = 0.8f))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Title(stringResource(title))
            Sub(summary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = LocalPalette.current.fg.copy(alpha = 0.4f))
    }
    Spacer(Modifier.height(8.dp))
}

/* ---------- Aparência ---------- */

@Composable
private fun AppearancePage(settings: LauncherSettings, prefs: Prefs, onRequestCalendar: () -> Unit) {
    Label(R.string.theme)
    val modes = ThemeMode.entries
    for (row in modes.chunked(4)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            for (mode in row) {
                ThemePreview(mode, selected = settings.theme == mode, Modifier.weight(1f)) {
                    prefs.update { it.copy(theme = mode) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    Spacer(Modifier.height(8.dp))
    Toggle(R.string.show_icons, settings.showIcons) { v -> prefs.update { it.copy(showIcons = v) } }
    Toggle(R.string.show_calendar, settings.showCalendar) { v ->
        prefs.update { it.copy(showCalendar = v) }
        if (v) onRequestCalendar()
    }
}

/** Miniatura da tela inicial no tema [mode]. */
@Composable
private fun ThemePreview(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = paletteFor(if (mode == ThemeMode.SYSTEM) ThemeMode.BLACK else mode)
    val shape = RoundedCornerShape(14.dp)
    val bg = when (mode) {
        ThemeMode.WALLPAPER -> Brush.verticalGradient(listOf(Color(0xFF3A6073), Color(0xFF16222A)))
        ThemeMode.SYSTEM -> Brush.horizontalGradient(0.5f to Color.Black, 0.5f to Color.White)
        else -> Brush.verticalGradient(listOf(p.background, p.background))
    }
    val iconColors = if (p.mono) listOf(Color.Gray, Color.Gray, Color.Gray)
    else listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF7043))
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(shape)
                .background(bg)
                .border(
                    if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    else BorderStroke(1.dp, LocalPalette.current.fg.copy(alpha = 0.2f)),
                    shape,
                )
                .padding(10.dp)
        ) {
            val fg = if (mode == ThemeMode.SYSTEM) Color.Gray else p.fg
            Column {
                Text(
                    "12:30", color = fg, fontSize = 18.sp,
                    fontFamily = if (p.serif) FontFamily.Serif else null,
                )
                Spacer(Modifier.weight(1f))
                for (c in iconColors) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.size(8.dp).background(c, CircleShape))
                        Spacer(Modifier.width(5.dp))
                        Box(Modifier.height(4.dp).fillMaxWidth(0.8f).background(fg.copy(alpha = 0.7f), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(themeShortLabel(mode)), fontSize = 12.sp, maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.primary else LocalPalette.current.fg.copy(alpha = 0.8f),
        )
    }
}

private fun themeShortLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.WALLPAPER -> R.string.theme_short_wallpaper
    ThemeMode.BLACK -> R.string.theme_black
    ThemeMode.WHITE -> R.string.theme_white
    ThemeMode.MONO_BLACK -> R.string.theme_short_mono
    ThemeMode.KINDLE -> R.string.theme_short_kindle
    ThemeMode.KINDLE_DARK -> R.string.theme_short_kindle_dark
    ThemeMode.SYSTEM -> R.string.theme_short_system
}

/* ---------- Notificações ---------- */

@Composable
private fun NotificationsPage(settings: LauncherSettings, prefs: Prefs, status: SetupStatus) {
    val context = LocalContext.current
    if (!status.notifications) {
        Fix(R.string.notification_access_missing) { SystemActions.openNotificationAccessSettings(context) }
    }
    Toggle(R.string.show_notifications, settings.showNotifications) { v ->
        prefs.update { it.copy(showNotifications = v) }
        if (v && !status.notifications) SystemActions.openNotificationAccessSettings(context)
    }
    if (settings.showNotifications) {
        Label(R.string.notification_panel)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelChoice(
                R.string.notification_panel_inline, inline = true,
                selected = settings.notificationPanel == NotificationPanelStyle.INLINE, modifier = Modifier.weight(1f),
            ) { prefs.update { it.copy(notificationPanel = NotificationPanelStyle.INLINE) } }
            PanelChoice(
                R.string.notification_panel_sheet, inline = false,
                selected = settings.notificationPanel == NotificationPanelStyle.SHEET, modifier = Modifier.weight(1f),
            ) { prefs.update { it.copy(notificationPanel = NotificationPanelStyle.SHEET) } }
        }
        Spacer(Modifier.height(16.dp))
        Explain(Gesture.LONG_PRESS, R.string.notification_panel_hint)
    }
}

/** Desenho simples de como o resumo aparece. */
@Composable
private fun PanelChoice(label: Int, inline: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fg = LocalPalette.current.fg
    val shape = RoundedCornerShape(14.dp)
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(shape)
                .border(
                    if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, fg.copy(alpha = 0.2f)),
                    shape,
                )
                .padding(10.dp)
        ) {
            repeat(2) { Bar(fg, 0.7f) }
            if (inline) {
                Box(Modifier.fillMaxWidth().height(34.dp).padding(vertical = 3.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(6.dp)))
                Bar(fg, 0.6f)
            } else {
                Bar(fg, 0.6f)
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(label), fontSize = 12.sp, color = if (selected) MaterialTheme.colorScheme.primary else fg.copy(alpha = 0.8f))
    }
}

@Composable
private fun Bar(fg: Color, w: Float) {
    Box(Modifier.padding(vertical = 4.dp).height(6.dp).fillMaxWidth(w).background(fg.copy(alpha = 0.4f), RoundedCornerShape(3.dp)))
}

/* ---------- Gestos ---------- */

@Composable
private fun GesturesPage(settings: LauncherSettings, prefs: Prefs, status: SetupStatus) {
    val context = LocalContext.current
    Explain(Gesture.SWIPE_UP_DRAWER, R.string.g_swipe_up_drawer)
    Explain(Gesture.LONG_PRESS, R.string.g_long_press_settings)
    Explain(Gesture.DOUBLE_TAP, R.string.double_tap_lock, trailing = {
        Switch(settings.doubleTapLock, { v ->
            prefs.update { it.copy(doubleTapLock = v) }
            if (v && !status.accessibility) SystemActions.openAccessibilitySettings(context)
        })
    })
    if (settings.doubleTapLock && !status.accessibility) {
        Fix(R.string.needs_accessibility) { SystemActions.openAccessibilitySettings(context) }
    }
    Explain(Gesture.SWIPE_DOWN, R.string.swipe_down_label)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 56.dp)) {
        for ((action, label) in listOf(
            SwipeDownAction.NOTIFICATIONS to R.string.chip_notifications,
            SwipeDownAction.SEARCH to R.string.chip_search,
            SwipeDownAction.NONE to R.string.chip_nothing,
        )) {
            FilterChip(
                selected = settings.swipeDown == action,
                onClick = { prefs.update { it.copy(swipeDown = action) } },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun NavigationPage(settings: LauncherSettings, prefs: Prefs, status: SetupStatus) {
    val context = LocalContext.current
    Toggle(R.string.nav_gestures, settings.navGestures) { v ->
        prefs.update { it.copy(navGestures = v) }
        if (v && !status.accessibility) SystemActions.openAccessibilitySettings(context)
    }
    if (settings.navGestures && !status.accessibility) {
        Fix(R.string.needs_accessibility) { SystemActions.openAccessibilitySettings(context) }
    }
    if (settings.navGestures) {
        Toggle(R.string.nav_line, settings.navLine) { v -> prefs.update { it.copy(navLine = v) } }
    }
    Spacer(Modifier.height(8.dp))
    Explain(Gesture.SWIPE_UP, R.string.g_home)
    Explain(Gesture.SWIPE_UP_HOLD, R.string.g_recents)
    Explain(Gesture.SIDE_BOTTOM, R.string.g_previous)
    Explain(Gesture.BACK_EDGE, R.string.g_back)
    Spacer(Modifier.height(8.dp))
    Sub(stringResource(R.string.nav_hint))
}

/* ---------- Favoritos ---------- */

@Composable
private fun FavoritesPage(favorites: List<AppEntry>, allApps: List<AppEntry>, prefs: Prefs) {
    var picking by remember { mutableStateOf(false) }
    val rowHeight = 56.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val palette = LocalPalette.current

    if (favorites.isEmpty()) Sub(stringResource(R.string.no_favorites))
    else Sub(stringResource(R.string.favorites_drag_hint))
    Spacer(Modifier.height(8.dp))

    val keys = favorites.map { it.key }
    favorites.forEachIndexed { index, app ->
        val dragging = dragKey == app.key
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .zIndex(if (dragging) 1f else 0f)
                .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                .background(if (dragging) palette.fg.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                .pointerInput(keys) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragKey = app.key; dragOffset = 0f },
                        onDragEnd = {
                            val target = (index + (dragOffset / rowPx).roundToInt()).coerceIn(0, keys.lastIndex)
                            if (target != index) {
                                prefs.setFavorites(keys.toMutableList().apply { removeAt(index); add(target, app.key) })
                            }
                            dragKey = null; dragOffset = 0f
                        },
                        onDragCancel = { dragKey = null; dragOffset = 0f },
                        onDrag = { change, amount -> change.consume(); dragOffset += amount.y },
                    )
                }
                .padding(horizontal = 4.dp),
        ) {
            Icon(Icons.Default.Menu, null, tint = palette.fg.copy(alpha = 0.4f))
            Spacer(Modifier.width(12.dp))
            app.icon?.let {
                Image(it, null, colorFilter = palette.iconFilter, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(app.label, color = palette.fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { prefs.toggleFavorite(app.key) }) {
                Icon(Icons.Default.Close, stringResource(R.string.remove), tint = palette.fg.copy(alpha = 0.6f))
            }
        }
    }
    if (favorites.size < MAX_FAVORITES) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { picking = true }) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_favorite))
        }
    }

    if (picking) {
        val candidates = allApps.filter { it.key !in keys }
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.add_favorite)) },
            text = {
                Column(Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                    for (app in candidates) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { prefs.toggleFavorite(app.key); picking = false }
                                .padding(vertical = 10.dp),
                        ) {
                            app.icon?.let {
                                Image(it, null, colorFilter = palette.iconFilter, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                            }
                            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
        )
    }
}

/* ---------- Ocultos ---------- */

@Composable
private fun HiddenPage(hiddenApps: List<AppEntry>, prefs: Prefs) {
    val palette = LocalPalette.current
    Sub(stringResource(if (hiddenApps.isEmpty()) R.string.no_hidden else R.string.hidden_hint))
    Spacer(Modifier.height(8.dp))
    for (app in hiddenApps) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            app.icon?.let {
                Image(it, null, colorFilter = palette.iconFilter, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(app.label, color = palette.fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = { prefs.setHidden(app.key, false) }) { Text(stringResource(R.string.unhide)) }
        }
    }
}

/* ---------- Sistema ---------- */

@Composable
private fun SystemPage(status: SetupStatus, onSetDefault: () -> Unit, openSetup: () -> Unit) {
    val context = LocalContext.current
    StatusRow(R.string.set_default, status.isDefault, onSetDefault)
    StatusRow(R.string.notification_access, status.notifications) { SystemActions.openNotificationAccessSettings(context) }
    StatusRow(R.string.accessibility_service, status.accessibility) { SystemActions.openAccessibilitySettings(context) }
    Spacer(Modifier.height(8.dp))
    Card(onClick = openSetup) {
        Icon(Icons.Default.Build, null, tint = LocalPalette.current.fg.copy(alpha = 0.8f))
        Spacer(Modifier.width(16.dp))
        Title(stringResource(R.string.setup_open), Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Card(onClick = {
            context.startSafely(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }) {
            Title(stringResource(R.string.language), Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = LocalPalette.current.fg.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun StatusRow(title: Int, ok: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Icon(
            if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, null,
            tint = if (ok) LocalPalette.current.fg.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Title(stringResource(title))
            Sub(stringResource(if (ok) R.string.enabled else R.string.disabled))
        }
    }
    Spacer(Modifier.height(8.dp))
}

/* ---------- Assistente ---------- */

@Composable
private fun SetupPage(status: SetupStatus, onSetDefault: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
        Text(stringResource(R.string.setup_title), fontSize = 30.sp, color = LocalPalette.current.fg)
        Spacer(Modifier.height(4.dp))
        Sub(stringResource(R.string.setup_subtitle))
        Spacer(Modifier.height(20.dp))
        Step(1, R.string.step_default, R.string.step_default_desc, status.isDefault, onSetDefault)
        Step(2, R.string.step_restricted, R.string.step_restricted_desc, null) {
            context.startSafely(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        }
        Step(3, R.string.step_notifications, R.string.step_notifications_desc, status.notifications) {
            SystemActions.openNotificationAccessSettings(context)
        }
        Step(4, R.string.step_accessibility, R.string.step_accessibility_desc, status.accessibility) {
            SystemActions.openAccessibilitySettings(context)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (status.complete) R.string.setup_done else R.string.setup_later))
        }
    }
}

@Composable
private fun Step(n: Int, title: Int, desc: Int, done: Boolean?, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Card(onClick = onClick) {
        Box(
            Modifier.size(32.dp).background(
                if (done == true) MaterialTheme.colorScheme.primary else palette.fg.copy(alpha = 0.12f), CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (done == true) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimary)
            else Text(n.toString(), color = palette.fg)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Title(stringResource(title))
            Sub(stringResource(desc))
        }
        if (done != true) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.fg.copy(alpha = 0.4f))
    }
    Spacer(Modifier.height(10.dp))
}

/* ---------- Peças comuns ---------- */

@Composable
private fun Page(title: Int, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = LocalPalette.current.fg)
            }
            Text(stringResource(title), fontSize = 24.sp, color = LocalPalette.current.fg)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            content()
        }
    }
}

@Composable
private fun Card(onClick: () -> Unit, highlight: Boolean = false, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else LocalPalette.current.fg.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun Title(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalPalette.current.fg, fontSize = 16.sp, modifier = modifier)
}

@Composable
private fun Sub(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalPalette.current.fg.copy(alpha = 0.6f), fontSize = 13.sp, modifier = modifier)
}

@Composable
private fun Label(text: Int) {
    Text(
        stringResource(text), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
    )
}

@Composable
private fun Toggle(title: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp),
    ) {
        Title(stringResource(title), Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

/** Aviso com botão para resolver algo que falta. */
@Composable
private fun Fix(text: Int, onClick: () -> Unit) {
    Card(onClick = onClick, highlight = true) {
        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Sub(stringResource(text), Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
}

/** Linha com mini animação do gesto + frase curta. */
@Composable
private fun Explain(gesture: Gesture, text: Int, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        GestureDemo(gesture)
        Spacer(Modifier.width(16.dp))
        Title(stringResource(text), Modifier.weight(1f))
        trailing?.invoke()
    }
}
