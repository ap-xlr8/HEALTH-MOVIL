package com.healthos.observability

import android.content.Context
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

object HealthLogger {
    private const val TAG = "HealthOS"

    fun d(message: String, vararg args: Any?) {
        Log.d(TAG, sanitize(String.format(message, *args)))
    }

    fun i(message: String, vararg args: Any?) {
        Log.i(TAG, sanitize(String.format(message, *args)))
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, sanitize(message), throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, sanitize(message), throwable)
    }

    /**
     * Redacta información médica protegida (PHI), correos, tokens JWT y contraseñas.
     */
    fun sanitize(raw: String): String {
        return raw
            .replace(Regex("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+"), "[REDACTED_EMAIL]")
            .replace(Regex("eyJ[a-zA-Z0-9-_]+\\.eyJ[a-zA-Z0-9-_]+\\.[a-zA-Z0-9-_]+"), "[REDACTED_JWT]")
            .replace(Regex("(?i)(password|token|clave|secret)\\s*[:=]\\s*[^\\s,]+"), "$1=[REDACTED]")
    }
}

@Singleton
class BatteryPerformanceMonitor
    @Inject
    constructor() {
        private var lastTraceTimestamp = SystemClock.elapsedRealtime()

        fun checkBatteryLevel(context: Context): Int {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        }

        inline fun <T> traceExecution(operationName: String, block: () -> T): T {
            val start = SystemClock.elapsedRealtime()
            try {
                return block()
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - start
                HealthLogger.d("Performance Trace [$operationName]: %d ms", elapsed)
            }
        }
    }

@Singleton
class FeatureFlagManager
    @Inject
    constructor() {
        private val flags = mutableMapOf(
            "FEATURE_ML_MULTI_MODEL" to true,
            "FEATURE_CRITICAL_SYNC_BYPASS" to true,
            "FEATURE_EXTENDED_CLINICAL_HISTORY" to true,
            "FEATURE_BLE_EDA_TEMP" to true,
            "FEATURE_DARK_THEME_OVERRIDE" to true,
            "FEATURE_REMOTE_CONFIG_POLLING" to true,
        )

        fun isEnabled(flagKey: String): Boolean {
            return flags[flagKey] ?: false
        }

        fun setFlag(flagKey: String, enabled: Boolean) {
            flags[flagKey] = enabled
        }
    }
