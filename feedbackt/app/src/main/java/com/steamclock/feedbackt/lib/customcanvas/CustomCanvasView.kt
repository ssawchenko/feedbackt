package com.steamclock.feedbackt.lib.customcanvas

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.util.AttributeSet
import android.view.View
import com.steamclock.feedbackt.lib.customcanvas.actions.CanvasAction
import com.steamclock.feedbackt.lib.customcanvas.actions.PathAction
import java.lang.Exception
import java.util.*

class CustomCanvasView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var canvasActions = LinkedList<CanvasAction>()
    private lateinit var pathActions: PathAction

    private var mBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    //private var mPath: Path
    //private var nextPath: Path? = null
    private val mBitmapPaint: Paint
//    private var mPaint: Paint
//
//    private var mX: Float = 0.toFloat()
//    private var mY: Float = 0.toFloat()

    private var canvasProxy = object: CanvasProxy {
        override fun addAction(action: CanvasAction) {
            canvasActions.add(action)
        }
    }

    init {
//        mPaint = Paint()
//        mPaint.setAntiAlias(true)
//        mPaint.setDither(true)
//        mPaint.setColor(-0x10000)
//        mPaint.setStyle(Paint.Style.STROKE)
//        mPaint.setStrokeJoin(Paint.Join.ROUND)
//        mPaint.setStrokeCap(Paint.Cap.ROUND)
//        mPaint.setStrokeWidth(12f)

        pathActions = PathAction(canvasProxy)

        //mPath = Path()
        mBitmapPaint = Paint(Paint.DITHER_FLAG)

        setWillNotDraw(false)
    }

    fun undo() {
        try {
            val lastAction = canvasActions.removeLast()
            lastAction.undo()
            canvasActions.add(lastAction)
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }

        invalidate()
    }

    fun redo() {
        try {
            val lastAction = canvasActions.removeLast()
            lastAction.redo()
            canvasActions.add(lastAction)
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        //canvas.drawColor(-0x555556)
        canvas.drawColor(Color.TRANSPARENT)
        //canvas.drawBitmap(mBitmap, 0f, 0f, mBitmapPaint)
        //canvas.drawPath(mPath, mPaint)
        pathActions.draw(canvas)
    }

    private fun onTouchStart(x: Float, y: Float) {
        pathActions.onTouchStart(x, y)
    }

    private fun onTouchMove(x: Float, y: Float) {
        pathActions.onTouchMove(x, y)
    }

    // left off trying to force a canvas redraw.

    private fun onTouchUp() {
        pathActions.onTouchUp(canvas!!)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                clearRedo()
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

    private fun clearRedo() {
        // Clear global stack
        canvasActions.clear()

        // Clear action stacks
        pathActions.clearRedo()
    }
}