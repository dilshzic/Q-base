package com.algorithmx.q_base.feature.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.algorithmx.q_base.feature.settings.presentation.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.algorithmx.q_base.feature.settings.AVATAR_TEMPLATES
import com.algorithmx.q_base.feature.settings.bounceClick

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