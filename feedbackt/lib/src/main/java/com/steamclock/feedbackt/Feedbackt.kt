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
import android.provider.Settings
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
import com.steamclock.feedbackt.extensions.*
import android.content.Context.WINDOW_SERVICE
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import com.steamclock.feedbackt.utils.FeedbacktFileProvider
import java.io.File
import java.lang.Exception


/**
 * Feedbackt Singleton
 */
object Feedbackt {

    const val TAG = "Feedbackt"

    //-------------------------------
    // Public
    //-------------------------------
    /**
     * If set, this email address will be used by default as a send-to
     */
    var email: String? = null

    /**
     * If set, this is the default title of the email sent
     */
    var emailTitle = "Sending feedback"

    /**
     * If set, this String will be appended to the email sent.
     * This can be useful if you want to send custom details along with the screenshot.
     */
    var emailContent: String? = null

    /**
     * If true, device information will be automatically added to the email content being sent.
     */
    var addDeviceInfo = true

    /**
     * If true, any actions made while editing the screenshot (ie. dropping numbers), will automatically
     * add some content to the email as a guide for the user to add further information about the issue.
     */
    var addActionContent = true

    /**
     * Default mode that the edit activity will open with; determines what actions are available
     * to the user while editing.
     */
    var editMode = EditFeedbacktActivity.defaultMode

    //-------------------------------
    // Private
    //-------------------------------
    /**
     *
     */
    private const val storedImageName = "feedbackt"

    /**
     *
     */
    private var commonHud: WeakReference<ProgressHUD>? = null

    /**
     *
     */
    private var currentActivity: WeakReference<Activity>? = null

    /**
     *
     */
    private var shakeDetector: ShakeDetector? = null

    /**
     *
     */
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

                // todo... put back in
                //Feedbackt.grabFeedbackAndEdit(activity)


                startCapture(activity)
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


    // Permissions
    private fun requestPermission(context: Activity, permission: String, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (hasPermission(context, permission)) return
            ActivityCompat.requestPermissions(context, arrayOf (permission), requestCode)
        }
    }

    private fun hasPermission(context: Activity, permission: String): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private const val REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 10
    private fun hasStoragePermissions(context: Activity) = hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun requestStoragePermissions(context: Activity) = requestPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE_CODE)

    private const val REQUEST_ACTION_MANAGE_OVERLAY_PERMISSION_CODE = 11
    private fun hasOverlayPermissions(context: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= 23) { return Settings.canDrawOverlays(context) }
        return false
    }

    private fun requestOverlayPermissions(context: Activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.applicationContext.packageName}")
            )
            context.startActivityForResult(intent, REQUEST_ACTION_MANAGE_OVERLAY_PERMISSION_CODE)
        }
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

//    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
//
//        if (requestCode == REQUEST_ACTION_MANAGE_OVERLAY_PERMISSION_CODE) {
//
//            if (Build.VERSION.SDK_INT >= 23) {
//                if (!Settings.canDrawOverlays(currentActivity)) {
//                    // SYSTEM_ALERT_WINDOW permission not granted...
//                }
//            }
//
//
//        }
//    }

    //-------------------------------
    // Video Capture
    //-------------------------------
    private var projectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var displayMetrics: DisplayMetrics? = null
    private var mediaProjectionCallback: MediaProjectionCallback? = null

    private class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            stopRecording()
        }
    }

    private var isCapturing = false
    fun startStopCapture(activity: Activity) {
        if (isCapturing) {
            stopRecording()
        } else {
            startCapture(activity)
        }
    }

    fun startCapture(activity: Activity) {
        currentActivity = WeakReference(activity)

        Log.v("Video", "startCapture")
        // Initialize what we can
        getDisplayMetrics(activity)
        projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // The rest will have to wait until we have permission...
        if (mediaRecorder == null) {
            requestScreenCapturePermission(activity)
        } else {
            startRecording()
        }
    }

    private fun getDisplayMetrics(activity: Activity) {
        Log.v("Video", "getDisplayMetrics")
        displayMetrics = DisplayMetrics()
        val wm = activity.getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(displayMetrics)
    }

    private fun createMovieFilePath(filename: String): String {
        Log.v("Video", "createMovieFilePath")
        val rootMovieDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val feedbacktAlbumDir = File(rootMovieDir, "Feedbackt")

        val saveDirectory = if (feedbacktAlbumDir.exists() || feedbacktAlbumDir.mkdirs()) {
            feedbacktAlbumDir
        } else {
            rootMovieDir
        }

        return "$saveDirectory${File.separator}$filename.mp4"
    }

    private const val RequestScreenCapturePermissionCode = 12
    private fun requestScreenCapturePermission(activity: Activity) {
        Log.v("Video", "requestScreenCapturePermission")
        activity.startActivityForResult(projectionManager?.createScreenCaptureIntent(), RequestScreenCapturePermissionCode)
    }

    private fun onScreenCapturePermissionGranted(resultCode: Int, data: Intent) {
        Log.v("Video", "onScreenCapturePermissionGranted")
        // Need to prove we have permissions to record.
        mediaProjection = projectionManager?.getMediaProjection(resultCode, data)

        if (mediaProjection == null) {
            // fail
        }
        else if (initRecorder()) {
            // start recording
            startRecording()
        }
        else {
            // fail
        }
    }

    private var currentVideoFilePath: String? = null
    private fun initRecorder(): Boolean {
        Log.v("Video", "initRecorder")
        if (mediaRecorder == null) {

            val recorder = MediaRecorder()
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoEncodingBitRate(512 * 1000)
            recorder.setVideoFrameRate(30)
            recorder.setVideoSize(1000, 1000)
            currentVideoFilePath = createMovieFilePath("capture")
            recorder.setOutputFile(currentVideoFilePath)

            try {
                recorder.prepare()
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

            mediaProjectionCallback = MediaProjectionCallback()
            mediaProjection?.registerCallback(mediaProjectionCallback, null)

            mediaProjection?.createVirtualDisplay(
                "FeedbacktRecording",
                displayMetrics?.widthPixels ?: 480,
                displayMetrics?.heightPixels ?: 640,
                displayMetrics?.densityDpi ?: DisplayMetrics.DENSITY_HIGH,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface, null, null
            )

            mediaRecorder = recorder
        }

        return true
    }

    private fun startRecording() {
        Log.v("Video", "startRecording")
        isCapturing = true
        mediaRecorder?.start()
    }

    private fun stopRecording() {
        Log.v("Video", "stopRecording")
        isCapturing = false
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        virtualDisplay?.release()
        emailVideo()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v("Video", "onActivityResult")
        if (requestCode == RequestScreenCapturePermissionCode)     {
            if (resultCode == Activity.RESULT_OK && data != null) {
                onScreenCapturePermissionGranted(resultCode, data)
            }
        }
    }

    private fun emailVideo() {
        Log.v("Video", "emailVideo")
        val context = currentActivity?.get()?.applicationContext ?: return

        val videoUri= FeedbacktFileProvider().getUriForFile(context, File(currentVideoFilePath))
        val emailIntent = Intent(Intent.ACTION_SEND)

        emailIntent.type = "audio/mpeg4-generic"

        if (email != null) emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitle)
        emailIntent.putExtra(Intent.EXTRA_TEXT, generateGreetingText())
        emailIntent.putExtra(Intent.EXTRA_STREAM, videoUri)
        emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        currentActivity?.get()?.startActivity(Intent.createChooser(emailIntent, "Send email"))
    }

    private fun createEmailWithVideo(text: String?): (context: Context, uri: Uri) -> Unit {
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

}