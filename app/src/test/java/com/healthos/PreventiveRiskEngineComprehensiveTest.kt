package com.healthos

import com.healthos.domain.model.Measurement
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.SyncStatus
import com.healthos.mlruntime.PreventiveRiskEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreventiveRiskEngineComprehensiveTest {

    private val engine = PreventiveRiskEngine()

    @Test
    fun analyze_emptyMeasurements_returnsNoDataState() = runTest {
        val result = engine.analyze(emptyList())
        assertEquals(0.0f, result.score, 0.001f)
        assertEquals("Sin mediciones suficientes", result.label)
    }

    @Test
    fun analyze_normalVitals_returnsStableRisk() = runTest {
        val measurements = listOf(
            Measurement("1", MetricType.HEART_RATE, 72.0, "bpm", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
            Measurement("2", MetricType.SPO2, 98.0, "%", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
            Measurement("3", MetricType.SKIN_TEMPERATURE, 36.6, "°C", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
            Measurement("4", MetricType.HRV_RMSSD, 45.0, "ms", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
        )

        val result = engine.analyze(measurements)
        assertEquals("Estable", result.label)
        assertTrue(result.score < 0.45f)
        assertNotNull(result.stressScore)
        assertNotNull(result.vo2MaxScore)
    }

    @Test
    fun analyze_criticalHypoxemia_returnsHighRisk() = runTest {
        val measurements = listOf(
            Measurement("1", MetricType.HEART_RATE, 115.0, "bpm", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
            Measurement("2", MetricType.SPO2, 88.0, "%", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
        )

        val result = engine.analyze(measurements)
        assertEquals("Riesgo alto", result.label)
        assertTrue(result.score >= 0.80f)
        assertTrue(result.details?.contains("oxígeno") == true)
    }

    @Test
    fun analyze_feverAndRestingTachycardia_detectsInfectionRisk() = runTest {
        val measurements = listOf(
            Measurement("1", MetricType.HEART_RATE, 108.0, "bpm", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
            Measurement("2", MetricType.SKIN_TEMPERATURE, 38.5, "°C", "2026-08-16T12:00:00Z", SyncStatus.SYNCED),
        )

        val result = engine.analyze(measurements)
        assertTrue((result.infectionRiskScore ?: 0.0f) > 0.6f)
        assertTrue(result.details?.contains("febril") == true || result.details?.contains("inflamatorio") == true)
    }
}
