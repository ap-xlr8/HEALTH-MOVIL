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

data class BleMeasurement(
    val heartRate: Int? = null,
    val spo2: Double? = null,
    val skinTempCelsius: Double? = null,
    val edaMicroSiemens: Double? = null,
    val systolicBp: Double? = null,
    val diastolicBp: Double? = null,
    val pttMs: Double? = null,
    val rmssd: Double? = null,
    val sdnn: Double? = null,
)

data class ScannedBleDevice(
    val mac: String,
    val name: String,
)

@SuppressLint("MissingPermission")
class BleConnectionManager(private val context: Context) {
    private val _connectionState = MutableStateFlow<BleState>(BleState.Disconnected)
    val connectionState: StateFlow<BleState> = _connectionState

    private val _measurements = MutableSharedFlow<BleMeasurement>(extraBufferCapacity = 16)
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
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(HEALTH_THERMOMETER_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(PULSE_OXIMETER_SERVICE_UUID)).build(),
        )
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

                // 1. Heart Rate Service
                gatt.getService(HEART_RATE_SERVICE_UUID)?.let { service ->
                    service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)?.let { char ->
                        enableNotification(gatt, char)
                    }
                }

                // 2. Health Thermometer Service
                gatt.getService(HEALTH_THERMOMETER_SERVICE_UUID)?.let { service ->
                    service.getCharacteristic(TEMPERATURE_MEASUREMENT_UUID)?.let { char ->
                        enableNotification(gatt, char)
                    }
                }

                // 3. Pulse Oximeter Service
                gatt.getService(PULSE_OXIMETER_SERVICE_UUID)?.let { service ->
                    service.getCharacteristic(SPO2_MEASUREMENT_UUID)?.let { char ->
                        enableNotification(gatt, char)
                    }
                }

                // 4. Blood Pressure Service
                gatt.getService(BLOOD_PRESSURE_SERVICE_UUID)?.let { service ->
                    service.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_UUID)?.let { char ->
                        enableNotification(gatt, char)
                    }
                }
            }

            private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
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
                when (characteristic.uuid) {
                    HEART_RATE_MEASUREMENT_UUID -> {
                        val parsed = HeartRateParser.parseDetailed(characteristic.value)
                        if (parsed != null) {
                            _measurements.tryEmit(
                                BleMeasurement(
                                    heartRate = parsed.heartRate,
                                    rmssd = parsed.rmssd,
                                    sdnn = parsed.sdnn,
                                ),
                            )
                        }
                    }
                    TEMPERATURE_MEASUREMENT_UUID -> {
                        BleTelemetryParser.parseTemperature(characteristic.value)?.let {
                            _measurements.tryEmit(BleMeasurement(skinTempCelsius = it.value))
                        }
                    }
                    SPO2_MEASUREMENT_UUID -> {
                        BleTelemetryParser.parseSpO2(characteristic.value)?.let {
                            _measurements.tryEmit(BleMeasurement(spo2 = it.value))
                        }
                    }
                    BLOOD_PRESSURE_MEASUREMENT_UUID -> {
                        BleTelemetryParser.parseBloodPressure(characteristic.value)?.let {
                            _measurements.tryEmit(
                                BleMeasurement(
                                    systolicBp = it.value,
                                    diastolicBp = it.secondaryValue,
                                ),
                            )
                        }
                    }
                    EDA_MEASUREMENT_UUID -> {
                        BleTelemetryParser.parseEda(characteristic.value)?.let {
                            _measurements.tryEmit(BleMeasurement(edaMicroSiemens = it.value))
                        }
                    }
                }
            }
        }

    fun parseHeartRate(payload: ByteArray?): Int? {
        return HeartRateParser.parse(payload)
    }

    companion object {
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

        val HEALTH_THERMOMETER_SERVICE_UUID: UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
        val TEMPERATURE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb")

        val PULSE_OXIMETER_SERVICE_UUID: UUID = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb")
        val SPO2_MEASUREMENT_UUID: UUID = UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb")

        val BLOOD_PRESSURE_SERVICE_UUID: UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
        val BLOOD_PRESSURE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb")

        val EDA_MEASUREMENT_UUID: UUID = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb")

        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}

