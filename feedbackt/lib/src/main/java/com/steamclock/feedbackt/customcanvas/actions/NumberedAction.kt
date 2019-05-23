package com.steamclock.feedbackt.customcanvas.actions

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.customcanvas.CanvasProxy
import com.steamclock.feedbackt.extensions.*
import kotlinx.android.synthetic.main.view_numbered_action.view.*
import java.lang.StringBuilder
import java.util.*

class NumberedAction(context: Context, canvasProxy: CanvasProxy, lineColor: Int = Color.RED): CanvasAction(canvasProxy) {

    data class PlacedBitmap(val bitmap: Bitmap, var x: Float, var y: Float)

    private var undoItems = LinkedList<PlacedBitmap>()
    private var redoItems = LinkedList<PlacedBitmap>()

    private var lastTouchX: Float = 0.toFloat()
    private var lastTouchY: Float = 0.toFloat()

    private var nextNum = 1
    private var paint: Paint = Paint()
    private val numberedViewSize = 30.px
    private val placementOffset = numberedViewSize / 2
    private val numberedView: View = LayoutInflater.from(context).inflate(R.layout.view_numbered_action, null)

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
        // Indicate to the canvas that we are complete.
        val startBitmap = createNextBitmap()
        undoItems.add(PlacedBitmap(startBitmap, lastTouchX, lastTouchY))
        canvasProxy.addAction(this)
    }

    override fun draw(canvas: Canvas) {
        undoItems.forEach {
            canvas.drawBitmap(it.bitmap, it.x - placementOffset, it.y - placementOffset, null)
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
        nextNum = undoItems.size + 1
    }

    override fun clearAll() {
        undoItems.clear()
        redoItems.clear()
        nextNum = 1
    }

    override fun emailContent(): String? {
        val numberOfBullets = undoItems.count()
        if (numberOfBullets == 0) {
            return null
        }

        val contentBuilder = StringBuilder()
        contentBuilder.appendln("--- Details by Number ---")
        for (i in 1..numberOfBullets) {
            contentBuilder.appendln("$i: ")
        }

        return contentBuilder.toString()
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

    private fun updateNextBitmap(x: Float, y: Float) {
        try {
            undoItems.last().let {
                it.x = x
                it.y = y
            }
        } catch (e: Exception) {
            // shhhhhhhh, it's ok.
        }
    }

}