package com.steamclock.feedbackt.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class FeedbacktFileProvider: FileProvider() {
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.feedbackt.fileprovider",
            file
        )
    }
}