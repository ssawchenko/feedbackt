package com.steamclock.feedbacktsample.lib.extensions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.steamclock.feedbacktsample.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

fun Bitmap.saveAsPng(context: Context, filename: String): Uri? {
    val path = Environment.getExternalStorageDirectory().toString()
    val file = File(path, filename)

    try {
        val stream: OutputStream = FileOutputStream(file)
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }

    // Supporting Nougat and up, we need to run our URIs through a FileProvider.
    val providerAuth = context.getString(R.string.file_provider_authority)
    val resultUri = FileProvider.getUriForFile(context, providerAuth, file)

    // Return the saved image path to uri
    return resultUri

    // Old method Return the saved image path to uri
    //return Uri.parse(file.absolutePath)
}