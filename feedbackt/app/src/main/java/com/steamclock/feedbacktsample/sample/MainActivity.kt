package com.steamclock.feedbacktsample.sample

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.steamclock.feedbacktsample.R
import kotlinx.android.synthetic.main.activity_main.*
import com.steamclock.feedbackt.Feedbackt
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.view_feedbackt_options.view.*

class MainActivity : AppCompatActivity() {

    var settingsDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val speedDialView = findViewById<SpeedDialView>(R.id.speedDial)
        addSpeedDialItem(speedDialView, R.id.fab_edit, R.drawable.ic_edit_white_48dp, "Edit")
        addSpeedDialItem(speedDialView, R.id.fab_email, R.drawable.ic_email_white_48dp, "Email")
        addSpeedDialItem(speedDialView, R.id.fab_settings, R.drawable.ic_settings_white_48dp, "Settings")
        addSpeedDialItem(speedDialView, R.id.fab_start_stop_recording,R.drawable.ic_undo_white_48dp, "Start/Stop Video")

        speedDialView.setOnActionSelectedListener { speedDialActionItem ->
            when (speedDialActionItem.id) {
                R.id.fab_email -> {
                    Feedbackt.grabFeedbackAndEmail(this)
                    false
                }
                R.id.fab_edit -> {
                    Feedbackt.grabFeedbackAndEdit(this)
                    false
                }
                R.id.fab_settings -> {
                    showSettingsDialog()
                    false
                }
                R.id.fab_start_stop_recording -> {
                    Feedbackt.startStopCapture(this)
                    false
                }
                else -> true
            }
        }

        next_page_button.setOnClickListener {
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            startActivity(intent)
        }

    }

    private fun addSpeedDialItem(speedDialView: SpeedDialView, itemId: Int, itemDrawableId: Int, label: String) {
        speedDialView.addActionItem(SpeedDialActionItem.Builder(itemId, itemDrawableId)
            .setLabel(label)
            .setLabelColor(ContextCompat.getColor(this, android.R.color.white))
            .setLabelBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
            .create())
    }

    override fun onPause() {
        super.onPause()
        settingsDialog?.dismiss()
    }

    private fun showSettingsDialog() {
        val alertBuilder = AlertDialog.Builder(this)
        val dialogView = View.inflate(this, R.layout.view_feedbackt_options, null)
        alertBuilder.setView(dialogView)

        settingsDialog?.dismiss()
        settingsDialog = alertBuilder.create()
        settingsDialog?.let { dialog ->

            dialogView.send_to_email.setText(Feedbackt.email)
            dialogView.send_to_email.onTextChanged { text -> Feedbackt.email = text }

            dialogView.email_title.setText(Feedbackt.emailTitle)
            dialogView.email_title.onTextChanged { text -> Feedbackt.emailTitle = text }

            dialogView.email_content.setText(Feedbackt.emailContent)
            dialogView.email_content.onTextChanged { text -> Feedbackt.emailContent = text }

            dialogView.add_device_info.isChecked = Feedbackt.addDeviceInfo
            dialogView.add_device_info.setOnCheckedChangeListener { _, isChecked -> Feedbackt.addDeviceInfo = isChecked }

            dialogView.add_edit_action_info.isChecked = Feedbackt.addActionContent
            dialogView.add_edit_action_info.setOnCheckedChangeListener { _, isChecked -> Feedbackt.addActionContent = isChecked }

            dialog.setCanceledOnTouchOutside(true)
            dialog.show()

        }
    }

    fun EditText.onTextChanged(onTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { onTextChanged(p0.toString())}
            override fun afterTextChanged(editable: Editable?) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Feedbackt.onActivityResult(requestCode, resultCode, data)
    }
}
