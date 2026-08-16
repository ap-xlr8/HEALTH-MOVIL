package com.healthos.presentation.patient.clinical

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.healthos.domain.model.Allergy
import com.healthos.domain.model.AllergyCategory
import com.healthos.domain.model.AllergySeverity
import com.healthos.domain.model.FamilyHistory
import com.healthos.domain.model.GynecoObstetricHistory
import com.healthos.domain.model.Kinship
import com.healthos.domain.model.LifestyleHabits
import com.healthos.domain.model.Medication
import com.healthos.domain.model.PathologicalHistory
import com.healthos.domain.model.PathologicalType
import com.healthos.presentation.patient.PatientViewModel
import com.healthos.presentation.theme.AmberDeep
import com.healthos.presentation.theme.AmberWarning
import com.healthos.presentation.theme.BorderSubtle
import com.healthos.presentation.theme.CoralCritical
import com.healthos.presentation.theme.CoralDeep
import com.healthos.presentation.theme.MidnightInk
import com.healthos.presentation.theme.MintDeep
import com.healthos.presentation.theme.MintSuccess
import com.healthos.presentation.theme.PanelSurface
import com.healthos.presentation.theme.SurfaceElevated
import com.healthos.presentation.theme.TealBright
import com.healthos.presentation.theme.TealDark
import com.healthos.presentation.theme.TealPrimary
import com.healthos.presentation.theme.TextPrimary
import com.healthos.presentation.theme.TextSecondary
import com.healthos.presentation.theme.TextTertiary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalHistoryScreen(
    viewModel: PatientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allergies by viewModel.allergies.collectAsState()
    val pathologicalHistory by viewModel.pathologicalHistory.collectAsState()
    val gynecoHistory by viewModel.gynecoObstetricHistory.collectAsState()
    val familyHistory by viewModel.familyHistory.collectAsState()
    val habits by viewModel.lifestyleHabits.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val healthProfile by viewModel.healthProfile.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Alergias", "Fármacos", "Patologías", "Hereditarios", "Estilo de Vida", "Gineco")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Historia Clínica Integral",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightInk),
            )
        },
        containerColor = MidnightInk,
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Patient Baseline Snapshot Banner
            healthProfile?.let { hp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelSurface),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Grupo Sanguíneo: ${hp.bloodType}${hp.rhFactor}",
                                color = TealBright,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                            Text(
                                "IMC: ${"%.1f".format(hp.weightKg / Math.pow(hp.heightCm / 100.0, 2.0))} kg/m² (${hp.weightKg} kg · ${hp.heightCm} cm)",
                                color = TextSecondary,
                                fontSize = 12.sp,
                            )
                        }
                        if (hp.emergencyContactName.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Contacto Emergencia:", color = TextTertiary, fontSize = 10.sp)
                                Text(hp.emergencyContactName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Tabs Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MidnightInk,
                contentColor = TealPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = TealBright,
                    )
                },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) TealBright else TextSecondary,
                            )
                        },
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                when (selectedTabIndex) {
                    0 -> AllergiesTabContent(
                        allergies = allergies,
                        onAddAllergy = { viewModel.addAllergy(it) },
                        onDeleteAllergy = { viewModel.deleteAllergy(it) },
                    )
                    1 -> MedicationsAdherenceTabContent(
                        medications = medications,
                        onMarkTaken = { viewModel.markMedicationTaken(it) },
                    )
                    2 -> PathologicalTimelineTabContent(
                        entries = pathologicalHistory,
                        onAddEntry = { viewModel.addPathologicalHistory(it) },
                        onDeleteEntry = { viewModel.deletePathologicalHistory(it) },
                    )
                    3 -> FamilyHistoryTabContent(
                        entries = familyHistory,
                        onAddEntry = { viewModel.addFamilyHistory(it) },
                        onDeleteEntry = { viewModel.deleteFamilyHistory(it) },
                    )
                    4 -> LifestyleHabitsTabContent(
                        habits = habits,
                        onSave = { viewModel.saveLifestyleHabits(it) },
                    )
                    5 -> GynecoObstetricTabContent(
                        history = gynecoHistory,
                        isFemale = healthProfile?.gender?.equals("Femenino", ignoreCase = true) ?: true,
                        onSave = { viewModel.saveGynecoObstetricHistory(it) },
                    )
                }
            }
        }
    }
}

// 1. ALLERGIES TAB
@Composable
fun AllergiesTabContent(
    allergies: List<Allergy>,
    onAddAllergy: (Allergy) -> Unit,
    onDeleteAllergy: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var allergenText by remember { mutableStateOf("") }
    var severityText by remember { mutableStateOf("Moderada") }
    var manifestationsText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alergias Conocidas (${allergies.size})", color = TextPrimary, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MidnightInk)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", color = MidnightInk, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (allergies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No se han registrado alergias.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allergies, key = { it.id }) { allergy ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PanelSurface),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(allergy.allergen, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (allergy.severity == AllergySeverity.SEVERA_ANAFILAXIA) CoralDeep else AmberDeep)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            allergy.severity.name,
                                            color = if (allergy.severity == AllergySeverity.SEVERA_ANAFILAXIA) CoralCritical else AmberWarning,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                if (allergy.manifestations.isNotBlank()) {
                                    Text("Reacción: ${allergy.manifestations}", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { onDeleteAllergy(allergy.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = TextTertiary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Nueva Alergia", color = TextPrimary) },
            containerColor = PanelSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = allergenText,
                        onValueChange = { allergenText = it },
                        label = { Text("Alérgeno (ej. Penicilina, Mariscos)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = manifestationsText,
                        onValueChange = { manifestationsText = it },
                        label = { Text("Reacción / Manifestación (ej. Urticaria, Disnea)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Leve", "Moderada", "Severa").forEach { sev ->
                            FilterChip(
                                selected = severityText == sev,
                                onClick = { severityText = sev },
                                label = { Text(sev, fontSize = 10.sp) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (allergenText.isNotBlank()) {
                            val mappedSeverity = when (severityText) {
                                "Severa" -> AllergySeverity.SEVERA_ANAFILAXIA
                                "Leve" -> AllergySeverity.LEVE
                                else -> AllergySeverity.MODERADA
                            }
                            onAddAllergy(
                                Allergy(
                                    id = UUID.randomUUID().toString(),
                                    allergen = allergenText.trim(),
                                    category = AllergyCategory.MEDICAMENTO,
                                    severity = mappedSeverity,
                                    manifestations = manifestationsText.trim(),
                                    diagnosedDate = java.time.LocalDate.now().toString(),
                                ),
                            )
                            showAddDialog = false
                            allergenText = ""
                            manifestationsText = ""
                        }
                    },
                ) {
                    Text("Guardar", color = TealBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
        )
    }
}

// 2. MEDICATIONS & ADHERENCE TAB
@Composable
fun MedicationsAdherenceTabContent(
    medications: List<Medication>,
    onMarkTaken: (String) -> Unit,
) {
    val totalTaken = medications.count { it.takenToday }
    val adherenceScore = if (medications.isNotEmpty()) (totalTaken * 100) / medications.size else 100

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelSurface),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Adherencia Farmacológica Diaria", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("$adherenceScore%", color = if (adherenceScore >= 80) MintSuccess else CoralCritical, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { adherenceScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (adherenceScore >= 80) MintSuccess else AmberWarning,
                    trackColor = SurfaceElevated,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("$totalTaken de ${medications.size} dosis administradas hoy", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Fármacos Activos", color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (medications.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No hay medicamentos activos.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(medications, key = { it.id }) { med ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PanelSurface),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(med.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${med.dose} · Vía ${med.route} · ${med.schedule}", color = TextSecondary, fontSize = 12.sp)
                                med.adherencePercentage?.let {
                                    Text("Histórico adherencia: $it%", color = TealBright, fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = { onMarkTaken(med.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (med.takenToday) MintDeep else TealPrimary,
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                if (med.takenToday) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MintSuccess)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tomada", color = MintSuccess, fontSize = 12.sp)
                                } else {
                                    Text("Registrar", color = MidnightInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. PATHOLOGICAL TIMELINE TAB
@Composable
fun PathologicalTimelineTabContent(
    entries: List<PathologicalHistory>,
    onAddEntry: (PathologicalHistory) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var titleText by remember { mutableStateOf("") }
    var typeText by remember { mutableStateOf("Crónica") }
    var icd10Text by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("2024") }
    var notesText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Cronología Patológica y Diagnósticos", color = TextPrimary, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MidnightInk)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo", color = MidnightInk, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Sin antecedentes patológicos registrados.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PanelSurface),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    entry.icd10Code?.let { code ->
                                        if (code.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TealDark)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                            ) {
                                                Text(code, color = TealBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Text("Año/Fecha: ${entry.yearOrDate} · Tipo: ${entry.type.name}", color = TextSecondary, fontSize = 12.sp)
                                if (entry.notes.isNotBlank()) {
                                    Text(entry.notes, color = TextTertiary, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { onDeleteEntry(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = TextTertiary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Antecedente Patológico", color = TextPrimary) },
            containerColor = PanelSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Condición / Diagnóstico (ej. Hipertensión)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = icd10Text,
                        onValueChange = { icd10Text = it },
                        label = { Text("Código CIE-10 (ej. I10, E11.9)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = { Text("Año / Fecha de diagnóstico") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notas de evolución / Tratamiento") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (titleText.isNotBlank()) {
                            val mappedType = when (typeText) {
                                "Cirugía" -> PathologicalType.CIRUGIA
                                "Hospitalización" -> PathologicalType.HOSPITALIZACION
                                "Implante" -> PathologicalType.IMPLANTE
                                else -> PathologicalType.ENFERMEDAD_CRONICA
                            }
                            onAddEntry(
                                PathologicalHistory(
                                    id = UUID.randomUUID().toString(),
                                    type = mappedType,
                                    title = titleText.trim(),
                                    icd10Code = icd10Text.trim(),
                                    yearOrDate = yearText.trim(),
                                    notes = notesText.trim(),
                                ),
                            )
                            showAddDialog = false
                            titleText = ""
                            icd10Text = ""
                        }
                    },
                ) {
                    Text("Guardar", color = TealBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
        )
    }
}

// 4. FAMILY HISTORY TAB
@Composable
fun FamilyHistoryTabContent(
    entries: List<FamilyHistory>,
    onAddEntry: (FamilyHistory) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var kinshipText by remember { mutableStateOf("Padre") }
    var conditionText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("Cardiovascular") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Antecedentes Heredofamiliares", color = TextPrimary, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MidnightInk)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", color = MidnightInk, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No hay antecedentes familiares registrados.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PanelSurface),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.kinship.name}: ${entry.conditionName}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Línea de riesgo: ${entry.category}", color = TealBright, fontSize = 12.sp)
                                if (entry.notes.isNotBlank()) {
                                    Text(entry.notes, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { onDeleteEntry(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = TextTertiary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Antecedente Heredofamiliar", color = TextPrimary) },
            containerColor = PanelSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kinshipText,
                        onValueChange = { kinshipText = it },
                        label = { Text("Parentesco (ej. Madre, Padre, Abuelo materno)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = conditionText,
                        onValueChange = { conditionText = it },
                        label = { Text("Enfermedad (ej. Diabetes Mellitus 2, IAM precoz)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (conditionText.isNotBlank()) {
                            val mappedKinship = when (kinshipText.lowercase()) {
                                "madre" -> Kinship.MADRE
                                "abuelos", "abuelo", "abuela" -> Kinship.ABUELOS
                                "hermanos", "hermano", "hermana" -> Kinship.HERMANOS
                                "padre" -> Kinship.PADRE
                                else -> Kinship.OTRO
                            }
                            onAddEntry(
                                FamilyHistory(
                                    id = UUID.randomUUID().toString(),
                                    kinship = mappedKinship,
                                    conditionName = conditionText.trim(),
                                    category = categoryText,
                                    notes = "",
                                ),
                            )
                            showAddDialog = false
                            conditionText = ""
                        }
                    },
                ) {
                    Text("Guardar", color = TealBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
        )
    }
}

// 5. LIFESTYLE HABITS TAB
@Composable
fun LifestyleHabitsTabContent(
    habits: LifestyleHabits?,
    onSave: (LifestyleHabits) -> Unit,
) {
    var smoking by remember { mutableStateOf(habits?.smokingStatus ?: "No fumador") }
    var alcohol by remember { mutableStateOf(habits?.alcoholFrequency ?: "Ocasional") }
    var exerciseDays by remember { mutableIntStateOf(habits?.exerciseDaysPerWeek ?: 3) }
    var sleepHours by remember { mutableStateOf((habits?.averageSleepHours ?: 7.5).toString()) }
    var diet by remember { mutableStateOf(habits?.dietPattern ?: "Balanceada") }
    var savedSuccess by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Cuestionario de Hábitos y Estilo de Vida", color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PanelSurface),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tabaquismo", color = TealBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("No fumador", "Exfumador", "Fumador activo").forEach { opt ->
                        FilterChip(
                            selected = smoking == opt,
                            onClick = { smoking = opt },
                            label = { Text(opt, fontSize = 11.sp) },
                        )
                    }
                }

                Text("Consumo de Alcohol", color = TealBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Ninguno", "Ocasional", "Semanal", "Frecuente").forEach { opt ->
                        FilterChip(
                            selected = alcohol == opt,
                            onClick = { alcohol = opt },
                            label = { Text(opt, fontSize = 11.sp) },
                        )
                    }
                }

                Text("Actividad Física Semanal: $exerciseDays días/semana", color = TealBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..7).forEach { day ->
                        FilterChip(
                            selected = exerciseDays == day,
                            onClick = { exerciseDays = day },
                            label = { Text("$day", fontSize = 11.sp) },
                        )
                    }
                }

                OutlinedTextField(
                    value = sleepHours,
                    onValueChange = { sleepHours = it },
                    label = { Text("Horas de sueño promedio por noche") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = diet,
                    onValueChange = { diet = it },
                    label = { Text("Patrón Dietético / Nutrición") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        val parsedSleep = sleepHours.toDoubleOrNull() ?: 7.5
                        onSave(
                            LifestyleHabits(
                                id = habits?.id ?: "habits_me",
                                smokingStatus = smoking,
                                packsPerDay = if (smoking == "Fumador activo") 0.5 else 0.0,
                                alcoholFrequency = alcohol,
                                exerciseDaysPerWeek = exerciseDays,
                                exerciseIntensity = "Moderada",
                                averageSleepHours = parsedSleep,
                                dietPattern = diet.trim(),
                            ),
                        )
                        savedSuccess = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Guardar Hábitos de Vida", color = MidnightInk, fontWeight = FontWeight.Bold)
                }

                if (savedSuccess) {
                    Text("Hábitos actualizados correctamente.", color = MintSuccess, fontSize = 12.sp)
                }
            }
        }
    }
}

// 6. GYNECO-OBSTETRIC TAB
@Composable
fun GynecoObstetricTabContent(
    history: GynecoObstetricHistory?,
    isFemale: Boolean,
    onSave: (GynecoObstetricHistory) -> Unit,
) {
    if (!isFemale) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PanelSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Módulo Gineco-Obstétrico no aplicable según el sexo registrado en el perfil.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }
        return
    }

    var menarche by remember { mutableStateOf((history?.menarcheAge ?: 12).toString()) }
    var lmp by remember { mutableStateOf(history?.lastMenstrualPeriod ?: "2026-08-01") }
    var gestas by remember { mutableIntStateOf(history?.gestas ?: 0) }
    var isPregnant by remember { mutableStateOf(history?.isPregnant ?: false) }
    var gestWeeks by remember { mutableStateOf((history?.gestationalWeeks ?: 0).toString()) }
    var contraceptive by remember { mutableStateOf(history?.contraceptiveMethod ?: "Ninguno") }
    var savedSuccess by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Historia Gineco-Obstétrica", color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PanelSurface),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = menarche,
                    onValueChange = { menarche = it },
                    label = { Text("Edad de Menarquía (años)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lmp,
                    onValueChange = { lmp = it },
                    label = { Text("Fecha de Última Menstruación (FUM)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Actualmente en Embarazo", color = TextPrimary, fontSize = 14.sp)
                    FilterChip(
                        selected = isPregnant,
                        onClick = { isPregnant = !isPregnant },
                        label = { Text(if (isPregnant) "Sí" else "No") },
                    )
                }

                if (isPregnant) {
                    OutlinedTextField(
                        value = gestWeeks,
                        onValueChange = { gestWeeks = it },
                        label = { Text("Semanas de Gestación") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = contraceptive,
                    onValueChange = { contraceptive = it },
                    label = { Text("Método Anticonceptivo Actual") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        onSave(
                            GynecoObstetricHistory(
                                id = history?.id ?: "gyneco_me",
                                menarcheAge = menarche.toIntOrNull() ?: 12,
                                lastMenstrualPeriod = lmp.trim(),
                                gestas = gestas,
                                partos = 0,
                                cesareas = 0,
                                abortos = 0,
                                contraceptiveMethod = contraceptive.trim(),
                                isPregnant = isPregnant,
                                gestationalWeeks = if (isPregnant) gestWeeks.toIntOrNull() else null,
                            ),
                        )
                        savedSuccess = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Guardar Datos Gineco-Obstétricos", color = MidnightInk, fontWeight = FontWeight.Bold)
                }

                if (savedSuccess) {
                    Text("Historial ginecológico guardado.", color = MintSuccess, fontSize = 12.sp)
                }
            }
        }
    }
}
