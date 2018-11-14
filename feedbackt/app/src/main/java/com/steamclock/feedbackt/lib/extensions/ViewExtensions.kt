package com.steamclock.feedbackt.lib.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View

/**
 * Must be called on UI Thread.
 */
fun View.prepForBitmapConversion() {
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