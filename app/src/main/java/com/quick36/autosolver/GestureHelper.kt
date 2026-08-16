package com.quick36.autosolver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Handles tapping digit buttons and the checkmark/confirm button.
 *
 * Priority order:
 *  1. Try to find the button as an accessibility node and call performAction(ACTION_CLICK).
 *     This is the most reliable and lowest-latency path (no gesture dispatch overhead).
 *  2. Fall back to a coordinate-based tap (dispatchGesture) using a percentage-based
 *     keypad map, so it still works across different screen resolutions.
 */
class GestureHelper(private val service: AccessibilityService) {

    // Percentage-based keypad layout, calibrated against the Quick36 numpad screenshot.
    // xPercent/yPercent are fractions of screen width/height (0.0 - 1.0).
    // Recalibrate these using `adb shell uiautomator dump` bounds if your device differs.
    private val digitLayout: Map<Int, Pair<Float, Float>> = mapOf(
        1 to (0.20f to 0.665f), 2 to (0.50f to 0.665f), 3 to (0.80f to 0.665f),
        4 to (0.20f to 0.755f), 5 to (0.50f to 0.755f), 6 to (0.80f to 0.755f),
        7 to (0.20f to 0.845f), 8 to (0.50f to 0.845f), 9 to (0.80f to 0.845f),
        0 to (0.50f to 0.935f)
    )
    private val checkmarkPercent = 0.80f to 0.935f
    private val backspacePercent = 0.20f to 0.935f

    private fun screenSize(): Pair<Int, Int> {
        val dm = DisplayMetrics()
        val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(dm)
        return (dm.widthPixels to dm.heightPixels)
    }

    /** Try node-based click first; returns true if it succeeded. */
    private fun tryNodeClick(root: AccessibilityNodeInfo?, text: String): Boolean {
        if (root == null) return false
        val matches = root.findAccessibilityNodeInfosByText(text) ?: return false
        for (node in matches) {
            var target: AccessibilityNodeInfo? = node
            // walk up to find a clickable ancestor if the text node itself isn't clickable
            while (target != null && !target.isClickable) {
                target = target.parent
            }
            if (target != null && target.isClickable) {
                return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    private fun tapAtPercent(xPercent: Float, yPercent: Float) {
        val (w, h) = screenSize()
        val x = w * xPercent
        val y = h * yPercent
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    fun tapDigit(digit: Int, root: AccessibilityNodeInfo?) {
        val handled = tryNodeClick(root, digit.toString())
        if (!handled) {
            val coords = digitLayout[digit] ?: return
            tapAtPercent(coords.first, coords.second)
        }
    }

    fun tapCheckmark(root: AccessibilityNodeInfo?) {
        val handled = tryNodeClick(root, "✓") || tryNodeClick(root, "check")
        if (!handled) {
            tapAtPercent(checkmarkPercent.first, checkmarkPercent.second)
        }
    }

    /** Types out every digit of [answer], then confirms. */
    fun submitAnswer(answer: Int, root: AccessibilityNodeInfo?) {
        val digits = if (answer < 0) "-$answer" else answer.toString()
        digits.forEach { ch ->
            if (ch.isDigit()) tapDigit(ch.digitToInt(), root)
        }
        tapCheckmark(root)
    }
}
