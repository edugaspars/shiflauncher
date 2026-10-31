package io.github.edugaspars.shiflauncher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Carrega todos os apps com ícone na tela (inclusive do perfil de trabalho / Segundo Espaço). */
class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadJob: Job? = null

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val iconSizePx = (40 * appContext.resources.displayMetrics.density).toInt()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(p: Array<out String>, user: UserHandle, r: Boolean) = reload()
        override fun onPackagesUnavailable(p: Array<out String>, user: UserHandle, r: Boolean) = reload()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val ownPackage = appContext.packageName
            val result = mutableListOf<AppEntry>()
            for (user in userManager.userProfiles) {
                val serial = userManager.getSerialNumberForUser(user)
                for (info in launcherApps.getActivityList(null, user)) {
                    val component = info.componentName
                    if (component.packageName == ownPackage) continue
                    val icon = runCatching {
                        info.getBadgedIcon(0).toBitmap(iconSizePx, iconSizePx).asImageBitmap()
                    }.getOrNull()
                    result += AppEntry(
                        key = "${component.flattenToString()}#$serial",
                        packageName = component.packageName,
                        component = component,
                        user = user,
                        originalLabel = info.label?.toString()?.trim().orEmpty()
                            .ifEmpty { component.packageName },
                        icon = icon,
                    )
                }
            }
            _apps.value = result
        }
    }
}
