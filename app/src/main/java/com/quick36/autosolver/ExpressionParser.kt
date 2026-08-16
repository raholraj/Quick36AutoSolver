package com.quick36.autosolver

/**
 * Parses a simple two-operand math expression like "4 + 4", "12 x 7", "9 - 3"
 * and returns the integer answer.
 */
object ExpressionParser {

    private val EXPR_REGEX = Regex("""(\d+)\s*([+\-x×÷*/])\s*(\d+)""")

    fun solve(raw: String): Int? {
        val match = EXPR_REGEX.find(raw) ?: return null
        val (aStr, op, bStr) = match.destructured
        val a = aStr.toIntOrNull() ?: return null
        val b = bStr.toIntOrNull() ?: return null

        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "x", "×", "*" -> a * b
            "÷", "/" -> if (b != 0) a / b else null
            else -> null
        }
    }

    fun cleanOcrText(text: String): String {
        return text
            .replace("t", "+", ignoreCase = true)
            .filter { it.isDigit() || it in "+-x×÷*/ " }
            .trim()
    }
}
