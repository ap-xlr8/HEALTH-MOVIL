package com.healthos.bluetooth

interface WearableAdapter {
    val name: String

    fun canHandle(model: String): Boolean

    fun parseMeasurement(payload: ByteArray): BleMeasurement?
}

class GenericBleAdapter : WearableAdapter {
    override val name = "GenericBLEAdapter"

    override fun canHandle(model: String) = true

    override fun parseMeasurement(payload: ByteArray) = HeartRateParser.parse(payload)?.let(::BleMeasurement)
}

class XiaomiBandAdapter : WearableAdapter {
    override val name = "XiaomiBandAdapter"

    override fun canHandle(model: String) = model.contains("xiaomi", ignoreCase = true)

    override fun parseMeasurement(payload: ByteArray) = HeartRateParser.parse(payload)?.let(::BleMeasurement)
}

class GarminAdapter : WearableAdapter {
    override val name = "GarminAdapter"

    override fun canHandle(model: String) = model.contains("garmin", ignoreCase = true)

    override fun parseMeasurement(payload: ByteArray) = HeartRateParser.parse(payload)?.let(::BleMeasurement)
}
