package com.steamclock.feedbackt.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File

class ExternalStorage {
    companion object {
        /**
         * Checks if external storage is available for read and write
         */
        fun isExternalStorageWritable(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }

        /**
         * Checks if external storage is available to at least read
         */
        fun isExternalStorageReadable(): Boolean {
            return Environment.getExternalStorageState() in setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
        }

        /**
         * Force ACTION_MEDIA_SCANNER_SCAN_FILE intent to make the image viewable in gallery immediately.
         */
        fun forceMediaScanOfFile(context: Context, file: File) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                //val f = File("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
                val contentUri = Uri.fromFile(file)
                mediaScanIntent.data = contentUri
                context.sendBroadcast(mediaScanIntent)
            } else {
                context.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse("file://" + Environment.getExternalStorageDirectory())
                    )
                )
            }
        }
    }
}