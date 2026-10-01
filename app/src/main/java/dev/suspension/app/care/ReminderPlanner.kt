package dev.suspension.app.care

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Which tasks to announce now, and the notification memory to store afterwards. */
data class ReminderPlan(val notify: List<TaskDue>, val notified: Map<String, Notified>) {
    val hasOverdue get() = notify.any { it.state == DueState.OVERDUE }
    val hasSoon get() = notify.any { it.state == DueState.SOON }
}

/** Pure decision logic of the daily check (§5.7). */
object ReminderPlanner {
    const val REPEAT_OVERDUE_DAYS = 14L

    /**
     * Nothing is announced when reminders are off or the permission is missing (memory unchanged).
     * A task is announced when it is SOON/OVERDUE in a state different from the last notified one,
     * or OVERDUE and the last notification is 14 days old. Tasks back to OK/DONE (or muted) lose
     * their stored state, so they notify again when they come due next time.
     */
    fun plan(
        data: CareData,
        permissionGranted: Boolean,
        evaluated: List<TaskDue>,
        today: LocalDate,
    ): ReminderPlan {
        if (!data.remindersEnabled || !permissionGranted) return ReminderPlan(emptyList(), data.notified)

        val memory = data.notified.toMutableMap()
        val notify = mutableListOf<TaskDue>()
        for (due in evaluated) {
            val id = due.task.id
            val active = due.state == DueState.SOON || due.state == DueState.OVERDUE
            if (!data.remindFor(due.task) || !active) {
                memory.remove(id)
                continue
            }
            val previous = memory[id]
            val changed = previous == null || previous.state != due.state
            val repeat = due.state == DueState.OVERDUE && previous != null &&
                ChronoUnit.DAYS.between(previous.date, today) >= REPEAT_OVERDUE_DAYS
            if (changed || repeat) {
                notify += due
                memory[id] = Notified(due.state, today)
            }
        }
        val ordered = DueCalculator.dueList(notify)
        return ReminderPlan(ordered, memory)
    }

    /** Body: up to three names joined by " · "; [more] is the localized "und 2 weitere" for the rest. */
    fun bodyNames(names: List<String>, more: (Int) -> String): String {
        val shown = names.take(3).joinToString(" · ")
        val rest = names.size - 3
        return if (rest > 0) "$shown ${more(rest)}" else shown
    }
}
