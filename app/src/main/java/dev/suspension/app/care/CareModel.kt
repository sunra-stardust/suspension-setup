package dev.suspension.app.care

import dev.suspension.app.R

/**
 * Where a statement comes from. The tag text is rendered from this enum, never from the content
 * string, so a tag cannot be edited out of a row.
 */
enum class CareTag(val labelResId: Int) {
    HERSTELLER(R.string.care_tag_hersteller),
    FAUSTREGEL(R.string.care_tag_faustregel),
    SCHAETZUNG(R.string.care_tag_schaetzung),
}

/** Static two-column row. [tag] is the row-level tag; null when the card carries one for all rows. */
data class CareRow(val labelRes: Int, val textRes: Int, val tag: CareTag? = null)

/** Three-column table without tags (device table in `Batterien`). */
data class CareTable(val header: List<Int>, val rows: List<List<Int>>)

/** Free paragraph with its own tag. */
data class CareParagraph(val textRes: Int, val tag: CareTag)

/**
 * A card either has a [cardTag] (shown once under the title) or tags on every row, never both,
 * never neither, unless it is [untagged] (reference list without sources) or [userData].
 */
data class CareCard(
    val titleRes: Int,
    val cardTag: CareTag? = null,
    val rows: List<CareRow> = emptyList(),
    val numbered: List<Int> = emptyList(),
    val table: CareTable? = null,
    val untagged: Boolean = false,
    val userData: Boolean = false,
)

data class CareSection(
    val id: String,
    val chipLabelRes: Int,
    val introRes: Int? = null,
    val cards: List<CareCard> = emptyList(),
    val closing: CareParagraph? = null,
)
