package com.steamclock.feedbackt.lib.customcanvas.actions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.steamclock.feedbackt.lib.customcanvas.CanvasProxy
import java.lang.Exception
import java.util.*

class PathAction(canvasProxy: CanvasProxy): CanvasAction(canvasProxy) {

    private var nextPath: Path? = null
    private var undoPaths = LinkedList<Path>()
    private var redoPaths = LinkedList<Path>()

    private var paint: Paint = Paint()
    private var lastTouchX: Float = 0.toFloat()
    private var lastTouchY: Float = 0.toFloat()

    init {
        paint.setAntiAlias(true)
        paint.setDither(true)
        paint.setColor(-0x10000)
        paint.setStyle(Paint.Style.STROKE)
        paint.setStrokeJoin(Paint.Join.ROUND)
        paint.setStrokeCap(Paint.Cap.ROUND)
        paint.setStrokeWidth(12f)
    }


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
            canvas!!.drawPath(it, paint)
            // kill this so we don't double draw
            //it.reset()
            updateNextPath()
            canvasProxy.addAction(this)
        }
        nextPath = null
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(generateFullPath(), paint)
    }

    private fun updateNextPath() {
        nextPath?.let {
            undo(false)
            add(it)
        }
    }

    fun add(path: Path) {
        undoPaths.add(path)
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

    private fun generateFullPath(): Path {
        val result = Path()
        undoPaths.forEach { result.addPath(it) }
        return result
    }

    companion object {
        private val MINP = 0.25f
        private val MAXP = 0.75f
        private val TOUCH_TOLERANCE = 4f
    }
}