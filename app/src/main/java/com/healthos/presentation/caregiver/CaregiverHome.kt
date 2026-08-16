package com.healthos.presentation.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthos.domain.model.AlertStatus
import com.healthos.domain.model.PatientSummary
import com.healthos.presentation.HealthScaffold
import com.healthos.presentation.InfoCard
import com.healthos.presentation.common.ProvideWindowSizeInfo
import com.healthos.presentation.theme.AmberDeep
import com.healthos.presentation.theme.AmberWarning
import com.healthos.presentation.theme.BlueBright
import com.healthos.presentation.theme.BorderMedium
import com.healthos.presentation.theme.BorderSubtle
import com.healthos.presentation.theme.CardShape
import com.healthos.presentation.theme.CoralBright
import com.healthos.presentation.theme.CoralCritical
import com.healthos.presentation.theme.CoralDeep
import com.healthos.presentation.theme.HealthBadge
import com.healthos.presentation.theme.HealthCard
import com.healthos.presentation.theme.MintDeep
import com.healthos.presentation.theme.MintSuccess
import com.healthos.presentation.theme.PanelDeep
import com.healthos.presentation.theme.PanelSurface
import com.healthos.presentation.theme.StatusDot
import com.healthos.presentation.theme.SurfaceElevated
import com.healthos.presentation.theme.TealBright
import com.healthos.presentation.theme.TealContainer
import com.healthos.presentation.theme.TextPrimary
import com.healthos.presentation.theme.TextSecondary
import com.healthos.presentation.theme.TextTertiary

@Composable
fun CaregiverHome(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    viewModel: CaregiverViewModel = hiltViewModel(),
) {
    val patients by viewModel.patients.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPatientId by remember { mutableStateOf<String?>(null) }
    val selectedPatient = patients.firstOrNull { it.id == selectedPatientId } ?: patients.firstOrNull()
    val tabs = listOf("Pacientes", "Detalle", "Alertas", "Cuenta")

    ProvideWindowSizeInfo { sizeInfo ->
        if (sizeInfo.isLandscape || !sizeInfo.isCompact) {
            // Master-Detail split view for wide screens and landscape
            HealthScaffold(
                title = "Cuidador • Monitor",
                tabs = listOf("Dashboard", "Cuenta"),
                selected = selectedTab.coerceAtMost(1),
                onSelect = { selectedTab = it },
                onLogout = onLogout,
                modifier = modifier,
            ) { contentModifier ->
                if (selectedTab == 0) {
                    CaregiverSplitView(
                        modifier = contentModifier.padding(16.dp),
                        patients = patients,
                        selectedPatient = selectedPatient,
                        onSelectPatient = { selectedPatientId = it.id },
                    )
                } else {
                    Box(
                        modifier = contentModifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            InfoCard("Cuenta de Cuidador", "Sesión de cuidador clínica activa y sincronizada en tiempo real.")
                            InfoCard("Seguridad", "Cifrado de extremo a extremo y autenticación de doble factor 2FA activos.")
                        }
                    }
                }
            }
        } else {
            // Compact Tabbed layout for portrait mobile
            HealthScaffold(
                title = "Panel Cuidador",
                tabs = tabs,
                selected = selectedTab,
                onSelect = { selectedTab = it },
                onLogout = onLogout,
                modifier = modifier,
            ) { contentModifier ->
                Box(
                    modifier = contentModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        when (selectedTab) {
                            0 -> PatientListScreen(
                                patients = patients,
                                selectedPatient = selectedPatient,
                                onSelectPatient = {
                                    selectedPatientId = it.id
                                    selectedTab = 1
                                },
                            )
                            1 -> PatientDetailScreen(selectedPatient)
                            2 -> CaregiverAlertsScreen(patients)
                            3 -> {
                                InfoCard("Cuenta de Cuidador", "Sesión de cuidador clínica activa (JWT Bearer Token).")
                                InfoCard("Protocolo de Triaje", "Alertas automáticas en caso de arritmia o hipoxia.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaregiverSplitView(
    modifier: Modifier,
    patients: List<PatientSummary>,
    selectedPatient: PatientSummary?,
    onSelectPatient: (PatientSummary) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Left Master Pane: Patient List
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Text(
                text = "Pacientes Monitoreados",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(12.dp))
            if (patients.isEmpty()) {
                InfoCard("Pacientes", "No tienes pacientes asignados todavía.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(patients) { patient ->
                        val isSelected = patient.id == selectedPatient?.id
                        PatientCard(
                            patient = patient,
                            isSelected = isSelected,
                            onClick = { onSelectPatient(patient) },
                        )
                    }
                }
            }
        }

        // Right Detail Pane: Patient Details & Telemetry
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Telemetría y Signos Vitales",
                color = BlueBright,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            if (selectedPatient != null) {
                PatientDetailCard(selectedPatient)
            } else {
                InfoCard("Detalle", "Selecciona un paciente de la lista para inspeccionar sus métricas.")
            }
        }
    }
}

@Composable
private fun PatientListScreen(
    patients: List<PatientSummary>,
    selectedPatient: PatientSummary?,
    onSelectPatient: (PatientSummary) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pacientes Asignados",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                HealthBadge(
                    text = "${patients.size} PACIENTES",
                    color = BlueBright,
                    backgroundColor = PanelDeep,
                    fontSize = 10.sp,
                )
            }
        }

        if (patients.isEmpty()) {
            item {
                InfoCard("Pacientes", "No tienes pacientes asignados en este momento.")
            }
        } else {
            items(patients) { patient ->
                PatientCard(
                    patient = patient,
                    isSelected = patient.id == selectedPatient?.id,
                    onClick = { onSelectPatient(patient) },
                )
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient: PatientSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isCritical = patient.status == AlertStatus.CRITICAL
    val isAlert = patient.status == AlertStatus.ALERT
    val statusColor = if (isCritical) CoralBright else if (isAlert) AmberWarning else MintSuccess
    val statusBg = if (isCritical) CoralDeep else if (isAlert) AmberDeep else MintDeep

    HealthCard(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = TealBright.copy(alpha = 0.2f)),
            onClick = onClick,
        ),
        containerColor = if (isSelected) SurfaceElevated else PanelSurface,
        borderColor = if (isSelected) TealBright else if (isCritical) CoralBright.copy(alpha = 0.6f) else BorderSubtle,
        borderWidth = if (isSelected) 1.5.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(statusBg)
                    .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isCritical || isAlert) Icons.Filled.Warning else Icons.Filled.Person,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(patient.firstName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    patient.latestMeasurement?.let { "${it.metricType.name}: ${it.value} ${it.unit}" } ?: "Sin registros recientes",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
            HealthBadge(
                text = patient.status.name,
                color = statusColor,
                backgroundColor = statusBg,
                fontSize = 11.sp,
                hasDot = isCritical,
            )
        }
    }
}

@Composable
private fun PatientDetailScreen(patient: PatientSummary?) {
    if (patient == null) {
        InfoCard("Detalle", "Selecciona un paciente para ver su información clínica.")
    } else {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PatientDetailCard(patient)
        }
    }
}

@Composable
private fun PatientDetailCard(patient: PatientSummary) {
    val isCritical = patient.status == AlertStatus.CRITICAL
    val isAlert = patient.status == AlertStatus.ALERT
    val statusColor = if (isCritical) CoralBright else if (isAlert) AmberWarning else MintSuccess
    val statusBg = if (isCritical) CoralDeep else if (isAlert) AmberDeep else MintDeep

    HealthCard(
        containerColor = PanelSurface,
        borderColor = BorderMedium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = patient.firstName,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "ID: ${patient.id}",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            HealthBadge(
                text = patient.status.name,
                color = statusColor,
                backgroundColor = statusBg,
                fontSize = 11.sp,
                hasDot = isCritical,
            )
        }

        Spacer(Modifier.height(16.dp))

        patient.latestMeasurement?.let { measurement ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelDeep)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CoralDeep),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Favorite, null, tint = CoralBright, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(measurement.metricType.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Última telemetría", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = "${measurement.value} ${measurement.unit}",
                        color = TealBright,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaregiverAlertsScreen(patients: List<PatientSummary>) {
    val alertPatients = patients.filter { it.status != AlertStatus.NORMAL }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Alertas Clínicas de Pacientes",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            HealthBadge(
                text = "${alertPatients.size} ACTIVAS",
                color = if (alertPatients.isNotEmpty()) CoralBright else MintSuccess,
                backgroundColor = if (alertPatients.isNotEmpty()) CoralDeep else MintDeep,
                fontSize = 10.sp,
            )
        }

        if (alertPatients.isEmpty()) {
            InfoCard("Alertas", "No hay pacientes con eventos críticos o anomalías en este momento.")
        } else {
            alertPatients.forEach { patient ->
                val isCritical = patient.status == AlertStatus.CRITICAL
                HealthCard(
                    containerColor = PanelSurface,
                    borderColor = if (isCritical) CoralBright else AmberWarning,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isCritical) CoralDeep else AmberDeep),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                null,
                                tint = if (isCritical) CoralBright else AmberWarning,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(patient.firstName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(2.dp))
                            Text("Triaje: ${patient.status.name}", color = if (isCritical) CoralBright else AmberWarning, fontSize = 12.sp)
                        }
                        HealthBadge(
                            text = if (isCritical) "URGENTE" else "ATENCIÓN",
                            color = if (isCritical) CoralBright else AmberWarning,
                            backgroundColor = if (isCritical) CoralDeep else AmberDeep,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}
