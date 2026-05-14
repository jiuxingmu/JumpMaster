package com.jumpmaster.app.data.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Streams normalized accelerometer samples for sensor-assisted jump detection.
 * Concrete implementation will register [android.hardware.Sensor.TYPE_ACCELEROMETER].
 */
interface SensorProvider {

    val accelerometer: Flow<AccelerometerSample>
}

data class AccelerometerSample(
    val timestampNanos: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)
