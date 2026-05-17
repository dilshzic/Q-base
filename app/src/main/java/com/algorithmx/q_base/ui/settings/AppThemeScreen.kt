package com.algorithmx.q_base.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.brainConfig.collectAsStateWithLifecycle()
    val currentTheme = config.themeMode

    val themes = listOf(
        ThemeOption("SYSTEM", "Follow System", "Default appearance based on device settings"),
        ThemeOption("LIGHT", "Light Mode", "Clean and bright interface"),
        ThemeOption("DARK", "Dark Mode", "Easy on the eyes in low light"),
        ThemeOption("MONOCHROME", "Monochrome", "Elegant black and white aesthetic")
    )

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = "App Theme",
                currentUser = null,
                onProfileClick = {},
                showProfileIcon = false,
                isLarge = false,
                titleCentered = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "CHOOSE APPEARANCE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
            }

            items(themes) { option ->
                ThemeCard(
                    option = option,
                    isSelected = currentTheme == option.id,
                    onClick = { viewModel.updateTheme(option.id) }
                )
            }
        }
    }
}

data class ThemeOption(val id: String, val title: String, val subtitle: String)

@Composable
fun ThemeCard(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null // Handled by card click
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(option.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(option.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            if (isSelected) {
                Icon(
                    Icons.Rounded.Check, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
