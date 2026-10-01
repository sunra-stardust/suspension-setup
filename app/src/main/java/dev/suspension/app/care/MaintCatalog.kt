package dev.suspension.app.care

import dev.suspension.app.R

// GENERATED from the Pflege specification; intervals, notes and tags are verbatim, do not "correct" them.

/** Component groups of the interval cards, in display order. [noteRes]/[noteTag]: line under the title. */
enum class MaintGroup(val titleRes: Int, val noteRes: Int? = null, val noteTag: CareTag? = null) {
    WHOLE(R.string.care_group_whole),
    SUSPENSION(R.string.care_group_suspension, R.string.care_group_suspension_note, CareTag.HERSTELLER),
    BRAKES(R.string.care_group_brakes),
    DRIVETRAIN(R.string.care_group_drivetrain),
    FRAME(R.string.care_group_frame),
    POST(R.string.care_group_post),
    WHEELS(R.string.care_group_wheels),
    BATTERY(R.string.care_group_battery),
}

/**
 * One maintenance task. The machine parameters drive [DueCalculator]:
 * [km], [hours] (converted with the rider's km per hour) and [months] recur; with several of them the
 * earliest wins. [once] tasks are finished when logged. [firstKm]/[firstMonths] replace the recurring
 * values until the first log entry. [resetBy]: ids whose log entries also count as "last done".
 */
data class MaintTask(
    val id: String,
    val group: MaintGroup,
    val nameRes: Int,
    val intervalRes: Int,
    val tag: CareTag,
    val noteRes: Int,
    val km: Int? = null,
    val hours: Int? = null,
    val months: Int? = null,
    val once: Boolean = false,
    val firstKm: Int? = null,
    val firstMonths: Int? = null,
    val resetBy: List<String> = emptyList(),
    val remindByDefault: Boolean = true,
)

object MaintCatalog {
    val tasks: List<MaintTask> = listOf(
        MaintTask(
            id = "first_inspection",
            group = MaintGroup.WHOLE,
            nameRes = R.string.care_task_first_inspection_name,
            intervalRes = R.string.care_task_first_inspection_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_first_inspection_note,
            firstKm = 300,
            firstMonths = 1,
            once = true,
        ),
        MaintTask(
            id = "annual_inspection",
            group = MaintGroup.WHOLE,
            nameRes = R.string.care_task_annual_inspection_name,
            intervalRes = R.string.care_task_annual_inspection_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_annual_inspection_note,
            km = 2000,
            months = 12,
            resetBy = listOf("first_inspection"),
        ),
        MaintTask(
            id = "screws",
            group = MaintGroup.WHOLE,
            nameRes = R.string.care_task_screws_name,
            intervalRes = R.string.care_task_screws_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_screws_note,
            firstKm = 100,
            once = true,
        ),
        MaintTask(
            id = "play_check",
            group = MaintGroup.WHOLE,
            nameRes = R.string.care_task_play_check_name,
            intervalRes = R.string.care_task_play_check_interval,
            tag = CareTag.SCHAETZUNG,
            noteRes = R.string.care_task_play_check_note,
            months = 3,
        ),
        MaintTask(
            id = "fork_lower",
            group = MaintGroup.SUSPENSION,
            nameRes = R.string.care_task_fork_lower_name,
            intervalRes = R.string.care_task_fork_lower_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_fork_lower_note,
            hours = 50,
        ),
        MaintTask(
            id = "fork_overhaul",
            group = MaintGroup.SUSPENSION,
            nameRes = R.string.care_task_fork_overhaul_name,
            intervalRes = R.string.care_task_fork_overhaul_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_fork_overhaul_note,
            hours = 125,
            months = 12,
        ),
        MaintTask(
            id = "shock_overhaul",
            group = MaintGroup.SUSPENSION,
            nameRes = R.string.care_task_shock_overhaul_name,
            intervalRes = R.string.care_task_shock_overhaul_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_shock_overhaul_note,
            hours = 125,
            months = 12,
        ),
        MaintTask(
            id = "pads",
            group = MaintGroup.BRAKES,
            nameRes = R.string.care_task_pads_name,
            intervalRes = R.string.care_task_pads_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_pads_note,
            months = 1,
            remindByDefault = false,
        ),
        MaintTask(
            id = "brake_bleed",
            group = MaintGroup.BRAKES,
            nameRes = R.string.care_task_brake_bleed_name,
            intervalRes = R.string.care_task_brake_bleed_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_brake_bleed_note,
            months = 24,
        ),
        MaintTask(
            id = "chain_check",
            group = MaintGroup.DRIVETRAIN,
            nameRes = R.string.care_task_chain_check_name,
            intervalRes = R.string.care_task_chain_check_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_chain_check_note,
            km = 500,
        ),
        MaintTask(
            id = "rear_bearings",
            group = MaintGroup.FRAME,
            nameRes = R.string.care_task_rear_bearings_name,
            intervalRes = R.string.care_task_rear_bearings_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_rear_bearings_note,
            months = 12,
            firstMonths = 6,
        ),
        MaintTask(
            id = "headset",
            group = MaintGroup.FRAME,
            nameRes = R.string.care_task_headset_name,
            intervalRes = R.string.care_task_headset_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_headset_note,
            months = 6,
        ),
        MaintTask(
            id = "post_pressure",
            group = MaintGroup.POST,
            nameRes = R.string.care_task_post_pressure_name,
            intervalRes = R.string.care_task_post_pressure_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_post_pressure_note,
            hours = 10,
            remindByDefault = false,
        ),
        MaintTask(
            id = "post_service",
            group = MaintGroup.POST,
            nameRes = R.string.care_task_post_service_name,
            intervalRes = R.string.care_task_post_service_interval,
            tag = CareTag.HERSTELLER,
            noteRes = R.string.care_task_post_service_note,
            months = 12,
        ),
        MaintTask(
            id = "post_remove",
            group = MaintGroup.POST,
            nameRes = R.string.care_task_post_remove_name,
            intervalRes = R.string.care_task_post_remove_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_post_remove_note,
            months = 12,
        ),
        MaintTask(
            id = "tubeless",
            group = MaintGroup.WHEELS,
            nameRes = R.string.care_task_tubeless_name,
            intervalRes = R.string.care_task_tubeless_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_tubeless_note,
            months = 4,
        ),
        MaintTask(
            id = "battery_check",
            group = MaintGroup.BATTERY,
            nameRes = R.string.care_task_battery_check_name,
            intervalRes = R.string.care_task_battery_check_interval,
            tag = CareTag.FAUSTREGEL,
            noteRes = R.string.care_task_battery_check_note,
            months = 2,
            remindByDefault = false,
        ),
    )

    val byId: Map<String, MaintTask> = tasks.associateBy { it.id }
}
