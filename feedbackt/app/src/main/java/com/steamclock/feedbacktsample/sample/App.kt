package com.steamclock.feedbacktsample.sample

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.steamclock.feedbackt.Feedbackt

class App: Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        Feedbackt.enableShakeToActivate(this)
    }

    override fun onActivityPaused(activity: Activity?) {
        Log.v(Feedbackt.TAG, "Sample onActivityPaused")
    }

    override fun onActivityResumed(activity: Activity?) {
        Log.v(Feedbackt.TAG, "Sample onActivityResumed")
    }

    override fun onActivityStarted(activity: Activity?) {

    }

    override fun onActivityDestroyed(activity: Activity?) {

    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {

    }

    override fun onActivityStopped(activity: Activity?) {

    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {

    }
}