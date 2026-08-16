package com.healthos

import com.healthos.bluetooth.BleTelemetryParser
import com.healthos.bluetooth.HeartRateParser
import com.healthos.domain.model.MetricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleTelemetryParserTest {

    @Test
    fun parseStandardHeartRate_8Bit() {
        val payload = byteArrayOf(0x00, 72.toByte())
        val hr = HeartRateParser.parse(payload)
        assertEquals(72, hr)
    }

    @Test
    fun parseStandardHeartRate_16Bit() {
        val payload = byteArrayOf(0x01, 0x58.toByte(), 0x01.toByte()) // 0x0158 = 344
        val hr = HeartRateParser.parse(payload)
        assertEquals(344, hr)
    }

    @Test
    fun parseHeartRateWithRrIntervalsAndHrv() {
        // Flags: 0x10 (RR present), HR: 70 bpm, RR: 800ms (raw = 819 -> 0x0333)
        val payload = byteArrayOf(
            0x10.toByte(),
            70.toByte(),
            0x33.toByte(), 0x03.toByte(),
            0x40.toByte(), 0x03.toByte(),
        )
        val result = HeartRateParser.parseDetailed(payload)
        assertNotNull(result)
        assertEquals(70, result?.heartRate)
        assertTrue((result?.rrIntervalsMs?.size ?: 0) >= 2)
        assertNotNull(result?.rmssd)
        assertNotNull(result?.sdnn)
    }

    @Test
    fun calculateRmssdAndSdnn_validCalculation() {
        val rr = listOf(800, 850, 780, 820, 840)
        val rmssd = HeartRateParser.calculateRmssd(rr)
        val sdnn = HeartRateParser.calculateSdnn(rr)

        assertTrue(rmssd > 0.0)
        assertTrue(sdnn > 0.0)
    }

    @Test
    fun parseSkinTemperature_Celsius() {
        // Flags: 0x00 (Celsius), Mantissa: 367, Exponent: -1 (36.7 °C)
        val payload = byteArrayOf(
            0x00,
            0x6F.toByte(), 0x01.toByte(), 0x00.toByte(), // 367
            (-1).toByte(), // exponent 10^-1
        )
        val telemetry = BleTelemetryParser.parseTemperature(payload)
        assertNotNull(telemetry)
        assertEquals(MetricType.SKIN_TEMPERATURE, telemetry?.type)
        assertEquals(36.7, telemetry?.value ?: 0.0, 0.1)
        assertEquals("°C", telemetry?.unit)
    }

    @Test
    fun parseSpO2_validReading() {
        val payload = byteArrayOf(0x00, 98.toByte())
        val telemetry = BleTelemetryParser.parseSpO2(payload)
        assertNotNull(telemetry)
        assertEquals(MetricType.SPO2, telemetry?.type)
        assertEquals(98.0, telemetry?.value ?: 0.0, 0.01)
    }

    @Test
    fun parseBloodPressure_mmHg() {
        // Systolic: 120, Diastolic: 80
        val payload = byteArrayOf(
            0x00,
            120.toByte(), 0x00,
            80.toByte(), 0x00,
            90.toByte(), 0x00,
        )
        val telemetry = BleTelemetryParser.parseBloodPressure(payload)
        assertNotNull(telemetry)
        assertEquals(MetricType.BLOOD_PRESSURE_SYSTOLIC, telemetry?.type)
        assertEquals(120.0, telemetry?.value ?: 0.0, 0.01)
        assertEquals(80.0, telemetry?.secondaryValue ?: 0.0, 0.01)
    }

    @Test
    fun parseEda_MicroSiemens() {
        // Raw: 250 -> 2.50 µS
        val payload = byteArrayOf(0xFA.toByte(), 0x00.toByte())
        val telemetry = BleTelemetryParser.parseEda(payload)
        assertNotNull(telemetry)
        assertEquals(MetricType.EDA, telemetry?.type)
        assertEquals(2.5, telemetry?.value ?: 0.0, 0.01)
    }

    @Test
    fun parseCorruptedPayload_returnsNullGracefully() {
        assertNull(HeartRateParser.parse(null))
        assertNull(HeartRateParser.parse(byteArrayOf()))
        assertNull(BleTelemetryParser.parseTemperature(byteArrayOf(0x01)))
        assertNull(BleTelemetryParser.parseSpO2(null))
    }
}
