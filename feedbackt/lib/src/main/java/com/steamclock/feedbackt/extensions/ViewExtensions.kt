package com.steamclock.feedbackt.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.steamclock.feedbackt.Feedbackt

/**
 * Must be called on UI Thread.
 */
fun View.prepForBitmapConversion() {
    // If we already have a measured size, do not force a re-measure
    if (measuredHeight != 0 && measuredWidth != 0) return

    // Else, force a measure on the view so that we have a measuredHeight and measuredWidth
    // for convertToBitmap
    val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(measureSpec, measureSpec)
    layout(0, 0, measuredWidth, measuredHeight)
}

fun View.convertToBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.TRANSPARENT)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

fun View.convertToBitmapWithKnownSize(knownWidth: Int, knownHeight: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(knownWidth, knownHeight, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.TRANSPARENT)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

/**
 * Note, does not check to make sure device supports numTouches number of pointers.
 */
fun View.setOnXLongPress(numTouches: Int, timeoutMs: Long, callback: () -> Unit) {

    val onTimeout = Runnable {
        Log.v(Feedbackt.TAG, "setOnXLongPress firing callback on event")
        callback()
    }

    setOnTouchListener { _, event ->
        Log.v(Feedbackt.TAG, "setOnTouchListener touch event")
        val action = event.action and MotionEvent.ACTION_MASK
        val pointerCount = event.pointerCount

        // Only trigger callback on the pointer down to avoid running logic twice
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            if (pointerCount == numTouches) {
                Log.v(Feedbackt.TAG, "setOnXLongPress starting timer")
                postDelayed(onTimeout, timeoutMs)

            } else {
                removeCallbacks(onTimeout)
            }
        }

        false
    }
}