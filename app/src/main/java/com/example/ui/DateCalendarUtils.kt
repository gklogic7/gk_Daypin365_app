package com.example.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MonthKey(val year: Int, val monthZeroBased: Int) {
    fun label(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun shortLabel(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    }
}

object DateCalendarUtils {

    fun todayStartMillis(): Long = dayStartMillis(System.currentTimeMillis())

    fun horizon365EndMillis(): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = todayStartMillis()
            add(Calendar.DAY_OF_YEAR, 365)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return cal.timeInMillis
    }

    fun dayStartMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun dayStartMillis(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun combineDateAndTime(dayStartMillis: Long, hourOfDay: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun shiftMonth(monthKey: MonthKey, deltaMonths: Int): MonthKey {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, monthKey.year)
            set(Calendar.MONTH, monthKey.monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return MonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun shiftYear(monthKey: MonthKey, deltaYears: Int): MonthKey {
        val nextYear = (monthKey.year + deltaYears).coerceIn(1, 9999)
        return MonthKey(nextYear, monthKey.monthZeroBased)
    }

    fun monthNameShort(monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    }

    fun next365DaysMonths(): List<MonthKey> {
        val startCal = Calendar.getInstance().apply { timeInMillis = todayStartMillis() }
        val endCal = Calendar.getInstance().apply { timeInMillis = horizon365EndMillis() }
        val result = mutableListOf<MonthKey>()
        val cursor = Calendar.getInstance().apply {
            set(Calendar.YEAR, startCal.get(Calendar.YEAR))
            set(Calendar.MONTH, startCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        while (cursor.get(Calendar.YEAR) < endCal.get(Calendar.YEAR) ||
            (cursor.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                cursor.get(Calendar.MONTH) <= endCal.get(Calendar.MONTH))
        ) {
            result.add(MonthKey(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH)))
            cursor.add(Calendar.MONTH, 1)
        }
        return result
    }

    fun daysInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun firstDayOfWeekOffset(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
    }

    fun dayNumberIn365Horizon(dayStartMillis: Long): Int? {
        val today = todayStartMillis()
        val diffDays = TimeUnit.MILLISECONDS.toDays(dayStartMillis - today).toInt()
        return if (diffDays in 0..365) diffDays + 1 else null
    }

    fun relativeDayLabel(dayStartMillis: Long): String {
        val today = todayStartMillis()
        val diffDays = TimeUnit.MILLISECONDS.toDays(dayStartMillis - today)
        return when {
            diffDays == 0L -> "TODAY"
            diffDays == 1L -> "TOMORROW"
            diffDays == -1L -> "YESTERDAY"
            diffDays in 2L..365L -> "DAY ${diffDays + 1} OF 365 (+${diffDays}D)"
            diffDays > 365L -> "+$diffDays DAYS IN FUTURE"
            else -> "${kotlin.math.abs(diffDays)} DAYS AGO"
        }
    }

    fun formatDateFull(millis: Long): String =
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(millis))

    fun formatDateShort(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

    fun formatTime(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    fun formatCountdown(targetMillis: Long): String {
        val diff = targetMillis - System.currentTimeMillis()
        if (diff <= 0L) return "Past / Due"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val mins = totalMinutes % 60
        return when {
            days > 0 -> "in ${days}d ${hours}h"
            hours > 0 -> "in ${hours}h ${mins}m"
            else -> "in ${mins.coerceAtLeast(1)}m"
        }
    }
}
