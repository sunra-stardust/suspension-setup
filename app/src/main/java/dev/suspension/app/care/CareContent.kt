package dev.suspension.app.care

import dev.suspension.app.R

// GENERATED from the Pflege specification; the German texts are verbatim, do not rephrase.

/** Static reference content of the `Pflege` tab (every section but the calendar's interval cards). */
object CareContent {
    /** Chip order of the tab; the calendar comes first. */
    val sections: List<CareSection> = listOf(
    CareSection(
        id = "calendar",
        chipLabelRes = R.string.care_chip_calendar,
    ),
    CareSection(
        id = "after_ride",
        chipLabelRes = R.string.care_chip_after_ride,
        cards = listOf(
            CareCard(
                titleRes = R.string.care_after_ride_c1_title,
                cardTag = CareTag.FAUSTREGEL,
                rows = listOf(
                    CareRow(R.string.care_after_ride_c1_r1_label, R.string.care_after_ride_c1_r1_text),
                    CareRow(R.string.care_after_ride_c1_r2_label, R.string.care_after_ride_c1_r2_text),
                    CareRow(R.string.care_after_ride_c1_r3_label, R.string.care_after_ride_c1_r3_text),
                    CareRow(R.string.care_after_ride_c1_r4_label, R.string.care_after_ride_c1_r4_text),
                    CareRow(R.string.care_after_ride_c1_r5_label, R.string.care_after_ride_c1_r5_text),
                    CareRow(R.string.care_after_ride_c1_r6_label, R.string.care_after_ride_c1_r6_text),
                    CareRow(R.string.care_after_ride_c1_r7_label, R.string.care_after_ride_c1_r7_text),
                    CareRow(R.string.care_after_ride_c1_r8_label, R.string.care_after_ride_c1_r8_text),
                    CareRow(R.string.care_after_ride_c1_r9_label, R.string.care_after_ride_c1_r9_text),
                ),
            ),
        ),
    ),
    CareSection(
        id = "products",
        chipLabelRes = R.string.care_chip_products,
        cards = listOf(
            CareCard(
                titleRes = R.string.care_products_c1_title,
                cardTag = CareTag.FAUSTREGEL,
                rows = listOf(
                    CareRow(R.string.care_products_c1_r1_label, R.string.care_products_c1_r1_text),
                    CareRow(R.string.care_products_c1_r2_label, R.string.care_products_c1_r2_text),
                    CareRow(R.string.care_products_c1_r3_label, R.string.care_products_c1_r3_text),
                    CareRow(R.string.care_products_c1_r4_label, R.string.care_products_c1_r4_text),
                    CareRow(R.string.care_products_c1_r5_label, R.string.care_products_c1_r5_text),
                    CareRow(R.string.care_products_c1_r6_label, R.string.care_products_c1_r6_text),
                ),
            ),
            CareCard(
                titleRes = R.string.care_products_c2_title,
                cardTag = CareTag.FAUSTREGEL,
                rows = listOf(
                    CareRow(R.string.care_products_c2_r1_label, R.string.care_products_c2_r1_text),
                    CareRow(R.string.care_products_c2_r2_label, R.string.care_products_c2_r2_text),
                    CareRow(R.string.care_products_c2_r3_label, R.string.care_products_c2_r3_text),
                    CareRow(R.string.care_products_c2_r4_label, R.string.care_products_c2_r4_text),
                ),
            ),
        ),
        closing = CareParagraph(R.string.care_products_closing, CareTag.FAUSTREGEL),
    ),
    CareSection(
        id = "protection",
        chipLabelRes = R.string.care_chip_protection,
        cards = listOf(
            CareCard(
                titleRes = R.string.care_protection_c1_title,
                cardTag = CareTag.FAUSTREGEL,
                rows = listOf(
                    CareRow(R.string.care_protection_c1_r1_label, R.string.care_protection_c1_r1_text),
                    CareRow(R.string.care_protection_c1_r2_label, R.string.care_protection_c1_r2_text),
                    CareRow(R.string.care_protection_c1_r3_label, R.string.care_protection_c1_r3_text),
                    CareRow(R.string.care_protection_c1_r4_label, R.string.care_protection_c1_r4_text),
                    CareRow(R.string.care_protection_c1_r5_label, R.string.care_protection_c1_r5_text),
                    CareRow(R.string.care_protection_c1_r6_label, R.string.care_protection_c1_r6_text),
                    CareRow(R.string.care_protection_c1_r7_label, R.string.care_protection_c1_r7_text),
                ),
            ),
            CareCard(
                titleRes = R.string.care_protection_c2_title,
                cardTag = CareTag.FAUSTREGEL,
                rows = listOf(
                    CareRow(R.string.care_protection_c2_r1_label, R.string.care_protection_c2_r1_text),
                    CareRow(R.string.care_protection_c2_r2_label, R.string.care_protection_c2_r2_text),
                    CareRow(R.string.care_protection_c2_r3_label, R.string.care_protection_c2_r3_text),
                    CareRow(R.string.care_protection_c2_r4_label, R.string.care_protection_c2_r4_text),
                    CareRow(R.string.care_protection_c2_r5_label, R.string.care_protection_c2_r5_text),
                    CareRow(R.string.care_protection_c2_r6_label, R.string.care_protection_c2_r6_text),
                    CareRow(R.string.care_protection_c2_r7_label, R.string.care_protection_c2_r7_text),
                ),
            ),
        ),
    ),
    CareSection(
        id = "battery",
        chipLabelRes = R.string.care_chip_battery,
        introRes = R.string.care_battery_intro,
        cards = listOf(
            CareCard(
                titleRes = R.string.care_battery_c1_title,
                numbered = listOf(
                    R.string.care_battery_c1_n1,
                    R.string.care_battery_c1_n2,
                    R.string.care_battery_c1_n3,
                    R.string.care_battery_c1_n4,
                    R.string.care_battery_c1_n5,
                    R.string.care_battery_c1_n6,
                ),
                untagged = true,
            ),
            CareCard(
                titleRes = R.string.care_battery_c2_title,
                rows = listOf(
                    CareRow(R.string.care_battery_c2_r1_label, R.string.care_battery_c2_r1_text, CareTag.HERSTELLER),
                    CareRow(R.string.care_battery_c2_r2_label, R.string.care_battery_c2_r2_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c2_r3_label, R.string.care_battery_c2_r3_text, CareTag.HERSTELLER),
                    CareRow(R.string.care_battery_c2_r4_label, R.string.care_battery_c2_r4_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c2_r5_label, R.string.care_battery_c2_r5_text, CareTag.HERSTELLER),
                    CareRow(R.string.care_battery_c2_r6_label, R.string.care_battery_c2_r6_text, CareTag.FAUSTREGEL),
                ),
            ),
            CareCard(
                titleRes = R.string.care_battery_c3_title,
                rows = listOf(
                    CareRow(R.string.care_battery_c3_r1_label, R.string.care_battery_c3_r1_text, CareTag.SCHAETZUNG),
                    CareRow(R.string.care_battery_c3_r2_label, R.string.care_battery_c3_r2_text, CareTag.SCHAETZUNG),
                    CareRow(R.string.care_battery_c3_r3_label, R.string.care_battery_c3_r3_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c3_r4_label, R.string.care_battery_c3_r4_text, CareTag.FAUSTREGEL),
                ),
            ),
            CareCard(
                titleRes = R.string.care_battery_c4_title,
                rows = listOf(
                    CareRow(R.string.care_battery_c4_r1_label, R.string.care_battery_c4_r1_text, CareTag.HERSTELLER),
                    CareRow(R.string.care_battery_c4_r2_label, R.string.care_battery_c4_r2_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c4_r3_label, R.string.care_battery_c4_r3_text, CareTag.HERSTELLER),
                    CareRow(R.string.care_battery_c4_r4_label, R.string.care_battery_c4_r4_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c4_r5_label, R.string.care_battery_c4_r5_text, CareTag.SCHAETZUNG),
                    CareRow(R.string.care_battery_c4_r6_label, R.string.care_battery_c4_r6_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c4_r7_label, R.string.care_battery_c4_r7_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c4_r8_label, R.string.care_battery_c4_r8_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c4_r9_label, R.string.care_battery_c4_r9_text, CareTag.HERSTELLER),
                ),
            ),
            CareCard(
                titleRes = R.string.care_battery_c5_title,
                rows = listOf(
                    CareRow(R.string.care_battery_c5_r1_label, R.string.care_battery_c5_r1_text, CareTag.SCHAETZUNG),
                    CareRow(R.string.care_battery_c5_r2_label, R.string.care_battery_c5_r2_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_battery_c5_r3_label, R.string.care_battery_c5_r3_text, CareTag.HERSTELLER),
                ),
            ),
        ),
    ),
    CareSection(
        id = "cells",
        chipLabelRes = R.string.care_chip_cells,
        cards = listOf(
            CareCard(
                titleRes = R.string.care_cells_c1_title,
                rows = listOf(
                    CareRow(R.string.care_cells_c1_r1_label, R.string.care_cells_c1_r1_text, CareTag.HERSTELLER),
                    CareRow(R.string.care_cells_c1_r2_label, R.string.care_cells_c1_r2_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_cells_c1_r3_label, R.string.care_cells_c1_r3_text, CareTag.FAUSTREGEL),
                    CareRow(R.string.care_cells_c1_r4_label, R.string.care_cells_c1_r4_text, CareTag.FAUSTREGEL),
                ),
                table = CareTable(
                    header = listOf(R.string.care_cells_c1_th1, R.string.care_cells_c1_th2, R.string.care_cells_c1_th3),
                    rows = listOf(
                        listOf(R.string.care_cells_c1_t1_1, R.string.care_cells_c1_t1_2, R.string.care_cells_c1_t1_3),
                        listOf(R.string.care_cells_c1_t2_1, R.string.care_cells_c1_t2_2, R.string.care_cells_c1_t2_3),
                        listOf(R.string.care_cells_c1_t3_1, R.string.care_cells_c1_t3_2, R.string.care_cells_c1_t3_3),
                    ),
                ),
            ),
        ),
    ),
    CareSection(
        id = "parts",
        chipLabelRes = R.string.care_chip_parts,
        cards = listOf(
            CareCard(
                titleRes = R.string.care_parts_c1_title,
                cardTag = CareTag.FAUSTREGEL,
                rows = listOf(
                    CareRow(R.string.care_parts_c1_r1_label, R.string.care_parts_c1_r1_text),
                    CareRow(R.string.care_parts_c1_r2_label, R.string.care_parts_c1_r2_text),
                    CareRow(R.string.care_parts_c1_r3_label, R.string.care_parts_c1_r3_text),
                    CareRow(R.string.care_parts_c1_r4_label, R.string.care_parts_c1_r4_text),
                ),
            ),
            CareCard(
                titleRes = R.string.care_parts_c2_title,
                cardTag = CareTag.HERSTELLER,
                rows = listOf(
                    CareRow(R.string.care_parts_c2_r1_label, R.string.care_parts_c2_r1_text),
                ),
            ),
        ),
    ),
    )

    const val CALENDAR_ID = "calendar"
}
