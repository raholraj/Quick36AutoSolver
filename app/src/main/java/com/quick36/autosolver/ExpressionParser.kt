package com.quick36.autosolver

/**
 * Parses a simple two-operand math expression like "4 + 4", "12 x 7", "9 - 3"
 * and returns the integer answer. Deliberately avoids any general-purpose
 * eval() — this is a tight regex + branch, which is effectively free
 * compared to the screen-reading step.
 */
object ExpressionParser {

    // Matches things like "4 + 4", "12x7", "9 ÷ 3", "8 * 6" with optional spaces
    private val EXPR_REGEX = Regex("""(\d+)\s*([+\-x×÷*/])\s*(\d+)""")

    /**
     * Extracts the first math expression found in [raw] and solves it.
     * Returns null if no valid expression / unsupported operator is found.
     */
    fun solve(raw: String): Int? {
        val match = EXPR_REGEX.find(raw) ?: return null
        val (aStr, op, bStr) = match.destructured
        val a = aStr.toIntOrNull() ?: return null
        val b = bStr.toIntOrNull() ?: return null
        return when (op) {
            "+", "+" -> a + b
            "-", "−" -> a - b
            "x", "×", "*" -> a * b
            "÷", "/" -> if (b != 0) a / b else null
            else -> null
        }
    }

    /** Quick check used by the service before doing any heavier work. */
    fun looksLikeExpression(text: String): Boolean =
        EXPR_REGEX.containsMatchIn(text)

    /** Light OCR post-processing (common misreads: "+" as "t" or "4" as "A"). */
    fun cleanOcrText(text: String): String {
        return text
            .replace("t", "+", ignoreCase = true)
            .replace("x", "x", ignoreCase = true)
            .filter { it.isDigit() || it in "+-x×÷*/ " }
            .trim()
    }
}
