package com.quick36.autosolver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Taps digits + confirm. Node-click first, coordinate fallback second.
 * Adds small delays between taps so the game registers each press.
 */
class GestureHelper(private val service: AccessibilityService) {

    private val handler = Handler(Looper.getMainLooper())

    // Numpad fractions of screen (calibrated for typical phone portrait).
    // If taps miss, dump bounds with: adb shell uiautomator dump
    private val digitLayout: Map<Int, Pair<Float, Float>> = mapOf(
        1 to (0.20f to 0.665f), 2 to (0.50f to 0.665f), 3 to (0.80f to 0.665f),
        4 to (0.20f to 0.755f), 5 to (0.50f to 0.755f), 6 to (0.80f to 0.755f),
        7 to (0.20f to 0.845f), 8 to (0.50f to 0.845f), 9 to (0.80f to 0.845f),
        0 to (0.50f to 0.935f)
    )
    private val checkmarkPercent = 0.80f to 0.935f

    private fun screenSize(): Pair<Int, Int> {
        val dm = DisplayMetrics()
        val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return dm.widthPixels to dm.heightPixels
    }

    private fun tryNodeClick(root: AccessibilityNodeInfo?, text: String): Boolean {
        if (root == null) return false
        val matches = root.findAccessibilityNodeInfosByText(text) ?: return false
        for (node in matches) {
            var target: AccessibilityNodeInfo? = node
            while (target != null && !target.isClickable) {
                target = target.parent
            }
            if (target != null && target.isClickable) {
                if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
            // Also try ACTION_CLICK on the text node itself
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        return false
    }

    private fun tapAtPercent(xPercent: Float, yPercent: Float) {
        val (w, h) = screenSize()
        val x = w * xPercent
        val y = h * yPercent
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 40))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    fun tapDigit(digit: Int, root: AccessibilityNodeInfo?) {
        if (!tryNodeClick(root, digit.toString())) {
            val coords = digitLayout[digit] ?: return
            tapAtPercent(coords.first, coords.second)
        }
    }

    fun tapCheckmark(root: AccessibilityNodeInfo?) {
        if (!tryNodeClick(root, "✓") &&
            !tryNodeClick(root, "✔") &&
            !tryNodeClick(root, "check") &&
            !tryNodeClick(root, "OK") &&
            !tryNodeClick(root, "Enter")
        ) {
            tapAtPercent(checkmarkPercent.first, checkmarkPercent.second)
        }
    }

    /**
     * Types each digit with ~80ms gap, then taps confirm after a short pause.
     * Runs on main looper so gestures are sequenced correctly.
     */
    fun submitAnswer(answer: Int, root: AccessibilityNodeInfo?) {
        val digits = answer.toString().filter { it.isDigit() }.map { it.digitToInt() }
        if (digits.isEmpty()) return

        fun tapNext(index: Int) {
            if (index < digits.size) {
                tapDigit(digits[index], root)
                handler.postDelayed({ tapNext(index + 1) }, 90L)
            } else {
                handler.postDelayed({ tapCheckmark(root) }, 120L)
            }
        }
        // Small initial delay so the UI has settled after the question appeared
        handler.postDelayed({ tapNext(0) }, 60L)
    }
}
