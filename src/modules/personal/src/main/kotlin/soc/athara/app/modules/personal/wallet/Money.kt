package soc.athara.app.modules.personal.wallet


internal fun sanitizeAmount(input: String): String {
    val filtered = input.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    if (dot == -1) return filtered.take(9)
    val whole = filtered.substring(0, dot).take(9)
    val fraction = filtered.substring(dot + 1).filter { it.isDigit() }.take(2)
    return "$whole.$fraction"
}


internal fun isValidAmount(text: String): Boolean = text.isNotBlank() && text != "." && text.any { it.isDigit() }


internal fun toMinorUnits(text: String): Long {
    if (!isValidAmount(text)) return 0L
    val parts = text.split('.')
    val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return 0L
    val fraction = (parts.getOrNull(1) ?: "").padEnd(2, '0').toLongOrNull() ?: 0L
    return whole * 100 + fraction
}

internal fun formatMinor(minor: Long): String = "%d.%02d".format(minor / 100, minor % 100)


internal const val CurrencySymbol = "\u20B1"


internal fun formatMoney(minor: Long): String = CurrencySymbol + formatMinor(minor)
