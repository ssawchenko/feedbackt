package com.steamclock.feedbackt.lib.customcanvas

import com.steamclock.feedbackt.lib.customcanvas.actions.CanvasAction

interface CanvasProxy {
    fun addAction(action: CanvasAction)
}