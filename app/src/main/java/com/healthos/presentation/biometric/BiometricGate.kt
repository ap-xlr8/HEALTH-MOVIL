package com.healthos.presentation.biometric

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthos.presentation.theme.BlueElectric
import com.healthos.presentation.theme.BorderMedium
import com.healthos.presentation.theme.ButtonVariant
import com.healthos.presentation.theme.CardShape
import com.healthos.presentation.theme.CoralBright
import com.healthos.presentation.theme.HealthBadge
import com.healthos.presentation.theme.HealthButton
import com.healthos.presentation.theme.HealthCard
import com.healthos.presentation.theme.MidnightInk
import com.healthos.presentation.theme.PanelDeep
import com.healthos.presentation.theme.PanelSurface
import com.healthos.presentation.theme.SurfaceElevated
import com.healthos.presentation.theme.TealBright
import com.healthos.presentation.theme.TealContainer
import com.healthos.presentation.theme.TealPrimary
import com.healthos.presentation.theme.TextPrimary
import com.healthos.presentation.theme.TextSecondary

@Composable
fun BiometricLockScreen(
    error: String?,
    onUnlock: () -> Unit,
    onLogout: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onUnlock()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030919),
                        MidnightInk,
                        Color(0xFF040A1A),
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        HealthCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(24.dp),
            containerColor = PanelSurface.copy(alpha = 0.95f),
            borderColor = BorderMedium,
            shape = CardShape,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Biometric animated pulse icon
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(TealBright.copy(alpha = pulseAlpha * 0.3f)),
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(TealContainer, SurfaceElevated)
                                )
                            )
                            .border(1.5.dp, TealBright.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            contentDescription = "Huella Dactilar",
                            tint = TealBright,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Health OS",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
                HealthBadge(
                    text = "ACCESO BIOMÉTRICO CIFRADO",
                    color = TealBright,
                    backgroundColor = TealContainer,
                    hasDot = true,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Desbloquea con tu huella dactilar o reconocimiento facial para acceder a tus datos clínicos.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                HealthButton(
                    text = "Desbloquear con biometría",
                    onClick = onUnlock,
                    variant = ButtonVariant.PRIMARY,
                    icon = Icons.Filled.Fingerprint,
                )

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = CoralBright,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(12.dp))

                HealthButton(
                    text = "Iniciar con correo y clave",
                    onClick = onLogout,
                    variant = ButtonVariant.GHOST,
                    icon = Icons.Filled.Lock,
                    height = 44.dp,
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Shield, null, tint = BlueElectric, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Almacenamiento protegido por SQLCipher",
                        color = BlueElectric,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricOptInDialog(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelSurface,
        shape = CardShape,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(TealContainer)
                    .border(1.dp, TealBright.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = TealBright,
                    modifier = Modifier.size(32.dp),
                )
            }
        },
        title = {
            Text(
                text = "¿Activar inicio con biometría?",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Accede a Health OS al instante mediante huella dactilar o Face Unlock sin necesidad de reintroducir tus credenciales en cada sesión.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            HealthButton(
                text = "Activar Biometría",
                onClick = onEnable,
                variant = ButtonVariant.PRIMARY,
                height = 44.dp,
            )
        },
        dismissButton = {
            HealthButton(
                text = "Ahora no",
                onClick = onDismiss,
                variant = ButtonVariant.GHOST,
                height = 44.dp,
            )
        },
    )
}
