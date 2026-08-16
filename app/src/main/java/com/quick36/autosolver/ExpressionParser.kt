package com.quick36.autosolver

object ExpressionParser {

    private val EXPR_REGEX = Regex("""(\d+)\s*([+\-×÷xX*/])\s*(\d+)""")

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

    fun cleanOcrText(text: String): String {
        return text
            .replace("t", "+", ignoreCase = true)
            .filter { it.isDigit() || it in "+-x×÷*/X " }
            .trim()
    }
}
