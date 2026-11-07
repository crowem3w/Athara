package soc.athara.app.modules.personal.wallet





internal data class Currency(val code: String, val symbol: String, val name: String)

internal val PhilippinePeso = Currency("PHP", CurrencySymbol, "Philippine peso")


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
