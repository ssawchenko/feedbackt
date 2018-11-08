package com.steamclock.feedbackt.lib

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.Toast
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.steamclock.feedbackt.lib.activities.EditFeedbacktActivity
import com.steamclock.feedbackt.lib.extensions.convertToBitmap

import com.steamclock.feedbackt.lib.extensions.saveAsPng


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
            grabFeedbackAndEdit(activity, rootView)
        } ?: run {
            // Problem.
        }
    }

    fun grabFeedbackAndEmail(context: Context, view: View) {
        grabFeedbackAndRun(context, view, ::emailBitmap)
    }

    fun grabFeedbackAndEdit(context: Context, view: View) {
        grabFeedbackAndRun(context, view, ::launchEdit)
    }

    fun grabFeedbackAndView(context: Context, view: View) {
        grabFeedbackAndRun(context, view, ::viewBitmap)
    }

    private fun grabFeedbackAndRun(context: Context, view: View, runThis: (context: Context, uri: Uri) -> Unit) {
        val bitmap = view.convertToBitmap()
        bitmap.saveAsPng(context, "feedbackt.png")?.let { uri ->
            runThis(context, uri)
        } ?: run {
            Log.e(TAG, "generateAndSendScreenshot failed")
            // todo error
        }
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

    private fun launchEdit(context: Context, uri: Uri) {
        val launchIntent = EditFeedbacktActivity.newIntent(context, uri)
        context.startActivity(launchIntent)
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