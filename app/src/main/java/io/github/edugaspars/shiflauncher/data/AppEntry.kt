package io.github.edugaspars.shiflauncher.data

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import java.text.Normalizer

data class AppEntry(
    val key: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val originalLabel: String,
    val icon: ImageBitmap?,
    val label: String = originalLabel,
)

private val accents = "\\p{Mn}+".toRegex()

/** Remove acentos e deixa minúsculo, para busca e ordenação ("Câmera" -> "camera"). */
fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(accents, "").lowercase()

/** Letra usada no índice lateral: A-Z, ou "#" para números/símbolos. */
fun String.indexLetter(): Char {
    val c = normalized().firstOrNull { !it.isWhitespace() }?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}
