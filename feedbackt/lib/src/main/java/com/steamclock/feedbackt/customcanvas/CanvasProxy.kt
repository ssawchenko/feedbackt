package com.steamclock.feedbackt.customcanvas

import com.steamclock.feedbackt.customcanvas.actions.CanvasAction

interface CanvasProxy {
    fun addAction(action: CanvasAction)
}