package com.healthos.mlruntime

import android.content.Context
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.MlRiskResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Singleton
class PreventiveRiskEngine {
    private var context: Context? = null
    private var hrAnomalyInterpreter: Interpreter? = null
    private var glucoseInterpreter: Interpreter? = null
    private var spo2Interpreter: Interpreter? = null
    private var sleepInterpreter: Interpreter? = null
    private var activityInterpreter: Interpreter? = null

    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        this.context = context
        hrAnomalyInterpreter = tryLoadModel(context, "models/heart_rate_anomaly.tflite")
        glucoseInterpreter = tryLoadModel(context, "models/glucose_patterns.tflite")
        spo2Interpreter = tryLoadModel(context, "models/spo2_critical.tflite")
        sleepInterpreter = tryLoadModel(context, "models/sleep_quality.tflite")
        activityInterpreter = tryLoadModel(context, "models/activity_recognition.tflite")
    }

    constructor() {
        this.context = null
    }

    private fun tryLoadModel(ctx: Context, assetPath: String): Interpreter? {
        return try {
            val fileDescriptor = ctx.assets.openFd(assetPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val buffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength,
            )
            Interpreter(buffer)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun analyze(measurements: List<Measurement>): MlRiskResult {
        if (measurements.isEmpty()) {
            return MlRiskResult(0.0f, "Sin mediciones suficientes")
        }

        val heartRate = measurements.firstOrNull { it.metricType == MetricType.HEART_RATE }?.value
        val spo2 = measurements.firstOrNull { it.metricType == MetricType.SPO2 }?.value
        val skinTemp = measurements.firstOrNull { it.metricType == MetricType.SKIN_TEMPERATURE || it.metricType == MetricType.TEMPERATURE }?.value ?: 36.5
        val eda = measurements.firstOrNull { it.metricType == MetricType.EDA }?.value ?: 2.5
        val ptt = measurements.firstOrNull { it.metricType == MetricType.PTT }?.value ?: 240.0
        val rmssd = measurements.firstOrNull { it.metricType == MetricType.HRV_RMSSD }?.value ?: max(15.0, 60.0 - ((heartRate ?: 75.0) * 0.2))
        val sdnn = measurements.firstOrNull { it.metricType == MetricType.HRV_SDNN }?.value ?: (rmssd * 1.2)

        val hrVal = heartRate ?: 75.0

        // 1. TFLite Inferences
        val hrAnomalyScore = runHrAnomalyInference(hrVal, rmssd, if (hrVal > 110) 1.0f else 0.0f)
        val glucoseRiskScore = runGlucoseInference(hrVal, rmssd, skinTemp, eda)
        val spo2RiskScore = runSpo2Inference(spo2 ?: 98.0, hrVal)
        val sleepScore = runSleepInference(hrVal, rmssd)

        // 2. On-Device Clinical Algorithmic Estimators
        // Estimador 1: Estrés Crónico / Fatiga SNC (rMSSD, SDNN, EDA, Temp)
        // Menor rMSSD + mayor EDA = Mayor estrés autonómico
        val normalizedHrvStress = (1.0 - (rmssd.coerceIn(10.0, 80.0) - 10.0) / 70.0).toFloat()
        val normalizedEdaStress = (eda.coerceIn(0.5, 10.0) / 10.0).toFloat()
        val stressScore = ((normalizedHrvStress * 0.6f) + (normalizedEdaStress * 0.4f)).coerceIn(0.0f, 1.0f)

        // Estimador 2: Detección Temprana de Infecciones (Temp basal elevada + taquicardia en reposo)
        val infectionScore = calculateInfectionRisk(skinTemp, hrVal)

        // Estimador 3: Riesgo de Hipertensión / Rigidez Arterial (Morfología de pulso y PTT)
        val hypertensionRiskScore = calculateHypertensionRisk(ptt, hrVal)

        // Estimador 4: VO2max Estimado / Capacidad Cardiovascular
        val vo2MaxScore = calculateEstimatedVo2Max(hrVal, rmssd)

        // 3. Puntuación de Riesgo Global Ponderado
        val primaryScore = maxOf(
            hrAnomalyScore,
            spo2RiskScore,
            infectionScore * 0.9f,
            hypertensionRiskScore * 0.7f,
        )

        val label = when {
            primaryScore >= 0.80f -> "Riesgo alto"
            primaryScore >= 0.45f -> "Riesgo moderado"
            else -> "Estable"
        }

        val detailsBuilder = StringBuilder()
        if (hrAnomalyScore > 0.6f) detailsBuilder.append("Anomalía en ritmo cardíaco. ")
        if (spo2RiskScore > 0.6f) detailsBuilder.append("Alerta de saturación de oxígeno. ")
        if (infectionScore > 0.6f) detailsBuilder.append("Patrón febril / inflamatorio sospechoso. ")
        if (stressScore > 0.7f) detailsBuilder.append("Índice de estrés SNC elevado. ")
        if (detailsBuilder.isEmpty()) detailsBuilder.append("Signos vitales dentro de parámetros normales.")

        return MlRiskResult(
            score = primaryScore,
            label = label,
            details = detailsBuilder.toString().trim(),
            metabolicRiskScore = glucoseRiskScore,
            sleepQualityScore = sleepScore,
            stressScore = stressScore,
            infectionRiskScore = infectionScore,
            hypertensionRiskScore = hypertensionRiskScore,
            vo2MaxScore = vo2MaxScore,
        )
    }

    private fun runHrAnomalyInference(hr: Double, variability: Double, activity: Float): Float {
        hrAnomalyInterpreter?.let { interpreter ->
            try {
                val input = ByteBuffer.allocateDirect(4 * 3).order(ByteOrder.nativeOrder())
                input.putFloat(hr.toFloat())
                input.putFloat(variability.toFloat())
                input.putFloat(activity)
                input.rewind()

                val output = ByteBuffer.allocateDirect(4 * 1).order(ByteOrder.nativeOrder())
                interpreter.run(input, output)
                output.rewind()
                return output.float.coerceIn(0.0f, 1.0f)
            } catch (_: Exception) {}
        }
        return if (hr > 120 || hr < 45) 0.88f else if (hr > 95) 0.48f else 0.15f
    }

    private fun runGlucoseInference(hr: Double, hrv: Double, temp: Double, eda: Double): Float {
        glucoseInterpreter?.let { interpreter ->
            try {
                val input = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder())
                input.putFloat(hr.toFloat())
                input.putFloat(hrv.toFloat())
                input.putFloat(temp.toFloat())
                input.putFloat(eda.toFloat())
                input.rewind()

                val output = ByteBuffer.allocateDirect(4 * 1).order(ByteOrder.nativeOrder())
                interpreter.run(input, output)
                output.rewind()
                return output.float.coerceIn(0.0f, 1.0f)
            } catch (_: Exception) {}
        }
        // Heurística metabólica: taquicardia postprandial + sudoración/EDA aumentada
        val metabolicFlux = if (eda > 5.0 && hr > 85) 0.65f else 0.22f
        return metabolicFlux
    }

    private fun runSpo2Inference(spo2: Double, hr: Double): Float {
        spo2Interpreter?.let { interpreter ->
            try {
                val input = ByteBuffer.allocateDirect(4 * 2).order(ByteOrder.nativeOrder())
                input.putFloat(spo2.toFloat())
                input.putFloat(hr.toFloat())
                input.rewind()

                val output = ByteBuffer.allocateDirect(4 * 1).order(ByteOrder.nativeOrder())
                interpreter.run(input, output)
                output.rewind()
                return output.float.coerceIn(0.0f, 1.0f)
            } catch (_: Exception) {}
        }
        return when {
            spo2 <= 90.0 -> 0.95f
            spo2 < 95.0 -> 0.65f
            else -> 0.12f
        }
    }

    private fun runSleepInference(hr: Double, hrv: Double): Float {
        sleepInterpreter?.let { interpreter ->
            try {
                val input = ByteBuffer.allocateDirect(4 * 2).order(ByteOrder.nativeOrder())
                input.putFloat(hr.toFloat())
                input.putFloat(hrv.toFloat())
                input.rewind()

                val output = ByteBuffer.allocateDirect(4 * 1).order(ByteOrder.nativeOrder())
                interpreter.run(input, output)
                output.rewind()
                return output.float.coerceIn(0.0f, 1.0f)
            } catch (_: Exception) {}
        }
        // Buen sueño: HR bajo y HRV alto
        return ((hrv / 60.0) * (1.0 - hr / 120.0)).toFloat().coerceIn(0.1f, 0.95f)
    }

    private fun calculateInfectionRisk(skinTemp: Double, restingHr: Double): Float {
        var score = 0.1f
        if (skinTemp >= 37.8) score += 0.5f
        else if (skinTemp >= 37.2) score += 0.25f

        if (restingHr > 100) score += 0.35f
        else if (restingHr > 85) score += 0.15f

        return score.coerceIn(0.0f, 1.0f)
    }

    private fun calculateHypertensionRisk(pttMs: Double, hr: Double): Float {
        // PTT inversamente proporcional a la presión arterial: PTT bajo = vasos rígidos / HTA
        var score = 0.15f
        if (pttMs < 180.0) score += 0.65f
        else if (pttMs < 210.0) score += 0.35f

        if (hr > 90) score += 0.2f
        return score.coerceIn(0.0f, 1.0f)
    }

    private fun calculateEstimatedVo2Max(restingHr: Double, hrvRmssd: Double): Float {
        // Fórmula Uth-Sørensen-Overgaard-Pedersen modificada para HRV:
        // VO2max ≈ 15.3 * (HRmax / HRrest) + corrección por HRV
        val estimatedHrMax = 190.0
        val baseVo2 = 15.3 * (estimatedHrMax / restingHr.coerceAtLeast(40.0))
        val hrvFactor = (hrvRmssd / 50.0).coerceIn(0.7, 1.3)
        val vo2MaxMlKgMin = (baseVo2 * hrvFactor).toFloat().coerceIn(20f, 65f)
        return vo2MaxMlKgMin
    }
}

