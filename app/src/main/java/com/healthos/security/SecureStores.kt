package com.healthos.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SecureTokenStore {
    private var prefs: android.content.SharedPreferences? = null

    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        prefs =
            EncryptedSharedPreferences.create(
                context,
                "healthos_secure_tokens",
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
    }

    constructor() {
        prefs = null
    }

    open fun save(
        accessToken: String,
        refreshToken: String,
        role: String,
        userId: String? = null,
    ) {
        prefs?.edit()
            ?.putString("access_token", accessToken)
            ?.putString("refresh_token", refreshToken)
            ?.putString("role", role)
            ?.apply {
                if (userId != null) putString("user_id", userId)
            }
            ?.apply()
    }

    open fun accessToken(): String? = prefs?.getString("access_token", null)

    open fun refreshToken(): String? = prefs?.getString("refresh_token", null)

    open fun role(): String? = prefs?.getString("role", null)

    open fun userId(): String? = prefs?.getString("user_id", null)

    open fun saveHealthProfile(
        weightKg: Double,
        heightCm: Int,
        bloodType: String,
    ) {
        prefs?.edit()
            ?.putString("health_weight_kg", weightKg.toString())
            ?.putString("health_height_cm", heightCm.toString())
            ?.putString("health_blood_type", bloodType)
            ?.apply()
    }

    open fun healthWeightKg(): Double? = prefs?.getString("health_weight_kg", null)?.toDoubleOrNull()

    open fun healthHeightCm(): Int? = prefs?.getString("health_height_cm", null)?.toIntOrNull()

    open fun healthBloodType(): String? = prefs?.getString("health_blood_type", null)

    open fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}

@Singleton
class DatabasePassphraseProvider
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs =
            EncryptedSharedPreferences.create(
                context,
                "healthos_database_key",
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        fun passphrase(): ByteArray {
            val existing = prefs.getString("db_passphrase", null)
            if (existing != null) return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            prefs.edit()
                .putString("db_passphrase", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                .apply()
            return bytes
        }
    }
