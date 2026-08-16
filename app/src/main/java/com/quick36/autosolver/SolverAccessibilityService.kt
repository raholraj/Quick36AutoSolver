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
        AutomationState.update(status = "Service connected")
        Log.i(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        AutomationState.update(seenPackage = pkg)

        val target = AutomationState.targetPackage
        val inTarget = pkg == target
        AutomationState.update(inTargetApp = inTarget)

        if (!inTarget) return
        if (!AutomationState.isActive) {
            AutomationState.update(status = "Paused — tap floating button")
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) return
        if (answering) return

        val root = rootInActiveWindow ?: return
        try {
            val question = findQuestionInNodeTree(root) ?: return
            handleQuestion(question, root)
        } catch (t: Throwable) {
            Log.e(TAG, "event error", t)
            AutomationState.update(status = "Error: ${t.message}")
        }
    }

    private fun handleQuestion(question: String, root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (question == lastQuestion && now - lastAnswerTime < 1200) return

        val answer = ExpressionParser.solve(question) ?: run {
            AutomationState.update(question = question, status = "Could not parse")
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
                gestureHelper.submitAnswer(answer, rootInActiveWindow)
            } finally {
                android.os.Handler(mainLooper).postDelayed({
                    answering = false
                    AutomationState.update(status = "Ready")
                }, 900L)
            }
        }
    }

    private fun findQuestionInNodeTree(root: AccessibilityNodeInfo): String? =
        searchTree(root)

    private fun searchTree(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        node.text?.toString()?.trim()?.let {
            if (it.isNotEmpty() && ExpressionParser.solve(it) != null) return it
        }
        node.contentDescription?.toString()?.trim()?.let {
            if (it.isNotEmpty() && ExpressionParser.solve(it) != null) return it
        }
        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null }
            searchTree(child)?.let { return it }
        }
        return null
    }

    override fun onInterrupt() {
        answering = false
    }
}
