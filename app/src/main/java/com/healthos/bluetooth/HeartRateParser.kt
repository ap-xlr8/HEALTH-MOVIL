package com.healthos.bluetooth

import kotlin.math.pow
import kotlin.math.sqrt

data class ParsedHeartRateResult(
    val heartRate: Int,
    val rrIntervalsMs: List<Int> = emptyList(),
    val rmssd: Double? = null,
    val sdnn: Double? = null,
)

object HeartRateParser {
    fun parse(payload: ByteArray?): Int? {
        return parseDetailed(payload)?.heartRate
    }

    fun parseDetailed(payload: ByteArray?): ParsedHeartRateResult? {
        if (payload == null || payload.size < 2) return null
        val flags = payload[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0
        val isEnergyExpendedPresent = (flags and 0x08) != 0
        val isRrIntervalPresent = (flags and 0x10) != 0

        var offset = 1
        val heartRate: Int = if (is16Bit && payload.size >= 3) {
            val hr = (payload[offset].toInt() and 0xFF) or ((payload[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
            hr
        } else {
            val hr = payload[offset].toInt() and 0xFF
            offset += 1
            hr
        }

        if (isEnergyExpendedPresent && payload.size >= offset + 2) {
            offset += 2
        }

        val rrIntervals = mutableListOf<Int>()
        if (isRrIntervalPresent) {
            while (payload.size >= offset + 2) {
                val rawRr = (payload[offset].toInt() and 0xFF) or ((payload[offset + 1].toInt() and 0xFF) shl 8)
                // In GATT standard 0x2A37, RR intervals are in 1/1024 seconds units
                val rrMs = (rawRr * 1000) / 1024
                if (rrMs in 200..2000) {
                    rrIntervals.add(rrMs)
                }
                offset += 2
            }
        }

        val rmssd = if (rrIntervals.size >= 2) calculateRmssd(rrIntervals) else null
        val sdnn = if (rrIntervals.size >= 2) calculateSdnn(rrIntervals) else null

        return ParsedHeartRateResult(
            heartRate = heartRate,
            rrIntervalsMs = rrIntervals,
            rmssd = rmssd,
            sdnn = sdnn,
        )
    }

    fun calculateRmssd(rrIntervalsMs: List<Int>): Double {
        if (rrIntervalsMs.size < 2) return 0.0
        var sumSquares = 0.0
        for (i in 0 until rrIntervalsMs.size - 1) {
            val diff = (rrIntervalsMs[i + 1] - rrIntervalsMs[i]).toDouble()
            sumSquares += diff.pow(2.0)
        }
        return sqrt(sumSquares / (rrIntervalsMs.size - 1))
    }

    fun calculateSdnn(rrIntervalsMs: List<Int>): Double {
        if (rrIntervalsMs.size < 2) return 0.0
        val mean = rrIntervalsMs.average()
        val variance = rrIntervalsMs.map { (it - mean).pow(2.0) }.average()
        return sqrt(variance)
    }
}

