package dev.suspension.app.data

/**
 * A UI string that is either fixed or formatted with computed values (e.g. the Fox pressure for
 * the rider's weight bracket). Data code builds these; the UI resolves them via `stringResource`,
 * so every user-visible word still lives in strings.xml.
 */
sealed class TextSpec {
    data class Res(val id: Int) : TextSpec()
    data class Format(val id: Int, val args: List<Any>) : TextSpec()

    /** A fractional format argument, rendered with the UI language's decimal separator. */
    data class Decimal(val value: Double, val step: Double)
}
