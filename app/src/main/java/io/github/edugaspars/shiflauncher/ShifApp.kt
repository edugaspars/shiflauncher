package io.github.edugaspars.shiflauncher

import android.app.Application
import io.github.edugaspars.shiflauncher.data.AppRepository
import io.github.edugaspars.shiflauncher.data.Prefs

class ShifApp : Application() {
    val repository by lazy { AppRepository(this) }
    val prefs by lazy { Prefs(this) }
}
