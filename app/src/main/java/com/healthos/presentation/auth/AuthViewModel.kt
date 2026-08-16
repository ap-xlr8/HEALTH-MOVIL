package com.healthos.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.Role
import com.healthos.domain.repository.AuthRepository
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
    ) : ViewModel() {
        val session = authRepository.session
        private val _authState = MutableStateFlow(AuthUiState())
        val authState = _authState.asStateFlow()

        private val _loginResult = MutableStateFlow<Boolean?>(null)
        val loginResult = _loginResult.asStateFlow()

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
        ) = run {
            authRepository.verify2FA(email, code)
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

        fun logout() {
            viewModelScope.launch { authRepository.logout() }
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
