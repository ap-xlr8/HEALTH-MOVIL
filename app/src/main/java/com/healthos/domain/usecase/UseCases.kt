package com.healthos.domain.usecase

import com.healthos.domain.model.Role
import com.healthos.domain.model.SosLocation
import com.healthos.domain.repository.AuthRepository
import com.healthos.domain.repository.PatientRepository
import javax.inject.Inject

class LoginUseCase
    @Inject
    constructor(private val authRepository: AuthRepository) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ) = authRepository.login(email.trim(), password)
    }

class RegisterUseCase
    @Inject
    constructor(private val authRepository: AuthRepository) {
        suspend operator fun invoke(
            email: String,
            password: String,
            role: Role,
            firstName: String,
            lastName: String,
        ) = authRepository.register(email.trim(), password, role, firstName.trim(), lastName.trim())
    }

class TriggerSosUseCase
    @Inject
    constructor(private val patientRepository: PatientRepository) {
        suspend operator fun invoke(
            lat: Double,
            lng: Double,
        ) = patientRepository.triggerSos(SosLocation(lat, lng))
    }
