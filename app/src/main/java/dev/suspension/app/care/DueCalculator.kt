package dev.suspension.app.care

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

enum class DueState { OVERDUE, SOON, OK, DONE, UNKNOWN }

/** Inputs of the due calculation: purchase date (may be missing), odometer, riding speed, today, the log. */
data class DueInput(
    val purchaseDate: LocalDate?,
    val odometerKm: Int,
    val kmPerHour: Int,
    val today: LocalDate,
    val log: List<LogEntry>,
)

/** Result for one task. [remainingKm]/[remainingDays] are zero or negative once that part is reached. */
data class TaskDue(
    val task: MaintTask,
    val state: DueState,
    val dueKm: Int?,
    val dueDate: LocalDate?,
    val remainingKm: Int?,
    val remainingDays: Int?,
    val lastDate: LocalDate?,
    val lastKm: Int?,
    /** Smallest remaining share of an interval over the parts; smaller = more urgent. */
    val urgency: Double,
)

enum class RemainingKind { KM, DAYS, MONTHS }

/** "in 230 km", "in 41 Tagen", "in 4 Monaten", or overdue "seit 80 km" / "seit 12 Tagen". */
data class RemainingPart(val kind: RemainingKind, val amount: Int, val overdue: Boolean)

object DueCalculator {
    private const val MAX_LEAD_KM = 100
    private const val MAX_LEAD_DAYS = 14
    private const val MONTHS_FROM_DAYS = 120
    private const val DAYS_PER_MONTH = 30

    /** [tasks]: the tasks that exist for the selected bike ([MaintCatalog.forBike]). */
    fun evaluateAll(input: DueInput, tasks: List<MaintTask> = MaintCatalog.tasks): List<TaskDue> = tasks.map { evaluate(it, input) }

    /** Overdue or soon, most urgent first (overdue before soon; then the smaller remaining share). */
    fun dueList(all: List<TaskDue>): List<TaskDue> =
        all.filter { it.state == DueState.OVERDUE || it.state == DueState.SOON }
            .sortedWith(compareBy<TaskDue> { it.state.ordinal }.thenBy { it.urgency })

    fun evaluate(task: MaintTask, input: DueInput): TaskDue {
        val ids = setOf(task.id) + task.resetBy
        val last = input.log.filter { entry -> entry.taskIds.any { it in ids } }
            .maxWithOrNull(compareBy<LogEntry> { it.date }.thenBy { it.createdAt })
        if (task.once && last != null) {
            return TaskDue(task, DueState.DONE, null, null, null, null, last.date, last.km, Double.MAX_VALUE)
        }
        val fromLog = last != null
        val baseDate = last?.date ?: input.purchaseDate
        val baseKm = last?.km ?: 0

        val useFirst = !fromLog && (task.firstKm != null || task.firstMonths != null)
        val intervalKm = if (useFirst) task.firstKm else task.km ?: task.hours?.let { it * input.kmPerHour }
        val months = if (useFirst) task.firstMonths else task.months

        val dueKm = intervalKm?.let { baseKm + it }
        val dueDate = if (months != null && baseDate != null) baseDate.plusMonths(months.toLong()) else null
        val lastDate = last?.date
        val lastKm = last?.km

        if (dueKm == null && dueDate == null) {
            return TaskDue(task, DueState.UNKNOWN, null, null, null, null, lastDate, lastKm, Double.MAX_VALUE)
        }
        val remainingKm = dueKm?.let { it - input.odometerKm }
        val remainingDays = dueDate?.let { ChronoUnit.DAYS.between(input.today, it).toInt() }

        val leadKm = intervalKm?.let { min(MAX_LEAD_KM, (0.2 * it).roundToInt()) }
        val leadDays = months?.let { min(MAX_LEAD_DAYS, (0.25 * it * DAYS_PER_MONTH).roundToInt()) }

        val overdue = (remainingKm != null && remainingKm <= 0) || (remainingDays != null && remainingDays <= 0)
        val soon = (remainingKm != null && leadKm != null && remainingKm <= leadKm) ||
            (remainingDays != null && leadDays != null && remainingDays <= leadDays)
        val state = when {
            overdue -> DueState.OVERDUE
            soon -> DueState.SOON
            else -> DueState.OK
        }

        val shares = buildList {
            intervalKm?.takeIf { it > 0 }?.let { interval -> remainingKm?.let { add(it.toDouble() / interval) } }
            months?.takeIf { it > 0 }?.let { m -> remainingDays?.let { add(it.toDouble() / (m * DAYS_PER_MONTH)) } }
        }
        return TaskDue(task, state, dueKm, dueDate, remainingKm, remainingDays, lastDate, lastKm, shares.minOrNull() ?: Double.MAX_VALUE)
    }

    /** Remaining values for every part that exists. From 120 days on the time part is given in months. */
    fun remainingParts(due: TaskDue): List<RemainingPart> = buildList {
        due.remainingKm?.let { add(RemainingPart(RemainingKind.KM, abs(it), it <= 0)) }
        due.remainingDays?.let { days ->
            when {
                days <= 0 -> add(RemainingPart(RemainingKind.DAYS, -days, true))
                days >= MONTHS_FROM_DAYS -> add(RemainingPart(RemainingKind.MONTHS, days / DAYS_PER_MONTH, false))
                else -> add(RemainingPart(RemainingKind.DAYS, days, false))
            }
        }
    }
}
