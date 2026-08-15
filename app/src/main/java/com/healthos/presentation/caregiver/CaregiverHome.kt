package com.healthos.presentation.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthos.domain.model.AlertStatus
import com.healthos.domain.model.PatientSummary
import com.healthos.presentation.HealthScaffold
import com.healthos.presentation.InfoCard
import com.healthos.presentation.common.ProvideWindowSizeInfo
import com.healthos.presentation.common.WindowSizeInfo

private val Panel = Color(0xFF121B2D)
private val PanelDeep = Color(0xFF050A18)
private val StrokeLine = Color(0xFF26344E)
private val TextMain = Color.White
private val TextMuted = Color(0xFFA8B7D2)
private val TealBright = Color(0xFF19E3BC)
private val Blue = Color(0xFF72B7FF)
private val PinkSoft = Color(0xFFFF6F91)
private val Yellow = Color(0xFFFFC247)

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
            // Split View / Master-Detail layout for wide screens and landscape
            HealthScaffold("Cuidador • Monitor", listOf("Dashboard", "Cuenta"), selectedTab.coerceAtMost(1), { selectedTab = it }, onLogout, modifier) { contentModifier ->
                if (selectedTab == 0) {
                    CaregiverSplitView(
                        modifier = contentModifier,
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
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp),
                        ) {
                            InfoCard("Cuenta de Cuidador", "Sesión de cuidador activa y sincronizada con el panel clínico.")
                            InfoCard("Seguridad", "Cifrado de extremo a extremo activado.")
                        }
                    }
                }
            }
        } else {
            // Compact Tabbed layout for mobile portrait
            HealthScaffold("Cuidador", tabs, selectedTab, { selectedTab = it }, onLogout, modifier) { contentModifier ->
                Box(
                    modifier = contentModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    ) {
                        when (selectedTab) {
                            0 ->
                                PatientListScreen(
                                    patients = patients,
                                    selectedPatient = selectedPatient,
                                    onSelectPatient = {
                                        selectedPatientId = it.id
                                        selectedTab = 1
                                    },
                                )
                            1 -> PatientDetailScreen(selectedPatient)
                            2 -> CaregiverAlertsScreen(patients)
                            3 -> InfoCard("Cuenta", "Sesión de cuidador activa (JWT Bearer Token)")
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
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        ) {
            Text("Pacientes Monitoreados", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
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

        // Right Detail Pane: Patient Details & Telemetry
        Column(
            modifier =
                Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Detalle del Paciente", color = Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (selectedPatient != null) {
                PatientDetailCard(selectedPatient)
            } else {
                InfoCard("Detalle", "Selecciona un paciente para ver su telemetría en tiempo real.")
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
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Pacientes Asignados", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        items(patients) { patient ->
            PatientCard(
                patient = patient,
                isSelected = patient.id == selectedPatient?.id,
                onClick = { onSelectPatient(patient) },
            )
        }
    }
}

@Composable
private fun PatientCard(
    patient: PatientSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isAlert = patient.status != AlertStatus.NORMAL
    val borderColor = if (isSelected) TealBright else if (isAlert) PinkSoft else StrokeLine

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF16253D) else Panel),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isAlert) PinkSoft.copy(alpha = 0.2f) else TealBright.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isAlert) Icons.Filled.Warning else Icons.Filled.Person,
                    contentDescription = null,
                    tint = if (isAlert) PinkSoft else TealBright,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(patient.firstName, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    patient.latestMeasurement?.let { "${it.metricType.name}: ${it.value} ${it.unit}" } ?: "Sin mediciones recientes",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            }
            Text(
                text = patient.status.name,
                color = if (isAlert) PinkSoft else TealBright,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PatientDetailScreen(patient: PatientSummary?) {
    if (patient == null) {
        InfoCard("Detalle", "Selecciona un paciente para ver su información.")
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
    val isAlert = patient.status != AlertStatus.NORMAL

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, StrokeLine, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(patient.firstName, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAlert) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (isAlert) PinkSoft else TealBright,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        patient.status.name,
                        color = if (isAlert) PinkSoft else TealBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            patient.latestMeasurement?.let { m ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PanelDeep)
                            .border(1.dp, StrokeLine, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, null, tint = PinkSoft, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(m.metricType.name, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("${m.value} ${m.unit}", color = TealBright, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("ID Paciente: ${patient.id}", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CaregiverAlertsScreen(patients: List<PatientSummary>) {
    val alertPatients = patients.filter { it.status != AlertStatus.NORMAL }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Alertas Clínicas", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        if (alertPatients.isEmpty()) {
            InfoCard("Alertas", "No hay pacientes con eventos críticos en este momento.")
        } else {
            alertPatients.forEach {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, PinkSoft, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Panel),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = PinkSoft, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(it.firstName, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Estado: ${it.status.name}", color = PinkSoft, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
