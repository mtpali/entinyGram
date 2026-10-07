package desu.inugram.helpers

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import org.telegram.messenger.time.FastDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import java.text.FieldPosition

object CalendarHelper {
    private val cache = HashMap<String, FastDateFormat>()

    @JvmStatic
    fun isEnabled(): Boolean = true

    @JvmStatic
    fun formatter(pattern: String, locale: Locale): FastDateFormat =
        if (pattern.replace(Regex("'[^']*'"), "").any { it == 'd' || it == 'D' }) altFormatter(pattern, locale)
        else FastDateFormat.getInstance(pattern, locale)

    @JvmStatic
    @Synchronized
    fun altFormatter(pattern: String, locale: Locale): FastDateFormat {
        val ca = "persian"
        val key = "$ca|$pattern|$locale|${TimeZone.getDefault().id}"
        return cache.getOrPut(key) {
            val altLocale = Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("ca", ca).build()
            val icuFormat = SimpleDateFormat(pattern, ULocale.forLocale(altLocale))
            object : FastDateFormat(pattern, TimeZone.getDefault(), locale) {
                @Synchronized
                override fun format(date: Date): String = icuFormat.format(date)
                @Synchronized
                override fun format(millis: Long): String = icuFormat.format(Date(millis))
                override fun format(calendar: Calendar): String = format(calendar.timeInMillis)
                override fun format(date: Date, buffer: StringBuffer): StringBuffer = buffer.append(format(date))
                override fun format(millis: Long, buffer: StringBuffer): StringBuffer = buffer.append(format(millis))
                override fun format(calendar: Calendar, buffer: StringBuffer): StringBuffer = buffer.append(format(calendar))
                override fun format(value: Any, buffer: StringBuffer, position: FieldPosition): StringBuffer =
                    when (value) {
                        is Date -> format(value, buffer)
                        is Calendar -> format(value, buffer)
                        is Long -> format(value, buffer)
                        else -> throw IllegalArgumentException("Unsupported date")
                    }
            }
        }
    }

    @JvmStatic
    fun altCalendar(locale: Locale): android.icu.util.Calendar {
        val ca = "persian"
        val altLocale = Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("ca", ca).build()
        return android.icu.util.Calendar.getInstance(ULocale.forLocale(altLocale))
    }

    @JvmStatic
    fun calendar(): android.icu.util.Calendar = altCalendar(Locale.getDefault())

    @JvmStatic
    fun formatYearMonthDay(dateMillis: Long, alwaysShowYear: Boolean, locale: Locale): String {
        val cal = altCalendar(locale)
        val nowYear = cal.get(android.icu.util.Calendar.YEAR)
        cal.timeInMillis = dateMillis
        val year = cal.get(android.icu.util.Calendar.YEAR)
        val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val monthName = altFormatter("MMM", locale).format(Date(dateMillis))
        return if (year == nowYear && !alwaysShowYear) "$monthName $day" else "$monthName $day, $year"
    }

    @JvmStatic
    fun formatYearMonth(dateMillis: Long, alwaysShowYear: Boolean, locale: Locale): String {
        val cal = altCalendar(locale)
        val nowYear = cal.get(android.icu.util.Calendar.YEAR)
        cal.timeInMillis = dateMillis
        val year = cal.get(android.icu.util.Calendar.YEAR)
        val monthName = altFormatter("MMMM", locale).format(Date(dateMillis))
        return if (year == nowYear && !alwaysShowYear) monthName else "$monthName $year"
    }
}
