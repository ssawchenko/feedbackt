package com.steamclock.feedbacktsample.lib.customcanvas.actions

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.steamclock.feedbacktsample.lib.customcanvas.CanvasProxy
import java.lang.Exception
import java.util.*

class PathAction(canvasProxy: CanvasProxy, lineColor: Int = Color.RED): CanvasAction(canvasProxy) {

    private var nextPath: Path? = null
    private var undoPaths = LinkedList<Path>()
    private var redoPaths = LinkedList<Path>()

    private var paint: Paint = Paint()
    private var lastTouchX: Float = 0.toFloat()
    private var lastTouchY: Float = 0.toFloat()

    init {
        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = lineColor
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 12f
    }

    //-------------------------------------------
    // CanvasAction
    //-------------------------------------------
    override fun onTouchStart(x: Float, y: Float) {
        nextPath = Path()
        nextPath?.let {
            it.reset()
            it.moveTo(x, y)
            lastTouchX = x
            lastTouchY = y
            undoPaths.add(it)
        }
    }

    override fun onTouchMove(x: Float, y: Float) {
        nextPath?.let {
            val dx = Math.abs(x - lastTouchX)
            val dy = Math.abs(y - lastTouchY)
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                it.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                lastTouchX = x
                lastTouchY = y
            }
            updateNextPath()
        }
    }

    override fun onTouchUp(canvas: Canvas) {
        nextPath?.let {
            it.lineTo(lastTouchX, lastTouchY)
            // commit the path to our offscreen
            canvas.drawPath(it, paint)
            // kill this so we don't double draw
            //it.reset()
            updateNextPath()
            canvasProxy.addAction(this) // Action considered complete.
        }
        nextPath = null
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(generateFullPath(), paint)
    }

    override fun undo(keep: Boolean) {
        try {
            val last = undoPaths.removeLast()
            if (keep) { redoPaths.add(last) }
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }
    }

    override fun redo() {
        try {
            undoPaths.add(redoPaths.removeLast())
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }
    }

    override fun clearRedo() {
        redoPaths.clear()
    }

    //-------------------------------------------
    // Private
    //-------------------------------------------
    private fun generateFullPath(): Path {
        val result = Path()
        undoPaths.forEach { result.addPath(it) }
        return result
    }

    private fun updateNextPath() {
        nextPath?.let {
            undo(false)
            undoPaths.add(it)
        }
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }
}