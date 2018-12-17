package com.steamclock.feedbackt.activities

import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.steamclock.feedbackt.Constants
import com.steamclock.feedbackt.Feedbackt
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.customcanvas.CustomCanvasView
import com.steamclock.feedbackt.utils.FeedbacktSharedPreferences
import kotlinx.android.synthetic.main.activity_edit_feedbackt.*
import kotlinx.android.synthetic.main.content_edit_feedbackt.*
import java.text.SimpleDateFormat
import java.util.*


class EditFeedbacktActivity : AppCompatActivity() {

    private var photoUri: Uri? = null
    private var height: Int? = null
    private var width: Int? = null

    private var dialog: AlertDialog? = null
    private lateinit var preferences: FeedbacktSharedPreferences
    private var shareMethodBottomSheet: EditFeedbackShareBottomSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_feedbackt)
        setSupportActionBar(toolbar)
        preferences = FeedbacktSharedPreferences(this)

        // Resize canvases once the original_image has scaled itself...
        original_image.post {
            // Grab the image bounds to determine scaled width and height.
            val bounds = getImageBounds(original_image)
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
            custom_canvas_view.mode = (intent.getSerializableExtra(Constants.EXTRA_CUSTOM_CANVAS_MODE) ?: EditFeedbacktActivity.defaultMode) as CustomCanvasView.Mode
            original_image.setImageURI(photoUri)
            setupCanvas()
        } catch (e: Exception) {
            // todo show error.
            showError()
        }

        // Setup undo/redo button listeners
        custom_canvas_view.canvasListener = object: CustomCanvasView.CanvasListener {
            override fun onUndoValueChange(count: Int) {
                updateUndoRedoButtonByCount(undo_button_layout, count)
            }
            override fun onRedoValueChange(count: Int) {
                updateUndoRedoButtonByCount(redo_button_layout, count)
            }
        }

        if (!preferences.hasShownDetailsDialog) showEditInstructions()
    }

    override fun onPause() {
        super.onPause()
        dialog?.dismiss()
    }

    private fun updateUndoRedoButtonByCount(buttonLayout: View, count: Int) {
        buttonLayout.isEnabled = count > 0
        buttonLayout.alpha = if (count > 0) 1.0f else 0.6f
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
        undo_button_layout.setOnClickListener { custom_canvas_view.undo() }
        redo_button_layout.setOnClickListener { custom_canvas_view.redo() }
        clear_button_layout.setOnClickListener { custom_canvas_view.clear() }
    }

    private fun getFileName(): String {
        val now = Calendar.getInstance().time
        var fileName = "Feedbackt_"

        fileName += try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            dateFormat.format(now)
        } catch (e: java.lang.Exception) {
            now.toString()
        }

        return fileName
    }

    private fun sendEdited() {
        shareMethodBottomSheet?.dismiss()
        shareMethodBottomSheet = EditFeedbackShareBottomSheet.newInstance()
        shareMethodBottomSheet?.methodSelectionListener = shareMethodSelectionListener
        shareMethodBottomSheet?.show(supportFragmentManager, "ShareMethodBottomSheetFragment")
    }

    private val shareMethodSelectionListener = object: EditFeedbackShareBottomSheet.MethodSelectionListener {
        override fun onSaveToGallery() {
            Feedbackt.grabFeedbackAndSave(this@EditFeedbacktActivity, edited_image_layout, getFileName())
        }

        override fun onEmail() {
            Feedbackt.grabFeedbackAndEmail(this@EditFeedbacktActivity, edited_image_layout, custom_canvas_view.getEmailContent())
        }
    }

    private fun showEditInstructions() {
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this).create()
        dialog?.let { dialog ->
            dialog.setTitle("How to edit your Feedbackt")
            dialog.setMessage("* Tap to drop a numbered bullet\n* Draw to highlight specific areas\n* 'Send it' and add details in your email")
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
                preferences.hasShownDetailsDialog = true
            }
            dialog.show()
        }
    }

    //---------------------------------------------------
    // Companion
    //---------------------------------------------------
    companion object {
        val defaultMode = CustomCanvasView.Mode.Fused

        fun newIntent(context: Context, uri: Uri, mode: CustomCanvasView.Mode = EditFeedbacktActivity.defaultMode): Intent {
            val intent = Intent(context, EditFeedbacktActivity::class.java)
            intent.putExtra(Constants.EXTRA_BITMAP_URI, uri.toString())
            intent.putExtra(Constants.EXTRA_CUSTOM_CANVAS_MODE, mode)
            return intent
        }
    }
}
