package com.quick36.autosolver

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Core automation service.
 *
 * Flow per screen-change event:
 *   1. Filter to only the target app's package.
 *   2. Try to read the question text straight from the accessibility node tree
 *      (fast path — no OCR needed if the text is a real Android view).
 *   3. Parse the expression and solve it (pure int math, negligible cost).
 *   4. Tap the digit buttons (node click preferred, coordinate gesture fallback).
 *   5. Dedupe against the last-seen question so repeated events for the same
 *      question don't cause duplicate/garbage taps.
 *
 * IMPORTANT — before this works against the real app you must:
 *   - Set the correct target package name below (TARGET_PACKAGE) and in
 *     accessibility_service_config.xml (android:packageNames).
 *   - Set the correct view id in QUESTION_VIEW_ID if you found one via
 *     `adb shell uiautomator dump`. If the text isn't in the tree at all,
 *     wire up the OCR fallback path (see onAccessibilityEvent below).
 */
class SolverAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "Quick36AutoSolver"

        // TODO: replace with the real Quick36 package name (adb shell dumpsys window | grep mCurrentFocus)
        private const val TARGET_PACKAGE = "com.quick36.app"

        // TODO: replace with the real resource-id of the question text view, if one exists
        private const val QUESTION_VIEW_ID = "com.quick36.app:id/question_text"
    }

    private lateinit var gestureHelper: GestureHelper
    private val ocrHelper = OcrHelper()
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var lastQuestion: String? = null
    private var lastAnswerTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        gestureHelper = GestureHelper(this)
        ocrHelper.warmUp()
        Log.d(TAG, "Service connected and warmed up")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != TARGET_PACKAGE) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val root = rootInActiveWindow ?: return

        // ---- Fast path: node tree ----
        val question = findQuestionInNodeTree(root)
        if (question != null) {
            handleQuestion(question, root)
            return
        }

        // ---- Fallback path: OCR ----
        // Only wire this up if the fast path above returns null for your target app,
        // i.e. the question text is custom-drawn (Canvas/Compose/game engine) and not
        // exposed as a real accessibility node. Requires MediaProjection screen capture
        // set up separately (see README for the permission-request flow) — omitted here
        // since it needs a foreground activity to request the capture intent once.
        // Sketch:
        // val bitmap = latestScreenCaptureBitmap ?: return
        // val cropped = ocrHelper.cropToQuestionRegion(bitmap)
        // ocrHelper.recognize(cropped) { rawText ->
        //     val cleaned = rawText?.let { ExpressionParser.cleanOcrText(it) } ?: return@recognize
        //     handleQuestion(cleaned, root)
        // }
    }

    private fun handleQuestion(question: String, root: AccessibilityNodeInfo) {
        // Dedupe: the same content-changed event can fire multiple times for one
        // screen update. Skip if we already answered this exact question recently.
        val now = System.currentTimeMillis()
        if (question == lastQuestion && now - lastAnswerTime < 800) return

        val answer = ExpressionParser.solve(question) ?: run {
            Log.w(TAG, "Could not parse expression from: \"$question\"")
            return
        }

        lastQuestion = question
        lastAnswerTime = now

        serviceScope.launch {
            gestureHelper.submitAnswer(answer, root)
            Log.d(TAG, "Q: \"$question\" -> A: $answer")
        }
    }

    /** Looks for the question text via known view id first, then a full-tree regex scan. */
    private fun findQuestionInNodeTree(root: AccessibilityNodeInfo): String? {
        // 1) Try the known resource id, if you found one via uiautomator dump.
        root.findAccessibilityNodeInfosByViewId(QUESTION_VIEW_ID)?.firstOrNull()?.text?.let {
            return it.toString()
        }

        // 2) Fallback: walk the whole tree looking for any node whose text matches
        //    a simple math expression pattern.
        return searchTreeForExpression(root)
    }

    private fun searchTreeForExpression(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        val text = node.text?.toString()
        if (text != null && ExpressionParser.solve(text) != null) {
            return text
        }
        for (i in 0 until node.childCount) {
            val result = searchTreeForExpression(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
}
