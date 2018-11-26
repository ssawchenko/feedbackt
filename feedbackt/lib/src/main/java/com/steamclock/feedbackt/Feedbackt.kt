package com.steamclock.feedbackt

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.kaopiz.kprogresshud.KProgressHUD
import com.squareup.seismic.ShakeDetector
import com.steamclock.feedbackt.activities.EditFeedbacktActivity
import com.steamclock.feedbackt.extensions.convertToBitmap
import com.steamclock.feedbackt.extensions.prepForBitmapConversion
import com.steamclock.feedbackt.extensions.saveAsPng
import com.steamclock.feedbackt.extensions.setOnXLongPress
import com.steamclock.feedbackt.utils.DoAsync
import java.lang.ref.WeakReference

/**
 * Feedbackt Singleton
 */
object Feedbackt {

    val TAG = "Feedbackt"
    private val email = "shayla@steamclock.com"
    private val emailTitle = "Sending feedback"

    // todo static field leaks, may want to move Feedbackt out of the singleton pattern.
    private var commonHud: WeakReference<KProgressHUD>? = null
    private var currentActivity: WeakReference<Activity>? = null
    private var shakeDetector: ShakeDetector? = null

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
    fun enableShakeToActivate(application: Application) {
        autoDetetctShake = true
        registerLifecycleCallbacks(application)
    }

    fun disableShakeToActivate(application: Application) {
        autoDetetctShake = false
        unregisterLifecycleCallbacksIfNoAutoDetectors(application)
    }

    fun enableShakeToActivateOnActivity(activity: Activity) {
        val sensorManager = activity.getSystemService(SENSOR_SERVICE) as SensorManager?
        shakeDetector = ShakeDetector {
            if (activity.isFinishing) return@ShakeDetector
            Feedbackt.grabFeedbackAndEdit(activity)
        }
        shakeDetector?.setSensitivity(ShakeDetector.SENSITIVITY_LIGHT)
        shakeDetector?.start(sensorManager)
    }

    fun disableShakeToActivateOnActivity() {
        shakeDetector?.stop()
        shakeDetector = null
    }

    //-------------------------------
    // Public
    //-------------------------------
    fun grabFeedbackAndEmail(activity: Activity, view: View? = getRootView(activity)) {
        grabFeedbackAndRun(activity, view, ::emailBitmap)
    }

    fun grabFeedbackAndEdit(activity: Activity, view: View? = getRootView(activity)) {
        grabFeedbackAndRun(activity, view, ::launchEdit)
    }

    fun grabFeedbackAndView(activity: Activity, view: View? = getRootView(activity)) {
        grabFeedbackAndRun(activity, view, ::viewBitmap)
    }

    fun showHud(activity: Activity, text: Any? = null) {
        hideHud()
        commonHud = WeakReference(KProgressHUD.create(activity)
            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
            .setCancellable(true)
            .setAnimationSpeed(2)
            .setBackgroundColor(ContextCompat.getColor(activity, R.color.colorAccent))
            .setDimAmount(0.5f))

        text?.let {
            if (it is String) {
                commonHud?.get()?.setLabel(it)
            }
            if (it is Int) {
                commonHud?.get()?.setLabel(activity.getString(it))
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

    private fun grabFeedbackAndRun(activity: Activity,
                                   view: View?,
                                   runThis: (context: Context, uri: Uri) -> Unit) {

        if (!hasStoragePermissions(activity)) {
            requestStoragePermissions(activity)
            return
        }

        showHud(activity, "Generating Feedbackt...")
        view?.prepForBitmapConversion()

        DoAsync<Uri?>()
            .doInBackground {
                val bitmap = view?.convertToBitmap()
                bitmap?.saveAsPng(activity, "feedbackt.png")
            }.doOnPostExectute { uri ->
                hideHud()

                if (uri == null) {
                    Log.e(TAG, "generateAndSendScreenshot failed")
                } else {
                    runThis(activity, uri)
                }

            }.execute()
    }

    fun emailBitmap(activity: Activity, bitmap: Bitmap) {
        bitmap.saveAsPng(activity, "feedbackt.png")?.let { uri ->
            hideHud()
            emailBitmap(activity, uri)
        } ?: run {
            hideHud()
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