package soc.athara.app.modules.personal.wallet

/**
 * A currency the Pay sheet can show. UI only for now: there is still a single
 * PHP wallet on the server; other currencies show 0.00 until the ledger is multi-currency.
 */
internal data class Currency(val code: String, val symbol: String, val name: String)

internal val PhilippinePeso = Currency("PHP", CurrencySymbol, "Philippine peso")

/** Shown in the currency picker. The first entry is the default. */
internal val SupportedCurrencies: List<Currency> = listOf(
    PhilippinePeso,
    Currency("USD", "$", "US dollar"),
    Currency("EUR", "€", "Euro"),
    Currency("JPY", "¥", "Japanese yen"),
    Currency("GBP", "£", "British pound"),
    Currency("SGD", "S$", "Singapore dollar"),
)

internal fun currencyByCode(code: String): Currency =
    SupportedCurrencies.firstOrNull { it.code == code } ?: PhilippinePeso
