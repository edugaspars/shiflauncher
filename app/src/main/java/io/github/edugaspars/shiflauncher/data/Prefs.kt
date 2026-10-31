package io.github.edugaspars.shiflauncher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class SwipeDownAction { NOTIFICATIONS, SEARCH, NONE }

data class LauncherSettings(
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val renames: Map<String, String> = emptyMap(),
    val blackBackground: Boolean = false,
    val showIcons: Boolean = true,
    val showNotifications: Boolean = true,
    val showCalendar: Boolean = false,
    val doubleTapLock: Boolean = true,
    val swipeDown: SwipeDownAction = SwipeDownAction.NOTIFICATIONS,
    val navGestures: Boolean = false,
    val navLine: Boolean = true,
)

const val MAX_FAVORITES = 8

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("shif", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<LauncherSettings> = _settings

    private fun load(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            favorites = sp.getString("favorites", null)?.let { s ->
                val a = JSONArray(s); List(a.length()) { a.getString(it) }
            } ?: d.favorites,
            hidden = sp.getStringSet("hidden", null)?.toSet() ?: d.hidden,
            renames = sp.getString("renames", null)?.let { s ->
                val o = JSONObject(s); o.keys().asSequence().associateWith { o.getString(it) }
            } ?: d.renames,
            blackBackground = sp.getBoolean("blackBackground", d.blackBackground),
            showIcons = sp.getBoolean("showIcons", d.showIcons),
            showNotifications = sp.getBoolean("showNotifications", d.showNotifications),
            showCalendar = sp.getBoolean("showCalendar", d.showCalendar),
            doubleTapLock = sp.getBoolean("doubleTapLock", d.doubleTapLock),
            navGestures = sp.getBoolean("navGestures", d.navGestures),
            navLine = sp.getBoolean("navLine", d.navLine),
            swipeDown = runCatching {
                SwipeDownAction.valueOf(sp.getString("swipeDown", null) ?: d.swipeDown.name)
            }.getOrDefault(d.swipeDown),
        )
    }

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val s = transform(_settings.value)
        _settings.value = s
        sp.edit()
            .putString("favorites", JSONArray(s.favorites).toString())
            .putStringSet("hidden", s.hidden)
            .putString("renames", JSONObject(s.renames).toString())
            .putBoolean("blackBackground", s.blackBackground)
            .putBoolean("showIcons", s.showIcons)
            .putBoolean("showNotifications", s.showNotifications)
            .putBoolean("showCalendar", s.showCalendar)
            .putBoolean("doubleTapLock", s.doubleTapLock)
            .putString("swipeDown", s.swipeDown.name)
            .putBoolean("navGestures", s.navGestures)
            .putBoolean("navLine", s.navLine)
            .apply()
    }

    /** Retorna false se a lista de favoritos já está cheia. */
    fun toggleFavorite(key: String): Boolean {
        val current = _settings.value.favorites
        if (key !in current && current.size >= MAX_FAVORITES) return false
        update { s -> s.copy(favorites = if (key in s.favorites) s.favorites - key else s.favorites + key) }
        return true
    }

    fun moveFavorite(key: String, delta: Int) = update { s ->
        val list = s.favorites.toMutableList()
        val i = list.indexOf(key)
        val j = i + delta
        if (i < 0 || j !in list.indices) return@update s
        list.removeAt(i); list.add(j, key)
        s.copy(favorites = list)
    }

    fun setHidden(key: String, hidden: Boolean) = update { s ->
        s.copy(
            hidden = if (hidden) s.hidden + key else s.hidden - key,
            favorites = if (hidden) s.favorites - key else s.favorites,
        )
    }

    fun rename(key: String, name: String?) = update { s ->
        s.copy(renames = if (name.isNullOrBlank()) s.renames - key else s.renames + (key to name.trim()))
    }
}
