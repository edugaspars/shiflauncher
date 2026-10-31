package io.github.edugaspars.shiflauncher

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import io.github.edugaspars.shiflauncher.ui.LocalPalette
import io.github.edugaspars.shiflauncher.ui.ShifPalette
import io.github.edugaspars.shiflauncher.ui.resolvedPalette
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.edugaspars.shiflauncher.data.AppEntry
import io.github.edugaspars.shiflauncher.data.CalendarHelper
import io.github.edugaspars.shiflauncher.data.SwipeDownAction
import io.github.edugaspars.shiflauncher.data.normalized
import io.github.edugaspars.shiflauncher.gestures.SystemActions
import io.github.edugaspars.shiflauncher.gestures.startSafely
import io.github.edugaspars.shiflauncher.notifications.NotificationStore
import io.github.edugaspars.shiflauncher.ui.AppActions
import io.github.edugaspars.shiflauncher.ui.AppDrawer
import io.github.edugaspars.shiflauncher.ui.HomeScreen
import io.github.edugaspars.shiflauncher.ui.SettingsScreen
import io.github.edugaspars.shiflauncher.ui.ShifTheme

enum class Screen { HOME, DRAWER, SETTINGS }

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(Screen.HOME)
    private var searchFocus by mutableStateOf(false)
    private var resumeTick by mutableIntStateOf(0)

    private val calendarPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }
    private val homeRole =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            resumeTick++
            // No HyperOS o pedido de "papel" às vezes é ignorado: abre a tela de apps padrão.
            if (!isDefaultLauncher()) openDefaultAppsSettings()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as ShifApp
        // Launcher não "fecha" com voltar: na tela inicial o gesto não faz nada.
        // (Gaveta e configurações registram o próprio BackHandler, que tem prioridade.)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                screen = Screen.HOME
            }
        })
        setContent {
            val settings by app.prefs.settings.collectAsStateWithLifecycle()
            val palette = resolvedPalette(settings.theme)
            SideEffect { applyWindowTheme(palette) }
            ShifTheme(palette) {
                LauncherRoot(app)
            }
        }
    }

    /**
     * Fora do modo papel de parede a janela fica opaca com a cor do tema, assim a
     * miniatura nos recentes e as transições mostram o fundo do ShifLauncher.
     */
    private fun applyWindowTheme(palette: ShifPalette) {
        if (palette.wallpaper) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setBackgroundDrawable(ColorDrawable(palette.background.toArgb()))
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = palette.light
            isAppearanceLightNavigationBars = palette.light
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Botão/gesto "início" com o launcher já aberto: volta para a tela inicial.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) screen = Screen.HOME
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onStop() {
        super.onStop()
        if (screen == Screen.DRAWER) screen = Screen.HOME
    }

    private fun openDrawer(withSearch: Boolean) {
        searchFocus = withSearch
        screen = Screen.DRAWER
    }

    private fun requestCalendar() {
        if (!CalendarHelper.hasPermission(this)) calendarPermission.launch(Manifest.permission.READ_CALENDAR)
    }

    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { homeRole.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                    .onSuccess { return }
            }
        }
        openDefaultAppsSettings()
    }

    private fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(home, 0)?.activityInfo?.packageName == packageName
    }

    private fun openDefaultAppsSettings() {
        startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) ||
            startSafely(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) ||
            startSafely(Intent(Settings.ACTION_SETTINGS))
    }

    @Composable
    private fun LauncherRoot(app: ShifApp) {
        val context = LocalContext.current
        val raw by app.repository.apps.collectAsStateWithLifecycle()
        val settings by app.prefs.settings.collectAsStateWithLifecycle()
        val notifications by NotificationStore.notifications.collectAsStateWithLifecycle()
        val actions = remember { AppActions(context, app.prefs) }
        var renaming by remember { mutableStateOf<AppEntry?>(null) }

        val all = remember(raw, settings.renames) {
            raw.map { it.copy(label = settings.renames[it.key] ?: it.originalLabel) }
                .sortedBy { it.label.normalized() }
        }
        val visible = remember(all, settings.hidden) { all.filter { it.key !in settings.hidden } }
        val hidden = remember(all, settings.hidden) { all.filter { it.key in settings.hidden } }
        val favorites = remember(all, settings.favorites) {
            val byKey = all.associateBy { it.key }
            settings.favorites.mapNotNull { byKey[it] }
        }
        val notificationsFor = { entry: AppEntry ->
            notifications[NotificationStore.key(entry.packageName, entry.user.hashCode())]
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(LocalPalette.current.background)
        ) {
            HomeScreen(
                favorites = favorites,
                notificationsFor = notificationsFor,
                settings = settings,
                resumeTick = resumeTick,
                actions = actions,
                onRename = { renaming = it },
                onSwipeUp = { openDrawer(withSearch = false) },
                onSwipeDown = {
                    when (settings.swipeDown) {
                        SwipeDownAction.NOTIFICATIONS -> SystemActions.expandNotifications(context)
                        SwipeDownAction.SEARCH -> openDrawer(withSearch = true)
                        SwipeDownAction.NONE -> Unit
                    }
                },
                onDoubleTap = { if (settings.doubleTapLock) SystemActions.lockScreen() },
                onLongPress = { screen = Screen.SETTINGS },
            )
            AnimatedVisibility(
                visible = screen == Screen.DRAWER,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut(),
            ) {
                AppDrawer(
                    apps = visible,
                    hiddenApps = hidden,
                    notificationsFor = notificationsFor,
                    settings = settings,
                    focusSearch = searchFocus,
                    actions = actions,
                    onRename = { renaming = it },
                    onClose = { screen = Screen.HOME },
                )
            }
            AnimatedVisibility(visible = screen == Screen.SETTINGS, enter = fadeIn(), exit = fadeOut()) {
                SettingsScreen(
                    settings = settings,
                    prefs = app.prefs,
                    favorites = favorites,
                    hiddenApps = hidden,
                    resumeTick = resumeTick,
                    onRequestCalendar = ::requestCalendar,
                    onSetDefault = ::requestDefaultLauncher,
                    onClose = { screen = Screen.HOME },
                )
            }
        }

        renaming?.let { target ->
            var name by remember(target.key) { mutableStateOf(target.label) }
            AlertDialog(
                onDismissRequest = { renaming = null },
                title = { Text(stringResource(R.string.rename)) },
                text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = { actions.rename(target, name); renaming = null }) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { actions.rename(target, null); renaming = null }) {
                        Text(stringResource(R.string.reset_name))
                    }
                },
            )
        }
    }
}
