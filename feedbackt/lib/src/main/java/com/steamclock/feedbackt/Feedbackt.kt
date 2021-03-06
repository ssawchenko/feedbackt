package com.steamclock.feedbackt

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.steamclock.feedbackt.activities.EditFeedbacktActivity
import com.steamclock.feedbackt.utils.DoAsync
import com.steamclock.feedbackt.utils.ProgressHUD
import com.steamclock.feedbackt.utils.ShakeDetector
import java.lang.ref.WeakReference
import com.steamclock.feedbackt.utils.ExternalStorage
import com.steamclock.feedbackt.extensions.*


/**
 * Feedbackt Singleton
 */
object Feedbackt {

    const val TAG = "Feedbackt"
    private const val storedImageName = "feedbackt"
    
    var email: String? = null
    var emailTitle = "Sending feedback"
    var emailContent: String? = null
    var addDeviceInfo = true
    var addActionContent = true
    var editMode = EditFeedbacktActivity.defaultMode

    private var commonHud: WeakReference<ProgressHUD>? = null
    private var currentActivity: WeakReference<Activity>? = null

    private var shakeDetector: ShakeDetector? = null
    private var grabInProgress = false

    // Application wide detection
    private var autoDetectMultitouch = false
    private var autoDetetctShake = false

    private fun registerLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(applicationLifecycleListener)
    }

    private fun unregisterLifecycleCallbacksIfNoAutoDetectors(application: Application) {
        if (autoDetetctShake || autoDetectMultitouch) {
            return
        }

        application.unregisterActivityLifecycleCallbacks(applicationLifecycleListener)
    }

    //-------------------------------
    // Multitouch
    //-------------------------------
    fun enableMultiTouchToActivate(application: Application): Boolean {
        Log.v(TAG, "Feedback attempting to enableMultiTouchToActivate")
        val pm = application.packageManager
        when {
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND) -> {
                // 5 or more simultaneous independent pointers
            }
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT) -> {
                // 2 independent pointers

            }
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) -> {
                // 2 basic pointers
            }
            else -> {
                Log.e(TAG, "No multitouch support; Feedbackt cannot enableMultiTouchToActivate")
                // No multitouch support
                return false
            }
        }

        Log.v(TAG, "Feedback called registerActivityLifecycleCallbacks")
        autoDetectMultitouch = true
        registerLifecycleCallbacks(application)
        return true
    }

    fun disableMultiTouchToActivate(application: Application) {
        autoDetectMultitouch = false
        unregisterLifecycleCallbacksIfNoAutoDetectors(application)
    }

    fun enableMultiTouchToActivateOnActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
        Log.v(TAG, "enableMultiTouchToActivateOnActivity")

        getContentView(activity)?.let {
            Log.v(TAG, "rootView is ${it.javaClass.name}")
            it.setOnXLongPress(3, 1000) {
                if (activity.isFinishing) return@setOnXLongPress
                Feedbackt.grabFeedbackAndEdit(activity)
            }
        } ?: run {
            Log.e(TAG, "Could not get RootView for activity")
        }
    }

    fun disableMultiTouchToActivateOnActivity() {
        Log.v(TAG, "disableMultiTouchToActivateOnActivity")
        currentActivity?.get()?.let { getRootView(it)?.setOnTouchListener(null) }
        currentActivity = null
    }

    //-------------------------------
    // Shake
    //-------------------------------
    fun enableShakeToActivate(application: Application, activity: Activity? = null) {
        autoDetetctShake = true
        registerLifecycleCallbacks(application)
        if (activity != null) enableShakeToActivateOnActivity(activity)
    }

    fun disableShakeToActivate(application: Application) {
        autoDetetctShake = false
        unregisterLifecycleCallbacksIfNoAutoDetectors(application)
    }

    fun enableShakeToActivateOnActivity(activity: Activity) {
        shakeDetector = ShakeDetector(activity, object: ShakeDetector.ShakeListener {
            override fun onShakeDetected() {
                Feedbackt.grabFeedbackAndEdit(activity)
            }

            override fun onShakeNotSupported() {
                if (activity.isFinishing != true) return
                Toast.makeText(activity, "Accelerometer not supported on device; cannot launch Feedbackt by shake", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun disableShakeToActivateOnActivity() {
        shakeDetector?.stop()
        shakeDetector = null
    }

    //-------------------------------
    // Public
    //-------------------------------
    fun grabFeedbackAndEmail(activity: Activity, view: View? = getRootView(activity), text: String? = null) {
        grabFeedbackAndRun(activity, view, createEmailBitmapWithString(text))
    }

    fun grabFeedbackAndEdit(activity: Activity, view: View? = getRootView(activity)) {
        grabFeedbackAndRun(activity, view, ::launchEdit)
    }

    fun grabFeedbackAndView(activity: Activity, view: View? = getRootView(activity)) {
        grabFeedbackAndRun(activity, view, ::viewBitmap)
    }

    fun grabFeedbackAndSave(activity: Activity,
                            view: View? = getRootView(activity),
                            fileName: String,
                            onComplete: (Boolean) -> Unit) {
        grabFeedbackAndRun(activity, view, saveFileWithFileName(activity, fileName, onComplete))
    }

    fun showHud(activity: Activity, text: Any? = null, showSpinner: Boolean = true) {
        if (activity.isFinishing) return

        val textString = when(text) {
            is String -> text
            is Int -> activity.getString(text)
            else -> null
        }

        commonHud?.get()?.let {
            if (it.isShowing()) {
                it.setText(textString)
                it.showSpinner(showSpinner)
                return
            }
        }

        commonHud = WeakReference(ProgressHUD.create(activity))
        commonHud?.get()?.showSpinner(showSpinner)

        text?.let {
            if (it is String) {
                commonHud?.get()?.setText(it)
            }
            if (it is Int) {
                commonHud?.get()?.setText(activity.getString(it))
            }
        }
        commonHud?.get()?.show()
    }

    fun hideHud() {
        commonHud?.get()?.dismiss()
    }

    //-------------------------------
    // Private
    //-------------------------------
    private fun getRootView(activity: Activity): View? {
        return activity.window?.decorView?.rootView
    }

    private fun getContentView(activity: Activity): View? {
        return activity.findViewById(android.R.id.content)
    }

    /**
     * runThis is responsible for calling hideHud after all follow up actions have
     * been completed.
     */
    private fun grabFeedbackAndRun(activity: Activity,
                                   view: View?,
                                   runThis: (context: Context, uri: Uri) -> Unit) {

        if (!hasStoragePermissions(activity)) {
            requestStoragePermissions(activity)
            return
        }

        if (grabInProgress) {
            return
        }

        grabInProgress = true
        showHud(activity, "Generating Feedbackt...")
        view?.prepForBitmapConversion()

        DoAsync<Uri?>()
            .doInBackground {
                val bitmap = view?.convertToBitmap()
                bitmap?.saveAsPrivatePng(activity, storedImageName)
            }.doOnPostExectute { uri ->
                grabInProgress = false

                if (uri == null) {
                    Log.e(TAG, "generateAndSendScreenshot failed")
                } else {
                    runThis(activity, uri)
                }

            }.execute()
    }

    fun emailBitmap(activity: Activity, bitmap: Bitmap, text: String = "") {
        hideHud()
        bitmap.saveAsPrivatePng(activity, storedImageName)?.let { uri ->
            createEmailBitmapWithString(text).invoke(activity, uri)
        } ?: run {
            hideHud()
            Log.e(TAG, "generateAndSendScreenshot failed")
            // todo error
        }
    }

    private fun createEmailBitmapWithString(text: String?): (context: Context, uri: Uri) -> Unit {
        val stringBuilder = StringBuilder()
        stringBuilder.appendln(generateGreetingText())
        stringBuilder.append("")
        if (addActionContent) stringBuilder.appendln(text)
        if (addDeviceInfo) stringBuilder.appendln(generateDeviceDetailsText())
        if (emailContent != null) stringBuilder.appendln(emailContent)
        val messageText = stringBuilder.toString()

        return { context, uri ->
            hideHud()
            val emailIntent = Intent(Intent.ACTION_SEND)

            emailIntent.type = "image/png"

            if (email != null) emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitle)
            emailIntent.putExtra(Intent.EXTRA_TEXT, messageText)
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri)

            emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(emailIntent, "Send email"))
        }
    }

    private fun launchEdit(context: Context, uri: Uri) {
        hideHud()
        val launchIntent = EditFeedbacktActivity.newIntent(context, uri, editMode)
        context.startActivity(launchIntent)
    }

    private fun viewBitmap(context: Context, uri: Uri) {
        hideHud()
        val viewIntent = Intent()
        viewIntent.action = Intent.ACTION_VIEW
        viewIntent.setDataAndType(uri, "image/*")
        viewIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(viewIntent, "View Screenshot"))
    }

    private fun saveFileWithFileName(activity: Activity, fileName: String, onComplete: (Boolean) -> Unit): (context: Context, uri: Uri) -> Unit {
        return { context, uri ->
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            showHud(activity, "Saving to Gallery...")
            bitmap.saveAsPublicPng(context, fileName)?.let { uri ->
                showHud(activity, "Save complete!", showSpinner = false)
                // Let HUD show for 1s to let user read complete notification
                Handler().postDelayed({
                    hideHud()
                    onComplete(true)
                }, 1000)
            } ?: run {
                hideHud()
                onComplete(false)
                Log.e(TAG, "generateAndSendScreenshot failed")
                // todo error
            }
        }
    }

    private fun generateGreetingText(): String {
        return "I have some feedback for the attached screenshot: "
    }

    private fun generateDeviceDetailsText(): String {
        val contentBuilder = StringBuilder()
        contentBuilder.appendln("--- Device Details ---")
        contentBuilder.appendln("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        contentBuilder.appendln("Device Model: ${Build.MANUFACTURER} ${Build.MODEL}")
        contentBuilder.appendln("OS Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        return contentBuilder.toString()
    }

    private fun requestStoragePermissions(context: Activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(context, arrayOf (Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    private fun hasStoragePermissions(context: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private val applicationLifecycleListener = object: Application.ActivityLifecycleCallbacks {
        // Auto enable / disable shake detector
        override fun onActivityPaused(activity: Activity?) {
            activity?.let {
                if (autoDetectMultitouch) disableMultiTouchToActivateOnActivity()
                if (autoDetetctShake) disableShakeToActivateOnActivity()
            }
        }

        override fun onActivityResumed(activity: Activity?) {
            if (activity is EditFeedbacktActivity) {
                return
            }

            activity?.let {
                if (autoDetectMultitouch) enableMultiTouchToActivateOnActivity(it)
                if (autoDetetctShake) enableShakeToActivateOnActivity(it)
            }
        }

        // Not used
        override fun onActivityStarted(activity: Activity?) {}

        override fun onActivityDestroyed(activity: Activity?) {}

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

        override fun onActivityStopped(activity: Activity?) {}

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
    }
}