package com.quick36.autosolver

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SolverAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "Quick36AutoSolver"
        private const val TARGET_PACKAGE = "ch.quick36.quick36"
        private const val QUESTION_VIEW_ID = "ch.quick36.quick36:id/question_text"
    }

    private lateinit var gestureHelper: GestureHelper
    private val ocrHelper = OcrHelper()
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var lastQuestion: String? = null
    private var lastAnswerTime = 0L
    private var answering = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        gestureHelper = GestureHelper(this)
        ocrHelper.warmUp()
        Log.d(TAG, "Service connected and warmed up")
        AutomationState.update(status = "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventPackage = event.packageName?.toString() ?: return

        // Always track last seen package so the overlay can show it for debugging
        AutomationState.update(seenPackage = eventPackage)

        if (eventPackage != TARGET_PACKAGE) return
        if (!AutomationState.isActive) {
            AutomationState.update(status = "Paused (tap floating button to start)")
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return
        if (answering) return

        val root = rootInActiveWindow ?: return
        try {
            val question = findQuestionInNodeTree(root) ?: return
            handleQuestion(question, root)
        } catch (t: Throwable) {
            Log.e(TAG, "Error in onAccessibilityEvent", t)
            AutomationState.update(status = "Error: ${t.message}")
        }
    }

    private fun handleQuestion(question: String, root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (question == lastQuestion && now - lastAnswerTime < 1200) return

        val answer = ExpressionParser.solve(question) ?: run {
            Log.w(TAG, "Could not parse: \"$question\"")
            AutomationState.update(question = question, status = "Found text but could not parse")
            return
        }

        lastQuestion = question
        lastAnswerTime = now
        answering = true

        AutomationState.update(
            question = question,
            answer = answer.toString(),
            status = "Answering $question = $answer"
        )

        serviceScope.launch {
            try {
                val liveRoot = rootInActiveWindow
                gestureHelper.submitAnswer(answer, liveRoot)
                Log.d(TAG, "Q: \"$question\" -> A: $answer")
            } finally {
                android.os.Handler(mainLooper).postDelayed({
                    answering = false
                    AutomationState.update(status = "Ready")
                }, 900L)
            }
        }
    }

    private fun findQuestionInNodeTree(root: AccessibilityNodeInfo): String? {
        root.findAccessibilityNodeInfosByViewId(QUESTION_VIEW_ID)?.firstOrNull()?.text?.let {
            return it.toString()
        }
        return searchTreeForExpression(root)
    }

    private fun searchTreeForExpression(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        node.text?.toString()?.let { t ->
            val cleaned = t.trim()
            if (cleaned.isNotEmpty() && ExpressionParser.solve(cleaned) != null) return cleaned
        }
        node.contentDescription?.toString()?.let { t ->
            val cleaned = t.trim()
            if (cleaned.isNotEmpty() && ExpressionParser.solve(cleaned) != null) return cleaned
        }
        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null }
            val result = searchTreeForExpression(child)
            if (result != null) return result
        }
        return null
    }

    override fun onInterrupt() {
        answering = false
        Log.d(TAG, "Service interrupted")
    }
}
