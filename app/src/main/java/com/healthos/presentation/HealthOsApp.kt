package com.healthos.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthos.domain.model.Role
import com.healthos.presentation.auth.AuthViewModel
import com.healthos.presentation.biometric.BiometricLockScreen
import com.healthos.presentation.biometric.BiometricOptInDialog
import com.healthos.presentation.caregiver.CaregiverHome
import com.healthos.presentation.common.ProvideWindowSizeInfo
import com.healthos.presentation.patient.PatientHome
import com.healthos.presentation.theme.BlueElectric
import com.healthos.presentation.theme.BorderMedium
import com.healthos.presentation.theme.BorderSubtle
import com.healthos.presentation.theme.ButtonShape
import com.healthos.presentation.theme.ButtonVariant
import com.healthos.presentation.theme.CardShape
import com.healthos.presentation.theme.HealthBadge
import com.healthos.presentation.theme.HealthButton
import com.healthos.presentation.theme.HealthCard
import com.healthos.presentation.theme.HealthTextField
import com.healthos.presentation.theme.MidnightInk
import com.healthos.presentation.theme.PanelDeep
import com.healthos.presentation.theme.PanelSurface
import com.healthos.presentation.theme.SurfaceElevated
import com.healthos.presentation.theme.TealBright
import com.healthos.presentation.theme.TealContainer
import com.healthos.presentation.theme.TealPrimary
import com.healthos.presentation.theme.TextPrimary
import com.healthos.presentation.theme.TextSecondary
import com.healthos.presentation.theme.TextTertiary

@Composable
fun HealthOsApp(viewModel: AuthViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val session by viewModel.session.collectAsState(initial = null)
    val authState by viewModel.authState.collectAsState()
    val loginResult by viewModel.loginResult.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsState()
    val showBiometricOptIn by viewModel.showBiometricOptIn.collectAsState()
    val biometricError by viewModel.biometricError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState.error) {
        authState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    if (session != null && isBiometricEnabled && !isSessionUnlocked) {
        BiometricLockScreen(
            error = biometricError,
            onUnlock = {
                if (activity != null) {
                    viewModel.unlockWithBiometric(activity)
                }
            },
            onLogout = viewModel::logout,
        )
    } else {
        Scaffold(
            containerColor = MidnightInk,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            when (session?.role) {
                Role.PATIENT -> {
                    PatientHome(modifier = Modifier.padding(padding), onLogout = viewModel::logout)
                    if (showBiometricOptIn && activity != null) {
                        BiometricOptInDialog(
                            onEnable = { viewModel.enableBiometricFromOptIn(activity) },
                            onDismiss = viewModel::dismissBiometricOptIn,
                        )
                    }
                }
                Role.CAREGIVER -> {
                    CaregiverHome(modifier = Modifier.padding(padding), onLogout = viewModel::logout)
                    if (showBiometricOptIn && activity != null) {
                        BiometricOptInDialog(
                            onEnable = { viewModel.enableBiometricFromOptIn(activity) },
                            onDismiss = viewModel::dismissBiometricOptIn,
                        )
                    }
                }
                null ->
                    AuthFlow(
                        modifier = Modifier.padding(padding),
                        loading = authState.loading,
                        loginResult = loginResult,
                        onLogin = viewModel::login,
                        onLoginResultConsumed = viewModel::clearLoginResult,
                        onRegister = viewModel::register,
                        onVerifyEmail = viewModel::verifyEmail,
                        onVerify2FA = viewModel::verify2FA,
                        onResend2FA = viewModel::resend2FA,
                        onForgot = viewModel::forgotPassword,
                        onOnboarding = viewModel::saveHealthProfile,
                    )
            }
        }
    }
}

@Composable
private fun AuthFlow(
    modifier: Modifier,
    loading: Boolean,
    loginResult: Boolean?,
    onLogin: (String, String) -> Unit,
    onLoginResultConsumed: () -> Unit,
    onRegister: (String, String, Role, String, String) -> Unit,
    onVerifyEmail: (String, String) -> Unit,
    onVerify2FA: (String, String) -> Unit,
    onResend2FA: (String) -> Unit,
    onForgot: (String) -> Unit,
    onOnboarding: (Double, Int, String) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var currentEmail by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(loginResult) {
        if (loginResult == true) {
            step = 3
        }
        onLoginResultConsumed()
    }

    ProvideWindowSizeInfo { sizeInfo ->
        val horizontalPadding = if (sizeInfo.isCompact) 16.dp else 32.dp
        val verticalPadding = if (sizeInfo.isLandscape) 12.dp else 24.dp

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF040D24),
                            MidnightInk,
                            Color(0xFF030816),
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center,
        ) {
            HealthCard(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                containerColor = PanelSurface.copy(alpha = 0.95f),
                borderColor = BorderMedium,
                shape = CardShape,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Header Brand
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(TealContainer, SurfaceElevated)
                                )
                            )
                            .border(1.5.dp, TealBright.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonitorHeart,
                            contentDescription = null,
                            tint = TealBright,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "HEALTH OS",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    HealthBadge(
                        text = "SISTEMA AUTÓNOMO DE SALUD • 2FA",
                        color = TealBright,
                        backgroundColor = TealContainer,
                        hasDot = true,
                        fontSize = 10.sp,
                    )

                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width / 4 } + fadeIn(tween(250)))
                                    .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally { width -> -width / 4 } + fadeIn(tween(250)))
                                    .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut(tween(200)))
                            }
                        },
                        label = "AuthStepAnimation",
                    ) { currentStep ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            when (currentStep) {
                                0 -> WelcomeScreen(
                                    onLogin = { step = 1 },
                                    onRegister = { step = 2 },
                                    onVerifyEmail = { step = 6 },
                                )
                                1 -> LoginScreen(
                                    loading = loading,
                                    onLogin = { email, pass ->
                                        currentEmail = email
                                        onLogin(email, pass)
                                    },
                                    onForgot = { step = 4 },
                                    onVerifyEmail = { step = 6 },
                                    onBack = { step = 0 },
                                )
                                2 -> RegisterScreen(
                                    loading = loading,
                                    onRegister = { email, pass, role, first, last ->
                                        currentEmail = email
                                        onRegister(email, pass, role, first, last)
                                        step = 3
                                    },
                                    onBack = { step = 0 },
                                )
                                3 -> TwoFactorVerifyScreen(
                                    loading = loading,
                                    email = currentEmail,
                                    onVerify = onVerify2FA,
                                    onResend = onResend2FA,
                                    onVerifyEmail = { step = 6 },
                                    onBack = { step = 0 },
                                )
                                4 -> ForgotPasswordScreen(
                                    loading = loading,
                                    onForgot = onForgot,
                                    onBack = { step = 1 },
                                )
                                5 -> OnboardingScreen(
                                    loading = loading,
                                    onOnboarding = onOnboarding,
                                )
                                6 -> VerifyEmailScreen(
                                    loading = loading,
                                    email = currentEmail,
                                    onVerify = { em, token ->
                                        onVerifyEmail(em, token)
                                        step = 1
                                    },
                                    onResend = onResend2FA,
                                    onGoTo2FA = { step = 3 },
                                    onBack = { step = 0 },
                                )
                            }
                        }
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
    onVerifyEmail: () -> Unit,
) {
    Text(
        text = "Bienvenido a tu plataforma médica",
        color = TextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))

    HealthButton(
        text = "Iniciar Sesión",
        onClick = onLogin,
        variant = ButtonVariant.PRIMARY,
        icon = Icons.Filled.Lock,
    )
    Spacer(Modifier.height(12.dp))

    HealthButton(
        text = "Crear Cuenta Nueva",
        onClick = onRegister,
        variant = ButtonVariant.SECONDARY,
        icon = Icons.Filled.Person,
    )
    Spacer(Modifier.height(12.dp))

    HealthButton(
        text = "Verificar Código de Correo",
        onClick = onVerifyEmail,
        variant = ButtonVariant.OUTLINE,
        icon = Icons.Filled.Email,
        height = 46.dp,
    )
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    onForgot: () -> Unit,
    onVerifyEmail: () -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text(
        text = "Acceso Clínico Seguro",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(16.dp))

    HealthTextField(
        value = email,
        onValueChange = { email = it },
        label = "Correo Electrónico",
        leadingIcon = Icons.Filled.Email,
    )
    Spacer(Modifier.height(12.dp))

    HealthTextField(
        value = password,
        onValueChange = { password = it },
        label = "Contraseña",
        leadingIcon = Icons.Filled.Lock,
        visualTransformation = PasswordVisualTransformation(),
    )
    Spacer(Modifier.height(18.dp))

    HealthButton(
        text = "Ingresar",
        onClick = { onLogin(email, password) },
        loading = loading,
        variant = ButtonVariant.PRIMARY,
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HealthButton(
            text = "Olvidé clave",
            onClick = onForgot,
            variant = ButtonVariant.GHOST,
            height = 40.dp,
            modifier = Modifier.weight(1f),
        )
        HealthButton(
            text = "Validar OTP",
            onClick = onVerifyEmail,
            variant = ButtonVariant.GHOST,
            height = 40.dp,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(6.dp))
    HealthButton(
        text = "Volver al inicio",
        onClick = onBack,
        variant = ButtonVariant.GHOST,
        height = 40.dp,
    )
}

@Composable
private fun RegisterScreen(
    loading: Boolean,
    onRegister: (String, String, Role, String, String) -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.PATIENT) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    Text(
        text = "Registro de Nuevo Usuario",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f)) {
            HealthTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = "Nombre",
            )
        }
        Box(Modifier.weight(1f)) {
            HealthTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Apellido",
            )
        }
    }
    Spacer(Modifier.height(10.dp))

    HealthTextField(
        value = email,
        onValueChange = { email = it },
        label = "Correo Electrónico",
        leadingIcon = Icons.Filled.Email,
    )
    Spacer(Modifier.height(10.dp))

    HealthTextField(
        value = password,
        onValueChange = { password = it },
        label = "Contraseña Segura",
        leadingIcon = Icons.Filled.Lock,
        visualTransformation = PasswordVisualTransformation(),
    )
    Spacer(Modifier.height(12.dp))

    // Role selection tabs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ButtonShape)
            .background(PanelDeep)
            .border(1.dp, BorderSubtle, ButtonShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Button(
            onClick = { role = Role.PATIENT },
            modifier = Modifier.weight(1f).height(42.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (role == Role.PATIENT) TealPrimary else Color.Transparent,
                contentColor = if (role == Role.PATIENT) MidnightInk else TextSecondary,
            ),
            shape = ButtonShape,
        ) {
            Text("Paciente", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Button(
            onClick = { role = Role.CAREGIVER },
            modifier = Modifier.weight(1f).height(42.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (role == Role.CAREGIVER) TealPrimary else Color.Transparent,
                contentColor = if (role == Role.CAREGIVER) MidnightInk else TextSecondary,
            ),
            shape = ButtonShape,
        ) {
            Text("Cuidador", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(16.dp))

    HealthButton(
        text = "Crear Cuenta y Verificar",
        onClick = { onRegister(email, password, role, firstName, lastName) },
        loading = loading,
        variant = ButtonVariant.PRIMARY,
    )
    Spacer(Modifier.height(8.dp))

    HealthButton(
        text = "Volver al inicio",
        onClick = onBack,
        variant = ButtonVariant.GHOST,
        height = 40.dp,
    )
}

@Composable
private fun VerifyEmailScreen(
    loading: Boolean,
    email: String,
    onVerify: (String, String) -> Unit,
    onResend: (String) -> Unit,
    onGoTo2FA: () -> Unit,
    onBack: () -> Unit,
) {
    var userEmail by remember { mutableStateOf(email) }
    var code by remember { mutableStateOf("") }

    Text(
        text = "Verificación de Correo",
        color = TealBright,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Ingresa el código OTP o token enviado a tu correo electrónico.",
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))

    HealthTextField(
        value = userEmail,
        onValueChange = { userEmail = it },
        label = "Email registrado",
        leadingIcon = Icons.Filled.Email,
    )
    Spacer(Modifier.height(10.dp))

    HealthTextField(
        value = code,
        onValueChange = { if (it.length <= 32) code = it.trim() },
        label = "Código Token de Verificación",
        leadingIcon = Icons.Filled.Pin,
    )
    Spacer(Modifier.height(16.dp))

    HealthButton(
        text = "Validar Correo",
        onClick = { onVerify(userEmail, code) },
        loading = loading,
        variant = ButtonVariant.PRIMARY,
    )
    Spacer(Modifier.height(10.dp))

    HealthButton(
        text = "Reenviar código de verificación",
        onClick = { onResend(userEmail) },
        variant = ButtonVariant.OUTLINE,
        height = 46.dp,
    )
    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HealthButton(
            text = "Ir a 2FA",
            onClick = onGoTo2FA,
            variant = ButtonVariant.GHOST,
            height = 40.dp,
            modifier = Modifier.weight(1f),
        )
        HealthButton(
            text = "Volver",
            onClick = onBack,
            variant = ButtonVariant.GHOST,
            height = 40.dp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TwoFactorVerifyScreen(
    loading: Boolean,
    email: String,
    onVerify: (String, String) -> Unit,
    onResend: (String) -> Unit,
    onVerifyEmail: () -> Unit,
    onBack: () -> Unit,
) {
    var userEmail by remember { mutableStateOf(email) }
    var code by remember { mutableStateOf("") }

    Text(
        text = "Verificación 2FA (OTP)",
        color = TealBright,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Hemos enviado un código numérico de 6 dígitos válido por 10 minutos.",
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))

    HealthTextField(
        value = userEmail,
        onValueChange = { userEmail = it },
        label = "Email registrado",
        leadingIcon = Icons.Filled.Email,
    )
    Spacer(Modifier.height(10.dp))

    HealthTextField(
        value = code,
        onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
        label = "Código de 6 dígitos",
        leadingIcon = Icons.Filled.Pin,
    )
    Spacer(Modifier.height(18.dp))

    HealthButton(
        text = "Confirmar 2FA y Entrar",
        onClick = { onVerify(userEmail, code) },
        loading = loading,
        variant = ButtonVariant.PRIMARY,
    )
    Spacer(Modifier.height(10.dp))

    HealthButton(
        text = "Reenviar código de acceso",
        onClick = { onResend(userEmail) },
        variant = ButtonVariant.OUTLINE,
        height = 46.dp,
    )
    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HealthButton(
            text = "Validar correo",
            onClick = onVerifyEmail,
            variant = ButtonVariant.GHOST,
            height = 40.dp,
            modifier = Modifier.weight(1f),
        )
        HealthButton(
            text = "Volver",
            onClick = onBack,
            variant = ButtonVariant.GHOST,
            height = 40.dp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ForgotPasswordScreen(
    loading: Boolean,
    onForgot: (String) -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("") }

    Text(
        text = "Recuperar Contraseña",
        color = TealBright,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Te enviaremos un enlace seguro para restablecer tu contraseña.",
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))

    HealthTextField(
        value = email,
        onValueChange = { email = it },
        label = "Email registrado",
        leadingIcon = Icons.Filled.Email,
    )
    Spacer(Modifier.height(18.dp))

    HealthButton(
        text = "Enviar Enlace Seguro",
        onClick = { onForgot(email) },
        loading = loading,
        variant = ButtonVariant.PRIMARY,
    )
    Spacer(Modifier.height(8.dp))

    HealthButton(
        text = "Volver al inicio de sesión",
        onClick = onBack,
        variant = ButtonVariant.GHOST,
        height = 40.dp,
    )
}

@Composable
private fun OnboardingScreen(
    loading: Boolean,
    onOnboarding: (Double, Int, String) -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }

    Text(
        text = "Completa tu Perfil Clínico",
        color = TealBright,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Estos datos permiten al motor de riesgo TFLite evaluar tus métricas corporales.",
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))

    HealthTextField(
        value = weight,
        onValueChange = { weight = it },
        label = "Peso (kg)",
    )
    Spacer(Modifier.height(10.dp))

    HealthTextField(
        value = height,
        onValueChange = { height = it },
        label = "Altura (cm)",
    )
    Spacer(Modifier.height(10.dp))

    HealthTextField(
        value = bloodType,
        onValueChange = { bloodType = it },
        label = "Grupo Sanguíneo (ej. O+, A-)",
    )
    Spacer(Modifier.height(18.dp))

    HealthButton(
        text = "Guardar Perfil",
        onClick = {
            val w = weight.toDoubleOrNull()
            val h = height.toIntOrNull()
            if (w != null && h != null && w in 20.0..300.0 && h in 50..250 && bloodType.isNotBlank()) {
                onOnboarding(w, h, bloodType.trim().uppercase())
            }
        },
        loading = loading,
        variant = ButtonVariant.PRIMARY,
    )
}

// Global Reusable Adaptive HealthScaffold
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
            // Adaptive Landscape & Tablet layout with side Navigation Rail
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(MidnightInk)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                NavigationRail(
                    containerColor = PanelSurface,
                    contentColor = TextPrimary,
                    header = {
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TealContainer)
                                    .border(1.dp, TealBright, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("H", color = TealBright, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(title, color = BlueElectric, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                ) {
                    Spacer(Modifier.weight(1f))
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selected == index
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { onSelect(index) },
                            icon = {},
                            label = {
                                Text(
                                    tab,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = TealBright,
                                indicatorColor = TealContainer,
                            ),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Salir", tint = TextTertiary)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    content(Modifier.fillMaxSize())
                }
            }
        } else {
            // Compact Portrait layout with bottom Navigation Bar
            Scaffold(
                modifier = modifier,
                containerColor = MidnightInk,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(TealBright),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onLogout) {
                                Icon(
                                    Icons.Filled.ExitToApp,
                                    contentDescription = "Salir",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = PanelSurface,
                        ),
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = PanelSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp,
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selected == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { onSelect(index) },
                                icon = {},
                                label = {
                                    Text(
                                        tab,
                                        color = if (isSelected) TealBright else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedTextColor = TealBright,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = TealContainer,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                content(Modifier.padding(padding))
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
    HealthCard(
        modifier = modifier.padding(vertical = 6.dp),
        containerColor = PanelSurface,
        borderColor = BorderSubtle,
    ) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
