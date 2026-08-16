package com.quick36.autosolver

/**
 * Parses math expressions and solves them.
 * Supports: +, -, x/X/*, /
 * Examples: "4 + 4", "12 x 7", "9 / 3", "8 * 6", "15 - 3", "36 / 4"
 */
object ExpressionParser {

    // Matches: digit(s), optional spaces, operator, optional spaces, digit(s)
    // Operators: + - x X * /
    private val EXPR_REGEX = Regex("""(\d+)\s*([+\-xX*/])\s*(\d+)""")

    /**
     * Extracts the first valid math expression from [raw] and returns the integer result.
     * Returns null if no expression found, operator unsupported, or division by zero.
     */
    fun solve(raw: String): Int? {
        // Normalize common unicode operators first
        val cleaned = raw
            .replace('\u00d7', 'x')  // ×
            .replace('\u00f7', '/')  // ÷
            .trim()
        val match = EXPR_REGEX.find(cleaned) ?: return null
        val (aStr, op, bStr) = match.destructured
        val a = aStr.toIntOrNull() ?: return null
        val b = bStr.toIntOrNull() ?: return null
        return when (op) {
            "+"          -> a + b
            "-"          -> a - b
            "x", "X", "*" -> a * b
            "/"          -> if (b != 0) a / b else null
            else         -> null
        }
    }

    /**
     * Cleans OCR noise conservatively — only correct clear OCR character mistakes.
     */
    fun cleanOcrText(text: String): String {
        return text
            .replace('\u00d7', 'x')
            .replace('\u00f7', '/')
            .replace('X', 'x')
            // Keep only digits, operators, and spaces
            .filter { it.isDigit() || it in "+-x*/ " }
            .trim()
    }
}
