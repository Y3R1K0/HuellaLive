package com.huellalive.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.Outline
import com.huellalive.app.ui.theme.SurfaceRaised
import com.huellalive.app.ui.theme.TextPrimary
import com.huellalive.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun HuellaMessageSnackbar(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = DustyRose
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2800)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier.padding(start = 18.dp, end = 18.dp, bottom = 108.dp),
        enter = slideInVertically { it / 3 } + fadeIn() + scaleIn(initialScale = 0.96f),
        exit = slideOutVertically { it / 4 } + fadeOut() + scaleOut(targetScale = 0.97f)
    ) {
        Surface(
            color = SurfaceRaised,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(color = accent, shape = RoundedCornerShape(50), modifier = Modifier.size(9.dp)) {}
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = message.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun StaggeredReveal(
    index: Int,
    resetKey: Any? = Unit,
    modifier: Modifier = Modifier,
    animatedItemLimit: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    if (index >= animatedItemLimit) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    var visible by remember(resetKey) { mutableStateOf(false) }
    val speed = 0.75f

    LaunchedEffect(resetKey) {
        val cappedIndex = index.coerceAtLeast(0).coerceAtMost(10)
        delay((cappedIndex * (45L / speed)).toLong())
        visible = true
    }

    val revealProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f * speed),
        label = "staggered reveal progress"
    ) {
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = revealProgress
            translationY = (1f - revealProgress) * 18f
            scaleX = 0.97f + (revealProgress * 0.03f)
            scaleY = 0.97f + (revealProgress * 0.03f)
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HuellaLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = DustyRose
) {
    LoadingIndicator(
        modifier = modifier.size(52.dp),
        color = color
    )
}

@Composable
fun ExpressiveAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = DustyRose
) {
    val container by animateColorAsState(
        if (selected) accent.copy(alpha = 0.22f) else SurfaceRaised
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f)
    )
    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        color = container,
        contentColor = if (selected) accent else TextPrimary,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.45f) else Outline)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(14.dp))
        }
    }
}

@Composable
fun TonalChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DustyRose,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.18f),
        contentColor = color,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Spacer(Modifier.width(10.dp))
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (action != null && onAction != null) {
            Surface(
                onClick = onAction,
                color = Color.Transparent,
                contentColor = DustyRose,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    action,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun SupportingText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = TextSecondary,
        style = MaterialTheme.typography.bodyMedium
    )
}
