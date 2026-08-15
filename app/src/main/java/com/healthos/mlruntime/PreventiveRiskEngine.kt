package com.healthos.mlruntime

import com.healthos.domain.model.Measurement
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.MlRiskResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreventiveRiskEngine
    @Inject
    constructor() {
        suspend fun analyze(measurements: List<Measurement>): MlRiskResult {
            val heartRate = measurements.firstOrNull { it.metricType == MetricType.HEART_RATE }?.value ?: 0.0
            val spo2 = measurements.firstOrNull { it.metricType == MetricType.SPO2 }?.value ?: 100.0
            val score =
                when {
                    heartRate > 120 || spo2 < 92 -> 0.92f
                    heartRate > 95 || spo2 < 95 -> 0.62f
                    else -> 0.18f
                }
            val label =
                when {
                    score >= 0.85f -> "Riesgo alto"
                    score >= 0.5f -> "Riesgo moderado"
                    else -> "Estable"
                }
            return MlRiskResult(score, label)
        }
    }
