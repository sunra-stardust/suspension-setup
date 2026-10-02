package dev.suspension.app.care

import dev.suspension.app.R
import dev.suspension.app.data.BikeTraits
import dev.suspension.app.data.Trait.BRAKES_SRAM_MINERAL
import dev.suspension.app.data.Trait.DROPPER_ONOFF_PIJA
import dev.suspension.app.data.Trait.EBIKE
import dev.suspension.app.data.Trait.EBIKE_BOSCH
import dev.suspension.app.data.Trait.FORK_FOX
import dev.suspension.app.data.Trait.FRAME_MONDRAKER_LEVEL
import dev.suspension.app.data.Trait.SHIFTING_SRAM_AXS
import dev.suspension.app.data.Trait.SHOCK_COIL
import dev.suspension.app.data.Trait.SHOCK_FOX
import dev.suspension.app.data.has
import dev.suspension.app.data.lacks

// Generated from the Pflege specification (German texts verbatim), then made bike-aware: whatever
// names a motor, a maker or a specific part carries a condition. Neutral variants (`*_generic`)
// leave the bike-specific part out and add no new facts.

/** Static reference content of the `Pflege` tab (every section but the calendar's interval cards). */
object CareContent {
    /** Motor, display, charging port: any e-bike. */
    private val ebike = listOf(has(EBIKE))
    private val noEbike = listOf(lacks(EBIKE))
    /** Statements by Bosch (battery, Mini Remote): only with a Bosch drive. */
    private val bosch = listOf(has(EBIKE_BOSCH))

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
                        CareRow(R.string.care_after_ride_c1_r1_label, R.string.care_after_ride_c1_r1_text, cond = ebike),
                        CareRow(R.string.care_after_ride_c1_r1_label, R.string.care_after_ride_c1_r1_text_generic, cond = noEbike),
                        CareRow(R.string.care_after_ride_c1_r2_label, R.string.care_after_ride_c1_r2_text, cond = ebike),
                        CareRow(R.string.care_after_ride_c1_r2_label, R.string.care_after_ride_c1_r2_text_generic, cond = noEbike),
                        CareRow(R.string.care_after_ride_c1_r3_label, R.string.care_after_ride_c1_r3_text, cond = ebike),
                        CareRow(R.string.care_after_ride_c1_r4_label, R.string.care_after_ride_c1_r4_text),
                        CareRow(R.string.care_after_ride_c1_r5_label, R.string.care_after_ride_c1_r5_text),
                        CareRow(R.string.care_after_ride_c1_r6_label, R.string.care_after_ride_c1_r6_text),
                        CareRow(R.string.care_after_ride_c1_r7_label, R.string.care_after_ride_c1_r7_text, cond = ebike),
                        CareRow(R.string.care_after_ride_c1_r7_label, R.string.care_after_ride_c1_r7_text_generic, cond = noEbike),
                        CareRow(R.string.care_after_ride_c1_r8_label, R.string.care_after_ride_c1_r8_text, cond = ebike),
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
            closing = listOf(
                CareParagraph(R.string.care_products_closing, CareTag.FAUSTREGEL, cond = ebike),
                CareParagraph(R.string.care_products_closing_generic, CareTag.FAUSTREGEL, cond = noEbike),
            ),
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
                        CareRow(R.string.care_protection_c1_r2_label, R.string.care_protection_c1_r2_text, cond = ebike),
                        CareRow(R.string.care_protection_c1_r2_label, R.string.care_protection_c1_r2_text_generic, cond = noEbike),
                        CareRow(R.string.care_protection_c1_r3_label, R.string.care_protection_c1_r3_text),
                        CareRow(R.string.care_protection_c1_r4_label, R.string.care_protection_c1_r4_text),
                        CareRow(R.string.care_protection_c1_r5_label, R.string.care_protection_c1_r5_text),
                        CareRow(R.string.care_protection_c1_r6_label, R.string.care_protection_c1_r6_text),
                        CareRow(R.string.care_protection_c1_r7_label, R.string.care_protection_c1_r7_text, cond = ebike),
                        CareRow(R.string.care_protection_c1_r7_label, R.string.care_protection_c1_r7_text_generic, cond = noEbike),
                    ),
                ),
                CareCard(
                    titleRes = R.string.care_protection_c2_title,
                    cardTag = CareTag.FAUSTREGEL,
                    rows = listOf(
                        CareRow(R.string.care_protection_c2_r1_label, R.string.care_protection_c2_r1_text),
                        CareRow(R.string.care_protection_c2_r2_label, R.string.care_protection_c2_r2_text),
                        // "Bremsscheiben (Flugrost), Kette, Stahlfeder, Kontakte und Lager." — spring and
                        // contacts only where the bike has them.
                        CareRow(
                            R.string.care_protection_c2_r3_label,
                            R.string.care_protection_c2_r3_text,
                            parts = listOf(
                                CarePart(R.string.care_part_rotors),
                                CarePart(R.string.care_part_chain),
                                CarePart(R.string.care_part_coil, listOf(has(SHOCK_COIL))),
                                CarePart(R.string.care_part_contacts, ebike),
                                CarePart(R.string.care_part_bearings),
                            ),
                        ),
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
            // Bosch statements throughout: only for a bike known to have a Bosch drive.
            cond = bosch,
            cards = listOf(
                CareCard(
                    titleRes = R.string.care_battery_c1_title,
                    numbered = listOf(
                        CareItem(R.string.care_battery_c1_n1),
                        CareItem(R.string.care_battery_c1_n2),
                        CareItem(R.string.care_battery_c1_n3),
                        CareItem(R.string.care_battery_c1_n4),
                        CareItem(R.string.care_battery_c1_n5),
                        CareItem(R.string.care_battery_c1_n6, listOf(has(SHIFTING_SRAM_AXS))),
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
                        CareRow(R.string.care_cells_c1_r1_label, R.string.care_cells_c1_r1_text, CareTag.HERSTELLER, bosch),
                        CareRow(
                            R.string.care_cells_c1_r2_label, R.string.care_cells_c1_r2_text, CareTag.FAUSTREGEL,
                            listOf(has(EBIKE_BOSCH, SHIFTING_SRAM_AXS)),
                        ),
                        // One CR1620 (Bosch remote) and one CR2032 (AXS pod): only with both.
                        CareRow(
                            R.string.care_cells_c1_r3_label, R.string.care_cells_c1_r3_text, CareTag.FAUSTREGEL,
                            listOf(has(EBIKE_BOSCH), has(SHIFTING_SRAM_AXS)),
                        ),
                        CareRow(R.string.care_cells_c1_r4_label, R.string.care_cells_c1_r4_text, CareTag.FAUSTREGEL, listOf(has(SHIFTING_SRAM_AXS))),
                    ),
                    table = CareTable(
                        header = listOf(R.string.care_cells_c1_th1, R.string.care_cells_c1_th2, R.string.care_cells_c1_th3),
                        rows = listOf(
                            CareTableRow(listOf(R.string.care_cells_c1_t1_1, R.string.care_cells_c1_t1_2, R.string.care_cells_c1_t1_3), bosch),
                            CareTableRow(
                                listOf(R.string.care_cells_c1_t2_1, R.string.care_cells_c1_t2_2, R.string.care_cells_c1_t2_3),
                                listOf(has(SHIFTING_SRAM_AXS)),
                            ),
                            CareTableRow(
                                listOf(R.string.care_cells_c1_t3_1, R.string.care_cells_c1_t3_2, R.string.care_cells_c1_t3_3),
                                listOf(has(SHIFTING_SRAM_AXS)),
                            ),
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
                        // Names Fox suspension, SRAM and Bosch as the standard parts of this exact bike.
                        CareRow(
                            R.string.care_parts_c1_r1_label,
                            R.string.care_parts_c1_r1_text,
                            cond = listOf(has(FRAME_MONDRAKER_LEVEL), has(FORK_FOX), has(SHOCK_FOX), has(EBIKE_BOSCH), has(BRAKES_SRAM_MINERAL)),
                        ),
                        CareRow(R.string.care_parts_c1_r2_label, R.string.care_parts_c1_r2_text, cond = listOf(has(FRAME_MONDRAKER_LEVEL))),
                        CareRow(R.string.care_parts_c1_r3_label, R.string.care_parts_c1_r3_text, cond = listOf(has(DROPPER_ONOFF_PIJA))),
                        CareRow(R.string.care_parts_c1_r4_label, R.string.care_parts_c1_r4_text, cond = listOf(has(FRAME_MONDRAKER_LEVEL))),
                    ),
                ),
                CareCard(
                    titleRes = R.string.care_parts_c2_title,
                    cardTag = CareTag.HERSTELLER,
                    rows = listOf(
                        CareRow(R.string.care_parts_c2_r1_label, R.string.care_parts_c2_r1_text),
                    ),
                    cond = listOf(has(DROPPER_ONOFF_PIJA)),
                ),
            ),
        ),
    )

    const val CALENDAR_ID = "calendar"

    /** The sections and their content as the given bike sees them; sections without content drop out. */
    fun forBike(bike: BikeTraits): List<CareSection> = sections.mapNotNull { it.forBike(bike) }
}
