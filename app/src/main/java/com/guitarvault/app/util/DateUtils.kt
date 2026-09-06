package com.guitarvault.app.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Material 3 DatePicker reports the selection as UTC-midnight millis; in
 * negative-offset timezones that formats as the previous day. Re-anchor the
 * picked calendar date to local noon so the displayed date always matches
 * what the user chose.
 */
fun utcMidnightToLocal(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val local = Calendar.getInstance().apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return local.timeInMillis
}
