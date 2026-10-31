package io.github.edugaspars.shiflauncher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale

object CalendarHelper {

    fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Próximo evento nas próximas 36h, já formatado: "14:00 · Reunião". */
    fun nextEvent(context: Context): String? {
        if (!hasPermission(context)) return null
        val now = System.currentTimeMillis()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, now + 36L * 60 * 60 * 1000)
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        return runCatching {
            context.contentResolver.query(
                builder.build(), projection,
                "${CalendarContract.Instances.VISIBLE} = 1", null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val begin = c.getLong(1)
                    val end = c.getLong(2)
                    val allDay = c.getInt(3) == 1
                    if (end < now) continue
                    if (allDay) continue
                    return@use "${formatTime(context, begin)} · $title"
                }
                null
            }
        }.getOrNull()
    }

    private fun formatTime(context: Context, millis: Long): String {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        val sameDay = target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            target.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        val time = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        val pattern = if (sameDay) time else DateFormat.getBestDateTimePattern(locale, "EEE") + " " + time
        return DateFormat.format(pattern, target).toString()
    }
}
