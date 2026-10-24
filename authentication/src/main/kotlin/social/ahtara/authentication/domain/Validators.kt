package social.ahtara.authentication.domain

import java.time.DateTimeException
import java.time.LocalDate


object Validators {
    private val PH_MOBILE = Regex("^9\\d{9}$")
    private val USERNAME = Regex("^[A-Za-z][A-Za-z0-9_]{2,19}$")
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val PHONE_LIKE = Regex("^\\+?[\\d\\s\\-().]+$")

    enum class Problem { REQUIRED, INVALID, WEAK, UNDERAGE }

    
    const val MIN_AGE = 13

    fun name(value: String): Problem? =
        if (value.isBlank()) Problem.REQUIRED else null

    



    fun nationalDigits(country: Country, raw: String): String {
        val t = raw.trim()
        var digits = t.filter { it.isDigit() }
        if (t.startsWith("+") && digits.startsWith(country.dialCode)) digits = digits.removePrefix(country.dialCode)
        return digits.removePrefix("0")
    }

    
    fun mobile(country: Country, raw: String): Problem? {
        if (raw.none { it.isDigit() }) return Problem.REQUIRED
        val n = nationalDigits(country, raw)
        val ok = if (country.iso == "PH") PH_MOBILE.matches(n)
        else n.length >= 4 && country.dialCode.length + n.length <= 15
        return if (ok) null else Problem.INVALID
    }

    
    fun username(value: String): Problem? {
        val v = value.trim()
        if (v.isEmpty()) return Problem.REQUIRED
        return if (USERNAME.matches(v)) null else Problem.INVALID
    }

    fun email(value: String): Problem? {
        val v = value.trim()
        if (v.isEmpty()) return Problem.REQUIRED
        return if (EMAIL.matches(v)) null else Problem.INVALID
    }

    




    fun loginKey(country: Country, raw: String): String {
        val t = raw.trim()
        return when {
            '@' in t -> t.lowercase()
            PHONE_LIKE.matches(t) && t.any { it.isDigit() } ->
                if (t.startsWith("+")) "+" + t.filter { it.isDigit() } else toE164(country, t)
            else -> t.lowercase()
        }
    }

    



    fun cleanPhMobile(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.startsWith("63") && (raw.trimStart().startsWith("+") || d.length > 10)) d = d.drop(2)
        return d.removePrefix("0").take(10)
    }

    
    fun birthday(month: String, day: String, year: String, today: LocalDate = LocalDate.now()): Problem? {
        if (month.isBlank() || day.isBlank() || year.isBlank()) return Problem.REQUIRED
        val m = month.toIntOrNull()
        val d = day.toIntOrNull()
        val y = year.toIntOrNull()
        if (m == null || d == null || y == null || year.length != 4 || y < 1900) return Problem.INVALID
        val date = try {
            LocalDate.of(y, m, d)
        } catch (_: DateTimeException) {
            return Problem.INVALID
        }
        return when {
            date.isAfter(today) -> Problem.INVALID
            date.isAfter(today.minusYears(MIN_AGE.toLong())) -> Problem.UNDERAGE
            else -> null
        }
    }

    
    fun birthdayIso(month: String, day: String, year: String): String =
        LocalDate.of(year.toInt(), month.toInt(), day.toInt()).toString()

    
    fun toE164(country: Country, raw: String): String = "+${country.dialCode}${nationalDigits(country, raw)}"

    fun required(value: String): Problem? =
        if (value.isBlank()) Problem.REQUIRED else null

    
    fun phone(value: String): Problem? {
        val v = value.trim()
        if (v.isEmpty()) return Problem.REQUIRED
        val allowedChars = v.all { it.isDigit() || it in " -.()+" } && v.indexOf('+') <= 0
        return if (allowedChars && v.count { it.isDigit() } in 7..15) null else Problem.INVALID
    }

    
    fun newPassword(value: String): Problem? = when {
        value.isEmpty() -> Problem.REQUIRED
        value.length < 8 || value.none { it.isLetter() } || value.none { it.isDigit() } -> Problem.WEAK
        else -> null
    }

    fun existingPassword(value: String): Problem? =
        if (value.isEmpty()) Problem.REQUIRED else null
}