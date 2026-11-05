package org.example.modules.personal.wallet

/** Keeps digits and one decimal point, at most two decimals. Accepts a comma as the separator. */
internal fun sanitizeAmount(input: String): String {
    val filtered = input.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    if (dot == -1) return filtered.take(9)
    val whole = filtered.substring(0, dot).take(9)
    val fraction = filtered.substring(dot + 1).filter { it.isDigit() }.take(2)
    return "$whole.$fraction"
}

/** True when [text] holds a number (0 included), so a balance of 0 can be saved. */
internal fun isValidAmount(text: String): Boolean = text.isNotBlank() && text != "." && text.any { it.isDigit() }

/** "12.5" -> 1250. Integer math only, so money never touches floating point. */
internal fun toMinorUnits(text: String): Long {
    if (!isValidAmount(text)) return 0L
    val parts = text.split('.')
    val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return 0L
    val fraction = (parts.getOrNull(1) ?: "").padEnd(2, '0').toLongOrNull() ?: 0L
    return whole * 100 + fraction
}

internal fun formatMinor(minor: Long): String = "%d.%02d".format(minor / 100, minor % 100)

/** Philippine peso sign. Change here if the target currency differs. */
internal const val CurrencySymbol = "\u20B1"

/** 1250 -> "\u20B11250.00". Use for display; [formatMinor] stays plain for editable fields. */
internal fun formatMoney(minor: Long): String = CurrencySymbol + formatMinor(minor)
