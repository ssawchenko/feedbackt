package com.steamclock.feedbackt.lib.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity;
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.lib.Constants

import kotlinx.android.synthetic.main.activity_edit_feedbackt.*
import kotlinx.android.synthetic.main.content_edit_feedbackt.*
import java.lang.Exception
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.steamclock.feedbackt.lib.Feedbackt


class EditFeedbacktActivity : AppCompatActivity() {

    private var photoUri: Uri? = null
    private var height: Int? = null
    private var width: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_feedbackt)
        setSupportActionBar(toolbar)

        original_image.post {
            // Once we have sized our original image, force the parent to use same layout.
            val editedImageLayout = edited_image_layout.layoutParams
            editedImageLayout.height = original_image.measuredHeight
            editedImageLayout.width = original_image.measuredWidth
            edited_image_layout.layoutParams = editedImageLayout

            val fingerpaintLayout = fingerpaint_view.layoutParams
            fingerpaintLayout.height = original_image.measuredHeight
            fingerpaintLayout.width = original_image.measuredWidth
            fingerpaint_view.layoutParams = fingerpaintLayout

            height = original_image.measuredHeight
            width = original_image.measuredWidth
        }

        try {
            photoUri = Uri.parse(intent.getStringExtra(Constants.EXTRA_BITMAP_URI))
            original_image.setImageURI(photoUri)
            setupCanvas()
        } catch (e: Exception) {
            // todo show error.
            showError()
        }
    }

    private fun showError() {
        bottom_actions.visibility = View.GONE
    }

    private fun setupCanvas() {
        bottom_actions.visibility = View.VISIBLE
        send_it_button.setOnClickListener { sendEdited() }
        undo_button.setOnClickListener { fingerpaint_view.undoLast() }
    }

    private fun sendEdited() {
        val view = findViewById<View>(R.id.edited_image_layout)
        //Feedbackt.grabFeedbackAndEmail(this, view)
        sendBitmapWithKnownSize(edited_image_layout, width ?: 0, height ?: 0)
    }

    /**
     * Problem, View.convertToBitmap() throwing an exception - indicating that the width and
     * height of the edited_image_layout is 0, even though we have already gone through a measure
     * pass. For now, this appears to be a work around.
     */
    private fun sendBitmapWithKnownSize (v: View, width: Int, height: Int) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        v.draw(canvas)
        //return bitmap
        Feedbackt.emailBitmap(this, bitmap)
    }

    //---------------------------------------------------
    // Companion
    //---------------------------------------------------
    companion object {
        fun newIntent(context: Context, uri: Uri): Intent {
            val intent = Intent(context, EditFeedbacktActivity::class.java)
            intent.putExtra(Constants.EXTRA_BITMAP_URI, uri.toString())
            return intent
        }
    }
}
