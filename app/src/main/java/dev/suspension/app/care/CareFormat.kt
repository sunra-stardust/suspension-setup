package dev.suspension.app.care

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The Pflege tab always writes numbers as `1.240` and dates as `dd.MM.yyyy`, whatever the device locale. */
object CareFormat {
    private val locale: Locale = Locale.GERMANY
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale)

    fun km(value: Int): String = NumberFormat.getIntegerInstance(locale).format(value)

    fun date(value: LocalDate): String = value.format(dateFormat)
}

/** Why a service entry cannot be saved yet. */
data class EntryErrors(val dateInFuture: Boolean, val kmMissing: Boolean, val nothingDone: Boolean) {
    val any get() = dateInFuture || kmMissing || nothingDone
}

object EntryValidation {
    const val MAX_NOTE_LENGTH = 500
    const val MAX_KM_DIGITS = 7

    fun parseKm(text: String): Int? = text.trim().takeIf { it.isNotEmpty() && it.length <= MAX_KM_DIGITS }?.toIntOrNull()?.takeIf { it >= 0 }

    /** At least one task or non-empty text; km a non-negative integer; no future date. */
    fun validate(date: LocalDate, kmText: String, taskIds: Collection<String>, note: String, today: LocalDate) = EntryErrors(
        dateInFuture = date.isAfter(today),
        kmMissing = parseKm(kmText) == null,
        nothingDone = taskIds.isEmpty() && note.isBlank(),
    )
}
