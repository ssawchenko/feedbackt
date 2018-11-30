package com.steamclock.feedbackt.customcanvas.actions

import android.content.Context
import android.graphics.*
import com.steamclock.feedbackt.customcanvas.CanvasProxy
import java.lang.StringBuilder
import java.util.*

/**
 * Fuses PathAction and NumberedAction together; if user doesn't move their touch within a
 * certain threshold, then a number is "dropped". If the user moves their finger past the
 * threshold, a path is drawn.
 */
class FusedAction(context: Context, canvasProxy: CanvasProxy): CanvasAction(canvasProxy) {

    private var pathAction = PathAction(canvasProxy)
    private var numberedAction = NumberedAction(context, canvasProxy)

    private var fusedUndoActions = LinkedList<CanvasAction>()
    private var fusedRedoActions = LinkedList<CanvasAction>()

    private var firstTouchX: Float = 0.toFloat()
    private var firstTouchY: Float = 0.toFloat()

    private var lastTouchX: Float = 0.toFloat()
    private var lastTouchY: Float = 0.toFloat()

    private var lineThreshold = 10
    private var drawingPath = false

    //-------------------------------------------
    // CanvasAction
    //-------------------------------------------
    override fun onTouchStart(x: Float, y: Float) {
        firstTouchX = x
        lastTouchX = x

        firstTouchY = y
        lastTouchY = y
    }

    private fun reachedThreshold(start: Float, end: Float): Boolean {
        return Math.abs(start - end) > lineThreshold
    }

    override fun onTouchMove(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y

        when {
            drawingPath -> pathAction.onTouchMove(x, y)
            reachedThreshold(firstTouchX, lastTouchX) || reachedThreshold(firstTouchY, lastTouchY) -> {
                drawingPath = true
                pathAction.onTouchStart(firstTouchX, firstTouchY)
                pathAction.onTouchMove(lastTouchX, lastTouchY)
            }
        }
    }

    override fun onTouchUp(canvas: Canvas) {
        when {
            drawingPath -> {
                pathAction.onTouchUp(canvas)
                fusedUndoActions.add(pathAction)
                drawingPath = false
            }
            else -> {
                numberedAction.onTouchStart(firstTouchX, firstTouchY)
                numberedAction.onTouchUp(canvas)
                fusedUndoActions.add(numberedAction)
            }
        }

        canvasProxy.addAction(this)
    }

    override fun draw(canvas: Canvas) {
        // Draw path under numbers
        pathAction.draw(canvas)
        numberedAction.draw(canvas)
    }

    override fun undo(keep: Boolean) {
        try {
            val last = fusedUndoActions.removeLast()
            if (keep) { fusedRedoActions.add(last) }
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }
    }

    override fun redo() {
        try {
            fusedUndoActions.add(fusedRedoActions.removeLast())
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }
    }

    override fun clearRedo() {
        fusedRedoActions.clear()
        pathAction.clearRedo()
        numberedAction.clearRedo()
    }

    override fun clearAll() {
        pathAction.clearAll()
        numberedAction.clearAll()
    }

    override fun emailContent(): String? {
        val pathContent = pathAction.emailContent()
        val numbersContent = numberedAction.emailContent()

        return when {
            numbersContent != null && pathContent != null -> "$numbersContent\n\n$pathContent"
            numbersContent != null -> numbersContent
            pathContent != null -> pathContent
            else -> null
        }
    }
}