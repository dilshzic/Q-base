package com.algorithmx.q_base.core.designsystem.components.reusable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.feature.sessions.data.StudySession
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.state.AppAccessState
import com.algorithmx.q_base.core.state.LocalAppAccessState
import com.algorithmx.q_base.core.designsystem.theme.QbaseTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopAppBar(
    title: String,
    currentUser: UserEntity?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showProfileIcon: Boolean = true,
    isLarge: Boolean = true,
    titleCentered: Boolean = false,
    appAccessState: AppAccessState = LocalAppAccessState.current
) {
    val titleContent: @Composable () -> Unit = {
        Column(
            horizontalAlignment = if (titleCentered) Alignment.CenterHorizontally else Alignment.Start,
            modifier = Modifier.padding(horizontal = if (titleCentered) 16.dp else 4.dp)
        ) {
            Text(
                text = title,
                style = if (isLarge) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                lineHeight = if (isLarge) 32.sp else 24.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }
            AppAccessStateBadge(appAccessState = appAccessState)
        }
    }

    val actionsContent: @Composable RowScope.() -> Unit = {
        actions()
        if (showProfileIcon) {
            ProfileIconButton(
                user = currentUser,
                onClick = onProfileClick
            )
        }
    }

    val colors = if (isLarge) {
        TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.9f)
        )
    } else {
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.9f)
        )
    }

    if (isLarge) {
        LargeTopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actionsContent,
            scrollBehavior = scrollBehavior,
            colors = colors,
            modifier = modifier
        )
    } else {
        if (titleCentered) {
            CenterAlignedTopAppBar(
                title = titleContent,
                navigationIcon = navigationIcon,
                actions = actionsContent,
                scrollBehavior = scrollBehavior,
                colors = colors,
                modifier = modifier
            )
        } else {
            TopAppBar(
                title = titleContent,
                navigationIcon = navigationIcon,
                actions = actionsContent,
                scrollBehavior = scrollBehavior,
                colors = colors,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AppAccessStateBadge(appAccessState: AppAccessState) {
    val (label, color) = when (appAccessState) {
        AppAccessState.Online -> "Online" to Color(0xFF2E7D32)
        AppAccessState.Offline -> "Offline" to MaterialTheme.colorScheme.error
        AppAccessState.NotLoggedIn -> "Not logged in" to MaterialTheme.colorScheme.tertiary
    }

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}


@Composable
fun SessionCard(
    session: StudySession,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PlayArrow, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = session.title.ifEmpty { "Practice Session" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Started: ${java.text.SimpleDateFormat("MMM dd", Locale.getDefault()).format(session.createdTimestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SessionListItem(
    session: StudySession,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.title.ifEmpty { "Mock Session: ${session.sessionId.take(5).uppercase()}" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val score = String.format(Locale.getDefault(), "%.0f", session.scoreAchieved)
                Text(
                    text = "Score: $score%",
                    color = if (session.scoreAchieved >= 50) Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Text(" | ", color = Color.Gray)
                Text("Started: ${java.text.SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(session.createdTimestamp)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@Composable
fun SectionHeader(
    title: String, 
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = title, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("See All", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SessionCardPreview() {
    QbaseTheme {
        SessionCard(
            session = StudySession(
                sessionId = "1",
                collectionId = "1",
                title = "Physics Practice",
                timeLimitSeconds = 3600,
                createdTimestamp = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionListItemPreview() {
    QbaseTheme {
        SessionListItem(
            session = StudySession(
                sessionId = "1",
                collectionId = "1",
                title = "MCQ Practice #1",
                scoreAchieved = 85.0f,
                timeLimitSeconds = 1800,
                createdTimestamp = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SectionHeaderPreview() {
    QbaseTheme {
        SectionHeader(
            title = "Recent Sessions",
            onActionClick = {},
            icon = Icons.Rounded.Star
        )
    }
}