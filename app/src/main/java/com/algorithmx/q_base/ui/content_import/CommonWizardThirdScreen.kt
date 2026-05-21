package com.algorithmx.q_base.ui.content_import

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ── Step 3: Active AI Generation Processing Screen (Waiting Screen) ──

@Composable
fun WaitingView(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "waiting")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    // Dynamic Loading Tips Carousel
    val tips = listOf(
        "Reviewing questions within 24 hours increases retention by 80%.",
        "Explain incorrect answers out loud to reinforce conceptual mastery.",
        "Shorter, focused practice sessions are more effective than cramming.",
        "Custom instructions allow you to restrict AI topics to specific chapters."
    )
    var currentTipIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentTipIndex = (currentTipIndex + 1) % tips.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotating outer ring
            Surface(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer { rotationZ = rotation },
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(2.dp, Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
            ) {}
            
            // Pulsing inner AI sphere
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text(
            text = message, 
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black, 
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "This may take up to a minute.", 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.outline
        )
        
        Spacer(Modifier.height(48.dp))
        
        // Smart tips container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = tips[currentTipIndex],
                    transitionSpec = {
                        fadeIn(tween(500)).togetherWith(fadeOut(tween(500)))
                    },
                    label = "TipCarousel"
                ) { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}