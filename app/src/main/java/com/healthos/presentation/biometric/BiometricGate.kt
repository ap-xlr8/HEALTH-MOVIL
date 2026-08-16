package com.healthos.presentation.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private val Ink = Color(0xFF020717)
private val Panel = Color(0xFF121B2D)
private val StrokeLine = Color(0xFF26344E)
private val TextMain = Color.White
private val TextMuted = Color(0xFFA8B7D2)
private val Teal = Color(0xFF16A394)
private val TealBright = Color(0xFF19E3BC)
private val Blue = Color(0xFF72B7FF)
private val PinkSoft = Color(0xFFFF6F91)

@Composable
fun BiometricGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!isBiometricAvailable(context)) {
            unlocked = true
        }
    }

    if (unlocked) {
        content()
    } else {
        BiometricLockScreen(
            error = error,
            onUnlock = {
                val activity = context as? FragmentActivity
                if (activity == null) {
                    unlocked = true
                } else {
                    showBiometricPrompt(
                        activity = activity,
                        onSuccess = { unlocked = true },
                        onError = { message -> error = message },
                    )
                }
            },
        )
    }
}

private fun isBiometricAvailable(context: Context): Boolean {
    return try {
        val manager = BiometricManager.from(context)
        manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Exception) {
        false
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt =
        BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_CANCELED) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    onError("Huella no reconocida. Inténtalo de nuevo.")
                }
            },
        )
    val info =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Health OS")
            .setSubtitle("Usa tu huella para continuar")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
    prompt.authenticate(info)
}

@Composable
private fun BiometricLockScreen(
    error: String?,
    onUnlock: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Teal.copy(alpha = 0.2f))
                        .border(1.dp, TealBright, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, null, tint = TealBright, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Health OS", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Desbloquea con tu huella o PIN del dispositivo.",
                color = TextMuted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text("Desbloquear", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(error, color = PinkSoft, fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text("Los datos clínicos están cifrados en el dispositivo.", color = Blue, fontSize = 11.sp)
        }
    }
}
