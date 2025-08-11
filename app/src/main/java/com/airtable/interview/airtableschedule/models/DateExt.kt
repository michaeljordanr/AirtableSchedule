package com.airtable.interview.airtableschedule.models

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

fun Date.toDateFormatted(): String {
    val localDate = this.toLocalDate()
    val monthName = localDate.month.name.lowercase().replaceFirstChar { it.uppercase() } // e.g. February

    val day = localDate.dayOfMonth
    val suffix = when {
        day in 11..13 -> "th"  // special case: 11th, 12th, 13th
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }

    return "$monthName $day$suffix"
}

fun Date.toDateFormattedShort(): String {
    val localDate = this.toLocalDate()
    val day = localDate.dayOfMonth.toString().padStart(2, '0')
    val month = localDate.monthValue.toString().padStart(2, '0')
    return "$day/$month"
}

fun Date.toLocalDate(): LocalDate =
    this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toDate(): Date =
    Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
