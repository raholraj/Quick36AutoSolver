package com.quick36.autosolver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

/**
 * Handles tapping digit buttons and the confirm button.
 *
 * Priority:
 *  1. Accessibility node ACTION_CLICK  — most reliable, instant.
 *  2. Coordinate tap via dispatchGesture — fallback for canvas-drawn UIs.
 *
 * All entry points are suspend functions so callers can add inter-tap delays
 * without blocking the main thread.
 */
class GestureHelper(private val service: AccessibilityService) {

    private val digitLayout: Map<Int, Pair<Float, Float>> = mapOf(
        1 to (0.20f to 0.665f), 2 to (0.50f to 0.665f), 3 to (0.80f to 0.665f),
        4 to (0.20f to 0.755f), 5 to (0.50f to 0.755f), 6 to (0.80f to 0.755f),
        7 to (0.20f to 0.845f), 8 to (0.50f to 0.845f), 9 to (0.80f to 0.845f),
        0 to (0.50f to 0.935f)
    )
    private val checkmarkPercent = 0.80f to 0.935f
    private val backspacePercent = 0.20f to 0.935f

    private fun screenSize(): Pair<Int, Int> {
        val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
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
                val success = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) AutomationState.update(clickInfo = "Node: \"$text\"")
                return success
            }
        }
        return false
    }

    private fun tapAtPercent(xPercent: Float, yPercent: Float, label: String) {
        val (w, h) = screenSize()
        val x = (w * xPercent).toInt()
        val y = (h * yPercent).toInt()
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        service.dispatchGesture(gesture, null, null)
        AutomationState.update(clickInfo = "Gesture: $label → ($x, $y)")
    }

    suspend fun tapDigit(digit: Int, root: AccessibilityNodeInfo?) {
        if (!tryNodeClick(root, digit.toString())) {
            val coords = digitLayout[digit] ?: return
            tapAtPercent(coords.first, coords.second, "digit $digit")
        }
        delay(180)
    }

    suspend fun tapCheckmark(root: AccessibilityNodeInfo?) {
        val hit = tryNodeClick(root, "✓") ||
                  tryNodeClick(root, "check") ||
                  tryNodeClick(root, "OK") ||
                  tryNodeClick(root, "Submit")
        if (!hit) {
            tapAtPercent(checkmarkPercent.first, checkmarkPercent.second, "✓ confirm")
        }
        delay(250)
    }

    suspend fun tapBackspace(root: AccessibilityNodeInfo?) {
        if (!tryNodeClick(root, "⌫") && !tryNodeClick(root, "del")) {
            tapAtPercent(backspacePercent.first, backspacePercent.second, "⌫ backspace")
        }
        delay(150)
    }

    suspend fun submitAnswer(answer: Int, root: AccessibilityNodeInfo?) {
        val digits = kotlin.math.abs(answer).toString()
        for (ch in digits) {
            if (ch.isDigit()) {
                tapDigit(ch.digitToInt(), root)
            }
        }
        delay(100)
        tapCheckmark(root)
    }
}
