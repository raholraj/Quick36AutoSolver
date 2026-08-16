package com.quick36.autosolver

/**
 * Simple shared state so the floating overlay (OverlayService) and the
 * background automation (SolverAccessibilityService) can talk to each other
 * without a full event bus. Not persisted — resets when the process dies,
 * which is fine since "active" should default to OFF on a fresh start.
 */
object AutomationState {

    /** Master switch. Automation only taps things when this is true. */
    @Volatile
    var isActive: Boolean = false

    /** Last package name seen by the accessibility service — lets you confirm
     *  the real package name of Quick36 live, without adb. */
    @Volatile
    var lastSeenPackage: String = "-"

    @Volatile
    var lastQuestion: String = "-"

    @Volatile
    var lastAnswer: String = "-"

    @Volatile
    var lastStatus: String = "Waiting..."

    /** Overlay registers itself here when visible, so the accessibility
     *  service can push live updates to it. Set back to null in onDestroy. */
    @Volatile
    var listener: StateListener? = null

    interface StateListener {
        fun onStateUpdated()
    }

    fun update(
        seenPackage: String? = null,
        question: String? = null,
        answer: String? = null,
        status: String? = null
    ) {
        seenPackage?.let { lastSeenPackage = it }
        question?.let { lastQuestion = it }
        answer?.let { lastAnswer = it }
        status?.let { lastStatus = it }
        listener?.onStateUpdated()
    }
}
