package com.steamclock.feedbackt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.steamclock.feedbackt.R
import com.steamclock.feedbackt.lib.Feedbackt

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { _ -> getFeedbackt() }
    }

    private fun getFeedbackt() {
        Feedbackt.grabFeedback(this)
    }
}
