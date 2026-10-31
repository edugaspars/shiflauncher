package io.github.edugaspars.shiflauncher.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import io.github.edugaspars.shiflauncher.data.ThemeMode

/** Cores do ShifLauncher para o tema atual. */
data class ShifPalette(
    val mode: ThemeMode,
    val background: Color,   // fundo da tela inicial (transparente = papel de parede)
    val surface: Color,      // fundo da gaveta e das configurações
    val fg: Color,           // texto
    val wallpaper: Boolean,
    val light: Boolean,
    val mono: Boolean,       // ícones em escala de cinza
    val serif: Boolean,
) {
    val iconFilter: ColorFilter? =
        if (mono) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
}

val LocalPalette = staticCompositionLocalOf { paletteFor(ThemeMode.WALLPAPER) }

fun paletteFor(mode: ThemeMode): ShifPalette = when (mode) {
    ThemeMode.WALLPAPER -> ShifPalette(mode, Color.Transparent, Color.Black.copy(alpha = 0.82f), Color.White, true, false, false, false)
    ThemeMode.BLACK, ThemeMode.SYSTEM -> ShifPalette(mode, Color.Black, Color.Black, Color.White, false, false, false, false)
    ThemeMode.WHITE -> ShifPalette(mode, Color.White, Color.White, Color(0xFF111111), false, true, false, false)
    ThemeMode.MONO_BLACK -> ShifPalette(mode, Color.Black, Color.Black, Color.White, false, false, true, false)
    ThemeMode.KINDLE -> ShifPalette(mode, Color(0xFFF7F5F0), Color(0xFFF7F5F0), Color(0xFF111111), false, true, true, true)
}

/** Resolve "seguir o sistema" para preto ou branco. */
@Composable
fun resolvedPalette(mode: ThemeMode): ShifPalette =
    if (mode == ThemeMode.SYSTEM) paletteFor(if (isSystemInDarkTheme()) ThemeMode.BLACK else ThemeMode.WHITE)
    else paletteFor(mode)

@Composable
fun ShifTheme(palette: ShifPalette, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        palette.mono && palette.light -> lightColorScheme(
            primary = Color.Black, onPrimary = Color.White,
            primaryContainer = Color(0xFFDDDDDD), onPrimaryContainer = Color.Black,
            secondaryContainer = Color(0xFFE6E6E6), surface = palette.surface, background = palette.surface,
            surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFF0F0F0),
        )
        palette.mono -> darkColorScheme(
            primary = Color.White, onPrimary = Color.Black,
            primaryContainer = Color(0xFF333333), onPrimaryContainer = Color.White,
            secondaryContainer = Color(0xFF2A2A2A), surface = Color.Black, background = Color.Black,
            surfaceContainer = Color(0xFF161616), surfaceContainerHigh = Color(0xFF1F1F1F),
        )
        palette.light -> if (dynamic) dynamicLightColorScheme(context) else lightColorScheme()
        else -> if (dynamic) dynamicDarkColorScheme(context) else darkColorScheme()
    }
    val typography = if (palette.serif) Typography().withFamily(FontFamily.Serif) else Typography()
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

private fun Typography.withFamily(f: FontFamily) = Typography(
    displayLarge.copy(fontFamily = f), displayMedium.copy(fontFamily = f), displaySmall.copy(fontFamily = f),
    headlineLarge.copy(fontFamily = f), headlineMedium.copy(fontFamily = f), headlineSmall.copy(fontFamily = f),
    titleLarge.copy(fontFamily = f), titleMedium.copy(fontFamily = f), titleSmall.copy(fontFamily = f),
    bodyLarge.copy(fontFamily = f), bodyMedium.copy(fontFamily = f), bodySmall.copy(fontFamily = f),
    labelLarge.copy(fontFamily = f), labelMedium.copy(fontFamily = f), labelSmall.copy(fontFamily = f),
)

/** Sombra leve para o texto continuar legível sobre papéis de parede claros. */
fun TextStyle.onWallpaper(enabled: Boolean): TextStyle =
    if (!enabled) this else copy(shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 8f))
