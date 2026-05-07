package com.jumpmaster.app.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

@Singleton
class DeviceTiltProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val _tiltFromVerticalDeg = MutableStateFlow(Float.NaN)
    val tiltFromVerticalDeg: StateFlow<Float> = _tiltFromVerticalDeg.asStateFlow()

    private val gravityListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val norm = sqrt(x * x + y * y + z * z)
                if (norm < 1e-3f) return
                val cosTheta = (abs(y) / norm).coerceIn(0f, 1f)
                _tiltFromVerticalDeg.value = acos(cosTheta) * (180f / Math.PI.toFloat())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

    init {
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val fallbackAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(
            gravityListener,
            gravitySensor ?: fallbackAccel,
            SensorManager.SENSOR_DELAY_GAME,
        )
    }

    fun stop() {
        sensorManager.unregisterListener(gravityListener)
    }
}
