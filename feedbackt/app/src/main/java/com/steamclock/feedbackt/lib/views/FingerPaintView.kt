package com.steamclock.feedbackt.lib.views

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.util.AttributeSet
import android.view.View
import java.lang.Exception
import java.util.*

class FingerPaintView @JvmOverloads constructor(context: Context,
                                          attrs: AttributeSet? = null,
                                          defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    //private var mPath: Path
    private var nextPath: Path? = null
    private val mBitmapPaint: Paint
    private var mPaint: Paint

    private var paths = LinkedList<Path>()
    private var redoPaths = LinkedList<Path>()

    private var mX: Float = 0.toFloat()
    private var mY: Float = 0.toFloat()

    init {
        mPaint = Paint()
        mPaint.setAntiAlias(true)
        mPaint.setDither(true)
        mPaint.setColor(-0x10000)
        mPaint.setStyle(Paint.Style.STROKE)
        mPaint.setStrokeJoin(Paint.Join.ROUND)
        mPaint.setStrokeCap(Paint.Cap.ROUND)
        mPaint.setStrokeWidth(12f)

        //mPath = Path()

        mBitmapPaint = Paint(Paint.DITHER_FLAG)

        setWillNotDraw(false)
    }

    fun undo() {
        try {
            redoPaths.add(paths.removeLast())
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }

        invalidate()
    }

    fun redo() {
        try {
            paths.add(redoPaths.removeLast())
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
    }

    private fun generateFullPath(): Path {
        val result = Path()
        paths.forEach { result.addPath(it) }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        //canvas.drawColor(-0x555556)
        canvas.drawColor(Color.TRANSPARENT)
        //canvas.drawBitmap(mBitmap, 0f, 0f, mBitmapPaint)
        //canvas.drawPath(mPath, mPaint)
        canvas.drawPath(generateFullPath(), mPaint)
    }

    private fun onTouchStart(x: Float, y: Float) {
        nextPath = Path()
        nextPath?.let {
            it.reset()
            it.moveTo(x, y)
            mX = x
            mY = y
            paths.add(it)
        }
    }

    private fun onTouchMove(x: Float, y: Float) {
        nextPath?.let {
            val dx = Math.abs(x - mX)
            val dy = Math.abs(y - mY)
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                it.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                mX = x
                mY = y
            }
            updateNextPath()
        }
    }

    // left off trying to force a canvas redraw.

    private fun onTouchUp() {
        nextPath?.let {
            it.lineTo(mX, mY)
            // commit the path to our offscreen
            mCanvas!!.drawPath(it, mPaint)
            // kill this so we don't double draw
            //it.reset()
            updateNextPath()
        }
        nextPath = null
    }

    private fun updateNextPath() {
        nextPath?.let {
            paths.removeLast()
            paths.add(it)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                redoPaths.clear() // Nuke redo stack
                onTouchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                onTouchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                onTouchUp()
                invalidate()
            }
        }
        return true
    }

    companion object {
        private val MINP = 0.25f
        private val MAXP = 0.75f
        private val TOUCH_TOLERANCE = 4f
    }
}