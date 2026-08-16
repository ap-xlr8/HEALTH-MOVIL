package com.healthos.presentation.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.NotificationPreferences
import com.healthos.domain.model.WearableDevice
import com.healthos.presentation.patient.PatientViewModel
import com.healthos.presentation.theme.AmberWarning
import com.healthos.presentation.theme.BlueElectric
import com.healthos.presentation.theme.BorderSubtle
import com.healthos.presentation.theme.CoralCritical
import com.healthos.presentation.theme.CoralDeep
import com.healthos.presentation.theme.EmeraldAccent
import com.healthos.presentation.theme.MidnightInk
import com.healthos.presentation.theme.MintSuccess
import com.healthos.presentation.theme.PanelSurface
import com.healthos.presentation.theme.PurpleAccent
import com.healthos.presentation.theme.SurfaceElevated
import com.healthos.presentation.theme.TealBright
import com.healthos.presentation.theme.TealDark
import com.healthos.presentation.theme.TealPrimary
import com.healthos.presentation.theme.TextPrimary
import com.healthos.presentation.theme.TextSecondary
import com.healthos.presentation.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PatientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notificationPrefs by viewModel.notificationPreferences.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val healthProfile by viewModel.healthProfile.collectAsState()

    var themeMode by remember { mutableStateOf("SYSTEM") }
    var accentColorName by remember { mutableStateOf("TEAL") }
    var biometricEnabled by remember { mutableStateOf(true) }

    // Patient profile edit state
    var firstName by remember(userProfile) { mutableStateOf(userProfile?.firstName ?: "Paciente") }
    var lastName by remember(userProfile) { mutableStateOf(userProfile?.lastName ?: "") }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var bloodType by remember(healthProfile) { mutableStateOf(healthProfile?.bloodType ?: "O") }
    var weightKg by remember(healthProfile) { mutableStateOf((healthProfile?.weightKg ?: 70.0).toString()) }
    var heightCm by remember(healthProfile) { mutableStateOf((healthProfile?.heightCm ?: 170).toString()) }
    var emergencyName by remember(healthProfile) { mutableStateOf(healthProfile?.emergencyContactName ?: "") }
    var emergencyPhone by remember(healthProfile) { mutableStateOf(healthProfile?.emergencyContactPhone ?: "") }
    var profileSavedNotice by remember { mutableStateOf(false) }

    val accentOptions = listOf(
        "TEAL" to TealPrimary,
        "BLUE" to BlueElectric,
        "PURPLE" to PurpleAccent,
        "EMERALD" to EmeraldAccent,
        "AMBER" to AmberWarning,
        "CORAL" to CoralCritical,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración & Ajustes", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // 1. APARIENCIA Y TEMA
                SectionTitle(icon = Icons.Default.Palette, title = "Apariencia y Personalización")
                Card(
                    colors = CardDefaults.cardColors(containerColor = PanelSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Modo de Tema", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SYSTEM" to "Sistema", "LIGHT" to "Claro", "DARK" to "Oscuro").forEach { (mode, label) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { themeMode = mode },
                                    label = { Text(label, fontSize = 12.sp) },
                                )
                            }
                        }

                        Text("Color de Énfasis / Acento", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            accentOptions.forEach { (name, color) ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (accentColorName == name) 3.dp else 0.dp,
                                            color = if (accentColorName == name) TextPrimary else Color.Transparent,
                                            shape = CircleShape,
                                        )
                                        .clickable { accentColorName = name },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (accentColorName == name) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MidnightInk, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. SEGURIDAD Y BIOMETRÍA
            item {
                SectionTitle(icon = Icons.Default.Security, title = "Seguridad & Acceso")
                Card(
                    colors = CardDefaults.cardColors(containerColor = PanelSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Autenticación Biométrica (Huella / Facial)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Protege tu historial médico con KeyStore AES-256 GCM", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { biometricEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary, checkedTrackColor = TealDark),
                        )
                    }
                }
            }

            // 3. PREFERENCIAS DE NOTIFICACIONES Y HORAS DE DESCANSO
            item {
                SectionTitle(icon = Icons.Default.Notifications, title = "Notificaciones & Horas de Silencio")
                Card(
                    colors = CardDefaults.cardColors(containerColor = PanelSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NotificationToggleRow(
                            label = "Notificaciones Push en tiempo real",
                            checked = notificationPrefs.pushEnabled,
                            onCheckedChange = { viewModel.updateNotificationPreferences(notificationPrefs.copy(pushEnabled = it)) },
                        )
                        NotificationToggleRow(
                            label = "Recordatorios de Medicamentos",
                            checked = notificationPrefs.medicationReminders,
                            onCheckedChange = { viewModel.updateNotificationPreferences(notificationPrefs.copy(medicationReminders = it)) },
                        )
                        NotificationToggleRow(
                            label = "Alertas de Signos Vitales Críticos",
                            checked = notificationPrefs.sosAlerts,
                            onCheckedChange = { viewModel.updateNotificationPreferences(notificationPrefs.copy(sosAlerts = it)) },
                        )
                        NotificationToggleRow(
                            label = "Activar Horas de Silencio (22:00 - 07:00)",
                            checked = notificationPrefs.quietHoursEnabled,
                            onCheckedChange = { viewModel.updateNotificationPreferences(notificationPrefs.copy(quietHoursEnabled = it)) },
                        )
                        NotificationToggleRow(
                            label = "Priorizar Bypass SOS sobre Horas de Silencio",
                            checked = notificationPrefs.bypassQuietHoursForSos,
                            onCheckedChange = { viewModel.updateNotificationPreferences(notificationPrefs.copy(bypassQuietHoursForSos = it)) },
                        )
                    }
                }
            }

            // 4. EDICIÓN DEL PERFIL Y DATOS CLÍNICOS
            item {
                SectionTitle(icon = Icons.Default.Person, title = "Editar Perfil del Paciente")
                Card(
                    colors = CardDefaults.cardColors(containerColor = PanelSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("Nombre") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Apellidos") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono de contacto") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weightKg,
                                onValueChange = { weightKg = it },
                                label = { Text("Peso (kg)") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = heightCm,
                                onValueChange = { heightCm = it },
                                label = { Text("Altura (cm)") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        OutlinedTextField(
                            value = emergencyName,
                            onValueChange = { emergencyName = it },
                            label = { Text("Nombre Contacto Emergencia") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it },
                            label = { Text("Teléfono Emergencia") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Button(
                            onClick = {
                                val parsedWeight = weightKg.toDoubleOrNull() ?: 70.0
                                val parsedHeight = heightCm.toIntOrNull() ?: 170
                                viewModel.updatePatientProfile(
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    phone = phone.trim(),
                                    healthProfile = HealthProfile(
                                        weightKg = parsedWeight,
                                        heightCm = parsedHeight,
                                        bloodType = bloodType,
                                        emergencyContactName = emergencyName.trim(),
                                        emergencyContactPhone = emergencyPhone.trim(),
                                    ),
                                )
                                profileSavedNotice = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Guardar Cambios de Perfil", color = MidnightInk, fontWeight = FontWeight.Bold)
                        }

                        if (profileSavedNotice) {
                            Text("Perfil actualizado exitosamente.", color = MintSuccess, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 5. GESTIÓN DE DISPOSITIVOS WEARABLES
            item {
                SectionTitle(icon = Icons.Default.Watch, title = "Dispositivos & Sensores Vinculados")
                if (devices.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PanelSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No hay wearables vinculados actualmente.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        devices.forEach { dev ->
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
                                        Text(dev.model, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("MAC: ${dev.id} · Batería: ${dev.batteryPercent}%", color = TealBright, fontSize = 12.sp)
                                        Text("Señal RSSI: ${dev.rssi} dBm · Firmware ${dev.firmwareVersion}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.unlinkDevice(dev.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralDeep),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text("Desvincular", color = CoralCritical, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TealBright, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary, checkedTrackColor = TealDark),
        )
    }
}
