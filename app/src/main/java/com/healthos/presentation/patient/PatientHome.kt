package com.healthos.presentation.patient

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthos.domain.model.Alert
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.WearableDevice
import com.healthos.presentation.common.ProvideWindowSizeInfo
import com.healthos.presentation.common.WindowSizeInfo
import kotlin.math.roundToInt

private val Ink = Color(0xFF020717)
private val Panel = Color(0xFF121B2D)
private val PanelDeep = Color(0xFF050A18)
private val StrokeLine = Color(0xFF26344E)
private val TextMain = Color.White
private val TextMuted = Color(0xFFA8B7D2)
private val Teal = Color(0xFF16A394)
private val TealBright = Color(0xFF19E3BC)
private val Blue = Color(0xFF72B7FF)
private val Pink = Color(0xFFED2553)
private val PinkSoft = Color(0xFFFF6F91)
private val Purple = Color(0xFF9A68FF)
private val Yellow = Color(0xFFFFC247)

private data class PatientTab(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun PatientHome(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel(),
) {
    val measurements by viewModel.measurements.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    val tabs =
        listOf(
            PatientTab("Inicio", Icons.Filled.Home),
            PatientTab("Metricas", Icons.Filled.ShowChart),
            PatientTab("SOS", Icons.Filled.Warning),
            PatientTab("Equipos", Icons.Outlined.Sensors),
            PatientTab("Perfil", Icons.Filled.Settings),
        )

    ProvideWindowSizeInfo { sizeInfo ->
        PatientShell(
            modifier = modifier,
            selected = selected,
            tabs = tabs,
            onSelect = { selected = it },
            sizeInfo = sizeInfo,
        ) { contentModifier ->
            when (selected) {
                0 ->
                    DashboardScreen(
                        modifier = contentModifier,
                        measurements = measurements,
                        medications = medications,
                        alerts = alerts,
                        actionState = actionState,
                        sizeInfo = sizeInfo,
                        onAnalyze = viewModel::analyzeRisk,
                        onMedicationTaken = viewModel::markMedicationTaken,
                    )
                1 -> MetricsScreen(contentModifier, measurements, sizeInfo)
                2 -> SosScreen(contentModifier, sizeInfo, viewModel::triggerSos)
                3 -> DevicesScreen(contentModifier, devices, sizeInfo, viewModel::linkMockDevice, viewModel::unlinkDevice)
                4 -> ProfileScreen(contentModifier, sizeInfo, onLogout)
            }
        }
    }
}

@Composable
private fun PatientShell(
    modifier: Modifier,
    selected: Int,
    tabs: List<PatientTab>,
    onSelect: (Int) -> Unit,
    sizeInfo: WindowSizeInfo,
    content: @Composable (Modifier) -> Unit,
) {
    if (sizeInfo.useNavRail) {
        // Landscape & Tablet layout with NavigationRail
        Row(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Ink)
                    .statusBarsPadding(),
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = Panel,
                contentColor = TextMain,
                header = {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Teal.copy(alpha = 0.25f))
                                    .border(1.dp, TealBright, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("H", color = TealBright, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                },
            ) {
                Spacer(Modifier.weight(1f))
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selected == index
                    val isSos = tab.label == "SOS"
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onSelect(index) },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint =
                                    if (isSos) {
                                        PinkSoft
                                    } else if (isSelected) {
                                        TealBright
                                    } else {
                                        TextMuted
                                    },
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextMain else TextMuted,
                            )
                        },
                        colors =
                            NavigationRailItemDefaults.colors(
                                selectedIconColor = TealBright,
                                indicatorColor = if (isSos) Pink.copy(alpha = 0.3f) else Teal.copy(alpha = 0.2f),
                            ),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                SecurityStrip(authenticated = true)
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content(Modifier.fillMaxSize())
                }
            }
        }
    } else {
        // Compact Portrait layout with bottom bar
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Ink)
                    .statusBarsPadding(),
        ) {
            SecurityStrip(authenticated = true)
            Box(Modifier.weight(1f)) {
                content(Modifier.fillMaxSize())
            }
            PatientNavigationBar(
                selected = selected,
                tabs = tabs,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun SecurityStrip(authenticated: Boolean) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color(0xFF071024))
                .border(1.dp, StrokeLine.copy(alpha = 0.75f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Security, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            MonoText("SQLCipher Encrypted", color = TextMuted, size = 13)
        }
        Row(Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Key, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            MonoText(if (authenticated) "Token: JWT" else "No Auth", color = TextMuted, size = 13)
        }
    }
}

@Composable
private fun PatientNavigationBar(
    selected: Int,
    tabs: List<PatientTab>,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(78.dp)
                .background(Panel)
                .border(1.dp, StrokeLine.copy(alpha = 0.65f)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selected == index
            val isSos = tab.label == "SOS"
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(if (isSos) 44.dp else 30.dp)
                            .clip(CircleShape)
                            .background(if (isSos) Pink else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected || isSos) TextMain else TextMuted,
                        modifier = Modifier.size(if (isSos) 24.dp else 22.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    color = if (isSelected || isSos) TextMain else TextMuted,
                    fontWeight = if (isSelected || isSos) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    modifier: Modifier,
    measurements: List<Measurement>,
    medications: List<Medication>,
    alerts: List<Alert>,
    actionState: PatientActionState,
    sizeInfo: WindowSizeInfo,
    onAnalyze: () -> Unit,
    onMedicationTaken: (String) -> Unit,
) {
    val heart = measurements.firstOrNull { it.metricType == MetricType.HEART_RATE }
    val spo2 = measurements.firstOrNull { it.metricType == MetricType.SPO2 }
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 32.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Bienvenido de nuevo", color = Blue, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Paciente", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(TealBright))
                        }
                    }
                    Avatar()
                }
            }
            item {
                SyncCard()
            }

            if (sizeInfo.isCompact && !sizeInfo.isLandscape) {
                // Single-column layout for compact portrait phones
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricTile(
                            title = "Ritmo Cardiaco",
                            value = (heart?.value ?: 74.0).roundToInt().toString(),
                            unit = "bpm",
                            status = "TFLite ML On-Device",
                            icon = Icons.Filled.Favorite,
                            accent = PinkSoft,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            title = "Oxigeno SpO2",
                            value = (spo2?.value ?: 98.0).roundToInt().toString(),
                            unit = "%",
                            status = "Rango Optimo",
                            icon = Icons.Outlined.Air,
                            accent = Color(0xFF08B6D7),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    MlRuntimeCard(actionState = actionState, onAnalyze = onAnalyze)
                }
                item {
                    DeviceSummaryCard()
                }
                item {
                    MedicationCard(medications = medications, onMedicationTaken = onMedicationTaken)
                }
                item {
                    AlertsCompactCard(alerts = alerts)
                }
            } else {
                // Two-column responsive layout for tablets / landscape
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Left Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricTile(
                                    title = "Ritmo Cardiaco",
                                    value = (heart?.value ?: 74.0).roundToInt().toString(),
                                    unit = "bpm",
                                    status = "TFLite ML On-Device",
                                    icon = Icons.Filled.Favorite,
                                    accent = PinkSoft,
                                    modifier = Modifier.weight(1f),
                                )
                                MetricTile(
                                    title = "Oxigeno SpO2",
                                    value = (spo2?.value ?: 98.0).roundToInt().toString(),
                                    unit = "%",
                                    status = "Rango Optimo",
                                    icon = Icons.Outlined.Air,
                                    accent = Color(0xFF08B6D7),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            MlRuntimeCard(actionState = actionState, onAnalyze = onAnalyze)
                            DeviceSummaryCard()
                        }
                        // Right Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            MedicationCard(medications = medications, onMedicationTaken = onMedicationTaken)
                            AlertsCompactCard(alerts = alerts)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar() {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF243147))
                .border(1.dp, Color(0xFF46556F), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("CL", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SyncCard() {
    DarkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Refresh, null, tint = Blue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("WorkManager Outbox Sync", color = Blue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("0 registros pendientes", color = TextMuted, fontSize = 13.sp)
            }
            Pill("SQLite + Room", Blue, Color(0xFF0F326C))
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    unit: String,
    status: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    DarkCard(modifier = modifier.height(154.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(title, color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = TextMain, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(4.dp))
            Text(unit, color = TextMuted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, null, tint = TealBright, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(status, color = TealBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MlRuntimeCard(
    actionState: PatientActionState,
    onAnalyze: () -> Unit,
) {
    DarkCard(
        modifier = Modifier.clickable { onAnalyze() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MonitorHeart, null, tint = Purple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("ml-runtime / TFLite Inferencia", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Pill("TFLite 2.16", Purple, Color(0xFF3B225D))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelDeep)
                    .border(1.dp, StrokeLine, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                MonoText("Modelo: arrhythmia_v2.tflite", color = TextMain, size = 14)
                Spacer(Modifier.height(6.dp))
                Text("Tiempo Inferencia: 14 ms", color = TextMuted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(actionState.risk?.label ?: "Sin anomalías", color = TealBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                val confidence = ((actionState.risk?.score ?: 0.984f) * 100).roundToInt()
                Text("Confianza: $confidence.4%", color = TealBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DeviceSummaryCard() {
    DarkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF243147))
                        .border(1.dp, Color(0xFF3A4A64), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Bluetooth, null, tint = TealBright)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Xiaomi Band 8", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("● Conectado BLE GATT", color = TealBright, fontSize = 12.sp)
            }
            OutlinePillButton("Gestionar") {}
        }
    }
}

@Composable
private fun MedicationCard(
    medications: List<Medication>,
    onMedicationTaken: (String) -> Unit,
) {
    DarkCard {
        val fallback =
            listOf(
                Medication("med-1", "Enalapril", "10mg", "08:00 AM", true),
                Medication("med-2", "Aspirina", "100mg", "20:00 PM", false),
            )
        val meds = medications.ifEmpty { fallback }
        val taken = meds.count { it.takenToday }

        Row(verticalAlignment = Alignment.Top) {
            Text(
                "MEDICAMENTOS DE HOY",
                color = TextMain,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$taken de ${meds.size}\ntomados",
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        meds.forEach { medication ->
            MedicationRow(medication = medication, onMedicationTaken = onMedicationTaken)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MedicationRow(
    medication: Medication,
    onMedicationTaken: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PanelDeep)
                .border(1.dp, StrokeLine.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = medication.takenToday,
            onCheckedChange = { checked ->
                if (checked && !medication.takenToday) onMedicationTaken(medication.id)
            },
            colors =
                CheckboxDefaults.colors(
                    checkedColor = Color(0xFF2487FF),
                    uncheckedColor = TextMain,
                    checkmarkColor = TextMain,
                ),
        )
        Column(Modifier.weight(1f)) {
            Text(
                "${medication.name} ${medication.dose}",
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textDecoration = if (medication.takenToday) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text("${medication.schedule} • Prescripcion Diaria", color = Color(0xFF7C91B5), fontSize = 12.sp)
        }
        Pill(
            text = if (medication.takenToday) "Completado" else "Pendiente",
            color = if (medication.takenToday) TealBright else Yellow,
            background = if (medication.takenToday) Color(0xFF063F3B) else Color(0xFF2E2413),
        )
    }
}

@Composable
private fun AlertsCompactCard(alerts: List<Alert>) {
    DarkCard {
        Text("Alertas activas", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        if (alerts.isEmpty()) {
            Text("Estado", color = TextMuted, fontSize = 13.sp)
            Text("Sin eventos criticos", color = TealBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        } else {
            alerts.forEach {
                Text(it.title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "${it.status.name} - ${it.timestamp}",
                    color = if (it.status.name == "CRITICAL") PinkSoft else Yellow,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun MetricsScreen(
    modifier: Modifier,
    measurements: List<Measurement>,
    sizeInfo: WindowSizeInfo,
) {
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 32.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Histórico de Métricas", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Spacer(Modifier.weight(1f))
                    FilterButton()
                }
            }

            if (sizeInfo.isCompact && !sizeInfo.isLandscape) {
                // Stacked layout for compact phones
                item {
                    ChartCard(
                        title = "Frecuencia Cardíaca (bpm)",
                        endpoint = "GET /v1/measurements",
                        values = listOf(72f, 75f, 68f, 71f, 74f, 78f, 74f),
                        minValue = 68f,
                        maxValue = 78f,
                        line = Color(0xFFFF466D),
                        fill = Color(0xFFFF466D).copy(alpha = 0.16f),
                    )
                }
                item {
                    ChartCard(
                        title = "Saturación SpO2 (%)",
                        endpoint = null,
                        values = listOf(98f, 97f, 99f, 98f, 98f, 97f, 98f),
                        minValue = 90f,
                        maxValue = 100f,
                        line = Color(0xFF08C8E8),
                        fill = Color(0xFF08C8E8).copy(alpha = 0.16f),
                    )
                }
            } else {
                // Side-by-side charts for tablets / landscape
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(Modifier.weight(1f)) {
                            ChartCard(
                                title = "Frecuencia Cardíaca (bpm)",
                                endpoint = "GET /v1/measurements",
                                values = listOf(72f, 75f, 68f, 71f, 74f, 78f, 74f),
                                minValue = 68f,
                                maxValue = 78f,
                                line = Color(0xFFFF466D),
                                fill = Color(0xFFFF466D).copy(alpha = 0.16f),
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            ChartCard(
                                title = "Saturación SpO2 (%)",
                                endpoint = null,
                                values = listOf(98f, 97f, 99f, 98f, 98f, 97f, 98f),
                                minValue = 90f,
                                maxValue = 100f,
                                line = Color(0xFF08C8E8),
                                fill = Color(0xFF08C8E8).copy(alpha = 0.16f),
                            )
                        }
                    }
                }
            }

            item {
                Text("Lecturas Recientes", color = Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                val chunks = measurements.take(6).chunked(if (sizeInfo.isCompact && !sizeInfo.isLandscape) 1 else 2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowItems.forEach { measurement ->
                                DarkCard(Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(measurement.metricType.name, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "${measurement.value} ${measurement.unit}",
                                            color = TealBright,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterButton() {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .border(1.dp, Color(0xFF46556F), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Últimos 7 Días", color = TextMain, fontSize = 13.sp)
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ChartCard(
    title: String,
    endpoint: String?,
    values: List<Float>,
    minValue: Float,
    maxValue: Float,
    line: Color,
    fill: Color,
) {
    DarkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            endpoint?.let { Pill(it, PinkSoft, Color(0xFF361629)) }
        }
        Spacer(Modifier.height(10.dp))
        LineChart(
            values = values,
            minValue = minValue,
            maxValue = maxValue,
            line = line,
            fill = fill,
            modifier = Modifier.fillMaxWidth().height(190.dp),
        )
    }
}

@Composable
private fun LineChart(
    values: List<Float>,
    minValue: Float,
    maxValue: Float,
    line: Color,
    fill: Color,
    modifier: Modifier,
) {
    val labels = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Hoy")
    Column {
        Canvas(modifier = modifier) {
            val left = 40.dp.toPx()
            val bottom = 24.dp.toPx()
            val chartWidth = size.width - left - 8.dp.toPx()
            val chartHeight = size.height - bottom - 8.dp.toPx()
            val stepX = chartWidth / (values.size - 1).coerceAtLeast(1)
            val yScale = chartHeight / (maxValue - minValue)
            val points =
                values.mapIndexed { index, value ->
                    Offset(
                        x = left + stepX * index,
                        y = 8.dp.toPx() + (maxValue - value) * yScale,
                    )
                }

            repeat(5) { index ->
                val y = 8.dp.toPx() + chartHeight * index / 4f
                drawLine(StrokeLine.copy(alpha = 0.65f), Offset(left, y), Offset(left + chartWidth, y), 1.dp.toPx())
            }
            repeat(values.size) { index ->
                val x = left + stepX * index
                drawLine(StrokeLine.copy(alpha = 0.65f), Offset(x, 8.dp.toPx()), Offset(x, 8.dp.toPx() + chartHeight), 1.dp.toPx())
            }

            val path =
                Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
            val area =
                Path().apply {
                    moveTo(points.first().x, 8.dp.toPx() + chartHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, 8.dp.toPx() + chartHeight)
                    close()
                }
            drawPath(area, fill)
            drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            points.forEach { point ->
                drawCircle(line, radius = 4.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, color = TextMuted, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun SosScreen(
    modifier: Modifier,
    sizeInfo: WindowSizeInfo,
    onSos: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val horizontalPadding = if (sizeInfo.isCompact) 20.dp else 40.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (sizeInfo.isLandscape) {
            // Horizontal / Landscape side-by-side layout
            Row(
                modifier =
                    Modifier
                        .widthIn(max = 1000.dp)
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left side: SOS Button
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    SosButton(onSos = onSos, compactSize = true)
                }

                // Right side: Info Card
                Column(
                    modifier = Modifier.weight(1.2f).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Pill("MÓDULO SOS EMERGENCIA", PinkSoft, Color(0xFF421127))
                    Spacer(Modifier.height(8.dp))
                    Text("Alerta Crítica Inmediata", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Presiona el botón para transmitir tu ubicación GPS actual y notificar de inmediato a tus cuidadores.",
                        color = Blue,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    DarkCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = PinkSoft, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Coordenadas GPS:", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        MonoText("Lat: 19.4326 | Lng: -99.1332 (CDMX)", color = Blue, size = 13)
                        Spacer(Modifier.height(6.dp))
                        Text("Expedited Critical SyncWorker Outbox Active", color = Color(0xFF7183A6), fontSize = 11.sp)
                    }
                }
            }
        } else {
            // Vertical layout for portrait
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = horizontalPadding, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Pill("MÓDULO SOS EMERGENCIA", PinkSoft, Color(0xFF421127))
                Spacer(Modifier.height(12.dp))
                Text("Alerta Crítica Inmediata", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Presiona el botón para transmitir tu ubicación GPS actual y notificar de inmediato a tus cuidadores.",
                    color = Blue,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                SosButton(onSos = onSos, compactSize = false)
                Spacer(Modifier.height(24.dp))
                DarkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = PinkSoft, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Coordenadas GPS:", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    MonoText("Lat: 19.4326 | Lng: -99.1332 (CDMX)", color = Blue, size = 13)
                    Spacer(Modifier.height(6.dp))
                    Text("Expedited Critical SyncWorker Outbox Active", color = Color(0xFF7183A6), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SosButton(
    onSos: () -> Unit,
    compactSize: Boolean,
) {
    val outerSize = if (compactSize) 200.dp else 240.dp
    val innerSize = if (compactSize) 160.dp else 194.dp

    Box(
        modifier =
            Modifier
                .size(outerSize)
                .clip(CircleShape)
                .background(Pink.copy(alpha = 0.17f)),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onSos,
            modifier = Modifier.size(innerSize),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Pink),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Warning, null, tint = TextMain, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("SOS", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                MonoText("POST /v1/alerts/sos", color = TextMain, size = 11)
            }
        }
    }
}

@Composable
private fun DevicesScreen(
    modifier: Modifier,
    devices: List<WearableDevice>,
    sizeInfo: WindowSizeInfo,
    onLink: () -> Unit,
    onUnlink: (String) -> Unit,
) {
    var showQr by remember { mutableStateOf(false) }
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 32.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .widthIn(max = 1000.dp)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dispositivos & BLE", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { showQr = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cámara QR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            item {
                AdapterSelector()
            }
            item {
                Row {
                    Text(
                        "VINCULADOS (GET /V1/DEVICES)",
                        color = Blue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                    )
                    Text(
                        "${devices.size.coerceAtLeast(1)} DISPOSITIVO",
                        color = Color(0xFF7183A6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
            itemsWithFallback(devices).forEach { device ->
                item {
                    DeviceRow(device = device, onUnlink = onUnlink)
                }
            }
            item {
                DarkCard {
                    Text("BLE Escáner GATT Manager", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PanelDeep)
                                .border(1.dp, StrokeLine, RoundedCornerShape(10.dp))
                                .padding(14.dp),
                    ) {
                        MonoText("[BLE_SCAN] Scanning Service UUID 0x180D (Heart Rate)...", color = TextMain, size = 12)
                        Spacer(Modifier.height(6.dp))
                        MonoText("[GATT_SUCCESS] Characteristic 0x2A37 Notification ENABLED", color = TealBright, size = 12)
                        Spacer(Modifier.height(6.dp))
                        MonoText("[ROOM_DB] Incoming byte[] parsed -> Room local save", color = Color(0xFF7183A6), size = 12)
                    }
                }
            }
        }
    }

    if (showQr) {
        QrDialog(
            onDismiss = { showQr = false },
            onDetected = {
                onLink()
                showQr = false
            },
        )
    }
}

private fun itemsWithFallback(devices: List<WearableDevice>): List<WearableDevice> =
    devices.ifEmpty {
        listOf(
            WearableDevice(
                id = "AA:BB:CC:DD:EE:FF",
                model = "Xiaomi Band 8",
                protocol = com.healthos.domain.model.DeviceProtocol.PROPRIETARY_XIAOMI,
                publicKey = "MIIBIjANBgkqhkiG9w0",
                connected = true,
            ),
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdapterSelector() {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("XiaomiBandAdapter (Protocolo Propietario)") }
    DarkCard {
        Text("Adapter Hardware (`bluetooth`):", color = Blue, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = darkFieldColors(),
                textStyle = LocalTextStyle.current.copy(color = TextMain, fontSize = 15.sp),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf<String>(
                    "XiaomiBandAdapter (Protocolo Propietario)",
                    "GenericGattAdapter (BLE Standard)",
                    "GarminAdapter (Protocolo Propietario)",
                ).forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            selected = it
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: WearableDevice,
    onUnlink: (String) -> Unit,
) {
    DarkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Teal.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Bluetooth, null, tint = TealBright)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.model, color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                MonoText("MAC: ${device.id}", color = TextMuted, size = 12)
            }
            Pill("GATT OK", TealBright, Color(0xFF074642))
            IconButton(onClick = { onUnlink(device.id) }) {
                Icon(Icons.Filled.Delete, null, tint = PinkSoft)
            }
        }
    }
}

@Composable
private fun QrDialog(
    onDismiss: () -> Unit,
    onDetected: () -> Unit,
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CameraX QR Scanner", color = TextMain, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 18.sp)
                TextButton(onClick = onDismiss) { Text("✕", color = Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PanelDeep)
                            .border(2.dp, Teal, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.QrCodeScanner, null, tint = Color(0xFF47556F), modifier = Modifier.size(60.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Escaneando código QR en wearable para vinculación de public_key BLE...",
                    color = Blue,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDetected,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Simular Detectado (Xiaomi Band 8)", fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    sizeInfo: WindowSizeInfo,
    onLogout: () -> Unit,
) {
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 32.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Perfil y Configuración", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            item {
                DarkCard {
                    Text("A6. PERFIL CLÍNICO", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileMetric("Peso", "75.5 kg", Modifier.weight(1f))
                        ProfileMetric("Altura", "180 cm", Modifier.weight(1f))
                        ProfileMetric("Sangre", "O+", Modifier.weight(1f))
                    }
                }
            }
            item {
                DarkCard {
                    Text("Seguridad Android", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    SecurityRow("Base de datos", "SQLCipher Encrypted")
                    SecurityRow("Tokens JWT", "EncryptedSharedPref")
                }
            }
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF260719), contentColor = PinkSoft),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(
    label: String,
    value: String,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .height(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PanelDeep)
                .border(1.dp, StrokeLine, RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = Blue, fontSize = 12.sp)
        Text(value, color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
private fun SecurityRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Blue, fontSize = 14.sp)
        MonoText(value, color = TealBright, size = 14)
    }
}

@Composable
private fun DarkCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Panel)
                .border(1.dp, StrokeLine, RoundedCornerShape(18.dp))
                .padding(16.dp),
        content = content,
    )
}

@Composable
private fun Pill(
    text: String,
    color: Color,
    background: Color,
) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = TextMain,
        fontSize = 13.sp,
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF253249))
                .border(1.dp, Color(0xFF566783), RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun MonoText(
    text: String,
    color: Color,
    size: Int,
) {
    Text(text = text, color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun darkFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextMain,
        unfocusedTextColor = TextMain,
        focusedContainerColor = PanelDeep,
        unfocusedContainerColor = PanelDeep,
        focusedBorderColor = StrokeLine,
        unfocusedBorderColor = StrokeLine,
        focusedTrailingIconColor = TextMain,
        unfocusedTrailingIconColor = TextMain,
    )
