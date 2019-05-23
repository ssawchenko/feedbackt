package com.steamclock.feedbackt.customcanvas.actions

import android.graphics.Canvas
import com.steamclock.feedbackt.customcanvas.CanvasProxy

abstract class CanvasAction(val canvasProxy: CanvasProxy) {
    // Proxied touch interactions
    open fun onTouchStart(x: Float, y: Float) {}
    open fun onTouchMove(x: Float, y: Float) {}
    open fun onTouchUp(canvas: Canvas) {}

    // Proxied stack interactions
    open fun undo(keep: Boolean = true) {}
    open fun redo() {}
    open fun clearRedo() {}
    open fun clearAll() {}

    // Proxied canvas interactions
    open fun draw(canvas: Canvas) {}

    // Any special content that this action wants to the email that will be
    // generated when feebackt is sent/shared.
    open fun emailContent(): String? { return null }
}