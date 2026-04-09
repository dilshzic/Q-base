package com.algorithmx.q_base.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.algorithmx.q_base.data.auth.UserProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    coroutineScope.launch {
                        try {
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            val authResult = auth.signInWithCredential(credential).await()
                            authResult.user?.let { user ->
                                viewModel.onGoogleSignInSuccess(user)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LoginScreen", "Firebase Google Auth failed", e)
                            Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: ApiException) {
                android.util.Log.e("LoginScreen", "Google Sign In failed", e)
                Toast.makeText(context, "Google Sign In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .size(240.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {}

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Q", 
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Continue your learning journey",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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

            val canSubmit = !state.isLoading && email.isNotBlank() && password.isNotBlank()
            val buttonScale by animateFloatAsState(
                targetValue = if (canSubmit) 1f else 0.95f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "button_scale"
            )

            Button(
                onClick = { viewModel.signIn(email, password) },
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
                        strokeWidth = 3.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                } else {
                    Text(
                        "LOGIN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.25.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.outline,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Sign in with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onNavigateToSignup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Don't have an account? Sign Up",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
