package com.healthos

import com.healthos.domain.model.Measurement
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.SyncStatus
import com.healthos.mlruntime.PreventiveRiskEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PreventiveRiskEngineTest {
    @Test
    fun flagsHighRiskWhenSpo2IsLow() =
        runTest {
            val result =
                PreventiveRiskEngine().analyze(
                    listOf(
                        Measurement("hr", MetricType.HEART_RATE, 80.0, "bpm", "now", SyncStatus.SYNCED),
                        Measurement("spo2", MetricType.SPO2, 90.0, "%", "now", SyncStatus.SYNCED),
                    ),
                )

            assertEquals("Riesgo alto", result.label)
        }
}
