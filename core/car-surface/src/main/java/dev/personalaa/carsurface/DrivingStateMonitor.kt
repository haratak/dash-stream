package dev.personalaa.carsurface

import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.Speed
import java.util.concurrent.Executor

/**
 * Subscribes to vehicle speed when Android Auto exposes it.
 * When speed is unknown (e.g. DHU without sensor), treats the vehicle as parked.
 */
class DrivingStateMonitor(
    private val carContext: CarContext,
    private val executor: Executor,
    private val onDrivingChanged: (Boolean) -> Unit,
) {
    private var hardwareManager: CarHardwareManager? = null
    private var subscribed = false
    private val speedListener = OnCarDataAvailableListener<Speed> { speed ->
        onDrivingChanged(isDriving(speed))
    }

    fun start() {
        if (subscribed) return
        subscribed = true
        runCatching {
            hardwareManager = carContext.getCarService(CarHardwareManager::class.java)
            hardwareManager?.carInfo?.addSpeedListener(executor, speedListener)
        }.onFailure {
            onDrivingChanged(false)
        }
    }

    fun stop() {
        subscribed = false
        runCatching {
            hardwareManager?.carInfo?.removeSpeedListener(speedListener)
        }
        hardwareManager = null
    }

    private fun isDriving(speed: Speed): Boolean {
        val displaySpeed = speed.displaySpeedMetersPerSecond
        if (displaySpeed.status == CarValue.STATUS_SUCCESS) {
            return (displaySpeed.value ?: 0f) > 0.5f
        }
        val rawSpeed = speed.rawSpeedMetersPerSecond
        if (rawSpeed.status == CarValue.STATUS_SUCCESS) {
            return (rawSpeed.value ?: 0f) > 0.5f
        }
        return false
    }
}
