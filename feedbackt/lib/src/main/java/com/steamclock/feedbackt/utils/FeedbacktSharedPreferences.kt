package com.steamclock.feedbackt.utils

import android.content.Context

class FeedbacktSharedPreferences(val context: Context) {
    private val reader = context.getSharedPreferences("FeedbacktSharedPrefs", Context.MODE_PRIVATE)
    private val writer = reader.edit()

    // Store the API url currently active.
    private val hasShownDetailsDialogKey = "hasShownDetailsDialog"
    var hasShownDetailsDialog: Boolean
        get() = reader.getBoolean(hasShownDetailsDialogKey, false)
        set(value) {
            writer.putBoolean(hasShownDetailsDialogKey, value).commit()
        }
}