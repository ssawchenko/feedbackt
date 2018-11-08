package com.steamclock.feedbackt.lib.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity;
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.lib.Constants
import com.steamclock.feedbackt.lib.Feedbackt

import kotlinx.android.synthetic.main.activity_edit_feedbackt.*
import kotlinx.android.synthetic.main.content_edit_feedbackt.*
import java.lang.Exception

class EditFeedbacktActivity : AppCompatActivity() {

    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_feedbackt)
        setSupportActionBar(toolbar)

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
    }

    private fun sendEdited() {
        Feedbackt.grabFeedbackAndEmail(this)
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
