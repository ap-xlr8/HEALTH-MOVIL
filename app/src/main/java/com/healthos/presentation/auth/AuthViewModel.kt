package com.healthos.presentation.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.Role
import com.healthos.domain.repository.AuthRepository
import com.healthos.security.BiometricAuthManager
import com.healthos.security.BiometricStatus
import com.healthos.security.SecureTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val secureTokenStore: SecureTokenStore,
        private val biometricAuthManager: BiometricAuthManager,
    ) : ViewModel() {
        val session = authRepository.session
        private val _authState = MutableStateFlow(AuthUiState())
        val authState = _authState.asStateFlow()

        private val _loginResult = MutableStateFlow<Boolean?>(null)
        val loginResult = _loginResult.asStateFlow()

        private val _isBiometricEnabled = MutableStateFlow(secureTokenStore.isBiometricEnabled())
        val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

        private val _showBiometricOptIn = MutableStateFlow(false)
        val showBiometricOptIn = _showBiometricOptIn.asStateFlow()

        private val _isSessionUnlocked = MutableStateFlow(!secureTokenStore.isBiometricEnabled())
        val isSessionUnlocked = _isSessionUnlocked.asStateFlow()

        private val _biometricError = MutableStateFlow<String?>(null)
        val biometricError = _biometricError.asStateFlow()

        fun clearLoginResult() {
            _loginResult.value = null
        }

        fun register(
            email: String,
            password: String,
            role: Role,
            firstName: String,
            lastName: String,
        ) = run {
            authRepository.register(email, password, role, firstName, lastName)
        }

        fun login(
            email: String,
            password: String,
        ) {
            viewModelScope.launch {
                _authState.value = AuthUiState(loading = true)
                _loginResult.value = null
                try {
                    val twoFactorRequired = authRepository.login(email, password)
                    _loginResult.value = twoFactorRequired
                    _authState.value = AuthUiState()
                } catch (error: Throwable) {
                    _authState.value = AuthUiState(error = error.message ?: "Error inesperado")
                }
            }
        }

        fun verifyEmail(
            email: String,
            code: String,
        ) = run {
            authRepository.verifyEmail(email, code)
        }

        fun verify2FA(
            email: String,
            code: String,
        ) {
            viewModelScope.launch {
                _authState.value = AuthUiState(loading = true)
                try {
                    val newSession = authRepository.verify2FA(email, code)
                    _authState.value = AuthUiState()
                    _isSessionUnlocked.value = true

                    // Consulta si se debe ofrecer biometría tras primer inicio exitoso
                    val isAvailable = biometricAuthManager.checkBiometricAvailability() == BiometricStatus.AVAILABLE
                    if (isAvailable && !secureTokenStore.isBiometricEnabled() && !secureTokenStore.isBiometricOptInDismissed()) {
                        _showBiometricOptIn.value = true
                    }
                } catch (error: Throwable) {
                    _authState.value = AuthUiState(error = error.message ?: "Error inesperado")
                }
            }
        }

        fun resend2FA(email: String) =
            run {
                authRepository.resend2FA(email)
            }

        fun forgotPassword(email: String) =
            run {
                authRepository.forgotPassword(email)
            }

        fun saveHealthProfile(
            weightKg: Double,
            heightCm: Int,
            bloodType: String,
        ) = run {
            authRepository.saveHealthProfile(HealthProfile(weightKg, heightCm, bloodType))
        }

        fun unlockWithBiometric(activity: FragmentActivity) {
            _biometricError.value = null
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Desbloquear Health OS",
                subtitle = "Usa tu huella dactilar o Face Unlock para continuar",
                negativeButtonText = null,
                onSuccess = {
                    _isSessionUnlocked.value = true
                    _biometricError.value = null
                },
                onError = { _, errString ->
                    _biometricError.value = errString.toString()
                },
                onFailed = {
                    _biometricError.value = "Biometría no reconocida. Inténtalo de nuevo."
                },
            )
        }

        fun enableBiometricFromOptIn(activity: FragmentActivity) {
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Confirmar Biometría",
                subtitle = "Confirma tu huella para habilitar accesos rápidos futuros",
                negativeButtonText = null,
                onSuccess = {
                    secureTokenStore.setBiometricEnabled(true)
                    secureTokenStore.setBiometricOptInDismissed(true)
                    _isBiometricEnabled.value = true
                    _showBiometricOptIn.value = false
                    _isSessionUnlocked.value = true
                },
                onError = { _, errString ->
                    _authState.value = AuthUiState(error = "No se pudo activar biometría: $errString")
                    _showBiometricOptIn.value = false
                },
            )
        }

        fun dismissBiometricOptIn() {
            secureTokenStore.setBiometricOptInDismissed(true)
            _showBiometricOptIn.value = false
        }

        fun setBiometricEnabled(enabled: Boolean, activity: FragmentActivity? = null) {
            if (enabled && activity != null) {
                biometricAuthManager.authenticate(
                    activity = activity,
                    title = "Activar Biometría",
                    subtitle = "Confirma tu identidad para habilitar biometría",
                    negativeButtonText = null,
                    onSuccess = {
                        secureTokenStore.setBiometricEnabled(true)
                        _isBiometricEnabled.value = true
                    },
                    onError = { _, errString ->
                        _authState.value = AuthUiState(error = "Error al activar biometría: $errString")
                    },
                )
            } else {
                secureTokenStore.setBiometricEnabled(false)
                _isBiometricEnabled.value = false
            }
        }

        fun logout() {
            viewModelScope.launch {
                authRepository.logout()
                _isSessionUnlocked.value = !secureTokenStore.isBiometricEnabled()
            }
        }

        private fun run(block: suspend () -> Any) {
            viewModelScope.launch {
                _authState.value = AuthUiState(loading = true)
                _authState.value =
                    try {
                        block()
                        AuthUiState()
                    } catch (error: Throwable) {
                        AuthUiState(error = error.message ?: "Error inesperado")
                    }
            }
        }
    }
