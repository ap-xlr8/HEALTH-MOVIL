package com.healthos.presentation.patient

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Sensors
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthos.bluetooth.BleState
import com.healthos.bluetooth.ScannedBleDevice
import com.healthos.domain.model.Alert
import com.healthos.domain.model.HealthProfile
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
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val healthProfile by viewModel.healthProfile.collectAsState()
    val bleState by viewModel.bleState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
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
                        devices = devices,
                        pendingSyncCount = pendingSyncCount,
                        actionState = actionState,
                        sizeInfo = sizeInfo,
                        onAnalyze = viewModel::analyzeRisk,
                        onMedicationTaken = viewModel::markMedicationTaken,
                    )
                1 -> MetricsScreen(contentModifier, measurements, sizeInfo)
                2 -> SosScreen(contentModifier, sizeInfo, viewModel::triggerSos)
                3 ->
                    DevicesScreen(
                        contentModifier,
                        devices,
                        bleState,
                        scannedDevices,
                        sizeInfo,
                        viewModel::startBleScan,
                        viewModel::stopBleScan,
                        viewModel::connectToScannedDevice,
                        viewModel::unlinkDevice,
                    )
                4 -> ProfileScreen(contentModifier, healthProfile, sizeInfo, onLogout)
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
    devices: List<WearableDevice>,
    pendingSyncCount: Int,
    actionState: PatientActionState,
    sizeInfo: WindowSizeInfo,
    onAnalyze: () -> Unit,
    onMedicationTaken: (String) -> Unit,
) {
    val heart = measurements.firstOrNull { it.metricType == MetricType.HEART_RATE }
    val spo2 = measurements.firstOrNull { it.metricType == MetricType.SPO2 }
    val heartValue = heart?.value?.roundToInt()?.toString()
    val spo2Value = spo2?.value?.roundToInt()?.toString()
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
                SyncCard(pendingSyncCount = pendingSyncCount)
            }

            if (sizeInfo.isCompact && !sizeInfo.isLandscape) {
                // Single-column layout for compact portrait phones
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricTile(
                            title = "Ritmo Cardiaco",
                            value = heartValue ?: "—",
                            unit = "bpm",
                            status = if (heart != null) "TFLite ML On-Device" else "Sin datos",
                            icon = Icons.Filled.Favorite,
                            accent = PinkSoft,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            title = "Oxigeno SpO2",
                            value = spo2Value ?: "—",
                            unit = "%",
                            status = if (spo2 != null) "Rango Optimo" else "Sin datos",
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
                    DeviceSummaryCard(devices = devices)
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
                                    value = heartValue ?: "—",
                                    unit = "bpm",
                                    status = if (heart != null) "TFLite ML On-Device" else "Sin datos",
                                    icon = Icons.Filled.Favorite,
                                    accent = PinkSoft,
                                    modifier = Modifier.weight(1f),
                                )
                                MetricTile(
                                    title = "Oxigeno SpO2",
                                    value = spo2Value ?: "—",
                                    unit = "%",
                                    status = if (spo2 != null) "Rango Optimo" else "Sin datos",
                                    icon = Icons.Outlined.Air,
                                    accent = Color(0xFF08B6D7),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            MlRuntimeCard(actionState = actionState, onAnalyze = onAnalyze)
                            DeviceSummaryCard(devices = devices)
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
        Icon(Icons.Filled.Person, null, tint = TextMain, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SyncCard(pendingSyncCount: Int) {
    DarkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Refresh, null, tint = Blue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("WorkManager Outbox Sync", color = Blue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    if (pendingSyncCount == 0) "Todo sincronizado" else "$pendingSyncCount registros pendientes",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
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
                MonoText("Modelo: heart_rate_anomaly.tflite", color = TextMain, size = 14)
                Spacer(Modifier.height(6.dp))
                Text("Inferencia on-device sin conexión", color = TextMuted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(actionState.risk?.label ?: "Sin analizar", color = TealBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                val confidence = actionState.risk?.score
                if (confidence != null) {
                    Text("Confianza: ${(confidence * 100).roundToInt()}%", color = TealBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("Pulsa para analizar", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DeviceSummaryCard(devices: List<WearableDevice>) {
    val device = devices.firstOrNull()
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
                Text(device?.model ?: "Sin dispositivo vinculado", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(
                    if (device != null) "● Conectado BLE GATT" else "Vincula tu banda wearable para registrar signos vitales.",
                    color = if (device != null) TealBright else TextMuted,
                    fontSize = 12.sp,
                )
            }
            if (device != null) {
                OutlinePillButton("Gestionar") {}
            }
        }
    }
}

@Composable
private fun MedicationCard(
    medications: List<Medication>,
    onMedicationTaken: (String) -> Unit,
) {
    DarkCard {
        val taken = medications.count { it.takenToday }

        Row(verticalAlignment = Alignment.Top) {
            Text(
                "MEDICAMENTOS DE HOY",
                color = TextMain,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$taken de ${medications.size}\ntomados",
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (medications.isEmpty()) {
            Text("No tienes medicamentos registrados todavía.", color = TextMuted, fontSize = 13.sp)
        } else {
            medications.forEach { medication ->
                MedicationRow(medication = medication, onMedicationTaken = onMedicationTaken)
                Spacer(Modifier.height(8.dp))
            }
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
    val heartSeries = measurements.filter { it.metricType == MetricType.HEART_RATE }.map { it.value.toFloat() }.reversed()
    val spo2Series = measurements.filter { it.metricType == MetricType.SPO2 }.map { it.value.toFloat() }.reversed()
    val heartRange = chartRange(heartSeries, 60f, 100f)
    val spo2Range = chartRange(spo2Series, 90f, 100f)

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
                        values = heartSeries,
                        minValue = heartRange.first,
                        maxValue = heartRange.second,
                        line = Color(0xFFFF466D),
                        fill = Color(0xFFFF466D).copy(alpha = 0.16f),
                    )
                }
                item {
                    ChartCard(
                        title = "Saturación SpO2 (%)",
                        endpoint = null,
                        values = spo2Series,
                        minValue = spo2Range.first,
                        maxValue = spo2Range.second,
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
                                values = heartSeries,
                                minValue = heartRange.first,
                                maxValue = heartRange.second,
                                line = Color(0xFFFF466D),
                                fill = Color(0xFFFF466D).copy(alpha = 0.16f),
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            ChartCard(
                                title = "Saturación SpO2 (%)",
                                endpoint = null,
                                values = spo2Series,
                                minValue = spo2Range.first,
                                maxValue = spo2Range.second,
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
                if (measurements.isEmpty()) {
                    Text("Aún no hay lecturas registradas.", color = TextMuted, fontSize = 13.sp)
                } else {
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
}

private fun chartRange(
    series: List<Float>,
    fallbackMin: Float,
    fallbackMax: Float,
): Pair<Float, Float> {
    if (series.isEmpty()) return fallbackMin to fallbackMax
    val min = series.min()
    val max = series.max()
    val pad = ((max - min).coerceAtLeast(1f) * 0.15f)
    return (min - pad) to (max + pad)
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
            endpoint?.let {
                val label = if (com.healthos.BuildConfig.DEBUG) it else "Historial 7D"
                Pill(label, PinkSoft, Color(0xFF361629))
            }
        }
        Spacer(Modifier.height(10.dp))
        if (values.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                Text("Sin datos todavía", color = TextMuted, fontSize = 13.sp)
            }
        } else {
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
                        MonoText("Ubicación GPS actual del dispositivo", color = Blue, size = 13)
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
                val sosSub = if (com.healthos.BuildConfig.DEBUG) "POST /v1/alerts/sos" else "EMERGENCIA GPS"
                MonoText(sosSub, color = TextMain, size = 11)
            }
        }
    }
}

@Composable
private fun DevicesScreen(
    modifier: Modifier,
    devices: List<WearableDevice>,
    bleState: BleState,
    scannedDevices: List<ScannedBleDevice>,
    sizeInfo: WindowSizeInfo,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (ScannedBleDevice) -> Unit,
    onUnlink: (String) -> Unit,
) {
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 32.dp
    val context = LocalContext.current
    val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                onScan()
            }
        }

    fun requestPermissionsAndScan() {
        val missing = requiredPermissions.filterNot { context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            onScan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

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
                Text("Dispositivos & BLE", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            item {
                AdapterSelector()
            }
            item {
                DarkCard {
                    Text("Escáner BLE", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    if (bleState == BleState.Scanning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Buscando dispositivos BLE...", color = TealBright, fontSize = 13.sp)
                            Button(
                                onClick = onStopScan,
                                colors = ButtonDefaults.buttonColors(containerColor = Pink),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Detener", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { requestPermissionsAndScan() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Filled.Bluetooth, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buscar dispositivos BLE", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val bleErrorMessage = (bleState as? BleState.Error)?.message
                    if (bleErrorMessage != null) {
                        Text(bleErrorMessage, color = PinkSoft, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (scannedDevices.isEmpty()) {
                        Text(
                            "Apunta a tu banda wearable para detectarla. Se muestran dispositivos con servicio de frecuencia cardíaca (0x180D).",
                            color = TextMuted,
                            fontSize = 13.sp,
                        )
                    } else {
                        scannedDevices.forEach { device ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PanelDeep)
                                        .border(1.dp, StrokeLine, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(device.name, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    MonoText(device.mac, color = TextMuted, size = 12)
                                }
                                TextButton(onClick = { onConnect(device) }) {
                                    Text("Vincular", color = TealBright, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row {
                    Text(
                        "DISPOSITIVOS VINCULADOS",
                        color = Blue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                    )
                    Text(
                        "${devices.size} ${if (devices.size == 1) "DISPOSITIVO" else "DISPOSITIVOS"}",
                        color = Color(0xFF7183A6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
            if (devices.isEmpty()) {
                item {
                    DarkCard {
                        Text("Ningún dispositivo vinculado.", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Usa el escáner BLE para detectar y vincular tu banda. Las mediciones se sincronizan automáticamente.",
                            color = TextMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                devices.forEach { device ->
                    item {
                        DeviceRow(device = device, onUnlink = onUnlink)
                    }
                }
            }
            item {
                DarkCard {
                    Text("Sincronización", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Las mediciones del wearable se guardan localmente y se sincronizan con el backend en segundo plano (WorkManager).",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
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
private fun ProfileScreen(
    modifier: Modifier,
    healthProfile: HealthProfile?,
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
                    Text("PERFIL CLÍNICO", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileMetric("Peso", healthProfile?.let { "${it.weightKg} kg" } ?: "—", Modifier.weight(1f))
                        ProfileMetric("Altura", healthProfile?.let { "${it.heightCm} cm" } ?: "—", Modifier.weight(1f))
                        ProfileMetric("Sangre", healthProfile?.bloodType ?: "—", Modifier.weight(1f))
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
