package com.steamclock.feedbackt.lib

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.View
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider

import com.steamclock.feedbackt.R


/**
 * Feedbackt Singleton
 */
object Feedbackt {

    private val TAG = "Feedbackt"

    private val email = "shayla@steamclock.com"
    private val emailTitle = "Sending feedback"

    fun grabFeedback(activity: Activity) {
        Toast.makeText(activity, "Hello from Feedbackt2", Toast.LENGTH_SHORT).show()

        requestStoragePermissions(activity)

        activity.window?.decorView?.rootView?.let { rootView ->
            generateAndSendScreenshot(activity, rootView)
        } ?: run {
            // Problem.
        }
    }

    // -------------------------
    private fun generateAndSendScreenshot(context: Context, view: View) {
        val bitmap = view.convertToBitmap()
        saveBitmapToFile(context, bitmap)?.let { uri ->
            Log.v(TAG, "generateAndSendScreenshot, with uri: $uri")
            emailBitmap(context, uri)
            //viewBitmap(context, uri)
        } ?: run {
            Log.e(TAG, "generateAndSendScreenshot failed")
            // todo error
        }
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
        val path = Environment.getExternalStorageDirectory().toString()
        val file = File(path, "feedbackt.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            Log.e(TAG, "saveBitmapToFile failed")
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

    private fun emailBitmap(context: Context, uri: Uri) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "image/png"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitle)
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri)

        emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(emailIntent, "Send email"))
    }

    private fun viewBitmap(context: Context, uri: Uri) {
        val viewIntent = Intent()
        viewIntent.action = Intent.ACTION_VIEW
        viewIntent.setDataAndType(uri, "image/*")
        viewIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(viewIntent, "View Screenshot"))
    }

    private fun requestStoragePermissions(context: Activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(context, arrayOf (Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }
}