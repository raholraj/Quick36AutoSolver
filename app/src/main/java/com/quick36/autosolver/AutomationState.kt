package com.quick36.autosolver

/**
 * Shared state bridge between OverlayService (UI) and SolverAccessibilityService (automation).
 * All fields are @Volatile so reads/writes across threads stay consistent.
 */
object AutomationState {

    /** Master on/off switch — nothing taps unless this is true. */
    @Volatile var isActive: Boolean = false

    /** The package we are targeting. User can change this live from the overlay. */
    @Volatile var targetPackage: String = "ch.quick36.quick36"

    /** Last package seen on screen (so user can confirm/correct the target package). */
    @Volatile var lastSeenPackage: String = "-"

    /** True when the target app is currently the foreground app. */
    @Volatile var isInTargetApp: Boolean = false

    @Volatile var lastQuestion: String = "-"
    @Volatile var lastAnswer: String = "-"
    @Volatile var lastStatus: String = "Waiting..."

    /** Where the solver just tapped — shown in the overlay so user can see activity. */
    @Volatile var lastClickInfo: String = "-"

    /** Overlay registers here so the accessibility service can push live updates. */
    @Volatile var listener: StateListener? = null

    interface StateListener {
        fun onStateUpdated()
    }

    fun update(
        seenPackage: String? = null,
        question: String? = null,
        answer: String? = null,
        status: String? = null,
        clickInfo: String? = null,
        inTargetApp: Boolean? = null
    ) {
        seenPackage?.let { lastSeenPackage = it }
        question?.let { lastQuestion = it }
        answer?.let { lastAnswer = it }
        status?.let { lastStatus = it }
        clickInfo?.let { lastClickInfo = it }
        inTargetApp?.let { isInTargetApp = it }
        listener?.onStateUpdated()
    }
}
