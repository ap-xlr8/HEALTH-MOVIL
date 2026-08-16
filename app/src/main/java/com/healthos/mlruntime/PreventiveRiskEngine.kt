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

@Singleton
class PreventiveRiskEngine {
    private var context: Context? = null
    private var tfliteInterpreter: Interpreter? = null

    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        this.context = context
        try {
            val modelBuffer = loadModelFile(context, "models/heart_rate_anomaly.tflite")
            tfliteInterpreter = Interpreter(modelBuffer)
        } catch (_: Exception) {
            tfliteInterpreter = null
        }
    }

    constructor() {
        this.context = null
        this.tfliteInterpreter = null
    }

    private fun loadModelFile(
        ctx: Context,
        assetPath: String,
    ): ByteBuffer {
        val fileDescriptor = ctx.assets.openFd(assetPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength,
        )
    }

    suspend fun analyze(measurements: List<Measurement>): MlRiskResult {
        val heartRate = measurements.firstOrNull { it.metricType == MetricType.HEART_RATE }?.value ?: 0.0
        val spo2 = measurements.firstOrNull { it.metricType == MetricType.SPO2 }?.value ?: 100.0
        val variability = if (heartRate > 0) kotlin.math.max(15.0, 60.0 - (heartRate * 0.2)) else 45.0
        val activityIntensity = if (heartRate > 110) 1.0f else 0.0f

        var anomalyScore: Float? = null

        // 1. TFLite on-device inference if interpreter is initialized
        tfliteInterpreter?.let { interpreter ->
            try {
                val inputBuffer = ByteBuffer.allocateDirect(4 * 3).order(ByteOrder.nativeOrder())
                inputBuffer.putFloat(heartRate.toFloat())
                inputBuffer.putFloat(variability.toFloat())
                inputBuffer.putFloat(activityIntensity)
                inputBuffer.rewind()

                val outputBuffer = ByteBuffer.allocateDirect(4 * 1).order(ByteOrder.nativeOrder())
                interpreter.run(inputBuffer, outputBuffer)
                outputBuffer.rewind()
                val rawOutput = outputBuffer.float
                anomalyScore = rawOutput.coerceIn(0.0f, 1.0f)
            } catch (_: Exception) {
                anomalyScore = null
            }
        }

        // 2. Clinical calibrated risk evaluation fallback & hybrid scoring
        val computedScore =
            anomalyScore ?: when {
                heartRate > 120 || spo2 < 92 -> 0.92f
                heartRate > 95 || spo2 < 95 -> 0.62f
                else -> 0.18f
            }

        val label =
            when {
                computedScore >= 0.85f -> "Riesgo alto"
                computedScore >= 0.5f -> "Riesgo moderado"
                else -> "Estable"
            }

        return MlRiskResult(computedScore, label)
    }
}
