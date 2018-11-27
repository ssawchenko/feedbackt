package com.steamclock.feedbackt.customcanvas

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.steamclock.feedbackt.customcanvas.actions.CanvasAction
import com.steamclock.feedbackt.customcanvas.actions.FusedAction
import com.steamclock.feedbackt.customcanvas.actions.NumberedAction
import com.steamclock.feedbackt.customcanvas.actions.PathAction
import java.lang.Exception
import java.util.*

class CustomCanvasView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    enum class Mode {
        None,
        Drawing,            // Lines only
        NumberedBullets,    // Numbers only
        Fused               // Path and numbers together
    }

    private var canvasUndoActions = LinkedList<CanvasAction>()
    private var canvasRedoActions = LinkedList<CanvasAction>()

    private var pathActions: PathAction
    private var numberedAction: NumberedAction
    private var fusedAction: FusedAction

    private var activeAction: CanvasAction? = null

    private var canvas: Canvas? = null
    private var bitmap: Bitmap? = null
    private val bitmapPaint: Paint

    var mode: Mode = Mode.Drawing
        set(value) {
            field = value
            activeAction = when(field) {
                Mode.None -> null
                Mode.Drawing -> pathActions
                Mode.NumberedBullets -> numberedAction
                Mode.Fused -> fusedAction
            }
        }

    /**
     * Allows actions to indicate when they are considered "complete".
     */
    private var canvasProxy = object: CanvasProxy {
        override fun addAction(action: CanvasAction) {
            log("Adding ${action.javaClass.name} to action list")
            canvasUndoActions.add(action)
        }
    }

    init {
        pathActions = PathAction(canvasProxy)
        numberedAction = NumberedAction(context, canvasProxy)
        fusedAction = FusedAction(context, canvasProxy)

        bitmapPaint = Paint(Paint.DITHER_FLAG)
        setWillNotDraw(false)

        // Setup default drawing mode
        mode = Mode.Fused
    }

    fun undo() {
        try {
            val lastAction = canvasUndoActions.removeLast()
            log("Calling undo for ${lastAction.javaClass.name}")
            lastAction.undo()
            canvasRedoActions.add(lastAction)
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }

        invalidate()
    }

    fun redo() {
        try {
            val lastAction = canvasRedoActions.removeLast()
            log("Calling redo for ${lastAction.javaClass.name}")
            lastAction.redo()
            canvasUndoActions.add(lastAction)
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT)
        drawAll(canvas)
    }

    private fun onTouchStart(x: Float, y: Float) {
        activeAction?.onTouchStart(x, y)
    }

    private fun onTouchMove(x: Float, y: Float) {
        activeAction?.onTouchMove(x, y)
    }

    private fun onTouchUp() {
        activeAction?.onTouchUp(canvas!!)
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

    private fun drawAll(canvas: Canvas) {
        pathActions.draw(canvas)
        numberedAction.draw(canvas)
        fusedAction.draw(canvas)
    }

    private fun clearRedo() {
        log("Calling clearRedo")
        // Clear global stack
        canvasRedoActions.clear()

        // Clear action stacks
        pathActions.clearRedo()
        numberedAction.clearRedo()
        fusedAction.clearRedo()
    }

    companion object {
        private const val TAG = "CustomCanvasView"

        fun log(message: String) {
            Log.v(TAG, message)
        }
    }
}