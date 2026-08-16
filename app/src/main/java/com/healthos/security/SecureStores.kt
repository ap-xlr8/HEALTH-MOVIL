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
        rhFactor: String = "+",
        birthDate: String = "",
        gender: String = "No especificado",
        emergencyContactName: String = "",
        emergencyContactPhone: String = "",
        emergencyContactRelation: String = "",
        insuranceProvider: String = "",
        policyNumber: String = "",
    ) {
        prefs?.edit()
            ?.putString("health_weight_kg", weightKg.toString())
            ?.putString("health_height_cm", heightCm.toString())
            ?.putString("health_blood_type", bloodType)
            ?.putString("health_rh_factor", rhFactor)
            ?.putString("health_birth_date", birthDate)
            ?.putString("health_gender", gender)
            ?.putString("health_emerg_name", emergencyContactName)
            ?.putString("health_emerg_phone", emergencyContactPhone)
            ?.putString("health_emerg_rel", emergencyContactRelation)
            ?.putString("health_insurance", insuranceProvider)
            ?.putString("health_policy", policyNumber)
            ?.apply()
    }

    open fun healthWeightKg(): Double? = prefs?.getString("health_weight_kg", null)?.toDoubleOrNull()
    open fun healthHeightCm(): Int? = prefs?.getString("health_height_cm", null)?.toIntOrNull()
    open fun healthBloodType(): String? = prefs?.getString("health_blood_type", null)
    open fun healthRhFactor(): String = prefs?.getString("health_rh_factor", "+") ?: "+"
    open fun healthBirthDate(): String = prefs?.getString("health_birth_date", "") ?: ""
    open fun healthGender(): String = prefs?.getString("health_gender", "No especificado") ?: "No especificado"
    open fun healthEmergencyName(): String = prefs?.getString("health_emerg_name", "") ?: ""
    open fun healthEmergencyPhone(): String = prefs?.getString("health_emerg_phone", "") ?: ""
    open fun healthEmergencyRelation(): String = prefs?.getString("health_emerg_rel", "") ?: ""
    open fun healthInsuranceProvider(): String = prefs?.getString("health_insurance", "") ?: ""
    open fun healthPolicyNumber(): String = prefs?.getString("health_policy", "") ?: ""

    open fun saveUserProfile(firstName: String, lastName: String, email: String, phone: String) {
        prefs?.edit()
            ?.putString("user_first_name", firstName)
            ?.putString("user_last_name", lastName)
            ?.putString("user_email", email)
            ?.putString("user_phone", phone)
            ?.apply()
    }

    open fun userFirstName(): String? = prefs?.getString("user_first_name", null)
    open fun userLastName(): String? = prefs?.getString("user_last_name", null)
    open fun userEmail(): String? = prefs?.getString("user_email", null)
    open fun userPhone(): String? = prefs?.getString("user_phone", null)

    open fun themeMode(): String = prefs?.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    open fun setThemeMode(mode: String) {
        prefs?.edit()?.putString("theme_mode", mode)?.apply()
    }

    open fun accentColor(): String = prefs?.getString("accent_color", "TEAL") ?: "TEAL"
    open fun setAccentColor(color: String) {
        prefs?.edit()?.putString("accent_color", color)?.apply()
    }

    open fun isBiometricEnabled(): Boolean = prefs?.getBoolean("biometric_auth_enabled", false) ?: false

    open fun setBiometricEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("biometric_auth_enabled", enabled)?.apply()
    }

    open fun isBiometricOptInDismissed(): Boolean = prefs?.getBoolean("biometric_opt_in_dismissed", false) ?: false

    open fun setBiometricOptInDismissed(dismissed: Boolean) {
        prefs?.edit()?.putBoolean("biometric_opt_in_dismissed", dismissed)?.apply()
    }

    open fun clear() {
        val bioEnabled = isBiometricEnabled()
        val bioDismissed = isBiometricOptInDismissed()
        val theme = themeMode()
        val accent = accentColor()
        prefs?.edit()?.clear()?.apply()
        // Preserve user preferences for UI and biometrics
        setBiometricEnabled(bioEnabled)
        setBiometricOptInDismissed(bioDismissed)
        setThemeMode(theme)
        setAccentColor(accent)
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
