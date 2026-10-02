package dev.suspension.app.care

import dev.suspension.app.R
import dev.suspension.app.data.BikeTraits
import dev.suspension.app.data.Cond

/**
 * Where a statement comes from. The tag text is rendered from this enum, never from the content
 * string, so a tag cannot be edited out of a row.
 */
enum class CareTag(val labelResId: Int) {
    HERSTELLER(R.string.care_tag_hersteller),
    FAUSTREGEL(R.string.care_tag_faustregel),
    SCHAETZUNG(R.string.care_tag_schaetzung),
}

/** A piece of a list sentence ("Bremsscheiben (Flugrost), Kette, Stahlfeder …") shown only if [cond] holds. */
data class CarePart(val res: Int, val cond: List<Cond> = emptyList())

/**
 * Static two-column row. [tag] is the row-level tag; null when the card carries one for all rows.
 * [cond]: shown only for bikes with these traits. [parts]: when set, the text is these pieces
 * joined as a list ("A, B und C") instead of [textRes].
 */
data class CareRow(
    val labelRes: Int,
    val textRes: Int,
    val tag: CareTag? = null,
    val cond: List<Cond> = emptyList(),
    val parts: List<CarePart> = emptyList(),
)

data class CareItem(val res: Int, val cond: List<Cond> = emptyList())

data class CareTableRow(val cells: List<Int>, val cond: List<Cond> = emptyList())

/** Three-column table without tags (device table in `Batterien`). */
data class CareTable(val header: List<Int>, val rows: List<CareTableRow>)

/** Free paragraph with its own tag. */
data class CareParagraph(val textRes: Int, val tag: CareTag, val cond: List<Cond> = emptyList())

/**
 * A card either has a [cardTag] (shown once under the title) or tags on every row, never both,
 * never neither, unless it is [untagged] (reference list without sources) or [userData].
 */
data class CareCard(
    val titleRes: Int,
    val cardTag: CareTag? = null,
    val rows: List<CareRow> = emptyList(),
    val numbered: List<CareItem> = emptyList(),
    val table: CareTable? = null,
    val untagged: Boolean = false,
    val userData: Boolean = false,
    val cond: List<Cond> = emptyList(),
) {
    val isEmpty: Boolean get() = rows.isEmpty() && numbered.isEmpty() && (table == null || table.rows.isEmpty())

    /** This card as the given bike sees it: rows, items and table rows that don't apply are left out. */
    fun forBike(bike: BikeTraits): CareCard? {
        if (!bike.allows(cond)) return null
        val filtered = copy(
            rows = rows.filter { bike.allows(it.cond) }.map { row -> row.copy(parts = row.parts.filter { bike.allows(it.cond) }) },
            numbered = numbered.filter { bike.allows(it.cond) },
            table = table?.let { t -> t.copy(rows = t.rows.filter { bike.allows(it.cond) }) },
        )
        return filtered.takeUnless { it.isEmpty }
    }
}

data class CareSection(
    val id: String,
    val chipLabelRes: Int,
    val introRes: Int? = null,
    val cards: List<CareCard> = emptyList(),
    /** Closing paragraphs; each carries its own condition (variants for different bikes). */
    val closing: List<CareParagraph> = emptyList(),
    val cond: List<Cond> = emptyList(),
) {
    /** Null when nothing of the section applies to this bike (its chip is hidden then). */
    fun forBike(bike: BikeTraits): CareSection? {
        if (id == CareContent.CALENDAR_ID) return this
        if (!bike.allows(cond)) return null
        val visibleCards = cards.mapNotNull { it.forBike(bike) }
        if (visibleCards.isEmpty()) return null
        return copy(cards = visibleCards, closing = closing.filter { bike.allows(it.cond) })
    }
}

object CareText {
    /** "A, B, C und D." — [joinLast] joins the last two pieces in the UI language ("%1$s und %2$s"). */
    fun listSentence(items: List<String>, joinLast: (String, String) -> String): String {
        val body = if (items.size < 2) items.joinToString() else joinLast(items.dropLast(1).joinToString(", "), items.last())
        return "$body."
    }
}
