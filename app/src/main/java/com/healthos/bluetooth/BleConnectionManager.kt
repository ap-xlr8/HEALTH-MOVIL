package com.healthos.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

sealed interface BleState {
    data object Disconnected : BleState

    data object Scanning : BleState

    data object Connecting : BleState

    data object Connected : BleState

    data class Error(val message: String) : BleState
}

data class BleMeasurement(val heartRate: Int)

@SuppressLint("MissingPermission")
class BleConnectionManager(private val context: Context) {
    private val _connectionState = MutableStateFlow<BleState>(BleState.Disconnected)
    val connectionState: StateFlow<BleState> = _connectionState

    private val _measurements = MutableSharedFlow<BleMeasurement>(extraBufferCapacity = 8)
    val measurements: SharedFlow<BleMeasurement> = _measurements

    fun startScanPlaceholder() {
        _connectionState.value = BleState.Scanning
    }

    fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = BleState.Connecting
        device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.value = BleState.Error("Error GATT: $status")
                    gatt.close()
                    return
                }
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _connectionState.value = BleState.Connected
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _connectionState.value = BleState.Disconnected
                        gatt.close()
                    }
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int,
            ) {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                val characteristic =
                    gatt.getService(HEART_RATE_SERVICE_UUID)
                        ?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                        ?: return
                gatt.setCharacteristicNotification(characteristic, true)
                characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)?.let { descriptor ->
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                    parseHeartRate(characteristic.value)?.let { _measurements.tryEmit(BleMeasurement(it)) }
                }
            }
        }

    fun parseHeartRate(payload: ByteArray?): Int? {
        return HeartRateParser.parse(payload)
    }

    companion object {
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
