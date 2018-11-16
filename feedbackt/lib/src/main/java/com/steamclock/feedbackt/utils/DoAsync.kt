package com.steamclock.feedbackt.utils

import android.os.AsyncTask

class DoAsync<T> : AsyncTask<Void, Void, T>() {
    private var onBackground: (() -> T)? = null
    private var onComplete: ((T?) -> Unit)? = null

    fun doInBackground(handler: () -> T): DoAsync<T> {
        onBackground = handler
        return this
    }

    fun doOnPostExectute(handler: (T?) -> Unit): DoAsync<T> {
        onComplete = handler
        return this
    }

    override fun doInBackground(vararg params: Void?): T? {
        return onBackground?.invoke()
    }

    override fun onPostExecute(result: T?) {
        super.onPostExecute(result)
        onComplete?.invoke(result)
    }
}
