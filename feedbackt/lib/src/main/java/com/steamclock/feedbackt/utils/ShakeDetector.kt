package com.steamclock.feedbackt.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

class ShakeDetector(context: Context, val listener: ShakeDetector.ShakeListener) {

    /**
     * Acceleration threshhold, not including gravity; modify to change sensitivity.
     */
    private var shakeAccelerationThreshhold = 1.7f //2.7f

    /**
     * Count of accelerated samples taken over time; this number will go up if we log the device
     * as shaking, and down if the sample indicates the device is not shaking. Once this value
     * reaches our shakeNumberThreshhold, we assume the device is being "shaken".
     */
    private var numberOfAcceleratedSamples: Int = 0

    private var acceleratedSamplesThreshhold = 20

    /**
     * Enable debug logs
     */
    var enableLogs = true

    /**
     * Sensor objects
     */
    private var sensorManger: SensorManager? = null
    private var sensor: Sensor? = null

    //------------------------------------------------------------
    // Callback interface
    //------------------------------------------------------------
    interface ShakeListener {
        fun onShakeDetected()
        fun onShakeNotSupported()
    }

    //------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------
    fun start() {
        logVerbose("Starting ShakeDetector")
        reset()
        if (sensorManger?.registerListener(
                sensorListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            ) != true
        ) {
            logError("Device does not have required sensors to detect shake gesture")
            listener.onShakeNotSupported()
        }
    }

    fun stop() {
        logVerbose("Stopping ShakeDetector")
        sensorManger?.unregisterListener(sensorListener)
    }

    //------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------
    private fun logVerbose(message: String) {
        if (!enableLogs) return
        Log.v(TAG, message)
    }

    private fun logError(message: String) {
        if (!enableLogs) return
        Log.e(TAG, message)
    }

    private fun reset() {
        numberOfAcceleratedSamples = 0
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(se: SensorEvent) {
            // Grab current acceleration values from the sensor.
            val x = se.values[0]
            val y = se.values[1]
            val z = se.values[2]

            // Determine if the magnitude of the acceleration is greater than our
            // the threshhold we use to determine if the device is being "accelerated"/"shaken"
            val currentMagnitude = (x * x) + (y * y) + (z * z)
            // Negate gravitational value.
            val acceleration = sqrt(currentMagnitude) - SensorManager.GRAVITY_EARTH
            // Use the absolute value to negate direction
            val isAccelerating = Math.abs(acceleration) > shakeAccelerationThreshhold

            when {
                isAccelerating -> numberOfAcceleratedSamples = Math.min(Int.MAX_VALUE, numberOfAcceleratedSamples+1)
                numberOfAcceleratedSamples > 0 -> numberOfAcceleratedSamples -= 1
            }

            logVerbose("acceleration: $acceleration | isAccelerating: $isAccelerating | numberOfAcceleratedSamples: $numberOfAcceleratedSamples")

            if (numberOfAcceleratedSamples >= acceleratedSamplesThreshhold) {
                listener.onShakeDetected()
                logVerbose("Complete shake gesture detected")
                reset()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    init {
        sensorManger = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManger?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accelerometer ->
            sensor = accelerometer
            start()
        } ?: run {
            listener.onShakeNotSupported()
        }
    }

    companion object {
        private const val TAG = "FeedbacktShakeDetector"
    }
}