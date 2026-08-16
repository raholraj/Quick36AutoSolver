package com.quick36.autosolver

object AutomationState {

    @Volatile var isActive: Boolean = false
    @Volatile var targetPackage: String = "ch.quick36.quick36"
    @Volatile var lastSeenPackage: String = "-"
    @Volatile var isInTargetApp: Boolean = false
    @Volatile var lastQuestion: String = "-"
    @Volatile var lastAnswer: String = "-"
    @Volatile var lastStatus: String = "Waiting..."
    @Volatile var lastClickInfo: String = "-"
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
