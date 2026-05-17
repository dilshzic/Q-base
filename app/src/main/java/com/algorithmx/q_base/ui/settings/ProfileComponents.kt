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
fun ProfileAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isClickable = onClick != null
    val clickModifier = if (isClickable) Modifier.bounceClick { onClick?.invoke() } else Modifier

    val matchedTemplate = remember(imageUrl) {
        if (imageUrl != null && imageUrl.startsWith("emoji:")) {
            val emojiVal = imageUrl.removePrefix("emoji:")
            AVATAR_TEMPLATES.find { it.emoji == emojiVal }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(130.dp)
            .then(clickModifier)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .border(
                width = 4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    )
                ),
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        if (matchedTemplate != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(matchedTemplate.startColor, matchedTemplate.endColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = matchedTemplate.emoji,
                    fontSize = 58.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else if (imageUrl != null && imageUrl.isNotEmpty() && !imageUrl.startsWith("emoji:")) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Default gorgeous gradient avatar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
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

@Composable
fun ProfilePropertyRow(
    label: String,
    value: String,
    isLink: Boolean = false,
    icon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    valueColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val isClickable = onClick != null
    val interactionModifier = if (isClickable) Modifier.clickable { onClick?.invoke() } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(interactionModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor ?: if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isLink || valueColor != null) FontWeight.Bold else FontWeight.Normal
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (isClickable) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfilePropertyToggleRow(
    label: String,
    valueText: String,
    checked: Boolean,
    icon: ImageVector? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}

@Composable
fun ProfileCardSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun ProfileAchievementsProperty(stats: UserStats) {
    ProfileCardSection(title = "Unlocked Milestones") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgeBadge(
                name = "Creator",
                icon = Icons.Rounded.Star,
                unlocked = stats.userCreatedQuestions >= 5,
                color = Color(0xFFFFD700),
                desc = "Created 5+ customized questions"
            )
            BadgeBadge(
                name = "Synergy",
                icon = Icons.Rounded.Group,
                unlocked = stats.sharedQuestions >= 5,
                color = Color(0xFFC0C0C0),
                desc = "Shared 5+ study packets"
            )
            BadgeBadge(
                name = "Innovator",
                icon = Icons.Rounded.Lightbulb,
                unlocked = stats.userCreatedQuestions >= 15,
                color = Color(0xFFE5E4E2),
                desc = "Created 15+ complex questions"
            )
        }
    }
}

@Composable
fun BadgeBadge(
    name: String,
    icon: ImageVector,
    unlocked: Boolean,
    color: Color,
    desc: String
) {
    var showTooltip by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { showTooltip = !showTooltip }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (unlocked) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (unlocked) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (unlocked) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = if (unlocked) "Unlocked" else "Locked",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
        )
        if (showTooltip) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.widthIn(max = 100.dp)
            ) {
                Text(
                    text = desc,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatCard(
    number: String,
    label: String,
    icon: ImageVector,
    gradientColors: List<Color>
) {
    Card(
        modifier = Modifier
            .width(155.dp)
            .height(115.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient accent glow background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                gradientColors[0].copy(alpha = 0.08f),
                                gradientColors[1].copy(alpha = 0.02f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = gradientColors[0].copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = gradientColors[0],
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                Column {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileAvatarPreview() {
    QbaseTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileAvatar(imageUrl = "emoji:🎓", onClick = {})
            ProfileAvatar(imageUrl = null, onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePropertyRowPreview() {
    QbaseTheme {
        Column {
            ProfilePropertyRow(
                label = "Display Name",
                value = "John Doe",
                icon = Icons.Rounded.Badge,
                onClick = {}
            )
            ProfilePropertyRow(
                label = "Email Address",
                value = "john.doe@example.com",
                icon = Icons.Rounded.MailOutline
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePropertyToggleRowPreview() {
    QbaseTheme {
        ProfilePropertyToggleRow(
            label = "Public Visibility",
            valueText = "Visible to other students",
            checked = true,
            icon = Icons.Rounded.Visibility,
            onToggle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileCardSectionPreview() {
    QbaseTheme {
        ProfileCardSection(title = "Account Details") {
            ProfilePropertyRow(
                label = "Display Name",
                value = "John Doe",
                icon = Icons.Rounded.Badge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileAchievementsPropertyPreview() {
    QbaseTheme {
        ProfileAchievementsProperty(
            stats = UserStats(
                userCreatedQuestions = 10,
                sharedQuestions = 5
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BadgeBadgePreview() {
    QbaseTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BadgeBadge(
                name = "Creator",
                icon = Icons.Rounded.Star,
                unlocked = true,
                color = Color(0xFFFFD700),
                desc = "Unlocked!"
            )
            BadgeBadge(
                name = "Creator",
                icon = Icons.Rounded.Star,
                unlocked = false,
                color = Color(0xFFFFD700),
                desc = "Locked!"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatCardPreview() {
    QbaseTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                number = "42",
                label = "Sessions Done",
                icon = Icons.Rounded.TaskAlt,
                gradientColors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
            )
        }
    }
}
