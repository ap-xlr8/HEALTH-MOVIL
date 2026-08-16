package com.healthos.bluetooth

import com.healthos.domain.model.MetricType

data class TelemetryMeasurement(
    val type: MetricType,
    val value: Double,
    val unit: String,
    val secondaryValue: Double? = null,
)

object BleTelemetryParser {

    /**
     * Parser para Health Thermometer (GATT Service 0x1809 / Characteristic 0x2A1C).
     * Soporta temperaturas Celsius / Fahrenheit según IEEE-11073 32-bit FLOAT.
     */
    fun parseTemperature(payload: ByteArray?): TelemetryMeasurement? {
        if (payload == null || payload.size < 5) return null
        val flags = payload[0].toInt() and 0xFF
        val isFahrenheit = (flags and 0x01) != 0

        // IEEE-11073 32-bit FLOAT: mantissa 24-bit con signo + exponent 8-bit con signo
        val mantissa = (payload[1].toInt() and 0xFF) or
            ((payload[2].toInt() and 0xFF) shl 8) or
            ((payload[3].toInt()) shl 16)
        val exponent = payload[4].toInt()

        var temp = mantissa * Math.pow(10.0, exponent.toDouble())
        var unit = "°C"
        if (isFahrenheit) {
            temp = (temp - 32.0) * (5.0 / 9.0) // Normalizado a Celsius internamente
        }

        if (temp in 25.0..45.0) {
            return TelemetryMeasurement(
                type = MetricType.SKIN_TEMPERATURE,
                value = Math.round(temp * 10.0) / 10.0,
                unit = unit,
            )
        }
        return null
    }

    /**
     * Parser para Pulse Oximeter (GATT Service 0x1822 / Characteristic 0x2A5E o 0x2A5F).
     * Devuelve saturación SpO2 (%) y frecuencia de pulso.
     */
    fun parseSpO2(payload: ByteArray?): TelemetryMeasurement? {
        if (payload == null || payload.size < 2) return null
        val flags = payload[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0

        val spo2Value = if (is16Bit && payload.size >= 4) {
            val raw = (payload[1].toInt() and 0xFF) or ((payload[2].toInt() and 0xFF) shl 8)
            raw.toDouble() / 10.0
        } else {
            (payload[1].toInt() and 0xFF).toDouble()
        }

        if (spo2Value in 50.0..100.0) {
            return TelemetryMeasurement(
                type = MetricType.SPO2,
                value = spo2Value,
                unit = "%",
            )
        }
        return null
    }

    /**
     * Parser para Blood Pressure / PTT (GATT Service 0x1810 / Characteristic 0x2A35).
     * Extrae presión sistólica, diastólica y tiempo de tránsito de pulso (PTT).
     */
    fun parseBloodPressure(payload: ByteArray?): TelemetryMeasurement? {
        if (payload == null || payload.size < 7) return null
        val flags = payload[0].toInt() and 0xFF
        val isKpa = (flags and 0x01) != 0

        var systolic = ((payload[1].toInt() and 0xFF) or ((payload[2].toInt() and 0xFF) shl 8)).toDouble()
        var diastolic = ((payload[3].toInt() and 0xFF) or ((payload[4].toInt() and 0xFF) shl 8)).toDouble()

        if (isKpa) {
            // Convertir kPa a mmHg (1 kPa ≈ 7.50062 mmHg)
            systolic *= 7.50062
            diastolic *= 7.50062
        }

        if (systolic in 60.0..260.0 && diastolic in 30.0..160.0) {
            return TelemetryMeasurement(
                type = MetricType.BLOOD_PRESSURE_SYSTOLIC,
                value = Math.round(systolic).toDouble(),
                unit = "mmHg",
                secondaryValue = Math.round(diastolic).toDouble(),
            )
        }
        return null
    }

    /**
     * Parser para Actividad Electrodérmica / EDA / GSR (Conductancia de la piel en microSiemens µS).
     */
    fun parseEda(payload: ByteArray?): TelemetryMeasurement? {
        if (payload == null || payload.size < 2) return null
        val raw = (payload[0].toInt() and 0xFF) or ((payload[1].toInt() and 0xFF) shl 8)
        val edaMicroSiemens = raw.toDouble() / 100.0 // ej. 2.45 µS
        if (edaMicroSiemens in 0.01..50.0) {
            return TelemetryMeasurement(
                type = MetricType.EDA,
                value = Math.round(edaMicroSiemens * 100.0) / 100.0,
                unit = "µS",
            )
        }
        return null
    }
}
