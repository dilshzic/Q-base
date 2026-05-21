package com.algorithmx.q_base.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.algorithmx.q_base.ui.theme.QbaseTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.algorithmx.q_base.ui.settings.components.*

data class AvatarTemplate(
    val name: String,
    val emoji: String,
    val startColor: Color,
    val endColor: Color
)

val AVATAR_TEMPLATES = listOf(
    AvatarTemplate("Scholar", "🎓", Color(0xFF6366F1), Color(0xFF4F46E5)),
    AvatarTemplate("Scientist", "🔬", Color(0xFF06B6D4), Color(0xFF0891B2)),
    AvatarTemplate("Tech Pioneer", "💻", Color(0xFF10B981), Color(0xFF059669)),
    AvatarTemplate("Art Creator", "🎨", Color(0xFFF43F5E), Color(0xFFE11D48)),
    AvatarTemplate("Space Voyager", "🪐", Color(0xFF8B5CF6), Color(0xFF7C3AED)),
    AvatarTemplate("Philosopher", "📚", Color(0xFFF59E0B), Color(0xFFD97706))
)

/**
 * Bounce press effect modifier for interactive elements.
 */
fun Modifier.bounceClick(onClick: () -> Unit = {}) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitFirstDown(false)
                    isPressed = true
                    val upEvent = waitForUpOrCancellation()
                    isPressed = false
                    if (upEvent != null) {
                        onClick()
                    }
                }
            }
        }
}

@Composable
fun ProfileParallaxHeader(
    user: com.algorithmx.q_base.data.core.UserEntity?,
    scrollState: androidx.compose.foundation.ScrollState,
    padding: PaddingValues,
    onAvatarClick: () -> Unit
) {
    // Parallax & Fade calculations based on scroll position
    val yOffset = (scrollState.value * 0.4f).dp
    val alpha = (1f - (scrollState.value / 350f)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .graphicsLayer {
                translationY = yOffset.toPx()
                this.alpha = alpha
            }
            .padding(top = padding.calculateTopPadding()),
        contentAlignment = Alignment.Center
    ) {
        ProfileAvatar(
            imageUrl = user?.profilePictureUrl,
            onClick = onAvatarClick
        )
    }
}