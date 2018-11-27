package com.steamclock.feedbackt.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.steamclock.feedbackt.Constants
import com.steamclock.feedbackt.Feedbackt
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.customcanvas.CustomCanvasView
import kotlinx.android.synthetic.main.activity_edit_feedbackt.*
import kotlinx.android.synthetic.main.content_edit_feedbackt.*
import androidx.constraintlayout.solver.widgets.WidgetContainer.getBounds
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.widget.ImageView


class EditFeedbacktActivity : AppCompatActivity() {

    private var photoUri: Uri? = null
    private var height: Int? = null
    private var width: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_feedbackt)
        setSupportActionBar(toolbar)

        // Resize canvases so that the user cannot "draw outside"
        original_image.post {

            val bounds = getImageBounds(original_image)

            //val boundsWidth = original_image.measuredHeight
            //val boundsHeight = original_image.measuredWidth
            val boundsWidth = bounds.width().toInt()
            val boundsHeight = bounds.height().toInt()

            // Once we have sized our original image, force the parent to use same layout.
            // If we don't do this the bitmap conversion appears to fail.
            val editedImageLayout = edited_image_layout.layoutParams
            editedImageLayout.height = boundsHeight
            editedImageLayout.width = boundsWidth
            edited_image_layout.layoutParams = editedImageLayout

            // Force canvas to be same size as edited image so the user cannot "draw off" the page.
            val customCanvasLayout = custom_canvas_view.layoutParams
            customCanvasLayout.height = boundsHeight
            customCanvasLayout.width = boundsWidth
            custom_canvas_view.layoutParams = customCanvasLayout

            // todo height/width may no longer be needed.
            height = original_image.measuredHeight
            width = original_image.measuredWidth
        }

        // Setup canvas
        try {
            photoUri = Uri.parse(intent.getStringExtra(Constants.EXTRA_BITMAP_URI))
            original_image.setImageURI(photoUri)
            setupCanvas()
        } catch (e: Exception) {
            // todo show error.
            showError()
        }
    }

    private fun getImageBounds(imageView: ImageView): RectF {
        val bounds = RectF()
        imageView.drawable?.let { imageView.imageMatrix.mapRect(bounds, RectF(it.bounds)) }
        return bounds
    }

    private fun showError() {
        bottom_actions.visibility = View.GONE
    }

    private fun setupCanvas() {
        custom_canvas_view.mode = CustomCanvasView.Mode.Fused
        bottom_actions.visibility = View.VISIBLE
        send_it_button.setOnClickListener { sendEdited() }
        undo_button.setOnClickListener { custom_canvas_view.undo() }
        redo_button.setOnClickListener { custom_canvas_view.redo() }
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
