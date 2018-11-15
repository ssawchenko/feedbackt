package com.steamclock.feedbackt.lib.customcanvas.actions

import android.graphics.Canvas
import com.steamclock.feedbackt.lib.customcanvas.CanvasProxy

abstract class CanvasAction(val canvasProxy: CanvasProxy) {
    // Proxied touch interactions
    open fun onTouchStart(x: Float, y: Float) {}
    open fun onTouchMove(x: Float, y: Float) {}
    open fun onTouchUp(canvas: Canvas) {}

    // Proxied stack interactions
    open fun undo(keep: Boolean = true) {}
    open fun redo() {}
    open fun clearRedo() {}

    // Proxied canvas interactions
    open fun draw(canvas: Canvas) {}
}