package com.steamclock.feedbacktsample.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.steamclock.feedbacktsample.R
import com.steamclock.feedbacktsample.lib.Feedbackt

import kotlinx.android.synthetic.main.activity_main.*
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val speedDialView = findViewById<SpeedDialView>(R.id.speedDial)
        speedDialView.addActionItem(SpeedDialActionItem.Builder(R.id.fab_edit, R.drawable.baseline_edit_white_48)
            .setLabel("Edit")
            .setLabelColor(resources.getColor(android.R.color.white))
            .setLabelBackgroundColor(resources.getColor(R.color.colorAccent))
            .create())
        speedDialView.addActionItem(SpeedDialActionItem.Builder(R.id.fab_email, R.drawable.baseline_email_white_48)
            .setLabel("Email")
            .setLabelBackgroundColor(resources.getColor(R.color.colorAccent))
            .setLabelColor(resources.getColor(android.R.color.white))
            .create())

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
                else -> true
            }
        }

    }



//    .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.material_white_1000, getTheme()))
//    .setFabImageTintColor(ResourcesCompat.getColor(getResources(), R.color.inbox_primary, getTheme()))
//    .setLabel(getString(R.string.label_custom_color))
//    .setLabelColor(Color.WHITE)
//    .setLabelBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.inbox_primary, getTheme()))
//    .setLabelClickable(false)
}
