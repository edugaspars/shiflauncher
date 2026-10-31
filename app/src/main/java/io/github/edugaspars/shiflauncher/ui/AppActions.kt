package io.github.edugaspars.shiflauncher.ui

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.widget.Toast
import io.github.edugaspars.shiflauncher.R
import io.github.edugaspars.shiflauncher.data.AppEntry
import io.github.edugaspars.shiflauncher.data.MAX_FAVORITES
import io.github.edugaspars.shiflauncher.data.Prefs
import io.github.edugaspars.shiflauncher.gestures.startSafely

class AppActions(private val context: Context, private val prefs: Prefs) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun launch(app: AppEntry) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, null, null) }
    }

    fun toggleFavorite(app: AppEntry) {
        if (!prefs.toggleFavorite(app.key)) {
            Toast.makeText(
                context, context.getString(R.string.favorites_full, MAX_FAVORITES), Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun setHidden(app: AppEntry, hidden: Boolean) = prefs.setHidden(app.key, hidden)

    fun rename(app: AppEntry, name: String?) = prefs.rename(app.key, name)

    fun openInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppEntry) {
        context.startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
    }
}
