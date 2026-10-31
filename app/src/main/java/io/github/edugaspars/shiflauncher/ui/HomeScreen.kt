package io.github.edugaspars.shiflauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.edugaspars.shiflauncher.R
import io.github.edugaspars.shiflauncher.data.AppEntry
import io.github.edugaspars.shiflauncher.data.CalendarHelper
import io.github.edugaspars.shiflauncher.data.LauncherSettings
import io.github.edugaspars.shiflauncher.gestures.startSafely
import io.github.edugaspars.shiflauncher.notifications.AppNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun HomeScreen(
    favorites: List<AppEntry>,
    notificationsFor: (AppEntry) -> AppNotifications?,
    settings: LauncherSettings,
    resumeTick: Int,
    actions: AppActions,
    onRename: (AppEntry) -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val shadow = !settings.blackBackground
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    val swipeUp by rememberUpdatedState(onSwipeUp)
    val swipeDown by rememberUpdatedState(onSwipeDown)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val longPress by rememberUpdatedState(onLongPress)
    var menuFor by remember { mutableStateOf<String?>(null) }

    // Relógio: atualiza no início de cada minuto.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(resumeTick) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    var event by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(resumeTick, settings.showCalendar, now / (5 * 60_000)) {
        event = if (settings.showCalendar) {
            withContext(Dispatchers.IO) { CalendarHelper.nextEvent(context) }
        } else null
    }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { doubleTap() },
                    onLongPress = { longPress() },
                )
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -threshold -> swipeUp()
                            total > threshold -> swipeDown()
                        }
                    },
                    onVerticalDrag = { change, amount -> change.consume(); total += amount },
                )
            }
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        val datePattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM") }
        val noRipple = remember { MutableInteractionSource() }
        Text(
            text = DateFormat.format(timePattern, now).toString(),
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Thin,
            style = MaterialTheme.typography.displayLarge.onWallpaper(shadow),
            modifier = Modifier.clickable(noRipple, null) {
                context.startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS))
            },
        )
        Text(
            text = DateFormat.format(datePattern, now).toString().replaceFirstChar { it.titlecase(locale) },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyLarge.onWallpaper(shadow),
            modifier = Modifier.clickable(noRipple, null) {
                context.startSafely(
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
                )
            },
        )
        event?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium.onWallpaper(shadow),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_favorites),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                style = MaterialTheme.typography.bodyMedium.onWallpaper(shadow),
            )
        }
        for (app in favorites) {
            AppRow(
                app = app,
                notifications = if (settings.showNotifications) notificationsFor(app) else null,
                showIcon = settings.showIcons,
                large = true,
                shadow = shadow,
                menuOpen = menuFor == app.key,
                onClick = { actions.launch(app) },
                onLongClick = { menuFor = app.key },
                menu = {
                    AppMenu(
                        app = app, isFavorite = true, isHidden = false, actions = actions,
                        onRename = { onRename(app) }, onDismiss = { menuFor = null },
                    )
                },
            )
        }
        Spacer(Modifier.height(56.dp))
    }
}
