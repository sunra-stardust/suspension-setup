package dev.suspension.app.care

import java.time.LocalDate

/** One completed service visit; may cover several tasks. Unknown task ids (catalog changes) are kept. */
data class LogEntry(
    val id: String,
    val date: LocalDate,
    val km: Int,
    val taskIds: List<String>,
    val note: String,
    val createdAt: Long,
)

/** Last notification sent for a task: its state then and the day. */
data class Notified(val state: DueState, val date: LocalDate)

/** Everything the calendar stores, as the screen sees it. */
data class CareData(
    val purchaseDate: LocalDate? = null,
    val odometerKm: Int = 0,
    val odometerUpdatedOn: LocalDate? = null,
    val remindersEnabled: Boolean = false,
    val kmPerHour: Int = DEFAULT_KM_PER_HOUR,
    val reminderOverrides: Map<String, Boolean> = emptyMap(),
    val notified: Map<String, Notified> = emptyMap(),
    val log: List<LogEntry> = emptyList(),
) {
    fun remindFor(task: MaintTask): Boolean = reminderOverrides[task.id] ?: task.remindByDefault

    fun dueInput(today: LocalDate) = DueInput(purchaseDate, odometerKm, kmPerHour, today, log)

    companion object {
        const val DEFAULT_KM_PER_HOUR = 15
        const val MIN_KM_PER_HOUR = 8
        const val MAX_KM_PER_HOUR = 25
        const val STALE_ODOMETER_DAYS = 30L
    }
}
