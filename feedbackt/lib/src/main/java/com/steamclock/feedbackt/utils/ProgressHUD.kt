package com.steamclock.feedbackt.utils

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.steamclock.feedbackt.Feedbackt
import com.steamclock.feedbackt.R
import kotlinx.android.synthetic.main.layout_progress_hud.*

class ProgressHUD(context: Context) {

    private val dialog = ProgressDialog(context)

    fun show() {
        try {
            dialog.show()
        } catch (e: Exception) {
            // If activity no longer running, this could fire an exception.
            // May want to log this, but we do not want to crash.
        }
    }

    fun dismiss() {
        try {
            if (dialog.isShowing) dialog.dismiss()
        } catch (e: Exception) {
            // If activity no longer running, this could fire an exception.
            // May want to log this, but we do not want to crash.
        }
    }

    fun setText(text: String?) {
        Log.v(Feedbackt.TAG, "ProgressHUD text: $text")
        dialog.text = text
    }

    fun showSpinner(show: Boolean) {
        dialog.showSpinner = show
    }

    fun isShowing(): Boolean {
        return dialog.isShowing
    }

    private inner class ProgressDialog(context: Context) : Dialog(context) {
        var text: String? = null
            set(value) {
                field = value
                progress_hud_text?.text = value
            }

        var showSpinner: Boolean = true
            set(value) {
                field = value
                progress_hud_spinner?.visibility = if (value) View.VISIBLE else View.GONE
            }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.layout_progress_hud)

            window?.let { dialogWindow ->
                dialogWindow.setBackgroundDrawable(ColorDrawable(0))
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                val layoutParams = dialogWindow.attributes
                layoutParams.dimAmount = 0.3f
                layoutParams.gravity = Gravity.CENTER
                dialogWindow.attributes = layoutParams
                setCanceledOnTouchOutside(false)
            }

            text?.let {
                progress_hud_text?.text = text
                progress_hud_text?.visibility = View.VISIBLE
            } ?: run {
                progress_hud_text?.visibility = View.GONE
            }

            progress_hud_spinner?.visibility = if (showSpinner) View.VISIBLE else View.GONE
        }
    }

    companion object {
        fun create(context: Context): ProgressHUD { return ProgressHUD(context) }
    }
}