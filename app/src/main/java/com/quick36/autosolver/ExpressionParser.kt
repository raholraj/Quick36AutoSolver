package com.quick36.autosolver

/**
 * Parses two-operand expressions: "4 + 4", "12x7", "9 - 3", "8 ÷ 2", etc.
 */
object ExpressionParser {

    // Allows optional spaces, unicode operators, and "x" / "X" for multiply
    private val EXPR_REGEX = Regex("""(\d+)\s*([+\-x×÷*/X])\s*(\d+)""")

    fun solve(raw: String): Int? {
        val match = EXPR_REGEX.find(raw.trim()) ?: return null
        val (aStr, op, bStr) = match.destructured
        val a = aStr.toIntOrNull() ?: return null
        val b = bStr.toIntOrNull() ?: return null

        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "x", "×", "*", "X" -> a * b
            "÷", "/" -> if (b != 0) a / b else null
            else -> null
        }
    }

    fun looksLikeExpression(text: String): Boolean =
        EXPR_REGEX.containsMatchIn(text)

    fun cleanOcrText(text: String): String {
        return text
            .replace('t', '+', ignoreCase = true)
            .filter { it.isDigit() || it in "+-x×÷*/X " }
            .trim()
    }
}
