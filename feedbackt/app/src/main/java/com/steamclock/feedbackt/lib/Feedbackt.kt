package com.steamclock.feedbackt.lib

import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View


/**
 * Feedbackt Singleton
 */
object Feedbackt {

    private val email = "shayla@steamclock.com"


    fun grabFeedback(activity: Activity) {
        Toast.makeText(activity, "Hello from Feedbackt2", Toast.LENGTH_SHORT).show()

        activity.window?.decorView?.rootView?.let {

        } ?: run {
            // Problem.
        }
    }

    // -------------------------
    private fun generateBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}