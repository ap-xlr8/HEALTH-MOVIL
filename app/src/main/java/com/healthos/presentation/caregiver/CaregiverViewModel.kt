package com.healthos.presentation.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.repository.CaregiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CaregiverViewModel
    @Inject
    constructor(
        caregiverRepository: CaregiverRepository,
    ) : ViewModel() {
        val patients: StateFlow<List<PatientSummary>> =
            caregiverRepository.patients()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }
