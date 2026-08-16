package com.healthos.presentation.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthos.domain.model.CaregiverProfile
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.repository.CaregiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaregiverViewModel
    @Inject
    constructor(
        private val caregiverRepository: CaregiverRepository,
    ) : ViewModel() {
        val patients: StateFlow<List<PatientSummary>> =
            caregiverRepository.patients()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val caregiverProfile: StateFlow<CaregiverProfile?> =
            caregiverRepository.caregiverProfile()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun saveCaregiverProfile(profile: CaregiverProfile) {
            viewModelScope.launch {
                caregiverRepository.saveCaregiverProfile(profile)
            }
        }
    }

