package com.healthos.presentation.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER,
    OUTLINE,
    GHOST,
}

@Composable
fun HealthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    shape: Shape = ButtonShape,
) {
    val containerColor = when (variant) {
        ButtonVariant.PRIMARY -> TealPrimary
        ButtonVariant.SECONDARY -> SurfaceElevated
        ButtonVariant.DANGER -> CoralCritical
        ButtonVariant.OUTLINE -> Color.Transparent
        ButtonVariant.GHOST -> Color.Transparent
    }

    val contentColor = when (variant) {
        ButtonVariant.PRIMARY -> MidnightInk
        ButtonVariant.SECONDARY -> TextPrimary
        ButtonVariant.DANGER -> TextPrimary
        ButtonVariant.OUTLINE -> TealBright
        ButtonVariant.GHOST -> TextSecondary
    }

    val border = when (variant) {
        ButtonVariant.OUTLINE -> androidx.compose.foundation.BorderStroke(1.dp, TealDark)
        ButtonVariant.SECONDARY -> androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
        else -> null
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = shape,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.4f),
        ),
        elevation = if (variant == ButtonVariant.PRIMARY) {
            ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
        } else {
            ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        },
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}

@Composable
fun HealthCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    containerColor: Color = PanelSurface,
    borderColor: Color = BorderSubtle,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = TealBright.copy(alpha = 0.2f)),
            onClick = onClick,
        )
    } else Modifier

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .then(clickableModifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun HealthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 14.sp) },
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            visualTransformation = visualTransformation,
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, null, tint = if (isError) CoralBright else TealBright, modifier = Modifier.size(20.dp)) }
            } else null,
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            shape = InputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextDisabled,
                focusedContainerColor = PanelDeep,
                unfocusedContainerColor = PanelDeep,
                disabledContainerColor = PanelDeep.copy(alpha = 0.5f),
                focusedBorderColor = TealBright,
                unfocusedBorderColor = BorderSubtle,
                errorBorderColor = CoralCritical,
                focusedLabelColor = TealBright,
                unfocusedLabelColor = TextSecondary,
                errorLabelColor = CoralBright,
                cursorColor = TealBright,
            ),
        )
        if (isError && errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = CoralBright,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun HealthBadge(
    text: String,
    color: Color = TealBright,
    backgroundColor: Color = TealContainer,
    modifier: Modifier = Modifier,
    hasDot: Boolean = false,
    fontSize: TextUnit = 11.sp,
) {
    Row(
        modifier = modifier
            .clip(BadgeShape)
            .background(backgroundColor)
            .border(0.5.dp, color.copy(alpha = 0.35f), BadgeShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (hasDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    size: Dp = 8.dp,
) {
    if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "scale",
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha",
        )
        Box(
            modifier = modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha)),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    fontSize: TextUnit = 12.sp,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        modifier = modifier,
    )
}
