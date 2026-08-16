package com.healthos.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface BleState {
    data object Disconnected : BleState

    data object Scanning : BleState

    data object Connecting : BleState

    data object Connected : BleState

    data class Error(val message: String) : BleState
}

data class BleMeasurement(val heartRate: Int)

data class ScannedBleDevice(
    val mac: String,
    val name: String,
)

@SuppressLint("MissingPermission")
class BleConnectionManager(private val context: Context) {
    private val _connectionState = MutableStateFlow<BleState>(BleState.Disconnected)
    val connectionState: StateFlow<BleState> = _connectionState

    private val _measurements = MutableSharedFlow<BleMeasurement>(extraBufferCapacity = 8)
    val measurements: SharedFlow<BleMeasurement> = _measurements

    private val _scannedDevices = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedBleDevice>> = _scannedDevices

    private var activeGatt: BluetoothGatt? = null
    private var scanJob: Job? = null
    private var scanCallback: ScanCallback? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val leScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    fun startScan(timeoutMillis: Long = 10_000L) {
        if (_connectionState.value == BleState.Scanning) return
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = BleState.Error("Bluetooth desactivado o no disponible.")
            return
        }
        val scanner = leScanner
        if (scanner == null) {
            _connectionState.value = BleState.Error("BLE no disponible en este dispositivo.")
            return
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = BleState.Scanning

        val callback =
            object : ScanCallback() {
                override fun onScanResult(
                    callbackType: Int,
                    result: ScanResult,
                ) {
                    val device = result.device
                    val name = device.name ?: result.scanRecord?.deviceName
                    val current = _scannedDevices.value
                    if (current.none { it.mac == device.address }) {
                        _scannedDevices.value = current + ScannedBleDevice(device.address, name ?: device.address)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    _connectionState.value = BleState.Error("Error de escaneo BLE (código $errorCode)")
                }
            }
        scanCallback = callback

        val settings =
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID)).build())
        try {
            scanner.startScan(filters, settings, callback)
        } catch (e: SecurityException) {
            _connectionState.value = BleState.Error("Permiso de Bluetooth requerido.")
            scanCallback = null
            return
        } catch (_: Exception) {
            _connectionState.value = BleState.Error("No se pudo iniciar el escaneo BLE.")
            scanCallback = null
            return
        }

        scanJob?.cancel()
        scanJob =
            scope.launch {
                delay(timeoutMillis)
                stopScan()
            }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        scanCallback?.let { callback ->
            try {
                leScanner?.stopScan(callback)
            } catch (_: SecurityException) {
                // Ignored
            } catch (_: Exception) {
                // Ignored
            }
        }
        scanCallback = null
        if (_connectionState.value == BleState.Scanning) {
            _connectionState.value = BleState.Disconnected
        }
    }

    fun connectToAddress(address: String): Boolean {
        return try {
            val adapter = bluetoothAdapter ?: return false
            val device = adapter.getRemoteDevice(address) ?: return false
            connectToDevice(device)
            true
        } catch (_: SecurityException) {
            _connectionState.value = BleState.Error("Permiso de Bluetooth requerido.")
            false
        } catch (_: Exception) {
            false
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        disconnect()
        _connectionState.value = BleState.Connecting
        activeGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {
            // Ignored
        } finally {
            activeGatt = null
            _connectionState.value = BleState.Disconnected
        }
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
                    try {
                        gatt.close()
                    } catch (_: Exception) {}
                    if (activeGatt == gatt) activeGatt = null
                    return
                }
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _connectionState.value = BleState.Connected
                        activeGatt = gatt
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _connectionState.value = BleState.Disconnected
                        try {
                            gatt.close()
                        } catch (_: Exception) {}
                        if (activeGatt == gatt) activeGatt = null
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
