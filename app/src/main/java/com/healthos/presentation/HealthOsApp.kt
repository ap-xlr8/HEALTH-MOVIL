package com.healthos.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthos.domain.model.Role
import com.healthos.presentation.auth.AuthViewModel
import com.healthos.presentation.caregiver.CaregiverHome
import com.healthos.presentation.common.ProvideWindowSizeInfo
import com.healthos.presentation.patient.PatientHome

private val AuthBg = Color(0xFF020717)
private val CardBg = Color(0xFF121B2D)
private val CardBorder = Color(0xFF26344E)
private val AccentTeal = Color(0xFF16A394)
private val AccentBlue = Color(0xFF72B7FF)
private val TextMain = Color.White
private val TextMuted = Color(0xFFA8B7D2)

@Composable
fun HealthOsApp(viewModel: AuthViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsState(initial = null)
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState.error) {
        authState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = AuthBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (session?.role) {
            Role.PATIENT -> PatientHome(modifier = Modifier.padding(padding), onLogout = viewModel::logout)
            Role.CAREGIVER -> CaregiverHome(modifier = Modifier.padding(padding), onLogout = viewModel::logout)
            null ->
                AuthFlow(
                    modifier = Modifier.padding(padding),
                    loading = authState.loading,
                    onLogin = viewModel::login,
                    onRegister = viewModel::register,
                    onVerify = viewModel::verifyEmail,
                    onForgot = viewModel::forgotPassword,
                    onOnboarding = viewModel::saveHealthProfile,
                )
        }
    }
}

@Composable
private fun AuthFlow(
    modifier: Modifier,
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, Role, String, String) -> Unit,
    onVerify: (String, String) -> Unit,
    onForgot: (String) -> Unit,
    onOnboarding: (Double, Int, String) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    ProvideWindowSizeInfo { sizeInfo ->
        val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 32.dp
        val verticalPadding = if (sizeInfo.isLandscape) 12.dp else 24.dp

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(AuthBg)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier =
                    Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBg),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Health OS",
                        color = TextMain,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Sistema Autónomo de Salud",
                        color = AccentBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(24.dp))
                    when (step) {
                        0 -> WelcomeScreen(onLogin = { step = 1 }, onRegister = { step = 2 })
                        1 -> LoginScreen(loading, onLogin, onForgot = { step = 4 }, onBack = { step = 0 })
                        2 -> RegisterScreen(loading, onRegister, onNext = { step = 3 }, onBack = { step = 0 })
                        3 -> VerifyEmailScreen(loading, onVerify, onNext = { step = 5 }, onBack = { step = 2 })
                        4 -> ForgotPasswordScreen(loading, onForgot, onBack = { step = 1 })
                        5 -> OnboardingScreen(loading, onOnboarding)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    Button(
        onClick = onLogin,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onRegister,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain),
    ) {
        Text("Registro", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    onForgot: () -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("paciente@ejemplo.com") }
    var password by remember { mutableStateOf("Password123!") }
    Field("Email", email) { email = it }
    PasswordField(password) { password = it }
    ActionButton(loading, "Entrar") { onLogin(email, password) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onForgot, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("Recuperar contraseña", color = AccentBlue)
    }
    Spacer(Modifier.height(4.dp))
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("Volver", color = TextMuted)
    }
}

@Composable
private fun RegisterScreen(
    loading: Boolean,
    onRegister: (String, String, Role, String, String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("paciente@ejemplo.com") }
    var password by remember { mutableStateOf("Password123!") }
    var role by remember { mutableStateOf(Role.PATIENT) }
    var firstName by remember { mutableStateOf("Carlos") }
    var lastName by remember { mutableStateOf("Lopez") }
    Field("Nombre", firstName) { firstName = it }
    Field("Apellido", lastName) { lastName = it }
    Field("Email", email) { email = it }
    PasswordField(password) { password = it }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { role = Role.PATIENT },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = if (role == Role.PATIENT) AccentTeal else Color(0xFF1E2D4A),
                ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Paciente")
        }
        Button(
            onClick = { role = Role.CAREGIVER },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = if (role == Role.CAREGIVER) AccentTeal else Color(0xFF1E2D4A),
                ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Cuidador")
        }
    }
    ActionButton(loading, "Crear cuenta") {
        onRegister(email, password, role, firstName, lastName)
        onNext()
    }
    Spacer(Modifier.height(4.dp))
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("Volver", color = TextMuted)
    }
}

@Composable
private fun VerifyEmailScreen(
    loading: Boolean,
    onVerify: (String, String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("paciente@ejemplo.com") }
    var code by remember { mutableStateOf("847291") }
    Field("Email", email) { email = it }
    Field("Codigo", code) { code = it }
    ActionButton(loading, "Verificar") {
        onVerify(email, code)
        onNext()
    }
    Spacer(Modifier.height(4.dp))
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("Volver", color = TextMuted)
    }
}

@Composable
private fun ForgotPasswordScreen(
    loading: Boolean,
    onForgot: (String) -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("paciente@ejemplo.com") }
    Field("Email", email) { email = it }
    ActionButton(loading, "Enviar reset") { onForgot(email) }
    Spacer(Modifier.height(4.dp))
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("Volver", color = TextMuted)
    }
}

@Composable
private fun OnboardingScreen(
    loading: Boolean,
    onOnboarding: (Double, Int, String) -> Unit,
) {
    var weight by remember { mutableStateOf("75.5") }
    var height by remember { mutableStateOf("180") }
    var bloodType by remember { mutableStateOf("O+") }
    Field("Peso kg", weight) { weight = it }
    Field("Altura cm", height) { height = it }
    Field("Tipo sangre", bloodType) { bloodType = it }
    ActionButton(loading, "Guardar perfil") {
        onOnboarding(weight.toDoubleOrNull() ?: 0.0, height.toIntOrNull() ?: 0, bloodType)
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = CardBorder,
                focusedLabelColor = AccentTeal,
                unfocusedLabelColor = TextMuted,
            ),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Contraseña") },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = CardBorder,
                focusedLabelColor = AccentTeal,
                unfocusedLabelColor = TextMuted,
            ),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ActionButton(
    loading: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextMain) else Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScaffold(
    title: String,
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    ProvideWindowSizeInfo { sizeInfo ->
        if (sizeInfo.useNavRail) {
            // Adaptive Landscape / Tablet layout with side Navigation Rail
            Row(
                modifier = modifier.fillMaxSize().background(AuthBg),
            ) {
                NavigationRail(
                    containerColor = CardBg,
                    contentColor = TextMain,
                    header = {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(title, color = AccentBlue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onLogout,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Salir", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    },
                ) {
                    Spacer(Modifier.weight(1f))
                    tabs.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = selected == index,
                            onClick = { onSelect(index) },
                            icon = {},
                            label = { Text(tab, fontSize = 12.sp, fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    content(Modifier.fillMaxSize().padding(16.dp))
                }
            }
        } else {
            // Compact Portrait layout with bottom Navigation Bar
            Scaffold(
                modifier = modifier,
                containerColor = AuthBg,
                topBar = {
                    TopAppBar(
                        title = { Text(title, color = TextMain, fontWeight = FontWeight.Bold) },
                        actions = {
                            OutlinedButton(
                                onClick = onLogout,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Salir", color = TextMuted)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg),
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = CardBg,
                        contentColor = TextMain,
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { onSelect(index) },
                                icon = {},
                                label = { Text(tab, color = if (selected == index) AccentTeal else TextMuted) },
                            )
                        }
                    }
                },
            ) { padding ->
                content(Modifier.padding(padding).padding(16.dp))
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = TextMuted, fontSize = 14.sp)
        }
    }
}
