package io.github.tonytonycoder11.agentictaskmanager.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Formats a due [Instant] in the device's local time zone for display, e.g. "28 Jun 2026, 14:00". */
internal object DueDateFormatter {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())

    fun format(instant: Instant): String = formatter.format(instant)
}
