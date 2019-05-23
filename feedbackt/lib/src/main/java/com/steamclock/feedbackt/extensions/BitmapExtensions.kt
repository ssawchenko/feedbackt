package com.steamclock.feedbackt.extensions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import com.steamclock.feedbackt.utils.ExternalStorage
import com.steamclock.feedbackt.utils.FeedbacktFileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun Bitmap.saveAsPng(context: Context, file: File): Uri? {
    try {
        val stream: OutputStream = FileOutputStream(file)
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    // Supporting Nougat and up, we need to run our URIs through a FileProvider.
    val resultUri = FeedbacktFileProvider().getUriForFile(context, file)

    // Return the saved image path to uri
    return resultUri
}

/**
 * filename should not include .png
 */
fun Bitmap.saveAsPrivatePng(context: Context, filename: String): Uri? {
    val path = Environment.getExternalStorageDirectory().toString()
    val file = File(path, "$filename.png")
    return this.saveAsPng(context, file)
}

/**
 * filename should not include .png
 */
fun Bitmap.saveAsPublicPng(context: Context, filename: String): Uri? {
    // Get the directory for the user's public pictures directory.
    val rootImageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val feedbacktAlbumDir = File(rootImageDir, "Feedbackt")

    val saveDirectory = if (feedbacktAlbumDir.exists() || feedbacktAlbumDir.mkdirs()) {
        feedbacktAlbumDir
    } else {
        rootImageDir
    }

    // Save file according to what directory is available to us.
    val file = File(saveDirectory, "$filename.png")
    val uri = this.saveAsPng(context, file)
    // todo may want to add meta data via context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ...)

    // Force ACTION_MEDIA_SCANNER_SCAN_FILE intent to make the image viewable in gallery immediately.
    ExternalStorage.forceMediaScanOfFile(context, file)

    return uri
}



