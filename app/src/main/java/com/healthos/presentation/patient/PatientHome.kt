package com.healthos.presentation.patient

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.healthos.presentation.theme.AmberDeep
import com.healthos.presentation.theme.AmberWarning
import com.healthos.presentation.theme.BadgeShape
import com.healthos.presentation.theme.BlueBright
import com.healthos.presentation.theme.BlueDeep
import com.healthos.presentation.theme.BlueElectric
import com.healthos.presentation.theme.BorderGlow
import com.healthos.presentation.theme.BorderMedium
import com.healthos.presentation.theme.BorderSubtle
import com.healthos.presentation.theme.ButtonShape
import com.healthos.presentation.theme.ButtonVariant
import com.healthos.presentation.theme.CardShape
import com.healthos.presentation.theme.CoralBright
import com.healthos.presentation.theme.CoralCritical
import com.healthos.presentation.theme.CoralDeep
import com.healthos.presentation.theme.HealthBadge
import com.healthos.presentation.theme.HealthButton
import com.healthos.presentation.theme.HealthCard
import com.healthos.presentation.theme.MidnightInk
import com.healthos.presentation.theme.MintDeep
import com.healthos.presentation.theme.MintSuccess
import com.healthos.presentation.theme.PanelDeep
import com.healthos.presentation.theme.PanelSurface
import com.healthos.presentation.theme.PurpleAccent
import com.healthos.presentation.theme.PurpleDeep
import com.healthos.presentation.theme.StatusDot
import com.healthos.presentation.theme.SurfaceElevated
import com.healthos.presentation.theme.TealBright
import com.healthos.presentation.theme.TealContainer
import com.healthos.presentation.theme.TealDark
import com.healthos.presentation.theme.TealPrimary
import com.healthos.presentation.theme.TextDisabled
import com.healthos.presentation.theme.TextPrimary
import com.healthos.presentation.theme.TextSecondary
import com.healthos.presentation.theme.TextTertiary
import kotlin.math.roundToInt

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
    val currentLocation by viewModel.currentLocation.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        PatientTab("Inicio", Icons.Filled.Home),
        PatientTab("Métricas", Icons.Filled.ShowChart),
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
                0 -> DashboardScreen(
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
                2 -> SosScreen(
                    contentModifier,
                    sizeInfo,
                    currentLocation,
                    viewModel::startLocationUpdates,
                    viewModel::triggerSos,
                )
                3 -> DevicesScreen(
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
            modifier = modifier
                .fillMaxSize()
                .background(MidnightInk)
                .statusBarsPadding(),
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = PanelSurface,
                contentColor = TextPrimary,
                header = {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(TealContainer)
                                .border(1.5.dp, TealBright, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("H", color = TealBright, fontWeight = FontWeight.Black, fontSize = 20.sp)
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
                                tint = if (isSos) CoralBright else if (isSelected) TealBright else TextSecondary,
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary,
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = TealBright,
                            indicatorColor = if (isSos) CoralDeep else TealContainer,
                        ),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                SecurityStrip(authenticated = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content(Modifier.fillMaxSize())
                }
            }
        }
    } else {
        // Compact Portrait layout with bottom bar
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MidnightInk)
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
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(PanelDeep)
            .border(1.dp, BorderSubtle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = TealBright, isPulsing = true, size = 6.dp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Security, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "SQLCipher AES-256",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(
            modifier = Modifier.padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Key, null, tint = BlueBright, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (authenticated) "JWT Token Activo" else "Sin Sesión",
                color = if (authenticated) BlueBright else CoralBright,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(72.dp)
            .background(PanelSurface)
            .border(1.dp, BorderSubtle),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selected == index
            val isSos = tab.label == "SOS"

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(ButtonShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = if (isSos) CoralBright.copy(alpha = 0.3f) else TealBright.copy(alpha = 0.2f)),
                    ) { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (isSos) {
                    // Elevated prominent SOS navigation pill
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CoralCritical)
                            .border(1.dp, CoralBright, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        color = CoralBright,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(if (isSelected) TealContainer else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) TealBright else TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = if (isSelected) TealBright else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
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
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 28.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Greeting & Status Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "MONITOR CLÍNICO AUTÓNOMO",
                            color = BlueBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Paciente",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.width(8.dp))
                            StatusDot(color = TealBright, isPulsing = true, size = 10.dp)
                        }
                    }
                    Avatar()
                }
            }

            // WorkManager Outbox Sync Card
            item {
                SyncCard(pendingSyncCount = pendingSyncCount)
            }

            if (sizeInfo.isCompact && !sizeInfo.isLandscape) {
                // Portrait single-column cards
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricTile(
                            title = "Frecuencia Cardíaca",
                            value = heartValue ?: "—",
                            unit = "bpm",
                            status = if (heart != null) "TFLite On-Device" else "Sin lecturas",
                            icon = Icons.Filled.Favorite,
                            accentColor = CoralBright,
                            gradientColors = listOf(Color(0xFF380C19), SurfaceElevated),
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            title = "Saturación SpO2",
                            value = spo2Value ?: "—",
                            unit = "%",
                            status = if (spo2 != null) "Rango Óptimo" else "Sin lecturas",
                            icon = Icons.Outlined.Air,
                            accentColor = BlueElectric,
                            gradientColors = listOf(Color(0xFF0C2448), SurfaceElevated),
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
                // Two-column responsive layout for tablets and landscape
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Left Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricTile(
                                    title = "Frecuencia Cardíaca",
                                    value = heartValue ?: "—",
                                    unit = "bpm",
                                    status = if (heart != null) "TFLite On-Device" else "Sin lecturas",
                                    icon = Icons.Filled.Favorite,
                                    accentColor = CoralBright,
                                    gradientColors = listOf(Color(0xFF380C19), SurfaceElevated),
                                    modifier = Modifier.weight(1f),
                                )
                                MetricTile(
                                    title = "Saturación SpO2",
                                    value = spo2Value ?: "—",
                                    unit = "%",
                                    status = if (spo2 != null) "Rango Óptimo" else "Sin lecturas",
                                    icon = Icons.Outlined.Air,
                                    accentColor = BlueElectric,
                                    gradientColors = listOf(Color(0xFF0C2448), SurfaceElevated),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            MlRuntimeCard(actionState = actionState, onAnalyze = onAnalyze)
                            DeviceSummaryCard(devices = devices)
                        }
                        // Right Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
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
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(SurfaceElevated)
            .border(1.5.dp, BorderMedium, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Person, null, tint = TextPrimary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SyncCard(pendingSyncCount: Int) {
    HealthCard(
        containerColor = PanelSurface,
        borderColor = BorderSubtle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BlueDeep)
                    .border(1.dp, BlueElectric.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Refresh, null, tint = BlueElectric, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "WorkManager Outbox Sync",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    if (pendingSyncCount == 0) "Todos los datos sincronizados" else "$pendingSyncCount mediciones en cola",
                    color = if (pendingSyncCount == 0) MintSuccess else AmberWarning,
                    fontSize = 12.sp,
                )
            }
            HealthBadge(
                text = "ROOM SQLCIPHER",
                color = BlueBright,
                backgroundColor = BlueDeep,
                fontSize = 10.sp,
            )
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
    accentColor: Color,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    HealthCard(
        modifier = modifier.height(160.dp),
        containerColor = PanelSurface,
        borderColor = BorderSubtle,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(17.dp))
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = accentColor, size = 6.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = status,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun MlRuntimeCard(
    actionState: PatientActionState,
    onAnalyze: () -> Unit,
) {
    HealthCard(
        onClick = onAnalyze,
        containerColor = PanelSurface,
        borderColor = BorderMedium,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PurpleDeep)
                    .border(1.dp, PurpleAccent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MonitorHeart, null, tint = PurpleAccent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Inferencia ML On-Device",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Spacer(Modifier.weight(1f))
            HealthBadge(
                text = "TFLITE 2.16",
                color = PurpleAccent,
                backgroundColor = PurpleDeep,
                fontSize = 10.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelDeep)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "heart_rate_anomaly.tflite",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Detección de anomalías en tiempo real",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val risk = actionState.risk
                if (risk != null) {
                    val riskColor = if (risk.score > 0.7f) CoralBright else MintSuccess
                    val riskBg = if (risk.score > 0.7f) CoralDeep else MintDeep
                    HealthBadge(
                        text = risk.label,
                        color = riskColor,
                        backgroundColor = riskBg,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Confianza: ${(risk.score * 100).roundToInt()}%",
                        color = riskColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    HealthBadge(
                        text = "Analizar Vitals",
                        color = TealBright,
                        backgroundColor = TealContainer,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceSummaryCard(devices: List<WearableDevice>) {
    val device = devices.firstOrNull()
    HealthCard(
        containerColor = PanelSurface,
        borderColor = BorderSubtle,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (device != null) TealContainer else SurfaceElevated)
                    .border(1.dp, if (device != null) TealBright.copy(alpha = 0.6f) else BorderMedium, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Bluetooth,
                    null,
                    tint = if (device != null) TealBright else TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device?.model ?: "Sin Wearable Vinculado",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(
                        color = if (device != null) TealBright else TextDisabled,
                        isPulsing = device != null,
                        size = 6.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (device != null) "Conectado BLE GATT" else "Vincula tu banda wearable",
                        color = if (device != null) TealBright else TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
            if (device != null) {
                HealthBadge("ACTIVO", TealBright, TealContainer, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MedicationCard(
    medications: List<Medication>,
    onMedicationTaken: (String) -> Unit,
) {
    HealthCard(
        containerColor = PanelSurface,
        borderColor = BorderSubtle,
    ) {
        val taken = medications.count { it.takenToday }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "MEDICAMENTOS DE HOY",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
            )
            HealthBadge(
                text = "$taken de ${medications.size} tomados",
                color = if (taken == medications.size && medications.isNotEmpty()) MintSuccess else AmberWarning,
                backgroundColor = if (taken == medications.size && medications.isNotEmpty()) MintDeep else AmberDeep,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (medications.isEmpty()) {
            Text(
                "No tienes prescripciones registradas.",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDeep)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = medication.takenToday,
            onCheckedChange = { checked ->
                if (checked && !medication.takenToday) onMedicationTaken(medication.id)
            },
            colors = CheckboxDefaults.colors(
                checkedColor = TealPrimary,
                uncheckedColor = TextSecondary,
                checkmarkColor = MidnightInk,
            ),
        )
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${medication.name} ${medication.dose}",
                color = if (medication.takenToday) TextSecondary else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textDecoration = if (medication.takenToday) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(
                text = "${medication.schedule} • Prescripción Diaria",
                color = TextTertiary,
                fontSize = 11.sp,
            )
        }
        HealthBadge(
            text = if (medication.takenToday) "Tomado" else "Pendiente",
            color = if (medication.takenToday) MintSuccess else AmberWarning,
            backgroundColor = if (medication.takenToday) MintDeep else AmberDeep,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AlertsCompactCard(alerts: List<Alert>) {
    HealthCard(
        containerColor = PanelSurface,
        borderColor = BorderSubtle,
    ) {
        Text(
            text = "ALERTAS CLÍNICAS ACTIVAS",
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(10.dp))

        if (alerts.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = MintSuccess, size = 8.dp)
                Spacer(Modifier.width(8.dp))
                Text("Sin eventos críticos detectados.", color = MintSuccess, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            alerts.forEach { alert ->
                val isCritical = alert.status.name == "CRITICAL"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(color = if (isCritical) CoralBright else AmberWarning, isPulsing = isCritical, size = 8.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(alert.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    HealthBadge(
                        text = alert.status.name,
                        color = if (isCritical) CoralBright else AmberWarning,
                        backgroundColor = if (isCritical) CoralDeep else AmberDeep,
                        fontSize = 10.sp,
                    )
                }
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
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 28.dp
    val heartSeries = measurements.filter { it.metricType == MetricType.HEART_RATE }.map { it.value.toFloat() }.reversed()
    val spo2Series = measurements.filter { it.metricType == MetricType.SPO2 }.map { it.value.toFloat() }.reversed()
    val heartRange = chartRange(heartSeries, 60f, 100f)
    val spo2Range = chartRange(spo2Series, 90f, 100f)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Histórico de Métricas",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                    )
                    FilterButton()
                }
            }

            if (sizeInfo.isCompact && !sizeInfo.isLandscape) {
                // Stacked charts on portrait phones
                item {
                    ModernChartCard(
                        title = "Frecuencia Cardíaca (bpm)",
                        endpoint = "GET /v1/measurements",
                        values = heartSeries,
                        minValue = heartRange.first,
                        maxValue = heartRange.second,
                        lineColor = CoralBright,
                        fillColor = CoralBright.copy(alpha = 0.16f),
                    )
                }
                item {
                    ModernChartCard(
                        title = "Saturación SpO2 (%)",
                        endpoint = null,
                        values = spo2Series,
                        minValue = spo2Range.first,
                        maxValue = spo2Range.second,
                        lineColor = BlueElectric,
                        fillColor = BlueElectric.copy(alpha = 0.16f),
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
                            ModernChartCard(
                                title = "Frecuencia Cardíaca (bpm)",
                                endpoint = "GET /v1/measurements",
                                values = heartSeries,
                                minValue = heartRange.first,
                                maxValue = heartRange.second,
                                lineColor = CoralBright,
                                fillColor = CoralBright.copy(alpha = 0.16f),
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            ModernChartCard(
                                title = "Saturación SpO2 (%)",
                                endpoint = null,
                                values = spo2Series,
                                minValue = spo2Range.first,
                                maxValue = spo2Range.second,
                                lineColor = BlueElectric,
                                fillColor = BlueElectric.copy(alpha = 0.16f),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "LECTURAS RECIENTES",
                    color = BlueBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                )
            }

            item {
                if (measurements.isEmpty()) {
                    Text(
                        "Aún no hay mediciones registradas.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
                } else {
                    val chunks = measurements.take(6).chunked(if (sizeInfo.isCompact && !sizeInfo.isLandscape) 1 else 2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowItems.forEach { measurement ->
                                    HealthCard(Modifier.weight(1f), containerColor = PanelDeep, borderColor = BorderSubtle) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                measurement.metricType.name,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            )
                                            Text(
                                                "${measurement.value} ${measurement.unit}",
                                                color = TealBright,
                                                fontWeight = FontWeight.Black,
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
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .border(1.dp, BorderMedium, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Últimos 7 Días", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ModernChartCard(
    title: String,
    endpoint: String?,
    values: List<Float>,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
    fillColor: Color,
) {
    HealthCard(
        containerColor = PanelSurface,
        borderColor = BorderSubtle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            endpoint?.let {
                val label = if (com.healthos.BuildConfig.DEBUG) it else "Historial 7D"
                HealthBadge(label, CoralBright, CoralDeep, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (values.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sin registros para graficar", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LineChart(
                values = values,
                minValue = minValue,
                maxValue = maxValue,
                line = lineColor,
                fill = fillColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
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
            val left = 36.dp.toPx()
            val bottom = 22.dp.toPx()
            val chartWidth = size.width - left - 8.dp.toPx()
            val chartHeight = size.height - bottom - 8.dp.toPx()
            val stepX = chartWidth / (values.size - 1).coerceAtLeast(1)
            val yScale = chartHeight / (maxValue - minValue).coerceAtLeast(0.001f)
            val points = values.mapIndexed { index, value ->
                Offset(
                    x = left + stepX * index,
                    y = 8.dp.toPx() + (maxValue - value) * yScale,
                )
            }

            // Grid Horizontal Lines
            repeat(4) { index ->
                val y = 8.dp.toPx() + chartHeight * index / 3f
                drawLine(BorderSubtle, Offset(left, y), Offset(left + chartWidth, y), 1.dp.toPx())
            }

            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            val area = Path().apply {
                moveTo(points.first().x, 8.dp.toPx() + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, 8.dp.toPx() + chartHeight)
                close()
            }

            drawPath(area, fill)
            drawPath(path, line, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            points.forEach { point ->
                drawCircle(MidnightInk, radius = 4.dp.toPx(), center = point)
                drawCircle(line, radius = 3.dp.toPx(), center = point)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { Text(it, color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
        }
    }
}

// -------------------------------------------------------------
// SOS SCREEN CON PULSO LUMINOSO RADIANTE MULTIANILLO CONTINUO
// -------------------------------------------------------------
@Composable
private fun SosScreen(
    modifier: Modifier,
    sizeInfo: WindowSizeInfo,
    currentLocation: Pair<Double, Double>?,
    onStartLocationUpdates: () -> Unit,
    onSos: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val horizontalPadding = if (sizeInfo.isCompact) 20.dp else 40.dp

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            onStartLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        val missing = locationPermissions.filterNot {
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            locationPermissionLauncher.launch(missing.toTypedArray())
        } else {
            onStartLocationUpdates()
        }
    }

    val locationText = currentLocation?.let {
        "Lat: ${String.format(java.util.Locale.US, "%.5f", it.first)} | Lng: ${String.format(java.util.Locale.US, "%.5f", it.second)}"
    } ?: "Obteniendo coordenadas satelitales GPS..."

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightInk),
        contentAlignment = Alignment.Center,
    ) {
        if (sizeInfo.isLandscape) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1000.dp)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    RadiantSosButton(onSos = onSos, compactSize = true)
                }

                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.Center,
                ) {
                    HealthBadge("MÓDULO SOS EMERGENCIA", CoralBright, CoralDeep, fontSize = 11.sp, hasDot = true)
                    Spacer(Modifier.height(10.dp))
                    Text("Alerta Crítica Inmediata", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Presiona el botón para transmitir tu ubicación GPS actual y alertar de inmediato a tus cuidadores.",
                        color = BlueBright,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    GpsInfoCard(locationText = locationText)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HealthBadge("MÓDULO SOS EMERGENCIA", CoralBright, CoralDeep, fontSize = 11.sp, hasDot = true)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Alerta Crítica Inmediata",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Presiona el botón de emergencia para transmitir tu posición GPS satelital en cola expedita prioritaria a tus cuidadores.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(28.dp))

                RadiantSosButton(onSos = onSos, compactSize = false)

                Spacer(Modifier.height(28.dp))
                GpsInfoCard(locationText = locationText)
            }
        }
    }
}

@Composable
private fun RadiantSosButton(
    onSos: () -> Unit,
    compactSize: Boolean,
) {
    val outerSize = if (compactSize) 190.dp else 230.dp
    val innerSize = if (compactSize) 140.dp else 170.dp

    // Multi-ring glowing pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave1_scale",
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave1_alpha",
    )

    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave2_scale",
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave2_alpha",
    )

    Box(
        modifier = Modifier.size(outerSize),
        contentAlignment = Alignment.Center,
    ) {
        // Outer radiating pulse rings
        Box(
            modifier = Modifier
                .size(innerSize)
                .scale(wave1Scale)
                .clip(CircleShape)
                .background(CoralCritical.copy(alpha = wave1Alpha)),
        )
        Box(
            modifier = Modifier
                .size(innerSize)
                .scale(wave2Scale)
                .clip(CircleShape)
                .background(CoralBright.copy(alpha = wave2Alpha)),
        )

        // Core Interactive SOS Button
        Button(
            onClick = onSos,
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .border(2.dp, CoralBright, CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralCritical,
                contentColor = TextPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "SOS",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(2.dp))
                val sosSub = if (com.healthos.BuildConfig.DEBUG) "POST /v1/alerts/sos" else "EMERGENCIA"
                Text(
                    text = sosSub,
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun GpsInfoCard(locationText: String) {
    HealthCard(
        containerColor = PanelDeep,
        borderColor = BorderSubtle,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, tint = CoralBright, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Coordenadas GPS Satelitales:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = locationText,
            color = TealBright,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Expedited Critical SyncWorker Outbox Active",
            color = TextTertiary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// -------------------------------------------------------------
// BLE DEVICES SCREEN
// -------------------------------------------------------------
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
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 28.dp
    val context = LocalContext.current
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            onScan()
        }
    }

    fun requestPermissionsAndScan() {
        val missing = requiredPermissions.filterNot {
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
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
            modifier = Modifier
                .widthIn(max = 1000.dp)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "Dispositivos & BLE",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            item {
                AdapterSelector()
            }
            item {
                HealthCard(
                    containerColor = PanelSurface,
                    borderColor = BorderSubtle,
                ) {
                    Text(
                        text = "Escáner BLE Wearable",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(10.dp))

                    if (bleState == BleState.Scanning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(color = TealBright, isPulsing = true, size = 10.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Buscando dispositivos BLE...", color = TealBright, fontSize = 13.sp)
                            }
                            Button(
                                onClick = onStopScan,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralCritical),
                                shape = ButtonShape,
                                modifier = Modifier.height(42.dp),
                            ) {
                                Text("Detener", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        HealthButton(
                            text = "Buscar Dispositivos BLE",
                            onClick = { requestPermissionsAndScan() },
                            variant = ButtonVariant.PRIMARY,
                            icon = Icons.Filled.Bluetooth,
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    val bleErrorMessage = (bleState as? BleState.Error)?.message
                    if (bleErrorMessage != null) {
                        Text(bleErrorMessage, color = CoralBright, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (scannedDevices.isEmpty()) {
                        Text(
                            "Apunta a tu banda wearable para detectarla. Detecta dispositivos con servicio de frecuencia cardíaca GATT (0x180D).",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    } else {
                        scannedDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PanelDeep)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(device.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(device.mac, color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                HealthButton(
                                    text = "Vincular",
                                    onClick = { onConnect(device) },
                                    variant = ButtonVariant.PRIMARY,
                                    height = 38.dp,
                                    modifier = Modifier.width(96.dp),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "DISPOSITIVOS VINCULADOS",
                        color = BlueBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    HealthBadge(
                        "${devices.size} VINCULADOS",
                        BlueBright,
                        BlueDeep,
                        fontSize = 10.sp,
                    )
                }
            }

            if (devices.isEmpty()) {
                item {
                    HealthCard(containerColor = PanelDeep, borderColor = BorderSubtle) {
                        Text("Ningún dispositivo vinculado", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Inicia el escáner BLE para conectar tu dispositivo médico.",
                            color = TextSecondary,
                            fontSize = 12.sp,
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
                HealthCard(containerColor = PanelSurface, borderColor = BorderSubtle) {
                    Text("Sincronización en Segundo Plano", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Las mediciones se capturan automáticamente en SQLite cifrado y se envían periódicamente al backend con WorkManager.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
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

    HealthCard(containerColor = PanelSurface, borderColor = BorderSubtle) {
        Text("Adaptador de Hardware (`bluetooth`):", color = BlueBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = darkFieldColors(),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
                shape = ButtonShape,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(PanelSurface),
            ) {
                listOf(
                    "XiaomiBandAdapter (Protocolo Propietario)",
                    "GenericGattAdapter (BLE Standard)",
                    "GarminAdapter (Protocolo Propietario)",
                ).forEach {
                    DropdownMenuItem(
                        text = { Text(it, color = TextPrimary, fontSize = 13.sp) },
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
    HealthCard(containerColor = PanelSurface, borderColor = BorderSubtle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealContainer)
                    .border(1.dp, TealBright.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Bluetooth, null, tint = TealBright, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.model, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("MAC: ${device.id}", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            HealthBadge("GATT OK", TealBright, TealContainer, fontSize = 10.sp)
            IconButton(onClick = { onUnlink(device.id) }) {
                Icon(Icons.Filled.Delete, null, tint = CoralBright, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// PROFILE & SETTINGS SCREEN
// -------------------------------------------------------------
@Composable
private fun ProfileScreen(
    modifier: Modifier,
    healthProfile: HealthProfile?,
    sizeInfo: WindowSizeInfo,
    onLogout: () -> Unit,
) {
    val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 28.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "Perfil y Seguridad",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            item {
                HealthCard(containerColor = PanelSurface, borderColor = BorderSubtle) {
                    Text("PERFIL CLÍNICO DEL PACIENTE", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileMetric("Peso", healthProfile?.let { "${it.weightKg} kg" } ?: "—", Modifier.weight(1f))
                        ProfileMetric("Altura", healthProfile?.let { "${it.heightCm} cm" } ?: "—", Modifier.weight(1f))
                        ProfileMetric("Sangre", healthProfile?.bloodType ?: "—", Modifier.weight(1f))
                    }
                }
            }
            item {
                HealthCard(containerColor = PanelSurface, borderColor = BorderSubtle) {
                    Text("Seguridad y Privacidad", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    SecurityRow("Base de Datos Local", "SQLCipher AES-256")
                    SecurityRow("Cifrado de Tokens", "EncryptedSharedPreferences")
                    SecurityRow("Comunicaciones", "TLS 1.3 / HTTPS")
                }
            }
            item {
                HealthButton(
                    text = "Cerrar Sesión",
                    onClick = onLogout,
                    variant = ButtonVariant.DANGER,
                    icon = Icons.Filled.Person,
                )
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
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDeep)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = BlueBright, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
private fun SecurityRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TealBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = PanelDeep,
    unfocusedContainerColor = PanelDeep,
    focusedBorderColor = TealBright,
    unfocusedBorderColor = BorderSubtle,
    focusedTrailingIconColor = TextPrimary,
    unfocusedTrailingIconColor = TextSecondary,
)
