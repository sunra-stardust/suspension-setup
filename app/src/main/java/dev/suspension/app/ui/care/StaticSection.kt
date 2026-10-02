package dev.suspension.app.ui.care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.care.CareCard
import dev.suspension.app.care.CareParagraph
import dev.suspension.app.care.CareSection
import dev.suspension.app.care.CareTable
import dev.suspension.app.care.CareTag
import dev.suspension.app.care.CareText
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.theme.AppTheme

private val LABEL_COLUMN_WIDTH = 96.dp

/** Below this width (in "text units", so it grows with the font scale) column 1 stacks above column 2. */
private val STACK_BELOW_WIDTH = 240.dp

/** A read-only reference section, rendered entirely from [CareContent] data. */
@Composable
fun StaticSection(section: CareSection) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        section.introRes?.let { res ->
            item { Text(text = stringResource(res), style = AppTheme.type.rowHint, color = AppTheme.colors.dim) }
        }
        items(section.cards.size) { index -> StaticCard(section.cards[index]) }
        section.closing.forEach { paragraph -> item { Closing(paragraph) } }
        item { SectionFooter() }
    }
}

@Composable
private fun StaticCard(card: CareCard) {
    CareCardFrame(title = stringResource(card.titleRes), tag = card.cardTag) {
        GroupCard {
            var first = true
            fun divider(): Boolean = !first.also { first = false }
            card.table?.let { table ->
                TableRows(table)
                first = false
            }
            card.numbered.forEachIndexed { index, item ->
                if (divider()) RowDivider()
                NumberedRow(index + 1, stringResource(item.res))
            }
            card.rows.forEach { row ->
                if (divider()) RowDivider()
                val tag = row.tag ?: card.cardTag
                if (tag != null) {
                    CareStaticRow(
                        label = stringResource(row.labelRes),
                        text = if (row.parts.isNotEmpty()) listSentence(row.parts.map { stringResource(it.res) }) else stringResource(row.textRes),
                        tag = tag,
                        showTag = row.tag != null,
                    )
                }
            }
        }
    }
}

/** One accessibility node per row: `<col 1>: <col 2>. Quelle: <tag>` (tag from the row or its card). */
@Composable
private fun CareStaticRow(label: String, text: String, tag: CareTag, showTag: Boolean) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val description = stringResource(R.string.care_a11y_row, label, text, stringResource(tag.labelResId))
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }) {
        val body: @Composable () -> Unit = {
            Text(text = text, style = type.body, color = colors.ink)
            if (showTag) TagLine(tag, Modifier.padding(top = 2.dp))
        }
        if (maxWidth / fontScale < STACK_BELOW_WIDTH) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(text = label, style = type.rowLabel, color = colors.ink)
                Column(modifier = Modifier.padding(top = 2.dp)) { body() }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(text = label, style = type.rowLabel, color = colors.ink, modifier = Modifier.width(LABEL_COLUMN_WIDTH))
                Column(modifier = Modifier.weight(1f)) { body() }
            }
        }
    }
}

@Composable
private fun NumberedRow(number: Int, text: String) {
    val description = stringResource(R.string.care_a11y_numbered, number, text)
    Text(
        text = "$number. $text",
        style = AppTheme.type.body,
        color = AppTheme.colors.ink,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** Header (dim) plus device rows; a table has no source tags. */
@Composable
private fun TableRows(table: CareTable) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        table.header.forEachIndexed { index, res ->
            Text(text = stringResource(res), style = type.rowHint, color = colors.dim, modifier = Modifier.weight(WEIGHTS[index]))
        }
    }
    table.rows.forEach { row ->
        RowDivider()
        val texts = row.cells.map { stringResource(it) }
        val description = stringResource(R.string.care_a11y_table_row, texts[0], texts[1], texts[2])
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            texts.forEachIndexed { index, text ->
                Text(
                    text = text,
                    style = if (index == 0) type.rowLabel else type.body,
                    color = colors.ink,
                    modifier = Modifier.weight(WEIGHTS[index]),
                )
            }
        }
    }
}

private val WEIGHTS = listOf(1.3f, 1f, 1f)

@Composable
private fun Closing(paragraph: CareParagraph) {
    Column {
        Text(text = stringResource(paragraph.textRes), style = AppTheme.type.body, color = AppTheme.colors.ink)
        TagLine(paragraph.tag, Modifier.padding(top = 2.dp))
    }
}

/** "A, B, C und D." — the list pieces that apply to the bike, as one sentence. */
@Composable
private fun listSentence(items: List<String>): String {
    val joinTemplate = stringResource(R.string.care_list_last)
    return CareText.listSentence(items) { a, b -> String.format(joinTemplate, a, b) }
}

/** Every section ends with the legend for the source tags (not a card). */
@Composable
fun SectionFooter() {
    Text(text = stringResource(R.string.care_footer_legend), style = AppTheme.type.rowHint, color = AppTheme.colors.dim)
}
