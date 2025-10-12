package com.laundrypro.mymaidmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laundrypro.mymaidmanager.ui.theme.MyMaidManagerTheme
import com.laundrypro.mymaidmanager.viewmodel.AuthResult
import com.laundrypro.mymaidmanager.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyMaidManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthScreen(authViewModel)
                }
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    var showLogin by rememberSaveable { mutableStateOf(true) }
    val authResult by viewModel.authResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to the main app screen
                viewModel.resetAuthState()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CleaningServices,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Maid Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = showLogin,
                label = "AuthFormAnimation",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(150)) togetherWith
                            fadeOut(animationSpec = tween(150)))
                        .using(SizeTransform(clip = false))
                }
            ) { isLoginScreen ->
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (isLoginScreen) {
                        LoginForm(
                            onSwitchToSignUp = { showLogin = false },
                            authResult = authResult,
                            onLoginClicked = { email, password ->
                                viewModel.loginUser(email, password)
                            }
                        )
                    } else {
                        SignUpForm(
                            onSwitchToLogin = { showLogin = true },
                            authResult = authResult,
                            onSignUpClicked = { name, email, password ->
                                viewModel.registerUser(name, email, password)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm(
    onSwitchToSignUp: () -> Unit,
    authResult: AuthResult,
    onLoginClicked: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading = authResult is AuthResult.Loading

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome Back!", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            },
            enabled = !isLoading
        )

        Button(
            onClick = { onLoginClicked(email, password) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        TextButton(onClick = onSwitchToSignUp, enabled = !isLoading) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun SignUpForm(
    onSwitchToLogin: () -> Unit,
    authResult: AuthResult,
    onSignUpClicked: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val isLoading = authResult is AuthResult.Loading

    val passwordValidation = remember(password) { validatePassword(password) }
    val passwordsMatch = remember(password, confirmPassword) { password == confirmPassword }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create Account", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, null) }
            },
            isError = password.isNotEmpty() && passwordValidation.isNotEmpty(),
            enabled = !isLoading
        )
        if (password.isNotEmpty() && passwordValidation.isNotEmpty()) {
            Text(
                text = passwordValidation.joinToString("\n"),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(imageVector = image, null) }
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            enabled = !isLoading
        )
        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                text = "Passwords do not match.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onSignUpClicked(name, email, password) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = passwordValidation.isEmpty() && passwordsMatch && email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        TextButton(onClick = onSwitchToLogin, enabled = !isLoading) {
            Text("Already have an account? Login")
        }
    }
}

private fun validatePassword(password: String): List<String> {
    val errors = mutableListOf<String>()
    if (password.length < 6) {
        errors.add("Must be at least 6 characters long.")
    }
    if (!password.any { it.isUpperCase() }) {
        errors.add("Requires an uppercase letter.")
    }
    if (!password.any { it.isDigit() }) {
        errors.add("Requires a number.")
    }
    if (password.all { it.isLetterOrDigit() }) {
        errors.add("Requires a special character.")
    }
    return errors
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyMaidManagerTheme {
        // AuthScreen(AuthViewModel()) // This preview won't work well due to the ViewModel dependency
    }
}