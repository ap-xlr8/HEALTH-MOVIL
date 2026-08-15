package com.healthos.bluetooth

object HeartRateParser {
    fun parse(payload: ByteArray?): Int? {
        if (payload == null || payload.size < 2) return null
        val flags = payload[0].toInt()
        return if ((flags and 0x01) != 0 && payload.size >= 3) {
            (payload[1].toInt() and 0xFF) or ((payload[2].toInt() and 0xFF) shl 8)
        } else {
            payload[1].toInt() and 0xFF
        }
    }
}
