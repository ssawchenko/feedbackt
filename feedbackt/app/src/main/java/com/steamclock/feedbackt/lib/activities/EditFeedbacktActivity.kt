package com.steamclock.feedbackt.lib.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.lib.Constants
import com.steamclock.feedbackt.lib.Feedbackt
import kotlinx.android.synthetic.main.activity_edit_feedbackt.*
import kotlinx.android.synthetic.main.content_edit_feedbackt.*


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
            // If we don't do this the bitmap conversion appears to fail.
            val editedImageLayout = edited_image_layout.layoutParams
            editedImageLayout.height = original_image.measuredHeight
            editedImageLayout.width = original_image.measuredWidth
            edited_image_layout.layoutParams = editedImageLayout

            // Force canvas to be same size as edited image
            val fingerpaintLayout = fingerpaint_view.layoutParams
            fingerpaintLayout.height = original_image.measuredHeight
            fingerpaintLayout.width = original_image.measuredWidth
            fingerpaint_view.layoutParams = fingerpaintLayout

            // todo height/width may no longer be needed.
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
        undo_button.setOnClickListener { fingerpaint_view.undo() }
        redo_button.setOnClickListener { fingerpaint_view.redo() }
    }

    private fun sendEdited() {
        val view = findViewById<View>(R.id.edited_image_layout)
        Feedbackt.grabFeedbackAndEmail(this, view)
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
