package com.healthos

import com.healthos.bluetooth.HeartRateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateParserTest {
    @Test
    fun parsesEightBitHeartRate() {
        assertEquals(78, HeartRateParser.parse(byteArrayOf(0x00, 78)))
    }

    @Test
    fun parsesSixteenBitHeartRate() {
        assertEquals(300, HeartRateParser.parse(byteArrayOf(0x01, 0x2C, 0x01)))
    }

    @Test
    fun rejectsEmptyPayload() {
        assertNull(HeartRateParser.parse(byteArrayOf()))
    }
}
