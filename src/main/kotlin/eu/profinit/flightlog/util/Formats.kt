package eu.profinit.flightlog.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Centralised date/time/decimal formats per NFR-14. */
object Formats {
    val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale("cs", "CZ"))
    val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("cs", "CZ"))

    fun parseDate(value: String): LocalDate = try {
        LocalDate.parse(value, DATE)
    } catch (ex: java.time.format.DateTimeParseException) {
        throw IllegalArgumentException("Datum '$value' není ve formátu DD-MM-YYYY.")
    }

    fun parseTime(value: String): LocalTime = try {
        LocalTime.parse(value, TIME)
    } catch (ex: java.time.format.DateTimeParseException) {
        throw IllegalArgumentException("Čas '$value' není ve formátu HH:mm (00:00–23:59).")
    }

    fun formatDate(d: OffsetDateTime): String = DATE.format(d)
    fun formatTime(d: OffsetDateTime): String = TIME.format(d)

    fun combine(date: LocalDate, time: LocalTime): OffsetDateTime =
        OffsetDateTime.of(date, time, ZoneOffset.UTC)

    fun formatDuration(hours: Double): String =
        String.format(Locale.US, "%.2f", hours)
}
