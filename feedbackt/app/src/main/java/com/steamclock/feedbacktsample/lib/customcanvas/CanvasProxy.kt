package com.steamclock.feedbacktsample.lib.customcanvas

import com.steamclock.feedbacktsample.lib.customcanvas.actions.CanvasAction

interface CanvasProxy {
    fun addAction(action: CanvasAction)
}