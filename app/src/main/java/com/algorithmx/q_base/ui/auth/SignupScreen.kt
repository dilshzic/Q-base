package com.algorithmx.q_base.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.data.auth.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSignupSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Decorative background element
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .size(200.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        ) {}

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBackToLogin) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Expressive Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Join Q-Base",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Create your professional account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Picture Placeholder
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { /* Future: Image Picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    // Small edit icon
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Rounded.Mail, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(
                        "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = expandVertically() + fadeIn()
                ) {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                val canSubmit = !state.isLoading && 
                                username.isNotBlank() && 
                                email.isNotBlank() && 
                                password.isNotBlank() && 
                                password == confirmPassword

                val buttonScale by animateFloatAsState(
                    targetValue = if (canSubmit) 1f else 0.95f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "button_scale"
                )

                Button(
                    onClick = { viewModel.signUp(email, password, username) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        },
                    enabled = canSubmit,
                    shape = MaterialTheme.shapes.large,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            "CREATE ACCOUNT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.25.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Already have an account? Log In",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
