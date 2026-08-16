package com.healthos.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED,
}

@Singleton
class BiometricAuthManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val KEY_NAME = "healthos_biometric_auth_key"
            private const val ANDROID_KEYSTORE = "AndroidKeyStore"
            private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        }

        fun checkBiometricAvailability(): BiometricStatus {
            val biometricManager = BiometricManager.from(context)
            return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
                else -> BiometricStatus.UNSUPPORTED
            }
        }

        fun authenticate(
            activity: FragmentActivity,
            title: String = "Autenticación de Seguridad",
            subtitle: String = "Confirma tu identidad biométrica para acceder a Health OS",
            negativeButtonText: String? = "Usar contraseña",
            onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
            onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
            onFailed: () -> Unit = {},
        ) {
            val executor = ContextCompat.getMainExecutor(activity)
            val promptInfoBuilder =
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)

            if (negativeButtonText != null) {
                promptInfoBuilder.setNegativeButtonText(negativeButtonText)
                promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            } else {
                promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            }

            val biometricPrompt =
                BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess(result)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errorCode, errString)
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onFailed()
                        }
                    },
                )

            biometricPrompt.authenticate(promptInfoBuilder.build())
        }

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_NAME)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val keyGenParameterSpec =
                    KeyGenParameterSpec.Builder(
                        KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(false)
                        .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
            return keyStore.getKey(KEY_NAME, null) as SecretKey
        }

        fun createCryptoObject(): BiometricPrompt.CryptoObject? {
            return try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val key = getOrCreateSecretKey()
                cipher.init(Cipher.ENCRYPT_MODE, key)
                BiometricPrompt.CryptoObject(cipher)
            } catch (_: Exception) {
                null
            }
        }
    }
