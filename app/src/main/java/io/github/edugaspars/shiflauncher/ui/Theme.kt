package io.github.edugaspars.shiflauncher.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

@Composable
fun ShifTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Sombra leve para o texto continuar legível sobre papéis de parede claros. */
fun TextStyle.onWallpaper(enabled: Boolean): TextStyle =
    if (!enabled) this else copy(shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 8f))
