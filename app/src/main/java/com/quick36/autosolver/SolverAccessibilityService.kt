package com.quick36.autosolver

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Watches Quick36 for math questions and auto-taps the answer.
 */
class SolverAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "Quick36AutoSolver"
        private const val TARGET_PACKAGE = "ch.quick36.quick36"
        // Soft dedupe window — same question within this many ms is ignored
        private const val DEDUPE_MS = 1200L
    }

    private lateinit var gestureHelper: GestureHelper
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var lastQuestion: String? = null
    private var lastAnswerTime = 0L
    private var answering = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        gestureHelper = GestureHelper(this)
        Log.i(TAG, "Service connected — watching $TARGET_PACKAGE")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != TARGET_PACKAGE) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> Unit
            else -> return
        }

        if (answering) return

        val root = rootInActiveWindow ?: return
        try {
            val question = findQuestionInNodeTree(root) ?: return
            handleQuestion(question, root)
        } catch (t: Throwable) {
            Log.e(TAG, "onAccessibilityEvent error", t)
        }
    }

    private fun handleQuestion(question: String, root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (question == lastQuestion && now - lastAnswerTime < DEDUPE_MS) return

        val answer = ExpressionParser.solve(question) ?: run {
            Log.d(TAG, "No parseable expression in: '$question'")
            return
        }

        lastQuestion = question
        lastAnswerTime = now
        answering = true

        Log.i(TAG, "SOLVE  $question  =>  $answer")

        serviceScope.launch {
            try {
                // Re-fetch root in case the tree changed
                val liveRoot = rootInActiveWindow
                gestureHelper.submitAnswer(answer, liveRoot)
            } finally {
                // Allow next question after taps have had time to finish
                android.os.Handler(mainLooper).postDelayed({
                    answering = false
                }, 800L)
            }
        }
    }

    /** Walk full tree looking for any text that parses as a + - x / expression. */
    private fun findQuestionInNodeTree(root: AccessibilityNodeInfo): String? {
        return searchTreeForExpression(root)
    }

    private fun searchTreeForExpression(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        // Check text
        node.text?.toString()?.let { t ->
            val cleaned = t.trim()
            if (cleaned.isNotEmpty() && ExpressionParser.solve(cleaned) != null) {
                return cleaned
            }
        }

        // Check contentDescription (some games put the question here)
        node.contentDescription?.toString()?.let { t ->
            val cleaned = t.trim()
            if (cleaned.isNotEmpty() && ExpressionParser.solve(cleaned) != null) {
                return cleaned
            }
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null }
            val result = searchTreeForExpression(child)
            if (result != null) return result
        }
        return null
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
        answering = false
    }
}
