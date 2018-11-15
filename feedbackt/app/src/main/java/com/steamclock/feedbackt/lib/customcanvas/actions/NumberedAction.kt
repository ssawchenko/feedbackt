package com.steamclock.feedbackt.lib.customcanvas.actions

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.lib.customcanvas.CanvasProxy
import com.steamclock.feedbackt.lib.extensions.*
import kotlinx.android.synthetic.main.view_numbered_action.view.*
import java.util.*

class NumberedAction(context: Context, canvasProxy: CanvasProxy, lineColor: Int = Color.RED): CanvasAction(canvasProxy) {

    private var nextPath: Path? = null
    private var undoItems = LinkedList<Path>()
    private var redoItems = LinkedList<Path>()

    private var lastTouchX: Float = 0.toFloat()
    private var lastTouchY: Float = 0.toFloat()

    private var nextNum = 1

    private var paint: Paint = Paint()
    private val numberedViewSize = 20.px
    private val numberedView: View =
        LayoutInflater.from(context).inflate(R.layout.view_numbered_action, null)


    private var testBitmap : Bitmap? = null


    init {
        paint.isAntiAlias = true
        paint.isDither = true
        paint.isFilterBitmap = true

        // Force size
        val numberedViewParams = RelativeLayout.LayoutParams(numberedViewSize, numberedViewSize)
        numberedView.layoutParams = numberedViewParams
    }

    //-------------------------------------------
    // CanvasAction
    //-------------------------------------------
    override fun onTouchStart(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
    }

    override fun onTouchMove(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
    }

    override fun onTouchUp(canvas: Canvas) {
        val nextBitmap = createNextBitmap()

        testBitmap = nextBitmap
       // canvas.drawBitmap(nextBitmap, lastTouchX, lastTouchY, null)
    }

    override fun draw(canvas: Canvas) {


        testBitmap?.let {
            canvas.drawBitmap(testBitmap, lastTouchX, lastTouchY, null)
        }


    }

    override fun undo(keep: Boolean) {
        try {
            val last = undoItems.removeLast()
            if (keep) { redoItems.add(last) }
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }
    }

    override fun redo() {
        try {
            undoItems.add(redoItems.removeLast())
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }
    }

    override fun clearRedo() {
        redoItems.clear()
    }

    //-------------------------------------------
    // Private
    //-------------------------------------------
    private fun createNextBitmap(): Bitmap {
        numberedView.action_number.text = nextNum.toString()
        nextNum++
        numberedView.prepForBitmapConversion()
        return numberedView.convertToBitmap()
    }

}