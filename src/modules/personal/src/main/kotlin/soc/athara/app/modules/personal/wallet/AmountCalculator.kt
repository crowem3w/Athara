package soc.athara.app.modules.personal.wallet

import java.math.BigDecimal
import java.math.RoundingMode







internal object AmountCalculator {
    const val Plus = '+'
    const val Minus = '−'
    const val Times = '×'
    const val Divide = '÷'

    private val operators = setOf(Plus, Minus, Times, Divide)
    private const val MaxWholeDigits = 9
    private const val MaxDecimals = 2
    private const val MaxLength = 30

    fun isOperator(c: Char) = c in operators

    fun hasOperator(expr: String) = expr.any(::isOperator)

    
    private fun currentOperand(expr: String) = expr.takeLastWhile { !isOperator(it) }

    fun digit(expr: String, d: Char): String {
        if (expr.length >= MaxLength) return expr
        val operand = currentOperand(expr)
        val dot = operand.indexOf('.')
        if (dot == -1) {
            
            if (operand == "0") return expr.dropLast(1) + d
            if (operand.length >= MaxWholeDigits) return expr
        } else if (operand.length - dot - 1 >= MaxDecimals) {
            return expr
        }
        return expr + d
    }

    fun dot(expr: String): String {
        if (expr.length >= MaxLength) return expr
        val operand = currentOperand(expr)
        if ('.' in operand) return expr
        return if (operand.isEmpty()) expr + "0." else "$expr."
    }

    fun operator(expr: String, op: Char): String {
        if (expr.isEmpty()) return expr
        val last = expr.last()
        return when {
            isOperator(last) -> expr.dropLast(1) + op
            last == '.' -> expr.dropLast(1) + op
            expr.length >= MaxLength -> expr
            else -> expr + op
        }
    }

    fun backspace(expr: String) = expr.dropLast(1)

    



    fun resolve(expr: String): String {
        val value = evaluate(expr) ?: return expr
        if (value.signum() < 0) return expr
        return value.setScale(MaxDecimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    
    fun toMinor(expr: String): Long {
        val resolved = if (hasOperator(expr)) resolve(expr) else expr
        if (hasOperator(resolved)) return 0L
        return toMinorUnits(resolved)
    }

    
    fun evaluate(expr: String): BigDecimal? {
        if (expr.isEmpty() || isOperator(expr.last())) return null
        val numbers = ArrayList<BigDecimal>()
        val ops = ArrayList<Char>()
        var buf = StringBuilder()
        for (c in expr) {
            if (isOperator(c)) {
                numbers += buf.toString().toBigDecimalOrNull() ?: return null
                ops += c
                buf = StringBuilder()
            } else buf.append(c)
        }
        numbers += buf.toString().toBigDecimalOrNull() ?: return null

        
        val terms = ArrayList<BigDecimal>().apply { add(numbers[0]) }
        val addOps = ArrayList<Char>()
        for (i in ops.indices) {
            val rhs = numbers[i + 1]
            when (ops[i]) {
                Times -> terms[terms.lastIndex] = terms.last().multiply(rhs)
                Divide -> {
                    if (rhs.signum() == 0) return null
                    terms[terms.lastIndex] = terms.last().divide(rhs, 8, RoundingMode.HALF_UP)
                }
                else -> { addOps += ops[i]; terms += rhs }
            }
        }
        
        var result = terms[0]
        for (i in addOps.indices) {
            result = if (addOps[i] == Plus) result.add(terms[i + 1]) else result.subtract(terms[i + 1])
        }
        return result
    }
}
